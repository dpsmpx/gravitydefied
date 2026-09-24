package org.happysanta.gd.Storage;

import org.happysanta.gd.Game.Physics;

import java.util.ArrayList;

public final class ReplayRecorder {

    private static final int MAX_FRAMES = 120000;

    private final ArrayList<Replay.Frame> frames = new ArrayList<>();
    private boolean recording;

    public void start() {
        frames.clear();
        recording = true;
    }

    public void cancel() {
        frames.clear();
        recording = false;
    }

    public boolean isRecording() {
        return recording;
    }

    public void record(Physics physics, long elapsedMs) {
        if (!recording || frames.size() >= MAX_FRAMES) {
            return;
        }

        Replay.Frame frame = new Replay.Frame((int) Math.max(0, Math.min(Integer.MAX_VALUE, elapsedMs)));
        frame.inputX = physics.getReplayInputX();
        frame.inputY = physics.getReplayInputY();
        physics.copyReplayState(frame);
        frames.add(frame);
    }

    public Replay finish(long levelId, int level, int track, long timeCs) {
        if (!recording || frames.isEmpty() || timeCs <= 0) {
            recording = false;
            return null;
        }

        recording = false;
        ArrayList<Replay.Frame> copy = new ArrayList<>(frames.size());
        for (Replay.Frame frame : frames) {
            copy.add(frame.copy());
        }
        frames.clear();
        return new Replay(levelId, level, track, timeCs, copy);
    }
}
