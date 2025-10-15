package com.inazumadraft.ui

import android.content.Intent
import android.os.Bundle
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.inazumadraft.R

class MainActivity : AppCompatActivity() {

    private val allSeasons = listOf(
        "S1",
        "S2",
        "S3",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val container = findViewById<LinearLayout>(R.id.seasonContainer)
        val selectedSeasons = mutableSetOf<String>()

        // Crear checkboxes dinámicamente
        allSeasons.forEach { season ->
            val cb = CheckBox(this).apply {
                text = season
                setTextColor(resources.getColor(android.R.color.white))
                setOnCheckedChangeListener { _, checked ->
                    if (checked) selectedSeasons.add(season) else selectedSeasons.remove(season)
                }
            }
            container.addView(cb)
        }

        val startButton = findViewById<MaterialButton>(R.id.btnStart)
        startButton.setOnClickListener {
            if (selectedSeasons.isEmpty()) {
                // Si no se selecciona nada, se usa todo
                selectedSeasons.addAll(allSeasons)
            }

            val intent = Intent(this, DraftActivity::class.java)
            intent.putStringArrayListExtra("selectedSeasons", ArrayList(selectedSeasons))
            startActivity(intent)
        }
    }
}
