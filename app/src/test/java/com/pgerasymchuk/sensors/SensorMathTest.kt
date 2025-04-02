package com.pgerasymchuk.sensors

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorMathTest {
    private val g = SensorMath.STANDARD_GRAVITY

    @Test
    fun tilt_flatOnTable_isLevel() {
        val tilt = SensorMath.tilt(0f, 0f, g)

        assertEquals(0f, tilt.pitch, 0.01f)
        assertEquals(0f, tilt.roll, 0.01f)
        assertTrue(SensorMath.isLevel(tilt))
    }

    @Test
    fun tilt_standingUpright_rollsNinetyDegrees() {
        val tilt = SensorMath.tilt(0f, g, 0f)

        assertEquals(90f, tilt.roll, 0.01f)
        assertFalse(SensorMath.isLevel(tilt))
    }

    @Test
    fun tilt_leaningOnSide_pitchesNinetyDegrees() {
        assertEquals(-90f, SensorMath.tilt(g, 0f, 0f).pitch, 0.01f)
        assertEquals(90f, SensorMath.tilt(-g, 0f, 0f).pitch, 0.01f)
    }

    @Test
    fun tilt_fortyFiveDegrees() {
        val tilt = SensorMath.tilt(0f, 1f, 1f)

        assertEquals(45f, tilt.roll, 0.01f)
    }

    @Test
    fun gForce_atRest_isOneG() {
        assertEquals(1f, SensorMath.gForce(0f, 0f, g), 1e-4f)
        assertEquals(1f, SensorMath.gForce(g / 2, g / 2, g * 0.70710677f), 1e-3f)
    }

    @Test
    fun gForce_freeFall_isZero() {
        assertEquals(0f, SensorMath.gForce(0f, 0f, 0f), 0f)
    }

    @Test
    fun heading_isNormalisedToFullCircle() {
        assertEquals(0f, SensorMath.headingDegrees(0f), 0.01f)
        assertEquals(90f, SensorMath.headingDegrees((Math.PI / 2).toFloat()), 0.01f)
        assertEquals(270f, SensorMath.headingDegrees((-Math.PI / 2).toFloat()), 0.01f)
        assertEquals(180f, SensorMath.headingDegrees(Math.PI.toFloat()), 0.01f)
    }

    @Test
    fun lowPass_movesTowardsInputByAlpha() {
        val output = floatArrayOf(0f, 10f, -4f)

        SensorMath.lowPass(floatArrayOf(10f, 10f, 4f), output, 0.25f)

        assertArrayEquals(floatArrayOf(2.5f, 10f, -2f), output, 1e-6f)
    }

    @Test
    fun altitude_atStandardPressure_isSeaLevel() {
        assertEquals(0f, SensorMath.altitudeMeters(SensorMath.STANDARD_PRESSURE_HPA), 0.01f)
    }

    @Test
    fun altitude_matchesStandardAtmosphere() {
        // ISA: ~898.7 hPa at 1000 m, ~795.0 hPa at 2000 m
        assertEquals(1000f, SensorMath.altitudeMeters(898.75f), 5f)
        assertEquals(2000f, SensorMath.altitudeMeters(794.95f), 10f)
    }

    @Test
    fun altitude_belowSeaLevel_isNegative() {
        assertTrue(SensorMath.altitudeMeters(1030f) < 0f)
    }

    @Test
    fun brightness_followsLightLevels() {
        assertEquals(0.1f, SensorMath.brightnessForLux(10f), 0f)
        assertEquals(0.2f, SensorMath.brightnessForLux(200f), 0f)
        assertEquals(0.5f, SensorMath.brightnessForLux(350f), 0f)
        assertEquals(0.7f, SensorMath.brightnessForLux(800f), 0f)
        assertEquals(1.0f, SensorMath.brightnessForLux(20_000f), 0f)
    }

    @Test
    fun forecast_usesPressureAndTemperature() {
        assertEquals("No forecast available", SensorMath.forecast(null, null))
        assertEquals("Clear skies likely", SensorMath.forecast(null, 1020f))
        assertEquals("Rain possible", SensorMath.forecast(25f, 995f))
        assertEquals("Snow possible", SensorMath.forecast(-3f, 990f))
        assertEquals("Hot weather", SensorMath.forecast(35f, 1010f))
    }

    @Test
    fun forecast_withoutTemperatureSensor_doesNotGuessSnow() {
        assertEquals("Partly cloudy", SensorMath.forecast(null, 990f))
    }
}
