package com.example.pizzamaniaapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ProfileDBHelper extends SQLiteOpenHelper {

    // DB name
    private static final String DATABASE_NAME = "profileDB.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_NAME = "profile";

    // primary key
    private static final String COLUMN_ID = "id";

    // store path as string
    private static final String COLUMN_IMAGE_PATH = "image_path";

    public ProfileDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create table
        String createTable = "CREATE TABLE " + TABLE_NAME + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY, "
                + COLUMN_IMAGE_PATH + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop old table if exists
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // Insert or update image path
    public void saveImagePath(String path) {
        SQLiteDatabase db = this.getWritableDatabase();

        // We will use ID = 1 since only one profile image per user
        ContentValues values = new ContentValues();
        values.put(COLUMN_IMAGE_PATH, path);

        // Check if entry already exists
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_ID + " = 1", null);
        if(cursor.moveToFirst()){
            // Update existing
            db.update(TABLE_NAME, values, COLUMN_ID + " = ?", new String[]{"1"});
        } else {
            // Insert new
            values.put(COLUMN_ID, 1);
            db.insert(TABLE_NAME, null, values);
        }
        cursor.close();
        db.close();
    }

    // Get saved image path
    public String getImagePath() {
        SQLiteDatabase db = this.getReadableDatabase();
        String path = null;

        Cursor cursor = db.rawQuery("SELECT " + COLUMN_IMAGE_PATH + " FROM " + TABLE_NAME + " WHERE " + COLUMN_ID + " = 1", null);
        if(cursor.moveToFirst()){
            path = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_PATH));
        }

        cursor.close();
        db.close();
        return path;
    }


}
