package com.bartech.api_usage_boilerplate

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bartech.api_usage_boilerplate.databinding.ActivityProfileBinding
import com.bartech.api_usage_boilerplate.databinding.DialogLogoutBinding
import com.bartech.api_usage_boilerplate.databinding.LoadingDialogBinding

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    private var logoutBinding: DialogLogoutBinding? = null
    private var logoutDialog: Dialog? = null
    private var loadingBinding: LoadingDialogBinding? = null
    private var loadingDialog: Dialog? = null

    private var userName: String? = ""
    private var userId: String? = ""
    private var userRole: String? = ""
    private var userPlantCode: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        userName = Prefs(this).getUserName() ?: ""
        userId = Prefs(this).getUserId() ?: ""
        userRole = Prefs(this).getUserRole() ?: ""
        userPlantCode = Prefs(this).getPlantCode() ?: ""

        binding.userNameValue.text = userName
        binding.userIdValue.text = userId
        binding.userRoleValue.text = userRole
        binding.plantCodeValue.text = userPlantCode

        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
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

}