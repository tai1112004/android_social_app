package com.yourapp;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;
import com.yourapp.util.BackendConfig;

public class SocialApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        BackendConfig.init(this);
    }
}