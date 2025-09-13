package com.example.pizzamaniaapp.models;

import com.google.firebase.database.IgnoreExtraProperties;

// Safe if DB has extra fields; Firebase will ignore them
@IgnoreExtraProperties
public class User {
    public String userID;
    public String name;
    public String email;
    public String phone;    // Use String to keep leading 0s (recommended)
    public String address;
    public String role;


    //Needed For FireBAse to work
    public User() {
    }

    //Use when saving data
    public User(String userID, String name, String email, String phone, String address, String role) {
        this.userID = userID;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.role = role;
    }
}
