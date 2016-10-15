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


import com.github.mikephil.charting.charts.LineChart;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    @Bind(R.id.chart)
    LineChart lineChart;


    private static final int REQUEST_ENABLE_BT = 1000;

    private CompositeSubscription compositeSubscription;
    private TimeManager timeManager;
    private IntentFilter filter;
    private TemperatureReader temperatureReader;
    private List<Double> doubleList = new ArrayList<>();
    private ChartManager chartManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
        this.timeManager = new TimeManager();
        this.chartManager = new ChartManager(lineChart);


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
        if (compositeSubscription != null) {
            compositeSubscription.clear();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.compositeSubscription != null) {
            this.compositeSubscription.clear();
        }
        if (temperatureReader != null) {
            temperatureReader.clear();
            temperatureReader = null;
        }
        unregisterReceiver(BlueToothBuddy.getInstance(this));
        BlueToothBuddy.getInstance(this).clear();
    }


    private void showProgress(boolean b) {
        findViewById(R.id.progress).setVisibility(b ? View.VISIBLE : View.GONE);
    }


    public void btnCheck(View v) {
        if(v.getId() == R.id.reset_all) {
            MainApplication.getBrewControl().resetTimer();
            showProgress(true);
            BlueToothBuddy.getInstance(this).clear();
            BlueToothBuddy.getInstance(this).setUpAndConnect();
            if (temperatureReader != null) {
                temperatureReader.reset();
            }
            if (chartManager != null) {
                chartManager.resetChart();
            }
            return;
        }
        if (chartManager != null) {
            chartManager.resetChart();
        }

    }

    @Override
    public void onFinishSetup(String name, int status) {
        showProgress(false);
        if (status == BlueToothBuddy.SUCCESSFUL_CONNECTION) {
            connectedHeader.setText("Connected to " + name);
            temperatureReader = new TemperatureReader();
            try {
                temperatureReader.setInputStream(BlueToothBuddy.getInstance(this).getSocket().getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            temperatureReader.setCallback(this);
            temperatureReader.start();
            this.compositeSubscription = new CompositeSubscription();
            this.compositeSubscription.add(timeManager.getTimeSubscription(this));

        } else {
            connectedHeader.setText("Not connected");
        }
    }

    @Override
    public void onTempRecieved(String _temp) {
        currentTemperature.setText(_temp + "°");
        chartManager.addTemp(_temp);
        showProgress(false);
    }

    @Override
    public void onUpdateMinuteAverage(String avrTemp) {
        averageTemperature.setText(avrTemp + "°");
    }

    @Override
    public void onUpdateChange(String s) {
        averageChange.setText(s + "°");
    }

    @Override
    public void onDoubleParsed(double d) {
        doubleList.add(d);
    }

    @Override
    public void onTimeMeasured(String time) {
        this.time.setText(time);
    }
}
