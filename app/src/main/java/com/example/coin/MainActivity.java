package com.example.coin;


import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.coin.login.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mFirebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFirebaseAuth = FirebaseAuth.getInstance();

        Init_wiget();
    }

    private void Init_wiget() {
        findViewById(R.id.ac_main_btn_logout).setOnClickListener(v -> {
            mFirebaseAuth.signOut();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });


        findViewById(R.id.ac_main_btn_delete).setOnClickListener(v -> {
            //탈퇴처리
            mFirebaseAuth.getCurrentUser().delete();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });


        findViewById(R.id.ac_main_btn_start).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, StartActivity.class);
            startActivity(intent);
        });
    }
}