package com.istandev.diponews;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

/**
 * Created by ADIK on 12/05/2015.
 */
public class Splashscreen extends Activity {
    private static int splashInterval = 3000;
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
            Intent i = new Intent(Splashscreen.this, MainActivity.class);
            startActivity(i);
            //Menyelesaikan Splashscreen
             Splashscreen.this.finish();
            }
        }, splashInterval);
    };

}
