package com.example.btl_chess;

import android.util.Log;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;
import java.util.concurrent.Executors;

public class ChessServer {
    private static final String TAG = "ChessServer";
    private final int port;
    private ServerSocket serverSocket;
    private PrintWriter printWriter;

    public ChessServer(int port) {
        this.port = port;
    }

    public void startServer(ChessDelegate delegate) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try (ServerSocket srvSkt = new ServerSocket(50000)) {
                serverSocket = srvSkt;
                Log.d(TAG, "Server started, waiting for connection on port " + port);
                Socket socket = srvSkt.accept();
                Log.d(TAG, "Client connected!");
                receiveMove(socket, delegate);
            } catch (SocketException e) {
                Log.e(TAG, "Socket closed", e);
            } catch (IOException e) {
                Log.e(TAG, "Server socket error", e);
            }
        });
    }


    private void receiveMove(Socket socket, ChessDelegate delegate) {
        try {
            Scanner scanner = new Scanner(socket.getInputStream());
            printWriter = new PrintWriter(socket.getOutputStream(), true);
            while (scanner.hasNextLine()) {
                String[] move = scanner.nextLine().split(",");
                int fromCol = Integer.parseInt(move[0]);
                int fromRow = Integer.parseInt(move[1]);
                int toCol = Integer.parseInt(move[2]);
                int toRow = Integer.parseInt(move[3]);

                // Gọi hàm movePiece của ChessDelegate để di chuyển quân cờ
                delegate.movePiece(new Square(fromCol, fromRow), new Square(toCol, toRow));
            }
        } catch (IOException e) {
            Log.e(TAG, "Error receiving move", e);
        } finally {
            closeSocket(socket); // Đảm bảo socket được đóng sau khi sử dụng
        }
    }

    public void sendMove(String moveStr) {
        if (printWriter != null) {
            Executors.newSingleThreadExecutor().execute(() -> printWriter.println(moveStr));
        }
    }

    private void closeSocket(Socket socket) {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing socket", e);
            }
        }
    }

    public void closeServerSocket() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing server socket", e);
            }
            serverSocket = null;
        }
    }
}
