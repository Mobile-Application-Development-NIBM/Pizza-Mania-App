package com.example.pizzamaniaapp; // Package name where this class belongs

import android.content.Context; // Provides access to app-specific resources and classes
import android.view.LayoutInflater; // Used to inflate XML layout files into View objects
import android.view.View; // Basic building block for UI components
import android.view.ViewGroup; // Container that holds views inside RecyclerView
import android.widget.TextView; // UI component for displaying text

import androidx.annotation.NonNull; // Annotation to indicate parameter/return value should not be null
import androidx.recyclerview.widget.LinearLayoutManager; // RecyclerView layout manager (linear orientation)
import androidx.recyclerview.widget.RecyclerView; // Used for displaying scrollable lists of items

import java.util.ArrayList; // List implementation that allows duplicates and preserves order
import java.util.LinkedHashSet; // Set implementation that preserves insertion order (no duplicates)
import java.util.List; // Interface for ordered collections
import java.util.Set; // Interface for collections of unique elements


// 🔹 Adapter to display branches and their menu items (grouped by branch)
public class BranchAdapter extends RecyclerView.Adapter<BranchAdapter.BranchViewHolder> {

    private final Context context; // Activity context
    private final List<AdminHomeActivity.MenuItem> menuList; // Full list of menu items
    private final OnEditClickListener editListener; // Callback for edit button
    private final List<String> uniqueBranches; // Unique list of branches (preserves order)

    // 🔹 Interface for handling edit clicks
    public interface OnEditClickListener {
        void onEditClick(AdminHomeActivity.MenuItem item); // Triggered when edit is clicked
    }

    // 🔹 Constructor
    public BranchAdapter(Context context, List<AdminHomeActivity.MenuItem> menuList,
                         OnEditClickListener editListener) {
        this.context = context; // Assign context
        this.menuList = menuList; // Assign menu list
        this.editListener = editListener; // Assign edit listener

        // Extract unique branches (preserves insertion order with LinkedHashSet)
        Set<String> branchSet = new LinkedHashSet<>();
        for (AdminHomeActivity.MenuItem item : menuList) { // Loop through all menu items
            if (item.branches != null) { // If branches exist
                branchSet.addAll(item.branches); // Add all branches of this item
            }
        }
        this.uniqueBranches = new ArrayList<>(branchSet); // Convert to list
    }

    @NonNull
    @Override
    public BranchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate branch_item_layout for each branch row
        View view = LayoutInflater.from(context).inflate(R.layout.branch_item_layout, parent, false);
        return new BranchViewHolder(view); // Return view holder
    }

    @Override
    public void onBindViewHolder(@NonNull BranchViewHolder holder, int position) {
        // Get branch name at this position
        String branchName = uniqueBranches.get(position);
        holder.branchName.setText(branchName); // Set branch name in TextView

        // Filter menus belonging to this branch
        List<AdminHomeActivity.MenuItem> filteredMenus = new ArrayList<>();
        for (AdminHomeActivity.MenuItem item : menuList) {
            if (item.branches != null && item.branches.contains(branchName)) {
                filteredMenus.add(item); // Add menu if it belongs to current branch
            }
        }

        // Create MenuAdapter for horizontal list of menus
        MenuAdapter menuAdapter = new MenuAdapter(filteredMenus, item -> {
            // Edit callback
            if (editListener != null) editListener.onEditClick(item);
        }, item -> {
            // Delete callback: call AdminHomeActivity's showDeletePopup
            if (context instanceof AdminHomeActivity) {
                ((AdminHomeActivity) context).showDeletePopup(item, null);
            }
        });

        // Set horizontal RecyclerView inside each branch row
        holder.menuRecyclerView.setLayoutManager(
                new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        holder.menuRecyclerView.setAdapter(menuAdapter); // Attach adapter
    }

    @Override
    public int getItemCount() {
        return uniqueBranches.size(); // Number of branches
    }

    // 🔹 ViewHolder for each branch row
    static class BranchViewHolder extends RecyclerView.ViewHolder {
        TextView branchName, viewAll; // Branch name (viewAll unused here)
        RecyclerView menuRecyclerView; // RecyclerView for menu items

        public BranchViewHolder(@NonNull View itemView) {
            super(itemView);
            branchName = itemView.findViewById(R.id.categoryName); // Branch name TextView
            menuRecyclerView = itemView.findViewById(R.id.menuRecyclerView); // RecyclerView inside branch
        }
    }

    // 🔹 Update adapter with new menu list
    public void updateList(List<AdminHomeActivity.MenuItem> newList) {
        this.menuList.clear(); // Clear old list
        this.menuList.addAll(newList); // Add new list

        // Rebuild unique branches
        uniqueBranches.clear();
        Set<String> branchSet = new LinkedHashSet<>();
        for (AdminHomeActivity.MenuItem item : menuList) {
            if (item.branches != null) {
                branchSet.addAll(item.branches);
            }
        }
        uniqueBranches.addAll(branchSet);

        notifyDataSetChanged(); // Refresh UI
    }
}
