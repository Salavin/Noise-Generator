package `in`.samlav.noisegenerator

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import `in`.samlav.noisegenerator.databinding.FragmentFirstBinding
import java.text.NumberFormat
import java.util.*
import kotlin.math.roundToInt
import kotlin.math.roundToLong

val buffer_vals = listOf("64", "128", "256", "512", "1024", "2048", "4096")

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

        when (sharedPref.getString(getString(R.string.noise_type_pref), getString(R.string.type_white)))
        {
            getString(R.string.type_white) -> binding.toggleButtonNoiseType.check(R.id.button_white)
            getString(R.string.type_pink) -> binding.toggleButtonNoiseType.check(R.id.button_pink)
        }
        binding.buttonWhite.setOnClickListener {
            with (sharedPref.edit()) {
                putString(getString(R.string.noise_type_pref), getString(R.string.type_white))
                apply()
            }
            (activity as MainActivity).audioHandler.isPink = false
        }
        binding.buttonPink.setOnClickListener {
            with (sharedPref.edit()) {
                putString(getString(R.string.noise_type_pref), getString(R.string.type_pink))
                apply()
            }
            (activity as MainActivity).audioHandler.isPink = true
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
        binding.switchAutoShutoff.setOnClickListener {
            binding.buttonTime.isEnabled = binding.switchAutoShutoff.isChecked
            with (sharedPref.edit()) {
                putBoolean(getString(R.string.auto_shutoff_pref), binding.switchAutoShutoff.isChecked)
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

        binding.menuBufferSizeValue.setText(sharedPref.getInt(getString(R.string.buffer_pref), 2048).toString())
        setUpMenuAdapter()
        binding.menuBufferSizeValue.setOnItemClickListener { _, _, position, _ ->
            with (sharedPref.edit()) {
                putInt(getString(R.string.buffer_pref), buffer_vals[position].toInt())
                apply()
            }
        }

        binding.buttonBufferInfo.setOnClickListener {
            context?.let { it1 ->
                MaterialAlertDialogBuilder(it1)
                    .setTitle("Buffer Size")
                    .setMessage("The buffer size is essentially the size of the chunk of sound being sent to the audio player. If you are having performance issues or glitchy audio, try increasing this size.")
                    .setNeutralButton("Ok") { _, _ -> }
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

    override fun onResume()
    {
        super.onResume()
        if ((activity as MainActivity).audioHandler.getState() == AudioHandlerConstants.STATE_PLAYING)
        {
            setBufferMenuEnabled(false)
        }
        else if ((activity as MainActivity).audioHandler.getState() != AudioHandlerConstants.STATE_STOPPED)
        {
            setBufferMenuEnabled(false)
            setFadesEnabled(false)
        }
        setUpMenuAdapter()
    }

    private fun setUpMenuAdapter()
    {
        val items = buffer_vals
        val adapter = ArrayAdapter(requireContext(), R.layout.list_item, items)
        binding.menuBufferSizeValue.setAdapter(adapter)
    }

    fun setBufferMenuEnabled(enabled: Boolean)
    {
        binding.menuBufferSize.isEnabled = enabled
    }

    fun setFadesEnabled(enabled: Boolean)
    {
        binding.sliderFadeIn.isEnabled = enabled
        binding.sliderFadeOut.isEnabled = enabled
    }
}