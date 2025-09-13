package com.example.pizzamaniaapp;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Adapter for displaying categories and their corresponding menu items
public class CustomerCategoryAdapter extends RecyclerView.Adapter<CustomerCategoryAdapter.CategoryViewHolder> {

    private final Context context; // Activity context
    private final List<CustomerHomeActivity.MenuItem> allMenus; // All menus for current branch
    private final OnViewClickListener viewListener; // Callback for when a menu item is clicked
    private String currentBranchID; // Currently selected branch ID
    private List<String> categoryList; // List of category names
    private Map<String, List<CustomerHomeActivity.MenuItem>> menusByCategory; // Map category -> menus

    // Interface to handle menu item clicks
    public interface OnViewClickListener {
        void onViewClick(CustomerHomeActivity.MenuItem item);
    }

    // Adapter constructor
    public CustomerCategoryAdapter(Context context, List<CustomerHomeActivity.MenuItem> menuList,
                                   OnViewClickListener viewListener, String branchID) {
        Log.d("CustomerCategoryAdapter", "Adapter constructor called");

        this.context = context;

        // Copy menus to prevent external modification
        this.allMenus = menuList != null ? new ArrayList<>(menuList) : new ArrayList<>();
        this.viewListener = viewListener;
        this.currentBranchID = branchID;

        // Build categories and menus map
        buildCategoryMap();
    }

    // Call this to refresh the adapter with new menus or when branch changes
    public void updateList(List<CustomerHomeActivity.MenuItem> newList, String branchID) {
        Log.d("CustomerCategoryAdapter", "updateList called, newList size: "
                + (newList != null ? newList.size() : "null") + ", branchID: '" + branchID + "'");

        if (newList == null) return;

        this.currentBranchID = branchID; // Update branch
        this.allMenus.clear();           // Clear old menus
        this.allMenus.addAll(newList);   // Add new menus
        buildCategoryMap();              // Rebuild category map
        notifyDataSetChanged();          // Refresh RecyclerView
    }

    // Build a map of categories -> menus filtered by the current branch
    private void buildCategoryMap() {
        categoryList = new ArrayList<>();
        menusByCategory = new HashMap<>();

        for (CustomerHomeActivity.MenuItem item : allMenus) {
            boolean branchMatch = false;

            // Check if the menu item belongs to the current branch
            if (item.branches != null) {
                for (String branch : item.branches) {
                    if (branch.trim().equalsIgnoreCase(currentBranchID.trim())) {
                        branchMatch = true;
                        break;
                    }
                }
            }
            if (!branchMatch) continue; // Skip menus not in this branch

            // Use "Other" if category is null
            String category = item.category != null ? item.category : "Other";

            // If category not in map, create list
            if (!menusByCategory.containsKey(category)) {
                menusByCategory.put(category, new ArrayList<>());
                categoryList.add(category); // Keep track of category order
            }

            // Add menu item to category
            menusByCategory.get(category).add(item);
        }

        Log.d("CustomerCategoryAdapter", "Categories built: " + categoryList.size());
    }

    // Inflate the category item layout
    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.category_item_layout, parent, false);
        return new CategoryViewHolder(view);
    }

    // Bind category data and nested horizontal RecyclerView for menus
    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        String category = categoryList.get(position);        // Current category
        holder.categoryName.setText(category);               // Set category name

        // Get menus for this category
        List<CustomerHomeActivity.MenuItem> menuList = menusByCategory.get(category);

        // Adapter for horizontal menu RecyclerView
        CustomerMenuAdapter menuAdapter = new CustomerMenuAdapter(menuList, item -> {
            if (viewListener != null) viewListener.onViewClick(item); // Trigger click listener
            Log.d("CustomerCategoryAdapter", "Menu item clicked: " + item.name);
        });

        // Set horizontal layout manager
        holder.menuRecyclerView.setLayoutManager(
                new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        );

        // Attach menu adapter
        holder.menuRecyclerView.setAdapter(menuAdapter);

        // ---- "View All" button ----
        holder.itemView.findViewById(R.id.viewAllButton).setOnClickListener(v -> {
            if (menuList != null && !menuList.isEmpty()) {
                // Start MenuUnderCategoryActivity with category menus
                Intent intent = new Intent(context, MenuUnderCategoryActivity.class);
                intent.putExtra("branchID", currentBranchID);           // Pass branch ID
                intent.putExtra("category_name", category);             // Pass category name
                intent.putParcelableArrayListExtra(                      // Pass menu list
                        "menu_list",
                        new ArrayList<>(menuList)
                );
                context.startActivity(intent);
            } else {
                Log.d("CustomerCategoryAdapter", "View All clicked but menuList is empty for category: " + category);
            }
        });
    }

    @Override
    public int getItemCount() {
        // Return number of categories
        return categoryList != null ? categoryList.size() : 0;
    }

    // ViewHolder for category items
    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView categoryName;         // Category title
        RecyclerView menuRecyclerView; // Horizontal RecyclerView of menus

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryName = itemView.findViewById(R.id.categoryName);
            menuRecyclerView = itemView.findViewById(R.id.menuRecyclerView);
        }
    }
}