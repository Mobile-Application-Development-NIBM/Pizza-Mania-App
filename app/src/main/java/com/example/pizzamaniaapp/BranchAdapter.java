package com.example.pizzamaniaapp; // Package name of this class

import android.content.Context; // Provides access to application resources
import android.view.LayoutInflater; // Converts XML layouts into View objects
import android.view.View; // Basic UI component
import android.view.ViewGroup; // Parent container for Views
import android.widget.TextView; // UI element for displaying text

import androidx.annotation.NonNull; // Annotation meaning "cannot be null"
import androidx.recyclerview.widget.LinearLayoutManager; // LayoutManager for RecyclerView
import androidx.recyclerview.widget.RecyclerView; // Scrollable list view

import java.util.ArrayList; // Dynamic list implementation
import java.util.LinkedHashSet; // Set that preserves insertion order
import java.util.List; // Generic list interface
import java.util.Map; // Key-Value mapping
import java.util.Set; // Collection of unique elements

// 🔹 Adapter to display branches and their menu items
public class BranchAdapter extends RecyclerView.Adapter<BranchAdapter.BranchViewHolder> {

    private final Context context; // Reference to Activity/Fragment context
    private final List<AdminHomeActivity.MenuItem> menuList; // All menu items
    private final OnEditClickListener editListener; // Callback for edit button clicks
    private final List<String> uniqueBranchIDs; // Stores distinct branch IDs
    private final Map<String, String> branchIdToName; // Maps ID → Name for branches

    // 🔹 Listener interface for handling edit button clicks
    public interface OnEditClickListener {
        void onEditClick(AdminHomeActivity.MenuItem item); // Called when edit is tapped
    }

    // 🔹 Constructor
    public BranchAdapter(Context context,
                         List<AdminHomeActivity.MenuItem> menuList,
                         OnEditClickListener editListener,
                         Map<String, String> branchIdToName) {
        this.context = context; // Save context
        this.menuList = menuList; // Save menu list
        this.editListener = editListener; // Save listener
        this.branchIdToName = branchIdToName; // Save mapping

        // Extract unique branch IDs from all menu items
        Set<String> branchSet = new LinkedHashSet<>(); // Maintains order + uniqueness
        for (AdminHomeActivity.MenuItem item : menuList) { // Loop through menu items
            if (item.branches != null) { // If menu has assigned branches
                branchSet.addAll(item.branches); // Add branch IDs to the set
            }
        }
        this.uniqueBranchIDs = new ArrayList<>(branchSet); // Convert to list
    }

    @NonNull
    @Override
    public BranchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate branch row layout from XML
        View view = LayoutInflater.from(context).inflate(R.layout.branch_item_layout, parent, false);
        return new BranchViewHolder(view); // Wrap inside ViewHolder
    }

    @Override
    public void onBindViewHolder(@NonNull BranchViewHolder holder, int position) {
        // Get branch ID for current position
        String branchID = uniqueBranchIDs.get(position);

        // Convert ID → Name if available, else fallback to raw ID
        String branchName = branchIdToName.getOrDefault(branchID, branchID);
        holder.branchName.setText(branchName); // Show branch name on UI

        // Filter menu items that belong to this branch
        List<AdminHomeActivity.MenuItem> filteredMenus = new ArrayList<>();
        for (AdminHomeActivity.MenuItem item : menuList) {
            if (item.branches != null && item.branches.contains(branchID)) {
                filteredMenus.add(item); // Add menu to this branch’s list
            }
        }

        // Create MenuAdapter for horizontal scrolling list of menus
        MenuAdapter menuAdapter = new MenuAdapter(filteredMenus, item -> {
            if (editListener != null) editListener.onEditClick(item); // Handle edit
        }, item -> {
            // Handle delete → call AdminHomeActivity’s method
            if (context instanceof AdminHomeActivity) {
                ((AdminHomeActivity) context).showDeletePopup(item, null);
            }
        });

        // Set horizontal layout for menu list
        holder.menuRecyclerView.setLayoutManager(
                new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        holder.menuRecyclerView.setAdapter(menuAdapter); // Attach adapter
    }

    @Override
    public int getItemCount() {
        return uniqueBranchIDs.size(); // Number of unique branches
    }

    // 🔹 ViewHolder = wrapper around row views for branch
    static class BranchViewHolder extends RecyclerView.ViewHolder {
        TextView branchName; // Displays branch name
        RecyclerView menuRecyclerView; // Holds horizontal list of menus

        public BranchViewHolder(@NonNull View itemView) {
            super(itemView);
            branchName = itemView.findViewById(R.id.categoryName); // Get TextView from XML
            menuRecyclerView = itemView.findViewById(R.id.menuRecyclerView); // Get RecyclerView
        }
    }

    // 🔹 Refresh adapter with new menu list
    public void updateList(List<AdminHomeActivity.MenuItem> newList) {
        this.menuList.clear(); // Remove old menus
        this.menuList.addAll(newList); // Add updated list

        // Recalculate unique branch IDs from updated menu list
        uniqueBranchIDs.clear();
        Set<String> branchSet = new LinkedHashSet<>();
        for (AdminHomeActivity.MenuItem item : menuList) {
            if (item.branches != null) {
                branchSet.addAll(item.branches); // Add branch IDs
            }
        }
        uniqueBranchIDs.addAll(branchSet);

        notifyDataSetChanged(); // Refresh RecyclerView
    }
}
