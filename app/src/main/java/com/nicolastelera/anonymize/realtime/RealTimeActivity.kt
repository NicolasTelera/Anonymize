package com.nicolastelera.anonymize.realtime

import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.LinearLayout
import com.camerakit.CameraKit
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.nicolastelera.anonymize.R
import io.alterac.blurkit.BlurLayout
import kotlinx.android.synthetic.main.activity_real_time.*

class RealTimeActivity : AppCompatActivity() {

    private val detector = FirebaseVision.getInstance().getVisionFaceDetector(
        FirebaseVisionFaceDetectorOptions.Builder()
            .setModeType(FirebaseVisionFaceDetectorOptions.FAST_MODE)
            .setLandmarkType(FirebaseVisionFaceDetectorOptions.NO_LANDMARKS)
            .setClassificationType(FirebaseVisionFaceDetectorOptions.NO_CLASSIFICATIONS)
            .setMinFaceSize(0.15f)
            .setTrackingEnabled(true)
            .build()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_real_time)

        //TODO: should not have to override flash value (crash when onStart without it)
        cameraKitView.flash = CameraKit.FLASH_OFF

        updateButton.setOnClickListener {
            cameraKitView.captureImage { _, bytes -> processBytesArray(bytes) }
        }
    }

    private fun processBytesArray(bytes: ByteArray) {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        detector.detectInImage(FirebaseVisionImage.fromBitmap(bitmap))
            .addOnSuccessListener { faces -> processDetectedFaces(faces) }
    }

    override fun onStart() {
        super.onStart()
        cameraKitView.onStart()
    }

    override fun onResume() {
        super.onResume()
        cameraKitView.onResume()
    }

    override fun onPause() {
        cameraKitView.onPause()
        super.onPause()
    }

    override fun onStop() {
        cameraKitView.onStop()
        super.onStop()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        cameraKitView.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun processDetectedFaces(faces: List<FirebaseVisionFace>) {
        faces.forEach { addBlurZone(it.boundingBox) }
    }

    private fun addBlurZone(rect: Rect) {
        val view = View.inflate(this@RealTimeActivity, R.layout.blurred_layout, null).apply {
            with(rect) {
                x = left.toFloat()
                y = top.toFloat()
                layoutParams = LinearLayout.LayoutParams(
                    width(),
                    height()
                )
            }
            elevation = 2f
        }
        (view as BlurLayout).startBlur()
        container.addView(view)
    }
}
