package com.example.btl_chess;

public class ChessPiece {
    private final int col;
    private final int row;
    private final Player player;
    private final Chessman chessman;
    private final int resID;

    public ChessPiece(int col, int row, Player player, Chessman chessman, int resID) {
        this.col = col;
        this.row = row;
        this.player = player;
        this.chessman = chessman;
        this.resID = resID;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    public Player getPlayer() {
        return player;
    }

    public Chessman getChessman() {
        return chessman;
    }

    public int getResID() {
        return resID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ChessPiece that = (ChessPiece) o;

        if (col != that.col) return false;
        if (row != that.row) return false;
        if (resID != that.resID) return false;
        if (player != that.player) return false;
        return chessman == that.chessman;
    }

    @Override
    public int hashCode() {
        int result = col;
        result = 31 * result + row;
        result = 31 * result + player.hashCode();
        result = 31 * result + chessman.hashCode();
        result = 31 * result + resID;
        return result;
    }

    @Override
    public String toString() {
        return "ChessPiece{" +
                "col=" + col +
                ", row=" + row +
                ", player=" + player +
                ", chessman=" + chessman +
                ", resID=" + resID +
                '}';
    }

    public ChessPiece copy(int col, int row) {
        return new ChessPiece(col, row, this.player, this.chessman, this.resID);
    }



}

