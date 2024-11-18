package com.example.btl_chess;

public class User {
    private String username;
    private int elo;
    private int gamesPlayed;
    private int gamesWon;
    private int gamesLost;
    private int gamesDrawn;

    public User(String username, int elo, int gamesPlayed, int gamesWon, int gamesLost, int gamesDrawn) {
        this.username = username;
        this.elo = elo;
        this.gamesPlayed = gamesPlayed;
        this.gamesWon = gamesWon;
        this.gamesLost = gamesLost;
        this.gamesDrawn = gamesDrawn;
    }

    public String getUsername() { return username; }
    public int getElo() { return elo; }
    public int getGamesPlayed() { return gamesPlayed; }
    public int getGamesWon() { return gamesWon; }
    public int getGamesLost() { return gamesLost; }
    public int getGamesDrawn() { return gamesDrawn; }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setElo(int elo) {
        this.elo = elo;
    }

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    public void setGamesWon(int gamesWon) {
        this.gamesWon = gamesWon;
    }

    public void setGamesLost(int gamesLost) {
        this.gamesLost = gamesLost;
    }

    public void setGamesDrawn(int gamesDrawn) {
        this.gamesDrawn = gamesDrawn;
    }
}
