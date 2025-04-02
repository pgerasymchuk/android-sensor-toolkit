package com.pgerasymchuk.sensors.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.pgerasymchuk.sensors.R
import com.pgerasymchuk.sensors.BrightnessHandler

class MainActivity : AppCompatActivity() {
    private lateinit var brightnessHandler: BrightnessHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        brightnessHandler = BrightnessHandler(this)

        val buttonNavigate = findViewById<Button>(R.id.buttonInclinationActivity)
        buttonNavigate.setOnClickListener {
            val intent = Intent(this, LevelActivity::class.java)
            startActivity(intent)
        }

        val buttonCompass = findViewById<Button>(R.id.buttonCompassActivity)
        buttonCompass.setOnClickListener {
            val intent = Intent(this, CompassActivity::class.java)
            startActivity(intent)
        }

        val buttonWeather = findViewById<Button>(R.id.buttonWeatherActivity)
        buttonWeather.setOnClickListener {
            val intent = Intent(this, BarometerActivity::class.java)
            startActivity(intent)
        }

        val buttonGForce = findViewById<Button>(R.id.buttonGForceActivity)
        buttonGForce.setOnClickListener {
            val intent = Intent(this, GForceActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        brightnessHandler.start()
    }

    override fun onPause() {
        super.onPause()
        brightnessHandler.stop()
    }
}