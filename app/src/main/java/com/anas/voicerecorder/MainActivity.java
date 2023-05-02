package com.anas.voicerecorder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.dynamiclinks.DynamicLink;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {


    MediaRecorder mediaRecorder;
    MediaPlayer mediaPlayer;

    String link = " ";

    Calendar calendar = Calendar.getInstance();
    int year = calendar.get(Calendar.YEAR);
    int month = calendar.get(Calendar.MONTH);
    int day = calendar.get(Calendar.DAY_OF_MONTH);
    String currentDate = "_" + day + ":" + (month + 1) + ":" + year + "_";
    int hour = calendar.get(Calendar.HOUR_OF_DAY);
    int minute = calendar.get(Calendar.MINUTE);
    String currentTime = hour + ":" + minute;

    Button btnStart, btnStop, btnPlay;

    Handler handler = new Handler();
    int DELAY = 5000; // 30 seconds in milliseconds
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        btnPlay = findViewById(R.id.btnPlay);

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                if (MainActivity.this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.RECORD_AUDIO)
                            == PackageManager.PERMISSION_DENIED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, 100);
                    }
//                } else {
//                    Toast.makeText(MainActivity.this, "No mic", Toast.LENGTH_SHORT).show();
//                }

                try {
                    mediaRecorder = new MediaRecorder();
                    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                    mediaRecorder.setOutputFile(getRecordingFilePath());
                    mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                    mediaRecorder.prepare();
                    mediaRecorder.start();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // Stop recording
                            mediaRecorder.stop();
                            mediaRecorder.release();
                            mediaRecorder = null;
                            sendAudio();
                            Toast.makeText(MainActivity.this, "Recording stopped automatically!", Toast.LENGTH_LONG).show();
                        }
                    }, DELAY);


                    Toast.makeText(MainActivity.this,"Recording started!!",Toast.LENGTH_LONG).show();
                }
                catch(Exception e){
                    e.printStackTrace();
                }

            }
        });


        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                sendAudio();
                Toast.makeText(MainActivity.this,"Recording stoped!!",Toast.LENGTH_LONG).show();
            }
        });

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    mediaPlayer = new MediaPlayer();
                    mediaPlayer.setDataSource(getRecordingFilePath());
                    mediaPlayer.prepare();
                    mediaPlayer.start();

                    Toast.makeText(MainActivity.this, "Recording is playing!!", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    e.printStackTrace();

                }
            }
        });
    }





    private String getRecordingFilePath() {
        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File musicDirectory = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        File file = new File(musicDirectory, "testRecordingFile.mp3");
        return file.getPath();
    }


    public void sendAudio() {

        Uri uriRec = Uri.fromFile(new File(getRecordingFilePath()));

        FirebaseStorage fdbs = FirebaseStorage.getInstance();
        StorageReference roots = fdbs.getReference().child("LOCATION_IMG/").child("testRecordingFile.mp3");

        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("audio/AMR_NB")
                .build();
        roots.putFile(uriRec,metadata)
                .addOnSuccessListener(taskSnapshot -> {
                    roots.getDownloadUrl().addOnSuccessListener(uri -> {

                        FirebaseDynamicLinks.getInstance().createDynamicLink()
                                .setLink(Uri.parse(uri.toString()))
                                .setDomainUriPrefix("https://rakshak2.page.link")
                                .setAndroidParameters(
                                        new DynamicLink.AndroidParameters.Builder()
                                                .setMinimumVersion(1)
                                                .build())
                                .buildShortDynamicLink()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Uri shortLink = task.getResult().getShortLink();
                                        link = shortLink.toString();
                                        System.out.println(link);
                                        Toast.makeText(this, link, Toast.LENGTH_SHORT).show();
                                    } else {
                                        // Handle error
                                    }
                                });

                    });
                })
                .addOnFailureListener(e -> {

                });
    }
}