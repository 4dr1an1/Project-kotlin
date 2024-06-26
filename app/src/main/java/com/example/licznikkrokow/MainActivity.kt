package com.example.licznikkrokow

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var isRunning = false
    private var initialStepCount = -1
    private var steps by mutableIntStateOf(0)
    private var totalSteps by mutableIntStateOf(0)
    private var accumulatedSteps = 0

    private var _elapsedTime = mutableLongStateOf(0L)
    private val elapsedTime: State<Long> = _elapsedTime
    private var previousElapsedTime = 0L
    private var startTime = 0L

    private var timerJob: Job? = null

    private var stepGoal by mutableIntStateOf(5000)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        setContent {
            LicznikKrokowTheme {
                MainScreen(
                    steps = totalSteps,
                    elapsedTime = elapsedTime.value,
                    stepGoal = stepGoal,
                    onStart = { startCounting() },
                    onStop = { stopCounting() },
                    onReset = { resetCounting() },
                    onGoalSet = { goal -> stepGoal = goal }
                )
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                    1
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (isRunning) {
            startCounting()
        }
    }

    override fun onStop() {
        super.onStop()
        stopCounting()
    }

    private fun startCounting() {
        if (!isRunning) {
            isRunning = true
            initialStepCount = -1
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_FASTEST)
            startTime = System.currentTimeMillis()
            startTimer()
        }
    }

    private fun stopCounting() {
        if (isRunning) {
            isRunning = false
            sensorManager.unregisterListener(this)
            timerJob?.cancel()
            previousElapsedTime += System.currentTimeMillis() - startTime
            accumulatedSteps += steps
        }
    }

    private fun resetCounting() {
        if (!isRunning) {
            steps = 0
            totalSteps = 0
            accumulatedSteps = 0
            initialStepCount = -1
            _elapsedTime.longValue = 0L
            previousElapsedTime = 0L
        }
    }

    private fun startTimer() {
        timerJob = CoroutineScope(Dispatchers.Main).launch {
            while (isRunning) {
                delay(1000L)
                _elapsedTime.longValue = previousElapsedTime + (System.currentTimeMillis() - startTime)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onSensorChanged(event: SensorEvent) {
        if (isRunning) {
            if (initialStepCount == -1) {
                initialStepCount = event.values[0].toInt()
            }
            steps = event.values[0].toInt() - initialStepCount
            totalSteps = accumulatedSteps + steps
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    @SuppressLint("DefaultLocale")
    @Composable
    fun MainScreen(
        steps: Int,
        elapsedTime: Long,
        stepGoal: Int,
        onStart: () -> Unit,
        onStop: () -> Unit,
        onReset: () -> Unit,
        onGoalSet: (Int) -> Unit
    ) {
        var goalInput by remember { mutableStateOf(stepGoal.toString()) }
        val distance = remember(steps) { steps * 0.8 }
        val focusManager = LocalFocusManager.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Licznik kroków",
                style = MaterialTheme.typography.h4,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Kroki: $steps",
                style = MaterialTheme.typography.h5,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(16.dp))
            PieChartView(steps = steps, goal = stepGoal)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Dystans: ${String.format("%.1f", distance)} m",
                style = MaterialTheme.typography.h5,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Czas: ${elapsedTime / 60000}:${String.format("%02d", (elapsedTime / 1000) % 60)}",
                style = MaterialTheme.typography.h5,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(32.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Button(onClick = onStart, modifier = Modifier.weight(1f)) {
                    Text(text = "Start")
                }
                Button(onClick = onStop, modifier = Modifier.weight(1f)) {
                    Text(text = "Stop")
                }
                Button(onClick = onReset, modifier = Modifier.weight(1f)) {
                    Text(text = "Reset")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    value = goalInput,
                    onValueChange = { goalInput = it },
                    label = { Text("Cel kroków") },
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = {
                    onGoalSet(goalInput.toIntOrNull() ?: stepGoal)
                    focusManager.clearFocus()
                }, modifier = Modifier.padding(start = 8.dp)) {
                    Text("Zatwierdź")
                }
            }
        }
    }

    @Composable
    fun PieChartView(steps: Int, goal: Int) {
        val sweepAngle = (steps / goal.toFloat()) * 360f

        Canvas(modifier = Modifier
            .size(200.dp)
            .clip(CircleShape)) {
            drawArc(
                color = Color.Gray,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = true
            )
            drawArc(
                color = Color.Blue,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = true
            )
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        LicznikKrokowTheme {
            MainScreen(
                steps = 0,
                elapsedTime = 0L,
                stepGoal = 5000,
                onStart = {},
                onStop = {},
                onReset = {},
                onGoalSet = {}
            )
        }
    }
}
