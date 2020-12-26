package com.example.nanocar

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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

    private val REQUEST_ENABLE_BT = 3

    private var dx: Float = 0F
    private var dy: Float = 0F

    private var bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var commServer: CommunicationServer =
        CommunicationServer(bluetoothAdapter?.getRemoteDevice(DEVICE_ADDRESS)!!)

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_lanscape)


        directionContainer = findViewById(R.id.direction_container)
        directionController = findViewById(R.id.direction_controller)
        headlight = findViewById(R.id.headlight)
        engine = findViewById(R.id.engine)
        leftDoor = findViewById(R.id.left_door)
        rightDoor = findViewById(R.id.right_door)
        leftSignal = findViewById(R.id.left_signal)
        rightSignal = findViewById(R.id.right_signal)
        fan = findViewById(R.id.fan)
        temperature = findViewById(R.id.temperature)
        seatBelt = findViewById(R.id.seat_belt)
        
        directionController.setOnTouchListener { v, event ->
            val rawX = event.rawX
            val rawY = event.rawY
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
//                    val xy = intArrayOf(0, 0)
//                    v.getLocationOnScreen(xy)
//                    val x = v.x
//                    val y = v.y
//                    dx = xy[0] - rawX
//                    dy = xy[1] - rawY
                    dx = v.x - event.rawX
                    dy = v.y - event.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    val minXminY = intArrayOf(0, 0)
                    directionContainer.getLocationOnScreen(minXminY)
                    val minX = minXminY[0].toFloat()
                    val minY = minXminY[1].toFloat()
                    val maxX = minX + directionContainer.width
                    val maxY = minY + directionContainer.height

                    v.animate()
//                        .x(min(maxX, max(minX, rawX + dx)))
                        .x(max(0F, min(directionContainer.width.toFloat(), rawX + dx)))
//                        .y(min(maxY, max(minY, rawY + dy)))
                        .y(max(0F, min(directionContainer.height.toFloat(), rawY + dy)))
                        .setDuration(0)
                        .start()
                }
            }

//            when (event.action) {
//                MotionEvent.ACTION_DOWN -> {
//                    dx = v.x - event.rawX
//                    dy = v.y - event.rawY
//
//                }
//                MotionEvent.ACTION_MOVE -> {
//                    v.animate()
//                        .x(event.rawX + dx)
//                        .y(event.rawY + dy)
//                        .setDuration(0)
//                        .start()
//                }
//            }

            true
        }

//        findViewById<Button>(R.id.button).setOnClickListener {
//            val command = findViewById<EditText>(R.id.command).text.toString()
//            val value = findViewById<EditText>(R.id.value).text.toString()
//            commServer.write((command + value + "F").byteInputStream().readBytes())
//        }

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)

        setupBluetooth()
    }

    override fun onStop() {
        super.onStop()

        commServer.stop()
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
}