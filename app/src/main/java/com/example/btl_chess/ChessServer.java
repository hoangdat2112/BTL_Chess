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

        public void sendMove(String move) {
            out.println(move);
        }

        @Override
        public void run() {
            try {
                while (in.hasNextLine()) {
                    String move = in.nextLine();
                    System.out.println("Nhận được nước đi từ " + playerName + ": " + move);

                    // Chuyển tiếp nước đi đến người chơi kia
                    if (this == player1 && player2 != null) {
                        player2.sendMove(move);
                    } else if (this == player2 && player1 != null) {
                        player1.sendMove(move);
                    }
                }
            } finally {
                try {
                    socket.close();
                    if (this == player1) {
                        player1 = null;
                    } else if (this == player2) {
                        player2 = null;
                    }
                    System.out.println(playerName + " đã ngắt kết nối");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        new ChessServer();
    }
}