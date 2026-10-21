package com.diamon.finalmission;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import java.util.Map;

/**
 * Ítems Power-Up de Final Mission:
 * 'W' = Wide (Disparo Amplio)
 * 'L' = Laser (Láser Penetración)
 * 'S' = Speed (Acelerador)
 * 'B' = Life (Recuperación de Energía)
 */
public class PowerUp {

    private float x, y;
    private final char type;
    private boolean active = true;
    private float timer = 0f;

    private final Paint paint = new Paint();

    public PowerUp(float x, float y, char type) {
        this.x = x;
        this.y = y;
        this.type = type;
        paint.setFilterBitmap(false);
    }

    public void update(float dt, float scrollSpeed) {
        timer += dt;
        x -= (0.5f + scrollSpeed);
        y += (float) Math.sin(timer * 3f) * 0.4f;

        if (x < -20f) {
            active = false;
        }
    }

    public void draw(Canvas canvas, Map<String, Bitmap> sprites) {
        if (!active) return;

        String key = "poder" + type + ".png";
        Bitmap bmp = sprites != null ? sprites.get(key) : null;
        if (bmp != null) {
            Rect dst = new Rect((int) x, (int) y, (int) (x + 14f), (int) (y + 14f));
            canvas.drawBitmap(bmp, null, dst, paint);
        }
    }

    public RectF getBoundingBox() {
        return new RectF(x, y, x + 14f, y + 14f);
    }

    public char getType() { return type; }
    public boolean isActive() { return active; }
    public void setActive(boolean a) { this.active = a; }
}
