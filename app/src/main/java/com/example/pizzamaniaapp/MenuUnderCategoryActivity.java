package com.example.pizzamaniaapp;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.pizzamaniaapp.CustomerHomeActivity.Cart;
import com.example.pizzamaniaapp.CustomerHomeActivity.CartItem;
import com.example.pizzamaniaapp.CustomerHomeActivity.MenuItem;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MenuUnderCategoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MenuUnderCategoryAdapter adapter;
    private List<MenuItem> categoryMenus = new ArrayList<>();

    private DatabaseReference dbRef;
    private Cart currentCart;
    private AlertDialog loadingDialog;

    private FusedLocationProviderClient fusedLocationClient;

    private String currentUserID;
    private String currentUserName;
    private String branchID;

    // ---------------- SEARCH VARIABLES ----------------
    private List<String> searchSuggestions = new ArrayList<>();
    private ArrayAdapter<String> suggestionsAdapter;
    private List<MenuItem> allMenus = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_under_category);

        setupEdgeToEdge();

        // Initialize Firebase and location
        dbRef = FirebaseDatabase.getInstance().getReference();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        recyclerView = findViewById(R.id.viewAllRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        TextView categoryText = findViewById(R.id.categoryText);

        // ------------------- GET BRANCH ID -------------------
        branchID = getIntent().getStringExtra("branchID");
        if (branchID == null) branchID = "defaultBranch"; // fallback

        String categoryName = getIntent().getStringExtra("category_name");
        if (categoryName != null) categoryText.setText(categoryName);

        List<CustomerHomeActivity.MenuItem> menuList =
                getIntent().getParcelableArrayListExtra("menu_list");

        if (menuList != null) {
            categoryMenus.clear();
            categoryMenus.addAll(menuList);
            allMenus.clear();
            allMenus.addAll(menuList);

            // Setup search immediately
            setupSearch();
            updateSearchSuggestions();
        } else {
            loadMenusForBranch(branchID, categoryName);
        }

        adapter = new MenuUnderCategoryAdapter(categoryMenus, this::showMenuPopup);
        recyclerView.setAdapter(adapter);

        // ------------------- GET USER INFO -------------------
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        currentUserID = prefs.getString("userID", "u001"); // fallback
        currentUserName = prefs.getString("name", "User"); // optional

        // ------------------- LOAD CART -------------------
        loadCart(branchID, currentUserID);

        // Back button
        ImageButton backBtn = findViewById(R.id.backButton);
        backBtn.setOnClickListener(v -> finish());

        // Chatbot button
        FloatingActionButton chatFab = findViewById(R.id.chatbotButton);
        chatFab.setOnClickListener(v -> {
            ChatbotDialogFragment chatbotDialog = new ChatbotDialogFragment();
            chatbotDialog.show(getSupportFragmentManager(), "ChatbotDialog");
        });
    }

    private void setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void loadMenusForBranch(String branchID, String categoryFilter) {
        showLoadingDialog("Loading menus...");
        dbRef.child("menu").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allMenus.clear();
                categoryMenus.clear();
                for (DataSnapshot menuSnap : snapshot.getChildren()) {
                    MenuItem menuItem = menuSnap.getValue(MenuItem.class);
                    if (menuItem != null
                            && menuItem.branches != null
                            && menuItem.branches.contains(branchID)
                            && (categoryFilter == null || menuItem.category.equals(categoryFilter))) {
                        allMenus.add(menuItem);
                        categoryMenus.add(menuItem);
                    }
                }
                adapter.notifyDataSetChanged();
                hideLoadingDialog();

                setupSearch();
                updateSearchSuggestions();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hideLoadingDialog();
                showCustomToast("Failed to load menus");
            }
        });
    }

    private void loadCart(String branchID, String customerID) {
        String cartID = "c_" + customerID;
        DatabaseReference cartRef = dbRef.child("carts").child(cartID);
        cartRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentCart = snapshot.getValue(Cart.class);
                } else {
                    currentCart = new Cart(cartID, branchID, customerID);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showCustomToast("Failed to load cart");
            }
        });
    }

    private void showMenuPopup(MenuItem item) {
        View popupView = LayoutInflater.from(this).inflate(R.layout.view_menu_popup, null);
        AlertDialog popupDialog = new AlertDialog.Builder(this).setView(popupView).create();
        popupDialog.show();

        if (popupDialog.getWindow() != null) {
            int width = (int) (300 * getResources().getDisplayMetrics().density + 0.5f);
            popupDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            popupDialog.getWindow().getDecorView().post(() ->
                    popupDialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT));
            popupDialog.getWindow().setGravity(Gravity.CENTER);
        }

        TextView title = popupView.findViewById(R.id.popupMenuTitle);
        TextView price = popupView.findViewById(R.id.popupMenuPrice);
        TextView desc = popupView.findViewById(R.id.popupMenuDescription);
        ImageView image = popupView.findViewById(R.id.popupMenuImage);

        title.setText(item.name);
        price.setText("Rs. " + item.price);
        desc.setText(item.description != null ? item.description : "No description");

        Glide.with(this).load(item.imageURL).placeholder(R.drawable.sample_pizza).into(image);

        TextView quantityText = popupView.findViewById(R.id.quantityText);
        ImageButton plusBtn = popupView.findViewById(R.id.plusButton);
        ImageButton minusBtn = popupView.findViewById(R.id.minusButton);

        int existingQuantity = 1;
        if (currentCart != null && currentCart.items != null) {
            for (CartItem ci : currentCart.items) {
                if (ci.menuID.equals(item.menuID)) {
                    existingQuantity = ci.quantity;
                    break;
                }
            }
        }
        quantityText.setText(String.valueOf(existingQuantity));

        plusBtn.setOnClickListener(v -> {
            int q = Integer.parseInt(quantityText.getText().toString());
            quantityText.setText(String.valueOf(q + 1));
        });
        minusBtn.setOnClickListener(v -> {
            int q = Integer.parseInt(quantityText.getText().toString());
            if (q > 1) quantityText.setText(String.valueOf(q - 1));
        });

        MaterialButton addToCartBtn = popupView.findViewById(R.id.addToCartButton);
        addToCartBtn.setOnClickListener(v -> {
            int selectedQuantity = Integer.parseInt(quantityText.getText().toString());
            if (currentCart == null) return;

            if (currentCart.items == null) currentCart.items = new ArrayList<>();

            boolean found = false;
            int initialQuantity = 0;

            for (CartItem ci : currentCart.items) {
                if (ci.menuID.equals(item.menuID)) {
                    initialQuantity = ci.quantity;
                    if (ci.quantity != selectedQuantity) {
                        ci.quantity = selectedQuantity;
                    }
                    found = true;
                    break;
                }
            }

            if (!found) {
                currentCart.items.add(new CartItem(item.menuID, item.name, item.price, selectedQuantity, item.imageURL));
            }

            if (found && initialQuantity == selectedQuantity) {
                showCustomToast("Quantity not changed");
                return;
            }

            int totalItems = 0;
            double totalPrice = 0;
            for (CartItem ci : currentCart.items) {
                totalItems += ci.quantity;
                totalPrice += ci.quantity * ci.price;
            }
            currentCart.totalItems = totalItems;
            currentCart.totalPrice = totalPrice;

            dbRef.child("carts").child(currentCart.cartID).setValue(currentCart)
                    .addOnSuccessListener(aVoid -> showCustomToast("Cart updated"))
                    .addOnFailureListener(e -> showCustomToast("Failed to update cart"));

            popupDialog.dismiss();
        });

        ImageButton closeBtn = popupView.findViewById(R.id.closeButton);
        closeBtn.setOnClickListener(v -> popupDialog.dismiss());
    }

    // ---------------- UPDATE SEARCH SUGGESTIONS ----------------
    private void updateSearchSuggestions() {
        searchSuggestions.clear();
        for (MenuItem menu : allMenus) {
            if (!searchSuggestions.contains(menu.name)) {
                searchSuggestions.add(menu.name);
            }
        }
        if (suggestionsAdapter != null) {
            suggestionsAdapter.notifyDataSetChanged();
        }
    }

    // ---------------- SETUP SEARCH ----------------
    private void setupSearch() {
        AutoCompleteTextView searchBox = findViewById(R.id.searchBox);
        ImageButton searchButton = findViewById(R.id.searchButton);

        suggestionsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, searchSuggestions);
        searchBox.setAdapter(suggestionsAdapter);
        searchBox.setThreshold(1);

        searchButton.setOnClickListener(v -> performSearch(searchBox.getText().toString().trim()));
        searchBox.setOnEditorActionListener((v, actionId, event) -> {
            performSearch(searchBox.getText().toString().trim());
            return true;
        });
    }

    // ---------------- PERFORM SEARCH ----------------
    private void performSearch(String query) {
        if (query.isEmpty()) {
            categoryMenus.clear();
            categoryMenus.addAll(allMenus);
            adapter.notifyDataSetChanged();
            return;
        }

        List<MenuItem> filtered = new ArrayList<>();
        for (MenuItem menu : allMenus) {
            if (menu.name.toLowerCase().contains(query.toLowerCase())) {
                filtered.add(menu);
            }
        }

        categoryMenus.clear();
        if (filtered.isEmpty()) {
            showCustomToast("No menus found");
            categoryMenus.addAll(allMenus); // restore menus instead of blank page
        } else {
            categoryMenus.addAll(filtered);
        }

        adapter.notifyDataSetChanged();
    }

    // ---------------- LOADING & TOAST ----------------
    private void showLoadingDialog(String message) {
        if (loadingDialog != null && loadingDialog.isShowing()) return;

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null);
        TextView textView = view.findViewById(R.id.loadingText);
        textView.setText(message);

        loadingDialog = new AlertDialog.Builder(this).setView(view).setCancelable(false).create();
        loadingDialog.show();

        if (loadingDialog.getWindow() != null) {
            int width = (int) (300 * getResources().getDisplayMetrics().density);
            int height = (int) (180 * getResources().getDisplayMetrics().density);
            loadingDialog.getWindow().setLayout(width, height);
            loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) loadingDialog.dismiss();
    }

    private void showCustomToast(String message) {
        View layout = LayoutInflater.from(this).inflate(R.layout.custom_message, null);
        TextView toastMessage = layout.findViewById(R.id.toast_message);
        ImageView close = layout.findViewById(R.id.toast_close);
        ProgressBar progressBar = layout.findViewById(R.id.toast_progress);

        toastMessage.setText(message);
        progressBar.setProgress(100);

        AlertDialog dialog = new AlertDialog.Builder(this).setView(layout).create();
        close.setOnClickListener(v -> dialog.dismiss());

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(0f);
            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.TOP;
            params.y = 50;
            dialog.getWindow().setAttributes(params);
        }

        dialog.show();

        new CountDownTimer(3000, 50) {
            public void onTick(long millisUntilFinished) {
                int progress = (int) Math.max(0, Math.round((millisUntilFinished / 3000.0) * 100));
                progressBar.setProgress(progress);
            }

            public void onFinish() {
                if (dialog.isShowing()) dialog.dismiss();
            }
        }.start();
    }
}