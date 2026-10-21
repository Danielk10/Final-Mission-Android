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
 * High-speed aerial attack drones that fly in sinusoidal wave formations and attack in squads.
 */
public class DroneFlyer extends Enemy {

    private final float baseSpeed;
    private final float startY;
    private float waveTimer = 0f;
    private float shootTimer;
    private float animTimer = 0f;
    private int animFrame = 0;
    private final boolean moveLeft;

    public DroneFlyer(float x, float y, boolean moveLeft) {
        super(x, y, 16f, 16f, 2f, 200);
        this.startY = y;
        this.moveLeft = moveLeft;
        this.baseSpeed = 70f + (float) (Math.random() * 25f);
        this.shootTimer = 1.0f + (float) (Math.random() * 1.5f);
        this.waveTimer = (float) (Math.random() * Math.PI * 2);
    }

    @Override
    public void update(float dt, Player player, List<Bullet> enemyBullets, float scrollSpeed) {
        if (!alive) return;

        if (hitFlashTimer > 0f) {
            hitFlashTimer -= dt;
        }

        animTimer += dt;
        if (animTimer >= 0.1f) {
            animTimer = 0f;
            animFrame = (animFrame + 1) % 3;
        }

        waveTimer += dt * 3.5f;

        // Sinusoidal wave flight
        float speed = moveLeft ? (baseSpeed + scrollSpeed) : -baseSpeed;
        x -= speed * dt;
        y = startY + (float) Math.sin(waveTimer) * 28f;

        // Aimed shooting
        shootTimer -= dt;
        if (shootTimer <= 0f) {
            shootTimer = 2.2f + (float) (Math.random() * 1.0f);
            if (x > 10 && x < 450 && player.alive) {
                enemyBullets.add(Bullet.createEnemyNormal(x + width / 2f, y + height / 2f,
                        player.x + player.width / 2f, player.y + player.height / 2f, 110f));
            }
        }

        if (x < -50 || x > 600) {
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
