package com.diamon.finalmission.entities;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.diamon.finalmission.engine.AssetLoader;

/**
 * Entidad de proyectiles tanto del jugador como de enemigos y jefes.
 */
public class Bullet {

    public enum Type {
        PLAYER_NORMAL,
        PLAYER_WIDE,
        PLAYER_LASER,
        SATELLITE,
        ENEMY_NORMAL,
        ENEMY_SALTADOR,
        ENEMY_MISSILE,
        BOSS_PLASMA,
        BOSS_ORB
    }

    public float x;
    public float y;
    public float vx;
    public float vy;
    public Type type;
    public float damage;
    public boolean penetrating;
    public boolean facingRight;
    public boolean alive = true;
    public float angle = 0f;

    // Animación de proyectil
    private float animTimer = 0f;
    private int animFrame = 0;
    private float lifetime = 0f;
    public int missileHp = 1;

    public Bullet(float x, float y, float vx, float vy, Type type, boolean facingRight, float damage) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.type = type;
        this.facingRight = facingRight;
        this.damage = damage;
        this.penetrating = (type == Type.PLAYER_LASER);
        this.angle = (float) Math.atan2(vy, vx);
    }

    public static Bullet createPlayerNormal(float x, float y, boolean facingRight) {
        float speed = 320f;
        float vx = facingRight ? speed : -speed;
        return new Bullet(x, y, vx, 0f, Type.PLAYER_NORMAL, facingRight, 1.0f);
    }

    public static Bullet createPlayerWide(float x, float y, boolean facingRight, float angleDeg) {
        float speed = 280f;
        float rad = (float) Math.toRadians(angleDeg);
        float vx = (float) Math.cos(rad) * speed * (facingRight ? 1f : -1f);
        float vy = (float) Math.sin(rad) * speed;
        return new Bullet(x, y, vx, vy, Type.PLAYER_WIDE, facingRight, 1.5f);
    }

    public static Bullet createPlayerLaser(float x, float y, boolean facingRight) {
        float speed = 460f;
        float vx = facingRight ? speed : -speed;
        return new Bullet(x, y, vx, 0f, Type.PLAYER_LASER, facingRight, 3.0f);
    }

    public static Bullet createSatellite(float x, float y, float angle) {
        float speed = 320f;
        float vx = (float) Math.cos(angle) * speed;
        float vy = (float) Math.sin(angle) * speed;
        Bullet b = new Bullet(x, y, vx, vy, Type.SATELLITE, vx >= 0, 1.2f);
        b.angle = angle;
        return b;
    }

    public static Bullet createEnemyNormal(float x, float y, float targetX, float targetY, float speed) {
        float dx = targetX - x;
        float dy = targetY - y;
        float dist = (float) Math.hypot(dx, dy);
        if (dist <= 0.001f) {
            dx = -1f;
            dy = 0f;
            dist = 1f;
        }
        float vx = (dx / dist) * speed;
        float vy = (dy / dist) * speed;
        return new Bullet(x, y, vx, vy, Type.ENEMY_NORMAL, vx >= 0, 1.0f);
    }

    public static Bullet createEnemySaltador(float x, float y, float vx, float vy) {
        return new Bullet(x, y, vx, vy, Type.ENEMY_SALTADOR, vx >= 0, 1.0f);
    }

    public static Bullet createEnemyMissile(float x, float y, boolean facingRight) {
        float vx = facingRight ? 140f : -140f;
        Bullet b = new Bullet(x, y, vx, 0f, Type.ENEMY_MISSILE, facingRight, 1.5f);
        b.missileHp = 2;
        return b;
    }

    public static Bullet createBossPlasma(float x, float y, float angle, float speed) {
        float vx = (float) Math.cos(angle) * speed;
        float vy = (float) Math.sin(angle) * speed;
        Bullet b = new Bullet(x, y, vx, vy, Type.BOSS_PLASMA, vx >= 0, 1.5f);
        b.angle = angle;
        return b;
    }

    public static Bullet createBossOrb(float x, float y, float angle, float speed) {
        float vx = (float) Math.cos(angle) * speed;
        float vy = (float) Math.sin(angle) * speed;
        Bullet b = new Bullet(x, y, vx, vy, Type.BOSS_ORB, vx >= 0, 1.5f);
        b.angle = angle;
        return b;
    }

    public void update(float dt) {
        if (!alive) return;
        lifetime += dt;
        animTimer += dt;

        // Comportamiento especial de misil: aceleración y ligera búsqueda
        if (type == Type.ENEMY_MISSILE) {
            vx += (facingRight ? 100f : -100f) * dt;
        }

        x += vx * dt;
        y += vy * dt;

        // Ciclo de animación
        if (animTimer >= 0.06f) {
            animTimer = 0f;
            animFrame = (animFrame + 1) % 4;
        }

        // Límite de vida útil fuera de pantalla
        if (x < -60 || x > 320 || y < -40 || y > 260) {
            alive = false;
        }
    }

    public void render(Canvas canvas, AssetLoader loader, Paint paint) {
        if (!alive) return;

        Bitmap bmp = null;
        switch (type) {
            case PLAYER_NORMAL:
                bmp = loader.getBitmap(facingRight ? "balaHD.png" : "balaHI.png");
                break;
            case PLAYER_WIDE:
                int wFrame = (animFrame % 3) + 1;
                bmp = loader.getBitmap((facingRight ? "balaWD" : "balaWI") + wFrame + ".png");
                break;
            case PLAYER_LASER:
                int lFrame = (animFrame % 3) + 1;
                bmp = loader.getBitmap((facingRight ? "balaLD" : "balaLI") + lFrame + ".png");
                break;
            case SATELLITE:
                bmp = loader.getBitmap("balaSatelite.png");
                break;
            case ENEMY_NORMAL:
                int eFrame = (animFrame % 4) + 1;
                bmp = loader.getBitmap("balaE" + eFrame + ".png");
                break;
            case ENEMY_SALTADOR:
                int sFrame = (animFrame % 2) + 1;
                bmp = loader.getBitmap("balaSaltador" + sFrame + ".png");
                break;
            case ENEMY_MISSILE:
                int mFrame = (animFrame % 2) + 1;
                bmp = loader.getBitmap((facingRight ? "misilD" : "misilH") + mFrame + ".png");
                break;
            case BOSS_PLASMA:
                int pFrame = (animFrame % 4) + 1;
                bmp = loader.getBitmap("balaE" + pFrame + ".png");
                break;
            case BOSS_ORB:
                int orbIdx = (int) ((lifetime * 16) % 16) + 1;
                bmp = loader.getBitmap("bola" + orbIdx + ".png");
                break;
        }

        if (bmp != null) {
            float drawX = x - bmp.getWidth() / 2f;
            float drawY = y - bmp.getHeight() / 2f;
            canvas.drawBitmap(bmp, drawX, drawY, paint);
        }
    }

    public RectF getBounds() {
        float r = 4f;
        switch (type) {
            case PLAYER_NORMAL:
                return new RectF(x - 3, y - 2, x + 3, y + 2);
            case PLAYER_WIDE:
                return new RectF(x - 4, y - 6, x + 4, y + 6);
            case PLAYER_LASER:
                return new RectF(x - 12, y - 3, x + 12, y + 3);
            case SATELLITE:
                return new RectF(x - 3, y - 3, x + 3, y + 3);
            case ENEMY_MISSILE:
                return new RectF(x - 5, y - 4, x + 5, y + 4);
            case BOSS_ORB:
                return new RectF(x - 6, y - 6, x + 6, y + 6);
            default:
                return new RectF(x - r, y - r, x + r, y + r);
        }
    }

    public boolean isPlayerBullet() {
        return type == Type.PLAYER_NORMAL || type == Type.PLAYER_WIDE ||
                type == Type.PLAYER_LASER || type == Type.SATELLITE;
    }

    public boolean isEnemyBullet() {
        return !isPlayerBullet();
    }
}
