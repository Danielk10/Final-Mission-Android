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
 * Asset loader and texture manager for Final Mission.
 * Backgrounds and full menu screens (256x240) are loaded at native 1:1 NES resolution.
 * Character actors and projectiles exported at 4x resolution are downscaled by 1/4 with
 * nearest-neighbor filtering to match the pixel grid of the retro 256x224 virtual canvas.
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
                Log.e(TAG, "Failed to decode sprite asset: " + name);
                return null;
            }

            Bitmap processed = processBitmap(name, raw);
            cache.put(name, processed);
            return processed;
        } catch (Exception e) {
            Log.e(TAG, "Error loading sprite asset: " + name, e);
            return null;
        }
    }

    private Bitmap processBitmap(String name, Bitmap raw) {
        // Full backgrounds and UI/menu screens retain native NES resolution
        if (name.startsWith("fondo") || name.startsWith("menu") || name.startsWith("barra")
                || name.startsWith("fin") || name.startsWith("intro") || name.equals("nivel1.png")
                || name.equals("ayuda.png") || name.equals("creditos.png") || name.equals("precentacion.png")
                || name.startsWith("continuar")) {
            return raw;
        }

        // Actors and projectiles exported at 4x are downscaled to 1/4 nearest-neighbor
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

    /**
     * Creates or retrieves a red-tinted version of Sparky's sprite for Sergei Missionov (2P),
     * swapping Sparky's blue suit palette (66, 64, 255) to Sergei's authentic red suit palette (181, 49, 32).
     */
    public Bitmap getSergeiBitmap(String name) {
        String key = "sergei_" + name;
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        Bitmap base = getBitmap(name);
        if (base == null) return null;

        int w = base.getWidth();
        int h = base.getHeight();
        int[] pixels = new int[w * h];
        base.getPixels(pixels, 0, w, 0, 0, w, h);
        for (int i = 0; i < pixels.length; i++) {
            int c = pixels[i];
            int a = (c >> 24) & 0xFF;
            int r = (c >> 16) & 0xFF;
            int g = (c >> 8) & 0xFF;
            int b = c & 0xFF;
            // Detect Sparky's blue suit color (66, 64, 255)
            if (a > 128 && b > 180 && r < 110 && g < 110) {
                // Replace with Sergei's red suit color (181, 49, 32)
                pixels[i] = (a << 24) | (181 << 16) | (49 << 8) | 32;
            }
        }
        Bitmap result = Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888);
        cache.put(key, result);
        return result;
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
