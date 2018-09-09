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
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions

private data class FaceRectangle(val bounds: Rect, val rotationX: Float, val rotationY: Float)

class BlurViewGroup(context: Context, attrsSet: AttributeSet) : RelativeLayout(context, attrsSet) {

    private val imageView = BlurImageView()
    private val rectangleList = mutableListOf<FaceRectangle>()
    private var srcBitmap: Bitmap? = null

    private val detector = FirebaseVision.getInstance().getVisionFaceDetector(
        FirebaseVisionFaceDetectorOptions.Builder()
            .setModeType(FirebaseVisionFaceDetectorOptions.FAST_MODE)
            .setLandmarkType(FirebaseVisionFaceDetectorOptions.NO_LANDMARKS)
            .setClassificationType(FirebaseVisionFaceDetectorOptions.NO_CLASSIFICATIONS)
            .setMinFaceSize(0.15f)
            .setTrackingEnabled(true)
            .build()
    )

    fun setImage(bitmap: Bitmap) {
        resetContainer()
        srcBitmap = bitmap
        imageView.setImageBitmap(bitmap)
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
                .addOnSuccessListener {
                    it.forEach { face ->
                        with(face) {
                            rectangleList.add(FaceRectangle(
                                boundingBox,
                                headEulerAngleY,
                                headEulerAngleZ
                            ))
                        }
                    }
                    addRectangles()
                }
                .addOnFailureListener {
                    // do nothing
                }
        }
    }

    private fun addRectangles() {
        rectangleList.forEach { rect ->
            val view = View.inflate(context, R.layout.bordered_layout, null).apply {
                x = rect.bounds.left.toFloat()
                y = rect.bounds.top.toFloat()
                layoutParams = LinearLayout.LayoutParams(rect.bounds.width(), rect.bounds.height())
                setOnClickListener {
                    imageView.applyBlur(rect.bounds)
                }
            }
            addView(view)
        }
    }

    private inner class BlurImageView : ImageView(context) {

        init {
            adjustViewBounds = true
            isFocusable = true
            isFocusableInTouchMode = true
        }

        fun applyBlur(bounds: Rect) {
            srcBitmap?.let {
                val output = Bitmap.createScaledBitmap(it, it.width, it.height, false)
                val rs = RenderScript.create(context)
                val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
                val inAlloc = Allocation.createFromBitmap(rs, it,
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
