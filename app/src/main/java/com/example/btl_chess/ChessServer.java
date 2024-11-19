package com.example.btl_chess;//package com.example.btl_chess;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class ChessServer {
    private static final int PORT = 50001;
    private ClientHandler player1;
    private ClientHandler player2;
    private ServerSocket serverSocket;

    public ChessServer() {
        startServer();
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server đang lắng nghe tại cổng " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client mới kết nối: " + clientSocket.getInetAddress());

                if (player1 == null) {
                    player1 = new ClientHandler(clientSocket, "Player 1");
                    new Thread(player1).start();
                } else if (player2 == null) {
                    player2 = new ClientHandler(clientSocket, "Player 2");
                    new Thread(player2).start();
                } else {
                    // Từ chối kết nối thêm
                    try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
                        out.println("GAME_FULL");
                        clientSocket.close();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private final String playerName;
        private PrintWriter out;
        private Scanner in;

        public ClientHandler(Socket socket, String playerName) {
            this.socket = socket;
            this.playerName = playerName;
            try {
                this.out = new PrintWriter(socket.getOutputStream(), true);
                this.in = new Scanner(socket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }

        @Override
        public void run() {
            try {
                while (in.hasNextLine()) {
                    String input = in.nextLine();
                    System.out.println("Nhận được dữ liệu từ " + playerName + ": " + input);

                    // Xử lý tin nhắn chat và nước đi
                    if (input.startsWith("CHAT:")) {
                        // Chuyển tiếp tin nhắn chat
                        forwardMessage(input, this);
                    } else {
                        // Chuyển tiếp nước đi
                        forwardMove(input, this);
                    }
                }
            } finally {
                try {
                    socket.close();
                    handleDisconnect(this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void forwardMessage(String message, ClientHandler sender) {
        ClientHandler recipient = (sender == player1) ? player2 : player1;
        if (recipient != null) {
            recipient.sendMessage(message);
            System.out.println("Chuyển tiếp tin nhắn từ " + sender.playerName + " đến " + recipient.playerName);
        }
    }

    private void forwardMove(String move, ClientHandler sender) {
        ClientHandler recipient = (sender == player1) ? player2 : player1;
        if (recipient != null) {
            recipient.sendMessage(move);
            System.out.println("Chuyển tiếp nước đi từ " + sender.playerName + " đến " + recipient.playerName);
        }
    }

    private void handleDisconnect(ClientHandler disconnectedPlayer) {
        if (disconnectedPlayer == player1) {
            System.out.println("Player 1 đã ngắt kết nối");
            player1 = null;
            if (player2 != null) {
                player2.sendMessage("OPPONENT_DISCONNECTED");
            }
        } else if (disconnectedPlayer == player2) {
            System.out.println("Player 2 đã ngắt kết nối");
            player2 = null;
            if (player1 != null) {
                player1.sendMessage("OPPONENT_DISCONNECTED");
            }
        }
    }

    public static void main(String[] args) {
        new ChessServer();
    }
}