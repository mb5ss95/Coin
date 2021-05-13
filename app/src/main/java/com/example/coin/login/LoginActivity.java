package com.example.coin.login;


import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.example.coin.MainActivity;
import com.example.coin.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity implements OnCompleteListener, OnFailureListener {

    private EditText mEdtEmail, mEdtPwd;
    private final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        InitWiget();
    }

    private void InitWiget() {
        mEdtEmail = findViewById(R.id.ac_login_edt_email);
        mEdtPwd = findViewById(R.id.ac_login_edt_pwd);

        findViewById(R.id.ac_login_btn_login).setOnClickListener(v -> {
            String strEmail = mEdtEmail.getText().toString();
            String strPwd = mEdtPwd.getText().toString();

            FirebaseAuth.getInstance().signInWithEmailAndPassword(strEmail, strPwd)
                    .addOnCompleteListener(LoginActivity.this, this).addOnFailureListener(this);
        });

        findViewById(R.id.ac_login_btn_register).setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this,  RegisterActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onComplete(@NonNull Task task) {
        if(task.isSuccessful()){
            Toast.makeText(LoginActivity.this, "로그인 성공하셨습니다!!", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(LoginActivity.this, "로그인 실패..", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onFailure(@NonNull Exception e) {
        Toast.makeText(LoginActivity.this, "로그인 실패..잠시 후 다시 시도 하세요.", Toast.LENGTH_SHORT).show();
        Log.e(TAG, "Login Fail : " + e);
    }

    public void initTitle() {
        setTitle("  WWWhisper Project");
        ActionBar ab = getSupportActionBar();

        ab.setIcon(R.drawable.emotion);
        ab.setDisplayUseLogoEnabled(true);
        ab.setDisplayShowHomeEnabled(true);
    }
}