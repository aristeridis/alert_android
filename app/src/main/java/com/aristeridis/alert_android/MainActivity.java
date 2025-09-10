package com.aristeridis.alert_android;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;

public class MainActivity extends AppCompatActivity {

    private FirebaseAnalytics mFirebaseAnalytics;
    private static final String TAG = "SmartAlert";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Αρχικοποίηση Firebase
        FirebaseApp.initializeApp(this);

        // Firebase Analytics
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // Στέλνουμε ένα test event
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.METHOD, "app_start");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, bundle);

        Log.d(TAG, "🔥 Firebase initialized and test event sent!");
    }
}