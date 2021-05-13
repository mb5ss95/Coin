package com.example.coin.login;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.coin.R;

public class LaunchActivity extends AppCompatActivity {

    /*
    implementation fileTree(include: ['*.jar'], dir: 'libs')
    implementation files('libs\\autobanh.jar')
     */

    static final int PERMISSIONS_REQUEST_CODE = 444;
    String[] PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!CheckPermisson(PERMISSIONS)) {

                //퍼미션 허가가 안되어있다면 사용자에게 요청
                requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            } else {
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        }


    }

    private boolean CheckPermisson(String[] permissons) {
        int result;

        for (String temp : permissons) {
            result = ContextCompat.checkSelfPermission(this, temp); //체크 권한

            if (result == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE:

                if (grantResults.length > 0) {
                    boolean cameraPermissionAccepted = grantResults[0]
                            == PackageManager.PERMISSION_GRANTED;
                    boolean diskPermissionAccepted = grantResults[1]
                            == PackageManager.PERMISSION_GRANTED;
                    boolean audioPermissonAccepted = grantResults[1]
                            == PackageManager.PERMISSION_GRANTED;

                    if (!cameraPermissionAccepted || !diskPermissionAccepted || !audioPermissonAccepted) {
                        showDialogForPermission("앱을 실행하려면 권한을 허가 하세요!!");
                        finish();
                    } else {
                        Intent intent = new Intent(this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission(String msg) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                finish();
            }
        });
        builder.create().show();
    }
}
