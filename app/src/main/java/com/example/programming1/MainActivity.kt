package com.example.programming1

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button
import android.widget.RadioButton
import android.widget.Toast
import android.widget.TextView
import android.widget.RadioGroup
import android.hardware.SensorManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.View
import java.io.File
import java.io.FileWriter
import java.io.RandomAccessFile
import java.util.LinkedList

class MainActivity : AppCompatActivity() {


    private var frequency : Int = 10000 // 100Hz
    private var isRecording : Boolean = false
    private var classBeingRecorded : Int? = null
    private var recordTimeStamp : Long = 0

    private var mAccelerometer: Sensor? = null
    private var mGyroscope: Sensor? = null
    private var mGravity: Sensor? = null
    private var mSensorEventListenerAccelerometer: SensorEventListener? = null
    private var mSensorEventListenerGyroscope: SensorEventListener? = null
    private var mSensorEventListenerGravity: SensorEventListener? = null
    private val tempAccelerometerQueue : MutableList<FloatArray> = mutableListOf()
    private val tempGyroscopeQueue : MutableList<FloatArray> = mutableListOf()
    private val tempGravityQueue : MutableList<FloatArray> = mutableListOf()

    private val sensorsValuesMap = mapOf(
        Sensor.TYPE_LINEAR_ACCELERATION to "linear",
        Sensor.TYPE_GRAVITY to "gravity",
        Sensor.TYPE_GYROSCOPE to "gyro"
    )

    private val radioButtonValuesMap = mapOf(
        R.id.radio_button_0 to 0,
        R.id.radio_button_1 to 1,
        R.id.radio_button_2 to 2,
        R.id.radio_button_3 to 3,
        R.id.radio_button_4 to 4,
        R.id.radio_button_5 to 5,
        R.id.radio_button_6 to 6
    )

    private lateinit var mSensorManager : SensorManager

    private lateinit var mAccelerometerThread: HandlerThread
    private lateinit var mGyroscopeThread: HandlerThread
    private lateinit var mGravityThread: HandlerThread
    private lateinit var mAccelerometerWriterThread: HandlerThread
    private lateinit var mGyroscopeWriterThread: HandlerThread
    private lateinit var mGravityWriterThread: HandlerThread

    private lateinit var mAccelerometerWorker: Handler
    private lateinit var mGyroscopeWorker: Handler
    private lateinit var mGravityWorker: Handler
    private lateinit var mAccelerometerWriterWorker: Handler
    private lateinit var mGyroscopeWriterWorker: Handler
    private lateinit var mGravityWriterWorker: Handler

    private var delayHandler : Handler = Handler(Looper.getMainLooper())
    private lateinit var startRecordingRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        checkFileArchitecture()

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        mGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

        mAccelerometerThread = HandlerThread("Accelerometer Thread")
        mGravityThread = HandlerThread("Gravity Thread")
        mGyroscopeThread = HandlerThread("Gyroscope Thread")
        mAccelerometerWriterThread  = HandlerThread("Accelerometer Writer Thread")
        mGravityWriterThread  = HandlerThread("Gravity Writer Thread")
        mGyroscopeWriterThread  = HandlerThread("GyroscopeWriter Thread")

        mAccelerometerThread.start()
        mGravityThread.start()
        mGyroscopeThread.start()
        mAccelerometerWriterThread.start()
        mGravityWriterThread.start()
        mGyroscopeWriterThread.start()

        mAccelerometerWorker = Handler(mAccelerometerThread.looper)
        mGravityWorker = Handler(mGravityThread.looper)
        mGyroscopeWorker = Handler(mGyroscopeThread.looper)
        mAccelerometerWriterWorker = Handler(mAccelerometerWriterThread.looper)
        mGravityWriterWorker = Handler(mGravityWriterThread.looper)
        mGyroscopeWriterWorker = Handler(mGyroscopeWriterThread.looper)

        mSensorEventListenerAccelerometer = object : SensorEventListener {
            @SuppressLint("DefaultLocale")
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_LINEAR_ACCELERATION) {
                    val values = floatArrayOf(event.values[0], event.values[1], event.values[2])
                    tempAccelerometerQueue.add(values)
                    runOnUiThread {
                        run {
                            findViewById<TextView>(R.id.accelerometer_x).text = String.format("acc_x :%f", values[0])
                            findViewById<TextView>(R.id.accelerometer_y).text = String.format("acc_x :%f", values[1])
                            findViewById<TextView>(R.id.accelerometer_z).text = String.format("acc_x :%f", values[2])
                        }
                    }
                }
            }

            override fun onAccuracyChanged(p0 : Sensor, p1 : Int) {}
        }
        mSensorEventListenerGyroscope =  object : SensorEventListener {
            @SuppressLint("DefaultLocale")
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_GYROSCOPE) {
                    val values = floatArrayOf(event.values[0], event.values[1], event.values[2])
                    //writeResultOnFile(Sensor.TYPE_GYROSCOPE,activityClass,values)
                    tempGyroscopeQueue.add(values)
                    runOnUiThread {
                        run {
                            findViewById<TextView>(R.id.rotation_x).text = String.format("gyr_x :%f", values[0])
                            findViewById<TextView>(R.id.rotation_y).text = String.format("gyr_x :%f", values[1])
                            findViewById<TextView>(R.id.rotation_z).text = String.format("gyr_x :%f", values[2])
                        }
                    }
                }
            }
            override fun onAccuracyChanged(p0 : Sensor, p1 : Int) {}
        }
        mSensorEventListenerGravity = object : SensorEventListener {
            @SuppressLint("DefaultLocale")
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_GRAVITY) {
                    val values = floatArrayOf(event.values[0], event.values[1], event.values[2])
                    //writeResultOnFile(Sensor.TYPE_GRAVITY,activityClass,values)
                    tempGravityQueue.add(values)
                    runOnUiThread {
                        run {
                            findViewById<TextView>(R.id.gravity_x).text = String.format("grv_x :%f", values[0])
                            findViewById<TextView>(R.id.gravity_y).text = String.format("grv_x :%f", values[1])
                            findViewById<TextView>(R.id.gravity_z).text = String.format("grv_x :%f", values[2])
                        }
                    }
                }
            }
            override fun onAccuracyChanged(p0 : Sensor, p1 : Int) {}
        }

        // Runnable made to delay the beginning of the recording of 5 seconds to let the user put the phone in his pocket,
        // or let him stop recording before the first 5 seconds
        startRecordingRunnable = Runnable {
            mSensorManager.registerListener(mSensorEventListenerAccelerometer, mAccelerometer, frequency,mAccelerometerWorker)
            mSensorManager.registerListener(mSensorEventListenerGyroscope, mGyroscope, frequency,mGyroscopeWorker)
            mSensorManager.registerListener(mSensorEventListenerGravity , mGravity, frequency,mGravityWorker)
            mAccelerometerWriterWorker.postDelayed(dataWritingAccelerometer, 5000)
            mGravityWriterWorker.postDelayed(dataWritingGravity, 5000)
            mGyroscopeWriterWorker.postDelayed(dataWritingGyroscope, 5000)
        }

        buttonHandling()
    }

    private fun checkFileArchitecture(){
        val externalFilesDir = this.getExternalFilesDir(null).toString()
        for (classNumber in 0..<radioButtonValuesMap.size){
            for ( sensorName in sensorsValuesMap.values) {
                try {
                    val directoryPath = "$externalFilesDir/49005113/$classNumber"
                    val filePath = "$directoryPath/$sensorName.csv"
                    val directory = File(directoryPath)
                    if (!directory.exists()) {
                        val result = directory.mkdirs()
                        if (result) {
                            Log.i("?","Directories created successfully: ${directory.absolutePath}")
                        } else {
                            Log.i("?","Failed to create directories or it already exists.")
                        }
                    }
                    val file = File(filePath)
                    if (!file.exists()) {
                        val result = file.createNewFile()
                        if (result) {
                            Log.i("?","File created successfully: ${file.absolutePath}")
                        } else {
                            Log.i("?","Failed to create file or it already exists.")
                        }
                    }
                } catch (e: Exception) {
                    Log.i("RuntimeError","An error occurred: ${e.message}")
                }
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private fun writeResultOnFile(sensor : Int, activityClass : Int?, values : FloatArray){
        val externalFilesDir = this.getExternalFilesDir(null).toString()
        val file = File("$externalFilesDir/49005113/$activityClass/${sensorsValuesMap[sensor]}.csv")
        file.appendText(String.format("%d,%d,%.9e,%.9e,%.9e\n", activityClass, System.currentTimeMillis(), values[0], values[1], values[2]))
    }

    private fun emptyFile(activityClass : Int?){
        val externalFilesDir = this.getExternalFilesDir(null).toString()
        for (sensorName in sensorsValuesMap.values) {
            val file = File("$externalFilesDir/49005113/$activityClass/$sensorName.csv")
            file.writeText("")
        }
    }

    private fun startRecording(activityClass : Int) {
        isRecording = true
        classBeingRecorded = activityClass
        delayHandler.postDelayed(startRecordingRunnable,5000)
    }

    private fun stopRecording() {
        isRecording = false
        mSensorManager.unregisterListener(mSensorEventListenerAccelerometer,mAccelerometer)
        mSensorManager.unregisterListener(mSensorEventListenerGyroscope,mGyroscope)
        mSensorManager.unregisterListener(mSensorEventListenerGravity,mGravity)
        try {
            delayHandler.removeCallbacks(startRecordingRunnable)
        } catch (e: Exception) {
            Log.d("No more delay",e.toString())
        }
        try {
            mAccelerometerWriterWorker.removeCallbacks(dataWritingAccelerometer)
            mGravityWriterWorker.removeCallbacks(dataWritingGravity)
            mGyroscopeWriterWorker.removeCallbacks(dataWritingGyroscope)
        } catch (e: Exception) {
            Log.d("No such callbacks",e.toString())
        }
        classBeingRecorded = null
    }

    private fun discardRecording(activityClass : Int?) {
        emptyFile(activityClass)
    }

    @SuppressLint("SetTextI18n")
    private fun buttonHandling(){
        val radioGroup = findViewById<RadioGroup>(R.id.radio_group_1)
        val buttonStart = findViewById<Button>(R.id.button_1)
        val buttonPause = findViewById<Button>(R.id.button_2)
        val buttonDiscard = findViewById<Button>(R.id.button_3)

        if (findViewById<RadioButton>(radioGroup.checkedRadioButtonId) == null) {
            val radioButtonOther = findViewById<RadioButton>(R.id.radio_button_0)
            radioGroup.check(radioButtonOther.id)
        }

        buttonStart.setOnClickListener {
            if (!isRecording) {
                // If nothing is recording, we start recording the class corresponding to the
                // checked radio button. If the first radio button is check, or the class is null
                // we do nothing.
                val action = findViewById<RadioButton>(radioGroup.checkedRadioButtonId)
                val activityClass = radioButtonValuesMap[action.id]
                if (activityClass != null) {
                    val activityRecordedView = findViewById<TextView>(R.id.activity_recorded)
                    buttonDiscard.visibility = View.INVISIBLE
                    activityRecordedView.visibility = View.VISIBLE
                    activityRecordedView.text = "Recording ${action.text}"
                    classBeingRecorded = activityClass
                    recordTimeStamp = System.currentTimeMillis()
                    startRecording(activityClass)
                }
            } else {
                Toast.makeText(this, "Already recording...", Toast.LENGTH_SHORT).show()
            }
        }
        // If we are recording, we check that the checked radio button is the one
        // corresponding to the recording class. If it is not, we notice the user. If it is,
        // we pause the recording
        buttonPause.setOnClickListener {
            if (isRecording) {
                val action = findViewById<RadioButton>(radioGroup.checkedRadioButtonId)
                val activityClass = radioButtonValuesMap[action.id]
                if (activityClass != classBeingRecorded){
                    Toast.makeText(this, "You need to select the class which is recording", Toast.LENGTH_SHORT).show()
                }else {
                    val activityRecordedView = findViewById<TextView>(R.id.activity_recorded)
                    buttonDiscard.visibility = View.VISIBLE
                    activityRecordedView.text = "Paused recording ${action.text}"
                    stopRecording()
                }
            } else {
                Toast.makeText(this, "Not recording...", Toast.LENGTH_SHORT).show()
            }
        }

        buttonDiscard.setOnClickListener {
            val action = findViewById<RadioButton>(radioGroup.checkedRadioButtonId)
            val activityClass = radioButtonValuesMap[action.id]
            val activityRecordedView = findViewById<TextView>(R.id.activity_recorded)
            activityRecordedView.visibility = View.INVISIBLE
            discardRecording(activityClass)
            Toast.makeText(this, "Discarded ${action.text}", Toast.LENGTH_SHORT).show()

        }
    }


    // Those runnable are made to write the data in temporary queue each 5 seconds to the three files
    private val dataWritingAccelerometer = object : Runnable {
        override fun run() {
            if (isRecording) {
                val dataToWrite = ArrayList(tempAccelerometerQueue)
                dataToWrite.forEach { values ->
                    writeResultOnFile(Sensor.TYPE_LINEAR_ACCELERATION, classBeingRecorded, values)
                }
                tempAccelerometerQueue.clear()
                mAccelerometerWriterWorker.postDelayed(this, 5000)
            }
        }
    }

    private val dataWritingGyroscope = object : Runnable {
        override fun run() {
            if (isRecording) {
                val dataToWrite = ArrayList(tempGyroscopeQueue)
                dataToWrite.forEach { values ->
                    writeResultOnFile(Sensor.TYPE_GYROSCOPE, classBeingRecorded, values)
                }
                tempGyroscopeQueue.clear()
                mGyroscopeWriterWorker.postDelayed(this, 5000)
            }
        }
    }

    private val dataWritingGravity = object : Runnable {
        override fun run() {
            if (isRecording) {
                val dataToWrite = ArrayList(tempGravityQueue)
                dataToWrite.forEach { values ->
                    writeResultOnFile(Sensor.TYPE_GRAVITY, classBeingRecorded, values)
                }
                tempGravityQueue.clear()
                mGravityWriterWorker.postDelayed(this, 5000)
            }
        }
    }

}