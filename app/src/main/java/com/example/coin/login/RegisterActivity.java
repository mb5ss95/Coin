package com.example.coin.login;


import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.coin.R;
import com.example.coin.model.UserAccount;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;


public class RegisterActivity extends AppCompatActivity implements OnCompleteListener<AuthResult> {

    private EditText mEdtEmail, mEdtPwd;
    private String TAG = "RegisterActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        Init_wiget();
    }

    private void Init_wiget() {
        mEdtEmail = findViewById(R.id.ac_register_edt_email);
        mEdtPwd = findViewById(R.id.ac_register_edt_pwd);

        findViewById(R.id.ac_register_btn_register).setOnClickListener(v -> {
            String strEmail = mEdtEmail.getText().toString();
            String strPwd = mEdtPwd.getText().toString();

            //Firebase Auth 진행
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(strEmail, strPwd)
                    .addOnCompleteListener(RegisterActivity.this, this);
        });
    }

    @Override
    public void onComplete(@NonNull Task<AuthResult> task) {
        if (task.isSuccessful()) {
            FirebaseUser firebaseUser = task.getResult().getUser();
            UserAccount account = new UserAccount();
            account.setIdToken(firebaseUser.getUid());
            account.setEmailId(firebaseUser.getEmail());
            account.setPassword(mEdtPwd.getText().toString());
            account.setFriends(" ");
            account.setRooms(" ");
            FirebaseDatabase.getInstance()
                    .getReference()
                    .child(firebaseUser.getUid())
                    .setValue(account);

            /*.child(firebaseUser.getUid())*/
            Toast.makeText(RegisterActivity.this, "회원가입에 성공하셨습니다!!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(RegisterActivity.this, "회원가입 실패..", Toast.LENGTH_SHORT).show();
        }
    }
}