package com.example.btl_chess;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;

public class RoomActivity extends AppCompatActivity {
    private EditText roomNameInput;
    private Button createRoomButton;
    private RecyclerView roomListRecyclerView;
    private RoomAdapter roomAdapter;
    private List<Room> roomList = new ArrayList<>();
    private Socket socket;
    private PrintWriter printWriter;
    private static final String SOCKET_HOST = "10.0.2.2";
    private static final int SOCKET_PORT = 50001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);

        // Ánh xạ view
        roomNameInput = findViewById(R.id.room_name_input);
        createRoomButton = findViewById(R.id.create_room_button);
        roomListRecyclerView = findViewById(R.id.room_list_recycler_view);

        // Thiết lập RecyclerView
        roomAdapter = new RoomAdapter(roomList, this::onRoomSelected);
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
    }

    private void connectToServer() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                socket = new Socket(SOCKET_HOST, SOCKET_PORT);
                printWriter = new PrintWriter(socket.getOutputStream(), true);

                // Lắng nghe phản hồi từ server
                listenToServerResponses();

                // Yêu cầu danh sách phòng
                printWriter.println("LIST_ROOMS");
            } catch (IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Lỗi kết nối máy chủ", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void listenToServerResponses() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    final String response = line;
                    runOnUiThread(() -> {
                        // Xử lý phản hồi từ server
                        if (response.startsWith("ROOM_LIST:")) {
                            updateRoomList(response);
                        } else if (response.startsWith("ROOM_CREATED:")) {
                            String roomId = response.split(":")[1];
                            addNewRoom(roomId);
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
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
                });
            }
        });
    }

    private void updateRoomList(String response) {
        try {
            // Kiểm tra định dạng response
            if (!response.startsWith("ROOM_LIST:") || response.length() <= 10) {
                // Response không hợp lệ
                return;
            }

            // Lấy phần danh sách phòng
            String roomListString = response.substring(10);

            // Kiểm tra nếu không có phòng
            if (roomListString.isEmpty()) {
                roomList.clear();
                roomAdapter.notifyDataSetChanged();
                return;
            }

            // Tách các phòng
            String[] rooms = roomListString.split(",");
            roomList.clear();

            for (String roomData : rooms) {
                // Kiểm tra định dạng dữ liệu phòng
                String[] details = roomData.split("\\|");
                if (details.length >= 2) {
                    // Đảm bảo đủ thông tin để tạo phòng
                    roomList.add(new Room(details[0], details[1]));
                } else {
                    // Ghi log hoặc xử lý dữ liệu không đúng định dạng
                    Log.w("RoomActivity", "Invalid room data: " + roomData);
                }
            }

            // Cập nhật adapter
            roomAdapter.notifyDataSetChanged();

        } catch (Exception e) {
            // Bắt và xử lý các ngoại lệ có thể xảy ra
            Log.e("RoomActivity", "Error updating room list", e);
            Toast.makeText(this, "Lỗi cập nhật danh sách phòng", Toast.LENGTH_SHORT).show();
        }
    }

    private void addNewRoom(String roomId) {
        // Thêm phòng mới vào danh sách
        Room newRoom = new Room(roomId, roomNameInput.getText().toString());
        roomList.add(newRoom);
        roomAdapter.notifyItemInserted(roomList.size() - 1);

        // Xóa text input sau khi tạo phòng
        roomNameInput.setText("");
    }

    private void onRoomSelected(Room room) {
        // Chuyển sang màn hình chơi game khi chọn phòng
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("ROOM_ID", room.getId());

        startActivity(intent);
    }

    // Lớp Room để lưu thông tin phòng
    public static class Room {
        private String id;
        private String name;
        private int currentPlayers; // Số người hiện tại trong phòng

        public Room(String id, String name) {
            this.id = id;
            this.name = name;
            this.currentPlayers = 0; // Khởi tạo số người hiện tại là 0
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public int getCurrentPlayers() { return currentPlayers; }
        public void incrementPlayers() { currentPlayers++; } // Tăng số người lên 1
    }


}