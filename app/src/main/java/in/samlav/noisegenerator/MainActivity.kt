package `in`.samlav.noisegenerator

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.MenuProvider
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import `in`.samlav.noisegenerator.databinding.ActivityMainBinding
import java.time.LocalTime

class MainActivity : AppCompatActivity()
{

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    lateinit var audioHandler: AudioHandler
    private lateinit var sharedPref: SharedPreferences
    private lateinit var handleUIChangesThread: Thread
    private lateinit var autoShutoffThread: Thread

    override fun onCreate(savedInstanceState: Bundle?)
    {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        sharedPref = getPreferences(Context.MODE_PRIVATE)

        audioHandler = AudioHandler(
            sharedPref.getString(getString(R.string.noise_type_pref), getString(R.string.type_white)) == getString(R.string.type_pink),
            sharedPref.getInt(getString(R.string.vol_pref), 10),
            sharedPref.getInt(getString(R.string.fade_in_pref), 50),
            sharedPref.getInt(getString(R.string.fade_out_pref), 50),
            sharedPref.getInt(getString(R.string.buffer_pref), 1024))

        autoShutoffThread = Thread {
            if (sharedPref.getBoolean(getString(R.string.auto_shutoff_pref), false) &&
                audioHandler.getState() == AudioHandlerConstants.STATE_PLAYING &&
                sharedPref.getInt(getString(R.string.shutoff_hour_pref), 9) == LocalTime.now().hour &&
                sharedPref.getInt(getString(R.string.shutoff_minute_pref), 0) == LocalTime.now().minute)
            {
                binding.fab.callOnClick()
            }
            Handler(Looper.getMainLooper()).postDelayed(autoShutoffThread, 60000)
        }
        Handler(Looper.getMainLooper()).postDelayed(autoShutoffThread, (60000 - (LocalTime.now().second * 1000)).toLong())

        binding.fab.setOnClickListener {
            val firstFragment: FirstFragment? = try
            {
                val navHostFragment =
                    supportFragmentManager.primaryNavigationFragment as NavHostFragment?
                val fragmentManager: FragmentManager = navHostFragment!!.childFragmentManager
                fragmentManager.primaryNavigationFragment as FirstFragment
            } catch (_: java.lang.ClassCastException) {
                null
            }

            when (audioHandler.getState())
            {
                AudioHandlerConstants.STATE_PLAYING -> {
                    firstFragment?.setFadesEnabled(false)
                    binding.fab.setImageDrawable(AppCompatResources.getDrawable(applicationContext, R.drawable.baseline_stop_24))
                    handleUIChangesThread = Thread {
                        if (audioHandler.getState() == AudioHandlerConstants.STATE_STOPPED)
                        {
                            if (firstFragment != null)
                            {
                                try
                                {
                                    with (firstFragment) {
                                        setFadesEnabled(true)
                                        setBufferMenuEnabled(true)
                                    }
                                } catch (_: java.lang.NullPointerException) { }
                            }
                            binding.fab.setImageDrawable(AppCompatResources.getDrawable(applicationContext, R.drawable.baseline_play_arrow_24))
                        }
                    }
                    Handler(Looper.getMainLooper()).postDelayed(handleUIChangesThread, sharedPref.getInt(getString(R.string.fade_out_pref), 50).toLong() + 1)
                }
                AudioHandlerConstants.STATE_STOPPED -> {
                    if (firstFragment != null)
                    {
                        with (firstFragment) {
                            setFadesEnabled(false)
                            setBufferMenuEnabled(false)
                        }
                    }
                    binding.fab.setImageDrawable(AppCompatResources.getDrawable(applicationContext, R.drawable.baseline_stop_24))
                    handleUIChangesThread = Thread {
                        if (audioHandler.getState() == AudioHandlerConstants.STATE_PLAYING)
                        {
                            try
                            {
                                firstFragment?.setFadesEnabled(true)
                            } catch (_: java.lang.NullPointerException) { }
                            binding.fab.setImageDrawable(AppCompatResources.getDrawable(applicationContext, R.drawable.baseline_pause_24))
                        }
                    }
                    Handler(Looper.getMainLooper()).postDelayed(handleUIChangesThread, sharedPref.getInt(getString(R.string.fade_in_pref), 50).toLong() + 1)
                }
                else -> {
                    if (firstFragment != null)
                    {
                        with (firstFragment) {
                            setFadesEnabled(true)
                            setBufferMenuEnabled(true)
                        }
                    }
                    binding.fab.setImageDrawable(AppCompatResources.getDrawable(applicationContext, R.drawable.baseline_play_arrow_24))
                }
            }

            audioHandler.isPink = sharedPref.getString(getString(R.string.noise_type_pref), getString(R.string.type_white)) == getString(R.string.type_pink)
            audioHandler.fadeIn = sharedPref.getInt(getString(R.string.fade_in_pref), 50)
            audioHandler.fadeOut = sharedPref.getInt(getString(R.string.fade_out_pref), 50)
            audioHandler.bufferSize = sharedPref.getInt(getString(R.string.buffer_pref), 1024)
            audioHandler.toggle()
        }

        addMenuProvider(object : MenuProvider
        {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Add menu items here
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean {
                // Handle the menu selection
                return when (item.itemId)
                {
                    R.id.action_about -> {
                        findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.action_FirstFragment_to_SecondFragment)
                        true
                    }
                    else -> false
                }
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean
    {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}