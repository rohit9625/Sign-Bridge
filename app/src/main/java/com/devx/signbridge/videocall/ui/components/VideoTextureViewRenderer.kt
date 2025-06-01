package com.devx.signbridge.videocall.ui.components

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.graphics.YuvImage
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import com.devx.signbridge.videocall.data.HandGestureRecognizer
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import org.webrtc.EglBase
import org.webrtc.EglRenderer
import org.webrtc.GlRectDrawer
import org.webrtc.RendererCommon.RendererEvents
import org.webrtc.ThreadUtils
import org.webrtc.VideoFrame
import org.webrtc.VideoSink
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors


/**
 * Custom [TextureView] used to render local/incoming videos on the screen.
 */
open class VideoTextureViewRenderer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : TextureView(context, attrs), VideoSink, SurfaceTextureListener {

    /**
     * Cached resource name.
     */
    private val resourceName: String = getResourceName()

    /**
     * Renderer used to render the video.
     */
    private val eglRenderer: EglRenderer = EglRenderer(resourceName)

    /**
     * Callback used for reporting render events.
     */
    private var rendererEvents: RendererEvents? = null

    /**
     * Handler to access the UI thread.
     */
    private val uiThreadHandler = Handler(Looper.getMainLooper())

    /**
     * Background executor for MediaPipe processing
     */
    private val backgroundExecutor = Executors.newSingleThreadExecutor()

    /**
     * Whether the first frame has been rendered or not.
     */
    private var isFirstFrameRendered = false

    /**
     * The rotated [VideoFrame] width.
     */
    private var rotatedFrameWidth = 0

    /**
     * The rotated [VideoFrame] height.
     */
    private var rotatedFrameHeight = 0

    /**
     * The rotated [VideoFrame] rotation.
     */
    private var frameRotation = 0

    /**
     * MediaPipe gesture recognizer instance
     */
    private var gestureRecognizer: HandGestureRecognizer? = null

    /**
     * Frame processing interval (process every Nth frame to avoid overwhelming MediaPipe)
     */
    private var frameProcessingInterval = 5
    private var frameCount = 0

    init {
        surfaceTextureListener = this
    }

    /**
     * Set the MediaPipe gesture recognizer
     */
    fun setGestureRecognizer(
        recognizer: HandGestureRecognizer,
        processingInterval: Int = 5
    ) {
        this.gestureRecognizer = recognizer
        this.frameProcessingInterval = processingInterval
    }

    /**
     * Convert I420 buffer to Bitmap
     */
    private fun convertI420ToBitmap(i420Buffer: VideoFrame.I420Buffer): Bitmap? {
        return try {
            val width = i420Buffer.width
            val height = i420Buffer.height

            val ySize = width * height
            val uvSize = width * height / 4

            val nv21 = ByteArray(ySize + uvSize * 2)

            // Copy Y plane
            i420Buffer.dataY.get(nv21, 0, ySize)

            // Interleave U and V planes to create NV21 format
            val uData = ByteArray(uvSize)
            val vData = ByteArray(uvSize)
            i420Buffer.dataU.get(uData)
            i420Buffer.dataV.get(vData)

            for (i in 0 until uvSize) {
                nv21[ySize + i * 2] = vData[i]
                nv21[ySize + i * 2 + 1] = uData[i]
            }

            // Convert NV21 to RGB
            val yuvImage = YuvImage(
                nv21,
                android.graphics.ImageFormat.NV21,
                width,
                height,
                null
            )

            val out = java.io.ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)

            val imageBytes = out.toByteArray()
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

        } catch (e: Exception) {
            Log.e("VideoTextureViewRenderer", "Error converting I420 to Bitmap", e)
            null
        }
    }

    /**
     * Process [VideoFrame] for gesture recognition
     */
    private fun processFrameForGestures(videoFrame: VideoFrame) {
        // Only process every Nth frame to avoid overwhelming MediaPipe
        frameCount++
        if (frameCount % frameProcessingInterval != 0) {
            return
        }
        val recognizer = gestureRecognizer ?: return

        backgroundExecutor.execute {
            // Process frame in background thread
            try {
                val frameTime = SystemClock.uptimeMillis()
                val bitmap = convertVideoFrameToBitmap(videoFrame)
                if (bitmap != null) {
                    recognizer.detectGestures(bitmap, frameTime)
                }
            } catch (e: Exception) {
                Log.e("VideoTextureViewRenderer", "Error processing frame for gestures", e)
            }
        }
    }

    /**
     * Convert WebRTC [VideoFrame] to [Bitmap] required for [GestureRecognizer]
     */
    private fun convertVideoFrameToBitmap(videoFrame: VideoFrame): Bitmap? {
        var i420Buffer: VideoFrame.I420Buffer? = null
        return try {
            i420Buffer = videoFrame.buffer.toI420()
            convertI420ToBitmap(i420Buffer!!)
        } catch (e: Exception) {
            Log.e("VideoTextureViewRenderer", "Error converting VideoFrame to Bitmap", e)
            null
        } finally {
            Log.i("VideoTextureViewRenderer", "Releasing I420 buffer")
            i420Buffer?.release()
        }
    }

    /**
     * Called when a new frame is received. Sends the frame to be rendered
     *      * and processes it for gesture recognition.
     *
     * @param videoFrame The [VideoFrame] received from WebRTC connection to draw on the screen.
     */
    override fun onFrame(videoFrame: VideoFrame) {
        eglRenderer.onFrame(videoFrame)
        updateFrameData(videoFrame)

        // Process frame for gesture recognition
        processFrameForGestures(videoFrame)
    }

    /**
     * Updates the frame data and notifies [rendererEvents] about the changes.
     */
    private fun updateFrameData(videoFrame: VideoFrame) {
        if (isFirstFrameRendered) {
            rendererEvents?.onFirstFrameRendered()
            isFirstFrameRendered = true
        }

        if (videoFrame.rotatedWidth != rotatedFrameWidth ||
            videoFrame.rotatedHeight != rotatedFrameHeight ||
            videoFrame.rotation != frameRotation
        ) {
            rotatedFrameWidth = videoFrame.rotatedWidth
            rotatedFrameHeight = videoFrame.rotatedHeight
            frameRotation = videoFrame.rotation

            uiThreadHandler.post {
                rendererEvents?.onFrameResolutionChanged(
                    rotatedFrameWidth,
                    rotatedFrameHeight,
                    frameRotation
                )
            }
        }
    }

    /**
     * After the view is laid out we need to set the correct layout aspect ratio to the renderer so that the image
     * is scaled correctly.
     */
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        eglRenderer.setLayoutAspectRatio((right - left) / (bottom.toFloat() - top))
    }

    /**
     * Initialise the renderer. Should be called from the main thread.
     *
     * @param sharedContext [EglBase.Context]
     * @param rendererEvents Sets the render event listener.
     */
    fun init(
        sharedContext: EglBase.Context,
        rendererEvents: RendererEvents
    ) {
        ThreadUtils.checkIsOnMainThread()
        this.rendererEvents = rendererEvents
        eglRenderer.init(sharedContext, EglBase.CONFIG_PLAIN, GlRectDrawer())
        eglRenderer.setMirror(true)
    }

    /**
     * [SurfaceTextureListener] callback that lets us know when a surface texture is ready and we can draw on it.
     */
    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        eglRenderer.createEglSurface(surfaceTexture)
    }

    /**
     * [SurfaceTextureListener] callback that lets us know when a surface texture is destroyed we need to stop drawing
     * on it.
     */
    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
        val completionLatch = CountDownLatch(1)
        backgroundExecutor.shutdown()
        eglRenderer.releaseEglSurface { completionLatch.countDown() }
        ThreadUtils.awaitUninterruptibly(completionLatch)
        return true
    }

    override fun onSurfaceTextureSizeChanged(
        surfaceTexture: SurfaceTexture,
        width: Int,
        height: Int
    ) {
    }

    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}

    override fun onDetachedFromWindow() {
        eglRenderer.release()
        super.onDetachedFromWindow()
    }

    private fun getResourceName(): String {
        return try {
            resources.getResourceEntryName(id) + ": "
        } catch (e: Resources.NotFoundException) {
            Log.e("VideoTextureViewRenderer", "Could not get resource name for $id", e)
            ""
        }
    }
}
