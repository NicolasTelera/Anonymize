package com.nicolastelera.anonymize

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class FileManager(private val context: Context) {

    companion object {
        private const val FILE_PROVIDER_AUTHORITY = "com.nicolastelera.anonymize.fileprovider"
        private const val DATE_FORMAT_PATTERN = "yyyyMMdd_HHmmss"
        private const val JPEG_PREFIX = "JPEG_"
        private const val JPG_EXTENSION = ".jpg"
        private const val APP_DIRECTORY_PICTURES = "/Anonymize"
    }

    private var currentPhotoName: String? = null
    private var currentPhotoPath: String? = null

    fun createPhotoUri(): Uri = FileProvider.getUriForFile(
            context,
            FILE_PROVIDER_AUTHORITY,
            createImageFile()
    )

    fun getPicture(targetWidth: Int): Bitmap {

        //TODO
        val uri = Uri.fromFile(File(currentPhotoPath))

        val bmOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            BitmapFactory.decodeFile(currentPhotoPath, this)
            val scaleFactor = outWidth / targetWidth
            inJustDecodeBounds = false
            inSampleSize = scaleFactor
        }
        return BitmapFactory.decodeFile(currentPhotoPath, bmOptions)
    }

    fun getOrientationFromUri(uri: Uri): Int {
        val cursor = context.contentResolver.query(
                uri,
                arrayOf(MediaStore.Images.ImageColumns.ORIENTATION),
                null,
                null,
                null
        )

        val orientation = with(cursor) {
            if (columnCount != 1) return -1
            moveToFirst()
            getInt(0)
        }

        cursor.close()
        return orientation
    }

    fun saveModifiedBitmap(bitmap: Bitmap) {
        val type = "${Environment.DIRECTORY_PICTURES}${File.separator}$APP_DIRECTORY_PICTURES"
        val path = Environment.getExternalStoragePublicDirectory(type)
        path.mkdirs()
        val file = File(path, "$currentPhotoName$JPG_EXTENSION")
        if (file.exists()) file.delete()
        try {
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.flush()
            out.close()
            Toast.makeText(context, context.getString(R.string.picture_saved), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, context.getString(R.string.picture_not_saved), Toast.LENGTH_SHORT).show()
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.FRANCE).format(Date())
        currentPhotoName = "$JPEG_PREFIX$timeStamp"
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(currentPhotoName, JPG_EXTENSION, storageDir)
        currentPhotoPath = image.absolutePath
        return image
    }
}
