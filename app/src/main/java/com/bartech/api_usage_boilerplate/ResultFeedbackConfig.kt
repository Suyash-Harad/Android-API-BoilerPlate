package com.bartech.api_usage_boilerplate

import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat

object ResultFeedbackConfig {

    // ERROR
    const val ERROR_FLASH_DURATION = 1500L
    const val ERROR_SOUND_DURATION = 1500L
    val ERROR_FLASH_COLOR = R.color.error

    // WARNING
    const val WARNING_FLASH_DURATION = 1500L
    const val WARNING_SOUND_DURATION = 1500L
    val WARNING_FLASH_COLOR = R.color.warning

    // SUCCESS
    const val SUCCESS_FLASH_DURATION = 0L
    val SUCCESS_FLASH_COLOR = R.color.success

    // SOUND
    val ERROR_SOUND_FILE = R.raw.wrong_sound
    val WARNING_SOUND_FILE = R.raw.wrong_sound
}