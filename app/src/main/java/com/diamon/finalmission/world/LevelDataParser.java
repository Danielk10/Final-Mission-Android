package com.diamon.finalmission.world;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser and loader for authentic enemy spawn definitions in Final Mission.
 * Reads the authentic data file app/src/main/assets/data/datosNiveles.txt
 * with structured support across all stages.
 */
public class LevelDataParser {
    private static final String TAG = "LevelDataParser";
    private static final String ASSET_PATH = "data/datosNiveles.txt";

    public static class ActorSpawnDef {
        public final String actorType;
        public final int origX;
        public final int origY;
        public final String levelName;

        public ActorSpawnDef(String actorType, int origX, int origY, String levelName) {
            this.actorType = actorType;
            this.origX = origX;
            this.origY = origY;
            this.levelName = levelName;
        }

        @Override
        public String toString() {
            return "ActorSpawnDef{" + actorType + " at (" + origX + ", " + origY + ") in " + levelName + "}";
        }
    }

    private final Map<String, List<ActorSpawnDef>> levelSpawns = new HashMap<>();
    private boolean isLoaded = false;
    private int defaultLevel = 1;

    public LevelDataParser() {
        for (int i = 1; i <= StageConfig.TOTAL_STAGES; i++) {
            levelSpawns.put("Nivel " + i, new ArrayList<>());
        }
        loadData((Context) null);
    }

    public LevelDataParser(InputStream is) {
        for (int i = 1; i <= StageConfig.TOTAL_STAGES; i++) {
            levelSpawns.put("Nivel " + i, new ArrayList<>());
        }
        loadData(is);
    }

    public LevelDataParser(Context context) {
        for (int i = 1; i <= StageConfig.TOTAL_STAGES; i++) {
            levelSpawns.put("Nivel " + i, new ArrayList<>());
        }
        loadData(context);
    }

    public void loadData(Context context) {
        if (context != null) {
            try (InputStream is = context.getAssets().open(ASSET_PATH)) {
                loadData(is);
                return;
            } catch (Exception ignored) {}
        }

        // Fallback 1: ClassLoader (useful in unit test environments and packaged jar/apk resources)
        try {
            InputStream is = LevelDataParser.class.getClassLoader() != null ?
                    LevelDataParser.class.getClassLoader().getResourceAsStream(ASSET_PATH) : null;
            if (is == null && LevelDataParser.class.getClassLoader() != null) {
                is = LevelDataParser.class.getClassLoader().getResourceAsStream("assets/" + ASSET_PATH);
            }
            if (is != null) {
                try (InputStream stream = is) {
                    loadData(stream);
                    return;
                }
            }
        } catch (Exception ignored) {}

        // Fallback 2: Direct file system paths (JVM unit tests)
        String[] paths = new String[]{
                ASSET_PATH,
                "app/src/main/assets/" + ASSET_PATH,
                "src/main/assets/" + ASSET_PATH
        };
        for (String p : paths) {
            java.io.File file = new java.io.File(p);
            if (file.exists() && file.canRead()) {
                try (InputStream is = new java.io.FileInputStream(file)) {
                    loadData(is);
                    return;
                } catch (Exception ignored) {}
            }
        }
    }

    public void loadData(InputStream is) {
        levelSpawns.clear();
        for (int i = 1; i <= StageConfig.TOTAL_STAGES; i++) {
            levelSpawns.put("Nivel " + i, new ArrayList<>());
        }

        if (is == null) return;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line1 = reader.readLine(); // boolean readInternalData
            String line2 = reader.readLine(); // int actorCount
            String line3 = reader.readLine(); // int levelNumber

            if (line3 != null && !line3.trim().isEmpty()) {
                try {
                    defaultLevel = Integer.parseInt(line3.trim());
                } catch (NumberFormatException ignored) {}
            }

            int count = 0;
            String typeLine;
            while ((typeLine = reader.readLine()) != null) {
                typeLine = typeLine.trim();
                if (typeLine.isEmpty()) continue;

                String xLine = reader.readLine();
                String yLine = reader.readLine();
                String levelLine = reader.readLine();

                if (xLine == null || yLine == null || levelLine == null) break;

                try {
                    int x = Integer.parseInt(xLine.trim());
                    int y = Integer.parseInt(yLine.trim());
                    String levelName = levelLine.trim();

                    ActorSpawnDef def = new ActorSpawnDef(typeLine, x, y, levelName);
                    List<ActorSpawnDef> list = levelSpawns.get(levelName);
                    if (list == null) {
                        list = new ArrayList<>();
                        levelSpawns.put(levelName, list);
                    }
                    list.add(def);
                    count++;
                } catch (NumberFormatException e) {
                    logWarning("Error parsing actor coordinate: " + typeLine, e);
                }
            }

            isLoaded = true;
            logInfo("Successfully loaded " + count + " actor spawn definitions from " + ASSET_PATH);
        } catch (Exception e) {
            logError("Error loading stream for " + ASSET_PATH, e);
        }
    }

    public List<ActorSpawnDef> getSpawnsForLevel(int levelNumber) {
        if (levelNumber < 1) levelNumber = 1;
        if (levelNumber > StageConfig.TOTAL_STAGES) levelNumber = StageConfig.TOTAL_STAGES;
        String key = "Nivel " + levelNumber;
        List<ActorSpawnDef> list = levelSpawns.get(key);
        return list != null ? list : new ArrayList<>();
    }

    public List<ActorSpawnDef> getSpawnsForStage(int stageNumber) {
        return getSpawnsForLevel(stageNumber);
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public int getDefaultLevel() {
        return defaultLevel;
    }

    private static void logInfo(String msg) {
        try {
            Log.i(TAG, msg);
        } catch (Throwable t) {
            System.out.println("[" + TAG + "] " + msg);
        }
    }

    private static void logWarning(String msg, Throwable e) {
        try {
            Log.w(TAG, msg, e);
        } catch (Throwable t) {
            System.err.println("[" + TAG + "] " + msg + ": " + e);
        }
    }

    private static void logError(String msg, Throwable e) {
        try {
            Log.e(TAG, msg, e);
        } catch (Throwable t) {
            System.err.println("[" + TAG + "] " + msg + ": " + e);
        }
    }
}
