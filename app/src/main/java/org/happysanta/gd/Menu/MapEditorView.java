package org.happysanta.gd.Menu;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;
import android.content.Context;

import org.happysanta.gd.Levels.LevelPackEditor.Point;
import org.happysanta.gd.Levels.LevelPackEditor.Track;

public class MapEditorView extends View {

    public static final int MODE_SELECT = 0;
    public static final int MODE_ADD = 1;
    public static final int MODE_DELETE = 2;

    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markerTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    private Track track;
    private int mode = MODE_SELECT;
    private int selectedIndex = -1;

    private float zoom = 4f;
    private float centerX;
    private float centerY;
    private float lastX;
    private float lastY;
    private float pinchDistance;
    private float pinchWorldX;
    private float pinchWorldY;
    private boolean movingPoint;

    public MapEditorView(Context context, Track track) {
        super(context);
        this.track = track;

        gridPaint.setColor(0xffdddddd);
        gridPaint.setStrokeWidth(1f);
        axisPaint.setColor(0xffaaaaaa);
        axisPaint.setStrokeWidth(2f);

        trackPaint.setColor(0xff1b6ca8);
        trackPaint.setStrokeWidth(4f);
        trackPaint.setStyle(Paint.Style.STROKE);

        pointPaint.setColor(0xff555555);
        selectedPaint.setColor(0xffd32f2f);
        markerPaint.setColor(0xff2e7d32);

        markerTextPaint.setColor(0xffffffff);
        markerTextPaint.setTextSize(20f);
        markerTextPaint.setTextAlign(Paint.Align.CENTER);

        setFocusable(true);
    }

    public void setMode(int mode) {
        this.mode = mode;
        movingPoint = false;
        invalidate();
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void zoomBy(float factor) {
        zoom = clamp(zoom * factor, 0.2f, 40f);
        invalidate();
    }

    public void fitTrack() {
        if (track == null || track.points.isEmpty() || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }

        int minX = track.points.get(0).x;
        int maxX = minX;
        int minY = track.points.get(0).y;
        int maxY = minY;

        for (Point point : track.points) {
            minX = Math.min(minX, point.x);
            maxX = Math.max(maxX, point.x);
            minY = Math.min(minY, point.y);
            maxY = Math.max(maxY, point.y);
        }

        float rangeX = Math.max(20f, maxX - minX);
        float rangeY = Math.max(20f, maxY - minY);
        float availableX = Math.max(100f, getWidth() - 80f);
        float availableY = Math.max(100f, getHeight() - 80f);

        zoom = clamp(Math.min(availableX / rangeX, availableY / rangeY), 0.2f, 20f);
        centerX = (minX + maxX) * 0.5f;
        centerY = (minY + maxY) * 0.5f;
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        fitTrack();
    }

    private float worldX(float screenX) {
        return centerX + (screenX - getWidth() * 0.5f) / zoom;
    }

    private float worldY(float screenY) {
        return centerY - (screenY - getHeight() * 0.5f) / zoom;
    }

    private float screenX(float worldX) {
        return getWidth() * 0.5f + (worldX - centerX) * zoom;
    }

    private float screenY(float worldY) {
        return getHeight() * 0.5f - (worldY - centerY) * zoom;
    }

    private int hitPoint(float x, float y) {
        int best = -1;
        float bestDistance = 26f;

        for (int i = 0; i < track.points.size(); i++) {
            Point point = track.points.get(i);
            float dx = screenX(point.x) - x;
            float dy = screenY(point.y) - y;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            if (distance <= bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return best;
    }

    private void addPointAt(float x, float y) {
        int newX = Math.round(worldX(x));
        int newY = Math.round(worldY(y));

        if (newX == -1) {
            newX = 0;
        }

        int position = 0;
        while (position < track.points.size() && track.points.get(position).x < newX) {
            position++;
        }

        long lower = position > 0 ? track.points.get(position - 1).x : Integer.MIN_VALUE;
        long upper = position < track.points.size() ? track.points.get(position).x : Integer.MAX_VALUE;

        if (upper - lower <= 1) {
            return;
        }

        if (newX <= lower) {
            newX = (int) (lower + 1);
        }
        if (newX >= upper) {
            newX = (int) (upper - 1);
        }
        if (newX <= lower || newX >= upper) {
            return;
        }

        track.points.add(position, new Point(newX, newY));

        if (track.startIndex >= position) {
            track.startIndex++;
        }
        if (track.finishIndex >= position) {
            track.finishIndex++;
        }

        if (track.finishIndex <= track.startIndex) {
            track.startIndex = 0;
            track.finishIndex = track.points.size() - 1;
        }

        selectedIndex = position;
        updateMarkers();
        invalidate();
    }

    private void deletePointAt(float x, float y) {
        int index = hitPoint(x, y);
        if (index < 0 || track.points.size() <= 2) {
            return;
        }

        track.points.remove(index);

        if (track.startIndex == index) {
            track.startIndex = Math.max(0, index - 1);
        } else if (track.startIndex > index) {
            track.startIndex--;
        }

        if (track.finishIndex == index) {
            track.finishIndex = Math.min(track.points.size() - 1, index);
        } else if (track.finishIndex > index) {
            track.finishIndex--;
        }

        if (track.finishIndex <= track.startIndex) {
            track.startIndex = 0;
            track.finishIndex = track.points.size() - 1;
        }

        selectedIndex = Math.min(index, track.points.size() - 1);
        updateMarkers();
        invalidate();
    }

    private void moveSelectedPoint(float x, float y) {
        if (!movingPoint || selectedIndex < 0 || selectedIndex >= track.points.size()) {
            return;
        }

        int newX = Math.round(worldX(x));
        int newY = Math.round(worldY(y));

        if (selectedIndex > 0) {
            newX = Math.max(newX, track.points.get(selectedIndex - 1).x + 1);
        }
        if (selectedIndex + 1 < track.points.size()) {
            newX = Math.min(newX, track.points.get(selectedIndex + 1).x - 1);
        }

        Point point = track.points.get(selectedIndex);
        point.x = newX;
        point.y = newY;
        updateMarkers();
        invalidate();
    }

    private void pan(float dx, float dy) {
        centerX -= dx / zoom;
        centerY += dy / zoom;
        invalidate();
    }

    private void updateMarkers() {
        if (track.points.isEmpty()) {
            return;
        }

        track.startIndex = Math.max(0, Math.min(track.startIndex, track.points.size() - 1));
        track.finishIndex = Math.max(0, Math.min(track.finishIndex, track.points.size() - 1));

        if (track.finishIndex <= track.startIndex) {
            track.finishIndex = track.points.size() - 1;
        }

        track.startX = track.points.get(track.startIndex).x;
        track.finishX = track.points.get(track.finishIndex).x;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(0xfff7f7f7);

        if (track == null || track.points.isEmpty()) {
            return;
        }

        drawGrid(canvas);

        path.reset();
        Point first = track.points.get(0);
        path.moveTo(screenX(first.x), screenY(first.y));
        for (int i = 1; i < track.points.size(); i++) {
            Point point = track.points.get(i);
            path.lineTo(screenX(point.x), screenY(point.y));
        }
        canvas.drawPath(path, trackPaint);

        for (int i = 0; i < track.points.size(); i++) {
            Point point = track.points.get(i);
            float radius = i == selectedIndex ? 9f : 6f;
            canvas.drawCircle(
                    screenX(point.x),
                    screenY(point.y),
                    radius,
                    i == selectedIndex ? selectedPaint : pointPaint
            );
        }

        drawMarker(canvas, track.startIndex, "S");
        drawMarker(canvas, track.finishIndex, "F");
    }

    private void drawMarker(Canvas canvas, int index, String label) {
        if (index < 0 || index >= track.points.size()) {
            return;
        }

        Point point = track.points.get(index);
        float x = screenX(point.x);
        float y = screenY(point.y);
        canvas.drawCircle(x, y, 15f, markerPaint);
        canvas.drawText(label, x, y + 7f, markerTextPaint);
    }

    private void drawGrid(Canvas canvas) {
        int spacing = zoom >= 8f ? 5 : 10;
        int firstX = floorTo(worldX(0), spacing);
        int lastX = ceilTo(worldX(getWidth()), spacing);
        int firstY = floorTo(worldY(getHeight()), spacing);
        int lastY = ceilTo(worldY(0), spacing);

        for (int x = firstX; x <= lastX; x += spacing) {
            float screen = screenX(x);
            canvas.drawLine(screen, 0, screen, getHeight(), gridPaint);
        }

        for (int y = firstY; y <= lastY; y += spacing) {
            float screen = screenY(y);
            canvas.drawLine(0, screen, getWidth(), screen, gridPaint);
        }

        canvas.drawLine(screenX(0), 0, screenX(0), getHeight(), axisPaint);
        canvas.drawLine(0, screenY(0), getWidth(), screenY(0), axisPaint);
    }

    private int floorTo(float value, int spacing) {
        return (int) Math.floor(value / spacing) * spacing;
    }

    private int ceilTo(float value, int spacing) {
        return (int) Math.ceil(value / spacing) * spacing;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (track == null) {
            return true;
        }

        ViewParentCompat.disallowIntercept(this, true);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = event.getX();
                lastY = event.getY();

                if (mode == MODE_SELECT) {
                    selectedIndex = hitPoint(lastX, lastY);
                    movingPoint = selectedIndex >= 0;
                } else if (mode == MODE_ADD) {
                    addPointAt(lastX, lastY);
                    movingPoint = false;
                } else if (mode == MODE_DELETE) {
                    deletePointAt(lastX, lastY);
                    movingPoint = false;
                }
                invalidate();
                return true;

            case MotionEvent.ACTION_POINTER_DOWN:
                if (event.getPointerCount() >= 2) {
                    float x1 = event.getX(0);
                    float y1 = event.getY(0);
                    float x2 = event.getX(1);
                    float y2 = event.getY(1);
                    pinchDistance = distance(x1, y1, x2, y2);

                    float focusX = (x1 + x2) * 0.5f;
                    float focusY = (y1 + y2) * 0.5f;
                    pinchWorldX = worldX(focusX);
                    pinchWorldY = worldY(focusY);
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() >= 2 && pinchDistance > 0f) {
                    float x1 = event.getX(0);
                    float y1 = event.getY(0);
                    float x2 = event.getX(1);
                    float y2 = event.getY(1);
                    float currentDistance = distance(x1, y1, x2, y2);
                    float factor = currentDistance / pinchDistance;
                    zoom = clamp(zoom * factor, 0.2f, 40f);
                    pinchDistance = currentDistance;

                    float focusX = (x1 + x2) * 0.5f;
                    float focusY = (y1 + y2) * 0.5f;
                    float worldAfterX = worldX(focusX);
                    float worldAfterY = worldY(focusY);
                    centerX += pinchWorldX - worldAfterX;
                    centerY += pinchWorldY - worldAfterY;
                    invalidate();
                    return true;
                }

                float x = event.getX();
                float y = event.getY();

                if (mode == MODE_SELECT && movingPoint) {
                    moveSelectedPoint(x, y);
                } else if (mode == MODE_SELECT && selectedIndex < 0) {
                    pan(x - lastX, y - lastY);
                }

                lastX = x;
                lastY = y;
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                movingPoint = false;
                pinchDistance = 0f;
                ViewParentCompat.disallowIntercept(this, false);
                return true;
        }

        return true;
    }

    private float distance(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private static final class ViewParentCompat {
        private static void disallowIntercept(View view, boolean disallow) {
            if (view.getParent() != null) {
                view.getParent().requestDisallowInterceptTouchEvent(disallow);
            }
        }
    }
}
