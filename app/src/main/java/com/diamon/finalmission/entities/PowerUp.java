package com.diamon.finalmission.entities;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.diamon.finalmission.engine.AssetLoader;
import com.diamon.finalmission.engine.AudioManager;

/**
 * Power-Up and Item Box system for Final Mission NES:
 * - ITEM_BOX (Canister): Floats smoothly across screen with 4-frame animation (cajaPoder1..4.png).
 *   Upon taking bullet hits, cracks open and releases a power-up capsule.
 * - CAPSULE (PowerUpCapsule):
 *   * 'W' (poderW.png): Wide Spread Shot (3-way spread wave)
 *   * 'L' (poderL.png): Laser Beam (piercing laser)
 *   * 'S' (poderS.png): Speed Up (propulsion speed boost)
 *   * 'B' (poderB.png): Life Energy Recovery (heals health segments - strictly NO BOMBS)
 *
 * Authentic oscillation physics ported from the original engine.
 */
public class PowerUp {

    public enum Type {
        ITEM_BOX,      // Floating canister container (cajaPoder1..4.png)
        CAJA,          // Legacy alias
        POD_CONTAINER, // Legacy alias
        WIDE,          // 'W'
        LASER,         // 'L'
        BOMB,          // 'B' (Heavy bomb weapon)
        SPEED,         // 'S'
        LIFE           // 'B' (Life energy recovery - authentic NES)
    }

    public float x;
    public float y;
    public float baseY;
    public float vx = -20f;
    public float vy = 0f;
    public Type type;
    public Type dropType = Type.WIDE;
    public boolean alive = true;

    // Item box durability
    public int containerHp = 2;
    public float hitFlashTimer = 0f;

    // Animation & floating bobbing
    private float animTimer = 0f;
    private int animFrame = 0;
    private float floatTimer = 0f;

    // Movement impulse cycle ported from the authentic engine (150 frames / ~2.5s)
    private float cycleTimer = 0f;
    private boolean impulseActive = false;
    private float impulseDuration = 0f;

    public PowerUp(float x, float y, Type type) {
        this(x, y, type, chooseRandomDrop());
    }

    public PowerUp(float x, float y, Type type, Type dropType) {
        this.x = x;
        this.y = y;
        this.baseY = y;
        this.type = type;
        this.dropType = dropType;

        if (isContainer()) {
            this.vx = -18f;
            this.containerHp = 2;
        } else {
            // Authentic base drift speed (~16 px/sec at retro resolution)
            this.vx = -16f;
        }
    }

    public boolean isContainer() {
        return type == Type.ITEM_BOX || type == Type.CAJA || type == Type.POD_CONTAINER;
    }

    public static Type chooseRandomDrop() {
        double r = Math.random();
        if (r < 0.25) return Type.WIDE;
        if (r < 0.50) return Type.LASER;
        if (r < 0.70) return Type.BOMB;
        if (r < 0.85) return Type.SPEED;
        return Type.LIFE; // 'B'
    }

    public void update(float dt) {
        if (!alive) return;

        animTimer += dt;
        floatTimer += dt;

        if (hitFlashTimer > 0f) {
            hitFlashTimer -= dt;
        }

        if (isContainer()) {
            // 1. Item box canister: 4-frame rotation animation
            if (animTimer >= 0.12f) {
                animTimer = 0f;
                animFrame = (animFrame + 1) % 4;
            }

            // Smooth horizontal drift with gentle vertical wave bobbing
            x += vx * dt;
            y = baseY + (float) Math.sin(floatTimer * 2.4f) * 6f;
        } else {
            // 2. Power-up capsule: periodic surge impulse
            cycleTimer += dt;
            if (cycleTimer >= 2.5f) {
                cycleTimer = 0f;
                impulseActive = true;
                impulseDuration = 0.8f;
            }

            float currentVx = vx;
            if (impulseActive) {
                impulseDuration -= dt;
                currentVx -= 28f; // Extra propulsion burst
                if (impulseDuration <= 0f) {
                    impulseActive = false;
                }
            }

            x += currentVx * dt;
            // Gentle sinusoidal hovering in midair
            y = baseY + (float) Math.sin(floatTimer * 3.2f) * 5f;
        }

        // Despawn offscreen left
        if (x < -40f) {
            alive = false;
        }
    }

    public boolean takeHit() {
        if (!isContainer() || !alive) return false;
        containerHp--;
        hitFlashTimer = 0.08f;
        return containerHp <= 0;
    }

    public void applyTo(Player player, AudioManager audio) {
        if (audio != null) {
            audio.playSound(AudioManager.SFX_PODER);
        }
        switch (type) {
            case WIDE:
                player.upgradeWeapon(WeaponType.WIDE);
                player.addSatellite();
                break;
            case LASER:
                player.upgradeWeapon(WeaponType.LASER);
                player.addSatellite();
                break;
            case BOMB:
            case LIFE:
                // 'B' Capsule: Upgrades to Bomb weapon, restores 1 life pip, and awards satellite
                player.upgradeWeapon(WeaponType.BOMB);
                player.heal(1);
                player.addSatellite();
                break;
            case SPEED:
                player.increaseSpeed();
                break;
            default:
                break;
        }
        alive = false;
    }

    public RectF getBounds() {
        float r = isContainer() ? 10f : 8f;
        return new RectF(x - r, y - r, x + r, y + r);
    }

    public void render(Canvas canvas, AssetLoader loader, Paint paint) {
        if (!alive) return;

        Bitmap bmp = null;
        if (isContainer()) {
            int pFrame = (animFrame % 4) + 1;
            bmp = loader.getBitmap("cajaPoder" + pFrame + ".png");
        } else {
            switch (type) {
                case WIDE:
                    bmp = loader.getBitmap("poderW.png");
                    break;
                case LASER:
                    bmp = loader.getBitmap("poderL.png");
                    break;
                case BOMB:
                case LIFE:
                    bmp = loader.getBitmap("poderB.png");
                    break;
                case SPEED:
                    bmp = loader.getBitmap("poderS.png");
                    break;
                default:
                    break;
            }
        }

        if (bmp != null) {
            float drawX = x - bmp.getWidth() / 2f;
            float drawY = y - bmp.getHeight() / 2f;
            if (isContainer() && hitFlashTimer > 0f) {
                // Visual hit feedback: brief flash when canister is struck by bullet
                Paint flashPaint = new Paint(paint);
                flashPaint.setColorFilter(new android.graphics.LightingColorFilter(0xFFFFFFFF, 0x00888888));
                canvas.drawBitmap(bmp, drawX, drawY, flashPaint);
            } else {
                canvas.drawBitmap(bmp, drawX, drawY, paint);
            }
        }
    }
}
