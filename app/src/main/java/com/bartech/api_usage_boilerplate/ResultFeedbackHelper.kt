package com.bartech.api_usage_boilerplate

import android.animation.ObjectAnimator
import android.app.Activity
import android.app.Dialog
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import com.bartech.api_usage_boilerplate.databinding.ResultFeedbackOverlayBinding
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar
import com.bartech.api_usage_boilerplate.R

object ResultFeedbackHelper {

    private var feedbackDialog: Dialog? = null
    private var mediaPlayer: MediaPlayer? = null

    private var binding: ResultFeedbackOverlayBinding? = null

    private val handler = Handler(Looper.getMainLooper())

    fun showErrorFeedback(activity: Activity) {

        stopCurrentFeedback()

        // SCREEN FLASH
        if (Prefs(activity).getScreenFlashForResultSetting()) {

            showFlashOverlay(
                activity,
                ContextCompat.getColor(activity,ResultFeedbackConfig.ERROR_FLASH_COLOR),
                ResultFeedbackConfig.ERROR_FLASH_DURATION
            )
        }

        // SOUND
        if (Prefs(activity).getResultSoundSetting()) {

            mediaPlayer = MediaPlayer.create(
                activity,
                ResultFeedbackConfig.ERROR_SOUND_FILE
            )

            mediaPlayer?.isLooping = false
            mediaPlayer?.start()

            handler.postDelayed({

                try {
                    mediaPlayer?.stop()
                } catch (_: Exception) {
                }

                mediaPlayer?.release()
                mediaPlayer = null

            }, ResultFeedbackConfig.ERROR_SOUND_DURATION)
        }
    }

    fun showWarningFeedback(activity: Activity) {

        stopCurrentFeedback()

        // SCREEN FLASH
        if (Prefs(activity).getScreenFlashForResultSetting()) {

            showFlashOverlay(
                activity,
                ContextCompat.getColor(activity,ResultFeedbackConfig.WARNING_FLASH_COLOR),
                ResultFeedbackConfig.WARNING_FLASH_DURATION
            )
        }

        // SOUND
        if (Prefs(activity).getResultSoundSetting()) {

            mediaPlayer = MediaPlayer.create(
                activity,
                ResultFeedbackConfig.WARNING_SOUND_FILE
            )

            mediaPlayer?.isLooping = false
            mediaPlayer?.start()

            handler.postDelayed({

                try {
                    mediaPlayer?.stop()
                } catch (_: Exception) {
                }

                mediaPlayer?.release()
                mediaPlayer = null

            }, ResultFeedbackConfig.WARNING_SOUND_DURATION)
        }
    }

    fun showSuccessFeedback(activity: Activity) {

        stopCurrentFeedback()

        if (Prefs(activity).getScreenFlashForResultSetting()) {

            showFlashOverlay(
                activity,
                ContextCompat.getColor(activity,ResultFeedbackConfig.SUCCESS_FLASH_COLOR),
                ResultFeedbackConfig.SUCCESS_FLASH_DURATION
            )
        }
    }

    private fun showFlashOverlay(
        activity: Activity,
        color: Int,
        duration: Long
    ) {

        try {
            feedbackDialog?.dismiss()
        } catch (_: Exception) {
        }

        feedbackDialog = Dialog(activity).apply {

            setCancelable(false)

            binding = ResultFeedbackOverlayBinding.inflate(
                activity.layoutInflater
            )

            binding?.let { overlayBinding ->

                setContentView(overlayBinding.root)

                overlayBinding.flashOverlayRoot.setBackgroundColor(color)
            }

            window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )

            window?.addFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            )

            window?.setBackgroundDrawableResource(
                android.R.color.transparent
            )
        }

        feedbackDialog?.show()

        binding?.flashOverlayRoot?.alpha = 0f

        binding?.flashOverlayRoot?.let { overlayView ->

            val animator = ObjectAnimator.ofFloat(
                overlayView,
                "alpha",
                0f,
                0.8f,
                0f,
                0.8f,
                0f
            )

            animator.duration = duration
            animator.start()
        }

        handler.postDelayed({

            try {
                feedbackDialog?.dismiss()
            } catch (_: Exception) {
            }

            feedbackDialog = null
            binding = null

        }, duration)
    }

    fun stopCurrentFeedback() {

        handler.removeCallbacksAndMessages(null)

        try {
            feedbackDialog?.dismiss()
        } catch (_: Exception) {
        }

        feedbackDialog = null
        binding = null

        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }

        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun showSuccessResult(
        rootView: View,
        message: String
    ) {
        showResultSnackbar(
            rootView = rootView,
            message = message,
            colorRes = R.color.success
        )
    }

    fun showErrorResult(
        rootView: View,
        message: String
    ) {
        showResultSnackbar(
            rootView = rootView,
            message = message,
            colorRes = R.color.error
        )
    }

    fun showWarningResult(
        rootView: View,
        message: String
    ) {
        showResultSnackbar(
            rootView = rootView,
            message = message,
            colorRes = R.color.warning
        )
    }

    private fun showResultSnackbar(
        rootView: View,
        message: String,
        colorRes: Int
    ) {
        val snackbar = Snackbar.make(
            rootView,
            message,
            Snackbar.LENGTH_LONG
        )

        val color = ContextCompat.getColor(
            rootView.context,
            colorRes
        )

        snackbar.setBackgroundTint(color)
        snackbar.setTextColor(Color.WHITE)

        // Allow multi-line messages
        val snackbarText = snackbar.view.findViewById<TextView>(
            com.google.android.material.R.id.snackbar_text
        )

        snackbarText.maxLines = 10
        snackbarText.isSingleLine = false

        // Show Snackbar at the top
        val snackbarView = snackbar.view

        val params = snackbarView.layoutParams as FrameLayout.LayoutParams

//        params.gravity = Gravity.TOP

        params.bottomMargin = (
                50 * rootView.resources.displayMetrics.density
                ).toInt()

        snackbarView.layoutParams = params

        snackbar.show()
    }

}