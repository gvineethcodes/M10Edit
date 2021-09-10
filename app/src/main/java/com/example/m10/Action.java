package com.example.m10;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

import java.util.Calendar;

public class Action extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try{
            //Toast.makeText(context,"R",Toast.LENGTH_LONG).show();
            if(intent.getAction() != null){
                switch (intent.getAction()){

                    case "alarm":
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(new Intent(context,alarmService.class));
                        }else{
                            context.startService(new Intent(context,alarmService.class));
                        }

                    case "android.intent.action.BOOT_COMPLETED":

                        Intent notifyIntent = new Intent(context, Action.class).setAction("alarm");

                        final PendingIntent notifyPendingIntent = PendingIntent.getBroadcast(context, 1111, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                        final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

                        long repeatInterval = AlarmManager.INTERVAL_HOUR;

                        Calendar calendar = Calendar.getInstance();
                        calendar.set(Calendar.HOUR_OF_DAY,(calendar.get(Calendar.HOUR_OF_DAY)+1));
                        calendar.set(Calendar.MINUTE,0);
                        calendar.set(Calendar.SECOND,0);

                        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), repeatInterval, notifyPendingIntent);

                }
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
    }
}
