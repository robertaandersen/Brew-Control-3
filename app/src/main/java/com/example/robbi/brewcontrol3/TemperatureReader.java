package com.example.robbi.brewcontrol3;

import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by robbi on 22/09/16.
 */
public class TemperatureReader implements Runnable {

    private Thread workerThread;
    private InputStream inputStream;
    private TemperatureReaderCallbacks callbacks;
    private Handler handler;


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
                                callbacks.onTempRecieved(t);
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

    public void setInputStream(InputStream inpuStream) {
        this.inputStream = inpuStream;
    }

    public void clear() {
        workerThread.interrupt();
    }

    public void setCallback(TemperatureReaderCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    public interface TemperatureReaderCallbacks {
        void onTempRecieved(String temp);
    }
}
