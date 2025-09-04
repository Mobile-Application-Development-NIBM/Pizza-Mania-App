package com.example.pizzamaniaapp; // Package declaration

import android.view.LayoutInflater; // To inflate XML layouts into Views
import android.view.View; // Base class for UI components
import android.view.ViewGroup; // Container for holding multiple views
import android.widget.ImageButton; // Button with an image icon
import android.widget.ImageView; // UI component for displaying images
import android.widget.TextView; // UI component for displaying text

import androidx.annotation.NonNull; // Marks parameters/returns as non-null
import androidx.recyclerview.widget.RecyclerView; // RecyclerView for efficient lists

import com.bumptech.glide.Glide; // Library for loading images from URLs

import java.util.List; // Collection type for ordered lists

// 🔹 Adapter for displaying individual menu items (inside a branch row)
public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.MenuViewHolder> {

    private final List<AdminHomeActivity.MenuItem> menuList; // List of menu items
    private final OnEditClickListener editListener; // Callback for edit button
    private final OnDeleteClickListener deleteListener; // Callback for delete button

    // 🔹 Interface for edit action
    public interface OnEditClickListener {
        void onEditClick(AdminHomeActivity.MenuItem item); // Called when edit is clicked
    }

    // 🔹 Interface for delete action
    public interface OnDeleteClickListener {
        void onDeleteClick(AdminHomeActivity.MenuItem item); // Called when delete is clicked
    }

    // 🔹 Constructor accepts list and callbacks
    public MenuAdapter(List<AdminHomeActivity.MenuItem> menuList,
                       OnEditClickListener editListener,
                       OnDeleteClickListener deleteListener) {
        this.menuList = menuList; // Assign menu list
        this.editListener = editListener; // Assign edit callback
        this.deleteListener = deleteListener; // Assign delete callback
    }

    @NonNull
    @Override
    public MenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate layout for each menu item
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.menu_item_layout, parent, false);
        return new MenuViewHolder(view); // Return holder
    }

    @Override
    public void onBindViewHolder(@NonNull MenuViewHolder holder, int position) {
        // Get the current menu item
        AdminHomeActivity.MenuItem item = menuList.get(position);

        // Set name and price
        holder.name.setText(item.name);
        holder.price.setText("Rs. " + item.price);

        // Load image with Glide (shows placeholder if null)
        Glide.with(holder.itemView.getContext())
                .load(item.imageURL)
                .placeholder(R.drawable.sample_pizza)
                .into(holder.image);

        // Handle edit button click
        holder.editBtn.setOnClickListener(v -> {
            if (editListener != null) {
                editListener.onEditClick(item);
            }
        });

        // Handle delete button click
        holder.deleteBtn.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return menuList.size(); // Return total number of menu items
    }

    // 🔹 ViewHolder class holds UI components for each menu item
    static class MenuViewHolder extends RecyclerView.ViewHolder {
        ImageView image; // Menu image
        TextView name, price; // Menu name and price
        ImageButton editBtn, deleteBtn; // Edit & delete buttons

        public MenuViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.menuImage); // Menu image
            name = itemView.findViewById(R.id.menuName); // Menu name
            price = itemView.findViewById(R.id.menuPrice); // Menu price
            editBtn = itemView.findViewById(R.id.editMenuBtn); // Edit button
            deleteBtn = itemView.findViewById(R.id.deleteMenuBtn); // Delete button
        }
    }
}
