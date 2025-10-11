package com.inazumadraft.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.material.button.MaterialButton
import com.inazumadraft.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val startButton = findViewById<MaterialButton>(R.id.btnStart)
        startButton.setOnClickListener {
            val intent = Intent(this, DraftActivity::class.java)
            startActivity(intent)

        }
    }
}
