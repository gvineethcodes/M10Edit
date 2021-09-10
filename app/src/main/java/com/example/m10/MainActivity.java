package com.example.m10;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    private static MainActivity ins;
    SharedPreferences sharedpreferences;
    SharedPreferences.Editor editor;
    Spinner spinner, spinner2;
    ImageButton imageButton, imageButton2, imageButton3;
    TextView textView;
    StorageReference mStorageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ins = this;

        spinner = findViewById(R.id.spinner);
        spinner2 = findViewById(R.id.spinner2);
        imageButton = findViewById(R.id.imageButton);
        imageButton2 = findViewById(R.id.imageButton2);
        imageButton3 = findViewById(R.id.imageButton3);
        textView = findViewById(R.id.textview);

        sharedpreferences = getSharedPreferences("MyM10", Context.MODE_PRIVATE);
        editor = sharedpreferences.edit();

        imageButton.setEnabled(false);
        imageButton2.setEnabled(false);
        imageButton3.setEnabled(false);

        mStorageRef = FirebaseStorage.getInstance().getReference();
        mStorageRef.listAll()
                .addOnSuccessListener(new OnSuccessListener<ListResult>() {
                    @Override
                    public void onSuccess(ListResult listResult) {
                        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, new ArrayList<String>());
                        spinner.setAdapter(arrayAdapter);

                        for (StorageReference prefix : listResult.getPrefixes()) {
                            arrayAdapter.add(prefix.getName());
                        }
                        spinner.setSelection(sharedpreferences.getInt("SubjectPosition", 0));
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        textView.setText(e.toString());
                    }
                });


        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                String subjectName = spinner.getSelectedItem().toString();
                mStorageRef.child(subjectName).listAll()
                        .addOnSuccessListener(new OnSuccessListener<ListResult>() {
                            @Override
                            public void onSuccess(ListResult listResult) {
                                ArrayAdapter<String> arrayAdapter2 = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, new ArrayList<String>());
                                spinner2.setAdapter(arrayAdapter2);

                                keepInSharedPreferences("topicSize",listResult.getItems().size());
                                int i=0;
                                for (StorageReference item : listResult.getItems()) {
                                    String name = item.getName();
                                    arrayAdapter2.add(name);
                                    keepInSharedPreferences(name,i);
                                    keepStringSharedPreferences(""+i,name);
                                    i=i+1;
                                }
                                spinner2.setSelection(sharedpreferences.getInt("TopicPosition", 0));
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                textView.setText(e.toString());
                            }
                        });
                keepInSharedPreferences("SubjectPosition", i);
                keepStringSharedPreferences("subject", subjectName);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        spinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                keepInSharedPreferences("TopicPosition", i);
                keepStringSharedPreferences("topic", spinner2.getSelectedItem().toString());

                if(sharedpreferences.getInt("play",0)==1){
                    playService.getInstance().play();
                    keepInSharedPreferences("play",0);
                }else {
                    imageButton.setEnabled(true);
                    imageButton2.setEnabled(true);
                    imageButton3.setEnabled(true);
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(getApplicationContext(),playService.class));
        }else{
            startService(new Intent(getApplicationContext(),playService.class));
        }

        if(sharedpreferences.getBoolean("one",true)){

            Intent notifyIntent = new Intent(this, Action.class).setAction("alarm");

            final PendingIntent notifyPendingIntent = PendingIntent.getBroadcast(this, 1111, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            final AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

            long repeatInterval = AlarmManager.INTERVAL_HOUR;

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY,(calendar.get(Calendar.HOUR_OF_DAY)+1));
            calendar.set(Calendar.MINUTE,0);
            calendar.set(Calendar.SECOND,0);

            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), repeatInterval, notifyPendingIntent);
            editor.putBoolean("one", false);
            editor.commit();
        }
}

    @Override
    protected void onResume() {
        super.onResume();

        imageButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //keepInSharedPreferences("activity",1);
                playService.getInstance().playPause();
//                startService(new Intent(getApplicationContext(),playService.class).setAction("playPause"));
                //sendBroadcast(new Intent(getApplicationContext(),Action.class).setAction("playPause"));
            }
        });

        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                prev();
//                startService(new Intent(getApplicationContext(),playService.class).setAction("playPause"));
                //sendBroadcast(new Intent(getApplicationContext(),Action.class).setAction("playPause"));
            }
        });

        imageButton3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                next();
//                startService(new Intent(getApplicationContext(),playService.class).setAction("playPause"));
                //sendBroadcast(new Intent(getApplicationContext(),Action.class).setAction("playPause"));
            }
        });

    }

    private void keepInSharedPreferences(String keyStr, int valueInt) {
        editor.putInt(keyStr, valueInt);
        editor.apply();
    }

    private void keepStringSharedPreferences(String keyStr1, String valueStr1) {
        editor.putString(keyStr1, valueStr1);
        editor.apply();
    }

    public static MainActivity getInstance() {
        return ins;
    }

    public void prev(){
        int prev = sharedpreferences.getInt("TopicPosition",0)-1;
        if(prev > -1) {
            spinner2.setSelection(prev);
            keepInSharedPreferences("play",1);
            //playService.getInstance().prev();
        }
    }

    public void next(){
        int next = sharedpreferences.getInt("TopicPosition",0)+1;
        if(next < sharedpreferences.getInt("topicSize",0)) {
            spinner2.setSelection(next);
            keepInSharedPreferences("play", 1);
            //playService.getInstance().next();
        }
    }

    public void enableButtons(String text){
        imageButton.setEnabled(true);
        imageButton2.setEnabled(true);
        imageButton3.setEnabled(true);
        textView.setText(text);
    }
    public void disableButtons(String text){
        imageButton.setEnabled(false);
        imageButton2.setEnabled(false);
        imageButton3.setEnabled(false);
        textView.setText(text);
    }

    public void playImage(int i){
        imageButton2.setImageResource(i);
    }



}