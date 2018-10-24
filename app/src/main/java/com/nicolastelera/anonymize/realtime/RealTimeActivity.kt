package com.nicolastelera.anonymize.realtime

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.nicolastelera.anonymize.R

class RealTimeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_real_time)
        savedInstanceState ?: supportFragmentManager.beginTransaction()
                .replace(R.id.cameraView, CameraFragment.newInstance(this))
                .commit()
    }
}
