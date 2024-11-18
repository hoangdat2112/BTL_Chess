package com.example.btl_chess;

import android.app.ActivityManager;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {
    private EditText userName, passWord;
    private Button login, register;
    private DBHelper dbHelper;
    private TextView goToRegister;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
       init();
       login.setOnClickListener(v ->{
           String username = userName.getText().toString().trim();
           String password = passWord.getText().toString().trim();
           if(dbHelper.checkLogin(username,password)){
               Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();
               Intent intent = new Intent(LoginActivity.this, ProfileActivity.class);
               intent.putExtra("username", username);
               startActivity(intent);
           }
           else{
               Toast.makeText(this, "Your Username or password is incorrect", Toast.LENGTH_SHORT).show();
           }
       });
      goToRegister.setOnClickListener(v ->{
          Intent intent= new Intent(LoginActivity.this,RegisterActivity.class);
          startActivity(intent);
      });
    }
    private void init(){
        dbHelper = new DBHelper(this);
        userName= findViewById(R.id.etUsername);
        passWord= findViewById(R.id.etPassword);
        goToRegister= findViewById(R.id.tvGotoRegister);
        login=findViewById(R.id.btnLogin);
    }
}