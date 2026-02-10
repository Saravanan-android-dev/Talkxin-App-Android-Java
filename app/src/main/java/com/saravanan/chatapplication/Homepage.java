package com.saravanan.chatapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class Homepage extends AppCompatActivity {

    ImageButton home, search, message, profile, uploadimage;
    ImageView profileImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);

        home = findViewById(R.id.home);
        search = findViewById(R.id.search);
        message = findViewById(R.id.message);
        profile = findViewById(R.id.profile);
        uploadimage = findViewById(R.id.imageupload);
        profileImage = findViewById(R.id.profileImage);

        // DELETE ACCOUNT BUTTON

        home.setOnClickListener(v ->
                startActivity(new Intent(Homepage.this, Learnigdrawer.class)));

        message.setOnClickListener(v ->
                startActivity(new Intent(Homepage.this, people_section.class)));

        profile.setOnClickListener(v -> {
            startActivity(new Intent(Homepage.this, profilepage.class));
            finish();
        });
        search.setOnClickListener(v -> {
            startActivity(new Intent(Homepage.this, learnig_fragment.class));
            finish();
        });


    }

    // 🔙 Back press exit dialog
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Exit")
                .setMessage("Are you sure want to exit?")
                .setPositiveButton("Yes", (dialog, which) -> finishAffinity())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }


}
