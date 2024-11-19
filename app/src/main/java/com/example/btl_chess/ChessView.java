package com.example.btl_chess;



import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ChessView extends View {

    private static final float SCALE_FACTOR = 1.0f;
    private float originX = 20f;
    private float originY = 200f;
    private float cellSide = 130f;
    private final int lightColor = Color.parseColor("#EEEEEE");
    private final int darkColor = Color.parseColor("#BBBBBB");
    private boolean showHints = false;
    private Set<Square> hintSquares = new HashSet<>();
    private final int hintColor = Color.parseColor("#80FF0000");
    private final int checkColor = Color.parseColor("#FFFF00");
    /*private final int lightColor = Color.parseColor("#EEEEEE");
    private final int darkColor = Color.parseColor("#333333");*/
    private final int lightPieceColor = Color.parseColor("#FFFFFF");
    private final int darkPieceColor = Color.parseColor("#CCCCCC");
    private Set<Integer> imgResIDs = Set.of(
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
    );

    private Map<Integer, Bitmap> bitmaps = new HashMap<>();
    private Paint paint = new Paint();

    private Bitmap movingPieceBitmap = null;
    private ChessPiece movingPiece = null;
    private int fromCol = -1;
    private int fromRow = -1;
    private float movingPieceX = -1f;
    private float movingPieceY = -1f;

    private ChessDelegate chessDelegate;
    private boolean isBlackSide = false;
    private boolean isPlayingBlack = false;
    private Player currentPlayer = Player.WHITE;

    public ChessView(Context context, AttributeSet attrs) {
        super(context, attrs);
        loadBitmaps();
    }
    public void setBlackSide(boolean blackSide) {
        this.isBlackSide = blackSide;
        invalidate(); // Vẽ lại bàn cờ khi thay đổi góc nhìn
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

        float chessBoardSide = Math.min(getWidth(), getHeight()) * SCALE_FACTOR;
        cellSide = chessBoardSide / 8f;
        originX = (getWidth() - chessBoardSide) / 2f;
        originY = (getHeight() - chessBoardSide) / 2f;

        drawChessboard(canvas);
        if (showHints) {
            drawHints(canvas);
        }
        drawPieces(canvas);
    }
    private void drawHints(Canvas canvas) {
        paint.setColor(hintColor);
        for (Square square : hintSquares) {
            canvas.drawCircle(
                    originX + (square.getCol() + 0.5f) * cellSide,
                    originY + (7 - square.getRow() + 0.5f) * cellSide,
                    cellSide * 0.3f,
                    paint
            );
        }


    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event == null) return false;

        float eventX = event.getX();
        float eventY = event.getY();



        // Check for clicks outside the chessboard
        if (eventX < originX || eventX > originX + 8 * cellSide ||
                eventY < originY || eventY > originY + 8 * cellSide) {
            if (event.getAction() == MotionEvent.ACTION_UP && movingPiece != null) {
                movingPiece = null;
                movingPieceBitmap = null;
                clearHints();
                invalidate();
            }
            return true;
        }

        // Calculate square position based on view perspective
        int col = (int)((eventX - originX) / cellSide);
        int row = 7 - (int)((eventY - originY) / cellSide);

        // Adjust coordinates based on perspective (black/white)
        if (isBlackSide) {
            col = 7 - col;
            row = 7 - row;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                fromCol = col;
                fromRow = row;

                if (fromCol >= 0 && fromCol < 8 && fromRow >= 0 && fromRow < 8) {
                    if (chessDelegate != null) {
                        ChessPiece piece = chessDelegate.pieceAt(new Square(fromCol, fromRow));
                        if (piece != null) {
                            movingPiece = piece;
                            movingPieceBitmap = bitmaps.get(piece.getResID());
                            movingPieceX = eventX;
                            movingPieceY = eventY;

                            if (showHints) {
                                updateHints(new Square(fromCol, fromRow));
                            }
                            invalidate();
                        }
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (movingPiece != null) {
                    movingPieceX = eventX;
                    movingPieceY = eventY;
                    invalidate();
                }
                break;

            case MotionEvent.ACTION_UP:
                if (movingPiece != null) {
                    int toCol = col;
                    int toRow = row;

                    if (toCol >= 0 && toCol < 8 && toRow >= 0 && toRow < 8) {
                        Square toSquare = new Square(toCol, toRow);
                        boolean isValidMove = !showHints || hintSquares.contains(toSquare);

                        if (fromCol != toCol || fromRow != toRow) {
                            if (chessDelegate != null && isValidMove) {
                                chessDelegate.movePiece(new Square(fromCol, fromRow), toSquare);
                            }
                        }
                    }

                    movingPiece = null;
                    movingPieceBitmap = null;
                    clearHints();
                    invalidate();
                }
                break;
        }

        return true;
    }
    public void setShowHints(boolean show) {
        showHints = show;
        invalidate();
    }

    public void updateHints(Square fromSquare) {
        if (chessDelegate != null) {
            // Lấy danh sách các nước đi hợp lệ từ delegate
            hintSquares = chessDelegate.getValidMoves(fromSquare);
            invalidate();
        }
    }

    public void clearHints() {
        hintSquares.clear();
        invalidate();
    }


    private void drawPieces(Canvas canvas) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if (chessDelegate != null) {
                    // Adjust coordinates based on perspective
                    int adjustedRow = isBlackSide ? row : 7 - row;
                    int adjustedCol = isBlackSide ? 7 - col : col;

                    ChessPiece piece = chessDelegate.pieceAt(new Square(adjustedCol, adjustedRow));
                    if (piece != null && !piece.equals(movingPiece)) {
                        drawPieceAt(canvas, col, row, piece.getResID());
                    }
                }
            }
        }

        // Draw moving piece
        if (movingPieceBitmap != null) {
            canvas.drawBitmap(movingPieceBitmap, null,
                    new RectF(movingPieceX - cellSide / 2, movingPieceY - cellSide / 2,
                            movingPieceX + cellSide / 2, movingPieceY + cellSide / 2), paint);
        }
    }
    private void drawPieceAt(Canvas canvas, int col, int row, int resID) {
        Bitmap bitmap = bitmaps.get(resID);
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, null,
                    new RectF(originX + col * cellSide, originY + (7 - row) * cellSide,
                            originX + (col + 1) * cellSide, originY + ((7 - row) + 1) * cellSide), paint);
        }
    }

    private void loadBitmaps() {
        for (int imgResID : imgResIDs) {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), imgResID);
            bitmaps.put(imgResID, bitmap);
        }
    }

    private void drawChessboard(Canvas canvas) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                // Tính toán màu ô dựa trên vị trí
                boolean isDark = (col + row) % 2 == 1;
                // Vẽ ô cờ với vị trí đã được điều chỉnh theo góc nhìn
                drawSquareAt(canvas, col, row, isDark);
            }
        }
    }

    private void drawSquareAt(Canvas canvas, int col, int row, boolean isDark) {
        paint.setColor(isDark ? darkColor : lightColor);
        canvas.drawRect(originX + col * cellSide, originY + row * cellSide,
                originX + (col + 1) * cellSide, originY + (row + 1) * cellSide, paint);
    }

    public void setChessDelegate(ChessDelegate chessDelegate) {
        this.chessDelegate = chessDelegate;
    }

}
