package com.diamon.finalmission;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import java.util.Map;

/**
 * Entidad Proyectil de Final Mission Android.
 */
public class Bullet {

    public static final int TYPE_PLAYER_NORMAL = 0;
    public static final int TYPE_PLAYER_WIDE = 1;
    public static final int TYPE_PLAYER_LASER = 2;
    public static final int TYPE_SATELLITE = 3;
    public static final int TYPE_ENEMY_BULLET = 4;
    public static final int TYPE_ENEMY_MISSILE = 5;

    private float x, y;
    private final float vx, vy;
    private final int type;
    private final boolean facingRight;
    private boolean active = true;

    private final Paint paint = new Paint();

    public Bullet(float x, float y, float vx, float vy, int type, boolean facingRight) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.type = type;
        this.facingRight = facingRight;
        paint.setFilterBitmap(false);
    }

    public void update(float dt) {
        x += vx;
        y += vy;

        // Desactivar si sale de los límites de la pantalla NES (256x224)
        if (x < -32f || x > 288f || y < -32f || y > 256f) {
            active = false;
        }
    }

    public void draw(Canvas canvas, Map<String, Bitmap> sprites) {
        if (!active) return;

        String key = null;
        float w = 6f, h = 3f;

        switch (type) {
            case TYPE_PLAYER_NORMAL:
                key = facingRight ? "balaHD.png" : "balaHI.png";
                w = 8f; h = 4f;
                break;
            case TYPE_PLAYER_WIDE:
                key = facingRight ? "balaWD1.png" : "balaWI1.png";
                w = 8f; h = 16f;
                break;
            case TYPE_PLAYER_LASER:
                key = facingRight ? "balaLD3.png" : "balaLI3.png";
                w = 32f; h = 4f;
                break;
            case TYPE_SATELLITE:
                key = "balaSatelite.png";
                w = 4f; h = 4f;
                break;
            case TYPE_ENEMY_BULLET:
                key = "balaE1.png";
                w = 6f; h = 6f;
                break;
            case TYPE_ENEMY_MISSILE:
                key = facingRight ? "misilD1.png" : "misilH1.png";
                w = 12f; h = 6f;
                break;
        }

        Bitmap bmp = sprites != null && key != null ? sprites.get(key) : null;
        if (bmp != null) {
            Rect dst = new Rect((int) x, (int) y, (int) (x + w), (int) (y + h));
            canvas.drawBitmap(bmp, null, dst, paint);
        } else {
            // Renderizado vectorial retro de respaldo
            paint.setColor(isPlayerBullet() ? Color.CYAN : Color.RED);
            canvas.drawRect(x, y, x + w, y + h, paint);
        }
    }

    public RectF getBoundingBox() {
        float w = 6f, h = 4f;
        if (type == TYPE_PLAYER_WIDE) { w = 8f; h = 16f; }
        else if (type == TYPE_PLAYER_LASER) { w = 32f; h = 4f; }
        else if (type == TYPE_SATELLITE) { w = 4f; h = 4f; }
        else if (type == TYPE_ENEMY_MISSILE) { w = 12f; h = 6f; }
        return new RectF(x, y, x + w, y + h);
    }

    public boolean isPlayerBullet() {
        return type == TYPE_PLAYER_NORMAL || type == TYPE_PLAYER_WIDE || type == TYPE_PLAYER_LASER || type == TYPE_SATELLITE;
    }

    public int getDamage() {
        switch (type) {
            case TYPE_PLAYER_LASER: return 3;
            case TYPE_PLAYER_WIDE: return 2;
            case TYPE_SATELLITE: return 1;
            case TYPE_PLAYER_NORMAL: default: return 1;
        }
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public float getX() { return x; }
    public float getY() { return y; }
}
