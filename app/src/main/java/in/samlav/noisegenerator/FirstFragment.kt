package `in`.samlav.noisegenerator

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.ArrayAdapter
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import `in`.samlav.noisegenerator.databinding.FragmentFirstBinding
import java.text.NumberFormat
import kotlin.math.roundToInt

val buffer_vals = listOf("64", "128", "256", "512", "1024", "2048", "4096")
val fab_location_vals = listOf("Upper Left", "Upper Right", "Lower Left", "Lower Right")
const val SPEED_SLOW = 25
const val SPEED_FAST = 50

/**
 * The [Fragment] that contains the main screen for the user.
 */
class FirstFragment : Fragment()
{

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var sharedPref: SharedPreferences
    private var currentlyAnimating = false
    private var stopAnim = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View
    {
        sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)!!
        if (!sharedPref.contains(getString(R.string.noise_type_pref)))
        {
            with (sharedPref.edit()) {
                putString(getString(R.string.noise_type_pref), getString(R.string.type_white))
                apply()
            }
        }
        if (!sharedPref.contains(getString(R.string.vol_pref)))
        {
            with (sharedPref.edit()) {
                putInt(getString(R.string.vol_pref), 10)
                apply()
            }
        }
        if (!sharedPref.contains(getString(R.string.fade_in_pref)))
        {
            with (sharedPref.edit()) {
                putInt(getString(R.string.fade_in_pref), 50)
                apply()
            }
        }
        if (!sharedPref.contains(getString(R.string.fade_out_pref)))
        {
            with (sharedPref.edit()) {
                putInt(getString(R.string.fade_out_pref), 50)
                apply()
            }
        }
        if (!sharedPref.contains(getString(R.string.auto_shutoff_pref)))
        {
            with (sharedPref.edit()) {
                putBoolean(getString(R.string.auto_shutoff_pref), false)
                apply()
            }
        }
        if (!sharedPref.contains(getString(R.string.shutoff_hour_pref)))
        {
            with (sharedPref.edit()) {
                putInt(getString(R.string.shutoff_hour_pref), 9)
                apply()
            }
        }
        if (!sharedPref.contains(getString(R.string.shutoff_minute_pref)))
        {
            with (sharedPref.edit()) {
                putInt(getString(R.string.shutoff_minute_pref), 0)
                apply()
            }
        }
        if (!sharedPref.contains(getString(R.string.show_eq_pref)))
        {
            with (sharedPref.edit()) {
                putBoolean(getString(R.string.show_eq_pref), true)
                apply()
            }
        }
        if (!sharedPref.contains(getString(R.string.buffer_pref)))
        {
            with (sharedPref.edit()) {
                putInt(getString(R.string.buffer_pref), 2056)
                apply()
            }
        }
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        binding.vumeter.stop(false)
        binding.vumeter.setOnClickListener {
            (activity as MainActivity).clickButton()
        }
        if (!sharedPref.getBoolean(getString(R.string.show_eq_pref), true))
        {
            binding.vumeter.visibility = View.GONE
        }

        when (sharedPref.getString(getString(R.string.noise_type_pref), getString(R.string.type_white)))
        {
            getString(R.string.type_white) -> {
                binding.toggleButtonNoiseType.check(R.id.button_white)
                binding.vumeter.speed = SPEED_FAST
            }
            getString(R.string.type_pink) -> {
                binding.toggleButtonNoiseType.check(R.id.button_pink)
                binding.vumeter.speed = SPEED_SLOW
            }
        }
        binding.buttonWhite.setOnClickListener {
            with (sharedPref.edit()) {
                putString(getString(R.string.noise_type_pref), getString(R.string.type_white))
                apply()
            }
            (activity as MainActivity).audioHandler.isPink = false
            binding.vumeter.speed = SPEED_FAST
        }
        binding.buttonPink.setOnClickListener {
            with (sharedPref.edit()) {
                putString(getString(R.string.noise_type_pref), getString(R.string.type_pink))
                apply()
            }
            (activity as MainActivity).audioHandler.isPink = true
            binding.vumeter.speed = SPEED_SLOW
        }

        val vol = sharedPref.getInt(getString(R.string.vol_pref), 10)
        binding.sliderVolume.value = vol.toFloat()
        binding.sliderVolume.addOnChangeListener { _, value, _ ->
            with (sharedPref.edit()) {
                putInt(getString(R.string.vol_pref), value.toInt())
                apply()
            }
            (activity as MainActivity).audioHandler.setVolume(value.toInt())
            (activity as MainActivity).audioHandler.vol = value.toInt()
            if (!currentlyAnimating)
            {
                binding.vumeter.blockMaxHeight = value / 10
            }
        }
        binding.sliderVolume.setLabelFormatter { value: Float ->
            NumberFormat.getPercentInstance().format(value / 10)
        }

        binding.sliderFadeIn.setLabelFormatter { value: Float ->
            if (value < 1000)
            {
                NumberFormat.getNumberInstance().format(value) + "ms"
            }
            else
            {
                NumberFormat.getNumberInstance().format(value / 1000) + "s"
            }
        }
        binding.sliderFadeIn.value = sharedPref.getInt(getString(R.string.fade_in_pref), 50).toFloat()
        binding.sliderFadeIn.addOnChangeListener { _, value, _ ->
            with (sharedPref.edit()) {
                putInt(getString(R.string.fade_in_pref), value.toInt())
                apply()
            }
            (activity as MainActivity).audioHandler.fadeIn = value.toInt()
        }

        binding.sliderFadeOut.setLabelFormatter { value: Float ->
            if (value < 1000)
            {
                NumberFormat.getNumberInstance().format(value) + "ms"
            }
            else
            {
                NumberFormat.getNumberInstance().format(value / 1000) + "s"
            }
        }
        binding.sliderFadeOut.value = sharedPref.getInt(getString(R.string.fade_out_pref), 50).toFloat()
        binding.sliderFadeOut.addOnChangeListener { _, value, _ ->
            with (sharedPref.edit()) {
                putInt(getString(R.string.fade_out_pref), value.toInt())
                apply()
            }
            (activity as MainActivity).audioHandler.fadeOut = value.toInt()
        }

        binding.switchAutoShutoff.isChecked = sharedPref.getBoolean(getString(R.string.auto_shutoff_pref), false)
        binding.buttonTime.isEnabled = sharedPref.getBoolean(getString(R.string.auto_shutoff_pref), false)
        binding.buttonTime.text = getString(R.string.time, ((sharedPref.getInt(getString(R.string.shutoff_hour_pref), 9) - 1) % 12) + 1, if (sharedPref.getInt(getString(R.string.shutoff_minute_pref), 0) < 10) "0" else "", sharedPref.getInt(getString(R.string.shutoff_minute_pref), 0), if (sharedPref.getInt(getString(R.string.shutoff_hour_pref), 9) < 12) "am" else "pm")
        binding.switchAutoShutoff.setOnCheckedChangeListener { _, isChecked ->
            binding.buttonTime.isEnabled = isChecked
            with (sharedPref.edit()) {
                putBoolean(getString(R.string.auto_shutoff_pref), isChecked)
                apply()
            }
        }
        binding.buttonTime.setOnClickListener {
            val picker =
                MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_12H)
                    .setHour(sharedPref.getInt(getString(R.string.shutoff_hour_pref), 9))
                    .setMinute(sharedPref.getInt(getString(R.string.shutoff_minute_pref), 0))
                    .setTitleText("Select Shutoff Time")
                    .build()
            picker.addOnPositiveButtonClickListener {
                with (sharedPref.edit()) {
                    putInt(getString(R.string.shutoff_hour_pref), picker.hour)
                    putInt(getString(R.string.shutoff_minute_pref), picker.minute)
                    apply()
                }
                binding.buttonTime.text = getString(R.string.time, ((sharedPref.getInt(getString(R.string.shutoff_hour_pref), 9) - 1) % 12) + 1, if (sharedPref.getInt(getString(R.string.shutoff_minute_pref), 0) < 10) "0" else "", sharedPref.getInt(getString(R.string.shutoff_minute_pref), 0), if (sharedPref.getInt(getString(R.string.shutoff_hour_pref), 9) < 12) "am" else "pm")
            }
            activity?.supportFragmentManager?.let { it1 -> picker.show(it1, "MainActivity") }
        }

        binding.switchShowEq.isChecked = sharedPref.getBoolean(getString(R.string.show_eq_pref), true)
        binding.switchShowEq.setOnCheckedChangeListener { _, isChecked ->
            with (sharedPref.edit()) {
                putBoolean(getString(R.string.show_eq_pref), isChecked)
                apply()
            }
            if (isChecked)
            {
                binding.vumeter.visibility = View.VISIBLE
            }
            else
            {
                binding.vumeter.visibility = View.GONE
            }
        }

        setUpMenuAdapter()

        binding.menuBufferSizeValue.setText(sharedPref.getInt(getString(R.string.buffer_pref), 2048).toString())
        binding.menuBufferSizeValue.setOnItemClickListener { _, _, position, _ ->
            with (sharedPref.edit()) {
                putInt(getString(R.string.buffer_pref), buffer_vals[position].toInt())
                apply()
            }
        }

        binding.menuFabLocationValue.setText(fab_location_vals[sharedPref.getInt(getString(R.string.fab_location_pref), 3)])
        binding.menuFabLocationValue.setOnItemClickListener { _, _, position, _ ->
            with (sharedPref.edit()) {
                putInt(getString(R.string.fab_location_pref), position)
                apply()
            }
            (activity as MainActivity).changeFabLocation(position)
        }

        binding.buttonBufferInfo.setOnClickListener {
            context?.let { it1 ->
                MaterialAlertDialogBuilder(it1)
                    .setTitle("Buffer Size")
                    .setMessage("The buffer size is essentially the size of the chunk of sound being sent to the audio player. If you are having performance issues or glitchy audio, try increasing this size.")
                    .setPositiveButton("Ok") { _, _ -> }
                    .show()
            }
        }

        // The usage of an interface lets you inject your own implementation
        val menuHost: MenuHost = requireActivity()

        // Add menu items without using the Fragment Menu APIs
        // Note how we can tie the MenuProvider to the viewLifecycleOwner
        // and an optional Lifecycle.State (here, RESUMED) to indicate when
        // the menu should be visible
        menuHost.addMenuProvider(object : MenuProvider
        {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Add menu items here
                menuInflater.inflate(R.menu.menu_main, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Handle the menu selection
                return when (menuItem.itemId) {
                    R.id.action_about -> {
                        findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
                        true
                    }
                    R.id.action_show_advanced -> {
                        menuItem.isChecked = !menuItem.isChecked
                        showAdvanced(menuItem.isChecked)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onDestroyView()
    {
        super.onDestroyView()
        _binding = null
    }

    override fun onPause()
    {
        super.onPause()
        if (currentlyAnimating)
        {
            cancelAnimation()
        }
        stopAnimation()
    }

    override fun onResume()
    {
        super.onResume()
        if ((activity as MainActivity).audioHandler.getState() == AudioHandlerConstants.STATE_PLAYING)
        {
            setBufferMenuEnabled(false)
            startAnimation()
        }
        else if ((activity as MainActivity).audioHandler.getState() == AudioHandlerConstants.STATE_STOPPED)
        {
            stopAnimation(false)
        }
        else
        {
            setBufferMenuEnabled(false)
            setFadesEnabled(false)
            var isFadingIn = false
            var fadeLength = binding.sliderFadeOut.value
            val curFrame = (((activity as MainActivity).audioHandler.samplesFaded.toFloat() / AudioHandlerConstants.SAMPLE_RATE) * 100).roundToInt()
            if ((activity as MainActivity).audioHandler.getState() == AudioHandlerConstants.STATE_FADING_IN)
            {
                isFadingIn = true
                fadeLength = binding.sliderFadeIn.value
            }
            startAnimation(isFadingIn, fadeLength.toInt(), curFrame)
        }
        setUpMenuAdapter()
    }

    /**
     * Sets up the Menu Adapter.
     */
    private fun setUpMenuAdapter()
    {
        val bufferItems = buffer_vals
        val bufferAdapter = ArrayAdapter(requireContext(), R.layout.list_item, bufferItems)
        binding.menuBufferSizeValue.setAdapter(bufferAdapter)

        val fabLocationItems = fab_location_vals
        val fabLocationAdapter = ArrayAdapter(requireContext(), R.layout.list_item, fabLocationItems)
        binding.menuFabLocationValue.setAdapter(fabLocationAdapter)
    }

    /**
     * Helper method for showing the advanced options.
     *
     * @param show whether we are showing or hiding the advanced options.
     */
    private fun showAdvanced(show: Boolean)
    {
        val list = listOf(
        binding.textAdvancedOptions,
        binding.textShowEq,
        binding.textShowEq,
        binding.switchShowEq,
        binding.textBufferSize,
        binding.buttonBufferInfo,
        binding.menuBufferSize,
        binding.menuBufferSizeValue,
        binding.textFabLocation,
        binding.menuFabLocation,
        binding.menuFabLocationValue)
        val animationDuration = 150L
        if (show)
        {
            for (viewItem in list)
            {
                viewItem.apply {
                    // Set the content view to 0% opacity but visible, so that it is visible
                    // (but fully transparent) during the animation.
                    alpha = 0f
                    visibility = View.VISIBLE

                    // Animate the content view to 100% opacity, and clear any animation
                    // listener set on the view.
                    animate()
                        .alpha(1f)
                        .setDuration(animationDuration)
                        .setListener(null)
                }
            }
        }
        else
        {
            for (viewItem in list)
            {
                viewItem.animate()
                    .alpha(0f)
                    .setDuration(animationDuration)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            viewItem.visibility = View.GONE
                        }
                    })
            }
        }
    }

    /**
     * Set buffer menu enabled
     *
     * @param enabled
     */
    fun setBufferMenuEnabled(enabled: Boolean)
    {
        binding.menuBufferSize.isEnabled = enabled
    }

    /**
     * Set fades enabled
     *
     * @param enabled
     */
    fun setFadesEnabled(enabled: Boolean)
    {
        binding.sliderFadeIn.isEnabled = enabled
        binding.sliderFadeOut.isEnabled = enabled
    }

    /**
     * Starts the eq animation with no fade.
     */
    fun startAnimation()
    {
        binding.vumeter.blockMaxHeight = binding.sliderVolume.value / 10
        binding.vumeter.resume(true)
    }

    /**
     * Stops the eq animation.
     *
     * @param animate whether or not to animate the stop.
     */
    fun stopAnimation(animate: Boolean = true)
    {
        binding.vumeter.stop(animate)
    }

    /**
     * Starts the eq animation with a fade in.
     *
     * @param isFadeIn whether this is a fade in or fade out
     * @param animLength how long to animate the fade in/out
     * @param startFromFrame optional: start from a specific point in the animation
     */
    fun startAnimation(isFadeIn: Boolean, animLength: Int, startFromFrame: Int = 0)
    {
        currentlyAnimating = true
        binding.vumeter.resume(true)
        Thread {
            var curAnimFrame = startFromFrame
            val totalAnimFrames = animLength / 10
            while (curAnimFrame++ < totalAnimFrames)
            {
                if (isFadeIn)
                {
                    if (stopAnim)
                    {
                        binding.vumeter.blockMaxHeight = binding.sliderVolume.value / 10
                        break
                    }
                    binding.vumeter.blockMaxHeight =
                        (curAnimFrame.toFloat() / totalAnimFrames) * (binding.sliderVolume.value / 10)
                } else
                {
                    if (stopAnim)
                    {
                        binding.vumeter.blockMaxHeight = 0f
                        break
                    }
                    binding.vumeter.blockMaxHeight =
                        (1 - (curAnimFrame.toFloat() / totalAnimFrames)) * (binding.sliderVolume.value / 10)
                }
                Thread.sleep(10)
            }
            if (!isFadeIn)
            {
                binding.vumeter.stop(true)
            }
            stopAnim = false
            currentlyAnimating = false
        }.start()
    }

    /**
     * Stop the animation at its current point and skip to playing or stopped.
     */
    fun cancelAnimation()
    {
        stopAnim = true
    }
}