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
 * - Uses maquinaParedI1/I2 (left side) and maquinaParedD1/D2 (right side).
 * - Anchors onto level walls or the biomechanical boss fortress hull.
 * - Opens its hatch to aim and fire high-speed laser beams at the player.
 * - Fully destructible independent entity.
 */
public class WallTurret extends Enemy {

    public static final float TURRET_HP = 14f;

    public enum Side { LEFT, RIGHT }

    public boolean facingLeft = true;
    public boolean isOpen = false;
    private float fireTimer = 1.0f;
    private float animTimer = 0f;

    public float vy = 0f;

    public WallTurret(float x, float y, boolean facingLeft) {
        super(x, y, 16f, 20f, TURRET_HP, 800);
        this.facingLeft = facingLeft;
        this.fireTimer = 0.8f + (float) (Math.random() * 1.0);
    }

    public WallTurret(float x, float y, Side side) {
        this(x, y, side == Side.LEFT);
    }

    public WallTurret(float x, float y, Side side, float vy) {
        this(x, y, side);
        this.vy = vy;
    }

    @Override
    public void update(float dt, Player player, List<Bullet> enemyBullets, float scrollSpeed) {
        if (!alive) return;

        if (hitFlashTimer > 0f) {
            hitFlashTimer -= dt;
        }

        x -= scrollSpeed * dt;
        y += vy * dt;

        fireTimer -= dt;

        // Open hatch 0.35s prior to firing
        if (fireTimer <= 0.35f && fireTimer > 0f) {
            isOpen = true;
        } else if (fireTimer > 0.35f && fireTimer < 1.8f) {
            isOpen = false;
        }

        // Fire laser beam
        if (fireTimer <= 0f) {
            fireTimer = 2.0f + (float) (Math.random() * 0.8);
            isOpen = true;

            if (player.alive) {
                float barrelX = facingLeft ? x - 4f : x + width + 4f;
                float barrelY = y + height / 2f;
                enemyBullets.add(Bullet.createEnemyWallLaser(barrelX, barrelY, facingLeft, 140f));
            }
        }
    }

    @Override
    public RectF getBounds() {
        return new RectF(x, y, x + width, y + height);
    }

    @Override
    public void render(Canvas canvas, AssetLoader loader, Paint paint) {
        if (!alive) return;

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
