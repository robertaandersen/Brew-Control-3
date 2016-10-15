package com.example.robbi.brewcontrol3;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by robbi on 22/09/16.
 */
public class TimeManager {

    public Subscription getTimeSubscription(final TimeManagerCallbacks timeManagerCallbacks) {
        return Observable.interval(1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Long>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Long aLong) {
                        long time = MainApplication.getBrewControl().getTime();
                        long secs = time / 1000;
                        long min = secs / 60;
                        long hours = min / 60;
                        String ss = String.valueOf(secs % 60);
                        ss = secs < 10 ? "0" + ss : ss;
                        String mm = String.valueOf(min % 60);
                        mm = min < 10 ? "0" + mm : mm;
                        String timeFormat = hours + ":" + mm  + ":" + ss;
                        timeManagerCallbacks.onTimeMeasured(timeFormat);
                    }
                });
    }

    public interface TimeManagerCallbacks {
        void onTimeMeasured(String time);
    }

}
