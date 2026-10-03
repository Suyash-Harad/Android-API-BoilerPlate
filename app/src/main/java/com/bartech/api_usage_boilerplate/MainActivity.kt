package com.bartech.api_usage_boilerplate

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TableRow
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bartech.api_usage_boilerplate.databinding.ActivityMainBinding
import com.bartech.api_usage_boilerplate.databinding.LoadingDialogBinding
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException

data class scannedCodeSubmitResultClass(
    @SerializedName("Status")
    val status: String?,
    @SerializedName("Message")
    val message: String?,
    @SerializedName("data")
    val data: String?
)

data class scannedMaterialDetailsClass(
    @SerializedName("Status")
    val status: String?,
    @SerializedName("Message")
    val message: String?,
    @SerializedName("item_code")
    val itemCode: String?,
    @SerializedName("item_description")
    val itemDescription: String?,
    @SerializedName("qty")
    val printQuantity: String?,
    @SerializedName("batch_qty")
    val batchQuantity: String?,
    @SerializedName("serial_no")
    val serialNo: String?
)

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val tableData = mutableListOf<scannedMaterialDetailsClass>()

    private var currentPage = 0
    private var rowsPerPage = 10

    private val apiManager by lazy { ApiManager(this, Prefs(this)) }
    private val gson by lazy { Gson() }

    private var userId: String? = ""

    private var loadingDialog: Dialog? = null
    private var loadingBinding: LoadingDialogBinding? = null

    private val scannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val scannedCode = result.data?.getStringExtra("scanned_barcode")
            scannedCode?.let {
                binding.scannedMaterialCodeInput.setText("") // Set the blank text in the input field
                binding.scannedResultTextView.text = it // Set the scanned code in the TextView below
                if (binding.removeCheckbox.isChecked) {
                    // Logic for removing an item from the table
                    removeScannedItem(scannedCode)
                }
                else {
                    // Call the function (api) related to scanned code here
                    validateScannedMaterial(scannedCode)
                }
                binding.scannedMaterialCodeInput.setText("")
                ensureFocusAndHideKeyboard(binding.scannedMaterialCodeInput)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        userId = Prefs(this).getUserId() ?: ""

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnRefresh.setOnClickListener {
            refreshData()
        }

        binding.btnClear.setOnClickListener {
            clearPage()
        }

        binding.btnSubmit.setOnClickListener {
            submitValidMaterial()
        }

        binding.scannedMaterialCodeInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                (event != null && event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)
            ) {
                val scannedCode = binding.scannedMaterialCodeInput.text.toString()
                if (scannedCode.isNotEmpty()) {
                    binding.scannedMaterialCodeInput.setText("") // Set the blank text in the input field
                    binding.scannedResultTextView.text = scannedCode // Set the scanned code in the TextView below
                    if (binding.removeCheckbox.isChecked) {
                        // Logic for removing an item from the table
                        removeScannedItem(scannedCode)
                    }
                    else {
                        // Call the function (api) related to scanned code here
                        validateScannedMaterial(scannedCode)
                    }
                    binding.scannedMaterialCodeInput.setText("")
                    ensureFocusAndHideKeyboard(binding.scannedMaterialCodeInput)
                }
                // Return true to consume the event and prevent the keyboard from staying open
                true
            } else {
                false
            }
        }

        binding.scannerMaterialLayout.setEndIconOnClickListener {
            val intent = Intent(this, CustomScannerActivity::class.java)
            scannerLauncher.launch(intent)
        }

        binding.scannedMaterialCodeInput.setText("")
        ensureFocusAndHideKeyboard(binding.scannedMaterialCodeInput)

        setupPagination()

    }

    private fun refreshData () {
        binding.resultCard.visibility = View.GONE
        hideAndClearDetailsCard()
        binding.removeCheckbox.isChecked = false
        tableData.clear()
        updateTableButtonUIState()
        refreshTableUI()
        binding.scannedMaterialCodeInput.setText("")
        ensureFocusAndHideKeyboard(binding.scannedMaterialCodeInput)
        binding.scannedResultTextView.text = "Scanned code will appear here"
    }

    private fun clearPage () {
        binding.resultCard.visibility = View.GONE
        hideAndClearDetailsCard()
        binding.removeCheckbox.isChecked = false
        tableData.clear()
        updateTableButtonUIState()
        refreshTableUI()
        binding.scannedMaterialCodeInput.setText("")
        ensureFocusAndHideKeyboard(binding.scannedMaterialCodeInput)
        binding.scannedResultTextView.text = "Scanned code will appear here"
    }

    private fun removeScannedItem(serialNo: String) {
        val initialSize = tableData.size
        tableData.removeAll { it.serialNo == serialNo }
        if (tableData.size < initialSize) {
            showSuccessResult("Material serial no. $serialNo removed successfully.")
        } else {
            showErrorResult("Material serial no. $serialNo not found.")
        }
        hideAndClearDetailsCard()
        updateTableButtonUIState()
        binding.scannedMaterialCodeInput.setText("")
        ensureFocusAndHideKeyboard(binding.scannedMaterialCodeInput)
    }

    private fun validateScannedMaterial(scannedCode: String) {
        binding.scannedMaterialCodeInput.setText("")
        ensureFocusAndHideKeyboard(binding.scannedMaterialCodeInput)

        if (Prefs(this).getIpAddress().isNullOrEmpty() || Prefs(this).getPortNumber().isNullOrEmpty()) {
            showErrorResult("Please do the Network Configuration first.")
            return
        }

        if (tableData.any() {it.serialNo == scannedCode}) {
            showErrorResult("Material of SerialNo: $scannedCode is already scanned.")
            hideAndClearDetailsCard()
            updateTableButtonUIState()
            return
        }

        val endpoint = "api/material/scanned-material-validation"

        val token = Prefs(this).getToken() ?: ""

        if (token.isEmpty()){
            showErrorResult("Token is empty.")
            return
        }

        val jsonObject = JsonObject().apply {
            addProperty("serial_no", scannedCode)
        }

        lifecycleScope.launch {
            showLoading("Validating Material...")

            try {
                // callApi is a suspend fun and already hops to Dispatchers.IO internally,
                // so we're back on Main as soon as it returns and can update UI directly.
                val response = apiManager.callApi(endpoint, "POST", jsonObject.toString(), token)
                val responseBody = response.body()?.string()

                hideLoading()
                if (response.isSuccessful && responseBody != null) {
                    handleScannedCodeValidationSuccessfulResponse(responseBody)
                } else {
                    val errorMessage = response.message()
                    if (response.code() != 440 && response.code() != 401) {
                        showErrorResult("Error ${response.code()}: $errorMessage")
                    } else {
                        showErrorResult(errorMessage)
                        hideAndClearDetailsCard()
                        updateTableButtonUIState()
                    }
                }
            } catch (e: IOException) {
                hideLoading()
                showErrorResult("Network Error: ${e.message}")
                hideAndClearDetailsCard()
                updateTableButtonUIState()
            } catch (e: Exception) {
                hideLoading()
                showErrorResult("Configuration Error: ${e.message}")
                hideAndClearDetailsCard()
                updateTableButtonUIState()
            }
        }
    }

    private fun handleScannedCodeValidationSuccessfulResponse(responseBody: String) {
        try {
            val jsonElement = JsonParser.parseString(responseBody)
            when {
                // =========================================================
                // CASE 1: JSON OBJECT
                // =========================================================
                jsonElement.isJsonObject -> {
                    val jsonObject = jsonElement.asJsonObject
                    // -----------------------------------------------------
                    // CASE 3: JSON OBJECT + DATA
                    // -----------------------------------------------------
                    if (jsonObject.has("data")) {
                        val submitResponse = gson.fromJson(jsonObject,scannedCodeSubmitResultClass::class.java)

                        when (submitResponse.status) {
                            "T" -> {
                                showLoading("Loading material details...")
                                val dataString = submitResponse.data
                                if (dataString.isNullOrBlank()) {
                                    showWarningResult(submitResponse.message ?: "No details found.")
                                    hideAndClearDetailsCard()
                                } else {
                                    try {
                                        val dataElement = JsonParser.parseString(dataString)

                                        if (!dataElement.isJsonArray) {
                                            showErrorResult("Invalid data format received.")
                                            hideAndClearDetailsCard()
                                        } else {
                                            val dataArray = dataElement.asJsonArray

                                            if (dataArray.size() == 0) {
                                                showWarningResult(submitResponse.message ?: "No details found.")
                                                hideAndClearDetailsCard()
                                            } else {
                                                showSuccessResult(submitResponse.message ?: "Valid Serial No. Scanned.")
                                                for (item in dataArray) {
                                                    val materialDetails = gson.fromJson(item, scannedMaterialDetailsClass::class.java)
                                                    tableData.add(materialDetails)
                                                }
                                            }
                                        }

                                    } catch (e: Exception) {
                                        Log.e("SCAN_RESPONSE", "Error parsing data array: ${e.message}", e)
                                        showErrorResult("Failed to parse material details.")
                                        hideAndClearDetailsCard()
                                    }
                                }
                            }

                            "F" -> {
                                showErrorResult(submitResponse.message ?: "Invalid Serial No. Scanned.")
                                hideAndClearDetailsCard()
                            }

                            else -> {
                                showErrorResult("Unknown status: ${submitResponse.status}")
                                hideAndClearDetailsCard()
                            }
                        }

                    } else {
                        // -------------------------------------------------
                        // CASE 1: NORMAL JSON OBJECT
                        // -------------------------------------------------
                        val validationResponse = gson.fromJson(jsonObject, scannedMaterialDetailsClass::class.java)
                        when (validationResponse.status) {
                            "T" -> {
                                showLoading("Loading material details...")
                                showSuccessResult(validationResponse.message ?: "Valid Serial No. Scanned.")
                                tableData.add(validationResponse)
                            }

                            "F" -> {
                                showErrorResult(validationResponse.message ?: "Invalid Serial No. Scanned.")
                                hideAndClearDetailsCard()
                            }

                            else -> {
                                showErrorResult("Unknown status: ${validationResponse.status}")
                                hideAndClearDetailsCard()
                            }
                        }
                    }
                }

                // =========================================================
                // CASE 2: JSON ARRAY
                // =========================================================
                jsonElement.isJsonArray -> {
                    val dataArray = jsonElement.asJsonArray

                    if (dataArray.size() == 0) {
                        showWarningResult("No material details found.")
                        hideAndClearDetailsCard()
                    } else {
                        showLoading("Loading material details...")
                        var successCount = 0
                        for (item in dataArray) {
                            val validationResponse = gson.fromJson(item, scannedMaterialDetailsClass::class.java)
                            when (validationResponse.status) {
                                "T" -> {
                                    successCount++
                                    tableData.add(validationResponse)
                                }
                                "F" -> {
                                    showErrorResult(validationResponse.message ?: "Invalid Serial No. Scanned.")
                                }

                                else -> {
                                    showErrorResult("Unknown status: ${validationResponse.status}")
                                }
                            }
                        }

                        if (successCount > 0) {
                            showSuccessResult("$successCount material detail(s) fetched successfully.")
                        } else {
                            showErrorResult("No valid material details found.")
                            hideAndClearDetailsCard()
                        }
                    }
                }

                // =========================================================
                // INVALID JSON
                // =========================================================
                else -> {
                    showErrorResult("Invalid response format.")
                    hideAndClearDetailsCard()
                }
            }

        } catch (e: Exception) {
            Log.e("SCAN_RESPONSE", "Error parsing response: ${e.message}", e)
            showErrorResult("Failed to parse server response.")
            hideAndClearDetailsCard()
        }

        updateTableButtonUIState()
    }

    private fun submitValidMaterial() {
        binding.scannedMaterialCodeInput.setText("")
        ensureFocusAndHideKeyboard(binding.scannedMaterialCodeInput)

        if (Prefs(this).getIpAddress().isNullOrEmpty() || Prefs(this).getPortNumber().isNullOrEmpty()) {
            showErrorResult("Please do the Network Configuration first.")
            return
        }

        if (tableData.isEmpty()) {
            showErrorResult("No valid material scanned.")
            hideAndClearDetailsCard()
            updateTableButtonUIState()
            binding.scannedMaterialCodeInput.setText("")
            ensureFocusAndHideKeyboard(binding.scannedMaterialCodeInput)
            return
        }

        val endpoint = "api/material/submit-valid-material"

        val token = Prefs(this).getToken() ?: ""

        if (token.isEmpty()){
            showErrorResult("Token is empty.")
            return
        }

        val scannedSerialNo =tableData.joinToString("$") { it.serialNo ?: "" }
        val serialNoQty = tableData.joinToString("$") { it.printQuantity ?: "" }
        val serialNoBatchQty = tableData.joinToString("$") { it.batchQuantity ?: "" }

        val jsonObject = JsonObject().apply {
            addProperty("serial_no", scannedSerialNo)
            addProperty("qty", serialNoQty)
            addProperty("batch_qty", serialNoBatchQty)
        }

        lifecycleScope.launch {
            showLoading("Submitting Data...")

            try {
                // callApi is a suspend fun and already hops to Dispatchers.IO internally,
                // so we're back on Main as soon as it returns and can update UI directly.
                val response = apiManager.callApi(endpoint, "POST", jsonObject.toString(), token)
                val responseBody = response.body()?.string()

                hideLoading()
                if (response.isSuccessful && responseBody != null) {
                    handleSubmitValidMaterialResponse(responseBody)
                } else {
                    val errorMessage = response.message()
                    if (response.code() != 440 && response.code() != 401) {
                        showErrorResult("Error ${response.code()}: $errorMessage")
                    } else {
                        showErrorResult(errorMessage)
                        hideAndClearDetailsCard()
                        updateTableButtonUIState()
                    }
                }
            } catch (e: IOException) {
                hideLoading()
                showErrorResult("Network Error: ${e.message}")
                hideAndClearDetailsCard()
                updateTableButtonUIState()
            } catch (e: Exception) {
                hideLoading()
                showErrorResult("Configuration Error: ${e.message}")
                hideAndClearDetailsCard()
                updateTableButtonUIState()
            }
        }
    }

    private fun handleSubmitValidMaterialResponse(responseBody: String) {
        val apiResponse = gson.fromJson(responseBody, scannedMaterialDetailsClass::class.java)

        // Handle the response based on the status field
        when (apiResponse.status) {
            "T" -> {
                try {
                    showSuccessResult(apiResponse.message ?: "Material submitted successfully.")
                    hideAndClearDetailsCard()
                    binding.removeCheckbox.isChecked = false
                    tableData.clear()
                    updateTableButtonUIState()
                    refreshTableUI()
                    binding.scannedMaterialCodeInput.setText("")
                    ensureFocusAndHideKeyboard(binding.scannedMaterialCodeInput)
                    binding.scannedResultTextView.text = "Scanned code will appear here"

                } catch (e: Exception) {
                    showErrorResult("Failed to parse details.")
                    hideAndClearDetailsCard()
                    updateTableButtonUIState()
                }
            }
            "F" -> {
                showErrorResult(apiResponse.message ?: "Failed to submit material.")
                hideAndClearDetailsCard()
                updateTableButtonUIState()
            }
            else -> {
                showErrorResult("Unknown status: ${apiResponse.status}")
                hideAndClearDetailsCard()
                updateTableButtonUIState()
            }
        }
    }

    private fun updateTableButtonUIState() {
        if (tableData.isNotEmpty()) {
            binding.tableLayoutCard.visibility = View.VISIBLE
            binding.btnSubmit.backgroundTintList = ContextCompat.getColorStateList(this, R.color.button_primary)
            binding.btnSubmit.setTextColor(ContextCompat.getColor(this, R.color.color_on_primary))
            binding.btnSubmit.isEnabled = true
        } else {
            binding.tableLayoutCard.visibility = View.GONE
            binding.btnSubmit.backgroundTintList = ContextCompat.getColorStateList(this, R.color.button_disabled)
            binding.btnSubmit.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary))
            binding.btnSubmit.isEnabled = false
        }

        refreshTableUI()
    }

    private fun setupPagination() {
        binding.paginationContainer.visibility = View.GONE
        val rowOptionsText = arrayOf(
            "10 records",
            "20 records",
            "30 records",
            "50 records"
        )
        val rowValues = arrayOf(10, 20, 30, 50)
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            rowOptionsText
        )

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.rowsPerPageSpinner.adapter = adapter
        binding.rowsPerPageSpinner.setSelection(0)
        binding.rowsPerPageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                rowsPerPage = rowValues[position]
                currentPage = 0
                refreshTableUI()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.previousIcon.setOnClickListener {
            if (currentPage > 0) {
                currentPage--
                refreshTableUI()
            }
        }

        binding.nextIcon.setOnClickListener {
            val totalPages = (tableData.size + rowsPerPage - 1) / rowsPerPage
            if (currentPage < totalPages - 1) {
                currentPage++
                refreshTableUI()
            }
        }
    }

    private fun updatePaginationUI() {
        val totalPages = maxOf(1, (tableData.size + rowsPerPage - 1) / rowsPerPage)
        binding.pageNumbers.text = "${currentPage + 1} / $totalPages"

        //---------------------------------------
        // Previous Button
        //---------------------------------------
        val previousEnabled = currentPage > 0
        binding.previousIcon.isEnabled = previousEnabled
        binding.previousIcon.isClickable = previousEnabled
        binding.previousIcon.setBackgroundResource(
            if (previousEnabled)
                R.drawable.pagination_icon_enable
            else
                R.drawable.pagination_icon_disable
        )

        //---------------------------------------
        // Next Button
        //---------------------------------------
        val nextEnabled = currentPage < totalPages - 1
        binding.nextIcon.isEnabled = nextEnabled
        binding.nextIcon.isClickable = nextEnabled
        binding.nextIcon.setBackgroundResource(
            if (nextEnabled)
                R.drawable.pagination_icon_enable
            else
                R.drawable.pagination_icon_disable
        )

        //---------------------------------------
        // Show / Hide Pagination
        //---------------------------------------
        binding.paginationContainer.visibility = if (tableData.size > 10) View.VISIBLE else View.GONE

    }

    private fun refreshTableUI() {
        // Clear all rows from the table, but keep the header row (index 0)
        binding.tableLayout.removeViews(1, binding.tableLayout.childCount - 1)

        val start = currentPage * rowsPerPage
        val end = minOf(start + rowsPerPage, tableData.size)

        for (i in start until end) {
            val item = tableData[i]
            val newRow = TableRow(this)
            newRow.layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )

            // Sr. No. column
            newRow.addView(createCellTextView((i + 1).toString()))

            // Serial No. column
            newRow.addView(createCellTextView(item.serialNo))

            // Item Code column
            newRow.addView(createCellTextView(item.itemCode))

            // Item Description column
            newRow.addView(createCellTextView(item.itemDescription))

            // Quantity column
            newRow.addView(createCellTextView(item.printQuantity))

            binding.tableLayout.addView(newRow)
        }

        binding.totalScannedMaterial.text = "Total Material: ${tableData.size}"
        binding.totalScanned.text = "Total Scanned: ${tableData.size}"
        binding.totalScannedQty.text = "Total Quantity: ${tableData.sumOf { it.printQuantity?.toString()?.toDouble() ?: 0.0 }}"

        val totalPages = if (tableData.isEmpty()) 0 else (tableData.size + rowsPerPage - 1) / rowsPerPage
        binding.pageNumbers.text = "${currentPage + 1} / $totalPages"

        binding.previousIcon.alpha = if (currentPage > 0) 1.0f else 0.5f
        binding.nextIcon.alpha = if (currentPage < totalPages - 1) 1.0f else 0.5f

        updatePaginationUI()
    }

    private fun createCellTextView(text: String?): TextView {
        return TextView(this).apply {
            this.text = text ?: ""
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(8, 20, 8, 20)
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
            setBackgroundResource(R.drawable.table_cell_border)
        }
    }

    private fun hideAndClearDetailsCard() {
        binding.materialDetailsCard.visibility = View.GONE
        binding.itemCodeValue.text = ""
        binding.itemDescValue.text = ""
        binding.itemQtyValue.text = ""
        binding.batchQtyValue.text = ""
    }

    private fun showSuccessResult(message: String) {
        binding.resultCard.visibility = View.VISIBLE
        binding.resultCard.setCardBackgroundColor(
            ContextCompat.getColor(this, R.color.success)
        )
        binding.resultText.text = message
        binding.resultIcon.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.ic_check_circle)
        )

        ResultFeedbackHelper.showSuccessResult(binding.root, message)
        ResultFeedbackHelper.showSuccessFeedback(this)
        VoiceAnnouncer.speak(this, message)
    }

    private fun showWarningResult(message: String) {
        binding.resultCard.visibility = View.VISIBLE
        binding.resultCard.setCardBackgroundColor(
            ContextCompat.getColor(this, R.color.warning)
        )
        binding.resultText.text = message
        binding.resultIcon.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.ic_warning_circle)
        )

        ResultFeedbackHelper.showWarningResult(binding.root, message)
        ResultFeedbackHelper.showWarningFeedback(this)
        VoiceAnnouncer.speak(this, message)
    }

    private fun showErrorResult(message: String) {
        binding.resultCard.visibility = View.VISIBLE
        binding.resultCard.setCardBackgroundColor(
            ContextCompat.getColor(this, R.color.error)
        )
        binding.resultText.text = message
        binding.resultIcon.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.ic_error_circle)
        )

        ResultFeedbackHelper.showWarningResult(binding.root, message)
        ResultFeedbackHelper.showErrorFeedback(this)
        VoiceAnnouncer.speak(this, message)
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