package com.nicolastelera.anonymize

import android.content.Context
import android.graphics.*
import android.renderscript.*
import android.util.AttributeSet
import android.widget.ImageView
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions

class DrawView(context: Context, attrsSet: AttributeSet) : ImageView(context, attrsSet) {

    data class FaceRectangle(val bounds: Rect, val rotationX: Float, val rotationY: Float)

    private val paint = Paint()
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

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        setupPaint()
    }

    private fun setupPaint() {
        paint.apply {
            color = Color.RED
            isAntiAlias = true
            strokeWidth = 5f
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        rectangleList.forEach {
            //canvas.drawRect(it.bounds, paint)
            srcBitmap?.let { bitmap ->
                setImageBitmap(applyBlur(bitmap, it.bounds))
            }
        }
    }

    fun setImage(bitmap: Bitmap) {
        rectangleList.clear()
        with(bitmap) {
            srcBitmap = this
            setImageBitmap(this)
        }
        detectFaces()
    }

    private fun applyBlur(bitmap: Bitmap, bounds: Rect): Bitmap {
        val output = Bitmap.createScaledBitmap(bitmap, bitmap.width, bitmap.height, false)
        val rs = RenderScript.create(context)
        val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        val inAlloc = Allocation.createFromBitmap(rs, srcBitmap, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_GRAPHICS_TEXTURE)
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
        return output
    }

    private fun detectFaces() {
        srcBitmap?.let { bitmap ->
            detector.detectInImage(FirebaseVisionImage.fromBitmap(bitmap))
                    .addOnSuccessListener {
                        it.forEach { face ->
                            with(face) {
                                drawRectangle(boundingBox, headEulerAngleY, headEulerAngleZ)
                            }
                        }
                    }
                    .addOnFailureListener {
                        // do nothing
                    }
        }
    }

    private fun drawRectangle(bounds: Rect, rotationX: Float, rotationY: Float) {
        rectangleList.add(FaceRectangle(bounds, rotationX, rotationY))
        invalidate()
    }
}
