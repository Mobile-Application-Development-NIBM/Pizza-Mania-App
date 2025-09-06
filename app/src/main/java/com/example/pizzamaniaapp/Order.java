package com.example.pizzamaniaapp;

//represents the data for one order
public class Order {
    private String orderId;
    private String customerName;
    private String items;
    private String status;

    public Order() {
        // Required empty constructor for Firebase
    }

    public Order(String orderId, String customerName, String items, String status) {
        this.orderId = orderId;
        this.customerName = customerName;
        this.items = items;
        this.status = status;
    }

    public String getOrderId() { return orderId; }
    public String getCustomerName() { return customerName; }
    public String getItems() { return items; }
    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
}
