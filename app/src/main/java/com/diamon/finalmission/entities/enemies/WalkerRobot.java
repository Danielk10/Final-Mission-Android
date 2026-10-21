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
 * Walker Robot (Armored Cyborg Soldier) enemy from Final Mission NES:
 * Heavy bipedal mechanized soldier patrolling platforms and engaging in direct frontal fire.
 */
public class WalkerRobot extends Enemy {

    private boolean facingRight = false;
    private float patrolTimer = 0f;
    private float patrolSpeed = 20f;
    private float shootTimer = 1.2f;

    public WalkerRobot(float x, float y) {
        super(x, y, 24f, 32f, 6f, 500);
    }

    @Override
    public void update(float dt, Player player, List<Bullet> enemyBullets, float scrollSpeed) {
        if (!alive) return;

        if (hitFlashTimer > 0f) {
            hitFlashTimer -= dt;
        }

        // Level scroll displacement + patrol walking
        x -= scrollSpeed * dt;

        patrolTimer += dt;
        if (patrolTimer >= 2.0f) {
            patrolTimer = 0f;
            facingRight = !facingRight;
        }

        x += (facingRight ? patrolSpeed : -patrolSpeed) * dt;

        // Face player if in proximity
        if (Math.abs(player.x - x) < 180f) {
            facingRight = (player.x > x);
        }

        // Firing volleys
        shootTimer -= dt;
        if (shootTimer <= 0f) {
            shootTimer = 1.8f;
            if (x > -10 && x < 450 && player.alive) {
                float muzzleX = facingRight ? (x + width) : x;
                float muzzleY = y + 12f;
                enemyBullets.add(Bullet.createEnemyNormal(muzzleX, muzzleY,
                        player.x + player.width / 2f, player.y + player.height / 2f, 120f));
            }
        }

        if (x < -40) {
            alive = false;
        }
    }

    @Override
    public void render(Canvas canvas, AssetLoader loader, Paint paint) {
        if (!alive) return;

        String spriteName = facingRight ? "robotD.png" : "robotI.png";
        Bitmap bmp = loader.getBitmap(spriteName);
        if (bmp != null) {
            canvas.drawBitmap(bmp, x, y, getActivePaint(paint));
        }
    }
}
