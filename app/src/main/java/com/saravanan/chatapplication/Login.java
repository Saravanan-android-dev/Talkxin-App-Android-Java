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

public class Login extends AppCompatActivity {

    EditText edtEmail, edtPassword;
    Button btnLogin;
    TextView txtCreateAccount, forgotpassword;

    FirebaseAuth mAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();



        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        txtCreateAccount = findViewById(R.id.txtSignup);
        forgotpassword = findViewById(R.id.txtForgotPassword);

        btnLogin.setOnClickListener(v -> loginUser());

        txtCreateAccount.setOnClickListener(v ->
                startActivity(new Intent(Login.this, signup.class))
        );

        // ✅ FORGOT PASSWORD CLICK
        forgotpassword.setOnClickListener(v -> resetPassword());
    }


    private void loginUser() {

        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            edtEmail.setError("Enter email");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            edtPassword.setError("Enter password");
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user.isEmailVerified()) {

                            Toast.makeText(
                                    Login.this,
                                    "Login successful ✅",
                                    Toast.LENGTH_SHORT
                            ).show();

                            startActivity(
                                    new Intent(Login.this, Homepage.class)
                            );
                            finish();

                        } else {
                            Toast.makeText(
                                    Login.this,
                                    "Please verify your email first 📧",
                                    Toast.LENGTH_LONG
                            ).show();
                        }

                    } else {
                        Toast.makeText(
                                Login.this,
                                         "Email address or password does not match our record",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }


    // 🔐 PASSWORD RESET LOGIC (CORRECT PLACE)
    private void resetPassword() {

        String email = edtEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            edtEmail.setError("Enter registered email");
            return;
        }

        FirebaseAuth.getInstance()
                .sendPasswordResetEmail(email)
                .addOnSuccessListener(unused ->
                        Toast.makeText(
                                Login.this,
                                "Password reset email sent 📧",
                                Toast.LENGTH_LONG
                        ).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(
                                Login.this,
                                e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
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
