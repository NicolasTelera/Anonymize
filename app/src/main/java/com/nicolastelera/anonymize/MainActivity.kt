package com.nicolastelera.anonymize

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_WRITE_EXTERNAL_STORAGE = 1664
        private const val REQUEST_TAKE_PHOTO = 1
        private const val FILE_PROVIDER_AUTHORITY = "com.nicolastelera.anonymize.fileprovider"
        private const val DATE_FORMAT_PATTERN = "yyyyMMdd_HHmmss"
        private const val JPEG_PREFIX = "JPEG_"
        private const val JPG_EXTENSION = ".jpg"
        private const val BLURRED_SUFFIX = "_blurred"
        private const val APP_DIRECTORY_PICTURES = "/Anonymize"
    }

    private var currentPhotoPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkForUserPermission()
        initButtonsListeners()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK) {
            blurViewGroup.setImageToBlur(getPicture())
            saveButton.visibility = View.VISIBLE
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
                    putExtra(MediaStore.EXTRA_OUTPUT, createPhotoUri())
                    startActivityForResult(this, REQUEST_TAKE_PHOTO)
                }
            }
        }

        saveButton.setOnClickListener {
            saveModifiedBitmap(blurViewGroup.getModifiedImage())
        }
    }

    private fun createPhotoUri(): Uri {
        return FileProvider.getUriForFile(
                this,
                FILE_PROVIDER_AUTHORITY,
                createImageFile()
        )
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.FRANCE).format(Date())
        val imageFileName = "$JPEG_PREFIX$timeStamp"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(imageFileName, JPG_EXTENSION, storageDir)
        currentPhotoPath = image.absolutePath
        return image
    }

    private fun getPicture(): Bitmap {
        val bmOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            BitmapFactory.decodeFile(currentPhotoPath, this)
            val scaleFactor = outWidth / blurViewGroup.width
            inJustDecodeBounds = false
            inSampleSize = scaleFactor
        }
        return BitmapFactory.decodeFile(currentPhotoPath, bmOptions)
    }

    private fun saveModifiedBitmap(bitmap: Bitmap) {
        val path = Environment.getExternalStoragePublicDirectory("${Environment.DIRECTORY_PICTURES}${File.separator}$APP_DIRECTORY_PICTURES")
        path.mkdirs()
        val file = File(path, "lol.jpg")
        if (file.exists()) file.delete()
        try {
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.flush()
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
