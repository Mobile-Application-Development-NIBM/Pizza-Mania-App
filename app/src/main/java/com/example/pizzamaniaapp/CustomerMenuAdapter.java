package com.example.pizzamaniaapp;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import java.util.List;

// Adapter for displaying menu items in a horizontal RecyclerView
public class CustomerMenuAdapter extends RecyclerView.Adapter<CustomerMenuAdapter.CustomerMenuViewHolder> {

    private static final String TAG = "CustomerMenuAdapter";

    private final List<CustomerHomeActivity.MenuItem> menuList; // List of menus to display
    private final OnViewClickListener viewListener; // Callback for menu item click events

    // Interface to handle menu item click
    public interface OnViewClickListener {
        void onViewClick(CustomerHomeActivity.MenuItem item);
    }

    // Adapter constructor
    public CustomerMenuAdapter(List<CustomerHomeActivity.MenuItem> menuList,
                               OnViewClickListener viewListener) {
        this.menuList = menuList;
        this.viewListener = viewListener;
        Log.d(TAG, "Adapter initialized with menu list size: " + (menuList != null ? menuList.size() : "0"));
    }

    // Inflate the menu item layout
    @NonNull
    @Override
    public CustomerMenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder called for viewType: " + viewType);

        // Inflate the XML layout for a single menu item
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.customer_menu_item_layout, parent, false);
        return new CustomerMenuViewHolder(view);
    }

    // Bind menu item data to the ViewHolder
    @Override
    public void onBindViewHolder(@NonNull CustomerMenuViewHolder holder, int position) {
        CustomerHomeActivity.MenuItem item = menuList.get(position);
        Log.d(TAG, "Binding menu item: " + item.name + " at position: " + position);

        // Set menu name
        holder.name.setText(item.name);

        // Set menu price with currency
        holder.price.setText("Rs. " + item.price);

        // Load menu image using Glide with a placeholder
        Glide.with(holder.itemView.getContext())
                .load(item.imageURL)
                .placeholder(R.drawable.sample_pizza) // default image while loading
                .into(holder.image);

        // Set click listener for the "View" button to trigger activity callback
        holder.viewButton.setOnClickListener(v -> viewListener.onViewClick(item));
    }

    // Return number of menu items
    @Override
    public int getItemCount() {
        return menuList != null ? menuList.size() : 0;
    }

    // ViewHolder for a single menu item
    static class CustomerMenuViewHolder extends RecyclerView.ViewHolder {
        ImageView image;            // Menu image
        TextView name, price;       // Menu name and price
        MaterialButton viewButton;  // Button to view more details / popup

        public CustomerMenuViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.menuImage);
            name = itemView.findViewById(R.id.menuName);
            price = itemView.findViewById(R.id.menuPrice);
            viewButton = itemView.findViewById(R.id.viewMenuButton);
        }
    }
}
