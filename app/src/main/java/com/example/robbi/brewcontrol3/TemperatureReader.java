package com.example.robbi.brewcontrol3;

import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by robbi on 22/09/16.
 */
public class TemperatureReader implements Runnable {

    private Thread workerThread;
    private InputStream inputStream;
    private TemperatureReaderCallbacks callbacks;
    private Handler handler;
    private List<Double> averageMeasures = new ArrayList<>();
    private List<Double> averageA = new ArrayList<>();
    private List<Double> averageB = new ArrayList<>();


    public void start() {
        handler = new Handler();
        workerThread = new Thread(this);
        workerThread.start();
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {

            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            String message = "";
            int bytesRead;
            try {
                bytesRead = inputStream.read(buffer);
                if (bytesRead != -1) {
                    while ((bytesRead == bufferSize) && (buffer[bufferSize - 1] != 0)) {
                        message = message + new String(buffer, 0, bytesRead);
                        bytesRead = inputStream.read(buffer);
                    }

                    message = message + new String(buffer, 0, bytesRead - 1);
                    final String finalMessage = message;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            String[] temps = finalMessage.split("\n");
                            if (temps.length >= 1) {
                                String temp = temps[temps.length - 1];
                                String t = "";
                                for (char c : temp.toCharArray()) {
                                    if (c == '-' || c == '>') {
                                        continue;
                                    }
                                    t += c;
                                }
                                if (!t.isEmpty()) {
                                    addToAverage(t);
                                    callbacks.onTempRecieved(t);
                                }
                            }
                        }
                    });
                    Thread.sleep(1000);
                }
            } catch (IOException e) {
                e.printStackTrace();
                workerThread.interrupt();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void addToAverage(String t) {
        try {
            double d = Double.valueOf(t);
            callbacks.onDoubleParsed(d);
            if(averageA.size() == 10){
                averageB.add(d);
                if(averageB.size() == 10) {
                    calculateAB();
                }
            } else {
                averageA.add(d);
            }

            if (averageMeasures.size() == 60) {
                double sum = 0;
                for (int i = 0; i < averageMeasures.size(); i++) {
                    sum += averageMeasures.get(i);
                }
                double roundOff = (double) Math.round((sum/60) * 100) / 100;
                callbacks.onUpdateMinuteAverage(String.valueOf(roundOff));
                averageMeasures = new ArrayList<>();
            }
            averageMeasures.add(d);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

    }

    private void calculateAB() {
        double sumA = 0;
        double sumB = 0;
        for(int i = 0; i < 10; i++){
            sumA += averageA.get(i);
            sumB += averageB.get(i);
        }
        averageA = new ArrayList<>();
        averageB = new ArrayList<>();
        double avA = sumA / 10;
        double avB = sumB / 10;
        double roundOff = (double) Math.round((avB-avA) * 100) / 100;
        callbacks.onUpdateChange(String.valueOf(roundOff));

    }

    public void setInputStream(InputStream inpuStream) {
        this.inputStream = inpuStream;
    }

    public void clear() {
        workerThread.interrupt();
    }

    public void setCallback(TemperatureReaderCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    public void reset() {
        averageMeasures = new ArrayList<>();
        averageA = new ArrayList<>();
        averageB = new ArrayList<>();

    }

    public interface TemperatureReaderCallbacks {
        void onTempRecieved(String temp);
        void onUpdateMinuteAverage(String avrTemp);
        void onUpdateChange(String s);
        void onDoubleParsed(double d);
    }
}
