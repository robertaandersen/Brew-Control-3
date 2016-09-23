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
                        Date d = new Date(MainApplication.getBrewControl().getTime());
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
                        timeManagerCallbacks.onTimeMeasured(simpleDateFormat.format(d));
                    }
                });
    }

    public interface TimeManagerCallbacks {
        void onTimeMeasured(String time);
    }

}
