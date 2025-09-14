package com.example.pizzamaniaapp;

public class Item {
    private String orderID;
    private String paymentStatus;
    private String status;
    private long timestamp;
    private String name;
    private String menuID;
    private String imageURL;
    private int quantity;
    private double price;       // kept for Firebase mapping
    private double totalPrice;

    public Item() {}

    public String getOrderID() { return orderID; }
    public void setOrderID(String orderID) { this.orderID = orderID; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMenuID() { return menuID; }
    public void setMenuID(String menuID) { this.menuID = menuID; }

    public String getImageURL() { return imageURL; }
    public void setImageURL(String imageURL) { this.imageURL = imageURL; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
}
