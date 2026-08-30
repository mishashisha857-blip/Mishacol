package com.example.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StepSensorManager(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val stepCounterSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val stepDetectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    private val _liveSteps = MutableStateFlow(0)
    val liveSteps: StateFlow<Int> = _liveSteps.asStateFlow()

    private val _isSensorAvailable = MutableStateFlow(false)
    val isSensorAvailable: StateFlow<Boolean> = _isSensorAvailable.asStateFlow()

    private var initialStepCount: Int = -1

    fun startListening() {
        if (sensorManager == null) {
            _isSensorAvailable.value = false
            return
        }

        var registered = false
        if (stepCounterSensor != null) {
            registered = sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI)
        }
        if (!registered && stepDetectorSensor != null) {
            registered = sensorManager.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_UI)
        }
        _isSensorAvailable.value = registered
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
    }

    fun addManualSteps(count: Int) {
        _liveSteps.value += count
    }

    fun setSteps(count: Int) {
        _liveSteps.value = count
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            val totalStepsSinceBoot = event.values[0].toInt()
            if (initialStepCount < 0) {
                initialStepCount = totalStepsSinceBoot
            }
            val delta = totalStepsSinceBoot - initialStepCount
            if (delta >= 0) {
                _liveSteps.value = delta
            }
        } else if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
            _liveSteps.value += 1
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    companion object {
        /**
         * Calculates calories burned based on step count and user body weight
         * Formula: 1 step burns approx 0.04 kcal for a 70kg person, scaled linearly
         */
        fun calculateBurnedCalories(steps: Int, weightKg: Float): Int {
            val factor = (weightKg / 70f).coerceIn(0.6f, 1.8f)
            return (steps * 0.04f * factor).toInt()
        }

        /**
         * Calculates approximate distance in km from step count
         * Average step length = 0.75 meters
         */
        fun calculateDistanceKm(steps: Int, heightCm: Float = 175f): Float {
            val stepLengthMeters = (heightCm * 0.415f) / 100f
            val distanceMeters = steps * stepLengthMeters
            return distanceMeters / 1000f
        }
    }
}
