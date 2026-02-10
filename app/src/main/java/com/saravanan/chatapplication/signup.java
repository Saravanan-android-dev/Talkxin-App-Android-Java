package com.saravanan.chatapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class signup extends AppCompatActivity {

    EditText edtEmail, edtPassword,username;
    Button btnCreateAccount;
    TextView txtSignIn;

    FirebaseAuth mAuth;
    FirebaseFirestore db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);



        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnCreateAccount = findViewById(R.id.btnSignup);
        username = findViewById(R.id.username);

        txtSignIn = findViewById(R.id.txtLogin);

        btnCreateAccount.setOnClickListener(v -> registerUser());

        txtSignIn.setOnClickListener(v -> {
            startActivity(new Intent(signup.this, Login.class));
            finish();
        });
    }

    private void registerUser() {

        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String name = username.getText().toString().trim();


        // 🔒 VALIDATION
        if (TextUtils.isEmpty(email)) {
            edtEmail.setError("Enter email");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            edtPassword.setError("Enter password");
            return;
        }
        if (TextUtils.isEmpty(name)) {
            username.setError("Enter username");
            return;
        }

        if (password.length() < 6) {
            edtPassword.setError("Password must be at least 6 characters");
            return;
        }

        // 🔥 CREATE USER
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user == null) return;

                        // 📧 SEND EMAIL VERIFICATION
                        user.sendEmailVerification()
                                .addOnSuccessListener(unused ->
                                        Toast.makeText(
                                                signup.this,
                                                "Verification email sent 📧",
                                                Toast.LENGTH_LONG
                                        ).show()
                                )
                                .addOnFailureListener(e ->
                                        Toast.makeText(
                                                signup.this,
                                                e.getMessage(),
                                                Toast.LENGTH_LONG
                                        ).show()
                                );

                        // 💾 SAVE USER IN FIRESTORE
                        Map<String, Object> map = new HashMap<>();
                        map.put("uid", user.getUid());
                        map.put("email", user.getEmail());
                        map.put("createdAt", System.currentTimeMillis());
                        map.put("username",name);

                        db.collection("Users")
                                .document(user.getUid())
                                .set(map);

                        // 🔥 IMPORTANT: SIGN OUT AFTER SIGNUP

                        // ➡️ GO TO LOGIN
                        startActivity(new Intent(signup.this, Login.class));
                        finish();


                    } else {
                        Toast.makeText(
                                signup.this,
                                task.getException() != null
                                        ? task.getException().getMessage()
                                        : "Signup failed",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Exit App")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", (d, w) -> super.onBackPressed())
                .setNegativeButton("No", null)
                .show();
    }
}
