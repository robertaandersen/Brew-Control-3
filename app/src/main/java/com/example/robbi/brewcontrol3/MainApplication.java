package com.example.robbi.brewcontrol3;

import android.app.Application;

import java.util.Date;

/**
 * Created by robbi on 22/09/16.
 */
public class MainApplication extends Application {

    private static MainApplication brewControl = null;
    private long time;

    public MainApplication(){
        brewControl = this;
        this.time = new Date().getTime();
    }

    public static MainApplication getBrewControl() {
        return brewControl;
    }

    public long getTime() {
        return new Date().getTime() - time;
    }

    public void resetTimer() {
        this.time = new Date().getTime();
    }
}
