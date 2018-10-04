package com.nicolastelera.anonymize

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_WRITE_EXTERNAL_STORAGE = 1664
        private const val REQUEST_TAKE_PHOTO = 1
        private const val REQUEST_LOAD_PHOTO = 2
        private const val TYPE_IMAGE = "image/*"
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
            val bitmap = when (requestCode) {
                REQUEST_TAKE_PHOTO -> fileManager.getPictureFromPath(blurViewGroup.width)
                REQUEST_LOAD_PHOTO -> data?.let {
                    fileManager.getPictureFromUri(data.data, blurViewGroup.width)
                }
                else -> null
            }
            bitmap?.let { updateViewWithImage(it) }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun checkForUserPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_WRITE_EXTERNAL_STORAGE)
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_WRITE_EXTERNAL_STORAGE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] != PERMISSION_GRANTED)) {
                    finish()
                }
                return
            }
        }
    }

    private fun initButtonsListeners() {
        pictureButton.setOnClickListener {
            with(Intent(MediaStore.ACTION_IMAGE_CAPTURE)) {
                putExtra(MediaStore.EXTRA_OUTPUT, fileManager.createPhotoUri())
                startActivityForResult(this, REQUEST_TAKE_PHOTO)
            }
        }

        importButton.setOnClickListener {
            with(Intent(Intent.ACTION_PICK)) {
                type = TYPE_IMAGE
                startActivityForResult(this, REQUEST_LOAD_PHOTO)
            }
        }

        saveButton.setOnClickListener {
            blurViewGroup.getFinalImage()?.let { bitmap ->
                fileManager.saveModifiedBitmap(bitmap)
            } ?: Toast.makeText(
                    this,
                    "Erreur lors de la sauvegarde de l'image",
                    Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun updateViewWithImage(container: SrcContainer) {
        blurViewGroup.setImageToBlur(container)
        saveButton.visibility = View.VISIBLE
    }
}
