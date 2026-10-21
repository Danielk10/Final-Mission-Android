package com.diamon.finalmission.entities.enemies;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.diamon.finalmission.engine.AssetLoader;
import com.diamon.finalmission.entities.Bullet;
import com.diamon.finalmission.entities.Enemy;
import com.diamon.finalmission.entities.Player;

import java.util.List;

/**
 * Drone Flyer enemy from Final Mission NES:
 * High-speed aerial scouts that fly in sinusoidal oscillating wave formations in squads of 3.
 * As they swoop across the screen and align with the player, they drop targeted angled plasma shots.
 */
public class DroneFlyer extends Enemy {

    private final float baseSpeed;
    private final float startY;
    private final float amplitude;
    private float waveTimer = 0f;
    private float shootCooldown = 0.5f;
    private int droppedShotCount = 0;
    private final int maxShotsPerPass;

    private float animTimer = 0f;
    private int animFrame = 0;
    private final boolean moveLeft;

    public DroneFlyer(float x, float y, boolean moveLeft) {
        this(x, y, moveLeft, 0f);
    }

    public DroneFlyer(float x, float y, boolean moveLeft, float waveOffset) {
        super(x, y, 16f, 16f, 2f, 200);
        this.startY = y;
        this.moveLeft = moveLeft;
        this.waveTimer = waveOffset;
        this.amplitude = 26f;
        this.baseSpeed = 75f;
        this.shootCooldown = 0.4f + (float) (Math.random() * 0.4f);
        this.maxShotsPerPass = 2;
    }

    @Override
    public void update(float dt, Player player, List<Bullet> enemyBullets, float scrollSpeed) {
        if (!alive) return;

        if (hitFlashTimer > 0f) {
            hitFlashTimer -= dt;
        }

        // Rotor/wing flap animation cycle (frames 1, 2, 3)
        animTimer += dt;
        if (animTimer >= 0.08f) {
            animTimer = 0f;
            animFrame = (animFrame + 1) % 3;
        }

        // Sinusoidal oscillating wave flight
        waveTimer += dt * 3.4f;
        float speed = moveLeft ? (baseSpeed + scrollSpeed) : -baseSpeed;
        x -= speed * dt;
        y = startY + (float) Math.sin(waveTimer) * amplitude;

        if (shootCooldown > 0f) {
            shootCooldown -= dt;
        }

        // Drops angled shots when aligned with player in airspace
        if (player.alive && x > 10f && x < 550f && shootCooldown <= 0f && droppedShotCount < maxShotsPerPass) {
            float playerCenterX = player.x + player.width / 2f;
            float droneCenterX = x + width / 2f;
            float dx = Math.abs(droneCenterX - playerCenterX);

            // Drop shot when within tactical alignment corridor
            if (dx <= 65f) {
                shootCooldown = 1.3f;
                droppedShotCount++;
                float dropX = droneCenterX;
                float dropY = y + height;
                float targetX = playerCenterX;
                float targetY = player.y + player.height / 2f;
                enemyBullets.add(Bullet.createEnemyNormal(dropX, dropY, targetX, targetY, 115f));
            }
        }

        if (x < -60f || x > 650f) {
            alive = false;
        }
    }

    @Override
    public void render(Canvas canvas, AssetLoader loader, Paint paint) {
        if (!alive) return;

        int frame = animFrame + 1;
        String spriteName = (moveLeft ? "voladorI" : "voladorD") + frame + ".png";
        Bitmap bmp = loader.getBitmap(spriteName);
        if (bmp != null) {
            canvas.drawBitmap(bmp, x, y, getActivePaint(paint));
        }
    }
}
