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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.firestore.auth.User;

public class SignUpActivity extends AppCompatActivity {

    private EditText inputName, inputEmail, inputPhone,inputAddress, inputPassword;
    private Button signupBtn;

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

        //Initialize Firebase
        usersRef = FirebaseDatabase.getInstance().getReference("users");

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

        // Run a transaction on userCounter
        counterRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Long currentvalue = currentData.getValue(Long.class);
                if(currentvalue == null){
                    currentvalue = 0L;
                }
                currentData.setValue(currentvalue+1);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot snapshot) {

                if(committed && snapshot != null ){
                    long newIdNumber = snapshot.getValue(Long.class);
                    String userId = String.format("u%03d", newIdNumber); // e.g. u001, u002

                    //create user obj
                    User user = new User(userId, name, email, phone, address, password);

                    // Save user under that ID
                    usersRef.child(userId).setValue(user).addOnCompleteListener( task -> {
                        if(task.isSuccessful()){
                            Toast.makeText(SignUpActivity.this, "Sign-up successful!", Toast.LENGTH_SHORT).show();

                            //Open Login Page
                            Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                            startActivity(intent);
                            finish();
                        }
                        else{
                            Toast.makeText(SignUpActivity.this, "Failed to save user", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                else{
                    Toast.makeText(SignUpActivity.this, "Transaction failed", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }


    public class User {

        public String userID, name, email, phone, address, password;

        public User() {
            // Default constructor needed for Firebase
        }

        public User(String userID, String name, String email, String phone, String address, String password) {
            this.userID = userID;
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.address = address;
            this.password = password;
        }
    }
}