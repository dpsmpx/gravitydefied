package org.happysanta.gd.Storage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class ReplayStore {

    private static final int MAGIC = 0x47445250;
    private static final int VERSION = 1;
    private static final int MAX_FRAMES = 120000;
    private static final String DIRECTORY_NAME = "replays";

    private ReplayStore() {
    }

    public static Replay load(long levelId, int level, int track) {
        if (levelId <= 0 || level < 0 || track < 0) {
            return null;
        }

        File file = getFile(levelId, level, track);
        if (!file.isFile() || !file.canRead()) {
            return null;
        }

        try (DataInputStream in = new DataInputStream(new BufferedInputStream(
                new GZIPInputStream(new FileInputStream(file))))) {

            if (in.readInt() != MAGIC) {
                return null;
            }
            if (in.readInt() != VERSION) {
                return null;
            }

            long storedLevelId = in.readLong();
            int storedLevel = in.readInt();
            int storedTrack = in.readInt();
            long timeCs = in.readLong();
            int frameCount = in.readInt();

            if (storedLevelId != levelId || storedLevel != level || storedTrack != track ||
                    timeCs <= 0 || frameCount < 1 || frameCount > MAX_FRAMES) {
                return null;
            }

            ArrayList<Replay.Frame> frames = new ArrayList<>(frameCount);
            int previousTime = -1;
            for (int i = 0; i < frameCount; i++) {
                int timeMs = in.readInt();
                if (timeMs < 0 || timeMs < previousTime) {
                    return null;
                }

                Replay.Frame frame = new Replay.Frame(timeMs);
                frame.inputX = in.readInt();
                frame.inputY = in.readInt();
                for (int part = 0; part < Replay.STATE_PARTS; part++) {
                    frame.x[part] = in.readInt();
                    frame.y[part] = in.readInt();
                    frame.angle[part] = in.readInt();
                }
                frames.add(frame);
                previousTime = timeMs;
            }

            return new Replay(levelId, level, track, timeCs, frames);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean saveIfBetter(
            long levelId,
            int level,
            int track,
            Replay replay,
            long existingRecordCs
    ) throws IOException {
        if (levelId <= 0 || replay == null || replay.frames.isEmpty()) {
            return false;
        }

        Replay previous = load(levelId, level, track);
        long previousReplayTime = previous == null ? Long.MAX_VALUE : previous.timeCs;
        long currentRecord = existingRecordCs > 0 ? existingRecordCs : Long.MAX_VALUE;
        if (replay.timeCs >= previousReplayTime || replay.timeCs >= currentRecord) {
            return false;
        }

        File directory = getDirectory();
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("Unable to create replay directory");
        }

        File target = getFile(levelId, level, track);
        File temp = new File(target.getAbsolutePath() + ".tmp");

        try {
            write(temp, replay);
            if (target.exists() && !target.delete()) {
                throw new IOException("Unable to replace replay");
            }
            if (!temp.renameTo(target)) {
                throw new IOException("Unable to finalize replay");
            }
            return true;
        } finally {
            if (temp.exists()) {
                temp.delete();
            }
        }
    }

    public static void deleteForLevel(long levelId) {
        if (levelId <= 0) {
            return;
        }

        File directory = getDirectory();
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        String prefix = levelId + "_";
        for (File file : files) {
            if (file.isFile() && file.getName().startsWith(prefix) && file.getName().endsWith(".rpl")) {
                file.delete();
            }
        }
    }

    public static void deleteAll() {
        File directory = getDirectory();
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isFile()) {
                file.delete();
            }
        }
    }

    private static void write(File target, Replay replay) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(
                new GZIPOutputStream(new FileOutputStream(target))))) {

            out.writeInt(MAGIC);
            out.writeInt(VERSION);
            out.writeLong(replay.levelId);
            out.writeInt(replay.level);
            out.writeInt(replay.track);
            out.writeLong(replay.timeCs);
            out.writeInt(replay.frames.size());

            for (Replay.Frame frame : replay.frames) {
                out.writeInt(frame.timeMs);
                out.writeInt(frame.inputX);
                out.writeInt(frame.inputY);
                for (int part = 0; part < Replay.STATE_PARTS; part++) {
                    out.writeInt(frame.x[part]);
                    out.writeInt(frame.y[part]);
                    out.writeInt(frame.angle[part]);
                }
            }
        }
    }

    private static File getDirectory() {
        return new File(LevelsManager.getLevelsDirectory(), DIRECTORY_NAME);
    }

    private static File getFile(long levelId, int level, int track) {
        return new File(getDirectory(), levelId + "_" + level + "_" + track + ".rpl");
    }
}
