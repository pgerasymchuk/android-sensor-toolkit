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
import androidx.appcompat.app.AppCompatActivity
import com.pgerasymchuk.sensors.BrightnessHandler
import com.pgerasymchuk.sensors.SensorMath

class BarometerActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var brightnessHandler: BrightnessHandler
    private lateinit var sensorManager: SensorManager
    private var temperatureSensor: Sensor? = null
    private var pressureSensor: Sensor? = null
    private lateinit var weatherView: WeatherView

    private var currentTemperature: Float? = null
    private var currentPressure: Float? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        brightnessHandler = BrightnessHandler(this)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        temperatureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

        weatherView = WeatherView(this)
        setContentView(weatherView)

        if (temperatureSensor == null && pressureSensor == null) {
            weatherView.setNoSensorsAvailable()
        }
    }

    override fun onResume() {
        super.onResume()
        temperatureSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        pressureSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        brightnessHandler.start()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        brightnessHandler.stop()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_AMBIENT_TEMPERATURE -> {
                currentTemperature = event.values[0]
                weatherView.updateWeather(currentTemperature, currentPressure)
            }
            Sensor.TYPE_PRESSURE -> {
                currentPressure = event.values[0]
                weatherView.updateWeather(currentTemperature, currentPressure)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    inner class WeatherView(context: Context) : View(context) {
        private val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 40f
            isAntiAlias = true
        }

        private val forecastPaint = Paint().apply {
            color = Color.BLUE
            textSize = 50f
            isAntiAlias = true
        }

        private var temperature: Float? = null
        private var pressure: Float? = null
        private var noSensorsAvailable: Boolean = false

        fun updateWeather(temp: Float?, press: Float?) {
            temperature = temp
            pressure = press
            invalidate()
        }

        fun setNoSensorsAvailable() {
            noSensorsAvailable = true
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val centerX = width / 2f
            val startY = height / 4f

            if (noSensorsAvailable) {
                val message = "No temperature or pressure sensors available"
                canvas.drawText(message, centerX - textPaint.measureText(message) / 2, startY, textPaint)
                return
            }

            val tempText = when {
                temperatureSensor == null -> "Temperature: Not available"
                temperature == null -> "Temperature: waiting for data"
                else -> "Temperature: ${String.format("%.1f", temperature)}°C"
            }
            canvas.drawText(tempText, centerX - textPaint.measureText(tempText) / 2, startY, textPaint)

            val currentPressure = pressure
            val pressureText = when {
                pressureSensor == null -> "Pressure: Not available"
                currentPressure == null -> "Pressure: waiting for data"
                else -> "Pressure: ${String.format("%.1f", currentPressure)} hPa"
            }
            canvas.drawText(pressureText, centerX - textPaint.measureText(pressureText) / 2, startY + 100f, textPaint)

            if (currentPressure != null) {
                val altitude = SensorMath.altitudeMeters(currentPressure)
                val altitudeText = "Altitude: ~${String.format("%.0f", altitude)} m"
                canvas.drawText(altitudeText, centerX - textPaint.measureText(altitudeText) / 2, startY + 200f, textPaint)
            }

            val forecast = "Forecast: ${SensorMath.forecast(temperature, currentPressure)}"
            canvas.drawText(forecast, centerX - forecastPaint.measureText(forecast) / 2, startY + 300f, forecastPaint)
        }
    }
}