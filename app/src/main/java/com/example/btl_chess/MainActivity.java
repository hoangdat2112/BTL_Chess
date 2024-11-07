package com.example.btl_chess;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements ChessDelegate {
    private static final String TAG = "MainActivity";
    private final String socketHost = "10.0.2.2"; // Địa chỉ IP cho emulator
    private final int socketGuestPort = 50000; // Cổng cho socket client
    private ChessView chessView;
    private ChessServer chessServer; // Khai báo ChessServer instance
    private ChessGame chessGame;
    private PrintWriter printWriter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chessGame = new ChessGame();
        chessView = findViewById(R.id.chess_view);
        Button resetButton = findViewById(R.id.reset_button);
        Button listenButton = findViewById(R.id.listen_button);
        Button connectButton = findViewById(R.id.connect_button);
        chessView.setChessDelegate(this);

        resetButton.setOnClickListener(v -> resetGame());
//        listenButton.setOnClickListener(v -> startServer());
        connectButton.setOnClickListener(v -> connectClient());
    }

    private void resetGame() {
        chessGame.reset();
        chessView.invalidate();
        if (chessServer != null) {
            chessServer.closeServerSocket(); // Đóng server socket
        }
    }

//    private void startServer() {
//        Toast.makeText(this, "Listening on port " + socketGuestPort, Toast.LENGTH_SHORT).show();
//        chessServer = new ChessServer(socketGuestPort, this);
//        chessServer.startServer();
//    }

    private void connectClient() {
        Log.d(TAG, "Socket client connecting ...");
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Log.d(TAG, "Connecting to " + socketHost + " on port " + socketGuestPort);
                Socket socket = new Socket(socketHost, socketGuestPort);
                receiveMove(socket);
                Log.d(TAG, "Connected to server!");
            } catch (ConnectException e) {
                Log.d(TAG, "Connection failed ...");
                runOnUiThread(() -> Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show());
            } catch (IOException e) {
                Log.e(TAG, "Socket client error", e);
            }
        });
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
                    chessGame.movePiece(new Square(fromCol, fromRow), new Square(toCol, toRow));
                    chessView.invalidate();
                });
            }
        } catch (IOException e) {
            Log.e(TAG, "Error receiving move from server", e);
        }
    }

    @Override
    public ChessPiece pieceAt(Square square) {
        return chessGame.pieceAt(square);
    }

    @Override
    public void movePiece(Square from, Square to) {
        chessGame.movePiece(from, to);
        chessView.invalidate();

        // Gửi nước đi tới client qua ChessServer
        String moveStr = from.getCol() + "," + from.getRow() + "," + to.getCol() + "," + to.getRow();
        if (chessServer != null) {
            chessServer.sendMove(moveStr);
        }
    }
}

//class ChessServer {
//    private static final String TAG = "ChessServer";
//    private final int port;
//    private ServerSocket serverSocket;
//    private PrintWriter printWriter;
//    private final ChessDelegate delegate;
//
//    public ChessServer(int port, ChessDelegate delegate) {
//        this.port = port;
//        this.delegate = delegate;
//    }
//
//    public void startServer() {
//        Executors.newSingleThreadExecutor().execute(() -> {
//            try {
//                serverSocket = new ServerSocket(port);
//                Log.d(TAG, "Server started, waiting for connection on port " + port);
//                Socket socket = serverSocket.accept();
//                Log.d(TAG, "Client connected!");
//                receiveMove(socket);
//            } catch (IOException e) {
//                Log.e(TAG, "Server socket error", e);
//            }
//        });
//    }
//
//    private void receiveMove(Socket socket) {
//        try {
//            Scanner scanner = new Scanner(socket.getInputStream());
//            printWriter = new PrintWriter(socket.getOutputStream(), true);
//            while (scanner.hasNextLine()) {
//                String[] move = scanner.nextLine().split(",");
//                int fromCol = Integer.parseInt(move[0]);
//                int fromRow = Integer.parseInt(move[1]);
//                int toCol = Integer.parseInt(move[2]);
//                int toRow = Integer.parseInt(move[3]);
//                delegate.movePiece(new Square(fromCol, fromRow), new Square(toCol, toRow));
//            }
//        } catch (IOException e) {
//            Log.e(TAG, "Error receiving move", e);
//        } finally {
//            closeSocket(socket); // Đảm bảo socket được đóng sau khi sử dụng
//        }
//    }
//
//    public void sendMove(String moveStr) {
//        if (printWriter != null) {
//            Executors.newSingleThreadExecutor().execute(() -> printWriter.println(moveStr));
//        }
//    }
//
//    private void closeSocket(Socket socket) {
//        if (socket != null && !socket.isClosed()) {
//            try {
//                socket.close();
//            } catch (IOException e) {
//                Log.e(TAG, "Error closing socket", e);
//            }
//        }
//    }
//
//    public void closeServerSocket() {
//        if (serverSocket != null) {
//            try {
//                serverSocket.close();
//            } catch (IOException e) {
//                Log.e(TAG, "Error closing server socket", e);
//            }
//            serverSocket = null;
//        }
//    }
