package com.mustafa.ai

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 80, 48, 48)
        }

        val title = TextView(this).apply {
            text = "Mustafa AI"
            textSize = 28f
        }
        val message = TextView(this).apply {
            text = "Yapay zeka uygulaman hazır. Buradan geliştirmeye devam edeceğiz."
            textSize = 18f
            setPadding(0, 32, 0, 0)
        }

        layout.addView(title)
        layout.addView(message)
        setContentView(layout)
    }
}
