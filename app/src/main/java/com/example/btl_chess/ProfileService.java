package com.example.btl_chess;

import android.content.Context;
import android.content.SharedPreferences;

public class ProfileService {
    private static final String PREFS_NAME = "ChessAppPrefs";
    private SharedPreferences sharedPreferences;

    public ProfileService(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void updateStats(boolean isWin, boolean isDraw) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        int totalGames = sharedPreferences.getInt("totalGames", 0) + 1;
        int wins = sharedPreferences.getInt("wins", 0);
        int losses = sharedPreferences.getInt("losses", 0);
        int draws = sharedPreferences.getInt("draws", 0);

        if (isWin) {
            wins++;
        } else if (isDraw) {
            draws++;
        } else {
            losses++;
        }

        editor.putInt("totalGames", totalGames);
        editor.putInt("wins", wins);
        editor.putInt("losses", losses);
        editor.putInt("draws", draws);
        editor.apply();

        updateEloAndRanking(isWin, isDraw);
    }

    private void updateEloAndRanking(boolean isWin, boolean isDraw) {
        int currentElo = sharedPreferences.getInt("elo", 1500);
        int eloChange = isWin ? 10 : (isDraw ? 0 : -10);
        int newElo = Math.max(100, currentElo + eloChange);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("elo", newElo);

        // This is a simplified ranking calculation. In a real app, you'd need a more sophisticated system.
        int newRanking = 10000 - newElo;
        editor.putInt("ranking", newRanking);

        editor.apply();
    }

    public ProfileData getProfileData() {
        String username = sharedPreferences.getString("username", "Guest");
        int elo = sharedPreferences.getInt("elo", 1500);
        int ranking = sharedPreferences.getInt("ranking", 1000);
        int totalGames = sharedPreferences.getInt("totalGames", 0);
        int wins = sharedPreferences.getInt("wins", 0);
        int losses = sharedPreferences.getInt("losses", 0);
        int draws = sharedPreferences.getInt("draws", 0);

        return new ProfileData(username, elo, ranking, totalGames, wins, losses, draws);
    }

    public static class ProfileData {
        public String username;
        public int elo;
        public int ranking;
        public int totalGames;
        public int wins;
        public int losses;
        public int draws;

        public ProfileData(String username, int elo, int ranking, int totalGames, int wins, int losses, int draws) {
            this.username = username;
            this.elo = elo;
            this.ranking = ranking;
            this.totalGames = totalGames;
            this.wins = wins;
            this.losses = losses;
            this.draws = draws;
        }
    }
}