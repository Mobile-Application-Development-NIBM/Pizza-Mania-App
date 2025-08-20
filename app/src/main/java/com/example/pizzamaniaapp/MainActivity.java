package com.example.pizzamaniaapp;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

//Firebase
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        HashMap<String, Object> testData = new HashMap<>();
        testData.put("message", "Firebase is working!");
        testData.put("timestamp", System.currentTimeMillis());

        db.collection("testCollection")
                .add(testData)
                .addOnSuccessListener(documentReference -> {
                    System.out.println("✅ Data added successfully!");
                })
                .addOnFailureListener(e -> {
                    System.out.println("❌ Error: " + e.getMessage());
                });
    }
}