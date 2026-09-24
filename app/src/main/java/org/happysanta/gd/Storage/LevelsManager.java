package org.happysanta.gd.Storage;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.StatFs;
import org.happysanta.gd.API.API;
import org.happysanta.gd.API.DownloadFile;
import org.happysanta.gd.API.DownloadHandler;
import org.happysanta.gd.Callback;
import org.happysanta.gd.DoubleCallback;
import org.happysanta.gd.GDActivity;
import org.happysanta.gd.Global;
import org.happysanta.gd.Levels.LevelHeader;
import org.happysanta.gd.Levels.LevelPackEditor;
import org.happysanta.gd.Levels.Reader;
import org.happysanta.gd.Menu.Menu;
import org.happysanta.gd.Menu.MenuScreen;
import org.happysanta.gd.R;
import org.happysanta.gd.Settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import static org.happysanta.gd.Helpers.getGDActivity;
import static org.happysanta.gd.Helpers.getGameMenu;
import static org.happysanta.gd.Helpers.getString;
import static org.happysanta.gd.Helpers.getTimestamp;
import static org.happysanta.gd.Helpers.isOnline;
import static org.happysanta.gd.Helpers.logDebug;
import static org.happysanta.gd.Helpers.showAlert;

public class LevelsManager {

	private LevelsDataSource dataSource;
	private boolean dbOK = false;
	private Level currentLevel;

	public LevelsManager() {
		GDActivity gd = getGDActivity();
		dataSource = new LevelsDataSource(gd);

		try {
			dataSource.open();

			if (!dataSource.isDefaultLevelCreated()) {
				Level level = dataSource.createLevel("GDTR original", "Codebrew Software", 10, 10, 10, 1, 0, 0, true, 1);
				logDebug("LevelsManager: Default level created!");
				logDebug(level);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			logDebug("LevelsManager: db feels bad :(");
			// return;
		}

		logDebug("LevelsManager: db feels OK :)");

		// Shared prefs
		// SharedPreferences settings = getSharedPreferences();
		// long levelId = settings.getLong(PREFS_LEVEL_ID, 0);
		long levelId = Settings.getLevelId();
		if (levelId < 1 || !mrgIsAvailable(levelId)) {
			logDebug("LevelsManager: levelId = " + levelId + ", < 1 or mrg is not available; now: reset id");
			/*SharedPreferences.Editor editor = settings.edit();
			editor.putLong(PREFS_LEVEL_ID, 1);
			editor.commit();*/
			resetId();
		}

		reload();
		dbOK = true;
	}

	public void resetId() {
		Settings.setLevelId(1);
	}

	public void reload() {
		long id = Settings.getLevelId();
		currentLevel = dataSource.getLevel(id);

		if (currentLevel == null) {
			logDebug("LevelsManager: failed to load currentLevel; currentId = " + id);
		} else {
			logDebug("LevelsManager: level = " + currentLevel);
		}

	}

	public void closeDataSource() {
		dataSource.close();
	}

	public long getCurrentId() {
		return currentLevel.getId();
	}

	public void setCurrentId(long id) {
		// currentId = id;
		Settings.setLevelId(id);
		/*SharedPreferences settings = getSharedPreferences();
		SharedPreferences.Editor edit = settings.edit();
		edit.putLong(PREFS_LEVEL_ID, id);
		edit.commit();*/
	}

	public Level getCurrentLevel() {
		return currentLevel;
	}

	private static final String DEFAULT_EDITED_MRG_NAME = "default-edited.mrg";

	public File getCurrentLevelsFile() {
		if (currentLevel.getId() > 1)
			return getMrgFileById(currentLevel.getId());

		File edited = getDefaultEditedLevelsFile();
		return edited.isFile() && edited.canRead() ? edited : null;
	}

	public File getDefaultEditedLevelsFile() {
		return new File(getLevelsDirectory(), DEFAULT_EDITED_MRG_NAME);
	}

	public LevelPackEditor.Pack readPack(Level level) throws Exception {
		if (level == null) {
			throw new IOException("No level pack");
		}

		if (level.getId() == 1) {
			File edited = getDefaultEditedLevelsFile();
			if (edited.isFile() && edited.canRead()) {
				try (InputStream in = new FileInputStream(edited)) {
					return LevelPackEditor.read(in, level.getName(), level.getAuthor());
				}
			}
			try (InputStream in = getGDActivity().getAssets().open("levels.mrg")) {
				return LevelPackEditor.read(in, level.getName(), level.getAuthor());
			}
		}

		File file = getMrgFileById(level.getId());
		if (file == null || !file.isFile() || !file.canRead()) {
			throw new IOException("Unable to read level pack");
		}
		try (InputStream in = new FileInputStream(file)) {
			return LevelPackEditor.read(in, level.getName(), level.getAuthor());
		}
	}

	public long saveEditedPack(long levelId, LevelPackEditor.Pack pack) throws Exception {
		if (levelId <= 0) {
			File temp = File.createTempFile("gravitydefied-editor-", ".mrg", getGDActivity().getCacheDir());
			try {
				try (OutputStream out = new FileOutputStream(temp)) {
					LevelPackEditor.write(pack, out);
				}
				return install(temp, pack.name, pack.author, 0);
			} finally {
				if (temp.exists()) {
					temp.delete();
				}
			}
		}

		File target = levelId == 1 ? getDefaultEditedLevelsFile() : getMrgFileById(levelId);
		if (target == null) {
			throw new IOException("Unable to save level pack");
		}

		File parent = target.getParentFile();
		if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
			throw new IOException("Unable to create level directory");
		}

		File temp = new File(target.getAbsolutePath() + ".tmp");
		try {
			try (OutputStream out = new FileOutputStream(temp)) {
				LevelPackEditor.write(pack, out);
			}
			if (target.exists() && !target.delete()) {
				throw new IOException("Unable to replace level pack");
			}
			if (!temp.renameTo(target)) {
				throw new IOException("Unable to finalize level pack");
			}

			Level stored = dataSource.getLevel(levelId);
			if (stored == null) {
				throw new IOException("Level metadata was not found");
			}
			stored.setName(pack.name);
			stored.setAuthor(pack.author);
			stored.setCount(
					pack.groups[0].size(),
					pack.groups[1].size(),
					pack.groups[2].size(),
					pack.groups[3].size()
			);
			stored.setSize((int) target.length());
			stored.setUnlocked(0, 0, -1, 0);
			stored.setUnlockedLevels(1);
			stored.setUnlockedLeagues(0);
			if (stored.getSelectedLevel() >= LevelPackEditor.GROUPS) {
				stored.setSelectedLevel(0);
			}
			int selectedGroup = stored.getSelectedLevel();
			if (pack.groups[selectedGroup].isEmpty()) {
				stored.setSelectedLevel(0);
				selectedGroup = 0;
			}
			if (stored.getSelectedTrack() >= pack.groups[selectedGroup].size()) {
				stored.setSelectedTrack(0);
			}
			dataSource.updateLevelMetadata(stored);
			dataSource.updateLevel(stored);
			dataSource.clearHighScores(levelId);

			if (currentLevel != null && currentLevel.getId() == levelId) {
				currentLevel = stored;
			}
			return levelId;
		} finally {
			if (temp.exists()) {
				temp.delete();
			}
		}
	}

	private boolean mrgIsAvailable(long id) {
		if (id == 1) // This is default built-in levels.mrg
			return true;

		File file = getMrgFileById(id);
		return file != null && file.isFile() && file.canRead();
	}

	public boolean isDbOK() {
		return dbOK;
	}

	public long install(File file, String name, String author, long apiId) throws Exception {
		if (file == null || !file.isFile() || !file.canRead()) {
			throw new IOException("Unable to read " + (file == null ? "level file" : file.getAbsolutePath()));
		}
		if (!isSpaceAvailable(file.length())) {
			throw new Exception(getString(R.string.e_no_space_left));
		}

		try (InputStream inputStream = new FileInputStream(file)) {
			LevelHeader header = Reader.readHeader(inputStream);
			if (!header.isCountsOk()) {
				throw new IOException(file.getName() + " is not valid");
			}
		}

		LevelHeader header;
		try (InputStream inputStream = new FileInputStream(file)) {
			header = Reader.readHeader(inputStream);
		}
		Level level = dataSource.createLevel(
				name,
				author,
				header.getCount(0),
				header.getCount(1),
				header.getCount(2),
				header.getCount(3),
				0,
				getTimestamp(),
				false,
				apiId
		);
		long id = level.getId();
		if (id < 1) {
			throw new Exception(getString(R.string.e_cannot_save_level));
		}

		File newFile = getMrgFileById(id);
		try {
			copy(file, newFile);
		} catch (Exception e) {
			dataSource.deleteLevel(level);
			if (newFile != null && newFile.exists()) {
				newFile.delete();
			}
			throw e;
		}
		return id;
	}

	public long install(Uri uri, String name, String author, long apiId) throws Exception {
		if (uri == null) {
			throw new IOException("No level file selected");
		}

		File temp = File.createTempFile("gravitydefied-import-", ".mrg", getGDActivity().getCacheDir());
		try {
			InputStream input = getGDActivity().getContentResolver().openInputStream(uri);
			if (input == null) {
				throw new IOException("Unable to open selected file");
			}
			try (InputStream in = input; OutputStream out = new FileOutputStream(temp)) {
				byte[] buffer = new byte[8192];
				int count;
				while ((count = in.read(buffer)) != -1) {
					out.write(buffer, 0, count);
				}
			}
			return install(temp, name, author, apiId);
		} finally {
			if (temp.exists()) {
				temp.delete();
			}
		}
	}

	public void installAsync(Uri uri, String name, String author, long apiId, final DoubleCallback callback) {
		GDActivity gd = getGDActivity();
		final ProgressDialog progressDialog = ProgressDialog.show(gd, getString(R.string.install), getString(R.string.installing), true);

		new AsyncInstallLevel() {
			@Override
			protected void onPostExecute(Object result) {
				progressDialog.dismiss();

				if (result instanceof Throwable) {
					Throwable throwable = (Throwable) result;
					throwable.printStackTrace();
					showAlert(getString(R.string.error), throwable.getMessage(), null);
					if (callback != null)
						callback.onFail();
					return;
				}

				if (callback != null)
					callback.onDone((long) result);
			}
		}.execute(uri, name, author, apiId);
	}

	public void installAsync(File file, String name, String author, long apiId, final DoubleCallback callback) {
		GDActivity gd = getGDActivity();
		final ProgressDialog progressDialog = ProgressDialog.show(gd, getString(R.string.install), getString(R.string.installing), true);

		new AsyncInstallLevel() {
			@Override
			protected void onPostExecute(Object result) {
				progressDialog.dismiss();

				if (result instanceof Throwable) {
					Throwable throwable = (Throwable) result;
					throwable.printStackTrace();
					showAlert(getString(R.string.error), throwable.getMessage(), null);
					if (callback != null)
						callback.onFail();
					return;
				}

				if (callback != null)
					callback.onDone((long) result);
			}
		}.execute(file, name, author, apiId);
	}

	public void load(Level level) throws RuntimeException {
		/*File file = getMrgFileById(level.getId());
		if (!mrgIsAvailable(level.getId())) {
			throw new RuntimeException("Unable to load levels \"" +level.getName() + "\"");
		}*/

		// Loader loader = getLevelLoader();
		// Menu menu = getGameMenu();

		// loader.setLevelsFile(file);
		// menu.reloadLevels();

		setCurrentId(level.getId());
		getGDActivity().restartApp();
	}

	public boolean isApiIdInstalled(long apiId) {
		return dataSource.isApiIdInstalled(apiId);
	}

	public Level[] getInstalledLevels(int offset, int count) {
		return dataSource.getLevels(offset, count).toArray(new Level[0]);
	}

	public Level getLeveL(long id) {
		return dataSource.getLevel(id);
	}

	public Level[] getAllInstalledLevels() {
		return dataSource.getAllLevels().toArray(new Level[0]);
	}

	public synchronized HashMap<String, Double> getLevelsStat() {
		Level[] levels = getAllInstalledLevels();
		HashMap<String, Double> stat = new HashMap<>();
		if (levels.length > 0) {
			for (Level level : levels) {
				int[] completed = level.getUnlockedAll();
				int completedCount = 0;
				for (int i = 0; i < completed.length; i++) {
					if (completed[i] < 0) completed[i] = 0;
					completedCount += completed[i];
				}

				double totalCount = level.getCountEasy() + level.getCountMedium() + level.getCountHard() + level.getCountEndless();
				double per = completedCount / totalCount * 100;

				stat.put(String.valueOf(level.getApiId()), per);
			}
		}
		return stat;
	}

	public void delete(Level level) {
		dataSource.deleteLevel(level);
		File file = getMrgFileById(level.getId());
		try {
			if (file.exists()) {
				file.delete();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void deleteAsync(Level level, final Runnable callback) {
		GDActivity gd = getGDActivity();
		final ProgressDialog progressDialog = ProgressDialog.show(gd, getString(R.string.delete), getString(R.string.deleting), true);

		new AsyncDeleteLevel() {
			@Override
			protected void onPostExecute(Void v) {
				progressDialog.dismiss();
				if (callback != null)
					callback.run();
			}
		}.execute(level);
	}

	public void updateLevelSettings() {
		dataSource.updateLevel(currentLevel);
	}

	public void downloadLevel(final Level level, final Callback successCallback) {
		final GDActivity gd = getGDActivity();
		File outputDir = gd.getCacheDir();

		try {
			boolean readable = isExternalStorageReadable();
			if (!readable) {
				throw new Exception(getString(R.string.e_external_storage_is_not_readable));
			}

			if (!isOnline()) {
				throw new Exception(getString(R.string.e_no_network_connection));
			}

			if (!isSpaceAvailable(level.getSize())) {
				throw new Exception(getString(R.string.e_no_space_left));
			}

			final File outputFile = File.createTempFile("levels" + level.getApiId(), "mrg", outputDir);
			FileOutputStream out = new FileOutputStream(outputFile);

			// logDebug("downloadLevel: 4");
			// final API api = new API();
			final ProgressDialog progress;
			final DownloadFile downloadFile = new DownloadFile(API.getMrgURL(level.getApiId()), out);

			progress = new ProgressDialog(gd);
			progress.setMessage(getString(R.string.downloading));
			progress.setIndeterminate(true);
			progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progress.setCancelable(true);

			final DownloadHandler handler = new DownloadHandler() {
				@Override
				public void onFinish(Throwable error) {
					progress.dismiss();

					if (error != null) {
						// error.printStackTrace();
						error.printStackTrace();
						showAlert(getString(R.string.error), error.getMessage(), null);

						outputFile.delete();
						return;
					}

					// Install
					installAsync(outputFile, level.getName(), level.getAuthor(), level.getApiId(), new DoubleCallback() {
						@Override
						public void onDone(Object... objects) {
							long id = (long) objects[0];
							outputFile.delete();

							if (successCallback != null)
								successCallback.onDone(id);
						}

						@Override
						public void onFail() {
							outputFile.delete();
						}
					});
				}

				@Override
				public void onStart() {
					progress.show();
				}

				@Override
				public void onProgress(int pr) {
					progress.setIndeterminate(false);
					progress.setMax(100);
					progress.setProgress(pr);
				}
			};
			progress.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					downloadFile.cancel();
					handler.onFinish(new InterruptedException(getString(R.string.e_downloading_was_interrupted)));
				}
			});

			downloadFile.setDownloadHandler(handler);
			downloadFile.start();
		} catch (Exception e) {
			showAlert(getString(R.string.error), e.getMessage(), null);
		}
	}

	public void showSuccessfullyInstalledDialog() {
		GDActivity gd = getGDActivity();
		AlertDialog success = new AlertDialog.Builder(gd)
				.setTitle(getString(R.string.installed))
				.setMessage(getString(R.string.successfully_installed))
				.setPositiveButton(getString(R.string.ok), null)
				.setNegativeButton(getString(R.string.open_installed), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Menu menu = getGameMenu();
						MenuScreen currentMenu = getGameMenu().getCurrentMenu(),
								newMenu = menu.managerInstalledScreen;

						if (currentMenu == menu.managerDownloadScreen || currentMenu.getNavTarget() == menu.managerDownloadScreen) {
							menu.managerDownloadScreen.onHide(menu.managerScreen);
						}

						menu.setCurrentMenu(newMenu, false);
					}
				})
				.create();
		success.show();
	}

	public HashMap<Long, Long> findInstalledLevels(ArrayList<Long> apiIds) {
		return dataSource.findInstalledLevels(apiIds);
	}

	public HighScores getHighScores(int level, int track) {
		HighScores scores = dataSource.getHighScores(currentLevel.getId(), level, track);
		// logDebug("LevelsManager.getHighScores: " + scores);
		return scores;
	}

	public void saveHighScores(HighScores scores) {
		dataSource.updateHighScores(scores);
	}

	public void clearHighScores() {
		dataSource.clearHighScores(currentLevel.getId());
	}

	public void clearAllHighScores() {
		dataSource.clearHighScores(0);
	}

	public void resetAllLevelsSettings() {
		dataSource.resetAllLevelsSettings();
		deleteDefaultEditedLevels();

		Level defaultLevel = dataSource.getLevel(1);
		if (defaultLevel != null) {
			defaultLevel.setName("GDTR original");
			defaultLevel.setAuthor("Codebrew Software");
			defaultLevel.setCount(10, 10, 10, 1);
			defaultLevel.setSize(8105);
			dataSource.updateLevelMetadata(defaultLevel);
			dataSource.updateLevel(defaultLevel);
		}

		logDebug("All levels now: " + dataSource.getAllLevels());
		logDebug("Level#1: " + dataSource.getLevel(1));
	}

	private void deleteDefaultEditedLevels() {
		File file = getDefaultEditedLevelsFile();
		if (file.isFile() && !file.delete()) {
			logDebug("LevelsManager: unable to delete default edited level file");
		}
	}

	private static final String LEVELS_DIRECTORY_NAME = "GDLevels";

	public static boolean isExternalStorageWritable() {
		return getLevelsDirectory().canWrite();
	}

	public static boolean isExternalStorageReadable() {
		return getLevelsDirectory().canRead();
	}

	public static File getLevelsDirectory() {
		GDActivity activity = getGDActivity();
		File base = activity.getExternalFilesDir(null);
		if (base == null) {
			base = activity.getFilesDir();
		}
		File file = new File(base, LEVELS_DIRECTORY_NAME);
		if (!file.exists() && !file.mkdirs() && !file.isDirectory()) {
			logDebug("LevelsManager.getLevelsDirectory: directory not created");
		}
		return file;
	}

	public static String getMrgFileNameById(long id) {
		return new File(getLevelsDirectory(), id + ".mrg").getAbsolutePath();
	}

	public static File getMrgFileById(long id) {
		if (id == 1) return null;
		return new File(getMrgFileNameById(id));
	}

	public static void copy(File src, File dst) throws IOException {
		try (InputStream in = new FileInputStream(src)) {
			copy(in, dst);
		}
	}

	private static void copy(InputStream in, File dst) throws IOException {
		if (dst == null) {
			throw new IOException("Invalid destination");
		}
		File parent = dst.getParentFile();
		if (parent != null && !parent.exists() && !parent.mkdirs()) {
			throw new IOException("Unable to create destination directory");
		}
		try (OutputStream out = new FileOutputStream(dst)) {
			byte[] buf = new byte[8192];
			int len;
			while ((len = in.read(buf)) != -1) {
				out.write(buf, 0, len);
			}
		}
	}

	public static boolean isSpaceAvailable(long bytes) {
		if (bytes <= 0) {
			return true;
		}
		StatFs stat = new StatFs(getLevelsDirectory().getPath());
		return stat.getAvailableBytes() >= bytes;
	}

	private class AsyncDeleteLevel extends AsyncTask<Level, Void, Void> {
		@Override
		protected Void doInBackground(Level... levels) {
			delete(levels[0]);
			return null;
		}
	}

	private class AsyncInstallLevel extends AsyncTask<Object, Void, Object> {
		@Override
		protected Object doInBackground(Object... objects) {
			Object source = objects[0];
			String name = (String) objects[1];
			String author = (String) objects[2];
			long apiId = (long) objects[3];

			try {
				if (source instanceof File) {
					return install((File) source, name, author, apiId);
				}
				if (source instanceof Uri) {
					return install((Uri) source, name, author, apiId);
				}
				return new IOException("Unsupported level file source");
			} catch (Throwable e) {
				return e;
			}
		}
	}

}
