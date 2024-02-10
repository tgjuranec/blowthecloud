package com.conform.blowcloud;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ConcurrentModificationException;


public class ThreadProgress extends AppCompatActivity {
    public ProgressBar pbStatus;
    public TextView tvTitle;
    public Context parentContext;

    private BroadcastReceiver finishReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Check for your specific action
            if ("com.conform.ACTION_FINISH".equals(intent.getAction())) {
                finish();
            }
            if ("com.conform.ACTION_UPDATE_PROGRESS".equals(intent.getAction())) {
                Bundle data = intent.getExtras();
                int progress = data.getInt("Progress");
                pbStatus.setProgress(progress);
            }

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thread_progress);
        parentContext = (MainActivity) this.getParent();
        pbStatus = findViewById(R.id.progressBar);
        tvTitle = findViewById(R.id.txtTitle);

        Bundle extraData = getIntent().getExtras();
        if(extraData != null){
            String strTitle = extraData.getString("Title");
            if(strTitle != null){
                tvTitle.setText(strTitle);
            }
        }

        // Register the receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(
                finishReceiver, new IntentFilter("com.conform.ACTION_FINISH")
        );
        LocalBroadcastManager.getInstance(this).registerReceiver(
                finishReceiver, new IntentFilter("com.conform.ACTION_UPDATE_PROGRESS")
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(finishReceiver);
    }


}