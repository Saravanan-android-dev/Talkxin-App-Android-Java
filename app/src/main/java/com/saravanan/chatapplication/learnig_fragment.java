package com.saravanan.chatapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.saravanan.chatapplication.databinding.ActivityLearnigFragmentBinding;
import com.saravanan.chatapplication.databinding.ActivityMainBinding;

public class learnig_fragment extends AppCompatActivity {

    ActivityLearnigFragmentBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLearnigFragmentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Default fragment
        switchFragment(new Home_fragment());

        binding.bottomnav.setOnNavigationItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.home) {
                switchFragment(new Home_fragment());

            } else if (id == R.id.profile) {
                switchFragment(new profile_fragment());

            } else if (id == R.id.settings) {
                switchFragment(new setting_fragment());
            }

            return true;
        });

    }

    private void switchFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.frames, fragment);
        transaction.commit();
    }
    @Override
    public void onBackPressed() {
        startActivity(new Intent(learnig_fragment.this,Homepage.class));
    }
}
