package com.diamon.finalmission.entities.enemies;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.diamon.finalmission.engine.AssetLoader;
import com.diamon.finalmission.entities.Bullet;
import com.diamon.finalmission.entities.Enemy;
import com.diamon.finalmission.entities.Player;

import java.util.List;

/**
 * Destructible wall turret mechanism (WallTurret) from Final Mission NES:
 * - Mounted on left or right walls in elevator shafts and corridors.
 * - Anchors onto level walls or the biomechanical boss fortress hull.
 * - Supports vertical scrolling velocity matching elevator shaft descent.
 * - Opens its hatch to aim and fire high-speed horizontal plasma pulses across the screen
 *   using authentic balaParedI / balaParedD projectiles.
 * - Fully destructible independent entity.
 */
public class WallTurret extends Enemy {

    public static final float TURRET_HP = 14f;

    public enum Side {
        LEFT,  // Mounted on left wall, fires right across the screen
        RIGHT  // Mounted on right wall, fires left across the screen
    }

    public boolean facingLeft = true;
    public boolean isOpen = false;
    private float fireTimer = 1.0f;
    private float openPostFireTimer = 0f;
    private int burstRemaining = 0;
    private float burstTimer = 0f;

    public float vy = 0f;

    public WallTurret(float x, float y, boolean facingLeft) {
        super(x, y, 16f, 20f, TURRET_HP, 800);
        this.facingLeft = facingLeft;
        this.fireTimer = 0.8f + (float) (Math.random() * 1.0f);
    }

    public WallTurret(float x, float y, Side side) {
        // When mounted on the LEFT wall, fires RIGHT across the screen (facingLeft = false)
        // When mounted on the RIGHT wall, fires LEFT across the screen (facingLeft = true)
        this(x, y, side == Side.RIGHT);
    }

    public WallTurret(float x, float y, Side side, float vy) {
        this(x, y, side);
        this.vy = vy;
    }

    public static WallTurret create(float x, float y, Side side, float vy) {
        return new WallTurret(x, y, side, vy);
    }

    public static WallTurret create(float x, float y, Side side) {
        return new WallTurret(x, y, side, 0f);
    }

    @Override
    public void update(float dt, Player player, List<Bullet> enemyBullets, float scrollSpeed) {
        if (!alive) return;

        if (hitFlashTimer > 0f) {
            hitFlashTimer -= dt;
        }

        // Horizontal level scroll and vertical shaft descent
        x -= scrollSpeed * dt;
        y += vy * dt;

        fireTimer -= dt;

        // Open hatch 0.35s prior to firing
        if (fireTimer <= 0.35f && fireTimer > 0f) {
            isOpen = true;
        }

        if (openPostFireTimer > 0f) {
            openPostFireTimer -= dt;
            if (openPostFireTimer <= 0f && fireTimer > 0.35f && burstRemaining == 0) {
                isOpen = false;
            }
        }

        // Trigger burst of rapid wall lasers across the screen
        if (fireTimer <= 0f) {
            fireTimer = 1.8f + (float) (Math.random() * 0.7f);
            isOpen = true;
            openPostFireTimer = 0.45f;
            burstRemaining = 2;
            burstTimer = 0f;
        }

        if (burstRemaining > 0) {
            burstTimer -= dt;
            if (burstTimer <= 0f) {
                burstTimer = 0.12f;
                burstRemaining--;
                if (player.alive && x > -30f && x < 550f && y > -30f && y < 260f) {
                    float barrelX = facingLeft ? (x - 6f) : (x + width + 6f);
                    float barrelY = y + height / 2f;
                    enemyBullets.add(Bullet.createEnemyWallLaser(barrelX, barrelY, facingLeft, 155f));
                }
            }
        }

        // Despawn bounds for horizontal and vertical scrolling
        float minY = (vy > 0f) ? -1000f : -60f;
        if (x < -60f || x > 650f || y < minY || y > 350f) {
            alive = false;
        }
    }

    @Override
    public RectF getBounds() {
        return new RectF(x, y, x + width, y + height);
    }

    @Override
    public void render(Canvas canvas, AssetLoader loader, Paint paint) {
        if (!alive) return;

        // maquinaParedI1/I2: base on right wall, nozzle facing LEFT
        // maquinaParedD1/D2: base on left wall, nozzle facing RIGHT
        String spriteName;
        if (facingLeft) {
            spriteName = isOpen ? "maquinaParedI2.png" : "maquinaParedI1.png";
        } else {
            spriteName = isOpen ? "maquinaParedD2.png" : "maquinaParedD1.png";
        }

        Bitmap bmp = loader.getBitmap(spriteName);
        if (bmp != null) {
            Paint activePaint = getActivePaint(paint);
            float drawX = x + (width - bmp.getWidth()) / 2f;
            float drawY = y + (height - bmp.getHeight()) / 2f;
            canvas.drawBitmap(bmp, drawX, drawY, activePaint);
        }
    }
}
