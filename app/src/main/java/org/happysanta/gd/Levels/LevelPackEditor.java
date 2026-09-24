package org.happysanta.gd.Levels;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;

public final class LevelPackEditor {

    public static final int GROUPS = 4;
    private static final int MAX_TRACKS = 16384;
    private static final int MAX_POINTS = 32767;
    private static final long COORDINATE_LIMIT = 262143L;
    private static final Charset CP1251 = Charset.forName("windows-1251");

    private LevelPackEditor() {
    }

    public static final class Point {
        public int x;
        public int y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public Point copy() {
            return new Point(x, y);
        }
    }

    public static final class Track {
        public String name;
        public int startX;
        public int startY;
        public int finishX;
        public int finishY;
        public int startIndex;
        public int finishIndex;
        public final ArrayList<Point> points = new ArrayList<>();

        public Track(String name) {
            this.name = name;
            this.startIndex = 0;
            this.finishIndex = 0;
        }

        public Track copy() {
            Track copy = new Track(name);
            copy.startX = startX;
            copy.startY = startY;
            copy.finishX = finishX;
            copy.finishY = finishY;
            copy.startIndex = startIndex;
            copy.finishIndex = finishIndex;
            for (Point point : points) {
                copy.points.add(point.copy());
            }
            return copy;
        }
    }

    @SuppressWarnings("unchecked")
    public static final class Pack {
        public String name;
        public String author;
        public final ArrayList<Track>[] groups = (ArrayList<Track>[]) new ArrayList[GROUPS];

        public Pack() {
            for (int i = 0; i < GROUPS; i++) {
                groups[i] = new ArrayList<>();
            }
        }

        public Pack copy() {
            Pack copy = new Pack();
            copy.name = name;
            copy.author = author;
            for (int group = 0; group < GROUPS; group++) {
                for (Track track : groups[group]) {
                    copy.groups[group].add(track.copy());
                }
            }
            return copy;
        }

        public int totalTracks() {
            int total = 0;
            for (int i = 0; i < GROUPS; i++) {
                total += groups[i].size();
            }
            return total;
        }
    }

    public static Pack read(InputStream input) throws IOException {
        if (input == null) {
            throw new IOException("No level data");
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) {
            bytes.write(buffer, 0, count);
        }

        byte[] data = bytes.toByteArray();
        if (data.length < 12) {
            throw new IOException("Level file is too small");
        }

        LevelHeader header = Reader.readHeader(new ByteArrayInputStream(data));
        Pack pack = new Pack();
        for (int group = 0; group < GROUPS; group++) {
            if (header.getCount(group) > MAX_TRACKS) {
                throw new IOException("Too many tracks");
            }

            for (int trackIndex = 0; trackIndex < header.getCount(group); trackIndex++) {
                int pointer = header.getPointers()[group][trackIndex];
                if (pointer < 0 || pointer >= data.length) {
                    throw new IOException("Invalid track pointer");
                }

                DataInputStream trackInput = new DataInputStream(
                        new ByteArrayInputStream(data, pointer, data.length - pointer)
                );
                Level level = new Level();
                level.readTrackData(trackInput);
                if (level.pointsCount < 2 || level.points == null) {
                    throw new IOException("Invalid track: " + header.getNames()[group][trackIndex]);
                }

                Track track = new Track(header.getNames()[group][trackIndex]);
                track.startX = fixedToRaw(level.startX);
                track.startY = fixedToRaw(level.startY);
                track.finishX = fixedToRaw(level.finishX);
                track.finishY = fixedToRaw(level.finishY);

                for (int i = 0; i < level.pointsCount; i++) {
                    track.points.add(new Point(level.points[i][0] >> 13, level.points[i][1] >> 13));
                }

                normalizeMarkers(track);
                pack.groups[group].add(track);
            }
        }

        return pack;
    }

    public static Pack read(InputStream input, String name, String author) throws IOException {
        Pack pack = read(input);
        pack.name = name == null ? "" : name;
        pack.author = author == null ? "" : author;
        return pack;
    }

    public static void write(Pack pack, OutputStream output) throws IOException {
        validate(pack);

        ArrayList<byte[]> records = new ArrayList<>();
        for (int group = 0; group < GROUPS; group++) {
            for (Track track : pack.groups[group]) {
                records.add(encodeTrack(track));
            }
        }

        int headerSize = GROUPS * 4;
        for (int group = 0; group < GROUPS; group++) {
            for (Track track : pack.groups[group]) {
                headerSize += 4 + encodeName(track.name).length + 1;
            }
        }

        DataOutputStream out = new DataOutputStream(output);
        int pointer = headerSize;
        int recordIndex = 0;

        for (int group = 0; group < GROUPS; group++) {
            out.writeInt(pack.groups[group].size());
            for (Track track : pack.groups[group]) {
                out.writeInt(pointer);
                byte[] name = encodeName(track.name);
                out.write(name);
                out.writeByte(0);
                pointer += records.get(recordIndex++).length;
            }
        }

        for (byte[] record : records) {
            out.write(record);
        }
        out.flush();
    }

    public static Pack createDefaultEditablePack() {
        Pack pack = new Pack();
        pack.name = "New levels";
        pack.author = "";
        for (int group = 0; group < 3; group++) {
            pack.groups[group].add(createDefaultTrack(group == 0 ? "New Track" : "New Track " + (group + 1)));
        }
        return pack;
    }

    public static Track createDefaultTrack(String name) {
        Track track = new Track(name);
        track.points.add(new Point(0, 0));
        track.points.add(new Point(20, 4));
        track.points.add(new Point(40, -2));
        track.points.add(new Point(60, 6));
        track.points.add(new Point(90, 0));
        track.startIndex = 0;
        track.finishIndex = track.points.size() - 1;
        track.startX = track.points.get(track.startIndex).x;
        track.finishX = track.points.get(track.finishIndex).x;
        track.startY = 15;
        track.finishY = track.points.get(track.finishIndex).y;
        return track;
    }

    public static void normalizeMarkers(Track track) {
        if (track.points.isEmpty()) {
            track.startIndex = 0;
            track.finishIndex = 0;
            return;
        }

        track.startIndex = findStartIndex(track);
        track.finishIndex = findFinishIndex(track);
        if (track.finishIndex <= track.startIndex) {
            track.finishIndex = track.points.size() - 1;
        }
        track.startX = track.points.get(track.startIndex).x;
        track.finishX = track.points.get(track.finishIndex).x;
    }

    private static int findStartIndex(Track track) {
        for (int i = 0; i < track.points.size(); i++) {
            if (track.points.get(i).x == track.startX) {
                return i;
            }
        }
        for (int i = 0; i < track.points.size(); i++) {
            if (track.points.get(i).x >= track.startX) {
                return i;
            }
        }
        return 0;
    }

    private static int findFinishIndex(Track track) {
        for (int i = track.points.size() - 1; i >= 0; i--) {
            if (track.points.get(i).x <= track.finishX) {
                return i;
            }
        }
        return track.points.size() - 1;
    }

    private static void validate(Pack pack) throws IOException {
        if (pack == null || pack.groups == null || pack.groups.length != GROUPS) {
            throw new IOException("Invalid level pack");
        }
        if (pack.name == null) {
            pack.name = "";
        }
        if (pack.author == null) {
            pack.author = "";
        }

        for (int group = 0; group < GROUPS; group++) {
            if (pack.groups[group] == null) {
                throw new IOException("Invalid track group");
            }
            if (pack.groups[group].size() > MAX_TRACKS) {
                throw new IOException("Too many tracks");
            }
            if (group < 3 && pack.groups[group].isEmpty()) {
                throw new IOException("Easy, Medium and Hard must each contain at least one track");
            }

            for (Track track : pack.groups[group]) {
                if (track == null || track.points == null || track.points.size() < 2) {
                    throw new IOException("Track must contain at least two points");
                }
                if (track.points.size() > MAX_POINTS) {
                    throw new IOException("Track has too many points");
                }
                if (track.name == null || track.name.trim().isEmpty()) {
                    track.name = "Untitled";
                }

                for (int i = 1; i < track.points.size(); i++) {
                    if (track.points.get(i).x <= track.points.get(i - 1).x) {
                        throw new IOException("Track points must have strictly increasing X coordinates");
                    }
                }

                validateCoordinate(track.startX);
                validateCoordinate(track.startY);
                validateCoordinate(track.finishX);
                validateCoordinate(track.finishY);
                for (Point point : track.points) {
                    validateCoordinate(point.x);
                    validateCoordinate(point.y);
                }

                if (track.startIndex < 0 || track.startIndex >= track.points.size()) {
                    track.startIndex = 0;
                }
                if (track.finishIndex < 0 || track.finishIndex >= track.points.size()) {
                    track.finishIndex = track.points.size() - 1;
                }
                if (track.finishIndex <= track.startIndex) {
                    throw new IOException("Finish must be after Start");
                }
                track.startX = track.points.get(track.startIndex).x;
                track.finishX = track.points.get(track.finishIndex).x;
            }
        }
    }

    private static byte[] encodeTrack(Track track) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);

        out.writeByte(0x33);
        out.writeInt(rawToFixed(track.startX));
        out.writeInt(rawToFixed(track.startY));
        out.writeInt(rawToFixed(track.finishX));
        out.writeInt(rawToFixed(track.finishY));
        out.writeShort(track.points.size());

        Point first = track.points.get(0);
        out.writeInt(first.x);
        out.writeInt(first.y);

        for (int i = 1; i < track.points.size(); i++) {
            Point previous = track.points.get(i - 1);
            Point point = track.points.get(i);
            int dx = point.x - previous.x;
            int dy = point.y - previous.y;

            if (dx >= -128 && dx <= 127 && dx != -1 && dy >= -128 && dy <= 127) {
                out.writeByte(dx);
                out.writeByte(dy);
            } else {
                out.writeByte(0xFF);
                out.writeInt(point.x);
                out.writeInt(point.y);
            }
        }

        out.flush();
        return bytes.toByteArray();
    }

    private static byte[] encodeName(String value) {
        String text = value == null ? "" : value.replace('\u0000', ' ').replace('\n', ' ').replace('\r', ' ');
        byte[] bytes = text.getBytes(CP1251);
        if (bytes.length <= 39) {
            return bytes;
        }
        byte[] truncated = new byte[39];
        System.arraycopy(bytes, 0, truncated, 0, 39);
        return truncated;
    }

    private static int fixedToRaw(int fixed) {
        return fixed >> 13;
    }

    private static int rawToFixed(int raw) throws IOException {
        long value = raw * 8192L;
        if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
            throw new IOException("Coordinate is out of range");
        }
        return (int) value;
    }

    private static void validateCoordinate(int value) throws IOException {
        if (Math.abs((long) value) > COORDINATE_LIMIT) {
            throw new IOException("Coordinate is out of range");
        }
    }
}
