package com.example.btl_chess;

import java.util.Set;

public interface ChessDelegate {
    ChessPiece pieceAt(Square square);
    void movePiece(Square from, Square to);
    Set<Square> getValidMoves(Square square);
}
