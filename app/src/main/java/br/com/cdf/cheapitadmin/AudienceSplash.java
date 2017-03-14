package br.com.cdf.cheapitadmin;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by jonathansuenaga on 3/9/17.
 */

public class AudienceSplash extends AppCompatActivity {

    private static int SPLASH_TIME_OUT = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audience_splash);

        new Handler().postDelayed(new Runnable() {

         /*
          * Showing splash screen with a timer. This will be useful when you
          * want to show case your app logo / company
          */

            @Override
            public void run() {
                Intent i = new Intent(AudienceSplash.this, Splash.class);
                startActivity(i);   //

                finish();
            }
        }, SPLASH_TIME_OUT);
    }
}

