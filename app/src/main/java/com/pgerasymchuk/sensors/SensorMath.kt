package com.pgerasymchuk.sensors

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Pure math behind the sensor screens, kept free of Android APIs so it can be
 * unit-tested on the JVM.
 */
object SensorMath {
    /** Standard gravity, same value as SensorManager.GRAVITY_EARTH. */
    const val STANDARD_GRAVITY = 9.80665f

    /** Standard sea-level pressure, same value as SensorManager.PRESSURE_STANDARD_ATMOSPHERE. */
    const val STANDARD_PRESSURE_HPA = 1013.25f

    data class Tilt(val pitch: Float, val roll: Float)

    /**
     * Pitch and roll in degrees from the gravity vector reported by the
     * accelerometer while the device is held still.
     */
    fun tilt(x: Float, y: Float, z: Float): Tilt {
        val roll = Math.toDegrees(atan2(y.toDouble(), z.toDouble())).toFloat()
        val pitch = Math.toDegrees(
            atan2(-x.toDouble(), sqrt(y.toDouble() * y + z.toDouble() * z))
        ).toFloat()
        return Tilt(pitch, roll)
    }

    fun isLevel(tilt: Tilt, toleranceDegrees: Float = 1f): Boolean =
        abs(tilt.pitch) < toleranceDegrees && abs(tilt.roll) < toleranceDegrees

    /** Magnitude of the acceleration vector expressed in units of g. */
    fun gForce(x: Float, y: Float, z: Float): Float =
        sqrt(x * x + y * y + z * z) / STANDARD_GRAVITY

    /** Converts an azimuth in radians (as returned by SensorManager.getOrientation) to a 0..360° heading. */
    fun headingDegrees(azimuthRadians: Float): Float {
        val degrees = Math.toDegrees(azimuthRadians.toDouble()).toFloat()
        return (degrees % 360f + 360f) % 360f
    }

    /**
     * Exponential low-pass filter: moves [output] towards [input] by [alpha].
     * Small alpha values give a smoother but slower reaction.
     */
    fun lowPass(input: FloatArray, output: FloatArray, alpha: Float) {
        for (i in output.indices) {
            output[i] += alpha * (input[i] - output[i])
        }
    }

    /**
     * Altitude in metres from atmospheric pressure using the international
     * barometric formula (the one behind SensorManager.getAltitude).
     */
    fun altitudeMeters(pressureHpa: Float, seaLevelHpa: Float = STANDARD_PRESSURE_HPA): Float =
        (44330.0 * (1.0 - (pressureHpa.toDouble() / seaLevelHpa).pow(1.0 / 5.255))).toFloat()

    /** Maps ambient light in lux to a window brightness in 0..1. */
    fun brightnessForLux(lux: Float): Float = when {
        lux <= 50 -> 0.1f     // Very dark
        lux <= 200 -> 0.2f    // Dim
        lux <= 500 -> 0.5f    // Normal indoor
        lux <= 1000 -> 0.7f   // Bright
        else -> 1.0f          // Very bright
    }

    /** Simple rule-based forecast; a missing reading is passed as null. */
    fun forecast(temperature: Float?, pressure: Float?): String {
        if (temperature == null && pressure == null) return "No forecast available"
        return when {
            pressure != null && pressure > 1015 -> "Clear skies likely"
            pressure != null && pressure < 1000 && temperature != null && temperature > 20 -> "Rain possible"
            pressure != null && pressure < 1000 && temperature != null && temperature < 10 -> "Snow possible"
            temperature != null && temperature > 30 -> "Hot weather"
            temperature != null && temperature < 0 -> "Cold weather"
            else -> "Partly cloudy"
        }
    }
}
