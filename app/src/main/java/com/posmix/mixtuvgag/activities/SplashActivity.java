package com.posmix.mixtuvgag.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import com.posmix.mixtuvgag.R;
import com.posmix.mixtuvgag.utils.CurrencyHelper;
import com.posmix.mixtuvgag.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CurrencyHelper.init(this);
        setContentView(R.layout.activity_splash);
        SessionManager sessionManager = new SessionManager(this);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Class<?> dest = sessionManager.isLoggedIn() ? MainActivity.class : LoginActivity.class;
            Intent i = new Intent(SplashActivity.this, dest);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i); finish();
        }, 2000);
    }
}
