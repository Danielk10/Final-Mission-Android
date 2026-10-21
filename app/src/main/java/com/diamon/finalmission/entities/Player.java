package com.diamon.finalmission.entities;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.diamon.finalmission.engine.AssetLoader;
import com.diamon.finalmission.engine.AudioManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Player Character (Sparky / Sergei Missionov) for Final Mission (Japanese NES):
 * - 3 Health blocks / max HP = 3 (strictly authentic Japanese Final Mission, unlike US 6 HP).
 * - Authentic shooting animation from original engine:
 *   * Idle / movement without firing: frame 1 (jugador1D1.png / jugador1I1.png).
 *   * Firing: alternating between frame 2 and frame 3 (jugador1D2/3.png or jugador1I2/3.png).
 * - Orbital satellites with 360-degree rotation and angle locking.
 */
public class Player {

    public static final int MAX_HEALTH = 3;
    public static final int STARTING_LIVES = 3;

    public float x = 40f;
    public float y = 100f;
    public float width = 23f;
    public float height = 29f;

    public boolean facingRight = true;
    public int characterId = 1; // 1 = Sparky (1P), 2 = Sergei Missionov (2P)
    public float screenWidth = 256f;
    public int health = MAX_HEALTH;
    public int lives = STARTING_LIVES;
    public boolean alive = true;

    public WeaponType weaponType = WeaponType.NORMAL;
    public float speedMultiplier = 1.0f;

    // Orbital satellites (up to 2)
    public final List<Satellite> satellites = new ArrayList<>();

    // Firing state & animation
    public boolean isFiring = false;
    private float fireAnimTimer = 0f;
    private int fireAnimFrame = 0;

    // Continuous autofire cooldown
    private float fireCooldown = 0f;
    private static final float FIRE_RATE = 0.12f; // ~8 shots per second

    // Invulnerability & damage flicker
    public float immuneTimer = 0f;
    private static final float IMMUNE_DURATION = 2.0f;
    private float blinkTimer = 0f;
    private boolean visible = true;

    public Player() {
        reset();
    }

    public void reset() {
        x = 40f;
        y = 100f;
        facingRight = true;
        health = MAX_HEALTH;
        lives = STARTING_LIVES;
        weaponType = WeaponType.NORMAL;
        speedMultiplier = 1.0f;
        alive = true;
        immuneTimer = 2.0f;
        isFiring = false;
        fireAnimTimer = 0f;
        fireAnimFrame = 0;
        satellites.clear();
        satellites.add(new Satellite(0));
        satellites.add(new Satellite(1));
    }

    public void addSatellite() {
        if (satellites.size() < 2) {
            satellites.add(new Satellite(satellites.size()));
        }
    }

    public void setFacing(boolean right) {
        this.facingRight = right;
    }

    public void update(float dt, boolean isTouching, boolean lockSatellites, Float satelliteAimAngle,
                       List<Bullet> bulletList, AudioManager audio) {
        if (!alive) return;

        this.isFiring = isTouching;

        // Firing animation (alternates frames 2 and 3 while firing)
        if (isFiring) {
            fireAnimTimer += dt;
            if (fireAnimTimer >= 0.08f) {
                fireAnimTimer = 0f;
                fireAnimFrame = (fireAnimFrame + 1) % 2;
            }
        } else {
            fireAnimTimer = 0f;
            fireAnimFrame = 0;
        }

        // Invulnerability management
        if (immuneTimer > 0f) {
            immuneTimer -= dt;
            blinkTimer += dt;
            if (blinkTimer >= 0.05f) {
                blinkTimer = 0f;
                visible = !visible;
            }
        } else {
            visible = true;
        }

        // Screen boundary limits
        x = Math.max(2f, Math.min(screenWidth - width - 2f, x));
        y = Math.max(16f, Math.min(224f - height - 4f, y));

        // Update orbital satellites
        float centerX = x + width / 2f;
        float centerY = y + height / 2f;
        for (Satellite sat : satellites) {
            sat.update(dt, centerX, centerY, lockSatellites, satelliteAimAngle);
        }

        // Continuous autofire while touching the screen
        if (fireCooldown > 0f) {
            fireCooldown -= dt;
        }

        if (isTouching && fireCooldown <= 0f) {
            fire(bulletList, audio);
            fireCooldown = FIRE_RATE;
        }
    }

    private void fire(List<Bullet> bulletList, AudioManager audio) {
        float muzzleX = facingRight ? (x + width + 2f) : (x - 8f);
        float muzzleY = y + height / 2f - 3f;

        switch (weaponType) {
            case NORMAL:
                bulletList.add(Bullet.createPlayerNormal(muzzleX, muzzleY, facingRight));
                audio.playSound(AudioManager.SFX_DISPARO);
                break;
            case WIDE:
                bulletList.add(Bullet.createPlayerWide(muzzleX, muzzleY, facingRight, 0f));
                bulletList.add(Bullet.createPlayerWide(muzzleX, muzzleY, facingRight, -15f));
                bulletList.add(Bullet.createPlayerWide(muzzleX, muzzleY, facingRight, 15f));
                audio.playSound(AudioManager.SFX_DISPARO);
                break;
            case LASER:
                bulletList.add(Bullet.createPlayerLaser(muzzleX, muzzleY, facingRight));
                audio.playSound(AudioManager.SFX_DISPARO_L);
                break;
        }

        // Coordinated satellite gunfire
        for (Satellite sat : satellites) {
            bulletList.add(sat.fire());
        }
    }

    public boolean takeDamage(int amount, AudioManager audio) {
        if (!alive || immuneTimer > 0f) {
            return false;
        }

        health -= amount;
        audio.playSound(AudioManager.SFX_MURIENDO);
        immuneTimer = IMMUNE_DURATION;

        if (health <= 0) {
            lives--;
            if (lives > 0) {
                // Respawn with full 3 pips
                health = MAX_HEALTH;
                x = 40f;
                y = 100f;
                immuneTimer = 2.5f;
            } else {
                alive = false;
            }
        }
        return true;
    }

    public void heal(int amount) {
        health = Math.min(MAX_HEALTH, health + amount);
    }

    public void upgradeWeapon(WeaponType newWeapon) {
        this.weaponType = newWeapon;
    }

    public void increaseSpeed() {
        this.speedMultiplier = Math.min(1.8f, this.speedMultiplier + 0.25f);
    }

    public RectF getBounds() {
        return new RectF(x + 2f, y + 2f, x + width - 2f, y + height - 2f);
    }

    /**
     * Authentic Japanese Final Mission character names:
     * 1P = SPARKY
     * 2P = SERGEI MISSIONOV
     */
    public String getCharacterName() {
        return characterId == 2 ? "SERGEI MISSIONOV" : "SPARKY";
    }

    public String getShortCharacterName() {
        return characterId == 2 ? "SERGEI" : "SPARKY";
    }

    public void render(Canvas canvas, AssetLoader loader, Paint paint) {
        if (!alive || !visible) return;

        // Render satellites
        for (Satellite sat : satellites) {
            sat.render(canvas, loader, paint);
        }

        // Render player sprite with authentic frame switching:
        // Idle/Movement: frame 1 (jugador1D1.png / jugador1I1.png)
        // Shooting: alternation between frame 2 and frame 3
        String spriteName;
        if (characterId == 2) {
            // Sergei Missionov (2P)
            if (isFiring) {
                int frameIdx = (fireAnimFrame % 2) + 2;
                spriteName = facingRight ? ("jugador2D" + frameIdx + ".png") : "jugador2I1.png";
            } else {
                spriteName = facingRight ? "jugador2D1.png" : "jugador2I1.png";
            }
        } else {
            // Sparky (1P)
            if (isFiring) {
                int frameIdx = (fireAnimFrame % 2) + 2;
                spriteName = facingRight ? ("jugador1D" + frameIdx + ".png") : ("jugador1I" + frameIdx + ".png");
            } else {
                spriteName = facingRight ? "jugador1D1.png" : "jugador1I1.png";
            }
        }

        Bitmap bmp = loader.getBitmap(spriteName);
        if (bmp != null) {
            float drawX = x + (width - bmp.getWidth()) / 2f;
            float drawY = y + (height - bmp.getHeight()) / 2f;
            canvas.drawBitmap(bmp, drawX, drawY, paint);
        }
    }
}
