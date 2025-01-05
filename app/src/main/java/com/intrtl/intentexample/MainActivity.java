package com.intrtl.intentexample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    private static final int ACTIVITY_RESULT_START_IR_REPORT = 1;
    private static final int ACTIVITY_RESULT_START_IR_VISIT = 2;
    private static final int ACTIVITY_RESULT_START_IR_SUMMARYREPORT = 3;
    private static final String IR_PACKAGE_NAME = "com.intelligenceretail.www.pilot";
    private static final String user = "solen.mobile.user"; // PARAM USER AUTH
    private static final String password = "Solentr.0624"; // PARAM PASS AUTH
    private static final String user_id = "34456"; // PARAM USER MERCH or PERFORMER
    private static final String visit_id = "test_Solen_05012024_new_ailet_test"; // NAME OF THE VISIT
    private static final String visit_id2 = "test-client2";
    private static final String store_id = "678091";  // STORE EXTERNAL ID
    private static final String store_id2 = "124324";
    private BroadcastReceiver shareShelfBroadcast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        shareShelfBroadcast = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                addlog("BROADCAST_SHARESHELF");
            }
        };

        this.registerReceiver(
                shareShelfBroadcast,
                new IntentFilter("IR_BROADCAST_SHARESHELF"));

        findViewById(R.id.btVisit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                Intent intent = new Intent();
                intent.setAction("com.intrtl.app.ACTION_VISIT");
                intent.putExtra("login", user);
                intent.putExtra("password", password);
                intent.putExtra("id", user_id);
                intent.putExtra("visit_id", visit_id);
                intent.putExtra("store_id", store_id);
                startActivityForResult(intent, ACTIVITY_RESULT_START_IR_VISIT);

            }
        });

        findViewById(R.id.btVisit2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent();
                intent.setAction("com.ailet.ACTION_VISIT");
                intent.putExtra("login", user);
                intent.putExtra("password", password);
                intent.putExtra("id", user_id);
                intent.putExtra("visit_id", visit_id2);
                intent.putExtra("store_id", store_id2);
                startActivityForResult(intent, ACTIVITY_RESULT_START_IR_VISIT);
            }
        });

        findViewById(R.id.btReport).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent();
                intent.setAction("com.intrtl.app.ACTION_REPORT");
                intent.putExtra("login", user);
                intent.putExtra("password", password);
                intent.putExtra("id", user_id);
                intent.putExtra("visit_id", visit_id);
                startActivityForResult(intent, ACTIVITY_RESULT_START_IR_REPORT);
            }
        });

        findViewById(R.id.btReport2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent();
                intent.setAction("com.intrtl.app.ACTION_REPORT");
                intent.putExtra("login", user);
                intent.putExtra("password", password);
                intent.putExtra("id", user_id);
                intent.putExtra("visit_id", visit_id2);
                startActivityForResult(intent, ACTIVITY_RESULT_START_IR_REPORT);
            }
        });

        findViewById(R.id.btSummaryReport).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent();
                intent.setAction("com.intrtl.app.ACTION_SUMMARY_REPORT");
                intent.putExtra("login", user);
                intent.putExtra("password", password);
                intent.putExtra("id", user_id);
                intent.putExtra("visit_id", visit_id2);
                startActivityForResult(intent, ACTIVITY_RESULT_START_IR_SUMMARYREPORT);
            }
        });

        findViewById(R.id.btSummaryReport2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent();
                intent.setAction("com.intrtl.app.ACTION_SUMMARY_REPORT");
                intent.putExtra("login", user);
                intent.putExtra("password", password);
                intent.putExtra("id", user_id);
                intent.putExtra("visit_id", visit_id2);
                startActivityForResult(intent, ACTIVITY_RESULT_START_IR_SUMMARYREPORT);

            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            String mode = "";
            switch (requestCode) {

                case (ACTIVITY_RESULT_START_IR_REPORT):
                    mode = "reports";
                    break;

                case (ACTIVITY_RESULT_START_IR_VISIT):
                    mode = "visit";
                    break;

                case (ACTIVITY_RESULT_START_IR_SUMMARYREPORT):
                    mode = "summaryReport";
                    break;
            }

            if (data.getData() != null) {
                String result = readFromUri(data.getData());
                Log.i("", result);

                try {
                    JSONObject json = new JSONObject(result);
                    json.put("report", "To many report data...");
                    addlog("Mode: " + mode + "\n" + json.toString());

                    Toast.makeText(getBaseContext(), mode + " " + json.getString("status"), Toast.LENGTH_LONG).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else {
            if (data.getExtras() != null)
                Toast.makeText(getBaseContext(), "ERROR_ACTIVITY_RESULT " + data.getExtras().getString("error"), Toast.LENGTH_LONG).show();
        }
    }

    private void addlog(String text){
        EditText logEditText = findViewById(R.id.logEditText);
        logEditText.setText(text);

    }

    private String readFromUri(Uri uri){
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            InputStreamReader isReader = new InputStreamReader(inputStream);
            BufferedReader reader = new BufferedReader(isReader);
            StringBuffer sb = new StringBuffer();
            String str;
            while((str = reader.readLine())!= null){
                sb.append(str);
            }

            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();

            return null;
        }
    }
}
