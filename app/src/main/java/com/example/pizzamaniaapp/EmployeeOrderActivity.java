package com.example.pizzamaniaapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class EmployeeOrderActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EmployeeOrderHistoryAdapter adapter;
    private List<Order> pendingOrders = new ArrayList<>();
    private TextView emptyText;
    private DatabaseReference dbRef;
    private String currentEmployeeBranchID;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_order);

        recyclerView = findViewById(R.id.recyclerEmployeeOrders);
        emptyText = findViewById(R.id.tvNoOrders);
        ImageButton backBtn = findViewById(R.id.BackBtn);

        backBtn.setOnClickListener(v -> finish());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EmployeeOrderHistoryAdapter(this, pendingOrders);
        recyclerView.setAdapter(adapter);

        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        currentEmployeeBranchID = prefs.getString("branchID", "b001");

        dbRef = FirebaseDatabase.getInstance().getReference("orders");
        loadPendingOrders();

    }

    private void loadPendingOrders(){

        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                pendingOrders.clear();

                for (DataSnapshot orderSnap : snapshot.getChildren()) {
                    Order order = orderSnap.getValue(Order.class);
                    if (order == null) continue;

                    if ("Delivery Pending".equalsIgnoreCase(order.getStatus())
                            && currentEmployeeBranchID.equals(order.getBranchID())) {
                        pendingOrders.add(order);
                    }
                }

                if (pendingOrders.isEmpty()) {
                    emptyText.setText("No pending orders yet");
                    emptyText.setVisibility(TextView.VISIBLE);
                    recyclerView.setVisibility(RecyclerView.GONE);
                } else {
                    emptyText.setVisibility(TextView.GONE);
                    recyclerView.setVisibility(RecyclerView.VISIBLE);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                emptyText.setText("Failed to load orders");
                emptyText.setVisibility(TextView.VISIBLE);
                recyclerView.setVisibility(RecyclerView.GONE);
            }
        });
    }
}