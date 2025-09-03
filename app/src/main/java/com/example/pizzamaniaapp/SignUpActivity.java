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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.firestore.auth.User;

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

    private void createAccount(){
        String name = inputName.getText().toString().trim();
        String email = inputEmail.getText().toString().trim();
        String phone = inputPhone.getText().toString().trim();
        String address = inputAddress.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();

        //Validation
        if(TextUtils.isEmpty(name) || TextUtils.isEmpty(email) ||
                TextUtils.isEmpty(phone) || TextUtils.isEmpty(address) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;

        }

        //Step 1: Create the users in Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email,password)
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        //Step2: Get unique Firebase User ID
                        String uid = mAuth.getCurrentUser().getUid();


                        //Step 3: Generate customer UID like u001 using transactions
                        counterRef.runTransaction(new Transaction.Handler() {
                            @NonNull
                            @Override
                            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                                Long currentValue = currentData.getValue(Long.class);
                                if(currentValue == null) currentValue = 0L;
                                currentData.setValue(currentValue + 1);
                                return Transaction.success(currentData);

                            }

                            @Override
                            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot snapshot) {
                                if(committed && snapshot != null){
                                    long newIdNumber= snapshot.getValue(Long.class);
                                    String userID = String.format("u%03d", newIdNumber);

                                    //Step 4: Create user Obj without Password
                                    User user = new User(userID, name, email, phone, address, "Customer");

                                    //Step 5: Svae the user dta in the Real Time Database
                                    usersRef.child(uid).setValue(user)
                                            .addOnCompleteListener(task1 -> {
                                                if(task1.isSuccessful()){
                                                    Toast.makeText(SignUpActivity.this, "Sign-up successful!", Toast.LENGTH_SHORT).show();

                                                    //Opening the Loging Activity
                                                    startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                                                    finish();
                                                }
                                                else{
                                                    Toast.makeText(SignUpActivity.this, "Failed to save user info", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                }
                                else {
                                    Toast.makeText(SignUpActivity.this, "Failed to generate userID", Toast.LENGTH_SHORT).show();
                                }

                            }
                        });
                    }
                    else{
                        // Firebase Auth error
                        Toast.makeText(SignUpActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });


    }


    public class User {

        public String userID, name, email, phone, address, role;

        //Neeed for Firebase
        public User() {
        }

        public User(String userID, String name, String email, String phone, String address, String role) {
            this.userID = userID;
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.address = address;
            this.role = role;
        }
    }
}