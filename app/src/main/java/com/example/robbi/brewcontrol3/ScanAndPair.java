package com.example.robbi.brewcontrol3;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

/**
 * Created by robbi on 17/09/16.
 */
public class ScanAndPair extends Activity {
    TextView out;
    String connectStatus;
    BluetoothAdapter mBluetoothAdapter = null;
    public OutputStream mOutputStream = null;
    public InputStream mInputStream = null;
    public BluetoothSocket mSocket = null;
    Handler handler = null;
    Thread workerThread;

    public Button connectButton = null;
    public Button getTempButton = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_and_pair);

        //Initialize globals (GUI/BT control things)
        handler = new Handler();
        out = (TextView) findViewById(R.id.outText);
        out.setText("Hall�");
        connectButton = (Button) findViewById(R.id.connectButton);
        getTempButton = (Button) findViewById(R.id.getTempButton);

        //Create the adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //Register intent for adapter
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy


        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment()).commit();
        }
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
                            bytesRead = mInputStream.read(buffer);
                            if (bytesRead != -1) {
                                while ((bytesRead == bufferSize) && (buffer[bufferSize - 1] != 0)) {
                                    message = message + new String(buffer, 0, bytesRead);
                                    bytesRead = mInputStream.read(buffer);
                                }
                                message = message + new String(buffer, 0, bytesRead - 1);
                                updateUI("Current temperature:" + message + "�", handler, out);
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


    public void updateUI(final String message, Handler handler, final TextView textView) {
        handler.post(new Runnable() {
            @Override
            public void run() {

                textView.setText(message);
            }
        });
    }

    public void connectToBT(View view) {
        out.setText("connecting...");
        if (mBluetoothAdapter == null) {
            out.setText("No Bluetooth adapter found...:(");
            return;
        }
        if (mBluetoothAdapter.isEnabled()) {

            if (!connectedToItead()) {
                mBluetoothAdapter.startDiscovery();
            }
        } else {
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBTIntent, 1);
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                String x = device.getAddress().toString();
                String y = "00:12:07:13:45:32";
                if (x.equalsIgnoreCase(y)) {
                    UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID
                    try {
                        mSocket = device.createRfcommSocketToServiceRecord(uuid);
                        mSocket.connect();
                        mOutputStream = mSocket.getOutputStream();
                        mInputStream = mSocket.getInputStream();
                        mBluetoothAdapter.cancelDiscovery();
                        connectionSuccess(device.getName());

                    } catch (IOException e) {

                        e.printStackTrace();
                        try {
                            mSocket.close();
                        } catch (IOException e1) {

                            e1.printStackTrace();
                        }
                    }
                }
            }
        }
    };

    private void connectionSuccess(String name) {
        out.setText("Connected!");
        connectButton.setVisibility(View.GONE);
        getTempButton.setVisibility(View.VISIBLE);
    }

    //Checks if already paired to Itead
    //and returns true if connection is successful
    public boolean connectedToItead() {
        Set pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (Object device : pairedDevices) {
                BluetoothDevice d = (BluetoothDevice) device;
                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID
                BluetoothSocket mSocket = null;
                try {
                    String name = d.getName();
                    if (name.equals("itead")) {
                        mSocket = d.createRfcommSocketToServiceRecord(uuid);
                        mSocket.connect();
                        mOutputStream = mSocket.getOutputStream();
                        mInputStream = mSocket.getInputStream();
                        connectionSuccess(d.getName());
                        return true;
                    }
                } catch (IOException e) {
                    try {
                        mSocket.close();
                        return false;
                    } catch (IOException e1) {
                        e1.printStackTrace();
                        return false;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_mesh, container,
                    false);
            return rootView;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }
        unregisterReceiver(mReceiver);
    }
}

