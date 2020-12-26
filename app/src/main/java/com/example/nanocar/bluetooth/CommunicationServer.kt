package com.example.nanocar.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class CommunicationServer(private val mDevice: BluetoothDevice) {
    private val TAG: String = "CommunicationServer"

    // Constants that indicate the current connection state
    val STATE_NONE = 0 // we're doing nothing
    val STATE_LISTEN = 1 // now listening for incoming connections
    val STATE_CONNECTING = 2 // now initiating an outgoing connection
    val STATE_CONNECTED = 3 // now connected to a remote device

    private val mAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var mState = STATE_NONE

    private var mConnectThread: ConnectThread? = null
    private var mConnectedThread: ConnectedThread? = null

    @Synchronized
    fun connect() {
        Log.d(TAG, "connect to device")

        // Cancel any thread attempting to make a connection

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING && mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
        }

        // Cancel any thread currently running a connection

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }

        // Start the thread to connect with the given device

        // Start the thread to connect with the given device
        mConnectThread = ConnectThread(mDevice)
        mConnectThread!!.start()
    }

    @Synchronized
    private fun connected(socket: BluetoothSocket, device: BluetoothDevice) {
        Log.d(TAG, "connected")

        // Cancel the thread that completed the connection

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
        }

        // Cancel any thread currently running a connection

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = ConnectedThread(socket)
        mConnectedThread!!.start()
    }

    /**
     * Stop all threads
     */
    @Synchronized
    fun stop() {
        Log.d(TAG, "stop")
        if (mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
        }
        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }

        mState = STATE_NONE
    }

    fun write(buffer: ByteArray) {
        var r: ConnectedThread?

        synchronized(this) {
            if (mState != STATE_CONNECTED) {
                return
            }
            r = mConnectedThread
        }

        r?.write(buffer)
    }

    private inner class ConnectThread(private val mmDevice: BluetoothDevice) : Thread() {
        private val TAG: String = "ConnectThread"
        private var mmSocket: BluetoothSocket? = null

        init {
            try {
                mmSocket =
                    mmDevice.createInsecureRfcommSocketToServiceRecord(mmDevice.uuids[0].uuid)
            } catch (e: IOException) {
                Log.e(TAG, "Socket creation failed $e")
            }

            mState = STATE_CONNECTING
        }

        override fun run() {
            super.run()

            mAdapter?.cancelDiscovery()

            try {
                mmSocket?.connect()
            } catch (e: IOException) {
                try {
                    mmSocket?.close()
                } catch (e: IOException) {
                    Log.e(TAG, "Closing failed $e")
                }
                Log.e(TAG, "Connection Failed $e")

                return
            }

            // Remove this thread from mConnectThread in order to avoid closing socket when cancel is called
            synchronized(this) {
                mConnectThread = null
            }

            connected(mmSocket as BluetoothSocket, mmDevice)
        }

        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Cancel Failed $e")
            }
        }
    }

    private inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {
        private val TAG: String = "ConnectedThread"

        private lateinit var mmInputStream: InputStream
        private lateinit var mmOutputStream: OutputStream

        init {
            try {
                mmInputStream = mmSocket.inputStream
                mmOutputStream = mmSocket.outputStream
            } catch (e: IOException) {
                Log.e(TAG, "Getting streams faield $e")
            }

            mState = STATE_CONNECTED
        }

        fun write(buffer: ByteArray) {
            try {
                Log.i(TAG, "Writing into outputstream")
                mmOutputStream.write(buffer)
            } catch (e: IOException) {
                Log.e(TAG, "Write failed $e")
            }
        }

        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
                Log.e(TAG, e.toString())
            }
        }
    }
}