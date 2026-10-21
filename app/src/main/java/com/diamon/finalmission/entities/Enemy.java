package com.diamon.finalmission.entities;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.RectF;

import com.diamon.finalmission.engine.AssetLoader;

import java.util.List;

/**
 * Clase base para todos los enemigos de Final Mission NES.
 */
public abstract class Enemy {

    public float x;
    public float y;
    public float width;
    public float height;
    public float hp;
    public float maxHp;
    public int scoreValue;
    public boolean alive = true;

    // Efecto de parpadeo al recibir impacto
    public float hitFlashTimer = 0f;
    private static final Paint HIT_PAINT = new Paint();

    static {
        // Matriz de color para destello blanco brillante al recibir daño
        ColorMatrix cm = new ColorMatrix(new float[]{
                2f, 0f, 0f, 0f, 120f,
                0f, 2f, 0f, 0f, 120f,
                0f, 0f, 2f, 0f, 120f,
                0f, 0f, 0f, 1f, 0f
        });
        HIT_PAINT.setColorFilter(new ColorMatrixColorFilter(cm));
    }

    public Enemy(float x, float y, float width, float height, float hp, int scoreValue) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.hp = hp;
        this.maxHp = hp;
        this.scoreValue = scoreValue;
    }

    public abstract void update(float dt, Player player, List<Bullet> enemyBullets, float scrollSpeed);

    public abstract void render(Canvas canvas, AssetLoader loader, Paint paint);

    public boolean takeDamage(float amount) {
        hp -= amount;
        hitFlashTimer = 0.08f;
        if (hp <= 0) {
            alive = false;
            return true;
        }
        return false;
    }

    public Paint getActivePaint(Paint defaultPaint) {
        if (hitFlashTimer > 0f) {
            return HIT_PAINT;
        }
        return defaultPaint;
    }

    public RectF getBounds() {
        return new RectF(x, y, x + width, y + height);
    }

    public boolean isOffscreen() {
        return x < -width - 20 || x > 300 || y < -height - 20 || y > 250;
    }
}
