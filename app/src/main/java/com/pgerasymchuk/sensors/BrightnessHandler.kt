package com.pgerasymchuk.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import androidx.appcompat.app.AppCompatActivity

class BrightnessHandler(private val activity: AppCompatActivity) : SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private var proximitySensor: Sensor? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var isInitialized = false

    private fun initialize() {
        sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        val powerManager = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
            wakeLock = powerManager.newWakeLock(
                PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                "sensors:ProximityWakeLock"
            )
        }

        isInitialized = true
    }

    fun start() {
        if (!isInitialized) initialize()

        lightSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        wakeLock?.let {
            if (!it.isHeld) it.acquire(10 * 60 * 1000L)
        }
    }

    fun stop() {
        if (isInitialized) {
            sensorManager.unregisterListener(this)
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_LIGHT -> {
                val lux = event.values[0]
                adjustBrightness(lux)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun adjustBrightness(lux: Float) {
        val window = activity.window
        val layoutParams = window.attributes
        layoutParams.screenBrightness = SensorMath.brightnessForLux(lux)
        window.attributes = layoutParams
    }
}
