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
 * Agile mechanized combat walker that patrols ground platforms,
 * charges up parabolic leaps toward the player, and launches jumping plasma
 * projectiles (balaSaltador1/2) near the peak of its trajectory.
 */
public class JumperMech extends Enemy {

    private static final int STATE_PATROL = 0;
    private static final int STATE_CHARGE_JUMP = 1;
    private static final int STATE_AIRBORNE = 2;
    private static final int STATE_LANDING = 3;

    private int state = STATE_PATROL;
    private final float groundY;
    private float vx = -20f;
    private float vy = 0f;
    private boolean facingRight = false;
    private float stateTimer = 0f;
    private float animTimer = 0f;
    private int animFrame = 1;
    private boolean hasShotInAir = false;

    public JumperMech(float x, float y) {
        super(x, y, 24f, 29f, 4f, 400);
        this.groundY = y;
        this.state = STATE_PATROL;
        this.stateTimer = 0.8f + (float) (Math.random() * 0.8f);
    }

    @Override
    public void update(float dt, Player player, List<Bullet> enemyBullets, float scrollSpeed) {
        if (!alive) return;

        if (hitFlashTimer > 0f) {
            hitFlashTimer -= dt;
        }

        // Horizontal displacement with level scroll
        x -= scrollSpeed * dt;

        stateTimer += dt;
        animTimer += dt;

        switch (state) {
            case STATE_PATROL:
                // Patrol ground platform
                y = groundY;
                vy = 0f;
                x += (facingRight ? 22f : -22f) * dt;

                // Alternate walking steps (saltador1 / saltador2)
                animFrame = ((int) (animTimer / 0.16f) % 2) + 1;

                // Turn or initiate leap towards player
                if (stateTimer >= 1.4f) {
                    stateTimer = 0f;
                    facingRight = (player.x > x);
                    state = STATE_CHARGE_JUMP;
                    animFrame = 2; // Crouch charge pose
                }
                break;

            case STATE_CHARGE_JUMP:
                // Crouch and charge leap impulse
                y = groundY;
                animFrame = 2;
                if (stateTimer >= 0.35f) {
                    stateTimer = 0f;
                    state = STATE_AIRBORNE;
                    hasShotInAir = false;
                    vy = -170f; // High vertical leap impulse
                    vx = (player.x < x) ? -45f : 40f;
                    animFrame = 3; // Ascending leap pose
                }
                break;

            case STATE_AIRBORNE:
                // Airborne physics: gravity and forward momentum
                vy += 275f * dt;
                y += vy * dt;
                x += vx * dt;

                // Sprite pose: frame 3 during ascent, frame 4 during descent
                animFrame = (vy < 0) ? 3 : 4;

                // Fire downward projectile at peak of jump arc
                if (!hasShotInAir && Math.abs(vy) <= 35f) {
                    hasShotInAir = true;
                    if (x > -15f && x < 550f) {
                        float bulletVx = (player.x < x) ? -30f : 30f;
                        enemyBullets.add(Bullet.createEnemyJumper(x + width / 2f, y + height, bulletVx, 135f));
                    }
                }

                // Land on ground surface
                if (y >= groundY) {
                    y = groundY;
                    vy = 0f;
                    vx = 0f;
                    state = STATE_LANDING;
                    stateTimer = 0f;
                    animFrame = 2; // Impact compression crouch
                }
                break;

            case STATE_LANDING:
                y = groundY;
                animFrame = 2;
                if (stateTimer >= 0.2f) {
                    stateTimer = 0f;
                    state = STATE_PATROL;
                    facingRight = (player.x > x);
                }
                break;
        }

        if (x < -50f) {
            alive = false;
        }
    }

    @Override
    public void render(Canvas canvas, AssetLoader loader, Paint paint) {
        if (!alive) return;

        String spriteName = "saltador" + animFrame + ".png";
        Bitmap bmp = loader.getBitmap(spriteName);
        if (bmp != null) {
            // saltador sprites natively face left; flip horizontally when facing right
            if (facingRight) {
                bmp = loader.getFlippedBitmap(bmp, true, false);
            }
            if (bmp != null) {
                float drawX = x + (width - bmp.getWidth()) / 2f;
                float drawY = y + (height - bmp.getHeight());
                canvas.drawBitmap(bmp, drawX, drawY, getActivePaint(paint));
            }
        }
    }
}
