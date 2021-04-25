package com.example.bluetoothtry2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "kamlans";

    private BluetoothAdapter bluetoothAdapter ;
    private String address;
    private BluetoothSocket socket;
    private static Handler handler;
    private  static  final int CONNECTING_STATUS = 1;
    private static final int MESSAGE_READ = 2;
    private ConnectionThread connectionThread;
    private CommThread commThread;
    private Button connectBtn;
    private String deviceName = null;
    private String deviceAddress;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        connectBtn = findViewById(R.id.buttonConnect);
        final Toolbar toolbar = findViewById(R.id.toolbar);
        final ProgressBar progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        final TextView textViewInfo = findViewById(R.id.textViewInfo);
        final Button buttonToggle = findViewById(R.id.buttonToggle);
        buttonToggle.setEnabled(false);



        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SelectDeviceActivity.class);
                startActivity(intent);
            }
        });

        deviceName = getIntent().getStringExtra("deviceName");
        if (deviceName != null){
            // Get the device address to make BT Connection
            deviceAddress = getIntent().getStringExtra("deviceAddress");
            // Show progree and connection status
            toolbar.setSubtitle("Connecting to " + deviceName + "...");
            progressBar.setVisibility(View.VISIBLE);
            connectBtn.setEnabled(false);

            /*
            This is the most important piece of code. When "deviceName" is found
            the code will call a new thread to create a bluetooth connection to the
            selected device (see the thread code below)
             */
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            connectionThread = new ConnectionThread(bluetoothAdapter,deviceAddress);
            connectionThread.start();
        }


        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg){
                switch (msg.what){
                    case CONNECTING_STATUS:
                        switch(msg.arg1){
                            case 1:
                                toolbar.setSubtitle("Connected to " + deviceName);
                                progressBar.setVisibility(View.GONE);
                                connectBtn.setEnabled(true);
                                buttonToggle.setEnabled(true);
                                break;
                            case -1:
                                toolbar.setSubtitle("Device fails to connect");
                                progressBar.setVisibility(View.GONE);
                                connectBtn.setEnabled(true);
                                break;
                        }
                        break;

                    case MESSAGE_READ:
                        String arduinoMsg = msg.obj.toString(); // Read message from Arduino
                        switch (arduinoMsg.toLowerCase()){
                            case "led is turned on":

                                textViewInfo.setText("Arduino Message : " + arduinoMsg);
                                break;
                            case "led is turned off":

                                textViewInfo.setText("Arduino Message : " + arduinoMsg);
                                break;
                        }
                        break;
                }
            }
        };
    }

    public class ConnectionThread extends Thread{

        BluetoothDevice bluetoothDevice;

        public ConnectionThread(BluetoothAdapter bluetoothAdapter, String address) {


            bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
            BluetoothSocket tmp = null;

           // UUID uuid = bluetoothDevice.getUuids()[0].getUuid();
          UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

            try{
                tmp = bluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID);
//                bluetoothDevice.getClass().getMethod("createRfcommSocket", new Class[] { int.class } ).invoke( bluetoothDevice , 1);
                Log.d(TAG, "ConnectionThread: temp successful");
            }catch (Exception e){
                Log.e(TAG, "Socket's create() method failed", e);
            }

            socket = tmp;
            Log.d(TAG, "run: checking connectivity    :"+socket.isConnected());
            Log.d(TAG, "run: checking connectivity tmp   :"+tmp.isConnected());
        }

        @Override
        public void run() {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            bluetoothAdapter.cancelDiscovery();

            try {

                socket.connect();

                handler.obtainMessage(CONNECTING_STATUS , 1 , -1).sendToTarget();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "run: error is  :"+e );

//                try {
//                    socket.close();
//                    Log.d(TAG, "run: closing ");
//                    handler.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget();
//
//                } catch (Exception ioException) {
//                    ioException.printStackTrace();
//                    Log.d(TAG, "run error in closing : "+ioException);
//                }
//                return;
                try{
                    socket = (BluetoothSocket) bluetoothDevice.getClass().getMethod("createRfcommSocket" , new Class[] {int.class}).invoke(bluetoothDevice , 1);
                    socket.connect();
                    Log.d(TAG, "socket failed: ");
                } catch (IllegalAccessException illegalAccessException) {
                    illegalAccessException.printStackTrace();
                    Log.d(TAG, "illegalAccessException: "+illegalAccessException);
                } catch (InvocationTargetException invocationTargetException) {
                    invocationTargetException.printStackTrace();
                    Log.d(TAG, "invocationTargetException: "+invocationTargetException);
                } catch (NoSuchMethodException noSuchMethodException) {
                    noSuchMethodException.printStackTrace();
                    Log.d(TAG, "noSuchMethodException: "+noSuchMethodException);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                    Log.d(TAG, "ioException: "+ioException);
                }

            }

            //TODO: add commThread here
            commThread = new CommThread(socket);
            commThread.start();
        }

        public  void cancel (){
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "cancel: "+e);
            }
        }
    }


    public class CommThread extends Thread {
        private final BluetoothSocket socket;
        private final InputStream InStream;
        private final OutputStream OutStream;

        public CommThread(BluetoothSocket socket) {
            this.socket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;


            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            InStream = tmpIn;
            OutStream = tmpOut;

        }


        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes = 0;

            while (true) {
                try {
                    /*
                    Read from the InputStream from Arduino until termination character is reached.
                    Then send the whole String message to GUI Handler.
                     */
                    buffer[bytes] = (byte) InStream.read();
                    String readMessage;
                    if (buffer[bytes] == '\n') {
                        readMessage = new String(buffer, 0, bytes);
                        Log.d(TAG, readMessage);
                        handler.obtainMessage(MESSAGE_READ, readMessage).sendToTarget();
                        bytes = 0;
                    } else {
                        bytes++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
        public void write(String input) {
            byte[] bytes = input.getBytes(); //converts entered String into bytes
            try {
                OutStream.write(bytes);
            } catch (IOException e) {
                Log.e("Send Error","Unable to send message",e);
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.d(TAG, "cancel: "+e);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (connectionThread != null) {
            connectionThread.cancel();
        }
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }
}