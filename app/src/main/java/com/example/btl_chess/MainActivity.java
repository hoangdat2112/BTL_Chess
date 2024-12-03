package com.example.btl_chess;

import static com.example.btl_chess.Chessman.*;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;

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
    private static final int SOCKET_PORT = 50002;

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
    // Thêm các trường để quản lý màu người chơi
    private Player currentPlayerColor = Player.BLACK;
    private boolean isFirstPlayerConnected = false;
    private boolean isSecondPlayerConnected = false;
    private ExoPlayer mediaPlayer;
    private ImageButton soundButton;
    private boolean isMusicPlaying = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Lấy Room ID từ Intent
        String roomId = getIntent().getStringExtra("ROOM_ID");

        // Hiển thị Room ID
        TextView roomIdDisplay = findViewById(R.id.room_id_display);
        if (roomId != null) {
            roomIdDisplay.setText("Room ID: " + roomId);
        } else {
            roomIdDisplay.setText("No Room ID");
        }
        // Khởi tạo ExoPlayer
        mediaPlayer = new ExoPlayer.Builder(this).build();

        // Ánh xạ nút âm thanh
        soundButton = findViewById(R.id.sound_button);

        // Sự kiện nhấn nút âm thanh
        soundButton.setOnClickListener(v -> toggleMusic());

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
        ImageButton settingsButton = findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(v -> {
            ChessView chessView = findViewById(R.id.chess_view);
            showColorSchemeDialog(chessView);
        });


        initializeButtons();
    }

    private void initializeButtons() {
//        resetButton = findViewById(R.id.reset_button);
        connectButton = findViewById(R.id.connect_button);
        Button sendButton = findViewById(R.id.send_button);

//        resetButton.setOnClickListener(v -> {
//            chessGame.reset();
//            chessView.invalidate();
//            closeServerSocket();
//        });

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
    private void showColorSchemeDialog(ChessView chessView) {
        String[] colorNames = chessView.getColorSchemeNames();

        new AlertDialog.Builder(this)
                .setTitle("Choose Chessboard Color")
                .setSingleChoiceItems(colorNames, chessView.getCurrentColorScheme(), (dialog, which) -> {
                    chessView.changeColorScheme(which);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void toggleMusic() {
        if (isMusicPlaying) {
            pauseMusic();
        } else {
            playMusic();
        }
    }

    private void playMusic() {
        // Thay đường dẫn bằng file nhạc của bạn trong thư mục raw
        MediaItem mediaItem = MediaItem.fromUri("android.resource://" + getPackageName() + "/raw/adventure");

        mediaPlayer.setMediaItem(mediaItem);
        mediaPlayer.prepare();
        mediaPlayer.play();

        isMusicPlaying = true;
        updateSoundButtonState();
    }

    private void pauseMusic() {
        mediaPlayer.pause();
        isMusicPlaying = false;
        updateSoundButtonState();
    }

    private void updateSoundButtonState() {
        // Thay đổi màu hoặc icon dựa trên trạng thái phát nhạc
        soundButton.setColorFilter(isMusicPlaying ? Color.GREEN : Color.GRAY);
    }

    private void connectToServer() {
        try {
            Socket socket = new Socket(SOCKET_HOST, SOCKET_PORT);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            Log.d(TAG, "Attempting connection. isFirstPlayerConnected: " + isFirstPlayerConnected);


            // Gửi yêu cầu về màu của người chơi
            if (!isFirstPlayerConnected) {
                out.println("FIRST_PLAYER_WHITE");
                currentPlayerColor = Player.WHITE;
                isFirstPlayerConnected = true;
                Log.d(TAG, "Sent FIRST_PLAYER_WHITE");
            } else {
                out.println("SECOND_PLAYER_BLACK");
                currentPlayerColor = Player.BLACK;
                isSecondPlayerConnected = true;
                Log.d(TAG, "Sent SECOND_PLAYER_BLACK");
            }

            // Nhận và xử lý dữ liệu từ server
            receiveData(socket);

            runOnUiThread(() -> {
                Toast.makeText(this, "Connected as " + currentPlayerColor + " player!", Toast.LENGTH_SHORT).show();
                connectButton.setEnabled(false);

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
                // Log toàn bộ dữ liệu nhận được để debug
                Log.d(TAG, "Received data: " + data);
                // Handle different types of messages
                if (data.startsWith("CHAT:")) {
                    // Chat message
                    String message = data.substring(5);
                    appendMessage("Opponent: " + message);
                } else if (data.equals("WHITE")) {
                    Log.d(TAG, "Setting color to WHITE");

                    // White player color assignment
                    runOnUiThread(() -> {
                        currentPlayerColor = Player.WHITE;
                        swapPlayerColors();
                    });
                }
                else if (data.equals("BLACK")) {
                    Log.d(TAG, "Setting color to BLACK");

                    runOnUiThread(() -> {
                        currentPlayerColor = Player.BLACK;
                    });
                } else if (data.equals("OPPONENT_CONNECTED")) {
                    // Opponent connection notification
                    runOnUiThread(() -> {
                        appendMessage("System: Opponent connected");
                        isSecondPlayerConnected = true;
                        swapPlayerColors(); // Thêm dòng này để swap colors
                    });
                } else if (data.equals("OPPONENT_DISCONNECTED")) {
                    // Opponent disconnection notification
                    runOnUiThread(() -> {
                        appendMessage("System: Opponent disconnected");
                        isSecondPlayerConnected = false;
                        connectButton.setEnabled(true);
                    });
                } else {
                    // Try to parse as a move
                    try {
                        String[] move = data.split(",");
                        if (move.length == 4) {
                            int fromCol = Integer.parseInt(move[0]);
                            int fromRow = Integer.parseInt(move[1]);
                            int toCol = Integer.parseInt(move[2]);
                            int toRow = Integer.parseInt(move[3]);

                            runOnUiThread(() -> {
                                movePiece(new Square(fromCol, fromRow), new Square(toCol, toRow));
                            });
                        }
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Invalid move format: " + data);
                    }
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
    // Thêm phương thức để kiểm tra màu người chơi hiện tại
    private boolean isCurrentPlayersTurn() {
        return currentPlayerColor == chessGame.getCurrentPlayer();
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
    private void updatePlayerColorUI() {
        // Cập nhật giao diện khi thay đổi màu
        runOnUiThread(() -> {
            Toast.makeText(this, "You are playing as " + currentPlayerColor, Toast.LENGTH_SHORT).show();
            appendMessage("System: You are playing as " + currentPlayerColor + " pieces");

            // Vô hiệu hóa nút kết nối
            connectButton.setEnabled(false);

            // Cập nhật trạng thái view nếu cần
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
    private void swapPlayerColors() {
        // Swap the current player color when the second player connects
        if (isFirstPlayerConnected && isSecondPlayerConnected) {
            if (currentPlayerColor == Player.WHITE) {
                currentPlayerColor = Player.BLACK;
            } else {
                currentPlayerColor = Player.WHITE;
            }

            // Cập nhật UI để phản ánh màu mới
            runOnUiThread(() -> {
                Toast.makeText(this, "Player color changed to " + currentPlayerColor, Toast.LENGTH_SHORT).show();

                // Nếu bạn có thêm logic để cập nhật giao diện theo màu người chơi
                // Thực hiện ở đây
                chessView.invalidate(); // Vẽ lại bàn cờ nếu cần
            });
        }
    }
    private boolean isMoving = false;
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
    private void handleWaitingPlayersRequest(String roomId) {
        // Hiển thị danh sách người chơi đang chờ
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Người chơi đang chờ");

        // Tạo danh sách người chơi (giả định)
        String[] waitingPlayers = {"Người chơi 1", "Người chơi 2"};

        builder.setItems(waitingPlayers, (dialog, which) -> {
            String selectedPlayer = waitingPlayers[which];

            // Hiển thị dialog xác nhận
            new AlertDialog.Builder(this)
                    .setTitle("Phê duyệt người chơi")
                    .setMessage("Bạn có muốn cho " + selectedPlayer + " vào phòng không?")
                    .setPositiveButton("Đồng ý", (d, w) -> {
                        approvePlayer(roomId, selectedPlayer);
                    })
                    .setNegativeButton("Từ chối", (d, w) -> {
                        rejectPlayer(roomId, selectedPlayer);
                    })
                    .show();
        });

        builder.show();
    }

    private void approvePlayer(String roomId, String playerName) {
        // Gửi thông điệp phê duyệt đến server
        if (printWriter != null) {
            printWriter.println("APPROVE_PLAYER:" + roomId + ":" + playerName);
            Toast.makeText(this, "Đã phê duyệt " + playerName, Toast.LENGTH_SHORT).show();
        }
    }

    private void rejectPlayer(String roomId, String playerName) {
        // Gửi thông điệp từ chối đến server
        if (printWriter != null) {
            printWriter.println("REJECT_PLAYER:" + roomId + ":" + playerName);
            Toast.makeText(this, "Đã từ chối " + playerName, Toast.LENGTH_SHORT).show();
        }
    }

    // Thêm vào phương thức xử lý tin nhắn từ server
    private void processServerMessage(String message) {
        // Các xử lý tin nhắn server khác...

        if (message.startsWith("PLAYER_WAITING:")) {
            // Thông báo có người chơi đang chờ
            String[] parts = message.split(":");
            String playerName = parts[1];
            String roomId = parts[2];

            // Hiển thị thông báo cho host
            new AlertDialog.Builder(this)
                    .setTitle("Yêu cầu vào phòng")
                    .setMessage(playerName + " muốn vào phòng")
                    .setPositiveButton("Quản lý", (dialog, which) -> {
                        handleWaitingPlayersRequest(roomId);
                    })
                    .setNegativeButton("Bỏ qua", null)
                    .show();
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