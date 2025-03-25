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
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class CompassActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var brightnessHandler: BrightnessHandler
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var magnetometer: Sensor
    private lateinit var compassView: CompassView

    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private var hasAccelerometerReading = false
    private var hasMagnetometerReading = false

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        brightnessHandler = BrightnessHandler(this)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if (accelerometerSensor == null || magnetometerSensor == null) {
            Toast.makeText(this, "Compass sensors are not available on this device", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        accelerometer = accelerometerSensor
        magnetometer = magnetometerSensor

        compassView = CompassView(this)
        setContentView(compassView)
    }

    override fun onResume() {
        super.onResume()

        sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_GAME
        )

        sensorManager.registerListener(
            this,
            magnetometer,
            SensorManager.SENSOR_DELAY_GAME
        )

        brightnessHandler.start()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        brightnessHandler.stop()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            smooth(event.values, accelerometerReading, hasAccelerometerReading)
            hasAccelerometerReading = true
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            smooth(event.values, magnetometerReading, hasMagnetometerReading)
            hasMagnetometerReading = true
        }

        if (hasAccelerometerReading && hasMagnetometerReading) {
            updateOrientationAngles()
        }
    }

    private fun smooth(values: FloatArray, reading: FloatArray, hasPrevious: Boolean) {
        if (hasPrevious) {
            SensorMath.lowPass(values, reading, FILTER_ALPHA)
        } else {
            System.arraycopy(values, 0, reading, 0, reading.size)
        }
    }

    private fun updateOrientationAngles() {
        val success = SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )
        if (!success) return
        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        compassView.updateDirection(SensorMath.headingDegrees(orientationAngles[0]))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    companion object {
        private const val FILTER_ALPHA = 0.25f
    }

    inner class CompassView(context: Context) : View(context) {
        private val compassCirclePaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }

        private val compassTickPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }

        private val cardinalPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            textSize = 60f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        private val directionArrowPaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        private val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 60f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        private var heading = 0f
        private val arrowPath = Path()

        fun updateDirection(heading: Float) {
            this.heading = heading
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val centerX = width / 2f
            val centerY = height / 2f
            val radius = centerX.coerceAtMost(centerY) * 0.8f

            canvas.save()
            canvas.translate(centerX, centerY)
            canvas.rotate(-heading)
            canvas.drawCircle(0f, 0f, radius, compassCirclePaint)

            for (i in 0 until 360 step 15) {
                val tickLength = if (i % 90 == 0) 40f else if (i % 45 == 0) 30f else 20f
                val angle = Math.toRadians(i.toDouble())
                val startX = radius * sin(angle).toFloat()
                val startY = -radius * cos(angle).toFloat()
                val endX = (radius - tickLength) * sin(angle).toFloat()
                val endY = -(radius - tickLength) * cos(angle).toFloat()

                canvas.drawLine(startX, startY, endX, endY, compassTickPaint)
            }

            val cardinalOffset = 0.8f * radius
            canvas.drawText("N", 0f, -cardinalOffset, cardinalPaint)
            canvas.drawText("E", cardinalOffset, 15f, cardinalPaint)
            canvas.drawText("S", 0f, cardinalOffset, cardinalPaint)
            canvas.drawText("W", -cardinalOffset, 15f, cardinalPaint)

            val diagonalOffset = 0.6f * radius
            canvas.drawText("NE", diagonalOffset, -diagonalOffset, cardinalPaint)
            canvas.drawText("SE", diagonalOffset, diagonalOffset, cardinalPaint)
            canvas.drawText("SW", -diagonalOffset, diagonalOffset, cardinalPaint)
            canvas.drawText("NW", -diagonalOffset, -diagonalOffset, cardinalPaint)

            canvas.restore()
            canvas.save()
            canvas.translate(centerX, centerY)

            val centerLineOffset = 0.5f * radius
            canvas.drawLine(0f, centerLineOffset, 0f, -centerLineOffset, compassTickPaint)
            canvas.drawLine(-centerLineOffset, 0f, centerLineOffset, 0f, compassTickPaint)

            arrowPath.reset()
            arrowPath.moveTo(0f, -radius)
            arrowPath.lineTo(20f, -radius * 0.8f)
            arrowPath.lineTo(0f, -radius * 0.85f)
            arrowPath.lineTo(-20f, -radius * 0.8f)
            arrowPath.close()
            canvas.drawPath(arrowPath, directionArrowPaint)

            val headingText = "Heading: ${heading.roundToInt() % 360}°"
            canvas.drawText(headingText, 0f, 1.5f * radius, textPaint)
        }
    }
}