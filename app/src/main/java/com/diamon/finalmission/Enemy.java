package com.diamon.finalmission;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import java.util.List;
import java.util.Map;

/**
 * Entidad Enemigo para Final Mission Android.
 * Subtipos:
 * 1. AntiAéreo (torreta que apunta y dispara)
 * 2. Volador (drones aéreos en onda senoidal)
 * 3. Saltador (bípede que salta entre plataformas)
 * 4. Robot (soldado mecánico caminante)
 * 5. LanzaMisil (vehículo de asedio de misiles)
 * 6. Boss MaquinaFinal (jefe mecánico de etapa)
 */
public class Enemy {

    public static final int TYPE_ANTIAEREO = 0;
    public static final int TYPE_VOLADOR = 1;
    public static final int TYPE_SALTADOR = 2;
    public static final int TYPE_ROBOT = 3;
    public static final int TYPE_LANZAMISIL = 4;
    public static final int TYPE_BOSS = 5;

    private float x, y;
    private final float startY;
    private final int type;
    private int health;
    private final int maxHealth;
    private float width, height;
    private boolean active = true;

    private float timer = 0f;
    private float fireTimer = 0f;
    private final float fireInterval;
    private int animFrame = 0;

    private final Paint paint = new Paint();

    public Enemy(float x, float y, int type) {
        this.x = x;
        this.y = y;
        this.startY = y;
        this.type = type;
        paint.setFilterBitmap(false);

        switch (type) {
            case TYPE_VOLADOR:
                width = 16f; height = 16f;
                health = maxHealth = 1;
                fireInterval = 1.8f;
                break;
            case TYPE_ANTIAEREO:
                width = 15f; height = 16f;
                health = maxHealth = 2;
                fireInterval = 2.2f;
                break;
            case TYPE_SALTADOR:
                width = 16f; height = 20f;
                health = maxHealth = 3;
                fireInterval = 2.5f;
                break;
            case TYPE_ROBOT:
                width = 16f; height = 24f;
                health = maxHealth = 4;
                fireInterval = 2.0f;
                break;
            case TYPE_LANZAMISIL:
                width = 24f; height = 18f;
                health = maxHealth = 5;
                fireInterval = 3.0f;
                break;
            case TYPE_BOSS:
                width = 64f; height = 64f;
                health = maxHealth = 45;
                fireInterval = 1.2f;
                break;
            default:
                width = 16f; height = 16f;
                health = maxHealth = 1;
                fireInterval = 2.0f;
        }
    }

    public void update(float dt, Player player, List<Bullet> bullets, AudioManager audio, float scrollSpeed) {
        timer += dt;
        fireTimer += dt;

        // Movimiento según tipo
        switch (type) {
            case TYPE_VOLADOR:
                x -= 1.8f + scrollSpeed;
                y = startY + (float) Math.sin(timer * 4.5f) * 28f;
                break;
            case TYPE_ANTIAEREO:
            case TYPE_LANZAMISIL:
                x -= scrollSpeed;
                break;
            case TYPE_SALTADOR:
                x -= 0.8f + scrollSpeed;
                float jumpCycle = (float) Math.abs(Math.sin(timer * 3.0f));
                y = startY - jumpCycle * 32f;
                break;
            case TYPE_ROBOT:
                x -= 0.6f + scrollSpeed;
                break;
            case TYPE_BOSS:
                // El jefe se desplaza suavemente verticalmente en el extremo derecho
                if (x > 185f) {
                    x -= 0.5f;
                }
                y = startY + (float) Math.sin(timer * 1.5f) * 45f;
                break;
        }

        // Lógica de disparo de enemigos
        if (fireTimer >= fireInterval && x > 0f && x < 250f) {
            fireTimer = 0f;
            float dirX = player.getX() - x;
            float dirY = player.getY() - y;
            float len = (float) Math.hypot(dirX, dirY);
            if (len > 0f) {
                dirX = (dirX / len) * 2.2f;
                dirY = (dirY / len) * 2.2f;
            }

            if (type == TYPE_LANZAMISIL) {
                bullets.add(new Bullet(x, y + 4f, -2.5f, 0f, Bullet.TYPE_ENEMY_MISSILE, false));
            } else if (type == TYPE_BOSS) {
                bullets.add(new Bullet(x, y + 16f, -3.0f, -0.6f, Bullet.TYPE_ENEMY_BULLET, false));
                bullets.add(new Bullet(x, y + 32f, -3.2f, 0f, Bullet.TYPE_ENEMY_BULLET, false));
                bullets.add(new Bullet(x, y + 48f, -3.0f, 0.6f, Bullet.TYPE_ENEMY_BULLET, false));
            } else {
                bullets.add(new Bullet(x, y + height / 2f, dirX, dirY, Bullet.TYPE_ENEMY_BULLET, false));
            }
        }

        // Desactivar si sale completamente de pantalla por la izquierda
        if (x < -80f) {
            active = false;
        }
    }

    public void draw(Canvas canvas, Map<String, Bitmap> sprites) {
        if (!active) return;

        String spriteName = "voladorD1.png";
        switch (type) {
            case TYPE_VOLADOR:
                int fv = ((int) (timer * 8f)) % 3 + 1;
                spriteName = "voladorI" + fv + ".png";
                break;
            case TYPE_ANTIAEREO:
                spriteName = "antiAreoD1.png";
                break;
            case TYPE_SALTADOR:
                int fs = ((int) (timer * 6f)) % 4 + 1;
                spriteName = "saltador" + fs + ".png";
                break;
            case TYPE_ROBOT:
                spriteName = "robotI.png";
                break;
            case TYPE_LANZAMISIL:
                int fl = ((int) (timer * 4f)) % 3 + 1;
                spriteName = "lanzaMisilI" + fl + ".png";
                break;
            case TYPE_BOSS:
                spriteName = "maquinaFinal.png";
                break;
        }

        Bitmap bmp = sprites != null ? sprites.get(spriteName) : null;
        if (bmp != null) {
            Rect dst = new Rect((int) x, (int) y, (int) (x + width), (int) (y + height));
            canvas.drawBitmap(bmp, null, dst, paint);
        } else {
            paint.setColor(type == TYPE_BOSS ? Color.MAGENTA : Color.YELLOW);
            canvas.drawRect(x, y, x + width, y + height, paint);
        }
    }

    public void takeDamage(int damage) {
        health -= damage;
        if (health <= 0) {
            active = false;
        }
    }

    public RectF getBoundingBox() {
        return new RectF(x, y, x + width, y + height);
    }

    public boolean isActive() { return active; }
    public void setActive(boolean a) { this.active = a; }
    public float getX() { return x; }
    public float getY() { return y; }
    public int getType() { return type; }
}
