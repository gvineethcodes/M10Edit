package com.example.m10;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;

public class alarmService extends Service {

    SharedPreferences sharedpreferences;
    SharedPreferences.Editor editor;
    MediaPlayer mediaPlayer = null;
    StorageReference mStorageRef;
    String subject, notify, topic="";
    NotificationManager notificationManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        sharedpreferences = getSharedPreferences("MyM10", Context.MODE_PRIVATE);
        editor = sharedpreferences.edit();
        mStorageRef = FirebaseStorage.getInstance().getReference();

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        try{
            if(intent.getAction()!=null) {
                if (intent.getAction().equals("close")) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        stopForeground(true);
                    } else {
                        notificationManager.cancel(2);
                    }
                    stopSelf();
                }
            }else play();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return START_STICKY;
    }

    public void play() {
        topic = sharedpreferences.getString("topic", "");
        subject=sharedpreferences.getString("subject", "");
        notify="preparing "+topic;

        showNotification();

        mStorageRef.child(subject)
                .child(topic)
                .getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                try {
                    mediaPlayer = new MediaPlayer();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        mediaPlayer.setAudioAttributes(
                                new AudioAttributes.Builder()
                                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                        .setUsage(AudioAttributes.USAGE_MEDIA)
                                        .build()
                        );
                    }
                    mediaPlayer.setDataSource(getApplicationContext(), uri);
                    mediaPlayer.prepareAsync();

                    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mediaPlayer) {
                            mediaPlayer.start();

                            notify=topic;
                            showNotification();

                        }
                    });
                    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mediaPlayer) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                stopForeground(true);
                            }else {
                                notificationManager.cancel(2);
                            }
                            stopSelf();
                        }
                    });
                    mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                        @Override
                        public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                stopForeground(true);
                            }else {
                                notificationManager.cancel(2);
                            }
                            stopSelf();
                            return false;
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        mStorageRef.child(subject)
                .child(topic)
                .getDownloadUrl().addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    stopForeground(true);
                }else {
                    notificationManager.cancel(2);
                }
                stopSelf();
            }
        });
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("2", "Hour", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("hour to hour");
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void showNotification() {

            PendingIntent playPI,prevPI,nextPI;

            Intent playI = new Intent(this, alarmService.class).setAction("close");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                playPI = PendingIntent.getForegroundService(this, 90, playI, PendingIntent.FLAG_UPDATE_CURRENT);
            }else{
                playPI = PendingIntent.getService(this, 90, playI, PendingIntent.FLAG_UPDATE_CURRENT);
            }

            Notification notification = new NotificationCompat.Builder(this, "2")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(subject)
                    .setContentText(notify)
                    .addAction(0, "close", playPI)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOnlyAlertOnce(true)
                    .build();

            notificationManager.notify(2, notification);


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            if(mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

}
