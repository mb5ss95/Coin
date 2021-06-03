package com.example.coin.login;


import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.dd.processbutton.iml.ActionProcessButton;
import com.example.coin.MainActivity;
import com.example.coin.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity implements OnCompleteListener, OnFailureListener {

    private EditText mEdtEmail, mEdtPwd;
    private final String TAG = "LoginActivity";

    ActionProcessButton actionProcessButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getSupportActionBar().hide();
        InitWiget();
    }

    private void InitWiget() {
        mEdtEmail = findViewById(R.id.ac_login_edt_email);
        mEdtPwd = findViewById(R.id.ac_login_edt_pwd);

        int col1, col2, col3, col4;
        Resources res = getApplicationContext().getResources();

        col1 = res.getColor(R.color.col1);
        col2 = res.getColor(R.color.col2);
        col3 = res.getColor(R.color.col3);
        col4 = res.getColor(R.color.col4);

        actionProcessButton = findViewById(R.id.ac_login_btn_login);
        actionProcessButton.setColorScheme(col1, col2, col3, col4);
        actionProcessButton.setOnClickListener(v -> {
            String strEmail = mEdtEmail.getText().toString();
            String strPwd = mEdtPwd.getText().toString();

            actionProcessButton.setProgress(50);
            FirebaseAuth.getInstance().signInWithEmailAndPassword(strEmail, strPwd)
                    .addOnCompleteListener(LoginActivity.this, this).addOnFailureListener(this);
        });

    }

    @Override
    public void onComplete(@NonNull Task task) {
        if (task.isSuccessful()) {
            actionProcessButton.setProgress(100);
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
        actionProcessButton.setProgress(0);
        Log.e(TAG, "Login Fail : " + e);
    }

    public void initTitle() {
        setTitle("  Coin");
        ActionBar ab = getSupportActionBar();

        ab.setIcon(R.drawable.emotion);
        ab.setDisplayUseLogoEnabled(true);
        ab.setDisplayShowHomeEnabled(true);
    }
}