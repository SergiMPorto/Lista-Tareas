package com.example.todolist;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.VideoView;

public class Splash extends AppCompatActivity {
    private static final int SPLASH_TIME_OUT = 5200;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        VideoView videoViewSplash = findViewById(R.id.videoSplash);

        // Establece la ruta del video en la carpeta "raw"
        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.splash);

        // Establece la URI del video en el VideoView
        videoViewSplash.setVideoURI(videoUri);

        // Inicia la reproducción del video
        videoViewSplash.start();

        // Espera durante el tiempo definido y luego inicia la siguiente actividad
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(Splash.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }, SPLASH_TIME_OUT);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pausa la reproducción del video cuando la actividad está en pausa
        VideoView videoViewSplash = findViewById(R.id.videoSplash);
        if (videoViewSplash.isPlaying()) {
            videoViewSplash.pause();
        }
    }

}
