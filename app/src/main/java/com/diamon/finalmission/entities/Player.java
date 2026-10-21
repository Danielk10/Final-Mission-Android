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

    // Movement tracking for movement-driven satellite orbit
    public float prevX = 40f;
    public float prevY = 100f;
    public boolean isMoving = false;

    // Death disintegration state & timing (authentic 0.6s 3-frame sequence)
    public boolean isDying = false;
    public float deathTimer = 0f;
    public static final float DEATH_DURATION = 0.60f; // 0.6s total disintegration
    public boolean shouldSpawnDeathExplosion = false;

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

    public Player(float x, float y) {
        reset();
        this.x = x;
        this.y = y;
        this.prevX = x;
        this.prevY = y;
    }

    public void reset() {
        x = 40f;
        y = 100f;
        prevX = 40f;
        prevY = 100f;
        isMoving = false;
        isDying = false;
        deathTimer = 0f;
        shouldSpawnDeathExplosion = false;
        facingRight = true;
        health = MAX_HEALTH;
        lives = STARTING_LIVES;
        weaponType = WeaponType.NORMAL;
        speedMultiplier = 1.0f;
        alive = true;
        immuneTimer = IMMUNE_DURATION;
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

        // Death disintegration handling: 3 frames over 0.6s
        if (isDying) {
            deathTimer += dt;
            isFiring = false;
            if (deathTimer >= DEATH_DURATION) {
                isDying = false;
                deathTimer = 0f;
                shouldSpawnDeathExplosion = true;
                lives--;
                if (lives > 0) {
                    health = MAX_HEALTH;
                    x = 40f;
                    y = 100f;
                    prevX = 40f;
                    prevY = 100f;
                    immuneTimer = IMMUNE_DURATION;
                    speedMultiplier = 1.0f;
                    weaponType = WeaponType.NORMAL;
                    satellites.clear();
                    satellites.add(new Satellite(0));
                    satellites.add(new Satellite(1));
                } else {
                    alive = false;
                }
            }
            return;
        }

        // Movement displacement tracking for movement-driven satellite orbit
        float dx = x - prevX;
        float dy = y - prevY;
        float moveDist = (float) Math.hypot(dx, dy);
        isMoving = (moveDist > 0.05f);
        prevX = x;
        prevY = y;

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

        // Update orbital satellites with movement information
        float centerX = x + width / 2f;
        float centerY = y + height / 2f;
        for (Satellite sat : satellites) {
            sat.update(dt, centerX, centerY, lockSatellites, satelliteAimAngle, dx, dy, isMoving, moveDist);
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
                if (audio != null) audio.playSound(AudioManager.SFX_DISPARO);
                break;
            case WIDE:
                bulletList.add(Bullet.createPlayerWide(muzzleX, muzzleY, facingRight, 0f));
                bulletList.add(Bullet.createPlayerWide(muzzleX, muzzleY, facingRight, -15f));
                bulletList.add(Bullet.createPlayerWide(muzzleX, muzzleY, facingRight, 15f));
                if (audio != null) audio.playSound(AudioManager.SFX_DISPARO);
                break;
            case LASER:
                bulletList.add(Bullet.createPlayerLaser(muzzleX, muzzleY, facingRight));
                if (audio != null) audio.playSound(AudioManager.SFX_DISPARO_L);
                break;
            case BOMB:
                bulletList.add(Bullet.createPlayerBomb(muzzleX, muzzleY, facingRight));
                if (audio != null) audio.playSound(AudioManager.SFX_DISPARO);
                break;
        }

        // Coordinated satellite gunfire
        for (Satellite sat : satellites) {
            bulletList.add(sat.fire(weaponType));
        }
    }

    public boolean takeDamage(int amount, AudioManager audio) {
        if (!alive || isDying || immuneTimer > 0f) {
            return false;
        }

        health -= amount;
        immuneTimer = IMMUNE_DURATION;
        // In authentic Japanese Final Mission, taking a hit loses special weapon back to Normal
        weaponType = WeaponType.NORMAL;

        if (health <= 0) {
            health = 0;
            isDying = true;
            deathTimer = 0f;
            isFiring = false;
            satellites.clear();
            if (audio != null) {
                audio.playSound(AudioManager.SFX_MURIENDO);
            }
        } else {
            if (audio != null) {
                audio.playSound(AudioManager.SFX_MURIENDO);
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
        if (!alive || isDying) {
            return new RectF(-1000f, -1000f, -1000f, -1000f);
        }
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
        if (!alive || !visible || loader == null) return;

        if (isDying) {
            // Authentic death disintegration sequence across 0.6s (3 frames, ~0.2s each)
            int dFrame = Math.min(2, (int) (deathTimer / 0.20f));
            String spriteName;
            if (characterId == 2) {
                // Sergei Missionov disintegration: jugador2D2 (0-0.2s) -> jugador2D3 (0.2-0.4s) -> jugador2D4 (0.4-0.6s)
                spriteName = "jugador2D" + (dFrame + 2) + ".png";
            } else {
                // Sparky disintegration: jugador1D4 (0-0.2s) -> jugador1D5 (0.2-0.4s) -> jugador1D6 (0.4-0.6s)
                spriteName = "jugador1D" + (dFrame + 4) + ".png";
            }
            Bitmap bmp = loader.getBitmap(spriteName);
            if (bmp != null) {
                if (!facingRight) {
                    bmp = loader.getFlippedBitmap(bmp, true, false);
                }
                float drawX = x + (width - bmp.getWidth()) / 2f;
                float drawY = y + (height - bmp.getHeight()) / 2f;
                canvas.drawBitmap(bmp, drawX, drawY, paint);
            }
            return;
        }

        // Render satellites
        for (Satellite sat : satellites) {
            sat.render(canvas, loader, paint);
        }

        // Render player sprite with authentic frame switching:
        // Idle/Movement: frame 1 (jugador1D1.png / jugador1I1.png for Sparky, jugador2D1.png / jugador2I1.png for Sergei)
        // Shooting: alternation between frame 2 and frame 3
        Bitmap bmp;
        if (characterId == 2) {
            // Sergei Missionov (2P)
            if (isFiring) {
                // NEVER use jugador2D2..D4 for firing (those are death disintegration frames!)
                // Use red-suit tinted version of jugador1 firing frames:
                int frameIdx = (fireAnimFrame % 2) + 2;
                String baseName = facingRight ? ("jugador1D" + frameIdx + ".png") : ("jugador1I" + frameIdx + ".png");
                bmp = loader.getSergeiBitmap(baseName);
            } else {
                String spriteName = facingRight ? "jugador2D1.png" : "jugador2I1.png";
                bmp = loader.getBitmap(spriteName);
            }
        } else {
            // Sparky (1P)
            if (isFiring) {
                int frameIdx = (fireAnimFrame % 2) + 2;
                String spriteName = facingRight ? ("jugador1D" + frameIdx + ".png") : ("jugador1I" + frameIdx + ".png");
                bmp = loader.getBitmap(spriteName);
            } else {
                String spriteName = facingRight ? "jugador1D1.png" : "jugador1I1.png";
                bmp = loader.getBitmap(spriteName);
            }
        }

        if (bmp != null) {
            float drawX = x + (width - bmp.getWidth()) / 2f;
            float drawY = y + (height - bmp.getHeight()) / 2f;
            canvas.drawBitmap(bmp, drawX, drawY, paint);
        }
    }
}
