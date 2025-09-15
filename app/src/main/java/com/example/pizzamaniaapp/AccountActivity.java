package com.example.pizzamaniaapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.app.Activity;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


import de.hdodenhof.circleimageview.CircleImageView;

public class AccountActivity extends AppCompatActivity {


    private static final int REQUEST_CAMERA = 100;
    private static final int REQUEST_GALLERY = 200;
    private static final int REQUEST_PERMISSIONS = 300;

    private EditText tvName, tvEmail, tvPhone, tvAddress;
    private TextView tvPassword;   // password field is now just a TextView (not editable)
    private Button btnUpdateDetails, btnLogout, btnChangePicture, btnChangePassword;
    private CircleImageView profileImage;

    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;

    private boolean isEditing = false;

    // Database for storing profile image path
    private ProfileDBHelper dbHelper;

    // to store temp camera image uri
    private Uri cameraImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_account);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Initialize SQLite helper
        dbHelper = new ProfileDBHelper(this);


        // Bind UI elements
        profileImage = findViewById(R.id.profileImage);
        btnChangePicture = findViewById(R.id.btnChangePicture);
        btnUpdateDetails = findViewById(R.id.btnUpdate);
        btnLogout = findViewById(R.id.btnLogout);
        btnChangePassword = findViewById(R.id.btnChangePassword);  // new button in layout

        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvAddress = findViewById(R.id.tvAddress);
        tvPassword = findViewById(R.id.tvPassword);

        // Disable editing by default
        setFieldsEditable(false);


        // Load User Information
        loadData();

        // Load profile image from SQLite
        loadProfileImageFromDB();


        // Update button click
        btnUpdateDetails.setOnClickListener(v -> {
            if (!isEditing) {
                // Enable editing
                isEditing = true;
                btnUpdateDetails.setText("Save Changes");
                setFieldsEditable(true);
            } else {
                // Save changes to Firebase
                saveChanges();
            }
        });

        // Change password button
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        //Change profile picture button
        btnChangePicture.setOnClickListener(v-> showImagePickerDialog());

        //User Log Out
        btnLogout.setOnClickListener(v -> logoutUser());
    }

    private void setFieldsEditable(boolean editable) {
        tvName.setEnabled(editable);
        tvPhone.setEnabled(editable);
        tvAddress.setEnabled(editable);
        // Email cannot be directly edited
        tvEmail.setEnabled(false);
    }

    private void loadData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "No user logged in!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String email = currentUser.getEmail();

        usersRef.orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists()){
                            for (DataSnapshot userSnap : snapshot.getChildren()) {
                                String name = userSnap.child("name").getValue(String.class);
                                String phone = userSnap.child("phone").getValue(String.class);
                                String address = userSnap.child("address").getValue(String.class);

                                tvName.setText(name);
                                tvEmail.setText(email);
                                tvPhone.setText(phone);
                                tvAddress.setText(address);

                                // Always hide password
                                tvPassword.setText("********");
                            }
                        } else {
                            Toast.makeText(AccountActivity.this, "User data not found!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(AccountActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveChanges(){
        String name = tvName.getText().toString().trim();
        String phone = tvPhone.getText().toString().trim();
        String address = tvAddress.getText().toString().trim();
        String email = tvEmail.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone) || TextUtils.isEmpty(address)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        usersRef.orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists()){
                            for (DataSnapshot userSnap : snapshot.getChildren()) {
                                String key = userSnap.getKey();

                                usersRef.child(key).child("name").setValue(name);
                                usersRef.child(key).child("phone").setValue(phone);
                                usersRef.child(key).child("address").setValue(address);

                                Toast.makeText(AccountActivity.this, "Details updated successfully!", Toast.LENGTH_SHORT).show();

                                isEditing = false;
                                btnUpdateDetails.setText("Update Details");
                                setFieldsEditable(false);
                                tvPassword.setText("********");
                            }
                        } else {
                            Toast.makeText(AccountActivity.this, "User data not found!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(AccountActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ==================== PROFILE IMAGE HANDLING ====================
    private void showImagePickerDialog(){
        String[] options = {"Take Photo", "Choose from Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Profile Picture");
        builder.setItems(options, (dialog, which) -> {
            if(which == 0){
                // Camera
                if (checkAndRequestPermissions()) openCamera();
            }
            else {
                // Gallery
                if (checkAndRequestPermissions()) openGallery();
            }
        });
        builder.show();
    }

    // Check camera and storage permissions
    private boolean checkAndRequestPermissions(){
        String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        boolean allGranted = true;

        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) allGranted = false;
        }
        if (!allGranted){
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS);
            return false;
        }
        return true;
    }

    private void openCamera(){
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File imageFile = createImageFile();
        if (imageFile != null) {
            cameraImageUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", imageFile);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            startActivityForResult(cameraIntent, REQUEST_CAMERA);
        }
    }

    private void openGallery(){
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent,REQUEST_GALLERY);
    }

    private File createImageFile(){
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filename = "IMG_" + timeStamp + ".jpg";
        File storageDir = getFilesDir(); // Internal storage
        return new File(storageDir, filename);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            Bitmap bitmap = null;
            if (requestCode == REQUEST_CAMERA) {
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), cameraImageUri);
                    saveImageAndPath(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (requestCode == REQUEST_GALLERY && data != null && data.getData() != null) {
                Uri imageUri = data.getData();
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                    saveImageAndPath(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void saveImageAndPath(Bitmap bitmap) {
        try {
            // Save to internal storage
            File imageFile = createImageFile();
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();

            // Save path to SQLite
            dbHelper.saveImagePath(imageFile.getAbsolutePath());

            // Display image
            profileImage.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadProfileImageFromDB() {
        String path = dbHelper.getImagePath();
        if (path != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(path);
            if (bitmap != null) profileImage.setImageBitmap(bitmap);
        }
    }

    //========================== PASSWORD UPDATING ==========================
    // Dialog to change password
    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Password");

        // Create a vertical LinearLayout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 10);

        // Current password field
        com.google.android.material.textfield.TextInputLayout currentPassLayout =
                new com.google.android.material.textfield.TextInputLayout(this);
        currentPassLayout.setHint("Current Password");
        com.google.android.material.textfield.TextInputEditText currentPass =
                new com.google.android.material.textfield.TextInputEditText(this);
        currentPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        currentPassLayout.addView(currentPass);

        // New password field
        com.google.android.material.textfield.TextInputLayout newPassLayout =
                new com.google.android.material.textfield.TextInputLayout(this);
        newPassLayout.setHint("New Password");
        com.google.android.material.textfield.TextInputEditText newPass =
                new com.google.android.material.textfield.TextInputEditText(this);
        newPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        newPassLayout.addView(newPass);

        // Add both to the layout
        layout.addView(currentPassLayout);
        layout.addView(newPassLayout);

        builder.setView(layout);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String currentPassword = currentPass.getText().toString().trim();
            String newPasswordText = newPass.getText().toString().trim();

            if (TextUtils.isEmpty(currentPassword) || TextUtils.isEmpty(newPasswordText)) {
                Toast.makeText(AccountActivity.this, "Both fields are required", Toast.LENGTH_SHORT).show();
                return;
            }
            reAuthenticateAndChangePassword(currentPassword, newPasswordText);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

    }


    private void reAuthenticateAndChangePassword(String currentPassword, String newPassword) {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null && user.getEmail() != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);

            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    user.updatePassword(newPassword).addOnCompleteListener(updateTask -> {
                        if (updateTask.isSuccessful()) {
                            Toast.makeText(AccountActivity.this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(AccountActivity.this, "Failed to update password", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(AccountActivity.this, "Re-authentication failed. Check your current password.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // ==================== USER LOG OUT ====================
    private void logoutUser() {
        // 1. Sign out from Firebase Authentication
        FirebaseAuth.getInstance().signOut();

        // 2. Clear session data in SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear(); // clears all saved keys (isLoggedIn, role, email, etc.)
        editor.apply();

        // 3. Redirect user back to LoginActivity
        Intent intent = new Intent(AccountActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        // clears back stack so user can’t press "Back" to return
        startActivity(intent);
        finish();
    }

}
