package com.example.m10;

import android.app.AlarmManager;
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
import android.support.v4.media.session.MediaSessionCompat;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.Calendar;

public class playService extends Service {

    private static playService playServiceInstance;
    SharedPreferences sharedpreferences;
    SharedPreferences.Editor editor;
    MediaPlayer mediaPlayer = null;
    MediaSessionCompat mediaSessionCompat;
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
        //Toast.makeText(this,"c",Toast.LENGTH_LONG).show();

        playServiceInstance=this;

        sharedpreferences = getSharedPreferences("MyM10", Context.MODE_PRIVATE);
        editor = sharedpreferences.edit();
        mStorageRef = FirebaseStorage.getInstance().getReference();

        mediaSessionCompat = new MediaSessionCompat(this, "mytagmediam9");
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        createNotificationChannel();


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
         super.onStartCommand(intent, flags, startId);
        //Toast.makeText(this,"s",Toast.LENGTH_LONG).show();

        try{
            if(intent.getAction() != null){
                switch (intent.getAction()){
                    case "playPause":
                        playPause();
                        break;
                    case "prev":
                        MainActivity.getInstance().prev();
                        break;
                    case "next":
                        MainActivity.getInstance().next();
                        break;

                }
            }
            if(mediaPlayer!=null){
                if(mediaPlayer.isPlaying()) MainActivity.getInstance().playImage(R.drawable.ic_baseline_pause_24);
                MainActivity.getInstance().enableButtons(topic);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        return START_STICKY;
    }

    public void playPause() {
        //Toast.makeText(this,"p",Toast.LENGTH_LONG).show();

        if (mediaPlayer != null && sharedpreferences.getString("topic", "").equals(sharedpreferences.getString("playingTopic", ""))) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                MainActivity.getInstance().playImage(R.drawable.ic_baseline_play_arrow_24);
//
//                if(sharedpreferences.getInt("activity",0)==1){
//                    keepInSharedPreferences("activity",0);
//                }
                showNotification(true,R.drawable.ic_baseline_play_arrow_24);
            }
            else {
                mediaPlayer.start();
                MainActivity.getInstance().playImage(R.drawable.ic_baseline_pause_24);

//                if(sharedpreferences.getInt("activity",0)==1){
//                    keepInSharedPreferences("activity",0);
//                }
                showNotification( true, R.drawable.ic_baseline_pause_24);
            }

        } else play();

    }

    public void play() {
        topic = sharedpreferences.getString("topic", "");
        subject=sharedpreferences.getString("subject", "");
        notify="preparing "+topic;
        MainActivity.getInstance().disableButtons(notify);

//        if(sharedpreferences.getInt("activity",0)==1){
//        }
        showNotification(false,R.drawable.ic_baseline_play_arrow_24);
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
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
                            MainActivity.getInstance().enableButtons(topic);
                            MainActivity.getInstance().playImage(R.drawable.ic_baseline_pause_24);

//                            if(sharedpreferences.getInt("activity",0)==1){
//                                keepInSharedPreferences("activity",0);
//                            }
                            notify=topic;
                            showNotification(true,R.drawable.ic_baseline_pause_24);
                            keepStringSharedPreferences("playingTopic", sharedpreferences.getString("topic", ""));

                        }
                    });
                    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mediaPlayer) {
                            MainActivity.getInstance().playImage(R.drawable.ic_baseline_play_arrow_24);

//                            if(sharedpreferences.getInt("activity",0)==1){
//                                //MainActivity.getInstance().enableButtons(topic);
//                                keepInSharedPreferences("activity",0);
//                            }
                            showNotification(true,R.drawable.ic_baseline_play_arrow_24);

                        }
                    });
                    mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                        @Override
                        public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
//                            if(sharedpreferences.getInt("activity",0)==1){
//                                keepInSharedPreferences("activity",0);
//                            }
                            MainActivity.getInstance().enableButtons("Try again "+topic);
                            MainActivity.getInstance().playImage(R.drawable.ic_baseline_play_arrow_24);

                            notify="Try again "+topic;
                            showNotification(true,R.drawable.ic_baseline_play_arrow_24);
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
                MainActivity.getInstance().enableButtons(e.getMessage());
                MainActivity.getInstance().playImage(R.drawable.ic_baseline_play_arrow_24);

//                if(sharedpreferences.getInt("activity",0)==1){
//                    keepInSharedPreferences("activity",0);
//                }
                notify=e.getMessage()+"\nTry again "+topic;
                showNotification(true,R.drawable.ic_baseline_play_arrow_24);

            }
        });
    }

//    public void prev(){
//        if(sharedpreferences.getInt("activity",0)==1){
//            play();
//        }else{
//            int prev = sharedpreferences.getInt("TopicPosition",0)-1;
//            if(prev > -1){
//                keepStringSharedPreferences("topic",sharedpreferences.getString(""+prev,""));
//                play();
//            }
//        }
//    }
//
//    public void next(){
//        if(sharedpreferences.getInt("activity",0)==1){
//            play();
//        }else{
//            int next = sharedpreferences.getInt("TopicPosition",0)+1;
//            if(next < sharedpreferences.getInt("topicSize",0)){
//                keepStringSharedPreferences("topic",sharedpreferences.getString(""+next,""));
//                play();
//            }
//        }
//    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("1", "play", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Media controls");
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void showNotification(boolean showButtons, int playPause) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        if(showButtons) {
            PendingIntent playPI,prevPI,nextPI;

            Intent playI = new Intent(this, playService.class).setAction("playPause");
            Intent prevI = new Intent(this, playService.class).setAction("prev");
            Intent nextI = new Intent(this, playService.class).setAction("next");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                playPI = PendingIntent.getForegroundService(this, 90, playI, PendingIntent.FLAG_UPDATE_CURRENT);
                prevPI = PendingIntent.getForegroundService(this, 9, prevI, PendingIntent.FLAG_UPDATE_CURRENT);
                nextPI = PendingIntent.getForegroundService(this, 80, nextI, PendingIntent.FLAG_UPDATE_CURRENT);
            }else{
                playPI = PendingIntent.getService(this, 90, playI, PendingIntent.FLAG_UPDATE_CURRENT);
                prevPI = PendingIntent.getService(this, 9, prevI, PendingIntent.FLAG_UPDATE_CURRENT);
                nextPI = PendingIntent.getService(this, 80, nextI, PendingIntent.FLAG_UPDATE_CURRENT);
            }



            Notification notification = new NotificationCompat.Builder(this, "1")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(subject)
                    .setContentText(notify)
                    .setContentIntent(contentIntent)
                    .addAction(R.drawable.ic_baseline_skip_previous_24, "prev", prevPI)
                    .addAction(playPause, "play", playPI)
                    .addAction(R.drawable.ic_baseline_skip_next_24, "next", nextPI)
                    .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                            .setMediaSession(mediaSessionCompat.getSessionToken()))
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOnlyAlertOnce(true)
                    .build();

            notificationManager.notify(1, notification);
        }else{

            Notification notification = new NotificationCompat.Builder(this, "1")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(subject)
                    .setContentText(notify)
                    .setContentIntent(contentIntent)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOnlyAlertOnce(true)
                    .build();

            notificationManager.notify(1, notification);
        }

    }

    private void keepInSharedPreferences(String keyStr, int valueInt) {
        editor.putInt(keyStr, valueInt);
        editor.apply();
    }

    private void keepStringSharedPreferences(String keyStr1, String valueStr1) {
        editor.putString(keyStr1, valueStr1);
        editor.apply();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            if(mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        Toast.makeText(this,"d",Toast.LENGTH_LONG).show();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
        }else {
            notificationManager.cancel(1);
        }
        stopSelf();
        Toast.makeText(this,"t",Toast.LENGTH_LONG).show();

    }

    public static playService getInstance() {
        return playServiceInstance;
    }

}
