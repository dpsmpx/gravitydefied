package org.happysanta.gd.Menu;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.InputType;
import android.widget.EditText;

import org.happysanta.gd.GDActivity;
import org.happysanta.gd.Levels.LevelPackEditor;
import org.happysanta.gd.Levels.LevelPackEditor.Pack;
import org.happysanta.gd.Levels.LevelPackEditor.Track;
import org.happysanta.gd.R;
import org.happysanta.gd.Storage.Level;
import org.happysanta.gd.Storage.LevelsManager;

import java.util.ArrayList;
import java.util.Locale;

import static org.happysanta.gd.Helpers.getGDActivity;
import static org.happysanta.gd.Helpers.getGameMenu;
import static org.happysanta.gd.Helpers.getStringArray;
import static org.happysanta.gd.Helpers.getString;
import static org.happysanta.gd.Helpers.showAlert;
import static org.happysanta.gd.Helpers.showConfirm;

public class MapEditorController {

    public final MenuScreen home;

    private final MenuScreen mainMenu;
    private final MenuScreen packListScreen;
    private final MenuScreen packScreen;
    private final MenuScreen trackScreen;
    private final MenuScreen libraryScreen;
    private final MenuScreen trackEditorScreen;
    private final LevelsManager levelsManager;

    private Pack workingPack;
    private long workingLevelId;
    private int workingGroup;
    private int workingTrack;
    private MapEditorView editorView;
    private String lastSearch = "";

    public MapEditorController(MenuScreen mainMenu) {
        this.mainMenu = mainMenu;
        this.levelsManager = getGDActivity().levelsManager;

        home = new MenuScreen(getString(R.string.map_editor), mainMenu);
        packListScreen = new MenuScreen(getString(R.string.editor_packs), home);
        packScreen = new MenuScreen(getString(R.string.editor_pack), packListScreen);
        trackScreen = new MenuScreen(getString(R.string.editor_track), packScreen);
        libraryScreen = new MenuScreen(getString(R.string.editor_track_library), packScreen);
        trackEditorScreen = new MenuScreen(getString(R.string.editor_track_editor), trackScreen);

        buildHome();
    }

    private void buildHome() {
        home.clear();
        home.setTitle(getString(R.string.map_editor));
        home.addItem(new EditorActionMenuElement(getString(R.string.editor_edit_pack), this::showPackList));
        home.addItem(new EditorActionMenuElement(getString(R.string.editor_edit_current), this::editCurrentPack));
        home.addItem(new EditorActionMenuElement(getString(R.string.editor_new_pack), this::newPack));
        home.addItem(new EditorActionMenuElement(getString(R.string.editor_download_mods), this::openMods));
        home.addItem(new EditorActionMenuElement(getString(R.string.install_mrg), this::importMrg));
        home.addItem(new ActionMenuElement(getString(R.string.back), ActionMenuElement.BACK, getGameMenu()));
    }

    private void showPackList() {
        packListScreen.clear();
        packListScreen.setTitle(getString(R.string.editor_packs));

        Level[] installed = levelsManager.getAllInstalledLevels();
        for (final Level level : installed) {
            String text = level.getName() + "  [" + counts(level) + "]";
            packListScreen.addItem(new EditorActionMenuElement(text, () -> editPack(level)));
        }

        packListScreen.addItem(new EditorActionMenuElement(getString(R.string.editor_new_pack), this::newPack));
        packListScreen.addItem(new ActionMenuElement(getString(R.string.back), ActionMenuElement.BACK, getGameMenu()));
        show(packListScreen);
    }

    private void editCurrentPack() {
        try {
            openPack(levelsManager.getCurrentLevel());
        } catch (Exception e) {
            showAlert(getString(R.string.error), message(e), null);
        }
    }

    private void editPack(Level level) {
        openPack(level);
    }

    private void openPack(Level level) {
        if (level == null) {
            showAlert(getString(R.string.error), getString(R.string.editor_no_pack), null);
            return;
        }

        try {
            workingPack = levelsManager.readPack(level);
            workingLevelId = level.getId();
            if (workingPack.name == null || workingPack.name.isEmpty()) {
                workingPack.name = level.getName();
            }
            if (workingPack.author == null || workingPack.author.isEmpty()) {
                workingPack.author = level.getAuthor();
            }
            showPack();
        } catch (Exception e) {
            showAlert(getString(R.string.error), message(e), null);
        }
    }

    private void newPack() {
        workingPack = LevelPackEditor.createDefaultEditablePack();
        workingLevelId = 0;
        showPack();
    }

    private void showPack() {
        if (workingPack == null) {
            showPackList();
            return;
        }

        packScreen.clear();
        packScreen.setTitle(safe(workingPack.name));
        packScreen.addItem(new BigTextMenuElement(
                getString(R.string.editor_pack_info) + ": " + safe(workingPack.name) + "\n" +
                getString(R.string.editor_author) + ": " + safe(workingPack.author) + "\n" +
                getString(R.string.editor_tracks) + ": " + counts(workingPack)
        ));
        packScreen.addItem(new EditorActionMenuElement(getString(R.string.editor_rename_pack), this::renamePack));
        packScreen.addItem(new EditorActionMenuElement(getString(R.string.editor_edit_author), this::renameAuthor));
        packScreen.addItem(new EditorActionMenuElement(getString(R.string.editor_add_track), this::addTrack));

        String[] groups = getStringArray(R.array.difficulty);
        for (int group = 0; group < LevelPackEditor.GROUPS; group++) {
            packScreen.addItem(new TextMenuElement(groups[group]));
            final int groupIndex = group;
            for (int i = 0; i < workingPack.groups[group].size(); i++) {
                final int trackIndex = i;
                Track track = workingPack.groups[group].get(i);
                packScreen.addItem(new EditorActionMenuElement(
                        (trackIndex + 1) + ". " + safe(track.name),
                        () -> showTrack(groupIndex, trackIndex)
                ));
            }
        }

        packScreen.addItem(new EditorActionMenuElement(getString(R.string.editor_track_library), this::openLibrary));
        packScreen.addItem(new EditorActionMenuElement(getString(R.string.editor_save_pack), () -> savePack(false)));
        packScreen.addItem(new EditorActionMenuElement(getString(R.string.editor_use_pack), this::usePack));
        packScreen.addItem(new EditorActionMenuElement(getString(R.string.back), () -> show(home)));
        show(packScreen);
    }

    private void showTrack(int group, int trackIndex) {
        if (workingPack == null ||
                group < 0 || group >= LevelPackEditor.GROUPS ||
                trackIndex < 0 || trackIndex >= workingPack.groups[group].size()) {
            showPack();
            return;
        }

        workingGroup = group;
        workingTrack = trackIndex;
        Track track = workingPack.groups[group].get(trackIndex);

        trackScreen.clear();
        trackScreen.setTitle(safe(track.name));
        trackScreen.addItem(new BigTextMenuElement(
                getString(R.string.editor_track_points) + ": " + track.points.size() + "\n" +
                getString(R.string.editor_start_finish) + ": " +
                (track.startIndex + 1) + " / " + (track.finishIndex + 1)
        ));
        trackScreen.addItem(new EditorActionMenuElement(getString(R.string.editor_edit_track), this::openTrackEditor));
        trackScreen.addItem(new EditorActionMenuElement(getString(R.string.editor_rename_track), this::renameTrack));
        trackScreen.addItem(new EditorActionMenuElement(getString(R.string.editor_move_track), this::moveTrack));
        trackScreen.addItem(new EditorActionMenuElement(getString(R.string.editor_move_up), () -> moveTrackOrder(-1)));
        trackScreen.addItem(new EditorActionMenuElement(getString(R.string.editor_move_down), () -> moveTrackOrder(1)));
        trackScreen.addItem(new EditorActionMenuElement(getString(R.string.editor_duplicate_track), this::duplicateTrack));
        trackScreen.addItem(new EditorActionMenuElement(getString(R.string.editor_delete_track), this::deleteTrack));
        trackScreen.addItem(new EditorActionMenuElement(getString(R.string.back), () -> show(packScreen)));
        show(trackScreen);
    }

    private void openTrackEditor() {
        final Track track = workingPack.groups[workingGroup].get(workingTrack);
        editorView = new MapEditorView(getGDActivity(), track);
        editorView.setMinimumHeight(dp(480));

        trackEditorScreen.clear();
        trackEditorScreen.setTitle(safe(track.name));
        trackEditorScreen.addItem(new TextMenuElement(getString(R.string.editor_canvas_hint)));
        trackEditorScreen.addItem(new EditorViewMenuElement(editorView));
        trackEditorScreen.addItem(new EditorActionMenuElement(getString(R.string.editor_select_mode), () -> setEditorMode(MapEditorView.MODE_SELECT)));
        trackEditorScreen.addItem(new EditorActionMenuElement(getString(R.string.editor_add_mode), () -> setEditorMode(MapEditorView.MODE_ADD)));
        trackEditorScreen.addItem(new EditorActionMenuElement(getString(R.string.editor_delete_mode), () -> setEditorMode(MapEditorView.MODE_DELETE)));
        trackEditorScreen.addItem(new EditorActionMenuElement(getString(R.string.editor_set_start), this::setStartMarker));
        trackEditorScreen.addItem(new EditorActionMenuElement(getString(R.string.editor_set_finish), this::setFinishMarker));
        trackEditorScreen.addItem(new EditorActionMenuElement(getString(R.string.editor_zoom_in), () -> editorView.zoomBy(1.35f)));
        trackEditorScreen.addItem(new EditorActionMenuElement(getString(R.string.editor_zoom_out), () -> editorView.zoomBy(0.74f)));
        trackEditorScreen.addItem(new EditorActionMenuElement(getString(R.string.editor_fit), editorView::fitTrack));
        trackEditorScreen.addItem(new EditorActionMenuElement(getString(R.string.editor_save_track), () -> showTrack(workingGroup, workingTrack)));
        trackEditorScreen.addItem(new EditorActionMenuElement(getString(R.string.back), () -> show(trackScreen)));
        show(trackEditorScreen);
    }

    private void setEditorMode(int mode) {
        if (editorView != null) {
            editorView.setMode(mode);
        }
    }

    private void setStartMarker() {
        if (editorView == null || editorView.getSelectedIndex() < 0) {
            showAlert(getString(R.string.editor_set_start), getString(R.string.editor_select_point_first), null);
            return;
        }
        Track track = workingPack.groups[workingGroup].get(workingTrack);
        int selected = editorView.getSelectedIndex();
        if (selected >= track.finishIndex) {
            showAlert(getString(R.string.editor_set_start), getString(R.string.editor_start_before_finish), null);
            return;
        }
        track.startIndex = selected;
        track.startX = track.points.get(selected).x;
        showTrackEditorAgain();
    }

    private void setFinishMarker() {
        if (editorView == null || editorView.getSelectedIndex() < 0) {
            showAlert(getString(R.string.editor_set_finish), getString(R.string.editor_select_point_first), null);
            return;
        }
        Track track = workingPack.groups[workingGroup].get(workingTrack);
        int selected = editorView.getSelectedIndex();
        if (selected <= track.startIndex) {
            showAlert(getString(R.string.editor_set_finish), getString(R.string.editor_finish_after_start), null);
            return;
        }
        track.finishIndex = selected;
        track.finishX = track.points.get(selected).x;
        showTrackEditorAgain();
    }

    private void showTrackEditorAgain() {
        openTrackEditor();
    }

    private void renamePack() {
        askText(getString(R.string.editor_rename_pack), workingPack.name, value -> {
            workingPack.name = value;
            showPack();
        });
    }

    private void renameAuthor() {
        askText(getString(R.string.editor_edit_author), workingPack.author, value -> {
            workingPack.author = value;
            showPack();
        });
    }

    private void renameTrack() {
        Track track = workingPack.groups[workingGroup].get(workingTrack);
        askText(getString(R.string.editor_rename_track), track.name, value -> {
            track.name = value;
            showTrack(workingGroup, workingTrack);
        });
    }

    private void addTrack() {
        chooseGroup(group -> {
            if (workingPack.groups[group].size() >= 16384) {
                showAlert(getString(R.string.error), getString(R.string.editor_too_many_tracks), null);
                return;
            }
            Track track = LevelPackEditor.createDefaultTrack(uniqueTrackName(getString(R.string.editor_new_track)));
            workingPack.groups[group].add(track);
            showTrack(group, workingPack.groups[group].size() - 1);
        });
    }

    private void moveTrack() {
        chooseGroup(group -> {
            if (group == workingGroup) {
                return;
            }

            if (workingPack.groups[group].size() >= 16384) {
                showAlert(getString(R.string.error), getString(R.string.editor_too_many_tracks), null);
                return;
            }

            if (workingGroup < 3 && workingPack.groups[workingGroup].size() <= 1) {
                showAlert(getString(R.string.error), getString(R.string.editor_cannot_delete_last), null);
                return;
            }

            Track track = workingPack.groups[workingGroup].remove(workingTrack);
            workingPack.groups[group].add(track);
            showTrack(group, workingPack.groups[group].size() - 1);
        });
    }

    private void moveTrackOrder(int direction) {
        ArrayList<Track> tracks = workingPack.groups[workingGroup];
        int target = workingTrack + direction;
        if (target < 0 || target >= tracks.size()) {
            return;
        }

        Track temp = tracks.get(workingTrack);
        tracks.set(workingTrack, tracks.get(target));
        tracks.set(target, temp);
        workingTrack = target;
        showTrack(workingGroup, workingTrack);
    }

    private void duplicateTrack() {
        ArrayList<Track> tracks = workingPack.groups[workingGroup];
        if (tracks.size() >= 16384) {
            showAlert(getString(R.string.error), getString(R.string.editor_too_many_tracks), null);
            return;
        }

        Track copy = tracks.get(workingTrack).copy();
        copy.name = uniqueTrackName(copy.name + " Copy");
        tracks.add(workingTrack + 1, copy);
        showTrack(workingGroup, workingTrack + 1);
    }

    private void deleteTrack() {
        if (workingPack.groups[workingGroup].size() <= (workingGroup < 3 ? 1 : 0)) {
            showAlert(getString(R.string.error), getString(R.string.editor_cannot_delete_last), null);
            return;
        }

        showConfirm(
                getString(R.string.editor_delete_track),
                getString(R.string.editor_delete_confirmation),
                () -> {
                    workingPack.groups[workingGroup].remove(workingTrack);
                    showPack();
                },
                null
        );
    }

    private void openLibrary() {
        show(libraryScreen);
        searchTracks();
    }

    private void searchTracks() {
        askText(getString(R.string.editor_search_title), lastSearch, value -> {
            lastSearch = value.trim();
            renderLibrary();
        });
    }

    private void renderLibrary() {
        libraryScreen.clear();
        libraryScreen.setTitle(getString(R.string.editor_track_library));
        libraryScreen.addItem(new BigTextMenuElement(
                getString(R.string.editor_library_hint) + "\n" +
                getString(R.string.editor_search_for) + ": " + safe(lastSearch)
        ));

        ArrayList<TrackRef> results = findTracks(lastSearch);
        if (results.isEmpty()) {
            libraryScreen.addItem(new TextMenuElement(getString(R.string.editor_no_tracks_found)));
        } else {
            for (final TrackRef result : results) {
                libraryScreen.addItem(new EditorActionMenuElement(
                        result.packName + " / " + result.difficulty + " / " + result.track.name,
                        () -> addLibraryTrack(result)
                ));
            }
        }

        libraryScreen.addItem(new EditorActionMenuElement(getString(R.string.editor_new_search), this::searchTracks));
        libraryScreen.addItem(new EditorActionMenuElement(getString(R.string.editor_download_mods), this::openModsFromLibrary));
        libraryScreen.addItem(new EditorActionMenuElement(getString(R.string.install_mrg), this::importMrg));
        libraryScreen.addItem(new EditorActionMenuElement(getString(R.string.back), () -> show(packScreen)));
    }

    private ArrayList<TrackRef> findTracks(String query) {
        String needle = query == null ? "" : query.toLowerCase(Locale.ROOT);
        ArrayList<TrackRef> result = new ArrayList<>();
        String[] difficulty = getStringArray(R.array.difficulty);

        if (workingPack != null) {
            for (int group = 0; group < LevelPackEditor.GROUPS; group++) {
                for (int i = 0; i < workingPack.groups[group].size(); i++) {
                    Track track = workingPack.groups[group].get(i);
                    if (needle.isEmpty() || track.name.toLowerCase(Locale.ROOT).contains(needle)) {
                        result.add(new TrackRef(
                                workingPack.name,
                                difficulty[group],
                                track,
                                group,
                                i
                        ));
                    }
                }
            }
        }

        Level[] packs = levelsManager.getAllInstalledLevels();
        for (Level pack : packs) {
            if (pack.getId() == workingLevelId) {
                continue;
            }

            try {
                Pack source = levelsManager.readPack(pack);
                for (int group = 0; group < LevelPackEditor.GROUPS; group++) {
                    for (int i = 0; i < source.groups[group].size(); i++) {
                        Track track = source.groups[group].get(i);
                        if (needle.isEmpty() || track.name.toLowerCase(Locale.ROOT).contains(needle)) {
                            result.add(new TrackRef(pack.getName(), difficulty[group], track, group, i));
                            if (result.size() >= 100) {
                                return result;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return result;
    }

    private void addLibraryTrack(TrackRef ref) {
        if (ref == null || ref.track == null || workingPack == null) {
            return;
        }

        chooseGroup(group -> {
            if (workingPack.groups[group].size() >= 16384) {
                showAlert(getString(R.string.error), getString(R.string.editor_too_many_tracks), null);
                return;
            }
            Track copy = ref.track.copy();
            copy.name = uniqueTrackName(copy.name);
            workingPack.groups[group].add(copy);
            showTrack(group, workingPack.groups[group].size() - 1);
        });
    }

    private String uniqueTrackName(String name) {
        String base = name == null || name.isEmpty() ? getString(R.string.editor_new_track) : name;
        String candidate = base;
        int counter = 2;

        while (containsTrackName(candidate)) {
            candidate = base + " (" + counter + ")";
            counter++;
        }
        return candidate;
    }

    private boolean containsTrackName(String name) {
        for (int group = 0; group < LevelPackEditor.GROUPS; group++) {
            for (Track track : workingPack.groups[group]) {
                if (track.name.equalsIgnoreCase(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void openMods() {
        getGameMenu().setCurrentMenu(getGameMenu().managerDownloadScreen, false);
    }

    private void openModsFromLibrary() {
        getGameMenu().setCurrentMenu(getGameMenu().managerDownloadScreen, false);
    }

    private void importMrg() {
        getGDActivity().openMrgFilePicker();
    }

    private void savePack(boolean thenLoad) {
        if (workingPack == null) {
            return;
        }

        try {
            long savedId = levelsManager.saveEditedPack(workingLevelId, workingPack);
            workingLevelId = savedId;

            if (thenLoad) {
                Level saved = levelsManager.getLeveL(savedId);
                if (saved == null) {
                    showAlert(getString(R.string.error), getString(R.string.editor_no_pack), null);
                } else {
                    levelsManager.load(saved);
                }
            } else if (savedId == levelsManager.getCurrentId()) {
                showAlert(
                        getString(R.string.editor_saved),
                        getString(R.string.editor_saved_restart),
                        () -> getGDActivity().restartApp()
                );
            } else {
                showAlert(getString(R.string.editor_saved), getString(R.string.editor_saved_text), this::showPack);
            }
        } catch (Exception e) {
            showAlert(getString(R.string.error), message(e), null);
        }
    }

    private void usePack() {
        if (workingLevelId == 0) {
            savePack(true);
            return;
        }

        Level saved = levelsManager.getLeveL(workingLevelId);
        if (saved != null) {
            levelsManager.load(saved);
        }
    }

    private void askText(String title, String value, final TextCallback callback) {
        GDActivity activity = getGDActivity();
        activity.runOnUiThread(() -> {
            final EditText input = new EditText(activity);
            input.setSingleLine(true);
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            input.setText(value == null ? "" : value);
            input.setSelection(input.length());

            new AlertDialog.Builder(activity)
                    .setTitle(title)
                    .setView(input)
                    .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                        String text = input.getText().toString().trim();
                        if (text.isEmpty()) {
                            text = getString(R.string.editor_new_track);
                        }
                        callback.onText(text);
                    })
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();

            input.requestFocus();
        });
    }

    private void chooseGroup(final GroupCallback callback) {
        final String[] groups = getStringArray(R.array.difficulty);
        new AlertDialog.Builder(getGDActivity())
                .setTitle(getString(R.string.editor_choose_difficulty))
                .setSingleChoiceItems(groups, -1, (dialog, which) -> {
                    dialog.dismiss();
                    callback.onGroup(which);
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void show(MenuScreen screen) {
        getGameMenu().setCurrentMenu(screen, false);
    }

    private String counts(Level level) {
        return level.getCountEasy() + " / " + level.getCountMedium() + " / " +
                level.getCountHard() + " / " + level.getCountEndless();
    }

    private String counts(Pack pack) {
        return pack.groups[0].size() + " / " + pack.groups[1].size() + " / " +
                pack.groups[2].size() + " / " + pack.groups[3].size();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private int dp(int value) {
        return Math.round(value * getGDActivity().getResources().getDisplayMetrics().density);
    }

    private String message(Throwable throwable) {
        return throwable == null || throwable.getMessage() == null
                ? getString(R.string.error)
                : throwable.getMessage();
    }

    private interface TextCallback {
        void onText(String value);
    }

    private interface GroupCallback {
        void onGroup(int group);
    }

    private static final class TrackRef {
        final String packName;
        final String difficulty;
        final Track track;
        final int group;
        final int trackIndex;

        TrackRef(String packName, String difficulty, Track track, int group, int trackIndex) {
            this.packName = packName;
            this.difficulty = difficulty;
            this.track = track;
            this.group = group;
            this.trackIndex = trackIndex;
        }
    }
}
