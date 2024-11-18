package com.example.btl_chess;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvUsername, tvElo, tvRanking, tvTotalGames, tvWins, tvLosses, tvDraws, tvWinRate;
    private MaterialButton btnPlayWithComputer, btnPlayWithHuman;

    private ProfileService profileService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        profileService = new ProfileService(this);

        initViews();
        loadProfileData();
        setupListeners();
    }

    private void initViews() {
        tvUsername = findViewById(R.id.tvUsername);
        tvElo = findViewById(R.id.tvElo);
        tvRanking = findViewById(R.id.tvRanking);
        tvTotalGames = findViewById(R.id.tvTotalGames);
        tvWins = findViewById(R.id.tvWins);
        tvLosses = findViewById(R.id.tvLosses);
        tvDraws = findViewById(R.id.tvDraws);
        tvWinRate = findViewById(R.id.tvWinRate);
        btnPlayWithComputer = findViewById(R.id.btnPlayWithComputer);
        btnPlayWithHuman = findViewById(R.id.btnPlayWithHuman);
    }

    private void loadProfileData() {
        ProfileService.ProfileData data = profileService.getProfileData();

        tvUsername.setText(data.username);
        tvElo.setText("Elo: " + data.elo);
        tvRanking.setText("Ranking: #" + data.ranking);
        tvTotalGames.setText("Total: " + data.totalGames);
        tvWins.setText("Wins: " + data.wins);
        tvLosses.setText("Losses: " + data.losses);
        tvDraws.setText("Draws: " + data.draws);

        float winRate = data.totalGames > 0 ? (float) data.wins / data.totalGames * 100 : 0;
        tvWinRate.setText(String.format("Win Rate: %.1f%%", winRate));
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