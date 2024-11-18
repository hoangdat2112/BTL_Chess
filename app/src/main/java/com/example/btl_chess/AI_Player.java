package com.example.btl_chess;


import static com.example.btl_chess.ChessGame_AI.*;
import static com.example.btl_chess.Chessman.KING;


import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class AI_Player extends AppCompatActivity implements ChessDelegate {
    private static final String TAG= "AI_Player";
    private ChessGame_AI chessGame;
    private ChessView_AI chessView;
    private Button resignButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ai_player);
        // Initialize game components
        chessGame = new ChessGame_AI();
        chessView = findViewById(R.id.chess_view_ai);
        chessView.setShowHints(true);
        chessView.setChessDelegate((ChessDelegate) this);
        initializeButtons();
    }
    private void initializeButtons() {
        resignButton = findViewById(R.id.btnResign);
        resignButton.setOnClickListener(v -> {
            chessGame.reset();
            chessView.invalidate();

        });
    }
    @Override
    public Set<Square> getValidMoves(Square square) {
        Set<Square> validMoves = new HashSet<>();
        ChessPiece piece = pieceAt(square);

        if (piece == null ) {
            return validMoves;
        }
        calculateValidMoves(square, piece, validMoves);
        filterCheckMoves(square, piece, validMoves);
        return validMoves;
    }

    private void calculateValidMoves(Square square, ChessPiece piece, Set<Square> validMoves) {
        switch (piece.getChessman()) {
            case PAWN:
                addPawnMoves(square, piece, validMoves);
                break;
            case KNIGHT:
                addKnightMoves(square, piece, validMoves);
                break;
            case BISHOP:
                addBishopMoves(square, piece, validMoves);
                break;
            case ROOK:
                addRookMoves(square, piece, validMoves);
                break;
            case QUEEN:
                addQueenMoves(square, piece, validMoves);
                break;
            case KING:
                addKingMoves(square, piece, validMoves);
                break;
        }
    }

    private void addPawnMoves(Square from, ChessPiece pawn, Set<Square> moves) {
        int direction = (pawn.getPlayer() == Player.WHITE) ? 1 : -1;
        int row = from.getRow();
        int col = from.getCol();

        // Forward moves
        Square oneStep = new Square(col, row + direction);
        if (isInBoard(oneStep) && pieceAt(oneStep) == null) {
            moves.add(oneStep);

            // Initial two-square move
            boolean isInitialPosition = (pawn.getPlayer() == Player.WHITE && row == 1) ||
                    (pawn.getPlayer() == Player.BLACK && row == 6);
            if (isInitialPosition) {
                Square twoStep = new Square(col, row + 2 * direction);
                if (pieceAt(twoStep) == null) {
                    moves.add(twoStep);
                }
            }
        }

        // Capture moves
        addPawnCaptureMoves(from, pawn, direction, moves);
    }

    private void addPawnCaptureMoves(Square from, ChessPiece pawn, int direction, Set<Square> moves) {
        int row = from.getRow();
        int col = from.getCol();

        Square[] captureSquares = {
                new Square(col - 1, row + direction),
                new Square(col + 1, row + direction)
        };

        for (Square captureSquare : captureSquares) {
            if (isInBoard(captureSquare)) {
                ChessPiece targetPiece = pieceAt(captureSquare);
                if (targetPiece != null && targetPiece.getPlayer() != pawn.getPlayer()) {
                    moves.add(captureSquare);
                }
            }
        }
    }

    private void addKnightMoves(Square from, ChessPiece knight, Set<Square> moves) {
        int[][] knightMoves = {
                {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2},
                {1, -2}, {1, 2}, {2, -1}, {2, 1}
        };

        for (int[] move : knightMoves) {
            Square to = new Square(from.getCol() + move[0], from.getRow() + move[1]);
            if (isValidMove(from, to, knight)) {
                moves.add(to);
            }
        }
    }

    private void addBishopMoves(Square from, ChessPiece bishop, Set<Square> moves) {
        int[][] directions = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        addSlidingMoves(from, bishop, moves, directions);
    }

    private void addRookMoves(Square from, ChessPiece rook, Set<Square> moves) {
        int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
        addSlidingMoves(from, rook, moves, directions);
    }

    private void addQueenMoves(Square from, ChessPiece queen, Set<Square> moves) {
        addBishopMoves(from, queen, moves);
        addRookMoves(from, queen, moves);
    }

    private void addKingMoves(Square from, ChessPiece king, Set<Square> moves) {
        int[][] directions = {
                {-1, -1}, {-1, 0}, {-1, 1},
                {0, -1},           {0, 1},
                {1, -1},  {1, 0},  {1, 1}
        };

        for (int[] dir : directions) {
            Square to = new Square(from.getCol() + dir[0], from.getRow() + dir[1]);
            if (isValidMove(from, to, king)) {
                moves.add(to);
            }
        }

        // TODO: Add castling logic here
    }

    private void addSlidingMoves(Square from, ChessPiece piece, Set<Square> moves, int[][] directions) {
        for (int[] dir : directions) {
            int col = from.getCol();
            int row = from.getRow();

            while (true) {
                col += dir[0];
                row += dir[1];
                Square to = new Square(col, row);

                if (!isInBoard(to)) break;

                ChessPiece targetPiece = pieceAt(to);
                if (targetPiece == null) {
                    moves.add(to);
                } else {
                    if (targetPiece.getPlayer() != piece.getPlayer()) {
                        moves.add(to);
                    }
                    break;
                }
            }
        }
    }

    private void filterCheckMoves(Square from, ChessPiece piece, Set<Square> moves) {
        Set<Square> illegalMoves = new HashSet<>();

        for (Square to : moves) {
            ChessPiece capturedPiece = pieceAt(to);
            movePieceWithoutNotifying(from, to);

            if (isKingInCheck(piece.getPlayer())) {
                illegalMoves.add(to);
            }

            // Restore the board state
            movePieceWithoutNotifying(to, from);
            if (capturedPiece != null) {
                setPieceAt(to, capturedPiece);
            }
        }

        moves.removeAll(illegalMoves);
    }

    private boolean isKingInCheck(Player player) {
        Square kingSquare = findKing(player);
        if (kingSquare == null) return false;

        // Check if any opponent piece can capture the king
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Square square = new Square(col, row);
                ChessPiece piece = pieceAt(square);

                if (piece != null && piece.getPlayer() != player) {
                    Set<Square> moves = new HashSet<>();
                    calculateValidMoves(square, piece, moves);
                    if (moves.contains(kingSquare)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private Square findKing(Player player) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Square square = new Square(col, row);
                ChessPiece piece = pieceAt(square);
                if (piece != null && piece.getChessman() == KING && piece.getPlayer() == player) {
                    return square;
                }
            }
        }
        return null;
    }

    private boolean isValidMove(Square from, Square to, ChessPiece piece) {
        return isInBoard(to) &&
                (pieceAt(to) == null || pieceAt(to).getPlayer() != piece.getPlayer());
    }

    private boolean isInBoard(Square square) {
        return square.getCol() >= 0 && square.getCol() < 8 &&
                square.getRow() >= 0 && square.getRow() < 8;
    }

    private void movePieceWithoutNotifying(Square from, Square to) {
        ChessPiece piece = pieceAt(from);
        setPieceAt(from, null);
        setPieceAt(to, piece);
    }


    @Override
    public ChessPiece pieceAt(Square square) {
        return chessGame.pieceAt(square);
    }

    private void setPieceAt(Square square, ChessPiece piece) {
        chessGame.setPieceAt(square, piece);
    }
    public Pair<ChessPiece,Pair<Square, Square>> AI_getBestMove_Easy() {
        Set<Square> allPossibleSquare = new HashSet<>();
        for(int i=0;i<8;i++){
            for(int j=0;j<8;j++){
                ChessPiece chessPiece= pieceAt(new Square(i,j));
                if(chessPiece ==null || chessPiece.getPlayer().isWhite())  continue;
                allPossibleSquare.add(new Square(i,j));
            }
        }
        // Chọn ngẫu nhiên một nước đi

        Square selectedSquare= new Square(1,0);
        ChessPiece chessPiece= pieceAt(selectedSquare);
        Square target= new Square(0,0);
      while (true) {
          Set<Square> allValidMove=  new HashSet<>();
          Random random = new Random();
          int index = random.nextInt(allPossibleSquare.size());
           selectedSquare = allPossibleSquare.toArray(new Square[0])[index];
           chessPiece = pieceAt(selectedSquare);
           allValidMove = getPossibleMoves(selectedSquare);
          for(Square it:allValidMove){
              Log.d("AI", it.toString()+" ");
          }
           if(allValidMove.size()==0) continue;
          index = random.nextInt(allValidMove.size());
           target= allValidMove.toArray(new Square[0])[index];
           break;
      }
         return new Pair<>(chessPiece, new Pair<>(selectedSquare, target));
    }

    @Override
    public void movePiece(Square from, Square to) {
       if(chessGame.getCurrentPlayer().isWhite()) {
           chessGame.movePiece(from, to);
           chessView.invalidate();
       }

      if(chessGame.getCurrentPlayer().isWhite()==false){

              // Thực hiện thuật toán Minimax trên luồng phụ
          Move bestMove = AIFunction.decideBestMove(2, Player.BLACK);
          Log.d("AI", bestMove.from+" "+bestMove.to);
              // Cập nhật UI trên Main Thread
          ChessGame_AI.movePiece(bestMove.from, bestMove.to);
          chessView.invalidate();
       }
    }
}