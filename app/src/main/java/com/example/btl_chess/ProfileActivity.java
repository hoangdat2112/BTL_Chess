package com.example.btl_chess;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvUsername, tvElo, tvTotalGames, tvWins, tvLosses, tvDraws, tvWinRate;
    private MaterialButton btnPlayWithComputer, btnPlayWithHuman;
    private DBHelper dbHelper;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        initViews();
        loadUserData();
        setupListeners();
    }

    private void initViews() {
        username =getIntent().getStringExtra("username");
        tvUsername = findViewById(R.id.tvUsername);
        tvElo = findViewById(R.id.tvElo);
        tvTotalGames = findViewById(R.id.tvTotalGames);
        tvWins = findViewById(R.id.tvWins);
        tvLosses = findViewById(R.id.tvLosses);
        tvDraws = findViewById(R.id.tvDraws);
        tvWinRate = findViewById(R.id.tvWinRate);
        btnPlayWithComputer = findViewById(R.id.btnPlayWithComputer);
        btnPlayWithHuman = findViewById(R.id.btnPlayWithHuman);
        dbHelper = new DBHelper(this);
    }

    private void loadUserData() {
        User user = dbHelper.getUserByUsername(username);

        if (user != null) {
            tvUsername.setText(user.getUsername());
            tvElo.setText("Elo: " + user.getElo());
            tvTotalGames.setText("Games Played: " + user.getGamesPlayed());
            tvWins.setText("Games Won: " + user.getGamesWon());
            tvLosses.setText("Games Lost: " + user.getGamesLost());
            tvDraws.setText("Games Drawn: " + user.getGamesDrawn());

            int winRate = (user.getGamesPlayed() > 0) ? (user.getGamesWon() * 100) / user.getGamesPlayed() : 0;
            tvWinRate.setText("Winrate: " + winRate + "%");
        }
    }

    private void setupListeners() {
        btnPlayWithComputer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileActivity.this, AI_Player.class);
                startActivity(intent);
            }
        });

        btnPlayWithHuman.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }
}