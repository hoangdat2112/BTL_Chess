package com.example.btl_chess;

import java.util.Objects;

public class Square {
    private final int col;
    private final int row;

    public Square(int col, int row) {
        this.col = col;
        this.row = row;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Square square = (Square) o;
        return col == square.col && row == square.row;
    }

    @Override
    public int hashCode() {
        return Objects.hash(col, row);
    }

    @Override
    public String toString() {
        return "Square{" +
                "col=" + col +
                ", row=" + row +
                '}';
    }
}
