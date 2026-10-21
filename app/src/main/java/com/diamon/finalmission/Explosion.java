package com.diamon.finalmission;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import java.util.Map;

/**
 * Efecto de Explosión de Final Mission.
 */
public class Explosion {

    private final float x, y;
    private final float size;
    private final boolean isBoss;
    private float timer = 0f;
    private static final float FRAME_TIME = 0.08f;
    private boolean active = true;

    private final Paint paint = new Paint();

    public Explosion(float x, float y, boolean isBoss) {
        this.x = x;
        this.y = y;
        this.isBoss = isBoss;
        this.size = isBoss ? 48f : 24f;
        paint.setFilterBitmap(false);
    }

    public void update(float dt) {
        timer += dt;
        int maxFrames = isBoss ? 5 : 4;
        if (timer >= maxFrames * FRAME_TIME) {
            active = false;
        }
    }

    public void draw(Canvas canvas, Map<String, Bitmap> sprites) {
        if (!active) return;

        int frame = (int) (timer / FRAME_TIME) + 1;
        String key = isBoss ? ("explosionB" + Math.min(frame, 5) + ".png") : ("explosion" + Math.min(frame, 4) + ".png");

        Bitmap bmp = sprites != null ? sprites.get(key) : null;
        if (bmp != null) {
            Rect dst = new Rect((int) (x - size / 2f), (int) (y - size / 2f), (int) (x + size / 2f), (int) (y + size / 2f));
            canvas.drawBitmap(bmp, null, dst, paint);
        }
    }

    public boolean isActive() { return active; }
}
