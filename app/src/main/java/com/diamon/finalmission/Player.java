package com.diamon.finalmission;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import java.util.List;

/**
 * Entidad Jugador (Arnold / Sigourney) para Final Mission Android.
 * - Animación de vuelo por propulsores jetpack
 * - Desplazamiento en 8 direcciones
 * - Sistema de poderes: Normal, Wide ('W'), Laser ('L'), Speed ('S'), Vida
 * - Barra de vida auténtica NES (6 segmentos)
 * - Invulnerabilidad temporal con parpadeo tras recibir impacto
 */
public class Player {

    public static final int POWER_NORMAL = 0;
    public static final int POWER_WIDE = 1;   // 'W'
    public static final int POWER_LASER = 2;  // 'L'

    private float x = 40f;
    private float y = 100f;
    private float targetX = 40f;
    private float targetY = 100f;

    private float width = 23f;   // 92 / 4 = 23 píxeles NES
    private float height = 29f;  // 116 / 4 = 29 píxeles NES

    private float speed = 1.6f;
    private boolean facingRight = true;

    private int powerType = POWER_NORMAL;
    private int lives = 3;
    private int health = 6; // 6 segmentos de vida estilo NES
    private static final int MAX_HEALTH = 6;

    private boolean invulnerable = false;
    private float invulnerableTimer = 0f;
    private static final float INVULNERABLE_DURATION = 2.0f;

    // Animación
    private int animFrame = 0;
    private float animTimer = 0f;
    private static final float FRAME_TIME = 0.10f;

    // Disparo
    private float fireCooldown = 0f;
    private static final float FIRE_RATE = 0.12f;

    private final Paint paint = new Paint();

    public Player() {
        paint.setFilterBitmap(false); // Pixel-art nítido
    }

    public void update(float dt, TouchController touch, List<Bullet> bullets, AudioManager audio, List<Bitmap> spritesRight, List<Bitmap> spritesLeft) {
        // Actualizar temporizador de invulnerabilidad
        if (invulnerable) {
            invulnerableTimer -= dt;
            if (invulnerableTimer <= 0f) {
                invulnerable = false;
            }
        }

        // Animación de propulsión jetpack
        animTimer += dt;
        if (animTimer >= FRAME_TIME) {
            animTimer = 0f;
            int totalFrames = facingRight ? (spritesRight != null ? spritesRight.size() : 6) : (spritesLeft != null ? spritesLeft.size() : 3);
            if (totalFrames > 0) {
                animFrame = (animFrame + 1) % totalFrames;
            }
        }

        // Suavizado hacia la posición objetivo del touch
        x += (targetX - x) * 0.35f;
        y += (targetY - y) * 0.35f;

        // Limitar dentro de los límites de pantalla de la NES (256 x 224)
        if (x < 8f) x = 8f;
        if (x > 256f - width - 8f) x = 256f - width - 8f;
        if (y < 16f) y = 16f;
        if (y > 224f - height - 16f) y = 224f - height - 16f;

        // Gestión de disparo continuo
        fireCooldown -= dt;
        if (touch.isFiring() && fireCooldown <= 0f) {
            fireCooldown = FIRE_RATE;
            fireWeapon(bullets, audio);
        }
    }

    private void fireWeapon(List<Bullet> bullets, AudioManager audio) {
        float bulletX = facingRight ? (x + width) : (x - 8f);
        float bulletY = y + height * 0.45f;
        float dirX = facingRight ? 1f : -1f;

        switch (powerType) {
            case POWER_NORMAL:
                // Dos balas rápidas paralelas
                bullets.add(new Bullet(bulletX, bulletY - 3f, dirX * 6f, 0f, Bullet.TYPE_PLAYER_NORMAL, facingRight));
                bullets.add(new Bullet(bulletX, bulletY + 3f, dirX * 6f, 0f, Bullet.TYPE_PLAYER_NORMAL, facingRight));
                audio.playSound("disparo.wav");
                break;
            case POWER_WIDE:
                // Disparo en abanico de 3 vías (Wave/Wide)
                bullets.add(new Bullet(bulletX, bulletY, dirX * 5.5f, 0f, Bullet.TYPE_PLAYER_WIDE, facingRight));
                bullets.add(new Bullet(bulletX, bulletY - 4f, dirX * 5.0f, -1.8f, Bullet.TYPE_PLAYER_WIDE, facingRight));
                bullets.add(new Bullet(bulletX, bulletY + 4f, dirX * 5.0f, 1.8f, Bullet.TYPE_PLAYER_WIDE, facingRight));
                audio.playSound("disparo.wav");
                break;
            case POWER_LASER:
                // Rayo láser de alta penetración
                bullets.add(new Bullet(bulletX, bulletY - 1f, dirX * 8f, 0f, Bullet.TYPE_PLAYER_LASER, facingRight));
                audio.playSound("disparoL.wav");
                break;
        }
    }

    public void draw(Canvas canvas, List<Bitmap> spritesRight, List<Bitmap> spritesLeft) {
        // Efecto parpadeo si es invulnerable
        if (invulnerable && ((int) (invulnerableTimer * 20f)) % 2 == 0) {
            return;
        }

        List<Bitmap> currentList = facingRight ? spritesRight : spritesLeft;
        if (currentList != null && !currentList.isEmpty()) {
            int frameIdx = Math.min(animFrame, currentList.size() - 1);
            Bitmap frame = currentList.get(frameIdx);
            if (frame != null) {
                Rect dst = new Rect((int) x, (int) y, (int) (x + width), (int) (y + height));
                canvas.drawBitmap(frame, null, dst, paint);
            }
        }
    }

    public void takeDamage(int amount, AudioManager audio) {
        if (invulnerable) return;

        health -= amount;
        invulnerable = true;
        invulnerableTimer = INVULNERABLE_DURATION;

        if (health <= 0) {
            lives--;
            health = MAX_HEALTH;
            powerType = POWER_NORMAL; // Pierde poderes al morir
            audio.playSound("muriendo.wav");
        } else {
            audio.playSound("explosion.wav");
        }
    }

    public void applyPowerUp(char type, AudioManager audio) {
        audio.playSound("poder.wav");
        switch (type) {
            case 'W':
                powerType = POWER_WIDE;
                break;
            case 'L':
                powerType = POWER_LASER;
                break;
            case 'S':
                speed = Math.min(speed + 0.35f, 2.8f);
                break;
            case 'B': // Heart / Life recovery
                health = Math.min(health + 2, MAX_HEALTH);
                break;
        }
    }

    public void setPositionTarget(float tx, float ty) {
        this.targetX = tx;
        this.targetY = ty;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public int getHealth() { return health; }
    public int getLives() { return lives; }
    public int getPowerType() { return powerType; }
    public boolean isFacingRight() { return facingRight; }
    public void setFacingRight(boolean right) { this.facingRight = right; }
    public boolean isDead() { return lives < 0; }
}
