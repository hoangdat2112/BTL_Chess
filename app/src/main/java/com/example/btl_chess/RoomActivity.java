package com.example.btl_chess;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class RoomActivity extends AppCompatActivity {
    private EditText roomNameInput;
    private EditText roomSearchInput;
    private Button createRoomButton;
    private RecyclerView roomListRecyclerView;
    private RoomAdapter roomAdapter;
    private List<Room> roomList = new ArrayList<>();
    private List<Room> filteredRoomList = new ArrayList<>();
    private Socket socket;
    private PrintWriter printWriter;
    private BufferedReader bufferedReader;
    private static final String SOCKET_HOST = "10.0.2.2";
    private static final int SOCKET_PORT = 50001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);

        // Ánh xạ view
        roomNameInput = findViewById(R.id.room_name_input);
        roomSearchInput = findViewById(R.id.room_search_input);
        createRoomButton = findViewById(R.id.create_room_button);
        roomListRecyclerView = findViewById(R.id.room_list_recycler_view);

        // Thiết lập RecyclerView
        roomAdapter = new RoomAdapter(filteredRoomList, this::onRoomSelected);
        roomListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        roomListRecyclerView.setAdapter(roomAdapter);

        // Kết nối socket
        connectToServer();

        // Sự kiện tạo phòng
        createRoomButton.setOnClickListener(v -> {
            String roomName = roomNameInput.getText().toString().trim();
            if (!roomName.isEmpty()) {
                createRoom(roomName);
            } else {
                Toast.makeText(this, "Vui lòng nhập tên phòng", Toast.LENGTH_SHORT).show();
            }
        });

        // Sự kiện tìm kiếm phòng
        roomSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterRooms(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        roomAdapter = new RoomAdapter(filteredRoomList, this::onRoomSelected);

    }

    private void connectToServer() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                socket = new Socket(SOCKET_HOST, SOCKET_PORT);
                printWriter = new PrintWriter(socket.getOutputStream(), true);
                bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Lắng nghe phản hồi từ server
                listenToServerResponses();

                // Yêu cầu danh sách phòng
                printWriter.println("LIST_ROOMS");
            } catch (IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Lỗi kết nối máy chủ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("RoomActivity", "Server connection error", e);
                });
            }
        });
    }

//    private void listenToServerResponses() {
//        Executors.newSingleThreadExecutor().execute(() -> {
//            try {
//                String line;
//                while ((line = bufferedReader.readLine()) != null) {
//                    final String response = line;
//                    runOnUiThread(() -> {
//                        // Xử lý phản hồi từ server
//                        if (response.startsWith("ROOM_LIST:")) {
//                            updateRoomList(response);
//                        } else if (response.startsWith("ROOM_CREATED:")) {
//                            String[] parts = response.split(":");
//                            if (parts.length > 1) {
//                                String roomId = parts[1];
//                                addNewRoom(roomId);
//                            }
//                        } else if (response.startsWith("ERROR:")) {
//                            handleServerError(response);
//                        }
//                    });
//                }
//            } catch (IOException e) {
//                runOnUiThread(() -> {
//                    Toast.makeText(this, "Mất kết nối với máy chủ", Toast.LENGTH_SHORT).show();
//                    Log.e("RoomActivity", "Server response listener error", e);
//                    reconnectToServer();
//                });
//            }
//        });
//    }

    private void handleServerError(String errorResponse) {
        Toast.makeText(this, errorResponse.substring(6), Toast.LENGTH_SHORT).show();
        Log.e("RoomActivity", "Server error: " + errorResponse);
    }

    private void reconnectToServer() {
        // Thử kết nối lại với server sau khi mất kết nối
        try {
            if (socket != null) {
                socket.close();
            }
            connectToServer();
        } catch (IOException e) {
            Log.e("RoomActivity", "Reconnection error", e);
        }
    }

    private void filterRooms(String searchText) {
        // Lọc danh sách phòng theo ID hoặc tên
        filteredRoomList.clear();
        if (searchText.isEmpty()) {
            filteredRoomList.addAll(roomList);
        } else {
            filteredRoomList.addAll(
                    roomList.stream()
                            .filter(room ->
                                    room.getId().toLowerCase().contains(searchText.toLowerCase()) ||
                                            room.getName().toLowerCase().contains(searchText.toLowerCase())
                            )
                            .collect(Collectors.toList())
            );
        }
        roomAdapter.notifyDataSetChanged();
    }

    private void createRoom(String roomName) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Kiểm tra kết nối socket
                if (socket == null || socket.isClosed()) {
                    connectToServer(); // Đảm bảo socket đã kết nối
                    Thread.sleep(500); // Chờ kết nối
                }

                // Gửi yêu cầu tạo phòng trong luồng riêng
                if (printWriter != null) {
                    printWriter.println("CREATE_ROOM:" + roomName);

                    // Cập nhật UI trên main thread
                    runOnUiThread(() -> {
                        roomNameInput.setText("");
                        Toast.makeText(this, "Đang tạo phòng...", Toast.LENGTH_SHORT).show();
                    });
                }

            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("RoomActivity", "Room creation error", e);
                });
            }
        });
    }

    private void updateRoomList(String response) {
        try {
            if (!response.startsWith("ROOM_LIST:") || response.length() <= 10) {
                return;
            }

            String roomListString = response.substring(10);

            if (roomListString.isEmpty()) {
                roomList.clear();
                filteredRoomList.clear();
                roomAdapter.notifyDataSetChanged();
                return;
            }

            String[] rooms = roomListString.split(",");
            roomList.clear();

            for (String roomData : rooms) {
                String[] details = roomData.split("\\|");
                if (details.length >= 2) {
                    roomList.add(new Room(details[0], details[1]));
                } else {
                    Log.w("RoomActivity", "Invalid room data: " + roomData);
                }
            }

            // Áp dụng bộ lọc hiện tại (nếu có)
            filterRooms(roomSearchInput.getText().toString());
        } catch (Exception e) {
            Log.e("RoomActivity", "Error updating room list", e);
            Toast.makeText(this, "Lỗi cập nhật danh sách phòng", Toast.LENGTH_SHORT).show();
        }
    }

    private void addNewRoom(String roomId) {
        // Thêm phòng mới vào danh sách
        Room newRoom = new Room(roomId, roomNameInput.getText().toString());
        roomList.add(newRoom);
        filterRooms(roomSearchInput.getText().toString());
    }

    private void onRoomSelected(Room room) {
        // Prompt for joining the room
        showJoinRoomDialog(room);
    }private void showJoinRoomDialog(Room room) {
        new AlertDialog.Builder(this)
                .setTitle("Join Room")
                .setMessage("Do you want to join the room '" + room.getName() + "'?")
                .setPositiveButton("Join", (dialog, which) -> {
                    joinRoom(room);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void joinRoom(Room room) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Ensure socket connection
                if (socket == null || socket.isClosed()) {
                    connectToServer();
                    Thread.sleep(500);
                }

                // Send join room request
                if (printWriter != null) {
                    printWriter.println("JOIN_ROOM:" + room.getId());
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error joining room: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("RoomActivity", "Room join error", e);
                });
            }
        });
    }
    private void listenToServerResponses() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    final String response = line;
                    runOnUiThread(() -> {
                        // Existing response handling
                        if (response.startsWith("ROOM_LIST:")) {
                            updateRoomList(response);
                        } else if (response.startsWith("ROOM_CREATED:")) {
                            String[] parts = response.split(":");
                            if (parts.length > 1) {
                                addNewRoom(parts[1]); // Thêm phòng mới vào danh sách
                            }
                        } else if (response.startsWith("ERROR:")) {
                            handleServerError(response);
                        }

                        // New response handling for queue mechanism
                        if (response.startsWith("WAITING_IN_QUEUE:")) {
                            handleWaitingInQueue(response);
                        } else if (response.startsWith("APPROVED_JOIN:")) {
                            handleApprovedJoin(response);
                        } else if (response.startsWith("REJECTED_JOIN:")) {
                            handleRejectedJoin(response);
                        }
                    });
                }
            } catch (IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Mất kết nối với máy chủ", Toast.LENGTH_SHORT).show();
                    Log.e("RoomActivity", "Server response listener error", e);
                    reconnectToServer();
                });
            }
        });
    }

    private void handleWaitingInQueue(String response) {
        String roomId = response.split(":")[1];
        Toast.makeText(this, "Waiting in queue for room " + roomId, Toast.LENGTH_SHORT).show();

        // Optionally, show a dialog indicating waiting status
        showWaitingDialog(roomId);
    }

    private void showWaitingDialog(String roomId) {
        AlertDialog waitingDialog = new AlertDialog.Builder(this)
                .setTitle("Waiting for Approval")
                .setMessage("Waiting for room owner to approve your join request...")
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // Optional: Send cancel request to server
                })
                .create();

        waitingDialog.show();
    }








































    private void handleApprovedJoin(String response) {
        String roomId = response.split(":")[1];
        Toast.makeText(this, "Approved to join room " + roomId, Toast.LENGTH_SHORT).show();

        // Navigate to game activity
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("ROOM_ID", roomId);
        startActivity(intent);
    }

    private void handleRejectedJoin(String response) {
        String roomId = response.split(":")[1];
        Toast.makeText(this, "Join request rejected for room " + roomId, Toast.LENGTH_SHORT).show();
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Đóng kết nối socket khi activity bị hủy
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            Log.e("RoomActivity", "Error closing socket", e);
        }
    }

    // Lớp Room để lưu thông tin phòng





    public static class Room {
        private String id;
        private String name;
        private int currentPlayers;
        private int maxPlayers;

        public Room(String id, String name) {
            this.id = id;
            this.name = name;
            this.currentPlayers = 0;
            this.maxPlayers = 2; // Mặc định là phòng 2 người chơi
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public int getCurrentPlayers() { return currentPlayers; }
        public int getMaxPlayers() { return maxPlayers; }
        public void incrementPlayers() {
            if (currentPlayers < maxPlayers) {
                currentPlayers++;
            }
        }
        public boolean isFull() {
            return currentPlayers >= maxPlayers;
        }
    }
}