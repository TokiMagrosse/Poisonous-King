package com.example.poisonousking;

import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

public class RegisterActivity extends AppCompatActivity {

    public String Username, EmailAddress, ID;
    EditText username, email_address, register_password, confirm_password;
    Button register_button;
    FirebaseAuth m_auth;
    TextView back_to_login_text;
    ProgressBar progress_bar;
    FirebaseFirestore f_store;
    String userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        username = findViewById(R.id.username);
        email_address = findViewById(R.id.email_address);
        register_password = findViewById(R.id.register_password);
        confirm_password = findViewById(R.id.confirm_password);
        m_auth = FirebaseAuth.getInstance();
        f_store = FirebaseFirestore.getInstance();
        back_to_login_text = findViewById(R.id.back_to_login_text);
        progress_bar = findViewById(R.id.progress_bar);
        register_button = findViewById(R.id.register_button);

        register_button.setOnClickListener(v -> checkCredentials());

        if (m_auth.getCurrentUser() != null) {
            Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
            startActivity(intent);
            // finish();
        }

        back_to_login_text.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
            startActivity(intent);
        });

    }

    private void checkCredentials() {
        String checkUsername = username.getText().toString().trim();
        String checkEmailAddress = email_address.getText().toString().trim();
        String checkPassword = register_password.getText().toString();
        String checkConfirmedPassword = confirm_password.getText().toString().trim();

        boolean isValid = true;

        if (checkUsername.isEmpty()) {
            showError(username, "Please enter your username");
            isValid = false;
        }
        else if (checkUsername.contains("-") ||
                checkUsername.contains("*") || checkUsername.contains("&") ||
                checkUsername.contains("#") || checkUsername.contains("%") ||
                checkUsername.contains("@") || checkUsername.contains("$") ||
                checkUsername.contains("|") || checkUsername.contains("/") ||
                checkUsername.contains("^") || checkUsername.contains("?") ||
                checkUsername.contains("(") || checkUsername.contains(")") ||
                checkUsername.contains("+") || checkUsername.contains("=") ||
                checkUsername.contains("<") || checkUsername.contains(">") ||
                checkUsername.contains(",") || checkUsername.contains("'")) {
            showError(username, "Your username can contain only specific character ( _ )");
            isValid = false;
        }
        else if (checkUsername.contains(" ")) {
            showError(username, "Your username should not contain spaces.");
            isValid = false;
        }
        else if (checkUsername.length() < 5) {
            showError(username, "Your username length must be at least 5 characters.");
            isValid = false;
        }
        else if (checkEmailAddress.isEmpty()) {
            showError(email_address, "Please enter your email.");
            isValid = false;
        }
        else if (!checkEmailAddress.contains("@") && !checkEmailAddress.contains(".") || !checkEmailAddress.contains("@") || !checkEmailAddress.contains(".")) {
            showError(email_address, "Please enter a valid email address.");
            isValid = false;
        }
        else if (checkPassword.isEmpty()) {
            showError(register_password, "Please enter your password.");
            isValid = false;
        }
        else if (checkPassword.length() < 8) {
            showError(register_password, "Your password length must be at least 8 characters.");
            isValid = false;
        }
        else if (checkPassword.contains(" ")) {
            showError(register_password, "Your password should not contain spaces.");
            isValid = false;
        }
        else if (checkPassword.length() > 64) {
            showError(register_password, "Your password can have at most 64 characters.");
            isValid = false;
        }
        else if (checkConfirmedPassword.isEmpty()) {
            showError(confirm_password, "Please confirm your password.");
            isValid = false;
        }
        else if (!checkConfirmedPassword.equals(checkPassword)) {
            showError(confirm_password, "Your password doesn't match the previous one.");
            isValid = false;
        }

        Username = checkUsername;
        EmailAddress = checkEmailAddress;
        ID = generatingUserID();

        progress_bar.setVisibility(View.VISIBLE);

        if (isValid) {
            m_auth.createUserWithEmailAndPassword(checkEmailAddress, checkPassword)
                    .addOnCompleteListener(this, task -> {
                        progress_bar.setVisibility(View.GONE);

                        if (task.isSuccessful()) {
                            FirebaseUser f_user = m_auth.getCurrentUser();
                            if (f_user != null) {
                                f_user.sendEmailVerification()
                                        .addOnSuccessListener(unused -> {
                                            Toast.makeText(RegisterActivity.this, "Email verification link sent to your email.", Toast.LENGTH_SHORT).show();
                                            // Proceed with user registration
                                            completeRegistration(checkUsername, checkEmailAddress, generatingUserID());
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.d(TAG, "Email not sent" + e.getMessage());
                                            Toast.makeText(RegisterActivity.this, "Failed to send email verification link.", Toast.LENGTH_SHORT).show();
                                        });
                            } else {
                                Toast.makeText(RegisterActivity.this, "Failed to get current user.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(RegisterActivity.this, "This email is already in use. Please enter another one.", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            progress_bar.setVisibility(View.GONE);
        }
    }

    private void completeRegistration(String username, String emailAddress, String ID) {
        Toast.makeText(RegisterActivity.this, "You have successfully registered", Toast.LENGTH_SHORT).show();

        userID = Objects.requireNonNull(m_auth.getCurrentUser()).getUid();
        DocumentReference documentReference = f_store.collection("all my users").document(userID);
        Map<String, Object> user = new HashMap<>();
        user.put("Username", username);
        user.put("Email address", emailAddress);
        user.put("Personal ID", ID);

        documentReference.set(user)
                .addOnSuccessListener(unused -> Log.d(TAG, "User profile has been created for " + userID))
                .addOnFailureListener(e -> Log.d(TAG, e.toString()));

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }

    private void showError(EditText input, String errorText) {
        input.setError(errorText);
        input.requestFocus();
    }

    private String generatingUserID() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = sharedPreferences.getString("user_id", null);

        if (userId == null) {
            // If user ID is not generated yet, generate a new one
            String uuid = UUID.randomUUID().toString();

            // Hash the UUID to produce a 10-digit ID
            String hashedId = hashString(uuid);

            // Save the generated user ID in SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("user_id", hashedId);
            editor.apply();

            // Set the generated user ID to the TextView
            return hashedId;
        } else {
            // If user ID is already generated, set it to the TextView
            return userId;
        }
    }

    private String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());

            // Convert the byte array to a hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            // Extract the first 10 characters to ensure the ID is 10 digits long
            return hexString.toString().substring(0, 10);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
}
