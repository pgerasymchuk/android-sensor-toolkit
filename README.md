# 🧭 Android Sensor Toolkit

A small Android app in Kotlin that turns the phone's hardware sensors into four everyday tools: a **bubble level**, a **compass**, a **barometer** with altitude and a simple forecast, and a **G-force meter** with a live graph. Every screen is drawn on a custom `Canvas` view and also adapts the screen brightness to the ambient light.

🎓 Built for the Android Development course at Igor Sikorsky Kyiv Polytechnic Institute (3rd year, spring 2025).

## 📱 Screens

| Screen | Sensors | What it shows |
|--------|---------|---------------|
| Level | accelerometer | pitch and roll, two "bubbles" that move with the tilt; the background turns green when the phone lies flat (within ±1°) |
| Compass | accelerometer + magnetometer | a rotating dial with ticks every 15°, cardinal and intercardinal points, and the heading in degrees |
| Barometer | pressure, ambient temperature | pressure in hPa, temperature (on devices that have the sensor), estimated altitude and a rule-based forecast |
| G-Force | accelerometer | a 0–4 G gauge with a needle, the current value and a graph of the last 100 samples |

On every screen `BrightnessHandler` listens to the **light sensor** and sets the window brightness from the lux level (≤50 → 10 %, ≤200 → 20 %, ≤500 → 50 %, ≤1000 → 70 %, brighter → 100 %), and holds a **proximity** wake lock that switches the display off while the sensor is covered. Listeners are registered in `onResume` and removed in `onPause`, and missing sensors are handled without crashing.

## 📐 The math

All formulas live in `SensorMath.kt`, a plain Kotlin object without Android dependencies, so they are covered by unit tests.

**Level.** At rest the accelerometer measures only gravity, so the tilt follows from the direction of that vector:

```
roll  = atan2(y, z)
pitch = atan2(-x, √(y² + z²))
```

Each bubble is shifted by `angle / 90°` of half the view size, so ±90° moves it to the edge.

**Compass.** Accelerometer and magnetometer readings are smoothed with an exponential low-pass filter, `out += α · (in − out)` with α = 0.25, which removes most of the jitter. `SensorManager.getRotationMatrix` combines gravity and the magnetic field into a rotation matrix (the result is used only when the matrix is valid), `getOrientation` extracts the azimuth, and it is normalised to a 0–360° heading. The dial is rotated by `−heading`, so "N" always points to magnetic north.

**G-force.** The accelerometer includes gravity, so a phone lying still reads 1 G:

```
G = √(x² + y² + z²) / 9.80665
```

The gauge maps 0–4 G onto a half circle (45° per G).

**Barometric altitude.** Altitude comes from the international barometric formula, the same one `SensorManager.getAltitude` uses:

```
h = 44330 · (1 − (p / p₀)^(1 / 5.255))      p₀ = 1013.25 hPa
```

It gives roughly 8 m per hPa near sea level. The forecast uses simple thresholds: above 1015 hPa → clear skies, below 1000 hPa → rain (warm) or snow (cold), otherwise based on temperature.

## 🧪 Tests

```bash
./gradlew testDebugUnitTest
```

`SensorMathTest` checks tilt angles for known orientations, G-force at rest and in free fall, heading normalisation, the low-pass filter, altitude against the standard atmosphere (≈1000 m at 898.7 hPa), brightness levels and the forecast rules.

## 🖥️ Build and run

Requirements: Android Studio (or JDK 17+ with the Android SDK, platform 35) and a real device with Android 7.0 (API 24) or newer. An emulator works too, with values set in *Extended controls → Virtual sensors*.

```bash
git clone https://github.com/pgerasymchuk/android-sensor-toolkit.git
cd android-sensor-toolkit
./gradlew installDebug
```

## 🗂 Project structure

```
app/src/main/java/com/pgerasymchuk/sensors/
├── activity/              MainActivity (menu) and the four sensor screens
├── BrightnessHandler.kt   light-based brightness + proximity wake lock
└── SensorMath.kt          tilt, heading, G-force, altitude, filters
app/src/main/res/          menu layout, strings, theme
app/src/test/              unit tests for SensorMath
```

## 🛠️ Tech stack

- Kotlin, Android SDK 35 (min SDK 24), AppCompat
- `SensorManager` (accelerometer, magnetometer, pressure, ambient temperature, light, proximity), `PowerManager` wake locks
- Custom views drawn with `Canvas`, `Paint` and `Path`
- Gradle Kotlin DSL with a version catalog, JUnit 4
