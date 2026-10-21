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
 * Jumper Mech enemy from Final Mission NES:
 * Agile bipedal combat android that performs high parabolic leaps between platforms and fires downward at player.
 */
public class JumperMech extends Enemy {

    private final float groundY;
    private float vy = 0f;
    private float vx = -25f;
    private boolean isGrounded = true;
    private float stateTimer = 0f;
    private boolean hasShotInAir = false;

    private float animTimer = 0f;
    private int animFrame = 1;

    public JumperMech(float x, float y) {
        super(x, y, 24f, 29f, 4f, 400);
        this.groundY = y;
    }

    @Override
    public void update(float dt, Player player, List<Bullet> enemyBullets, float scrollSpeed) {
        if (!alive) return;

        if (hitFlashTimer > 0f) {
            hitFlashTimer -= dt;
        }

        // Horizontal scroll
        x -= scrollSpeed * dt;

        stateTimer += dt;
        animTimer += dt;

        if (isGrounded) {
            y = groundY;
            vy = 0;
            animFrame = (animTimer % 0.3f < 0.15f) ? 1 : 2;

            // Prepare and initiate jump towards player
            if (stateTimer >= 1.2f) {
                stateTimer = 0f;
                isGrounded = false;
                hasShotInAir = false;
                vy = -160f; // Vertical jump impulse
                vx = (player.x < x) ? -55f : 35f;
            }
        } else {
            // Airborne physics
            vy += 280f * dt; // Gravity
            y += vy * dt;
            x += vx * dt;
            animFrame = (vy < 0) ? 3 : 4;

            // Fire downward projectile at peak of jump arc
            if (!hasShotInAir && vy >= -30f && vy <= 30f) {
                hasShotInAir = true;
                if (x > -10 && x < 450) {
                    enemyBullets.add(Bullet.createEnemyJumper(x + width / 2f, y + height, 0f, 130f));
                }
            }

            // Land on ground
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
