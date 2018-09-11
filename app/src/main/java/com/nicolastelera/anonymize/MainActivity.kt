package com.nicolastelera.anonymize

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_TAKE_PHOTO = 1
        private const val FILE_PROVIDER_AUTHORITY = "com.nicolastelera.anonymize.fileprovider"
        private const val DATE_FORMAT_PATTERN = "yyyyMMdd_HHmmss"
        private const val JPEG_PREFIX = "JPEG_"
        private const val JPG_EXTENSION = ".jpg"
    }

    private var currentPhotoPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pictureButton.setOnClickListener {
            with(Intent(MediaStore.ACTION_IMAGE_CAPTURE)) {
                if (resolveActivity(packageManager) != null) {
                    putExtra(MediaStore.EXTRA_OUTPUT, createPhotoUri())
                    startActivityForResult(this, REQUEST_TAKE_PHOTO)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK) {
            blurViewGroup.setImageToBlur(getPicture())
            saveButton.visibility = View.VISIBLE
        }
        super.onActivityResult(requestCode, resultCode, data)
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
}
