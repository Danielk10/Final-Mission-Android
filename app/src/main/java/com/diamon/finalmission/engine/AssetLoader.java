package com.diamon.finalmission.engine;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Cargador y gestor de texturas y sprites para Final Mission.
 * Los fondos y pantallas (256x240) se cargan a resolución nativa NES 1:1.
 * Los sprites de actores exportados a 4x se reescalan a 1/4 con filtro nearest-neighbor
 * para coincidir con la cuadrícula de píxeles del lienzo retro virtual 256x224.
 */
public class AssetLoader {
    private static final String TAG = "AssetLoader";

    private final Context context;
    private final Map<String, Bitmap> cache = new HashMap<>();

    public AssetLoader(Context context) {
        this.context = context.getApplicationContext();
    }

    public Bitmap getBitmap(String name) {
        if (cache.containsKey(name)) {
            return cache.get(name);
        }

        try {
            InputStream is = context.getAssets().open("sprites/" + name);
            Bitmap raw = BitmapFactory.decodeStream(is);
            is.close();

            if (raw == null) {
                Log.e(TAG, "No se pudo decodificar imagen: " + name);
                return null;
            }

            Bitmap processed = processBitmap(name, raw);
            cache.put(name, processed);
            return processed;
        } catch (Exception e) {
            Log.e(TAG, "Error cargando sprite: " + name, e);
            return null;
        }
    }

    private Bitmap processBitmap(String name, Bitmap raw) {
        // Fondos y pantallas de menú/UI permanecen a resolución nativa
        if (name.startsWith("fondo") || name.startsWith("menu") || name.startsWith("barra")
                || name.startsWith("fin") || name.startsWith("intro") || name.equals("nivel1.png")
                || name.equals("ayuda.png") || name.equals("creditos.png") || name.equals("precentacion.png")
                || name.startsWith("continuar")) {
            return raw;
        }

        // Los actores y proyectiles exportados a 4x se escalan a 1/4
        int targetW = Math.max(1, raw.getWidth() / 4);
        int targetH = Math.max(1, raw.getHeight() / 4);

        if (targetW != raw.getWidth() || targetH != raw.getHeight()) {
            Bitmap scaled = Bitmap.createScaledBitmap(raw, targetW, targetH, false);
            if (scaled != raw) {
                raw.recycle();
            }
            return scaled;
        }

        return raw;
    }

    public Bitmap getFlippedBitmap(Bitmap src, boolean flipX, boolean flipY) {
        if (src == null) return null;
        if (!flipX && !flipY) return src;

        Matrix matrix = new Matrix();
        matrix.preScale(flipX ? -1 : 1, flipY ? -1 : 1);
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, false);
    }

    public void clear() {
        for (Bitmap bmp : cache.values()) {
            if (bmp != null && !bmp.isRecycled()) {
                bmp.recycle();
            }
        }
        cache.clear();
    }
}
