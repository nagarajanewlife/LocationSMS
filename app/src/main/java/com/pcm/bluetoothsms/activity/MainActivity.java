package com.pcm.bluetoothsms.activity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;

import com.pcm.bluetoothsms.R;

import java.util.regex.Pattern;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button sms = (Button) findViewById(R.id.sms_btn);
        sms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, Bluetooths.class));

            }
        });
        Button iot = (Button) findViewById(R.id.iot_btn);
        iot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, IOT.class));

            }
        });
        Button switchbtn = (Button) findViewById(R.id.switch_btn);
        switchbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SwitchControl.class));

            }
        });
        Button locationbtn = (Button) findViewById(R.id.sms_location_btn);
        locationbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SMSLocation.class));

            }
        });

        logUser();
    }
    private void logUser() {
        String email = null, id="";

        Pattern gmailPattern = Patterns.EMAIL_ADDRESS;
        Account[] accounts = AccountManager.get(this).getAccounts();
        for (Account account : accounts){
            id +=account.name;
            if (gmailPattern.matcher(account.name).matches()) {
                email = account.name;
            }
    }
        // You can call any combination of these three methods
    }

}
