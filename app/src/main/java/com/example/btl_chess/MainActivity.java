package com.example.btl_chess;

import static com.example.btl_chess.Chessman.*;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.Scanner;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements ChessDelegate {
    private static final String TAG = "MainActivity";
    private static final String SOCKET_HOST = "10.0.2.2";
    private static final int SOCKET_PORT = 50001;

    private ChessGame chessGame;
    private ChessView chessView;
    private Button resetButton;
    private Button connectButton;
    private PrintWriter printWriter = null;
    private ServerSocket serverSocket = null;
    private final boolean isEmulator = Build.FINGERPRINT.contains("generic");
    private LinearLayout chatLayout;
    private TextView chatMessages;
    private EditText messageInput;
    private StringBuilder chatHistory;
    private Button chatButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize game components
        chessGame = new ChessGame();
        chessView = findViewById(R.id.chess_view);
        chessView.setShowHints(true);
        chessView.setChessDelegate(this);
        // Initialize chat components
        chatLayout = findViewById(R.id.chat_layout);
        chatMessages = findViewById(R.id.chat_messages);
        messageInput = findViewById(R.id.message_input);
        chatHistory = new StringBuilder();

        initializeButtons();
    }

    private void initializeButtons() {
        resetButton = findViewById(R.id.reset_button);
        connectButton = findViewById(R.id.connect_button);
        Button sendButton = findViewById(R.id.send_button);

        resetButton.setOnClickListener(v -> {
            chessGame.reset();
            chessView.invalidate();
            closeServerSocket();
        });

        connectButton.setOnClickListener(v -> {
            Log.d(TAG, "Socket client connecting...");
            Executors.newSingleThreadExecutor().execute(() -> connectToServer());
        });

        sendButton.setOnClickListener(v -> sendMessage());

        // Set up message input enter key listener
        messageInput.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }
    private void connectToServer() {
        try {
            Socket socket = new Socket(SOCKET_HOST, SOCKET_PORT);
            receiveData(socket);
            runOnUiThread(() -> {
                Toast.makeText(this, "Connected successfully!", Toast.LENGTH_SHORT).show();
                connectButton.setEnabled(false);
                appendMessage("System: Connected to opponent");
            });
        } catch (ConnectException e) {
            runOnUiThread(() -> {
                Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show();
                connectButton.setEnabled(true);
            });
        } catch (IOException e) {
            e.printStackTrace();
            runOnUiThread(() -> connectButton.setEnabled(true));
        }
    }
    private void receiveData(Socket socket) {
        try {
            Scanner scanner = new Scanner(socket.getInputStream());
            printWriter = new PrintWriter(socket.getOutputStream(), true);

            while (scanner.hasNextLine()) {
                String data = scanner.nextLine();
                if (data.startsWith("CHAT:")) {
                    // Handle chat message
                    String message = data.substring(5); // Remove "CHAT:" prefix
                    appendMessage("Opponent: " + message);
                } else {
                    // Handle chess move
                    String[] move = data.split(",");
                    int fromCol = Integer.parseInt(move[0]);
                    int fromRow = Integer.parseInt(move[1]);
                    int toCol = Integer.parseInt(move[2]);
                    int toRow = Integer.parseInt(move[3]);

                    runOnUiThread(() -> {
                        movePiece(new Square(fromCol, fromRow), new Square(toCol, toRow));
                    });
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                appendMessage("System: Connection lost");
                connectButton.setEnabled(true);
            });
        }
    }
    private void toggleChat() {
        if (chatLayout.getVisibility() == View.VISIBLE) {
            chatLayout.setVisibility(View.GONE);
        } else {
            chatLayout.setVisibility(View.VISIBLE);
        }
    }

    private void sendMessage() {
        String message = messageInput.getText().toString().trim();
        if (!message.isEmpty() && printWriter != null) {
            final String finalMessage = message;
            Executors.newSingleThreadExecutor().execute(() -> {
                // Send message in background thread
                printWriter.println("CHAT:" + finalMessage);
                runOnUiThread(() -> {
                    appendMessage("You: " + finalMessage);
                    messageInput.setText("");
                });
            });
        } else if (printWriter == null) {
            Toast.makeText(this, "Please connect first", Toast.LENGTH_SHORT).show();
        }
    }
    private void appendMessage(String message) {
        runOnUiThread(() -> {
            chatHistory.append(message).append("\n");
            chatMessages.setText(chatHistory.toString());
            // Auto scroll to bottom
            chatMessages.post(() -> {
                final int scrollAmount = chatMessages.getLayout().getLineTop(chatMessages.getLineCount()) - chatMessages.getHeight();
                if (scrollAmount > 0)
                    chatMessages.scrollTo(0, scrollAmount);
            });
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

    private void receiveMove(Socket socket) {
        try {
            Scanner scanner = new Scanner(socket.getInputStream());
            printWriter = new PrintWriter(socket.getOutputStream(), true);

            while (scanner.hasNextLine()) {
                String[] move = scanner.nextLine().split(",");
                int fromCol = Integer.parseInt(move[0]);
                int fromRow = Integer.parseInt(move[1]);
                int toCol = Integer.parseInt(move[2]);
                int toRow = Integer.parseInt(move[3]);

                runOnUiThread(() -> {
                    movePiece(new Square(fromCol, fromRow), new Square(toCol, toRow));
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ChessPiece pieceAt(Square square) {
        return chessGame.pieceAt(square);
    }

    private void setPieceAt(Square square, ChessPiece piece) {
        chessGame.setPieceAt(square, piece);
    }

    @Override
    public void movePiece(Square from, Square to) {
        chessGame.movePiece(from, to);
        chessView.invalidate();

        if (printWriter != null) {
            String moveStr = from.getCol() + "," + from.getRow() + "," +
                    to.getCol() + "," + to.getRow();
            Executors.newSingleThreadExecutor().execute(() -> printWriter.println(moveStr));
        }
    }


    private void closeServerSocket() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            serverSocket = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeServerSocket();
    }
}