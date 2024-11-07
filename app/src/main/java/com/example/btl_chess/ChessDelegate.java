package com.example.btl_chess;

public interface ChessDelegate {
    ChessPiece pieceAt(Square square);
    void movePiece(Square from, Square to);
}
