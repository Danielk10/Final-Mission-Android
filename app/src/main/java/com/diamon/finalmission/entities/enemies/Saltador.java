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
 * Enemigo Saltador de Final Mission NES:
 * Androide ágil que da grandes brincos parabólicos por las plataformas y dispara hacia abajo en el aire.
 */
public class Saltador extends Enemy {

    private final float groundY;
    private float vy = 0f;
    private float vx = -25f;
    private boolean isGrounded = true;
    private float stateTimer = 0f;
    private boolean hasShotInAir = false;

    private float animTimer = 0f;
    private int animFrame = 1;

    public Saltador(float x, float y) {
        super(x, y, 24f, 29f, 4f, 400);
        this.groundY = y;
    }

    @Override
    public void update(float dt, Player player, List<Bullet> enemyBullets, float scrollSpeed) {
        if (!alive) return;

        if (hitFlashTimer > 0f) {
            hitFlashTimer -= dt;
        }

        // Scroll horizontal
        x -= scrollSpeed * dt;

        stateTimer += dt;
        animTimer += dt;

        if (isGrounded) {
            y = groundY;
            vy = 0;
            animFrame = (animTimer % 0.3f < 0.15f) ? 1 : 2;

            // Preparar y ejecutar salto hacia el jugador
            if (stateTimer >= 1.2f) {
                stateTimer = 0f;
                isGrounded = false;
                hasShotInAir = false;
                vy = -160f; // Impulso vertical hacia arriba
                vx = (player.x < x) ? -55f : 35f;
            }
        } else {
            // En el aire
            vy += 280f * dt; // Gravedad
            y += vy * dt;
            x += vx * dt;
            animFrame = (vy < 0) ? 3 : 4;

            // Disparar en el punto álgido del salto
            if (!hasShotInAir && vy >= -30f && vy <= 30f) {
                hasShotInAir = true;
                if (x > 0 && x < 250) {
                    enemyBullets.add(Bullet.createEnemySaltador(x + width / 2f, y + height, 0f, 130f));
                }
            }

            // Tocar suelo
            if (y >= groundY) {
                y = groundY;
                isGrounded = true;
                stateTimer = 0f;
                vy = 0;
            }
        }

        if (x < -40) {
            alive = false;
        }
    }

    @Override
    public void render(Canvas canvas, AssetLoader loader, Paint paint) {
        if (!alive) return;

        String spriteName = "saltador" + animFrame + ".png";
        Bitmap bmp = loader.getBitmap(spriteName);
        if (bmp != null) {
            canvas.drawBitmap(bmp, x, y, getActivePaint(paint));
        }
    }
}
