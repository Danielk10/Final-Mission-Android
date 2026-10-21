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
        ENEMY_WALL_LASER,
        BOSS_PLASMA,
        BOSS_LASER,
        BOSS_ENERGY_SPHERE,
        BOSS_BARRAGE,
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
    public float lifetime = 0f;
    public int missileHp = 1;
    public int sphereHp = 2; // Resistencia para esferas de energía destructibles

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
        float speed = 340f;
        float vx = facingRight ? speed : -speed;
        return new Bullet(x, y, vx, 0f, Type.PLAYER_NORMAL, facingRight, 1.0f);
    }

    public static Bullet createPlayerWide(float x, float y, boolean facingRight, float angleDeg) {
        float speed = 300f;
        float rad = (float) Math.toRadians(angleDeg);
        float vx = (float) Math.cos(rad) * speed * (facingRight ? 1f : -1f);
        float vy = (float) Math.sin(rad) * speed;
        return new Bullet(x, y, vx, vy, Type.PLAYER_WIDE, facingRight, 1.5f);
    }

    public static Bullet createPlayerLaser(float x, float y, boolean facingRight) {
        float speed = 480f;
        float vx = facingRight ? speed : -speed;
        return new Bullet(x, y, vx, 0f, Type.PLAYER_LASER, facingRight, 3.0f);
    }

    public static Bullet createSatellite(float x, float y, float angle) {
        float speed = 340f;
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

    public static Bullet createEnemyWallLaser(float x, float y, boolean facingLeft, float speed) {
        float vx = facingLeft ? -speed : speed;
        return new Bullet(x, y, vx, 0f, Type.ENEMY_WALL_LASER, !facingLeft, 1.2f);
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

    // FASE 1 JEFE: Láser dirigido de 3 vías
    public static Bullet createBossLaser(float x, float y, float angle, float speed) {
        float vx = (float) Math.cos(angle) * speed;
        float vy = (float) Math.sin(angle) * speed;
        Bullet b = new Bullet(x, y, vx, vy, Type.BOSS_LASER, vx >= 0, 2.0f);
        b.angle = angle;
        return b;
    }

    // FASE 2 JEFE: Esferas de energía destructibles
    public static Bullet createBossEnergySphere(float x, float y, float vx, float vy) {
        Bullet b = new Bullet(x, y, vx, vy, Type.BOSS_ENERGY_SPHERE, vx >= 0, 1.5f);
        b.sphereHp = 2;
        return b;
    }

    // FASE 3 JEFE: Ráfagas rápidas de desesperación
    public static Bullet createBossBarrage(float x, float y, float angle, float speed) {
        float vx = (float) Math.cos(angle) * speed;
        float vy = (float) Math.sin(angle) * speed;
        Bullet b = new Bullet(x, y, vx, vy, Type.BOSS_BARRAGE, vx >= 0, 1.5f);
        b.angle = angle;
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

    public void deflect() {
        // Desvío por orbe defensivo
        this.vx = -Math.abs(vx) * 0.6f;
        this.vy = (float) ((Math.random() - 0.5) * 160f);
        this.alive = false; // Se disipa al rebotar
    }

    public void update(float dt) {
        if (!alive) return;
        lifetime += dt;
        animTimer += dt;

        // Comportamiento de misil
        if (type == Type.ENEMY_MISSILE) {
            vx += (facingRight ? 100f : -100f) * dt;
        }

        // Ondulación de esferas destructibles
        if (type == Type.BOSS_ENERGY_SPHERE) {
            y += (float) Math.sin(lifetime * 4.0) * 0.4f;
        }

        x += vx * dt;
        y += vy * dt;

        // Ciclo de animación
        if (animTimer >= 0.06f) {
            animTimer = 0f;
            animFrame = (animFrame + 1) % 4;
        }

        // Límite de vida útil fuera de pantalla
        if (x < -80 || x > 600 || y < -60 || y > 300) {
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
            case BOSS_PLASMA:
            case BOSS_BARRAGE:
                int eFrame = (animFrame % 4) + 1;
                bmp = loader.getBitmap("balaE" + eFrame + ".png");
                break;
            case ENEMY_WALL_LASER:
                bmp = loader.getBitmap(facingRight ? "balaParedD.png" : "balaParedI.png");
                break;
            case ENEMY_SALTADOR:
            case BOSS_ENERGY_SPHERE:
                int sFrame = (animFrame % 2) + 1;
                bmp = loader.getBitmap("balaSaltador" + sFrame + ".png");
                break;
            case ENEMY_MISSILE:
                int mFrame = (animFrame % 2) + 1;
                bmp = loader.getBitmap((facingRight ? "misilD" : "misilH") + mFrame + ".png");
                break;
            case BOSS_LASER:
                // Rayo láser de jefe
                int blFrame = (animFrame % 3) + 1;
                bmp = loader.getBitmap("balaLI" + blFrame + ".png");
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
            case ENEMY_WALL_LASER:
                return new RectF(x - 8, y - 3, x + 8, y + 3);
            case ENEMY_MISSILE:
                return new RectF(x - 5, y - 4, x + 5, y + 4);
            case BOSS_ENERGY_SPHERE:
                return new RectF(x - 6, y - 6, x + 6, y + 6);
            case BOSS_LASER:
                return new RectF(x - 8, y - 4, x + 8, y + 4);
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
