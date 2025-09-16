// Defines the package name for this class
package com.example.pizzamaniaapp;

// Imports for Android UI components and RecyclerView classes
import android.view.LayoutInflater; // Used to inflate XML layouts into View objects
import android.view.View; // Base class for all UI components
import android.view.ViewGroup; // Container for views
import android.widget.ImageView; // UI component to display images
import android.widget.TextView; // UI component to display text

import androidx.annotation.NonNull; // Annotation indicating a parameter or return value cannot be null
import androidx.recyclerview.widget.RecyclerView; // A flexible view for providing a limited window into a large dataset

import com.bumptech.glide.Glide; // Third-party library for efficient image loading from URLs
import com.google.android.material.button.MaterialButton; // A styled button with Material Design principles

import java.util.List; // Java interface for ordered collections (used for the list of menu items)

// The adapter class for displaying a list of menu items within a single category in a RecyclerView
public class MenuUnderCategoryAdapter extends RecyclerView.Adapter<MenuUnderCategoryAdapter.MenuViewHolder> {

    // A list to hold the menu items to be displayed
    private final List<CustomerHomeActivity.MenuItem> menuList;
    // A listener to handle clicks on a menu item
    private final OnMenuClickListener listener;

    // An interface to define the callback method for when a menu item is clicked
    public interface OnMenuClickListener {
        void onMenuClick(CustomerHomeActivity.MenuItem item);
    }

    // Constructor to initialize the adapter with the list of menus and the click listener
    public MenuUnderCategoryAdapter(List<CustomerHomeActivity.MenuItem> menuList,
                                    OnMenuClickListener listener) {
        this.menuList = menuList; // Assign the provided list of menu items
        this.listener = listener; // Assign the provided click listener
    }

    // Called when RecyclerView needs a new ViewHolder to represent an item
    @NonNull
    @Override
    public MenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.customer_menu_grid_item_layout, parent, false); // CHANGED LINE
        return new MenuViewHolder(view);
    }

    // Called by RecyclerView to display the data at a specified position
    @Override
    public void onBindViewHolder(@NonNull MenuViewHolder holder, int position) {
        // Gets the MenuItem object from the list at the current position
        CustomerHomeActivity.MenuItem item = menuList.get(position);

        // Binds the menu item's name to the TextView in the ViewHolder
        holder.name.setText(item.name);
        // Binds the menu item's price to the TextView, formatted with "Rs. "
        holder.price.setText("Rs. " + item.price);

        // Uses Glide to load the image from the item's URL
        Glide.with(holder.itemView.getContext())
                .load(item.imageURL)
                // Sets a placeholder image while the actual image is loading
                .placeholder(R.drawable.sample_pizza)
                // Sets the loaded image into the ImageView in the ViewHolder
                .into(holder.image);

        // Sets a click listener on the "View" button to handle user interaction
        holder.viewButton.setOnClickListener(v -> {
            // Checks if a listener has been set
            if (listener != null) {
                // Calls the onMenuClick method on the listener, passing the clicked menu item
                listener.onMenuClick(item);
            }
        });
    }

    // Returns the total number of items in the data set held by the adapter
    @Override
    public int getItemCount() {
        // Returns the size of the menu list, or 0 if the list is null
        return menuList != null ? menuList.size() : 0;
    }

    // A static inner class that describes an item view and metadata about its place within the RecyclerView
    static class MenuViewHolder extends RecyclerView.ViewHolder {
        // Declares UI components of the menu item layout
        ImageView image; // The image of the menu item
        TextView name, price; // The name and price text
        MaterialButton viewButton; // The button to view the menu item details

        // Constructor to find and hold references to the UI components
        public MenuViewHolder(@NonNull View itemView) {
            super(itemView); // Calls the superclass constructor
            // Finds the ImageView by its ID from the layout
            image = itemView.findViewById(R.id.menuImage);
            // Finds the TextViews for name and price
            name = itemView.findViewById(R.id.menuName);
            price = itemView.findViewById(R.id.menuPrice);
            // Finds the MaterialButton for viewing the item
            viewButton = itemView.findViewById(R.id.viewMenuButton);
        }
    }
}