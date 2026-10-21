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
 * Heavy bipedal mechanized soldier patrolling ground platforms with authentic
 * mechanized walking gait and engaging in periodic multi-shot forward plasma gun bursts.
 */
public class WalkerRobot extends Enemy {

    private static final int STATE_PATROL = 0;
    private static final int STATE_BURST_FIRE = 1;

    private int state = STATE_PATROL;
    public boolean facingRight = false;
    private float patrolTimer = 0f;
    private float patrolSpeed = 22f;
    private float shootTimer = 1.4f;

    private int burstShotsRemaining = 0;
    private float burstIntervalTimer = 0f;
    private float postBurstPauseTimer = 0f;
    private float walkBobTimer = 0f;

    public WalkerRobot(float x, float y) {
        super(x, y, 24f, 32f, 6f, 500);
        this.shootTimer = 1.0f + (float) (Math.random() * 1.0f);
    }

    @Override
    public void update(float dt, Player player, List<Bullet> enemyBullets, float scrollSpeed) {
        if (!alive) return;

        if (hitFlashTimer > 0f) {
            hitFlashTimer -= dt;
        }

        // Horizontal level scroll displacement
        x -= scrollSpeed * dt;

        switch (state) {
            case STATE_PATROL:
                walkBobTimer += dt * 8.0f;
                patrolTimer += dt;
                if (patrolTimer >= 2.2f) {
                    patrolTimer = 0f;
                    facingRight = !facingRight;
                }

                // Smoothly patrol ground
                x += (facingRight ? patrolSpeed : -patrolSpeed) * dt;

                // Face player when in tactical combat range
                if (Math.abs(player.x - x) < 200f) {
                    facingRight = (player.x > x);
                }

                // Check firing trigger
                shootTimer -= dt;
                if (shootTimer <= 0f) {
                    if (x > -15f && x < 550f && player.alive) {
                        state = STATE_BURST_FIRE;
                        burstShotsRemaining = 3;
                        burstIntervalTimer = 0f;
                    } else {
                        shootTimer = 1.5f;
                    }
                }
                break;

            case STATE_BURST_FIRE:
                // Stationary forward burst volley
                burstIntervalTimer -= dt;
                if (burstIntervalTimer <= 0f && burstShotsRemaining > 0) {
                    burstIntervalTimer = 0.14f;
                    burstShotsRemaining--;

                    float muzzleX = facingRight ? (x + width + 2f) : (x - 4f);
                    float muzzleY = y + 11f;
                    float targetX = facingRight ? (muzzleX + 100f) : (muzzleX - 100f);
                    // Slight variation to cover player height
                    float targetY = player.alive ? (player.y + player.height / 2f) : muzzleY;
                    enemyBullets.add(Bullet.createEnemyNormal(muzzleX, muzzleY, targetX, targetY, 150f));

                    if (burstShotsRemaining == 0) {
                        postBurstPauseTimer = 0.45f;
                    }
                }

                if (burstShotsRemaining == 0) {
                    postBurstPauseTimer -= dt;
                    if (postBurstPauseTimer <= 0f) {
                        state = STATE_PATROL;
                        shootTimer = 2.0f + (float) (Math.random() * 0.8f);
                        if (player.alive) {
                            facingRight = (player.x > x);
                        }
                    }
                }
                break;
        }

        if (x < -60f) {
            alive = false;
        }
    }

    @Override
    public void render(Canvas canvas, AssetLoader loader, Paint paint) {
        if (!alive) return;

        String spriteName = facingRight ? "robotD.png" : "robotI.png";
        Bitmap bmp = loader.getBitmap(spriteName);
        if (bmp != null) {
            float drawX = x;
            float drawY = y;
            // Mechanized walking bob oscillation (1 pixel step)
            if (state == STATE_PATROL && ((int) walkBobTimer % 2 == 1)) {
                drawY -= 1.0f;
            }
            canvas.drawBitmap(bmp, drawX, drawY, getActivePaint(paint));
        }
    }
}
