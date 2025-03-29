package com.pgerasymchuk.sensors.activity

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.pgerasymchuk.sensors.BrightnessHandler
import com.pgerasymchuk.sensors.SensorMath

class LevelActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var brightnessHandler: BrightnessHandler
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var inclinationView: InclinationView
    private lateinit var layout: ConstraintLayout

    private var pitch = 0f
    private var roll = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        brightnessHandler = BrightnessHandler(this)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: run {
            Toast.makeText(this, "Accelerometer is not available on this device", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        inclinationView = InclinationView(this)
        layout = ConstraintLayout(this)
        layout.addView(inclinationView)
        setContentView(layout)
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        brightnessHandler.start()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        brightnessHandler.stop()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val tilt = SensorMath.tilt(x, y, z)
            pitch = tilt.pitch
            roll = tilt.roll

            val isFlat = SensorMath.isLevel(tilt)

            if (isFlat) {
                layout.setBackgroundColor(Color.GREEN)
            } else {
                layout.setBackgroundColor(Color.WHITE)
            }

            inclinationView.updateAngles(pitch, roll)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    inner class InclinationView(context: Context) : View(context) {
        private val pitchCirclePaint = Paint().apply {
            color = Color.BLUE
            style = Paint.Style.FILL
        }

        private val rollCirclePaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.FILL
        }

        private val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 40f
        }

        private var currentPitch = 0f
        private var currentRoll = 0f

        fun updateAngles(pitch: Float, roll: Float) {
            currentPitch = pitch
            currentRoll = roll
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val centerX = width / 2f
            val centerY = height / 2f
            val radius = 100f

            val maxOffsetX = width / 2f
            val maxOffsetY = height / 2f

            val offsetX = (currentPitch / 90f) * maxOffsetX
            val offsetY = (currentRoll / 90f) * maxOffsetY

            canvas.drawCircle(centerX - offsetX, centerY, radius, pitchCirclePaint)
            canvas.drawCircle(centerX, centerY + offsetY, radius, rollCirclePaint)

            canvas.drawText("Pitch: ${currentPitch.toInt()}°", 50f, 100f, textPaint)
            canvas.drawText("Roll: ${currentRoll.toInt()}°", 50f, 150f, textPaint)
        }
    }
}