package com.bartech.api_usage_boilerplate

import android.annotation.SuppressLint
import android.app.Dialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.auth0.android.jwt.JWT
import com.bartech.api_usage_boilerplate.databinding.ActivityLoginBinding
import com.bartech.api_usage_boilerplate.databinding.LoadingDialogBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

data class LoginResponse(
    @SerializedName("Status")
    val status: String,
    @SerializedName("Message")
    val message: String,
    @SerializedName("token")
    val token: String?,
    @SerializedName("is_change_password")
    val isChangePassword: Boolean?
)

data class UpdatedVersionDetails(
    @SerializedName("Status")
    val status: String,
    @SerializedName("Message")
    val message: String,
    @SerializedName("hht_version_code")
    val versionCode: String?,
    @SerializedName("hht_version_name")
    val versionName: String?,
    @SerializedName("hht_download_link")
    val link: String?,
    @SerializedName("hht_apk_name")
    val newApkName: String?
)

data class UserPayload(
    @SerializedName("user_id") val userId: String,
    @SerializedName("user_name") val userName: String,
    @SerializedName("user_status") val userStatus: String,
    @SerializedName("user_role") val userRole: String,
    @SerializedName("name") val name: String,
    @SerializedName("hht_menu_access") val hhtMenuAccess: String,
    @SerializedName("company_code") val companyCode: String?,
    @SerializedName("plant_code") val plantCode: String?,
    @SerializedName("warehouse_code") val warehouseCode: String?,
    @SerializedName("line_code") val lineCode: String?
)

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    // Correctly initialize dependencies with lazy delegation
    private val prefs by lazy { Prefs(this) }
    private val apiManager by lazy { ApiManager(this, prefs) }
    private val gson by lazy { Gson() }

    private var versionCode: String = ""
    private var versionName: String = ""
    private var dbVersionCode: String = "3"
    private var dbVersionName: String = "1.0.3"

    private var apkUrl: String = "http://192.168.29.33:5000/"

    private var newApkName: String = "android_api_usage_bolierplate_v1.0.2"

    private var loadingDialog: Dialog? = null
    private var loadingBinding: LoadingDialogBinding? = null

    private var updateRequired = false

    private var downloadId: Long = -1L

    private val downloadReceiver =
        object : BroadcastReceiver() {

            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {

                val id =
                    intent?.getLongExtra(
                        DownloadManager.EXTRA_DOWNLOAD_ID,
                        -1L
                    ) ?: return

                if (id == downloadId) {

                    hideLoading()

                    installDownloadedApk()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        registerReceiver(
            downloadReceiver,
            IntentFilter(
                DownloadManager.ACTION_DOWNLOAD_COMPLETE
            ),
            RECEIVER_NOT_EXPORTED
        )

        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            versionName = packageInfo.versionName ?: ""
            binding.versionNameTextView.text = "Version: $versionName"
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                versionCode = packageInfo.longVersionCode.toString()
            } else {
                versionCode = packageInfo.versionCode.toLong().toString()
            }
        } catch (e: Exception) {
            0L
        }

        if (prefs.getIpAddress().isNullOrEmpty() || prefs.getPortNumber().isNullOrEmpty()) {
            showResult("Please do the Network Configuration first.", false)
        }

        if (
            versionCode.isNotEmpty() && versionName.isNotEmpty() &&
            dbVersionCode.isNotEmpty() && dbVersionName.isNotEmpty() &&
            versionCode != dbVersionCode && versionName != dbVersionName
        ) {
            updateRequired = true
            showUpdateDialog(dbVersionName, apkUrl, newApkName)
        } else {
            updateRequired = false
        }

//        fetchUpdatedVersionCode()

        if (prefs.getRememberMeUserId().isNullOrEmpty() || prefs.getRememberMePassword().isNullOrEmpty()) {
            binding.userIdInput.setText("")
            binding.passwordInput.setText("")
            binding.rememberMeCheckbox.isChecked = false
        } else {
            binding.userIdInput.setText(prefs.getRememberMeUserId())
            binding.passwordInput.setText(prefs.getRememberMePassword())
            binding.rememberMeCheckbox.isChecked = true
        }

        val message = intent.getStringExtra("snackBar_message")
        val success = intent.getBooleanExtra("snackBar_success", false)

        if (message != null) {
            showResult(message, success)
        }

        binding.settingsBtn.setOnClickListener {
            val intent = Intent(this, WebSettingActivity::class.java)
            startActivity(intent)
        }

        binding.loginBtn.setOnClickListener {
//            checkCredentials()

            val userId = binding.userIdInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()

            // Clear previous errors
            binding.userIdLayout.error = null
            binding.passwordLayout.error = null

            if (userId.isEmpty()) {
                binding.userIdLayout.error = "Please enter username."
                ensureFocusAndHideKeyboard(binding.userIdInput)
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                binding.passwordLayout.error = "Please enter password."
                ensureFocusAndHideKeyboard(binding.passwordInput)
                return@setOnClickListener
            }

            if (updateRequired) {
                showResult("Update Required", false)
                showUpdateDialog(dbVersionName, apkUrl, newApkName)
                return@setOnClickListener
            }

            prefs.saveLoginData("token", "admin", "admin", "Admin",
                "1_01,1_02,2_01,2_02,3_01,3_02", "BDSPL", "TNA-01",
                "WH-01", "LN-01")

            if (binding.rememberMeCheckbox.isChecked) {
                prefs.setRememberMe(
                    binding.userIdInput.text.toString(),
                    binding.passwordInput.text.toString()
                )
            } else {
                prefs.clearRememberMe()
            }

            val intent = Intent(this, MainMenuActivity::class.java)
            intent.putExtra("snackBar_message", "Login successful. Welcome $userId")
            intent.putExtra("snackBar_success", true)

            startActivity(intent)
            finish()
        }

    }

    private fun fetchUpdatedVersionCode() {
        updateRequired = false

        if (Prefs(this).getIpAddress().isNullOrEmpty() || Prefs(this).getPortNumber().isNullOrEmpty()) {
            showResult("Please do the Network Configuration first.", false)
            return
        }

        val endpoint = "api/apk/fetch-version-code"

//        val token = Prefs(this).getToken() ?: ""

//        if (token.isEmpty()){
//            showResult("Token is empty.", false)
//            return
//        }

        val jsonObject = JsonObject().apply {}

        lifecycleScope.launch {
            showLoading("Checking for updates...")

            try {
                // callApi is a suspend fun and already hops to Dispatchers.IO internally,
                // so we can call it directly from the Main-dispatched scope and update
                // UI right after it returns.
                val response = apiManager.callApi(endpoint, "POST", jsonObject.toString())
                val responseBody = response.body()?.string()

                hideLoading()
                if (response.isSuccessful && responseBody != null) {
                    handleFetchUpdatedVersionCodeResponse(responseBody)
                } else {
                    val errorMessage = response.message()
                    showResult("Error ${response.code()}: $errorMessage", false)
                }
            } catch (e: IOException) {
                hideLoading()
                showResult("Network Error: ${e.message}", false)
            } catch (e: Exception) {
                hideLoading()
                showResult("Configuration Error: ${e.message}", false)
            }
        }
    }

    private fun handleFetchUpdatedVersionCodeResponse(responseBody: String) {
        val apiResponse = gson.fromJson(responseBody, UpdatedVersionDetails::class.java)

        // Handle the response based on the status field
        when (apiResponse.status) {
            "T" -> {
                try {
                    dbVersionCode = apiResponse.versionCode ?: ""
                    dbVersionName = apiResponse.versionName ?: ""
                    newApkName = apiResponse.newApkName ?: ""
                    apkUrl = apiResponse.link ?: ""

                    if (
                        versionCode.isNotEmpty() && versionName.isNotEmpty() &&
                        dbVersionCode.isNotEmpty() && dbVersionName.isNotEmpty() &&
                        versionCode != dbVersionCode && versionName != dbVersionName
                    ) {
                        updateRequired = true
                        showUpdateDialog(dbVersionName, apkUrl, newApkName)
                    } else {
                        updateRequired = false
                    }

                } catch (e: Exception) {
                    showResult("Failed to parse details.", false)
                }
            }
            "F" -> {
//                showResult(apiResponse.message, false)
            }
            else -> {
                showResult("Unknown status: ${apiResponse.status}", false)
            }
        }
    }

    private fun showUpdateDialog(latestVersion: String, apkUrl: String, newApkName: String) {

        MaterialAlertDialogBuilder(this)
            .setTitle("Update Required")
            .setMessage(
                """
        A newer version of the application is available.

        Current Version : $versionName
        Latest Version : $latestVersion

        You must update the application before continuing.
        """.trimIndent()
            )
            .setCancelable(true)
            .setNegativeButton("Not Now") { _, _ ->
            }
            .setPositiveButton("Update Now") { _, _ ->
                downloadAndInstallApk(apkUrl, newApkName)
            }
            .show()
    }

    private fun downloadAndInstallApk(apkUrl: String, newApkName: String) {

        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!packageManager.canRequestPackageInstalls()) {
                    startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:$packageName")
                        )
                    )
                    showResult("Please allow install from unknown apps.", false)
                    return
                }
            }

            showLoading("Preparing update...")

            lifecycleScope.launch(Dispatchers.IO) {

                deleteOtherApkFiles(newApkName)

                withContext(Dispatchers.Main) {

                    showLoading("Downloading update...")

                    val request = DownloadManager.Request(Uri.parse(apkUrl))
                        .setTitle("Application Update")
                        .setDescription("Downloading latest version")
                        .setNotificationVisibility(
                            DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                        )
                        .setDestinationInExternalPublicDir(
                            Environment.DIRECTORY_DOWNLOADS,
                            "Updated Application/$newApkName"
                        )

                    val downloadManager =
                        getSystemService(DOWNLOAD_SERVICE) as DownloadManager

                    downloadId = downloadManager.enqueue(request)

                    prefs.setAPKFileName(newApkName)
                }
            }

        } catch (e: Exception) {
            hideLoading()
            showResult(e.message ?: "Something went wrong.", false)
        }
    }

    private fun deleteOtherApkFiles(currentApkName: String) {

        try {

            val updateFolder = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "Updated Application"
            )

            if (!updateFolder.exists()) {
                updateFolder.mkdirs()
                return
            }

            updateFolder.listFiles()?.forEach { file ->

                if (
                    file.isFile &&
                    file.extension.equals("apk", true) &&
                    file.name != currentApkName
                ) {

                    val deleted = file.delete()

                    Log.d(
                        "APK_CLEANUP",
                        "${file.name} deleted = $deleted"
                    )
                }
            }

        } catch (e: Exception) {
            Log.e("APK_CLEANUP", "Failed to delete old APKs", e)
        }
    }

    private fun installDownloadedApk() {
        try {
            val fileName = prefs.getAPKFileName() ?: "update.apk"
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val apkFile = File(downloadsDir, "Updated Application/$fileName")

            if (!apkFile.exists()) {
                showResult("APK file not found at expected location.", false)
                return
            }

            val apkUri = FileProvider.getUriForFile(this, "$packageName.provider", apkFile)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Explicitly grant permission to all apps that can handle this intent,
            // not just relying on the flag — this covers OEM package installer quirks
            val resolvedActivities = packageManager.queryIntentActivities(installIntent, PackageManager.MATCH_DEFAULT_ONLY)
            for (resolvedActivity in resolvedActivities) {
                val packageName = resolvedActivity.activityInfo.packageName
                grantUriPermission(packageName, apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(installIntent)

        } catch (e: Exception) {
            showResult(e.message ?: "Something went wrong.", false)
        }
    }

    private fun checkCredentials() {
        val userId = binding.userIdInput.text.toString().trim()
        val password = binding.passwordInput.text.toString().trim()
        val deviceSN = getDeviceSerialNumber().trim()

        Log.d("Calling Login api", "checkCredentials:")

        if (prefs.getIpAddress().isNullOrEmpty() || prefs.getPortNumber().isNullOrEmpty()) {
            showResult("Please do the Network Configuration first.", false)
            return
        }

        if (updateRequired) {
            showResult("Update Required", false)
            showUpdateDialog(dbVersionName, apkUrl, newApkName)
            return
        }

        // Clear previous errors
        binding.userIdLayout.error = null
        binding.passwordLayout.error = null

        if (userId.isEmpty()) {
            binding.userIdLayout.error = "Please enter username."
            ensureFocusAndHideKeyboard(binding.userIdInput)
            return
        }

        if (password.isEmpty()) {
            binding.passwordLayout.error = "Please enter password."
            ensureFocusAndHideKeyboard(binding.passwordInput)
            return
        }

        val endpoint = "api/auth/check-credentials"

        val jsonObject = JsonObject().apply {
            addProperty("User_ID", userId)
            addProperty("User_Password", password)
            addProperty("DeviceSN", deviceSN)
            addProperty("ApplicationType", "HHT")
        }

        lifecycleScope.launch {

            showLoading("Validating credentials...")

            try {
                // callApi is a suspend fun and already hops to Dispatchers.IO internally,
                // so we're back on Main as soon as it returns and can update UI directly.
                val response = apiManager.callApi(
                    endpoint,
                    "POST",
                    jsonObject.toString()
                )

                showLoading("Processing response...")

                val responseBody = response.body()?.string()

                hideLoading()

                if (response.isSuccessful && responseBody != null) {
                    handleSuccessfulResponse(responseBody)
                } else {
                    val errorMessage = response.message()
                    showResult("Error ${response.code()}: $errorMessage", false)
                }

            } catch (e: IOException) {

                hideLoading()
                showResult("Network Error: ${e.message}", false)

            } catch (e: Exception) {

                hideLoading()
                showResult("Configuration Error: ${e.message}", false)
            }
        }
    }

    private fun handleSuccessfulResponse(responseBody: String) {
        val loginResponse = gson.fromJson(responseBody, LoginResponse::class.java)
        Log.d("Response Body", responseBody)

        when (loginResponse.status) {
            "T" -> {
                loginResponse.token?.let { token ->
                    val userPayload = decodeJwt(token)
                    if (userPayload != null) {
                        // Correctly calling saveLoginData on the prefs instance
                        prefs.saveLoginData(token, userPayload.userId, userPayload.userRole, userPayload.userName,
                            userPayload.hhtMenuAccess, userPayload.companyCode, userPayload.plantCode,
                            userPayload.warehouseCode, userPayload.lineCode)
                        if (loginResponse.isChangePassword == true) {
                            val message = loginResponse.message ?: "Login successful. Please change password for first time login."
                            showResult(message, true)
                            startActivity(Intent(this, ChangePasswordActivity::class.java))
                            intent.putExtra("snackBar_message", "Login successful. Please change password for first time login.")
                            intent.putExtra("snackBar_success", true)
                        } else {
                            val message = loginResponse.message ?: "Login successful. Welcome ${userPayload.userId}"
                            if (binding.rememberMeCheckbox.isChecked) {
                                prefs.setRememberMe(
                                    binding.userIdInput.text.toString(),
                                    binding.passwordInput.text.toString()
                                )
                            } else {
                                prefs.clearRememberMe()
                            }
                            showResult(message, true)
                            val intent = Intent(this, MainMenuActivity::class.java)
                            intent.putExtra("snackBar_message", "Login successful. Welcome ${userPayload.userId}")
                            intent.putExtra("snackBar_success", true)
                            startActivity(intent)
                            finish()
                        }
                    }
                }
            }
            "F" -> {
                val message = loginResponse.message ?: "Login failed."
                showResult(message, false)
            }
            "G" -> {
                val message = loginResponse.message ?: "Device is not registered or not being approved, contact administrator."
                ResultFeedbackHelper.showWarningResult(binding.root, message)
                startActivity(Intent(this, UserDeviceRegisterActivity::class.java))
                intent.putExtra("snackBar_message", "Device is not registered or not being approved, contact administrator.")
            }
            else -> {
                showResult("Unknown status: ${loginResponse.status}", false)
            }
        }
    }

    private fun decodeJwt(token: String): UserPayload? {
        return try {
            val jwt = JWT(token)
            val userClaim = jwt.getClaim("user")
            // The user claim is a JsonObject. Convert it to a String first, then use Gson.
            val userClaimJson = userClaim.asObject(JsonObject::class.java).toString()
            gson.fromJson(userClaimJson, UserPayload::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @SuppressLint("HardwareIds")
    private fun getDeviceSerialNumber(): String {
        return try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    // Android 10+ (no access to real serial for normal apps)
                    Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    try {
                        Build.getSerial() // requires READ_PHONE_STATE permission
                    } catch (e: SecurityException) {
                        Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                    }
                }
                else -> {
                    @Suppress("DEPRECATION")
                    Build.SERIAL
                }
            }
        } catch (e: Exception) {
            Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        }
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

    private fun ensureFocusAndHideKeyboard(targetField: TextInputEditText) {
        // Launch a coroutine tied to the activity's lifecycle
        lifecycleScope.launch(Dispatchers.Main) {
            // Delay ensures the view hierarchy is stable and rendering is complete
            delay(50)

            // 1. Request focus
            targetField.requestFocus()

            targetField.text?.let {
                targetField.setSelection(it.length)
            }

            // 2. Control the keyboard
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

            // Use the token of the target field for reliable hiding
            targetField.windowToken?.let { token ->
                imm.hideSoftInputFromWindow(token, 0)
            }
            // Fallback using the current focus token if the target field isn't yet active
                ?: currentFocus?.windowToken?.let { token ->
                    imm.hideSoftInputFromWindow(token, 0)
                }
        }
    }

}