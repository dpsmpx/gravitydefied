package org.happysanta.gd.Storage;

import java.util.ArrayList;

public final class Replay {

    public static final int STATE_PARTS = 6;

    public final long levelId;
    public final int level;
    public final int track;
    public final long timeCs;
    public final ArrayList<Frame> frames;

    public Replay(long levelId, int level, int track, long timeCs, ArrayList<Frame> frames) {
        this.levelId = levelId;
        this.level = level;
        this.track = track;
        this.timeCs = timeCs;
        this.frames = frames == null ? new ArrayList<Frame>() : frames;
    }

    public boolean isEmpty() {
        return frames.isEmpty();
    }

    public Frame getFrameAt(long elapsedMs) {
        if (frames.isEmpty()) {
            return null;
        }

        if (elapsedMs <= frames.get(0).timeMs) {
            return frames.get(0);
        }

        int lastIndex = frames.size() - 1;
        Frame last = frames.get(lastIndex);
        if (elapsedMs >= last.timeMs) {
            return last;
        }

        int low = 0;
        int high = lastIndex;
        while (low + 1 < high) {
            int mid = (low + high) >>> 1;
            if (frames.get(mid).timeMs <= elapsedMs) {
                low = mid;
            } else {
                high = mid;
            }
        }

        Frame a = frames.get(low);
        Frame b = frames.get(high);
        if (b.timeMs <= a.timeMs) {
            return a;
        }

        float factor = (float) (elapsedMs - a.timeMs) / (float) (b.timeMs - a.timeMs);
        return Frame.interpolate(a, b, factor);
    }

    public static final class Frame {
        public final int timeMs;
        public int inputX;
        public int inputY;
        public final int[] x = new int[STATE_PARTS];
        public final int[] y = new int[STATE_PARTS];
        public final int[] angle = new int[STATE_PARTS];

        public Frame(int timeMs) {
            this.timeMs = Math.max(0, timeMs);
        }

        private Frame(int timeMs, int inputX, int inputY) {
            this.timeMs = Math.max(0, timeMs);
            this.inputX = inputX;
            this.inputY = inputY;
        }

        public Frame copy() {
            Frame copy = new Frame(timeMs, inputX, inputY);
            System.arraycopy(x, 0, copy.x, 0, STATE_PARTS);
            System.arraycopy(y, 0, copy.y, 0, STATE_PARTS);
            System.arraycopy(angle, 0, copy.angle, 0, STATE_PARTS);
            return copy;
        }

        private static Frame interpolate(Frame a, Frame b, float factor) {
            factor = Math.max(0f, Math.min(1f, factor));
            Frame frame = new Frame(
                    Math.round(a.timeMs + (b.timeMs - a.timeMs) * factor),
                    Math.round(a.inputX + (b.inputX - a.inputX) * factor),
                    Math.round(a.inputY + (b.inputY - a.inputY) * factor)
            );

            for (int i = 0; i < STATE_PARTS; i++) {
                frame.x[i] = Math.round(a.x[i] + (b.x[i] - a.x[i]) * factor);
                frame.y[i] = Math.round(a.y[i] + (b.y[i] - a.y[i]) * factor);
                frame.angle[i] = Math.round(a.angle[i] + (b.angle[i] - a.angle[i]) * factor);
            }
            return frame;
        }
    }
}
