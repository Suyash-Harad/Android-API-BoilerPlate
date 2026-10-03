package com.bartech.api_usage_boilerplate

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bartech.api_usage_boilerplate.databinding.ActivityWebSettingBinding
import com.bartech.api_usage_boilerplate.databinding.LoadingDialogBinding
import com.google.android.material.snackbar.Snackbar
import okhttp3.OkHttpClient
import okhttp3.Request

class WebSettingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWebSettingBinding
    private lateinit var prefs: Prefs

    private var loadingDialog: Dialog? = null
    private var loadingBinding: LoadingDialogBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        enableEdgeToEdge()

        binding = ActivityWebSettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        prefs = Prefs(this)

        // Protocol dropdown
        val protocols = resources.getStringArray(R.array.protocol_options)
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            protocols
        )
        binding.protocolDropdown.setAdapter(adapter)

        // Load saved configuration
        val ipAddress = prefs.getIpAddress() ?: ""
        val port = prefs.getPortNumber() ?: ""
        val protocol = prefs.getProtocol() ?: "http"

        binding.ipAddressInput.setText(ipAddress)
        binding.portInput.setText(port)
        binding.protocolDropdown.setText(protocol, false)

        if (ipAddress.isNotBlank() && port.isNotBlank()) {

            val generatedUrl = "$protocol://$ipAddress:$port"

            binding.generatedUrlText.text = generatedUrl

            setVisibilityForFields(View.VISIBLE)

            showSuccessResult("Configuration loaded successfully.")

        } else {

            setVisibilityForFields(View.VISIBLE)

            showWarningResult("No saved server configuration found.")
        }

        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.copyUrlBtn.setOnClickListener {

            val generatedUrl = binding.generatedUrlText.text.toString().trim()

            if (generatedUrl.isNotEmpty()) {

                val clipboard =
                    getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

                val clip = ClipData.newPlainText(
                    "Generated URL",
                    generatedUrl
                )

                clipboard.setPrimaryClip(clip)

                showSuccessResult("URL copied to clipboard.")

            } else {

                showWarningResult("No URL available to copy.")
            }
        }

        binding.saveBtn.setOnClickListener {

            val ipAddress =
                binding.ipAddressInput.text.toString().trim()

            val portNumber =
                binding.portInput.text.toString().trim()

            val protocol =
                binding.protocolDropdown.text.toString().trim()

            if (ipAddress.isEmpty()) {

                binding.ipAddressInput.error = "Enter IP Address"
                binding.ipAddressInput.requestFocus()
                return@setOnClickListener
            }

            if (portNumber.isEmpty()) {

                binding.portInput.error = "Enter Port Number"
                binding.portInput.requestFocus()
                return@setOnClickListener
            }

            prefs.setIpAddressAndPort(
                protocol,
                ipAddress,
                portNumber
            )

            val generatedUrl = "$protocol://$ipAddress:$portNumber"

            binding.generatedUrlText.text = generatedUrl

            setVisibilityForFields(View.VISIBLE)

            showSuccessResult("Configuration saved successfully.")

            testConnection()
        }

        binding.testConnectionBtn.setOnClickListener {
            testConnection()
        }
    }

    private fun testConnection() {

        val generatedUrl =
            binding.generatedUrlText.text.toString().trim()

        if (generatedUrl.isEmpty()) {

            showWarningResult("No server URL available for testing.")
            return
        }

        showLoading("Testing server connection...\nPlease wait.")

        Thread {

            try {

                val client = OkHttpClient.Builder()
                    .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url(generatedUrl)
                    .get()
                    .build()

                val response = client.newCall(request).execute()

                runOnUiThread {

                    hideLoading()

                    if (response.isSuccessful) {

                        showSuccessResult(
                            "Connection successful.\nServer is reachable."
                        )

                    } else {

                        showErrorResult(
                            "Configuration saved, but the server returned HTTP ${response.code}."
                        )
                    }

                    response.close()
                }

            } catch (e: Exception) {

                runOnUiThread {

                    hideLoading()

                    showErrorResult(
                        "Unable to connect to the server.\n\n" +
                                "Please verify:\n" +
                                "• Protocol\n" +
                                "• IP Address\n" +
                                "• Port Number\n" +
                                "• Network Connection"
                    )
                }
            }

        }.start()
    }

    private fun setVisibilityForFields(visibility: Int) {
        binding.urlDisplayCard.visibility = visibility
        binding.testConnectionBtn.visibility = visibility
    }

    private fun showLoading(message: String = "Please wait...") {

        if (loadingDialog == null) {

            loadingBinding = LoadingDialogBinding.inflate(layoutInflater)

            loadingDialog = Dialog(this).apply {

                setCancelable(false)

                setContentView(loadingBinding!!.root)

                window?.setBackgroundDrawableResource(android.R.color.transparent)

                window?.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        }

        loadingBinding?.tvLoadingMessage?.text = message

        if (loadingDialog?.isShowing != true) {
            loadingDialog?.show()
        }
    }

    private fun hideLoading() {

        loadingDialog?.dismiss()
        loadingDialog = null
        loadingBinding = null
    }

    private fun showSuccessResult(message: String) {

        binding.resultCard.visibility = View.GONE

        binding.resultCard.setCardBackgroundColor(
            ContextCompat.getColor(this, R.color.success)
        )

        binding.resultText.text = message

        ResultFeedbackHelper.showSuccessResult(binding.root, message)
    }

    private fun showErrorResult(message: String) {

        binding.resultCard.visibility = View.GONE

        binding.resultCard.setCardBackgroundColor(
            ContextCompat.getColor(this, R.color.error)
        )

        binding.resultText.text = message

        ResultFeedbackHelper.showErrorResult(binding.root, message)

    }

    private fun showWarningResult(message: String) {

        binding.resultCard.visibility = View.GONE

        binding.resultCard.setCardBackgroundColor(
            ContextCompat.getColor(this, R.color.warning)
        )

        binding.resultText.text = message

        ResultFeedbackHelper.showWarningResult(binding.root, message)
    }

    override fun onDestroy() {
        super.onDestroy()
        hideLoading()
    }
}