package `in`.samlav.noisegenerator

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import `in`.samlav.noisegenerator.databinding.FragmentSecondBinding

/**
 * A simple [Fragment] representing the About page.
 */
class SecondFragment : Fragment()
{
    private var _binding: FragmentSecondBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View
    {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonWebsite.setOnClickListener {
            goToUrl("https://samlav.in")
        }
        binding.buttonProjectRepository.setOnClickListener {
            goToUrl("https://github.com/Salavin/Ear-Trainer")
        }
        binding.buttonBuyMeACoffee.setOnClickListener {
            goToUrl("https://www.buymeacoffee.com/salavin")
        }
        binding.textVersion.text = getString(R.string.version, BuildConfig.VERSION_NAME)
    }

    override fun onDestroyView()
    {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Helper function to send the User to a website.
     *
     * @param url the URL of the website to send the User to
     */
    private fun goToUrl (url: String)
    {
        val uriUrl = Uri.parse(url)
        val launchBrowser = Intent(Intent.ACTION_VIEW, uriUrl)
        startActivity(launchBrowser)
    }
}