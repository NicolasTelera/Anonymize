package com.nicolastelera.anonymize

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.provider.MediaStore
import android.support.media.ExifInterface
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import android.R.attr.bitmap
import android.net.Uri
import android.opengl.ETC1.getHeight
import android.opengl.ETC1.getWidth

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_WRITE_EXTERNAL_STORAGE = 1664
        private const val REQUEST_TAKE_PHOTO = 1
        private const val REQUEST_LOAD_PHOTO = 2
    }

    private val fileManager = FileManager(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkForUserPermission()
        initButtonsListeners()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_TAKE_PHOTO -> {
                    val bitmap = fileManager.getPicture(blurViewGroup.width)
                    updateViewWithImage(bitmap)
                }
                REQUEST_LOAD_PHOTO -> {
                    data?.let {
                        val inputStream = contentResolver.openInputStream(data.data)
                        val src = BitmapFactory.decodeStream(inputStream)
                        val scaleFactor = src.width.toFloat() / blurViewGroup.width.toFloat()
                        val bitmap = Bitmap.createScaledBitmap(src, (src.width.toFloat() / scaleFactor).toInt(), (src.height.toFloat() / scaleFactor).toInt(), false)

                        val orientation = fileManager.getOrientationFromUri(data.data)
                        if (orientation <= 0) {
                            updateViewWithImage(bitmap)
                        }
                        val matrix = Matrix()
                        matrix.postRotate(orientation.toFloat())
                        val bitmap1 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
                        updateViewWithImage(bitmap1)
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun checkForUserPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_WRITE_EXTERNAL_STORAGE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_WRITE_EXTERNAL_STORAGE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
                    finish()
                }
                return
            }
        }
    }

    private fun initButtonsListeners() {
        pictureButton.setOnClickListener {
            with(Intent(MediaStore.ACTION_IMAGE_CAPTURE)) {
                if (resolveActivity(packageManager) != null) {
                    putExtra(MediaStore.EXTRA_OUTPUT, fileManager.createPhotoUri())
                    startActivityForResult(this, REQUEST_TAKE_PHOTO)
                }
            }
        }

        importButton.setOnClickListener {
            with(Intent(Intent.ACTION_PICK)) {
                type = "image/*"
                startActivityForResult(this, REQUEST_LOAD_PHOTO)
            }
        }

        saveButton.setOnClickListener {
            fileManager.saveModifiedBitmap(blurViewGroup.getModifiedImage())
        }
    }

    private fun updateViewWithImage(bitmap: Bitmap) {
        blurViewGroup.setImageToBlur(bitmap)
        saveButton.visibility = View.VISIBLE
    }
}
