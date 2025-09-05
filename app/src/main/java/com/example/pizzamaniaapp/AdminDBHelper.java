package com.example.pizzamaniaapp;

import android.database.sqlite.SQLiteOpenHelper;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
public class AdminDBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "appDB";
    private static final int DB_VERSION = 1;
    private static final String TABLE_ADMIN = "Admin";

    public AdminDBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create Admin table
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ADMIN + " (" +
                "adminID TEXT PRIMARY KEY," +
                "username TEXT," +
                "password TEXT)");

        // Example admin (optional, can remove if manually added)
        db.execSQL("INSERT INTO " + TABLE_ADMIN + " (adminID, username, password) VALUES ('a001','admin','admin123')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ADMIN);
        onCreate(db);
    }

    // Check if admin exists with username/email and password
    public boolean checkAdmin(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_ADMIN +
                " WHERE username=? AND password=?", new String[]{username, password});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }


}
