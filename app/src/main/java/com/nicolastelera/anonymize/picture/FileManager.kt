package com.nicolastelera.anonymize.picture

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.content.FileProvider
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

    private var currentPhotoPath: String? = null

    fun createPhotoUri(): Uri = FileProvider.getUriForFile(
            context,
        FILE_PROVIDER_AUTHORITY,
            createImageFile()
    )

    fun getPictureFromPath(targetWidth: Int): SrcContainer = getPictureFromUri(
            Uri.fromFile(File(currentPhotoPath)),
            targetWidth
    )

    fun getPictureFromUri(uri: Uri, targetWidth: Int): SrcContainer {
        val src = BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
        val scaledBitmap = getOrientationFromUri(uri).let { orientation ->
            if (orientation <= 0) createScaledBitmap(src, targetWidth)
            else createRotatedBitmap(src, targetWidth, orientation)
        }
        return SrcContainer(src, scaledBitmap)
    }

    private fun createRotatedBitmap(src: Bitmap, targetWidth: Int, orientation: Int): Bitmap {
        val matrix = Matrix().apply { postRotate(orientation.toFloat()) }
        val srcBitmap = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, false)
        return createScaledBitmap(srcBitmap, targetWidth)
    }

    private fun createScaledBitmap(src: Bitmap, targetWidth: Int): Bitmap {
        return (src.width.toFloat() / targetWidth).let { scaleFactor ->
            Bitmap.createScaledBitmap(
                    src,
                    (src.width.toFloat() / scaleFactor).toInt(),
                    (src.height.toFloat() / scaleFactor).toInt(),
                    false
            )
        }
    }

    fun saveModifiedBitmap(bitmap: Bitmap?): Boolean {
        return bitmap?.let {
            val type = "${Environment.DIRECTORY_PICTURES}${File.separator}$APP_DIRECTORY_PICTURES"
            val path = Environment.getExternalStoragePublicDirectory(type)
            path.mkdirs()
            val file = File(path, "${createUniqueFileName()}$JPG_EXTENSION")
            if (file.exists()) file.delete()
            try {
                val out = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                out.flush()
                out.close()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        } ?: false
    }

    private fun createImageFile(): File {
        val currentPhotoName = createUniqueFileName()
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(currentPhotoName,
            JPG_EXTENSION, storageDir)
        currentPhotoPath = image.absolutePath
        return image
    }

    private fun createUniqueFileName(): String {
        val timeStamp = SimpleDateFormat(
            DATE_FORMAT_PATTERN, Locale.FRANCE).format(Date())
        return "$JPEG_PREFIX$timeStamp"
    }

    @SuppressLint("Recycle")
    private fun getOrientationFromUri(uri: Uri): Int {
        val cursor = context.contentResolver.query(
                uri,
                arrayOf(MediaStore.Images.ImageColumns.ORIENTATION),
                null,
                null,
                null
        ) ?: return 0

        val orientation = with(cursor) {
            if (columnCount != 1) return -1
            moveToFirst()
            getInt(0)
        }

        cursor.close()
        return orientation
    }
}
