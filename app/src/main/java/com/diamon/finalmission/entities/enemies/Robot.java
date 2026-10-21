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
 * Enemigo Robot (Cyborg acorazado) de Final Mission NES:
 * Soldado pesado que patrulla plataformas e intercambia fuego frontal directo.
 */
public class Robot extends Enemy {

    private boolean facingRight = false;
    private float patrolTimer = 0f;
    private float patrolSpeed = 20f;
    private float shootTimer = 1.2f;

    public Robot(float x, float y) {
        super(x, y, 24f, 32f, 6f, 500);
    }

    @Override
    public void update(float dt, Player player, List<Bullet> enemyBullets, float scrollSpeed) {
        if (!alive) return;

        if (hitFlashTimer > 0f) {
            hitFlashTimer -= dt;
        }

        // Desplazamiento de scroll + patrulla
        x -= scrollSpeed * dt;

        patrolTimer += dt;
        if (patrolTimer >= 2.0f) {
            patrolTimer = 0f;
            facingRight = !facingRight;
        }

        x += (facingRight ? patrolSpeed : -patrolSpeed) * dt;

        // Orientación hacia el jugador si está cerca
        if (Math.abs(player.x - x) < 180f) {
            facingRight = (player.x > x);
        }

        // Disparo en ráfagas
        shootTimer -= dt;
        if (shootTimer <= 0f) {
            shootTimer = 1.8f;
            if (x > 0 && x < 250 && player.alive) {
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
