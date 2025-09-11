package com.example.pizzamaniaapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivity extends AppCompatActivity {

    private EditText inputEmail, inputPassword;
    private Button loginBtn;
    private TextView signUpText;

    private FirebaseAuth mAuth;
    private DatabaseReference employeesRef;
    private AdminDBHelper adminDBHelper;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        employeesRef = FirebaseDatabase.getInstance().getReference("employees");

        // Initialize SQLite
        adminDBHelper = new AdminDBHelper(this);

        // SharedPreferences for session persistence
        sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);

        // Check if already logged in
        if (sharedPreferences.getBoolean("isLoggedIn", false)) {
            redirectToHome(sharedPreferences.getString("role", ""));
        }

        //Find By View
        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        loginBtn = findViewById(R.id.loginBtn);
        signUpText = findViewById(R.id.signUpText);

        //Clicable Blue TExt
        signUpText.setMovementMethod(LinkMovementMethod.getInstance());
        signUpText.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, SignUpActivity.class)));

        //Loging Button
        loginBtn.setOnClickListener( v -> loginUser());
    }

    private void loginUser(){
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();

        //Validation
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        //Check Admin in SQLite
        if(adminDBHelper.checkAdmin(email,password)){
            saveSession("Admin", email, null);
            redirectToHome("Admin");
            return;
        }

        //CHeck Employee in Firebase
        employeesRef.orderByChild("email").equalTo(email).get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful() && task.getResult().exists()){
                        for(DataSnapshot snapshot : task.getResult().getChildren()) {
                            String empEmail = snapshot.child("email").getValue(String.class);
                            String empPassword = snapshot.child("password").getValue(String.class);

                            if(empEmail == null || empPassword == null) {
                                Toast.makeText(LoginActivity.this, "Employee data missing", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            if(email.equals(empEmail) && password.equals(empPassword)) {
                                saveSession("Employee", email, password);
                                redirectToHome("Employee");
                                return;
                            }
                        }
                        Toast.makeText(LoginActivity.this, "Invalid password for employee", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        //Cheching for customers
                        loginDeliveryman(email, password);
                    }
                })
                .addOnFailureListener(e->
                        Toast.makeText(LoginActivity.this, "Employee check failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );

    }

    private void loginDeliveryman(String email, String password) {
        DatabaseReference deliveryRef = FirebaseDatabase.getInstance().getReference("deliverymen");
        deliveryRef.orderByChild("email").equalTo(email).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        for (DataSnapshot snapshot : task.getResult().getChildren()) {
                            String delEmail = snapshot.child("email").getValue(String.class);
                            String delPassword = snapshot.child("password").getValue(String.class);
                            String delID = snapshot.child("delID").getValue(String.class);

                            if (delEmail == null || delPassword == null) {
                                Toast.makeText(LoginActivity.this, "Deliveryman data missing", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            if (email.equals(delEmail) && password.equals(delPassword)) {
                                saveSession("Deliveryman", email, delID);
                                redirectToHome("Deliveryman");
                                return;
                            }
                        }
                        Toast.makeText(LoginActivity.this, "Invalid password for deliveryman", Toast.LENGTH_SHORT).show();
                    } else {
                        // If not Deliveryman → check Customer
                        loginCustomer(email, password);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(LoginActivity.this, "Deliveryman check failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
    private void loginCustomer (String email, String password){
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(authTask -> {
                    if(authTask.isSuccessful()){
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();

                        if(firebaseUser == null){
                            Toast.makeText(LoginActivity.this, "Unexpected error: Firebase user is null", Toast.LENGTH_LONG).show();
                            return;
                        }

                        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
                        usersRef.orderByChild("email").equalTo(email).get()
                                .addOnCompleteListener(userTask -> {
                                    if(userTask.isSuccessful() && userTask.getResult().exists()){
                                        for (DataSnapshot userSnapshot : userTask.getResult().getChildren()){
                                            String userID = userSnapshot.child("userID").getValue(String.class);

                                            if(userID == null){
                                                Toast.makeText(LoginActivity.this, "User ID missing in database", Toast.LENGTH_SHORT).show();
                                                return;
                                            }

                                            saveSession("Customer", email, userID);
                                            redirectToHome("Customer");
                                            return;
                                        }
                                    }
                                    else {
                                        Toast.makeText(LoginActivity.this, "User record not found in database", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(e->
                                        Toast.makeText(LoginActivity.this, "Database error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                                );
                    }
                    else {
                        String errorMsg = authTask.getException() != null ? authTask.getException().getMessage() : "Unknown error";
                        Toast.makeText(LoginActivity.this, "Login failed: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private  void saveSession(String role, String email, String userID){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("role", role);
        editor.putString("email", email);
        if (userID != null) editor.putString("userID", userID);
        editor.apply();
    }

    private void redirectToHome(String role){
        Intent intent;
        switch (role) {
            case "Admin":
                intent = new Intent(this, AdminHomeActivity.class);
                break;
            case "Employee":
                intent = new Intent(this, EmployeeHomeActivity.class);
                break;
            case "Deliveryman":
                intent = new Intent(this, DeliverymanHomeActivity.class);
                break;
            case "Customer":
            default:
                intent = new Intent(this, CustomerHomeActivity.class);
        }
        startActivity(intent);
        finish();
    }
}