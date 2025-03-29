package com.pgerasymchuk.sensors.activity

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pgerasymchuk.sensors.BrightnessHandler
import com.pgerasymchuk.sensors.SensorMath

class GForceActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var brightnessHandler: BrightnessHandler
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var gForceView: GForceView

    private val accelerationData = mutableListOf<Float>()
    private val maxDataPoints = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        brightnessHandler = BrightnessHandler(this)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: run {
            Toast.makeText(this, "Accelerometer is not available on this device", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        gForceView = GForceView(this)
        setContentView(gForceView)
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
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

            val gForce = SensorMath.gForce(x, y, z)

            accelerationData.add(gForce)
            if (accelerationData.size > maxDataPoints) {
                accelerationData.removeAt(0)
            }

            gForceView.updateGForce(gForce, accelerationData.toList())
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    inner class GForceView(context: Context) : View(context) {
        private val meterPaint = Paint().apply {
            color = Color.BLUE
            style = Paint.Style.STROKE
            strokeWidth = 8f
            isAntiAlias = true
        }

        private val needlePaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }

        private val graphPaint = Paint().apply {
            color = Color.GREEN
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }

        private val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 40f
            isAntiAlias = true
        }

        private val gridPaint = Paint().apply {
            color = Color.GRAY
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }

        private var currentGForce = 0f
        private var dataPoints = listOf<Float>()
        private val path = Path()

        fun updateGForce(gForce: Float, data: List<Float>) {
            currentGForce = gForce
            dataPoints = data
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val centerX = width / 2f
            val meterY = height / 4f
            val graphY = height * 3/4f
            val radius = Math.min(width, height) / 4f

            canvas.drawArc(
                centerX - radius, meterY - radius,
                centerX + radius, meterY + radius,
                180f, 180f, false, meterPaint
            )

            val maxG = 4
            val maxGFloat = maxG.toFloat()

            for (g in 0..maxG) {
                val angle = 180f + (g * 45f)
                val x = centerX + (radius * Math.cos(Math.toRadians(angle.toDouble()))).toFloat()
                val y = meterY + (radius * Math.sin(Math.toRadians(angle.toDouble()))).toFloat()
                canvas.drawLine(x, y, x * 0.95f + centerX * 0.05f, y, meterPaint)

                val textX = x * 0.85f + centerX * 0.15f
                val textY = y  + if (g > 0) 40f else -20f
                canvas.drawText("${g}G", textX, textY, textPaint)
            }

            val needleAngle = 180f + (currentGForce.coerceIn(0f, maxGFloat) * 45f)
            val needleX = centerX + (radius * 0.8f * Math.cos(Math.toRadians(needleAngle.toDouble()))).toFloat()
            val needleY = meterY + (radius * 0.8f * Math.sin(Math.toRadians(needleAngle.toDouble()))).toFloat()
            canvas.drawLine(centerX, meterY, needleX, needleY, needlePaint)

            val gForceText = "G-Force: ${String.format("%.2f", currentGForce)} G"
            canvas.drawText(gForceText, centerX - textPaint.measureText(gForceText) / 2, meterY + radius + 50f, textPaint)

            if (dataPoints.isNotEmpty()) {
                val graphHeight = height / 4f
                val graphWidth = width * 0.9f
                val stepX = graphWidth / (maxDataPoints - 1)

                path.reset()
                path.moveTo(
                    centerX - graphWidth / 2,
                    graphY - (dataPoints[0].coerceIn(0f, maxGFloat) / maxGFloat) * graphHeight
                )

                for (i in 1 until dataPoints.size) {
                    val x = centerX - graphWidth / 2 + i * stepX
                    val y = graphY - (dataPoints[i].coerceIn(0f, maxGFloat) / maxGFloat) * graphHeight
                    path.lineTo(x, y)
                }
                canvas.drawPath(path, graphPaint)

                for (i in 0 until maxG + 1) {
                    val y = graphY - graphHeight / maxG * i
                    canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
                    canvas.drawText("${i}G", 10f, y + 30f, textPaint)
                }

                canvas.drawLine(0f, graphY, width.toFloat(), graphY, meterPaint)
                canvas.drawText("Time", 0.8f * width, graphY + 40f, textPaint)
                canvas.drawText("G-Force", 0f, graphY - graphHeight - 30f, textPaint)
            }
        }
    }
}