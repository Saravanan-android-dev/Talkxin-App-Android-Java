package com.saravanan.chatapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class AudiocallActivity extends AppCompatActivity {


    TextView txtUserName;
    ImageView imgUser;

    ArrayList<String> imageList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.audiocall);


        Button btnEnd;


         txtUserName = findViewById(R.id.txtUserName);

        imgUser=findViewById(R.id.imgUser);
        btnEnd = findViewById(R.id.btnEnd);



        String receiverUsername = getIntent().getStringExtra("receiverUsername");
        String calltype = getIntent().getStringExtra("CALL_TYPE");

        if (receiverUsername == null || receiverUsername.isEmpty()) {
            receiverUsername = "Unknown User";
        }

        txtUserName.setText(receiverUsername);


        btnEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(AudiocallActivity.this,Chat_activity.class));
            finish();

            }
        });



    }


}