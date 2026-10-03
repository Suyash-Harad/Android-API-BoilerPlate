package com.bartech.api_usage_boilerplate

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlin.apply

data class savedPrinterPSClass(
    @SerializedName("module_name")
    val moduleName: String,
    @SerializedName("printer_name")
    val printerName: String,
    @SerializedName("printer_ip")
    val printerIp: String,
    @SerializedName("dpi")
    val printerDpi: String
)

class Prefs(context: Context) {

    companion object {
        private const val USER_ID = "user_id"
        private const val PROTOCOL = "protocol"
        private const val IP_ADDRESS = "ip_address"
        private const val PORT_NUMBER = "port_number"
        private const val COMPANY_CODE = "company_code"
        private const val PLANT_CODE = "plant_code"
        private const val WAREHOUSE_CODE = "warehouse_code"
        private const val LINE_CODE = "line_code"
        private const val USER_ROLE = "user_role"
        private const val USER_NAME = "user_name"
        private const val HHT_MENU_ACCESS = "hht_menu_access"
        private const val TOKEN = "token"
        private const val FG_STOCK_TAKE_NO = "fg_stock_take_no"
        private const val RM_STOCK_TAKE_NO = "rm_stock_take_no"
        private const val FG_SAVED_PRINTER_SETTINGS = "fg_saved_printer_settings"
        private const val RM_SAVED_PRINTER_SETTINGS = "rm_saved_printer_settings"
        private const val SCREEN_FLASH_FOR_RESULT = "screen_flash_for_result"
        private const val RESULT_SOUND_EFFECT = "result_sound_effect"
        private const val RESULT_VOICE_EFFECT = "result_voice_effect"
        private const val REMEMBER_ME_USERID = "remember_me_userid"
        private const val REMEMBER_ME_PASSWORD = "remember_me_password"
        private const val APK_FILE_NAME = "apk_file_name"
    }

    private val preferences: SharedPreferences = context.getSharedPreferences("Api_Usage_Boilerplate_Prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun setScreenFlashForResultSetting(settingValue: Boolean) { //on or off
        preferences.edit().apply {
            putBoolean(SCREEN_FLASH_FOR_RESULT, settingValue)
            apply()
        }
    }

    fun setResultSoundSetting(settingValue: Boolean) { //on or off
        preferences.edit().apply {
            putBoolean(RESULT_SOUND_EFFECT, settingValue)
            apply()
        }
    }

    fun getScreenFlashForResultSetting(): Boolean {
        return preferences.getBoolean(SCREEN_FLASH_FOR_RESULT, false)
    }

    fun getResultSoundSetting(): Boolean {
        return preferences.getBoolean(RESULT_SOUND_EFFECT, false)
    }

    fun setVoiceEffect(voiceEffect: Boolean) {
        preferences.edit().apply {
            putBoolean(RESULT_VOICE_EFFECT, voiceEffect)
            apply()
        }
    }

    fun getVoiceEffect(): Boolean {
        return preferences.getBoolean(RESULT_VOICE_EFFECT, false)
    }

    fun setIpAddressAndPort(protocol: String, ipAddress: String, port: String) {
        preferences.edit().apply {
            putString(PROTOCOL, protocol)
            putString(IP_ADDRESS, ipAddress)
            putString(PORT_NUMBER, port)
            apply()
        }
    }

    fun saveFgPrinterSettingsList(settingsList: List<savedPrinterPSClass>) {
        val json = gson.toJson(settingsList)
        preferences.edit().apply {
            putString(FG_SAVED_PRINTER_SETTINGS, json)
            apply()
        }
    }

    fun saveRmPrinterSettingsList(settingsList: List<savedPrinterPSClass>) {
        val json = gson.toJson(settingsList)
        preferences.edit().apply {
            putString(RM_SAVED_PRINTER_SETTINGS, json)
            apply()
        }
    }

    fun getFgPrinterSettingsList(): List<savedPrinterPSClass> {
        val json = preferences.getString(FG_SAVED_PRINTER_SETTINGS, null)
        return if (json != null) {
            val type = object : TypeToken<List<savedPrinterPSClass>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } else {
            emptyList()
        }
    }

    fun getRmPrinterSettingsList(): List<savedPrinterPSClass> {
        val json = preferences.getString(RM_SAVED_PRINTER_SETTINGS, null)
        return if (json != null) {
            val type = object : TypeToken<List<savedPrinterPSClass>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } else {
            emptyList()
        }
    }

    fun getFgPrinterIpByModule(moduleName: String): String? {
        val savedSettings = getFgPrinterSettingsList()

        // Find the first entry whose moduleName matches the input
        val setting = savedSettings.find { it.moduleName == moduleName }

        // Return the printerIp if found, otherwise return null
        return setting?.printerIp
    }

    fun getRmPrinterIpByModule(moduleName: String): String? {
        val savedSettings = getRmPrinterSettingsList()

        // Find the first entry whose moduleName matches the input
        val setting = savedSettings.find { it.moduleName == moduleName }

        // Return the printerIp if found, otherwise return null
        return setting?.printerIp
    }

    fun getIpAddress(): String? {
        return preferences.getString(IP_ADDRESS, null)
    }

    fun getPortNumber(): String? {
        return preferences.getString(PORT_NUMBER, null)
    }

    fun getProtocol(): String? {
        return preferences.getString(PROTOCOL, null)
    }

    fun saveLoginData(token: String?, userId: String?, userRole: String?, userName: String?, hhtAccessMenu: String?, companyCode: String?, plantCode: String?, warehouseCode: String?, lineCode: String?) {
        preferences.edit().apply {
            putString(TOKEN, token)
            putString(USER_ID, userId)
            putString(USER_ROLE, userRole)
            putString(USER_NAME, userName)
            putString(HHT_MENU_ACCESS, hhtAccessMenu)
            putString(COMPANY_CODE, companyCode)
            putString(PLANT_CODE, plantCode)
            putString(WAREHOUSE_CODE, warehouseCode)
            putString(LINE_CODE, lineCode)
            apply()
        }
    }

    fun getUserName(): String? {
        return preferences.getString(USER_NAME, null)
    }

    fun getUserRole(): String? {
        return preferences.getString(USER_ROLE, null)
    }

    fun getUserId(): String? {
        return preferences.getString(USER_ID, null)
    }

    fun getToken(): String? {
        return preferences.getString(TOKEN, null)
    }

    fun getCompanyCode(): String? {
        return preferences.getString(COMPANY_CODE, null)
    }

    fun getPlantCode(): String? {
        return preferences.getString(PLANT_CODE, null)
    }

    fun getWarehouseCode(): String? {
        return preferences.getString(WAREHOUSE_CODE, null)
    }

    fun getLineCode(): String? {
        return preferences.getString(LINE_CODE, null)
    }

    fun getHHTMenuAccess(): String? {
        return preferences.getString(HHT_MENU_ACCESS, null)
    }

    fun setFGStockTakeNo(stockTakeNo: String) {
        preferences.edit().apply {
            putString(FG_STOCK_TAKE_NO, stockTakeNo)
            apply()
        }
    }

    fun getFGStockTakeNo(): String? {
        return preferences.getString(FG_STOCK_TAKE_NO, null)
    }

    fun setRMStockTakeNo(stockTakeNo: String) {
        preferences.edit().apply {
            putString(RM_STOCK_TAKE_NO, stockTakeNo)
            apply()
        }
    }

    fun getRMStockTakeNo(): String? {
        return preferences.getString(RM_STOCK_TAKE_NO, null)
    }

    fun setAPKFileName(fileName: String) {
        preferences.edit().apply {
            putString(APK_FILE_NAME, fileName)
            apply()
        }
    }

    fun getAPKFileName(): String? {
        return preferences.getString(APK_FILE_NAME, null)
    }


    fun clearToken() {
        preferences.edit().apply {
            remove(TOKEN)
            remove(USER_ID)
            remove(COMPANY_CODE)
            remove(PLANT_CODE)
            remove(WAREHOUSE_CODE)
            remove(LINE_CODE)
            remove(USER_ROLE)
            remove(USER_NAME)
            remove(HHT_MENU_ACCESS)
            apply()
        }
    }

    fun setRememberMe(userId: String, password: String) {
        preferences.edit().apply {
            putString(REMEMBER_ME_USERID, userId)
            putString(REMEMBER_ME_PASSWORD, password)
            apply()
        }
    }

    fun getRememberMeUserId(): String? {
        return preferences.getString(REMEMBER_ME_USERID, null)
    }

    fun getRememberMePassword(): String? {
        return preferences.getString(REMEMBER_ME_PASSWORD, null)
    }

    fun clearRememberMe() {
        preferences.edit().apply {
            remove(REMEMBER_ME_USERID)
            remove(REMEMBER_ME_PASSWORD)
            apply()
        }
    }

}