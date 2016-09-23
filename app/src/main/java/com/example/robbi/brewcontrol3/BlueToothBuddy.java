package com.example.robbi.brewcontrol3;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.DropBoxManager;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by robbi on 22/09/16.
 */
public class BlueToothBuddy extends BroadcastReceiver {

    public static final int SUCCESSFUL_CONNECTION = 1;
    public static final int UNSUCCESSFUL_CONNECTION = -1;
    private static BlueToothCallBacks callBacks;
    private static BlueToothBuddy blueToothBuddy;
    private final BluetoothAdapter blueToothAdapter;
    private BluetoothDevice arduino;
    private BluetoothSocket socket;

    public BlueToothBuddy() {
        this.blueToothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public static BlueToothBuddy getInstance(BlueToothCallBacks _callBacks) {
        callBacks = _callBacks;
        if (blueToothBuddy == null) {
            blueToothBuddy = new BlueToothBuddy();
        }
        return blueToothBuddy;
    }

    // Create a BroadcastReceiver for ACTION_FOUND
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        // When discovery s a device
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            // Get the BluetoothDevice object from the Intent
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device != null && device.getAddress() != null && device.getAddress().equalsIgnoreCase("00:12:07:13:45:32")) {
                arduino = device;
                this.blueToothAdapter.cancelDiscovery();
                connectToDevice();
            }
        } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action) && arduino == null) {
            callBacks.onFinishSetup("No device found", UNSUCCESSFUL_CONNECTION);
        }
    }

    public boolean isEnabled() {
        return this.blueToothAdapter.isEnabled();
    }

    public void setUpAndConnect() {
        if(signToPairedDevise())
                return;
        if (arduino == null) {
            if (!this.blueToothAdapter.isDiscovering()) {
                this.blueToothAdapter.startDiscovery();
            }
        }
    }

    private void connectToDevice() {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID
        try {
            socket = arduino.createRfcommSocketToServiceRecord(uuid);
            socket.connect();
            callBacks.onFinishSetup(arduino.getName(), SUCCESSFUL_CONNECTION);
            return;

        } catch (IOException e) {
            Class<?> clazz = socket.getRemoteDevice().getClass();
            Class<?>[] paramTypes = new Class<?>[] {Integer.TYPE};
            Method m = null;
            try {
                m = clazz.getMethod("createRfcommSocket", paramTypes);
            } catch (NoSuchMethodException e1) {
                e1.printStackTrace();
            }
            Object[] params = new Object[] {Integer.valueOf(1)};
            try {
                socket = (BluetoothSocket) m.invoke(socket.getRemoteDevice(), params);
                socket.connect();
                callBacks.onFinishSetup(arduino.getName(), SUCCESSFUL_CONNECTION);
                return;
            } catch (IllegalAccessException e1) {
            } catch (InvocationTargetException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        callBacks.onFinishSetup(arduino.getName(), UNSUCCESSFUL_CONNECTION);


    }

    private boolean signToPairedDevise() {
        Set<BluetoothDevice> pairedDevices = blueToothAdapter.getBondedDevices();
        for (BluetoothDevice bd : pairedDevices) {
            if (bd.getName().toLowerCase().equals("itead")) {
                arduino = bd;
                connectToDevice();
                return true;
            }
        }
        return false;
    }

    public void clear() {
        try {
            if (socket != null && socket.isConnected()) {
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (arduino != null) {
            arduino = null;
        }


    }

    public BluetoothSocket getSocket() {
        return socket;
    }


//    public class TempHandler extends Handler {
//
//        private List<Double> averageMeasureMents = new ArrayList<>();
//
////        public void update(final String s) {
////
////        }
//
//        private void addToAverage(String t) {
//            try {
//                double d = Double.valueOf(t);
//                averageMeasureMents.add(d);
//                if (averageMeasureMents.size() == 60) {
//                    averageMeasureMents = new ArrayList<Double>();
//                }
//            } catch (NumberFormatException e) {
//
//            }
//        }
//    }

    public interface BlueToothCallBacks {
        void onFinishSetup(String name, int Status);
    }
}
