package com.nicolastelera.anonymize

import android.content.Context
import android.graphics.Bitmap
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

private data class FaceRectangle(val bounds: Rect, val rotationX: Float, val rotationY: Float)

class BlurViewGroup(context: Context, attrsSet: AttributeSet) : RelativeLayout(context, attrsSet) {

    private val imageView = BlurImageView()
    private val rectangleList = mutableListOf<FaceRectangle>()
    private var srcBitmap: Bitmap? = null
    private var scaleFactor: Float = 0f

    private val detector = FirebaseVision.getInstance().getVisionFaceDetector(
        FirebaseVisionFaceDetectorOptions.Builder()
            .setModeType(FirebaseVisionFaceDetectorOptions.FAST_MODE)
            .setLandmarkType(FirebaseVisionFaceDetectorOptions.NO_LANDMARKS)
            .setClassificationType(FirebaseVisionFaceDetectorOptions.NO_CLASSIFICATIONS)
            .setMinFaceSize(0.15f)
            .setTrackingEnabled(true)
            .build()
    )

    fun setImageToBlur(bitmap: Bitmap) {
        resetContainer()
        with(bitmap) {
            srcBitmap = this
            imageView.updateImage()
            scaleFactor = this@BlurViewGroup.width / width.toFloat()
        }
        detectFaces()
    }

    private fun resetContainer() {
        rectangleList.clear()
        removeAllViews()
        addView(imageView)
    }

    private fun detectFaces() {
        srcBitmap?.let { bitmap ->
            detector.detectInImage(FirebaseVisionImage.fromBitmap(bitmap))
                .addOnSuccessListener { processDetectedFaces(it) }
        }
    }

    private fun processDetectedFaces(faces: List<FirebaseVisionFace>) {
        faces.forEach { face ->
            with(face) {
                rectangleList.add(com.nicolastelera.anonymize.FaceRectangle(
                    boundingBox,
                    headEulerAngleY,
                    headEulerAngleZ
                ))
            }
        }
        addRectangles()
    }

    private fun addRectangles() {
        rectangleList.forEach { rect ->
            View.inflate(context, R.layout.bordered_layout, null).apply {
                with(rect.bounds) {
                    x = left.toFloat() * scaleFactor
                    y = top.toFloat() * scaleFactor
                    layoutParams = LinearLayout.LayoutParams(
                        (width() * scaleFactor).toInt(),
                        (height() * scaleFactor).toInt()
                    )
                    setOnClickListener { imageView.applyBlur(this) }
                }
                addView(this)
            }
        }
    }

    private inner class BlurImageView : ImageView(context) {

        init {
            adjustViewBounds = true
            isFocusable = true
            isFocusableInTouchMode = true
        }

        fun updateImage() {
            srcBitmap?.let { setImageBitmap(it) }
        }

        fun applyBlur(bounds: Rect) {
            srcBitmap?.let { src ->
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
                setImageBitmap(output)
            }
        }
    }
}
