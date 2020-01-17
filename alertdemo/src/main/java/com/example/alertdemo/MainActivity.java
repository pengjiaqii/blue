package com.example.alertdemo;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private AlertDialogUtil mAlertDialogUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAlertDialogUtil = AlertDialogUtil.getInstance();
    }


    public void showNormalDialog(View view) {
        mAlertDialogUtil.showNormalDialog();
    }

    public void showListDialog(View view) {
        mAlertDialogUtil.showListDialog();
    }

    public void showSingleChoiceDialog(View view) {
        mAlertDialogUtil.showSingleChoiceDialog();
    }

    public void showMultiChoiceDialog(View view) {
        mAlertDialogUtil.showMultiChoiceDialog();
    }

    public void showCustomDialog(View view) {
        mAlertDialogUtil.showCustomDialog();
    }
}
