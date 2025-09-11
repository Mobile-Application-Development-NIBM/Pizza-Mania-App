package com.example.pizzamaniaapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.Log;
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
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.pizzamaniaapp.CartAdapter;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CustomerHomeActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST = 100;
    private static final String TAG = "CustomerHome";

    private String currentBranchID;

    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference dbRef;

    private RecyclerView recyclerView;
    private List<MenuItem> menuList = new ArrayList<>();
    private CustomerCategoryAdapter categoryAdapter;

    // Cart
    Cart currentCart;
    private TextView cartBadge;
    public TextView cartPopupTotalText;

    // Loading dialog
    private AlertDialog loadingDialog;

    private final String currentUserID = "u001"; // Replace with actual logged-in user

    private List<String> searchSuggestions = new ArrayList<>();
    private ArrayAdapter<String> suggestionsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Activity started");
        setContentView(R.layout.activity_customer_home);

        recyclerView = findViewById(R.id.branchRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        categoryAdapter = new CustomerCategoryAdapter(this, menuList, this::showMenuPopup, "initialBranch");
        recyclerView.setAdapter(categoryAdapter);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        dbRef = FirebaseDatabase.getInstance().getReference();

        FloatingActionButton chatFab = findViewById(R.id.chatbotButton);
        chatFab.setOnClickListener(v -> {
            ChatbotDialogFragment chatbotDialog = new ChatbotDialogFragment();
            chatbotDialog.show(getSupportFragmentManager(), "ChatbotDialog");
        });

        cartBadge = findViewById(R.id.cartBadge);
        updateCartBadge();

        checkPermissionsAndLoad();

        ImageButton cartButton = findViewById(R.id.cartButton);
        cartButton.setOnClickListener(v -> showCartPopup());

        setupCartListener(currentUserID);
        setupSearch();
    }

    private void updateCartBadge() {
        if (currentCart != null && currentCart.totalItems > 0) {
            cartBadge.setText(String.valueOf(currentCart.totalItems));
            cartBadge.setVisibility(View.VISIBLE);
        } else {
            cartBadge.setVisibility(View.GONE);
        }
    }

    private void checkPermissionsAndLoad() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        } else {
            checkGpsAndFetchLocation();
        }
    }


    private AlertDialog gpsDialog;

    private void checkGpsAndFetchLocation() {
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            if (gpsDialog != null && gpsDialog.isShowing()) return; // avoid duplicate dialogs

            gpsDialog = new AlertDialog.Builder(this)
                    .setTitle("Enable GPS")
                    .setMessage("GPS is required to find the nearest branch. Enable it?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                        showCustomToast("Cannot proceed without GPS");
                        dialog.dismiss();
                    })
                    .setCancelable(false) // prevent closing by tapping outside
                    .create();
            gpsDialog.show();
        } else {
            if (gpsDialog != null && gpsDialog.isShowing()) gpsDialog.dismiss(); // ✅ dismiss when GPS is enabled
            fetchCurrentLocation();
        }
    }

    private void fetchCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        showLoadingDialog("Fetching location...");

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    hideLoadingDialog();
                    if (location != null) {
                        findNearestBranchAndLoadMenus(location);
                    } else {
                        showCustomToast("Location is null, move outside for GPS fix");
                    }
                });
    }

    private void findNearestBranchAndLoadMenus(Location userLocation) {
        showLoadingDialog("Finding nearest branch...");
        dbRef.child("branches").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String nearestBranchID = null;
                double nearestDistance = Double.MAX_VALUE;
                for (DataSnapshot branchSnap : snapshot.getChildren()) {
                    Branch branch = branchSnap.getValue(Branch.class);
                    if (branch != null) {
                        Location branchLocation = new Location("");
                        branchLocation.setLatitude(branch.latitude);
                        branchLocation.setLongitude(branch.longitude);
                        float distance = userLocation.distanceTo(branchLocation);
                        if (distance < nearestDistance) {
                            nearestDistance = distance;
                            nearestBranchID = branch.branchID;
                        }
                    }
                }
                hideLoadingDialog();
                if (nearestBranchID != null) {
                    currentBranchID = nearestBranchID; // store it
                    loadCart(nearestBranchID, currentUserID);
                    loadMenusForBranch(nearestBranchID);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hideLoadingDialog();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: re-checking permissions and GPS");
        checkPermissionsAndLoad();

        // ✅ Extra safety: close dialog if still open
        if (gpsDialog != null && gpsDialog.isShowing()) {
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                gpsDialog.dismiss();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult: requestCode=" + requestCode);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted — proceed to check GPS and fetch location.
                Log.d(TAG, "Location permission granted");
                checkGpsAndFetchLocation();
            } else {
                Log.d(TAG, "Location permission denied");
                showCustomToast("Location permission denied. App needs location to find nearest branch.");
                // Optionally: you can show an explanatory dialog with a button to open app settings.
            }
        }
    }


    private void loadMenusForBranch(String branchID) {
        showLoadingDialog("Loading menus...");
        dbRef.child("menu").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                menuList.clear();
                for (DataSnapshot menuSnap : snapshot.getChildren()) {
                    MenuItem menuItem = menuSnap.getValue(MenuItem.class);
                    if (menuItem != null && menuItem.branches != null && menuItem.branches.contains(branchID)) {
                        menuList.add(menuItem);
                    }
                }
                categoryAdapter.updateList(menuList, branchID);
                updateSearchSuggestions();
                hideLoadingDialog();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hideLoadingDialog();
            }
        });
    }

    // ---------------- CART METHODS ----------------
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
                updateCartBadge();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showCustomToast("Failed to load cart");
            }
        });
    }

    private void setupCartListener(String customerID) {
        String cartID = "c_" + customerID;
        DatabaseReference cartRef = dbRef.child("carts").child(cartID);

        cartRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentCart = snapshot.getValue(Cart.class);
                } else {
                    currentCart = new Cart(cartID, "initialBranch", customerID);
                }
                updateCartBadge();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showCustomToast("Failed to load cart");
            }
        });
    }

    // ---------------- MENU POPUP ----------------
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

        // Bind data
        TextView title = popupView.findViewById(R.id.popupMenuTitle);
        TextView price = popupView.findViewById(R.id.popupMenuPrice);
        TextView desc = popupView.findViewById(R.id.popupMenuDescription);
        ImageView image = popupView.findViewById(R.id.popupMenuImage);

        title.setText(item.name);
        price.setText("Rs. " + item.price);
        desc.setText(item.description != null ? item.description : "No description");
        Glide.with(this).load(item.imageURL).placeholder(R.drawable.sample_pizza).into(image);

        // Quantity
        TextView quantityText = popupView.findViewById(R.id.quantityText);
        ImageButton plusBtn = popupView.findViewById(R.id.plusButton);
        ImageButton minusBtn = popupView.findViewById(R.id.minusButton);

        int initialQuantity = 1;
        if (currentCart != null && currentCart.items != null) {
            for (CartItem ci : currentCart.items) {
                if (ci.menuID.equals(item.menuID)) {
                    initialQuantity = ci.quantity;
                    break;
                }
            }
        }
        quantityText.setText(String.valueOf(initialQuantity));

        plusBtn.setOnClickListener(v -> quantityText.setText(String.valueOf(Integer.parseInt(quantityText.getText().toString()) + 1)));
        minusBtn.setOnClickListener(v -> {
            int q = Integer.parseInt(quantityText.getText().toString());
            if (q > 1) quantityText.setText(String.valueOf(q - 1));
        });

        // Add to cart
        MaterialButton addToCartBtn = popupView.findViewById(R.id.addToCartButton);
        int finalInitialQuantity = initialQuantity;
        addToCartBtn.setOnClickListener(v -> {
            int selectedQuantity = Integer.parseInt(quantityText.getText().toString());
            if (currentCart == null) return;

            if (currentCart.items == null) currentCart.items = new ArrayList<>();

            boolean found = false;
            for (CartItem ci : currentCart.items) {
                if (ci.menuID.equals(item.menuID)) {
                    found = true;
                    if (ci.quantity != selectedQuantity) ci.quantity = selectedQuantity;
                    break;
                }
            }

            if (!found) {
                currentCart.items.add(new CartItem(item.menuID, item.name, item.price, selectedQuantity, item.imageURL));
            }

            if (found && finalInitialQuantity == selectedQuantity) {
                showCustomToast("Quantity not changed");
                return;
            }

            // Update totals
            int totalItems = 0;
            double totalPrice = 0;
            for (CartItem ci : currentCart.items) {
                totalItems += ci.quantity;
                totalPrice += ci.quantity * ci.price;
            }
            currentCart.totalItems = totalItems;
            currentCart.totalPrice = totalPrice;

            dbRef.child("carts").child(currentCart.cartID).setValue(currentCart)
                    .addOnSuccessListener(aVoid -> {
                        showCustomToast("Cart updated");
                        updateCartBadge();
                    })
                    .addOnFailureListener(e -> showCustomToast("Failed to update cart"));

            popupDialog.dismiss();
        });

        ImageButton closeBtn = popupView.findViewById(R.id.closeButton);
        closeBtn.setOnClickListener(v -> popupDialog.dismiss());
    }

    // ---------------- CART POPUP ----------------
    private void showCartPopup() {
        if (currentCart == null) return;

        View popupView = LayoutInflater.from(this).inflate(R.layout.cart_popup, null);
        AlertDialog popupDialog = new AlertDialog.Builder(this).setView(popupView).create();
        popupDialog.show();

        if (popupDialog.getWindow() != null) {
            int width = (int) (330 * getResources().getDisplayMetrics().density + 0.5f);
            popupDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            popupDialog.getWindow().getDecorView().post(() ->
                    popupDialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT));
            popupDialog.getWindow().setGravity(Gravity.CENTER);
        }

        ImageButton closeBtn = popupView.findViewById(R.id.closeCartButton);
        closeBtn.setOnClickListener(v -> popupDialog.dismiss());

        RecyclerView cartRecyclerView = popupView.findViewById(R.id.cartRecyclerView);
        cartRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        cartRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        ImageView emptyImage = popupView.findViewById(R.id.emptyCartImage);
        cartPopupTotalText = popupView.findViewById(R.id.cartTotalAmount);

        Runnable updateCartUI = () -> {
            if (currentCart.items == null || currentCart.items.isEmpty()) {
                cartRecyclerView.setVisibility(View.GONE);
                emptyImage.setVisibility(View.VISIBLE);
                cartPopupTotalText.setText("Cart is empty");
            } else {
                cartRecyclerView.setVisibility(View.VISIBLE);
                emptyImage.setVisibility(View.GONE);
                cartPopupTotalText.setText("Total: Rs. " + currentCart.totalPrice);
            }
        };
        updateCartUI.run();

        CartAdapter cartAdapter = new CartAdapter(currentCart.items, this);
        cartRecyclerView.setAdapter(cartAdapter);

        MaterialButton placeOrderBtn = popupView.findViewById(R.id.placeOrderButton);
        placeOrderBtn.setOnClickListener(v -> {
            if (currentCart.items == null || currentCart.items.isEmpty()) {
                showCustomToast("Cart is empty");
                return;
            }
            placeOrder();
            popupDialog.dismiss();
        });

        cartAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() { updateCartUI.run(); }
            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) { updateCartUI.run(); }
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) { updateCartUI.run(); }
        });
    }


    public void updateCartAfterRemoval(CartItem removedItem) {
        if (currentCart == null || currentCart.items == null) return;

        int totalItems = 0;
        double totalPrice = 0;
        for (CartItem ci : currentCart.items) {
            totalItems += ci.quantity;
            totalPrice += ci.quantity * ci.price;
        }
        currentCart.totalItems = totalItems;
        currentCart.totalPrice = totalPrice;
        updateCartBadge();
        dbRef.child("carts").child(currentCart.cartID).setValue(currentCart);
    }

    private void placeOrder() {
        if (currentCart == null || currentCart.items == null || currentCart.items.isEmpty()) return;
        showLoadingDialog("Placing order...");

        dbRef.child("orders").orderByKey().limitToLast(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String newOrderID = "o001";
                        if (snapshot.exists()) {
                            for (DataSnapshot lastOrderSnap : snapshot.getChildren()) {
                                String lastID = lastOrderSnap.getKey();
                                if (lastID != null && lastID.startsWith("o")) {
                                    int num = Integer.parseInt(lastID.substring(1)) + 1;
                                    newOrderID = String.format("o%03d", num);
                                }
                            }
                        }

                        long timestamp = System.currentTimeMillis();
                        Order order = new Order(
                                newOrderID,
                                currentCart.branchID,
                                currentUserID,
                                "Niviru", "Random Address, Colombo",
                                6.9271, 79.8612,
                                "",
                                "Order Pending",
                                currentCart.totalPrice,
                                timestamp,
                                0,
                                "Pending",
                                new ArrayList<>(currentCart.items) // ✅ Save ordered items
                        );

                        dbRef.child("orders").child(newOrderID).setValue(order)
                                .addOnSuccessListener(aVoid -> {
                                    showCustomToast("Order placed successfully!");

                                    // ✅ Reset the cart instead of deleting
                                    currentCart.items.clear();
                                    currentCart.totalItems = 0;
                                    currentCart.totalPrice = 0;

                                    dbRef.child("carts").child(currentCart.cartID).setValue(currentCart);

                                    updateCartBadge();
                                    hideLoadingDialog();
                                })
                                .addOnFailureListener(e -> {
                                    showCustomToast("Failed to place order");
                                    hideLoadingDialog();
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showCustomToast("Failed to fetch last order ID");
                        hideLoadingDialog();
                    }
                });
    }

    private void updateSearchSuggestions() {
        searchSuggestions.clear();
        for (MenuItem menu : menuList) {
            searchSuggestions.add(menu.name);
            if (menu.category != null && !searchSuggestions.contains(menu.category)) {
                searchSuggestions.add(menu.category);
            }
        }
        suggestionsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, searchSuggestions);
        AutoCompleteTextView searchBox = findViewById(R.id.searchBox);
        searchBox.setAdapter(suggestionsAdapter);
        searchBox.setThreshold(1);
    }

    private void setupSearch() {
        AutoCompleteTextView searchBox = findViewById(R.id.searchBox);
        ImageButton searchButton = findViewById(R.id.searchButton);

        searchButton.setOnClickListener(v -> performSearch(searchBox.getText().toString().trim()));
        searchBox.setOnEditorActionListener((v, actionId, event) -> {
            performSearch(searchBox.getText().toString().trim());
            return true;
        });
    }

    private void performSearch(String query) {
        if (query.isEmpty()) {
            categoryAdapter.updateList(menuList, currentBranchID);
            return;
        }

        List<MenuItem> filtered = new ArrayList<>();
        for (MenuItem menu : menuList) {
            if (menu.name.toLowerCase().contains(query.toLowerCase()) ||
                    (menu.category != null && menu.category.toLowerCase().contains(query.toLowerCase()))) {
                filtered.add(menu);
            }
        }

        if (filtered.isEmpty()) {
            showCustomToast("No menus found");
            categoryAdapter.updateList(menuList, currentBranchID);
        } else {
            categoryAdapter.updateList(filtered, currentBranchID);
        }
    }

    // ---------------- MODELS ----------------
    public static class Branch { public String branchID; public String name; public double latitude; public double longitude; public long contact; public Branch() {} }
    public static class MenuItem implements Parcelable {
        public String menuID, name, category, description, imageURL;
        public double price;
        public List<String> branches;

        public MenuItem() {}

        protected MenuItem(Parcel in) {
            menuID = in.readString();
            name = in.readString();
            category = in.readString();
            description = in.readString();
            imageURL = in.readString();
            price = in.readDouble();
            branches = in.createStringArrayList();
        }

        public static final Parcelable.Creator<MenuItem> CREATOR = new Creator<MenuItem>() {
            @Override
            public MenuItem createFromParcel(Parcel in) {
                return new MenuItem(in);
            }

            @Override
            public MenuItem[] newArray(int size) {
                return new MenuItem[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(menuID);
            parcel.writeString(name);
            parcel.writeString(category);
            parcel.writeString(description);
            parcel.writeString(imageURL);
            parcel.writeDouble(price);
            parcel.writeStringList(branches);
        }
    }


    public static class CartItem { public String menuID, name, imageURL; public double price; public int quantity; public CartItem() {} public CartItem(String menuID, String name, double price, int quantity, String imageURL) { this.menuID = menuID; this.name = name; this.price = price; this.quantity = quantity; this.imageURL = imageURL; } }
    public static class Cart { public String cartID, branchID, customerID; public List<CartItem> items; public int totalItems; public double totalPrice; public Cart() {} public Cart(String cartID, String branchID, String customerID) { this.cartID = cartID; this.branchID = branchID; this.customerID = customerID; this.items = new ArrayList<>(); this.totalItems = 0; this.totalPrice = 0; } }
    public static class Order {
        public String orderID, branchID, customerID, customerName, customerAddress,
                assignedDeliverymanID, status, paymentStatus;
        public double customerLat, customerLng, totalPrice;
        public long timestamp, deliveredTimestamp;
        public List<CartItem> items; // ✅ New field

        public Order() {}

        public Order(String orderID, String branchID, String customerID,
                     String customerName, String customerAddress,
                     double customerLat, double customerLng,
                     String assignedDeliverymanID, String status,
                     double totalPrice, long timestamp, long deliveredTimestamp,
                     String paymentStatus, List<CartItem> items) {
            this.orderID = orderID;
            this.branchID = branchID;
            this.customerID = customerID;
            this.customerName = customerName;
            this.customerAddress = customerAddress;
            this.customerLat = customerLat;
            this.customerLng = customerLng;
            this.assignedDeliverymanID = assignedDeliverymanID;
            this.status = status;
            this.totalPrice = totalPrice;
            this.timestamp = timestamp;
            this.deliveredTimestamp = deliveredTimestamp;
            this.paymentStatus = paymentStatus;
            this.items = items; // ✅ store ordered menus
        }
    }

    // ---------------- LOADING DIALOG ----------------
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

    // ---------------- CUSTOM TOAST ----------------
    private void showCustomToast(String message) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_message, null);

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

            public void onFinish() { if (dialog.isShowing()) dialog.dismiss(); }
        }.start();
    }
}