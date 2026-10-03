package com.bartech.api_usage_boilerplate

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bartech.api_usage_boilerplate.databinding.ActivityMainMenuBinding
import com.bartech.api_usage_boilerplate.databinding.DialogLogoutBinding
import com.bartech.api_usage_boilerplate.databinding.LoadingDialogBinding

class MainMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainMenuBinding
    private var logoutBinding: DialogLogoutBinding? = null
    private var logoutDialog: Dialog? = null
    private var loadingBinding: LoadingDialogBinding? = null
    private var loadingDialog: Dialog? = null
    private var isUpdatingSettings = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainMenuBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        window.statusBarColor = getColor(R.color.background_primary)
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

        val message = intent.getStringExtra("snackBar_message")
        val success = intent.getBooleanExtra("snackBar_success", false)

        if (message != null) {
            showResult(message, success)
        }

        binding.btnSideBar.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.aboutUsLayout.setOnClickListener {
            closeDrawer()
            val intent = Intent(this, AboutUsActivity::class.java)
            startActivity(intent)
        }

        binding.viewProfileLayout.setOnClickListener {
            closeDrawer()
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }

        binding.btnLogoutLayout.setOnClickListener {
            showLogoutDialog()
        }

        setUpDrawerToggle()

    }

    private fun setUpDrawerToggle() {

        val prefs = Prefs(this)

        // Initialize toggles from saved session values
        initializeResultSettings(prefs)

        // Result Voice
        binding.voiceToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->

            if (isUpdatingSettings || !isChecked) return@addOnButtonCheckedListener

            when (checkedId) {

                R.id.voiceOnBtn -> {
                    prefs.setVoiceEffect(true)

                    // Voice ON → Buzzer OFF
                    prefs.setResultSoundSetting(false)

                    isUpdatingSettings = true
                    binding.buzzerToggleGroup.check(R.id.buzzerOffBtn)
                    isUpdatingSettings = false
                }

                R.id.voiceOffBtn -> {
                    prefs.setVoiceEffect(false)
                }
            }
        }

        // Buzzer
        binding.buzzerToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->

            if (isUpdatingSettings || !isChecked) return@addOnButtonCheckedListener

            when (checkedId) {

                R.id.buzzerOnBtn -> {
                    prefs.setResultSoundSetting(true)

                    // Buzzer ON → Voice OFF
                    prefs.setVoiceEffect(false)

                    isUpdatingSettings = true
                    binding.voiceToggleGroup.check(R.id.voiceOffBtn)
                    isUpdatingSettings = false
                }

                R.id.buzzerOffBtn -> {
                    prefs.setResultSoundSetting(false)
                }
            }
        }

        // Flash
        binding.flashToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->

            if (isUpdatingSettings || !isChecked) return@addOnButtonCheckedListener

            when (checkedId) {

                R.id.flashOnBtn -> {
                    prefs.setScreenFlashForResultSetting(true)
                }

                R.id.flashOffBtn -> {
                    prefs.setScreenFlashForResultSetting(false)
                }
            }
        }
    }

    private fun initializeResultSettings(prefs: Prefs) {

        isUpdatingSettings = true

        val voiceEnabled = prefs.getVoiceEffect()
        val buzzerEnabled = prefs.getResultSoundSetting()
        val flashEnabled = prefs.getScreenFlashForResultSetting()

        // Voice
        binding.voiceToggleGroup.check(
            if (voiceEnabled) {
                R.id.voiceOnBtn
            } else {
                R.id.voiceOffBtn
            }
        )

        // Buzzer
        binding.buzzerToggleGroup.check(
            if (buzzerEnabled) {
                R.id.buzzerOnBtn
            } else {
                R.id.buzzerOffBtn
            }
        )

        // Flash
        binding.flashToggleGroup.check(
            if (flashEnabled) {
                R.id.flashOnBtn
            } else {
                R.id.flashOffBtn
            }
        )

        isUpdatingSettings = false
    }

    private fun closeDrawer() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    private fun showLogoutDialog() {
        logoutBinding = DialogLogoutBinding.inflate(layoutInflater)

        logoutDialog = Dialog(this).apply {
            setContentView(logoutBinding!!.root)
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.92).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        with(logoutBinding!!) {
            btnCancel.setOnClickListener { logoutDialog?.dismiss() }
            btnLogout.setOnClickListener { performLogout() }
        }

        logoutDialog?.show()
    }

    private fun performLogout() {
        showLoading("Logging out...")
        Prefs(this).clearToken()

        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        intent.putExtra("snackBar_message", "Logout successful.")
        intent.putExtra("snackBar_success", true)
        startActivity(intent)
        finish()
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
    }



    private fun showResult(message: String, success: Boolean) {
        if (success) {
            ResultFeedbackHelper.showSuccessResult(binding.root, message)
        } else {
            ResultFeedbackHelper.showErrorResult(binding.root, message)
        }
    }

}