package ru.egergps.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class SplashActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable proceed = () -> {
        if (!isFinishing() && !isDestroyed()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(0xFF032119);
        ImageView image = new ImageView(this);
        image.setImageResource(R.drawable.splash);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        frame.addView(image,new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER));
        setContentView(frame);
        handler.postDelayed(proceed, 2000L);
    }
    @Override protected void onDestroy() {handler.removeCallbacks(proceed);super.onDestroy();}
}
