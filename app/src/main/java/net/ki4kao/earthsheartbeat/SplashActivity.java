package net.ki4kao.earthsheartbeat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Two-stage splash:
 *   stage 1  ->  1.png (drawable splash1)  for 2 seconds
 *   stage 2  ->  2.jpg (drawable splash2)  for 2 seconds
 * then hands off to MainActivity. The window background is already splash1
 * (via Theme.Heartbeat.Splash) so there is no white flash on cold start.
 */
public class SplashActivity extends AppCompatActivity {

    private static final long STAGE_MS = 2000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        final ImageView image = findViewById(R.id.splashImage);
        image.setImageResource(R.drawable.splash1);

        final Handler h = new Handler(Looper.getMainLooper());

        // After 2s show the second splash image.
        h.postDelayed(new Runnable() {
            @Override public void run() {
                image.setImageResource(R.drawable.splash2);
            }
        }, STAGE_MS);

        // After 4s total, launch the main WebView screen.
        h.postDelayed(new Runnable() {
            @Override public void run() {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }
        }, STAGE_MS * 2);
    }
}
