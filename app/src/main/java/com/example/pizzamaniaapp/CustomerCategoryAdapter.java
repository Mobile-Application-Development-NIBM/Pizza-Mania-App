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

public class CustomerCategoryAdapter extends RecyclerView.Adapter<CustomerCategoryAdapter.CategoryViewHolder> {

    private final Context context;
    private final List<CustomerHomeActivity.MenuItem> allMenus;
    private final OnViewClickListener viewListener;
    private String currentBranchID;
    private List<String> categoryList; // list of category names
    private Map<String, List<CustomerHomeActivity.MenuItem>> menusByCategory;

    public interface OnViewClickListener {
        void onViewClick(CustomerHomeActivity.MenuItem item);
    }

    public CustomerCategoryAdapter(Context context, List<CustomerHomeActivity.MenuItem> menuList,
                                   OnViewClickListener viewListener, String branchID) {
        Log.d("CustomerCategoryAdapter", "Adapter constructor called");
        this.context = context;
        this.allMenus = menuList != null ? new ArrayList<>(menuList) : new ArrayList<>();
        this.viewListener = viewListener;
        this.currentBranchID = branchID;
        buildCategoryMap();
    }

    // Call this whenever new menus or branch change
    public void updateList(List<CustomerHomeActivity.MenuItem> newList, String branchID) {
        Log.d("CustomerCategoryAdapter", "updateList called, newList size: " + (newList != null ? newList.size() : "null") + ", branchID: '" + branchID + "'");
        if (newList == null) return;

        this.currentBranchID = branchID;
        this.allMenus.clear();
        this.allMenus.addAll(newList);
        buildCategoryMap();
        notifyDataSetChanged();
    }

    // Build a map: category -> menus in that category & branch
    private void buildCategoryMap() {
        categoryList = new ArrayList<>();
        menusByCategory = new HashMap<>();

        for (CustomerHomeActivity.MenuItem item : allMenus) {
            boolean branchMatch = false;
            if (item.branches != null) {
                for (String branch : item.branches) {
                    if (branch.trim().equalsIgnoreCase(currentBranchID.trim())) {
                        branchMatch = true;
                        break;
                    }
                }
            }
            if (!branchMatch) continue;

            String category = item.category != null ? item.category : "Other";
            if (!menusByCategory.containsKey(category)) {
                menusByCategory.put(category, new ArrayList<>());
                categoryList.add(category);
            }
            menusByCategory.get(category).add(item);
        }
        Log.d("CustomerCategoryAdapter", "Categories built: " + categoryList.size());
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.category_item_layout, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        String category = categoryList.get(position);
        holder.categoryName.setText(category);

        // Horizontal RecyclerView for this category
        List<CustomerHomeActivity.MenuItem> menuList = menusByCategory.get(category);
        CustomerMenuAdapter menuAdapter = new CustomerMenuAdapter(menuList, item -> {
            if (viewListener != null) viewListener.onViewClick(item);
            Log.d("CustomerCategoryAdapter", "Menu item clicked: " + item.name);
        });

        holder.menuRecyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        holder.menuRecyclerView.setAdapter(menuAdapter);

        // ---- View All button ----
        holder.itemView.findViewById(R.id.viewAllButton).setOnClickListener(v -> {
            if (menuList != null && !menuList.isEmpty()) {
                Intent intent = new Intent(context, MenuUnderCategoryActivity.class);
                intent.putExtra("branchID", currentBranchID);           // pass branch
                intent.putExtra("category_name", category);             // pass category
                intent.putParcelableArrayListExtra(                      // ✅ use Parcelable
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
        return categoryList != null ? categoryList.size() : 0;
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView categoryName;
        RecyclerView menuRecyclerView;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryName = itemView.findViewById(R.id.categoryName);
            menuRecyclerView = itemView.findViewById(R.id.menuRecyclerView);
        }
    }
}
