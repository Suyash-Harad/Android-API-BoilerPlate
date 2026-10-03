package com.bartech.api_usage_boilerplate

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bartech.api_usage_boilerplate.databinding.ActivityAboutUsBinding

class AboutUsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutUsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutUsBinding.inflate(layoutInflater)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.ivLinkedin.setOnClickListener {
            // Handle LinkedIn click
            openLinkedIn()
        }

        binding.ivYoutube.setOnClickListener {
            // Handle YouTube click
            openYouTube()
        }

        // Open phone dialer
        binding.tvPhone.setOnClickListener {
            val phoneIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:8976722202")
            }
            startActivity(phoneIntent)
        }

        // Open email app
        binding.tvEmail.setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:sales@bartechdata.net")
            }
            startActivity(Intent.createChooser(emailIntent, "Send Email"))
        }

        // Open website
        binding.tvWebsite.setOnClickListener {
            val websiteIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://www.bartechdata.net")
            }
            startActivity(websiteIntent)
        }

        // Open Google Maps for Address 1
        binding.tvAddress1.setOnClickListener {
            val address = binding.tvAddress1.text.toString()
            val gmapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(address)))
            gmapIntent.setPackage("com.google.android.apps.maps")
            if (gmapIntent.resolveActivity(packageManager) != null) {
                startActivity(gmapIntent)
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=" + Uri.encode(address)))
                startActivity(webIntent)
            }
        }

        // Open Google Maps for Address 2
        binding.tvAddress2.setOnClickListener {
            val address = binding.tvAddress2.text.toString()
            val gmapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(address)))
            gmapIntent.setPackage("com.google.android.apps.maps")
            if (gmapIntent.resolveActivity(packageManager) != null) {
                startActivity(gmapIntent)
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=" + Uri.encode(address)))
                startActivity(webIntent)
            }
        }

    }

    private fun openLinkedIn() {
        // Replace with your actual LinkedIn profile URL
        val linkedInUrl = "https://www.linkedin.com/company/74933930/"
        val linkedInAppUri = "linkedin://company/74933930"

        try {
            // Check if LinkedIn app is installed
            if (isAppInstalled("com.linkedin.android")) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(linkedInAppUri))
                intent.setPackage("com.linkedin.android")
                startActivity(intent)
            } else {
                // Open in browser if app is not installed
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(linkedInUrl))
                startActivity(intent)
            }
        } catch (e: Exception) {
            // Fallback to browser if app intent fails
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(linkedInUrl))
            startActivity(intent)
            Toast.makeText(this, "Opening LinkedIn in browser", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openYouTube() {
        // Replace with your actual YouTube channel URL
        val youtubeUrl = "https://www.youtube.com/watch?v=74ZxsGqy78Y"
        val youtubeAppUri = "vnd.youtube://74ZxsGqy78Y"
        try {
            // Check if YouTube app is installed
            if (isAppInstalled("com.google.android.youtube")) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(youtubeAppUri))
                intent.setPackage("com.google.android.youtube")
                startActivity(intent)
            } else {
                // Open in browser if app is not installed
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(youtubeUrl))
                startActivity(intent)
            }
        } catch (e: Exception) {
            // Fallback to browser if app intent fails
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(youtubeUrl))
            startActivity(intent)
            Toast.makeText(this, "Opening YouTube in browser", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

}