package com.example.exam

import android.content.Context
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult

class GestureRecognizerHelper(
    private val context: Context,
    private val onGestureResult: (Result) -> Unit
) {
    data class Result(
        val landmarks: List<NormalizedLandmark>,
        val handX: Float,   // 归一化 0-1，未跟踪时为 -1
        val handY: Float,   // 归一化 0-1，未跟踪时为 -1
        val isTracking: Boolean
    )

    data class NormalizedLandmark(val x: Float, val y: Float, val z: Float)

    private var recognizer: GestureRecognizer? = null
    // 位置映射模式：直接输出手部在画面中的绝对位置

    init {
        setupGestureRecognizer()
    }

    private fun setupGestureRecognizer() {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("models/gesture_recognizer.task")
                .build()
            val options = GestureRecognizer.GestureRecognizerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumHands(1)
                .setMinHandDetectionConfidence(0.5f)
                .setMinHandPresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setResultListener { result, _ -> handleResult(result) }
                .setErrorListener { }
                .build()
            recognizer = GestureRecognizer.createFromOptions(context, options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleResult(result: GestureRecognizerResult) {
        if (result.landmarks().isEmpty()) {
            onGestureResult(Result(emptyList(), -1f, -1f, false))
            return
        }

        val handLandmarks = result.landmarks()[0]
        val landmarks = handLandmarks.map { NormalizedLandmark(it.x(), it.y(), it.z()) }

        // 用掌心（手腕 + 四指 MCP 关节的平均值）作为追踪点
        // 比单点更稳定，且更符合“手的位置”直觉
        val palmIndices = listOf(0, 5, 9, 13, 17)
        val handX = palmIndices.map { landmarks[it].x }.average().toFloat()
        val handY = palmIndices.map { landmarks[it].y }.average().toFloat()

        onGestureResult(Result(landmarks, handX, handY, true))
    }

    val analyzer = ImageAnalysis.Analyzer { imageProxy ->
        val mpImage = imageProxy.toMPImage()
        if (mpImage != null && recognizer != null) {
            try {
                recognizer?.recognizeAsync(mpImage, SystemClock.uptimeMillis())
            } catch (e: Exception) {
            }
        }
        imageProxy.close()
    }

    private fun ImageProxy.toMPImage(): MPImage? {
        return try {
            val bitmap = toBitmap()
            val rotation = imageInfo.rotationDegrees
            val matrix = android.graphics.Matrix()
            // 步骤1：旋转到显示方向（手机竖屏时前置摄像头通常需旋转 270°）
            // 修复关键 bug：之前缺少旋转导致坐标系差 90°，手向上被识别为向左
            matrix.postRotate(rotation.toFloat())
            // 步骤2：前置摄像头镜像翻转（自拍视图）
            matrix.postScale(-1f, 1f)
            val processed = android.graphics.Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )
            BitmapImageBuilder(processed).build()
        } catch (e: Exception) {
            null
        }
    }

    fun close() {
        recognizer?.close()
        recognizer = null
    }
}
