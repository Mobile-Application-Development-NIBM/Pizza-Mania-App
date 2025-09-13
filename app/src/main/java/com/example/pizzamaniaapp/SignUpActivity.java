package com.example.pizzamaniaapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.pizzamaniaapp.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

public class SignUpActivity extends AppCompatActivity {

    private EditText inputName, inputEmail, inputPhone, inputAddress, inputPassword;
    private Button signupBtn;

    private FirebaseAuth mAuth;

    //Firebase class that points to a location
    private DatabaseReference usersRef,counterRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        //Real Time Firebase Database refernces
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        counterRef = FirebaseDatabase.getInstance().getReference("userCounter");

        //Finds the view
        inputName = findViewById(R.id.inputName);
        inputEmail = findViewById(R.id.inputEmail);
        inputPhone = findViewById(R.id.inputPhone);
        inputPassword = findViewById(R.id.inputPassword);
        inputAddress = findViewById(R.id.inputAddress);
        signupBtn = findViewById(R.id.signupBtn);


        //On Click for button
        signupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createAccount();
            }
        });
    }

    private void createAccount() {
        String name = inputName.getText().toString().trim();
        String email = inputEmail.getText().toString().trim();
        String phone = inputPhone.getText().toString().trim();
        String address = inputAddress.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();

        //Validation
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) ||
                TextUtils.isEmpty(phone) || TextUtils.isEmpty(address) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Step 1: Create user in Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {

                        // Step 2: Find the highest userID in "users"
                        usersRef.get().addOnCompleteListener(snapshotTask -> {
                            if (snapshotTask.isSuccessful() && snapshotTask.getResult() != null) {
                                long maxId = 0;
                                for (DataSnapshot child : snapshotTask.getResult().getChildren()) {
                                    String key = child.getKey(); // e.g., "u018"
                                    if (key != null && key.startsWith("u")) {
                                        try {
                                            long num = Long.parseLong(key.substring(1));
                                            if (num > maxId) {
                                                maxId = num;
                                            }
                                        } catch (NumberFormatException ignored) {}
                                    }
                                }

                                // Next available ID
                                long newIdNumber = maxId + 1;
                                String userID = String.format("u%03d", newIdNumber);

                                // Step 3: Create User object (role = Customer)
                                User user = new User(userID, name, email, phone, address, "Customer");

                                // Step 4: Save under users/{userID}
                                usersRef.child(userID).setValue(user)
                                        .addOnCompleteListener(task1 -> {
                                            if (task1.isSuccessful()) {
                                                Toast.makeText(SignUpActivity.this, "Sign-up successful!", Toast.LENGTH_SHORT).show();

                                                // Step 5: Go to LoginActivity
                                                startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                                                finish();
                                            } else {
                                                Toast.makeText(SignUpActivity.this, "Failed to save user info", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            } else {
                                Toast.makeText(SignUpActivity.this, "Failed to fetch users", Toast.LENGTH_SHORT).show();
                            }
                        });

                    } else {
                        // Firebase Auth error
                        Toast.makeText(SignUpActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}