package com.example.facedetection

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Make sure we use AppCompat Activity and theme
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
