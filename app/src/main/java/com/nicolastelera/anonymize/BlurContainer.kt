package com.nicolastelera.anonymize

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.renderscript.*
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions

/**
 * TODO :
 * - adapt final blur to preview blur intensity
 * - round blurred part
 * - display face detection in real time to known which faces will be recognized
 * https://firebase.google.com/docs/ml-kit/detect-faces
 */

data class SrcContainer(
        val srcBitmap: Bitmap,
        val scaledBitmap: Bitmap
)

private data class FaceRectangle(
        val bounds: Rect,
        var shouldBeBlurred: Boolean = false
) {
    fun toScaledRect(scaleFactor: Float) = Rect(
            (bounds.left * scaleFactor).toInt(),
            (bounds.top * scaleFactor).toInt(),
            (bounds.right * scaleFactor).toInt(),
            (bounds.bottom * scaleFactor).toInt()
    )
}

class BlurContainer(context: Context, attrsSet: AttributeSet) : RelativeLayout(context, attrsSet) {

    companion object {
        private const val PREVIEW_BLUR_INTENSITY = 5
        private const val FINAL_BLUR_INTENSITY = 20
    }

    private val imageView = BlurImageView()
    private val rectangleList = mutableListOf<FaceRectangle>()
    private var srcBitmap: Bitmap? = null
    private var scaledBitmap: Bitmap? = null
    private var srcScaleFactor: Float = 0f

    private val detector = FirebaseVision.getInstance().getVisionFaceDetector(
            FirebaseVisionFaceDetectorOptions.Builder()
                    .setModeType(FirebaseVisionFaceDetectorOptions.FAST_MODE)
                    .setLandmarkType(FirebaseVisionFaceDetectorOptions.NO_LANDMARKS)
                    .setClassificationType(FirebaseVisionFaceDetectorOptions.NO_CLASSIFICATIONS)
                    .setMinFaceSize(0.15f)
                    .setTrackingEnabled(true)
                    .build()
    )

    fun setImageToBlur(container: SrcContainer) {
        resetContainer()
        srcScaleFactor = container.scaledBitmap.width.toFloat() / container.srcBitmap.width.toFloat()
        srcBitmap = container.srcBitmap
        scaledBitmap = container.scaledBitmap
        imageView.updateImage()
    }

    fun getFinalImage(): Bitmap? {
        srcBitmap?.let { srcBitmap ->
            var src = srcBitmap.copy(Bitmap.Config.ARGB_8888, false)
            rectangleList.forEach {
                if (it.shouldBeBlurred) {
                    src = applyBlur(src, it.bounds, FINAL_BLUR_INTENSITY)
                }
            }
            return src
        } ?: return null
    }

    private fun resetContainer() {
        rectangleList.clear()
        removeAllViews()
        addView(imageView.apply { isProcessed = false })
    }

    private fun detectFaces() {
        srcBitmap?.let { bitmap ->
            detector.detectInImage(FirebaseVisionImage.fromBitmap(bitmap))
                    .addOnSuccessListener { processDetectedFaces(it) }
        }
    }

    private fun processDetectedFaces(faces: List<FirebaseVisionFace>) {
        faces.forEach { rectangleList.add(FaceRectangle(it.boundingBox)) }
        addRectangles()
    }

    private fun addRectangles() {
        val scaleFactor = imageView.width.toFloat() / this@BlurContainer.width.toFloat()
        rectangleList.forEach { faceRect ->

            val rect = faceRect.toScaledRect(srcScaleFactor)

            View.inflate(context, R.layout.bordered_layout, null).apply {
                with(rect) {
                    x = left.toFloat() * scaleFactor
                    y = top.toFloat() * scaleFactor
                    layoutParams = LinearLayout.LayoutParams(
                            (width() * scaleFactor).toInt(),
                            (height() * scaleFactor).toInt()
                    )
                    setOnClickListener {
                        faceRect.shouldBeBlurred = !faceRect.shouldBeBlurred
                        imageView.updateBlurredZones()
                    }
                }
                addView(this)
            }
        }
    }

    private fun applyBlur(src: Bitmap, bounds: Rect, iterations: Int): Bitmap {
        val output = Bitmap.createScaledBitmap(src, src.width, src.height, false)
        val rs = RenderScript.create(context)
        val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        val inAlloc = Allocation.createFromBitmap(rs, src,
                Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_GRAPHICS_TEXTURE)
        val outAlloc = Allocation.createFromBitmap(rs, output)
        val launchOptions = with(bounds) {
            Script.LaunchOptions().apply {
                setX(left, right)
                setY(top, bottom)
            }
        }
        script.apply {
            setRadius(25f)
            setInput(inAlloc)
            forEach(outAlloc, launchOptions)
        }
        outAlloc.copyTo(output)
        rs.destroy()
        return if (iterations == 0) output else applyBlur(output, bounds, iterations - 1)
    }

    private inner class BlurImageView : ImageView(context) {

        var isProcessed = false

        init {
            adjustViewBounds = true
            isFocusable = true
            isFocusableInTouchMode = true
        }

        override fun onDraw(canvas: Canvas?) {
            super.onDraw(canvas)
            if (!isProcessed) {
                detectFaces()
                isProcessed = true
            }
        }

        fun updateImage() {
            scaledBitmap?.let { setImageBitmap(it) }
        }

        fun updateBlurredZones() {
            scaledBitmap?.let { srcBitmap ->
                var src = srcBitmap.copy(Bitmap.Config.ARGB_8888, false)
                rectangleList.forEach { faceRect ->
                    if (faceRect.shouldBeBlurred) {
                        src = applyBlur(src, faceRect.toScaledRect(srcScaleFactor), PREVIEW_BLUR_INTENSITY)
                    }
                }
                setImageBitmap(src)
            }
        }
    }
}
