package com.example.btl_chess;
import java.util.HashSet;
import java.util.Set;
import static java.lang.Math.abs;

public class ChessGame {
    private static Set<ChessPiece> piecesBox;
    private static Set<Square> possibleMoves = new HashSet<>();

    public ChessGame() {
        piecesBox = new HashSet<>();
        reset();
    }
    public static Set<Square> getPossibleMoves(Square from) {
        possibleMoves.clear();
        ChessPiece piece = pieceAt(from);

        if (piece == null) return possibleMoves;

        // Kiểm tra tất cả các ô trên bàn cờ
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Square to = new Square(col, row);
                if (canMove(from, to)) {
                    possibleMoves.add(to);
                }
            }
        }

        return possibleMoves;
    }

    // Phương thức kiểm tra xem một ô có phải là nước đi hợp lệ không
    public static boolean isValidMove(Square square) {
        return possibleMoves.contains(square);
    }

    // Phương thức xóa gợi ý
    public static void clearPossibleMoves() {
        possibleMoves.clear();
    }

    public static void clear() {
        piecesBox.clear();
    }

    public static void addPiece(ChessPiece piece) {
        piecesBox.add(piece);
    }

    private static boolean canKnightMove(Square from, Square to) {
        return (abs(from.getCol() - to.getCol()) == 2 && abs(from.getRow() - to.getRow()) == 1) ||
                (abs(from.getCol() - to.getCol()) == 1 && abs(from.getRow() - to.getRow()) == 2);
    }

    private static boolean canRookMove(Square from, Square to) {
        return (from.getCol() == to.getCol() && isClearVerticallyBetween(from, to)) ||
                (from.getRow() == to.getRow() && isClearHorizontallyBetween(from, to));
    }

    private static boolean isClearVerticallyBetween(Square from, Square to) {
        if (from.getCol() != to.getCol()) return false;
        int gap = abs(from.getRow() - to.getRow()) - 1;
        if (gap == 0) return true;

        for (int i = 1; i <= gap; i++) {
            int nextRow = to.getRow() > from.getRow() ? from.getRow() + i : from.getRow() - i;
            if (pieceAt(new Square(from.getCol(), nextRow)) != null) {
                return false;
            }
        }
        return true;
    }

    private static boolean isClearHorizontallyBetween(Square from, Square to) {
        if (from.getRow() != to.getRow()) return false;
        int gap = abs(from.getCol() - to.getCol()) - 1;
        if (gap == 0) return true;

        for (int i = 1; i <= gap; i++) {
            int nextCol = to.getCol() > from.getCol() ? from.getCol() + i : from.getCol() - i;
            if (pieceAt(new Square(nextCol, from.getRow())) != null) {
                return false;
            }
        }
        return true;
    }

    private static boolean isClearDiagonally(Square from, Square to) {
        if (abs(from.getCol() - to.getCol()) != abs(from.getRow() - to.getRow())) return false;
        int gap = abs(from.getCol() - to.getCol()) - 1;

        for (int i = 1; i <= gap; i++) {
            int nextCol = to.getCol() > from.getCol() ? from.getCol() + i : from.getCol() - i;
            int nextRow = to.getRow() > from.getRow() ? from.getRow() + i : from.getRow() - i;
            if (pieceAt(nextCol, nextRow) != null) {
                return false;
            }
        }
        return true;
    }

    private static boolean canBishopMove(Square from, Square to) {
        return abs(from.getCol() - to.getCol()) == abs(from.getRow() - to.getRow()) &&
                isClearDiagonally(from, to);
    }

    private static boolean canQueenMove(Square from, Square to) {
        return canRookMove(from, to) || canBishopMove(from, to);
    }

    private static boolean canKingMove(Square from, Square to) {
        int deltaCol = abs(from.getCol() - to.getCol());
        int deltaRow = abs(from.getRow() - to.getRow());

        // Vua chỉ có thể di chuyển 1 ô theo mọi hướng
        return deltaCol <= 1 && deltaRow <= 1 && (deltaCol + deltaRow > 0);
    }

    private static boolean canPawnMove(Square from, Square to) {
        ChessPiece movingPiece = pieceAt(from);
        if (movingPiece == null) return false;

        int direction = (movingPiece.getPlayer() == Player.WHITE) ? 1 : -1;
        int startRow = (movingPiece.getPlayer() == Player.WHITE) ? 1 : 6;

        // Di chuyển thẳng
        if (from.getCol() == to.getCol()) {
            // Di chuyển 1 ô
            if (to.getRow() == from.getRow() + direction) {
                return pieceAt(to) == null;
            }
            // Di chuyển 2 ô từ vị trí khởi đầu
            if (from.getRow() == startRow && to.getRow() == from.getRow() + 2 * direction) {
                Square intermediate = new Square(from.getCol(), from.getRow() + direction);
                return pieceAt(to) == null && pieceAt(intermediate) == null;
            }
        }
        // Ăn chéo
        else if (abs(from.getCol() - to.getCol()) == 1 && to.getRow() == from.getRow() + direction) {
            ChessPiece targetPiece = pieceAt(to);
            return targetPiece != null && targetPiece.getPlayer() != movingPiece.getPlayer();
        }
        return false;
    }


    public static boolean canMove(Square from, Square to) {
        if (from.getCol() == to.getCol() && from.getRow() == to.getRow()) {
            return false;
        }

        ChessPiece movingPiece = pieceAt(from);
        ChessPiece targetPiece = pieceAt(to);

        // Kiểm tra có quân cờ để di chuyển không
        if (movingPiece == null) return false;

        // Kiểm tra nếu ô đích có quân cùng màu
        if (targetPiece != null && targetPiece.getPlayer() == movingPiece.getPlayer()) {
            return false;
        }

        switch (movingPiece.getChessman()) {
            case KNIGHT: return canKnightMove(from, to);
            case ROOK: return canRookMove(from, to);
            case BISHOP: return canBishopMove(from, to);
            case QUEEN: return canQueenMove(from, to);
            case KING: return canKingMove(from, to);
            case PAWN: return canPawnMove(from, to);
            default: return false;
        }
    }

    public static void movePiece(Square from, Square to) {
        if (canMove(from, to)) {
            movePiece(from.getCol(), from.getRow(), to.getCol(), to.getRow());
        }
    }

    private static void movePiece(int fromCol, int fromRow, int toCol, int toRow) {
        if (fromCol == toCol && fromRow == toRow) return;
        ChessPiece movingPiece = pieceAt(fromCol, fromRow);
        if (movingPiece == null) return;

        ChessPiece targetPiece = pieceAt(toCol, toRow);
        if (targetPiece != null) {
            if (targetPiece.getPlayer() == movingPiece.getPlayer()) {
                return;
            }
            piecesBox.remove(targetPiece);
        }

        piecesBox.remove(movingPiece);
        addPiece(new ChessPiece(toCol, toRow, movingPiece.getPlayer(),
                movingPiece.getChessman(), movingPiece.getResID()));
    }

    public static void reset() {
        clear();
        for (int i = 0; i < 2; i++) {
            addPiece(new ChessPiece(0 + i * 7, 0, Player.WHITE, Chessman.ROOK, R.drawable.rook_white));
            addPiece(new ChessPiece(0 + i * 7, 7, Player.BLACK, Chessman.ROOK, R.drawable.rook_black));

            addPiece(new ChessPiece(1 + i * 5, 0, Player.WHITE, Chessman.KNIGHT, R.drawable.knight_white));
            addPiece(new ChessPiece(1 + i * 5, 7, Player.BLACK, Chessman.KNIGHT, R.drawable.knight_black));

            addPiece(new ChessPiece(2 + i * 3, 0, Player.WHITE, Chessman.BISHOP, R.drawable.bishop_white));
            addPiece(new ChessPiece(2 + i * 3, 7, Player.BLACK, Chessman.BISHOP, R.drawable.bishop_black));
        }

        for (int i = 0; i < 8; i++) {
            addPiece(new ChessPiece(i, 1, Player.WHITE, Chessman.PAWN, R.drawable.pawn_white));
            addPiece(new ChessPiece(i, 6, Player.BLACK, Chessman.PAWN, R.drawable.pawn_black));
        }

        addPiece(new ChessPiece(3, 0, Player.WHITE, Chessman.QUEEN, R.drawable.queen_white));
        addPiece(new ChessPiece(3, 7, Player.BLACK, Chessman.QUEEN, R.drawable.queen_black));
        addPiece(new ChessPiece(4, 0, Player.WHITE, Chessman.KING, R.drawable.king_white));
        addPiece(new ChessPiece(4, 7, Player.BLACK, Chessman.KING, R.drawable.king_black));
    }

    public static ChessPiece pieceAt(Square square) {
        return pieceAt(square.getCol(), square.getRow());
    }

    private static ChessPiece pieceAt(int col, int row) {
        for (ChessPiece piece : piecesBox) {
            if (col == piece.getCol() && row == piece.getRow()) {
                return piece;
            }
        }
        return null;
    }

    public String pgnBoard() {
        StringBuilder desc = new StringBuilder(" \n");
        desc.append("  a b c d e f g h\n");
        for (int row = 7; row >= 0; row--) {
            desc.append(row + 1);
            desc.append(boardRow(row));
            desc.append(" ").append(row + 1);
            desc.append("\n");
        }
        desc.append("  a b c d e f g h");
        return desc.toString();
    }

    @Override
    public String toString() {
        StringBuilder desc = new StringBuilder(" \n");
        for (int row = 7; row >= 0; row--) {
            desc.append(row);
            desc.append(boardRow(row));
            desc.append("\n");
        }
        desc.append("  0 1 2 3 4 5 6 7");
        return desc.toString();
    }

    private String boardRow(int row) {
        StringBuilder desc = new StringBuilder();
        for (int col = 0; col < 8; col++) {
            desc.append(" ");
            ChessPiece piece = pieceAt(col, row);
            if (piece != null) {
                boolean white = piece.getPlayer() == Player.WHITE;
                switch (piece.getChessman()) {
                    case KING: desc.append(white ? "k" : "K"); break;
                    case QUEEN: desc.append(white ? "q" : "Q"); break;
                    case BISHOP: desc.append(white ? "b" : "B"); break;
                    case ROOK: desc.append(white ? "r" : "R"); break;
                    case KNIGHT: desc.append(white ? "n" : "N"); break;
                    case PAWN: desc.append(white ? "p" : "P"); break;
                }
            } else {
                desc.append(".");
            }
        }
        return desc.toString();
    }
}