package com.example.btl_chess;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class RegisterActivity extends AppCompatActivity {
   private EditText userName, passWord, confirmPassword;
   private Button Register;
   private TextView goToLogin;
   private DBHelper dbHelper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
       init();
       Register.setOnClickListener(v->{
           String username= userName.getText().toString().trim();
           String password= passWord.getText().toString().trim();
           String confirmpassword= confirmPassword.getText().toString().trim();
           if(username.isEmpty()||password.isEmpty()||confirmpassword.isEmpty()){
               Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
               return;
           }
           if(password.equals(confirmpassword)==false){
               Toast.makeText(this, "Your Confirm password is wrong", Toast.LENGTH_SHORT).show();
               return;
           }
           if(dbHelper.registerUser(username, password)){
               Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show();
               // Chuyển đến màn hình Login
               startActivity(new Intent(this, LoginActivity.class));
           }
           else{
               Toast.makeText(this, "Username already exists", Toast.LENGTH_SHORT).show();
           }
       });
       goToLogin.setOnClickListener(v->{
           startActivity(new Intent(this, LoginActivity.class));
       });
    }
    private void init(){
        dbHelper = new DBHelper(this);
        userName= findViewById(R.id.etUsername);
        passWord= findViewById(R.id.etPassword);
        confirmPassword= findViewById(R.id.etConfirmPassword);
        goToLogin= findViewById(R.id.tvLoginPrompt);
        Register=findViewById(R.id.btnRegister);
    }
}