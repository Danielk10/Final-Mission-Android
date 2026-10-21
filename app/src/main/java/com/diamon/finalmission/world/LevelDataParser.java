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
 * Analizador y cargador de definiciones de apariciones (spawns) de enemigos para Final Mission.
 * Lee el archivo de datos original app/src/main/assets/data/datosNiveles.txt
 * con soporte para los 8 niveles (Nivel 1 a Nivel 8).
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

    public LevelDataParser(Context context) {
        loadData(context);
    }

    public void loadData(Context context) {
        levelSpawns.clear();
        for (int i = 1; i <= 8; i++) {
            levelSpawns.put("Nivel " + i, new ArrayList<>());
        }

        try (InputStream is = context.getAssets().open(ASSET_PATH);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            String line1 = reader.readLine(); // boolean leerDatosInternos
            String line2 = reader.readLine(); // int numeroActores
            String line3 = reader.readLine(); // int numeroNivel

            if (line3 != null && !line3.trim().isEmpty()) {
                try {
                    defaultLevel = Integer.parseInt(line3.trim());
                } catch (NumberFormatException ignored) {}
            }

            int expectedCount = 0;
            if (line2 != null && !line2.trim().isEmpty()) {
                try {
                    expectedCount = Integer.parseInt(line2.trim());
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
                    Log.w(TAG, "Error parseando coordenadas de actor: " + typeLine, e);
                }
            }

            isLoaded = true;
            Log.i(TAG, "Cargadas " + count + " definiciones de actores desde " + ASSET_PATH);
        } catch (Exception e) {
            Log.e(TAG, "Error cargando archivo " + ASSET_PATH, e);
        }
    }

    public List<ActorSpawnDef> getSpawnsForLevel(int levelNumber) {
        String key = "Nivel " + levelNumber;
        List<ActorSpawnDef> list = levelSpawns.get(key);
        return list != null ? list : new ArrayList<>();
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public int getDefaultLevel() {
        return defaultLevel;
    }
}
