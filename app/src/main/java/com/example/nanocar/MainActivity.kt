package com.example.nanocar

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.nanocar.bluetooth.CommunicationServer
import kotlin.math.max
import kotlin.math.min

private const val DEVICE_NAME = ""
private const val DEVICE_ADDRESS = "A8:87:B3:48:E4:F2"

class MainActivity : AppCompatActivity() {
    val TAG: String = "Main Activity"

    // Direction Controller box dimension
    private val boxDim = 40
    private val on = "on"
    private val off = "off"
    private val speedProportion = 2 / 3f

    private lateinit var directionContainer: RelativeLayout
    private lateinit var directionController: View
    private lateinit var headlight: Button
    private lateinit var engine: Button
    private lateinit var leftDoor: Button
    private lateinit var rightDoor: Button
    private lateinit var leftSignal: Button
    private lateinit var rightSignal: Button
    private lateinit var fan: Button
    private lateinit var temperature: Button
    private lateinit var seatBelt: Button
    private lateinit var speedGaugeLine: View
    private lateinit var dirGaugeLine: View

    private var headlightState = false
    private var engineState = false
    private var leftDoorState = false
    private var rightDoorState = false
    private var leftSignalState = false
    private var rightSignalState = false
    private var fanState = false
    private var temperatureState = 25
    private var seatBeltState = false

    // updated by message thread
    private var brakeState = false

    private var speed = 21
    private var direction = 42

    private val REQUEST_ENABLE_BT = 3

    private var dx: Float = 0F
    private var dy: Float = 0F

    // At the initialization, decide if items are placed
    private var dirCentered = false
    private var speedGaugeLineCentered = false
    private var dirGaugeLineCentered = false

    private var bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var commServer: CommunicationServer =
        CommunicationServer(bluetoothAdapter?.getRemoteDevice(DEVICE_ADDRESS)!!)

    private var messageThread: MessageThread? = null
    private var temperatureThread: TemperatureThread? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_lanscape)

        // Set direction controller dimension programmatically
        val scale = applicationContext.resources.displayMetrics.density

        speedGaugeLine = findViewById(R.id.speed_gauge)
        speedGaugeLine.pivotX = scale
        speedGaugeLine.pivotY = scale
        val speedGaugeContainer = findViewById<RelativeLayout>(R.id.speed_gauge_container)
        speedGaugeContainer.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (!speedGaugeLineCentered) {
                speedGaugeLine.x = (right - left) / 2f
                speedGaugeLine.y = (bottom - top) / 2f + 10f
                speedGaugeLine.rotation = -210f
                speedGaugeLineCentered = true
            }
        }

        dirGaugeLine = findViewById(R.id.dir_gauge)
        dirGaugeLine.pivotX = scale
        dirGaugeLine.pivotY = scale
        val dirGaugeContainer = findViewById<RelativeLayout>(R.id.dir_gauge_container)
        dirGaugeContainer.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (!dirGaugeLineCentered) {
                dirGaugeLine.x = (right - left) / 2f - 10f
                dirGaugeLine.y = bottom - top - scale * 2
                dirGaugeLine.rotation = -90f
                dirGaugeLineCentered = true
            }
        }

        headlight = findViewById(R.id.headlight)
        headlightState = true
        toggleHeadlight()
        headlight.setOnClickListener {
            toggleHeadlight()
        }

        engine = findViewById(R.id.engine)
        engineState = true
        toggleEngine()
        engine.setOnClickListener {
            toggleEngine()
        }

        leftDoor = findViewById(R.id.left_door)
        leftDoorState = true
        toggleLeftDoor()
        leftDoor.setOnClickListener {
            toggleLeftDoor()
        }

        rightDoor = findViewById(R.id.right_door)
        rightDoorState = true
        toggleRightDoor()
        rightDoor.setOnClickListener {
            toggleRightDoor()
        }

        leftSignal = findViewById(R.id.left_signal)
        leftSignalState = true
        toggleLeftSignal()
        leftSignal.setOnClickListener {
            toggleLeftSignal()
        }

        rightSignal = findViewById(R.id.right_signal)
        rightSignalState = true
        toggleRightSignal()
        rightSignal.setOnClickListener {
            toggleRightSignal()
        }

        fan = findViewById(R.id.fan)
        fanState = true
        toggleFan()
        fan.setOnClickListener {
            toggleFan()
        }

        temperature = findViewById(R.id.temperature)
        temperature.text = resources.getString(R.string.temperature).format(temperatureState)

        seatBelt = findViewById(R.id.seat_belt)
        seatBeltState = true
        toggleSeatBelt()
        seatBelt.setOnClickListener {
            toggleSeatBelt()
        }

        directionContainer = findViewById(R.id.direction_container)
        directionController = findViewById(R.id.direction_controller)

        directionController.layoutParams =
            RelativeLayout.LayoutParams((boxDim * scale).toInt(), (boxDim * scale).toInt())

        directionContainer.addOnLayoutChangeListener { _, left, top, right, bottom,
                                                       _, _, _, _ ->
            if (!dirCentered) {
                directionController.x = (right - left - boxDim * scale) / 2f
                directionController.y = (bottom - top - boxDim * scale) * speedProportion
                dirCentered = true
            }
        }

        directionController.setOnTouchListener { v, event ->
            val rawX = event.rawX
            val rawY = event.rawY

            val boxScale = boxDim * scale
            val boxHalf = boxScale / 2

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dx = v.x - event.rawX
                    dy = v.y - event.rawY
                }
                MotionEvent.ACTION_UP -> {
                    v.animate()
                        .x((directionContainer.width - boxScale) / 2f)
                        .y((directionContainer.height) * speedProportion - boxHalf)
                        .setDuration(0)
                        .start()
                }
                MotionEvent.ACTION_MOVE -> {
                    v.animate()
                        .x(
                            max(
                                -boxHalf,
                                min(directionContainer.width - boxHalf, rawX + dx)
                            )
                        )
                        .y(
                            max(
                                -boxHalf,
                                min(directionContainer.height - boxHalf - 1f, rawY + dy)
                            )
                        )
                        .setDuration(0)
                        .start()
                }
            }

            updateSpeedAndDirection()
            true
        }

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)

        setupBluetooth()

        messageThread = MessageThread()
        messageThread!!.start()

        temperatureThread = TemperatureThread()
        temperatureThread!!.start()

    }

    override fun onStop() {
        super.onStop()

        temperatureThread?.cancel()

        messageThread?.cancel()

        commServer.stop()
    }

    private fun updateSpeedAndDirection() {
        val scale = applicationContext.resources.displayMetrics.density

        val boxHalf = boxDim * scale / 2

        val containerCenterX = directionContainer.width / 2f
        val containerCenterY = directionContainer.height * speedProportion

        val centerX = directionController.x + boxHalf
        val centerY = directionController.y + boxHalf

        direction =
            ((centerX - containerCenterX) / containerCenterX * 42).toInt() + 42

        speed = if (centerY < containerCenterY) {
            ((containerCenterY - centerY) / containerCenterY * 120).toInt() + 20
        } else {
            ((centerY - containerCenterY) / (directionContainer.height - containerCenterY) * 20).toInt()
        }

        speedGaugeLine.rotation = if (speed >= 20) {
            -210f + (speed - 20) / 120f * 240f
        } else {
            -210f + speed / 120f * 240f
        }

        dirGaugeLine.rotation = -132f + direction
    }

    private fun buildStateData(): Byte {
        var i = 0
        i = i.or(brakeState.toInt().shl(7))
        i = i.or(engineState.toInt().shl(6))
        i = i.or(seatBeltState.toInt().shl(5))
        i = i.or(headlightState.toInt().shl(4))
        i = i.or(rightSignalState.toInt().shl(3))
        i = i.or(leftSignalState.toInt().shl(2))
        i = i.or(rightDoorState.toInt().shl(1))
        i = i.or(leftDoorState.toInt())

        return i.toByte()
    }


    private fun toggleHeadlight() {
        headlightState = !headlightState
        headlight.text =
            resources.getString(R.string.headlight).format(if (headlightState) on else off)
        headlight.setBackgroundColor(
            resources.getColor(
                if (headlightState) R.color.green else R.color.red,
                null
            )
        )
    }

    private fun toggleEngine() {
        engineState = !engineState
        engine.text = resources.getString(R.string.engine).format(if (engineState) on else off)
        engine.setBackgroundColor(
            resources.getColor(
                if (engineState) R.color.green else R.color.red,
                null
            )
        )
    }

    private fun toggleLeftDoor() {
        leftDoorState = !leftDoorState
        leftDoor.text =
            resources.getString(R.string.left_door).format(if (leftDoorState) on else off)
        leftDoor.setBackgroundColor(
            resources.getColor(
                if (leftDoorState) R.color.green else R.color.red,
                null
            )
        )
    }

    private fun toggleRightDoor() {
        rightDoorState = !rightDoorState
        rightDoor.text =
            resources.getString(R.string.right_door).format(if (rightDoorState) on else off)
        rightDoor.setBackgroundColor(
            resources.getColor(
                if (rightDoorState) R.color.green else R.color.red,
                null
            )
        )
    }

    private fun toggleLeftSignal() {
        leftSignalState = !leftSignalState
        leftSignal.text =
            resources.getString(R.string.left_signal).format(if (leftSignalState) on else off)
        leftSignal.setBackgroundColor(
            resources.getColor(
                if (leftSignalState) R.color.green else R.color.red,
                null
            )
        )
    }

    private fun toggleRightSignal() {
        rightSignalState = !rightSignalState
        rightSignal.text =
            resources.getString(R.string.right_signal).format(if (rightSignalState) on else off)
        rightSignal.setBackgroundColor(
            resources.getColor(
                if (rightSignalState) R.color.green else R.color.red,
                null
            )
        )
    }

    private fun toggleFan() {
        fanState = !fanState
        fan.text = resources.getString(R.string.fan).format(if (fanState) on else off)
        fan.setBackgroundColor(
            resources.getColor(
                if (fanState) R.color.green else R.color.red,
                null
            )
        )
    }

    private fun toggleSeatBelt() {
        seatBeltState = !seatBeltState
        seatBelt.text =
            resources.getString(R.string.seat_belt_fastened).format(if (seatBeltState) on else off)
        seatBelt.setBackgroundColor(
            resources.getColor(
                if (seatBeltState) R.color.cyan else R.color.blue,
                null
            )
        )
    }

    private fun setupBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null) {
            Log.d(TAG, "The device does not have bluetooth")

            return
        }

        if (bluetoothAdapter?.isEnabled == false) {
            Log.d(TAG, "Bluetooth is not enabled")
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)

            return
        }

        val devices = bluetoothAdapter?.bondedDevices

        devices?.forEach { dev ->
            if (dev.name == "HK_NANO") {
                commServer = CommunicationServer(dev)
                commServer.connect()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_ENABLE_BT -> {
                if (resultCode == Activity.RESULT_OK) {
                    Log.i(TAG, "REQUEST_ENABLE_BT")

                    setupBluetooth()
                } else {
                    Log.i(TAG, "BT NOT ENABLED")
                }
            }
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action

            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                    Log.i(
                        TAG,
                        "deviceName %s, deviceHardwareAddress %s".format(
                            device?.name,
                            device?.address
                        )
                    )
                }
            }
        }
    }

    private inner class MessageThread : Thread() {
        private var shouldStop = false
        private var previousSpeed = 0

        override fun run() {
            super.run()
            previousSpeed = speed

            while (!shouldStop) {
                // Update brake
                brakeState = previousSpeed > speed || speed <= 21

                val states = buildStateData()

                val bytes = ByteArray(6)
                bytes[0] = "S"[0].toByte()
                bytes[1] = speed.toByte()
                bytes[2] = direction.toByte()
                bytes[3] = states
                bytes[4] = temperatureState.toByte()
                bytes[5] = "F"[0].toByte()

                commServer.write(bytes)

                previousSpeed = speed

                sleep(100)
            }
        }

        fun cancel() {
            shouldStop = true
        }
    }

    private inner class TemperatureThread : Thread() {
        private var shouldStop = false
        private val maxTemp = 50
        override fun run() {
            super.run()

            while (!shouldStop) {
                temperatureState =
                    min(maxTemp, max(0, temperatureState + (if (fanState) -1 else 1)))

                runOnUiThread {
                    temperature.text =
                        resources.getString(R.string.temperature).format(temperatureState)

                    temperature.setBackgroundColor(
                        Color.argb(
                            255,
                            (temperatureState / maxTemp.toFloat() * 255).toInt(),
                            0,
                            ((maxTemp - temperatureState) / maxTemp.toFloat() * 255).toInt()
                        )
                    )
                }
                sleep(1000)
            }
        }

        fun cancel() {
            shouldStop = true
        }
    }
}

private fun Boolean.toInt(): Int {
    return if (this) 1 else 0
}