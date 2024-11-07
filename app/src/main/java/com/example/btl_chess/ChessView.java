package com.example.btl_chess;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChessView extends View {
    private Square selectedSquare = null;
    private Paint hintPaint;
    private final float scaleFactor = 1.0f;
    private float originX = 20f;
    private float originY = 200f;
    private float cellSide = 130f;
    private final int lightColor = Color.parseColor("#EEEEEE");
    private final int darkColor = Color.parseColor("#BBBBBB");
    private final int highlightColor = Color.YELLOW; // Màu cho ô được nổi bật
    private final int[] imgResIDs = {
            R.drawable.bishop_black,
            R.drawable.bishop_white,
            R.drawable.king_black,
            R.drawable.king_white,
            R.drawable.queen_black,
            R.drawable.queen_white,
            R.drawable.rook_black,
            R.drawable.rook_white,
            R.drawable.knight_black,
            R.drawable.knight_white,
            R.drawable.pawn_black,
            R.drawable.pawn_white
    };
    private final Map<Integer, Bitmap> bitmaps = new HashMap<>();
    private final Paint paint = new Paint();

    private Bitmap movingPieceBitmap;
    private ChessPiece movingPiece;
    private int fromCol = -1;
    private int fromRow = -1;
    private float movingPieceX = -1f;
    private float movingPieceY = -1f;

    private List<Square> highlightedSquares = new ArrayList<>(); // Danh sách các ô được nổi bật

    private ChessDelegate chessDelegate;

    public ChessView(Context context, AttributeSet attrs) {
        super(context, attrs);
        loadBitmaps();
        init();
    }
    private void init() {
        // Khởi tạo Paint cho gợi ý nước đi
        hintPaint = new Paint();
        hintPaint.setColor(Color.argb(128, 0, 255, 0)); // Màu xanh lá trong suốt
        hintPaint.setStyle(Paint.Style.FILL);
    }


    public void setChessDelegate(ChessDelegate chessDelegate) {
        this.chessDelegate = chessDelegate;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int smaller = Math.min(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(smaller, smaller);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (canvas == null) return;

        float chessBoardSide = Math.min(getWidth(), getHeight()) * scaleFactor;
        cellSide = chessBoardSide / 8f;
        originX = (getWidth() - chessBoardSide) / 2f;
        originY = (getHeight() - chessBoardSide) / 2f;

        drawChessboard(canvas);
        drawPieces(canvas);
        if (selectedSquare != null) {
            drawMoveHints(canvas);
        }
//        drawHighlightedSquares(canvas); // Vẽ các ô được nổi bật
    }
    private void drawMoveHints(Canvas canvas) {
        Set<Square> possibleMoves = ChessGame.getPossibleMoves(selectedSquare);
        float squareSize = getWidth() / 8f;

        for (Square square : possibleMoves) {
            float left = square.getCol() * squareSize;
            float top = (7 - square.getRow()) * squareSize;

            // Vẽ hình tròn gợi ý
            if (ChessGame.pieceAt(square) != null) {
                // Nếu có quân đối phương, vẽ viền để chỉ ra có thể ăn
                canvas.drawCircle(
                        left + squareSize/2,
                        top + squareSize/2,
                        squareSize/2,
                        getHintStrokePaint()
                );
            } else {
                // Nếu ô trống, vẽ chấm tròn nhỏ
                canvas.drawCircle(
                        left + squareSize/2,
                        top + squareSize/2,
                        squareSize/4,
                        hintPaint
                );
            }
        }
    }
    private Paint getHintStrokePaint() {
        Paint strokePaint = new Paint();
        strokePaint.setColor(Color.argb(128, 255, 0, 0)); // Màu đỏ trong suốt
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(5);
        return strokePaint;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float squareSize = getWidth() / 8f;
            int col = (int)(event.getX() / squareSize);
            int row = 7 - (int)(event.getY() / squareSize);

            Square touchedSquare = new Square(col, row);

            if (selectedSquare == null) {
                // Nếu chưa có quân được chọn và ô chạm vào có quân
                if (ChessGame.pieceAt(touchedSquare) != null) {
                    selectedSquare = touchedSquare;
                    invalidate(); // Vẽ lại để hiện gợi ý
                }
            } else {
                // Nếu đã có quân được chọn
                if (ChessGame.isValidMove(touchedSquare)) {
                    // Thực hiện nước đi nếu hợp lệ
                    ChessGame.movePiece(selectedSquare, touchedSquare);
                }
                // Xóa chọn và gợi ý
                selectedSquare = null;
                ChessGame.clearPossibleMoves();
                invalidate();
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    private void drawPieces(Canvas canvas) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if (chessDelegate != null) {
                    ChessPiece piece = chessDelegate.pieceAt(new Square(col, row));
                    if (piece != null && piece != movingPiece) {
                        drawPieceAt(canvas, col, row, piece.getResID());
                    }
                }
            }
        }

        if (movingPieceBitmap != null) {
            canvas.drawBitmap(movingPieceBitmap, null,
                    new RectF(movingPieceX - cellSide / 2, movingPieceY - cellSide / 2,
                            movingPieceX + cellSide / 2, movingPieceY + cellSide / 2), paint);
        }
    }

    private void drawPieceAt(Canvas canvas, int col, int row, int resID) {
        Bitmap bitmap = bitmaps.get(resID);
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, null, new RectF(
                    originX + col * cellSide, originY + (7 - row) * cellSide,
                    originX + (col + 1) * cellSide, originY + ((7 - row) + 1) * cellSide), paint);
        }
    }

    private void loadBitmaps() {
        for (int imgResID : imgResIDs) {
            bitmaps.put(imgResID, BitmapFactory.decodeResource(getResources(), imgResID));
        }
    }

    private void drawChessboard(Canvas canvas) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                drawSquareAt(canvas, col, row, (col + row) % 2 == 1);
            }
        }
    }

    private void drawSquareAt(Canvas canvas, int col, int row, boolean isDark) {
        paint.setColor(isDark ? darkColor : lightColor);
        canvas.drawRect(originX + col * cellSide, originY + row * cellSide,
                originX + (col + 1) * cellSide, originY + (row + 1) * cellSide, paint);
    }

    // Tính toán các ô có thể di chuyển
    private List<Square> calculatePossibleMoves(ChessPiece piece, Square position) {
        List<Square> possibleMoves = new ArrayList<>();

        switch (piece.getChessman()) { // Sử dụng getChessman() thay vì getType()
            case KING:
                for (int row = -1; row <= 1; row++) {
                    for (int col = -1; col <= 1; col++) {
                        if (row == 0 && col == 0) continue; // Bỏ qua ô hiện tại
                        Square newSquare = new Square(position.getCol() + col, position.getRow() + row);
                        if (isValidSquare(newSquare)) {
                            possibleMoves.add(newSquare);
                        }
                    }
                }
                break;
            case QUEEN:
                for (int dir = -1; dir <= 1; dir++) {
                    for (int row = -1; row <= 1; row++) {
                        if (dir == 0 && row == 0) continue; // Bỏ qua ô hiện tại
                        for (int i = 1; i < 8; i++) {
                            Square newSquare = new Square(position.getCol() + dir * i, position.getRow() + row * i);
                            if (!isValidSquare(newSquare)) break; // Ra ngoài bàn cờ
                            possibleMoves.add(newSquare);
                        }
                    }
                }
                break;
            case ROOK:
                for (int dir = -1; dir <= 1; dir += 2) {
                    for (int i = 1; i < 8; i++) {
                        Square newSquare = new Square(position.getCol() + dir * i, position.getRow());
                        if (!isValidSquare(newSquare)) break; // Ra ngoài bàn cờ
                        possibleMoves.add(newSquare);
                    }
                    for (int i = 1; i < 8; i++) {
                        Square newSquare = new Square(position.getCol(), position.getRow() + dir * i);
                        if (!isValidSquare(newSquare)) break; // Ra ngoài bàn cờ
                        possibleMoves.add(newSquare);
                    }
                }
                break;
            case BISHOP:
                for (int dir = -1; dir <= 1; dir += 2) {
                    for (int row = -1; row <= 1; row += 2) {
                        for (int i = 1; i < 8; i++) {
                            Square newSquare = new Square(position.getCol() + dir * i, position.getRow() + row * i);
                            if (!isValidSquare(newSquare)) break; // Ra ngoài bàn cờ
                            possibleMoves.add(newSquare);
                        }
                    }
                }
                break;
            case KNIGHT:
                int[][] knightMoves = {
                        {2, 1}, {2, -1}, {-2, 1}, {-2, -1},
                        {1, 2}, {1, -2}, {-1, 2}, {-1, -2}
                };
                for (int[] move : knightMoves) {
                    Square newSquare = new Square(position.getCol() + move[0], position.getRow() + move[1]);
                    if (isValidSquare(newSquare)) {
                        possibleMoves.add(newSquare);
                    }
                }
                break;
            case PAWN:
                int direction = piece.getPlayer().isWhite() ? 1 : -1; // Tùy thuộc vào màu sắc
                Square forwardMove = new Square(position.getCol(), position.getRow() + direction);
                if (isValidSquare(forwardMove)) {
                    possibleMoves.add(forwardMove);
                }
                Square leftCapture = new Square(position.getCol() - 1, position.getRow() + direction);
                Square rightCapture = new Square(position.getCol() + 1, position.getRow() + direction);
                if (isValidSquare(leftCapture)) {
                    possibleMoves.add(leftCapture);
                }
                if (isValidSquare(rightCapture)) {
                    possibleMoves.add(rightCapture);
                }
                break;
        }

        return possibleMoves;
    }

    private boolean isValidSquare(Square square) {
        return square.getCol() >= 0 && square.getCol() < 8 && square.getRow() >= 0 && square.getRow() < 8;
    }

//    private void drawHighlightedSquares(Canvas canvas) {
//        for (Square square : highlightedSquares) {
//            paint.setColor(highlightColor);
//            canvas.drawRect(
//                    originX + square.getCol() * cellSide,
//                    originY + (7 - square.getRow()) * cellSide,
//                    originX + (square.getCol() + 1) * cellSide,
//                    originY + ((7 - square.getRow()) + 1) * cellSide,
//                    paint
//            );
//        }
//    }
}

