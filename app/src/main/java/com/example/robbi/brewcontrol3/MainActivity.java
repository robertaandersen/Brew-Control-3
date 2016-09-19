package com.example.robbi.brewcontrol3;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.util.TimeUtils;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


import butterknife.Bind;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.observables.AsyncOnSubscribe;
import rx.observables.StringObservable;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import rx.subscriptions.CompositeSubscription;

public class MainActivity extends AppCompatActivity {


    @Bind(R.id.connected)
    TextView connectedHeader;

    @Bind(R.id.current_temperature)
    TextView currentTemperature;

    @Bind(R.id.average_temperature)
    TextView averageTemperature;

    @Bind(R.id.time)
    TextView time;

    private static final int REQUEST_ENABLE_BT = 1000;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice arduino;
    private BluetoothSocket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private BehaviorSubject<String> behaviorSubject = BehaviorSubject.create();
    private CompositeSubscription compositeSubscription;
    private HashMap<Integer, CurrentReading> readings = new HashMap<>();
    private Thread workerThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number

        //Create the adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //Register intent for adapter
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode != RESULT_OK) {
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        compositeSubscription = new CompositeSubscription();
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {

            if (arduino == null && !signToPairedDevise()) {
                if (!bluetoothAdapter.isDiscovering()) {
                    showProgress(true);
                    bluetoothAdapter.startDiscovery();
                }
            } else {
                subscribeToSocket();
            }
        }

    }

    private void subscribeToSocket() {

    }

    private void updateUI() {
        SortedSet<Integer> keys = new TreeSet<Integer>(readings.keySet());
        CurrentReading currentReading = readings.get(keys.last());
        currentTemperature.setText(currentReading.temperature.toString());
        averageTemperature.setText(currentReading.averageTemp.toString());
        time.setText(currentReading.duration.toString());


    }

    private void processString(String s) {
        String[] arr = s.split("currentReading");
        List<String> l = new ArrayList<>();
        if (arr.length > 1) {
            String startPattern = "\":{";
            String endPattern = "\"}}\r\n{\"";
            for (String string : arr) {
                if (string.length() > 2 && string.substring(0, 3).equals(startPattern))
                    if (string.split(",").length == 4)
                        l.add(string.replace(startPattern, "{").replace(endPattern, "}"));
            }
        }
        if (l.size() > 0) {
            for (String candidate : l) {
                try {
                    CurrentReading r = new ObjectMapper().readValue(candidate, CurrentReading.class);
                    readings.put(r.duration, r);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void showProgress(boolean b) {
        findViewById(R.id.progress).setVisibility(b ? View.VISIBLE : View.GONE);
    }

    private boolean signToPairedDevise() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice bd : pairedDevices) {
            if (bd.getName().toLowerCase().equals("itead")) {
                arduino = bd;
                connectToDevice();
                return true;
            }
        }
        return false;
    }

    public void btnCheck(View v) {


    }

    public void getTemperature(View view) {
        workerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int tempDate = 1000;
                int stopTime = 0;
                while (!Thread.currentThread().isInterrupted()) {

                    int bufferSize = 1024;
                    byte[] buffer = new byte[bufferSize];
                    String message = "";
                    try {
                        int bytesRead = -1;

                        Date d = new Date();
                        int startTime = (int) d.getTime();

                        if (tempDate >= 1000) {
                            message = "";
                            bytesRead = inputStream.read(buffer);
                            if (bytesRead != -1) {
                                while ((bytesRead == bufferSize) && (buffer[bufferSize - 1] != 0)) {
                                    message = message + new String(buffer, 0, bytesRead);
                                    bytesRead = inputStream.read(buffer);
                                }
                                message = message + new String(buffer, 0, bytesRead - 1);
                                processString(message);
                                updateUI();
                                stopTime = (int) d.getTime();
                            }
                        }
                        tempDate = startTime - stopTime;
                    } catch (IOException e) {
                        workerThread.interrupt();
                    }
                }
            }
        });
        workerThread.start();

		/*BluetoothSocketListener blisten = new BluetoothSocketListener(mSocket, handler, out, mInputStream);
        Thread messageListener = new Thread(blisten);
		messageListener.start();*/
		/*
		int read = -1;
		final byte[] bytes = new byte[2048];


				try {
					read = mInputStream.read(bytes);
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (read > 0) {
					final int count = read;
				}
		*/

		/*
		out.setText("hahahah");
		String message = "Android - Hallo\n";
		byte[] msgBuffer = message.getBytes();

		try {
			mOutputStream.write(msgBuffer);
		} catch (IOException e) {}*/
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothAdapter != null) {
            bluetoothAdapter = null;
        }
        try {
            if (socket != null && socket.isConnected())
                socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        unregisterReceiver(mReceiver);

    }

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery s a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && device.getAddress() != null && device.getAddress().equalsIgnoreCase("00:12:07:13:45:32")) {
                    arduino = device;
                    showProgress(false);
                    bluetoothAdapter.cancelDiscovery();
                    connectToDevice();
                }
            }
        }
    };

    private void connectToDevice() {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID
        try {
            socket = arduino.createRfcommSocketToServiceRecord(uuid);
            socket.connect();
            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();
            connectedHeader.setText("Connected to " + arduino.getName());
            subscribeToSocket();

        } catch (IOException e) {
            e.printStackTrace();
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

    }
}
