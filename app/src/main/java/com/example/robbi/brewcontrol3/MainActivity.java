package com.example.robbi.brewcontrol3;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;


import java.io.IOException;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.subscriptions.CompositeSubscription;

public class MainActivity extends AppCompatActivity implements BlueToothBuddy.BlueToothCallBacks, TimeManager.TimeManagerCallbacks, TemperatureReader.TemperatureReaderCallbacks {


    private static final String TAG = MainActivity.class.getSimpleName();
    @Bind(R.id.connected)
    TextView connectedHeader;

    @Bind(R.id.current_temperature)
    TextView currentTemperature;

    @Bind(R.id.average_temperature)
    TextView averageTemperature;

    @Bind(R.id.average_change)
    TextView averageChange;

    @Bind(R.id.time)
    TextView time;

    private static final int REQUEST_ENABLE_BT = 1000;

    private CompositeSubscription compositeSubscription;
    private TimeManager timeManager;
    private IntentFilter filter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
        this.timeManager = new TimeManager();


        if (!BlueToothBuddy.getInstance(this).isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            showProgress(true);
            BlueToothBuddy.getInstance(this).setUpAndConnect();
        }

        //Register intent for adapter
        filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
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
        BlueToothBuddy.getInstance(this).clear();
        showProgress(true);
        registerReceiver(BlueToothBuddy.getInstance(this), filter);
        BlueToothBuddy.getInstance(this).setUpAndConnect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        BlueToothBuddy.getInstance(this).clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.compositeSubscription != null) {
            this.compositeSubscription.clear();
        }
        unregisterReceiver(BlueToothBuddy.getInstance(this));
        BlueToothBuddy.getInstance(this).clear();
    }


    private void showProgress(boolean b) {
        findViewById(R.id.progress).setVisibility(b ? View.VISIBLE : View.GONE);
    }


    public void btnCheck(View v) {
        MainApplication.getBrewControl().resetTimer();
        showProgress(true);
        BlueToothBuddy.getInstance(this).clear();
        BlueToothBuddy.getInstance(this).setUpAndConnect();
    }

    @Override
    public void onFinishSetup(String name, int status) {
        showProgress(false);
        if (status == BlueToothBuddy.SUCCESSFUL_CONNECTION) {
            connectedHeader.setText("Connected to " + name);
            TemperatureReader temperatureReader;
            temperatureReader = new TemperatureReader();
            try {
                temperatureReader.setInputStream(BlueToothBuddy.getInstance(this).getSocket().getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            temperatureReader.setCallback(this);
            temperatureReader.start();
//            this.compositeSubscription = new CompositeSubscription();
//            this.compositeSubscription.add(timeManager.getTimeSubscription(this));

        } else {
            connectedHeader.setText("Not connected");
        }
    }

    @Override
    public void onTempRecieved(String _temp) {
        currentTemperature.setText(_temp + "°");
        showProgress(false);
    }

    @Override
    public void onTimeMeasured(String time) {
        this.time.setText(time);
    }
}
