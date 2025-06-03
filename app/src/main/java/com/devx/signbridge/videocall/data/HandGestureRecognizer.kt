package com.devx.signbridge.videocall.data

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.devx.signbridge.videocall.domain.models.Classification
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult

class HandGestureRecognizer(
    private val context: Context,
    private val threshold: Float = 0.5f,
    private val maxClassifications: Int = 1,
    private val gestureRecognizerListener: GestureRecognizerListener? = null
) {

    private var gestureRecognizer: GestureRecognizer? = null

    init {
        setupGestureRecognizer()
    }

    fun setupGestureRecognizer() {
        val baseOptionBuilder = BaseOptions.builder()
            .setDelegate(Delegate.CPU)
            .setModelAssetPath(MP_GESTURE_MODEL)

        try {
            val baseOptions = baseOptionBuilder.build()
            val optionsBuilder = GestureRecognizer.GestureRecognizerOptions.builder()
                .setBaseOptions(baseOptions)
                .setNumHands(2)
                .setMinHandPresenceConfidence(threshold)
                .setMinHandDetectionConfidence(threshold)
                .setMinTrackingConfidence(threshold)
                .setRunningMode(RunningMode.LIVE_STREAM) // For video call stream
                .setResultListener(this::returnLivestreamResult)
                .setErrorListener(this::returnLivestreamError)

            val options = optionsBuilder.build()
            gestureRecognizer = GestureRecognizer.createFromOptions(context, options)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize [GestureRecognizer]", e)
        }
    }

    fun clearGestureRecognizer() {
        gestureRecognizer?.close()
        gestureRecognizer = null
    }

    fun detectGestures(bitMap: Bitmap, frameTime: Long) {
        // Convert the input Bitmap object to an MPImage object to run inference
        val mpImage = BitmapImageBuilder(bitMap).build()
        gestureRecognizer?.recognizeAsync(mpImage, frameTime)
    }

    // Return the recognition result to the GestureRecognizerHelper's caller
    private fun returnLivestreamResult(
        result: GestureRecognizerResult, input: MPImage
    ) {
        val finishTimeMs = SystemClock.uptimeMillis()
        val inferenceTime = finishTimeMs - result.timestampMs()

        // Extract top category (if available)
        val category = result.gestures()
            .firstOrNull() // get first gesture list (usually single handed input)
            ?.firstOrNull() // get top category

        val classification = if (category != null) {
            Classification(
                name = category.categoryName(),
                confidence = category.score()
            )
        } else {
            Classification(name = "No gesture", confidence = 0f)
        }

        gestureRecognizerListener?.onResults(
            ResultBundle(
                listOf(classification), inferenceTime, input.height, input.width
            )
        )
    }

    // Return errors thrown during recognition to this GestureRecognizerHelper's
    // caller
    private fun returnLivestreamError(error: RuntimeException) {
        gestureRecognizerListener?.onError(
            error.message ?: "An unknown error has occurred"
        )
    }

    interface GestureRecognizerListener {
        fun onError(error: String, errorCode: Int = 0)
        fun onResults(resultBundle: ResultBundle)
    }

    data class ResultBundle(
        val results: List<Classification>,
        val inferenceTime: Long,
        val inputImageHeight: Int,
        val inputImageWidth: Int,
    )

    companion object {
        private const val TAG = "HandGestureClassifier"
        private const val MP_GESTURE_MODEL = "static_gesture_recognizer.task"
    }
}