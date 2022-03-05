package com.arnoldiii.taxiapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;

public class DriverOrClientActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_or_passenger);

    }
    //start activity for passenger authorization
    public void goToPassengerSignIn(View view) {
        startActivity(new Intent(
                DriverOrClientActivity.this,PassengerSignInActivity.class));
    }
    //start activity for driver authorization
    public void goToDriverSingIn(View view) {
        startActivity(new Intent(
                DriverOrClientActivity.this,DriverSignInActivity.class));
    }
}