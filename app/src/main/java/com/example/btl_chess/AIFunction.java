package com.example.btl_chess;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AIFunction {

        // Quyết định nước đi tốt nhất cho AI
        public static Move decideBestMove(int depth, Player currentPlayer) {
            int bestValue = Integer.MIN_VALUE;
            Move bestMove = null;

            // Lấy tất cả các nước đi hợp lệ
            for (Move move : getAllPossibleMoves(currentPlayer)) {
                // Thực hiện thử nước đi
                List<ChessPiece> currentState=saveCurrentState();
                ChessGame_AI.movePiece(move.from,move.to);
                // Gọi Minimax với Alpha-Beta Pruning
                int moveValue = minimax(depth - 1, false, Integer.MIN_VALUE, Integer.MAX_VALUE, getOpponent(currentPlayer));

                // Hoàn tác nước đi
               restoreState(currentState);

                // Lưu nước đi tốt nhất
                if (moveValue > bestValue) {
                    bestValue = moveValue;
                    bestMove = move;
                }
            }

            return bestMove; // Trả về nước đi tốt nhất
        }

        // Thuật toán Minimax kết hợp Alpha-Beta Pruning
        public static int minimax(int depth, boolean maximizingPlayer, int alpha, int beta, Player currentPlayer) {
            // Điều kiện dừng: Đạt đến độ sâu tối đa hoặc trò chơi kết thúc
            if (depth == 0 || isGameOver()) {
                return evaluateBoard();
            }

            if (maximizingPlayer) { // Lượt AI (tối đa hóa điểm)
                int maxEval = Integer.MIN_VALUE;

                for (Move move : getAllPossibleMoves(currentPlayer)) {
                    List<ChessPiece> currentState = saveCurrentState();
                    ChessGame_AI.movePiece(move.from, move.to);
                    int eval = minimax(depth - 1, false, alpha, beta, getOpponent(currentPlayer)); // Gọi đệ quy
                   restoreState(currentState); // Hoàn tác nước đi

                    maxEval = Math.max(maxEval, eval);
                    alpha = Math.max(alpha, eval); // Cập nhật alpha
                    if (beta <= alpha) break; // Cắt tỉa
                }

                return maxEval;

            } else { // Lượt đối thủ (tối thiểu hóa điểm)
                int minEval = Integer.MAX_VALUE;

                for (Move move : getAllPossibleMoves(currentPlayer)) {
                    List<ChessPiece> currentState = saveCurrentState();
                    ChessGame_AI.movePiece(move.from, move.to);
                    int eval = minimax(depth - 1, true, alpha, beta, getOpponent(currentPlayer));
                    restoreState(currentState);

                    minEval = Math.min(minEval, eval);
                    beta = Math.min(beta, eval); // Cập nhật beta
                    if (beta <= alpha) break; // Cắt tỉa
                }

                return minEval;
            }
        }

        // Hàm đánh giá bàn cờ
        public static int evaluateBoard() {
            int score = 0;

            for (ChessPiece piece : ChessGame_AI.getPiecesBox()) {
                int pieceValue = getPieceValue(piece.getChessman());

                if (piece.getPlayer() == ChessGame_AI.getCurrentPlayer()) {
                    score += pieceValue; // Cộng điểm nếu là quân của AI
                } else {
                    score -= pieceValue; // Trừ điểm nếu là quân của đối thủ
                }
            }

            return score;
        }

        // Trả về giá trị của từng quân cờ
        private static int getPieceValue(Chessman chessman) {
            switch (chessman) {
                case KING: return 1000;
                case QUEEN: return 9;
                case ROOK: return 5;
                case BISHOP:
                case KNIGHT: return 3;
                case PAWN: return 1;
                default: return 0;
            }
        }

        // Lấy tất cả các nước đi hợp lệ
        public static List<Move> getAllPossibleMoves(Player player) {
            List<Move> moves = new ArrayList<>();

            for (ChessPiece piece : new HashSet<>(ChessGame_AI.getPiecesBox())) {
                if (piece.getPlayer() == player) {
                    Square from = new Square(piece.getCol(), piece.getRow());
                    Set<Square> possibleMoves = ChessGame_AI.getPossibleMoves(from);

                    for (Square to : possibleMoves) {
                        moves.add(new Move(from, to));
                    }
                }

            }

            return moves;
        }

        // Thực hiện một nước đi
        public static void makeMove(Move move) {
            ChessGame_AI.movePiece(move.from, move.to);
        }

        // Hoàn tác một nước đi
        public static void undoMove(Move move) {
            ChessGame_AI.movePiece(move.to, move.from);

        }

        // Trả về đối thủ của một người chơi
        public static Player getOpponent(Player player) {
            return (player == Player.WHITE) ? Player.BLACK : Player.WHITE;
        }

        // Kiểm tra xem trò chơi đã kết thúc chưa
        public static boolean isGameOver() {
            // Kiểm tra "chiếu hết" hoặc "hòa cờ"
            return ChessGame_AI.isKingInCheck(ChessGame_AI.getCurrentPlayer()) &&
                    ChessGame_AI.getPossibleMoves(new Square(0, 0)).isEmpty();
        }
    private static List<ChessPiece> saveCurrentState() {
        // Tạo bản sao của tất cả các quân cờ trên bàn cờ
        List<ChessPiece> state = new ArrayList<>();
        for (ChessPiece piece : ChessGame_AI.getPiecesBox()) {
            state.add(new ChessPiece(piece.getCol(), piece.getRow(), piece.getPlayer(), piece.getChessman(), piece.getResID()));
        }
        return state;
    }
    private static void restoreState(List<ChessPiece> state) {
        ChessGame_AI.clear(); // Xóa tất cả quân cờ trên bàn cờ hiện tại
        for (ChessPiece piece : state) {
            ChessGame_AI.addPiece(piece); // Thêm lại các quân cờ từ bản sao
        }
    }
    }

    // Lớp Move để đại diện cho một nước đi
    class Move {
        public Square from;
        public Square to;

        public Move(Square from, Square to) {
            this.from = from;
            this.to = to;
        }
}
