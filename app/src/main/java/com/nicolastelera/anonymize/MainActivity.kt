package com.nicolastelera.anonymize

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
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
    }

    private val fileManager = FileManager(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkForUserPermission()
        initButtonsListeners()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK) {
            blurViewGroup.setImageToBlur(fileManager.getPicture(blurViewGroup.width))
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
                    putExtra(MediaStore.EXTRA_OUTPUT, fileManager.createPhotoUri())
                    startActivityForResult(this, REQUEST_TAKE_PHOTO)
                }
            }
        }

        importButton.setOnClickListener {
            Toast.makeText(this, "importe picture", Toast.LENGTH_SHORT).show()
        }

        saveButton.setOnClickListener {
            fileManager.saveModifiedBitmap(blurViewGroup.getModifiedImage())
        }
    }
}
