package com.example.btl_chess;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "ChessApp.db";
    private static final int DATABASE_VERSION = 2;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE IF NOT EXISTS Users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT UNIQUE, " +
                "password TEXT, " +
                "elo INTEGER DEFAULT 1200, " +
                "games_played INTEGER DEFAULT 0, " +
                "games_won INTEGER DEFAULT 0, " +
                "games_lost INTEGER DEFAULT 0, " +
                "games_drawn INTEGER DEFAULT 0)";
        db.execSQL(createTable);

    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
          //
    }
    // Đăng ký người dùng
    public boolean registerUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("password", password);
        values.put("elo", 1200);
        values.put("games_played", 0);
        values.put("games_won", 0);
        values.put("games_lost", 0);
        values.put("games_drawn", 0);
        long result = db.insert("Users", null, values);
        return result != -1;
    }
    // Kiểm tra thông tin đăng nhập
    public boolean checkLogin(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM Users WHERE username = ? AND password = ?";
        Cursor cursor = db.rawQuery(query, new String[]{username, password});
        boolean isLoggedIn = cursor.getCount() > 0;
        cursor.close();
        return isLoggedIn;
    }
    public User getUserByUsername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("Users", null, "username = ?", new String[]{username}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            @SuppressLint("Range") User user = new User(
                    cursor.getString(cursor.getColumnIndex("username")),
                    cursor.getInt(cursor.getColumnIndex("elo")),
                    cursor.getInt(cursor.getColumnIndex("games_played")),
                    cursor.getInt(cursor.getColumnIndex("games_won")),
                    cursor.getInt(cursor.getColumnIndex("games_lost")),
                    cursor.getInt(cursor.getColumnIndex("games_drawn"))
            );
            cursor.close();
            return user;
        }
        return null;
    }
}
