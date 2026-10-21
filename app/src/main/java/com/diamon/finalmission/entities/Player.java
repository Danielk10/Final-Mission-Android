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
 * Personaje jugador (Arnold / Sigourney) para Final Mission.
 * Equipado con propulsión por jetpack, disparo automático continuo,
 * gestión de satélites y sistema de salud/vidas fiel a NES.
 */
public class Player {

    public static final int MAX_HEALTH = 6;
    public static final int STARTING_LIVES = 3;

    public float x = 40f;
    public float y = 100f;
    public float width = 23f;
    public float height = 29f;

    public boolean facingRight = true;
    public int characterId = 1; // 1 = Arnold, 2 = Sigourney
    public float screenWidth = 256f;
    public int health = MAX_HEALTH;
    public int lives = STARTING_LIVES;
    public boolean alive = true;

    public WeaponType weaponType = WeaponType.NORMAL;
    public float speedMultiplier = 1.0f;

    // Satélites orbitales (hasta 2)
    public final List<Satellite> satellites = new ArrayList<>();

    // Cuadros de animación jetpack
    private float animTimer = 0f;
    private int animFrame = 0;

    // Cadencia de disparo continuo
    private float fireCooldown = 0f;
    private static final float FIRE_RATE = 0.12f; // ~8 disparos/segundo

    // Invulnerabilidad y parpadeo tras daño o reaparición
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
        immuneTimer = 2.0f; // Breve inmunidad al comenzar
        satellites.clear();
        // Iniciar con 1 satélite para acción arcade clásica de NES
        satellites.add(new Satellite(0));
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

        // Animación de propulsión de jetpack
        animTimer += dt;
        if (animTimer >= 0.08f) {
            animTimer = 0f;
            animFrame = (animFrame + 1) % 6;
        }

        // Gestión de invulnerabilidad
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

        // Límites en pantalla horizontal widescreen retro
        x = Math.max(2f, Math.min(screenWidth - width - 2f, x));
        y = Math.max(16f, Math.min(224f - height - 4f, y));

        // Actualizar satélites
        float centerX = x + width / 2f;
        float centerY = y + height / 2f;
        for (Satellite sat : satellites) {
            sat.update(dt, centerX, centerY, lockSatellites, satelliteAimAngle);
        }

        // Cadencia y disparo automático continuo mientras se toca la pantalla
        if (fireCooldown > 0f) {
            fireCooldown -= dt;
        }

        if (isTouching && fireCooldown <= 0f) {
            fire(bulletList, audio);
            fireCooldown = FIRE_RATE;
        }
    }

    private void fire(List<Bullet> bulletList, AudioManager audio) {
        float muzzleX = facingRight ? (x + width) : x;
        float muzzleY = y + height / 2f - 2f;

        switch (weaponType) {
            case NORMAL:
                bulletList.add(Bullet.createPlayerNormal(muzzleX, muzzleY, facingRight));
                audio.playSound(AudioManager.SFX_DISPARO);
                break;
            case WIDE:
                bulletList.add(Bullet.createPlayerWide(muzzleX, muzzleY, facingRight, 0f));
                bulletList.add(Bullet.createPlayerWide(muzzleX, muzzleY, facingRight, -18f));
                bulletList.add(Bullet.createPlayerWide(muzzleX, muzzleY, facingRight, 18f));
                audio.playSound(AudioManager.SFX_DISPARO);
                break;
            case LASER:
                bulletList.add(Bullet.createPlayerLaser(muzzleX, muzzleY, facingRight));
                audio.playSound(AudioManager.SFX_DISPARO_L);
                break;
        }

        // Disparo coordinado de los satélites
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
                // Reaparecer
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
        return new RectF(x + 3f, y + 4f, x + width - 3f, y + height - 3f);
    }

    public String getCharacterName() {
        return characterId == 2 ? "SIGOURNEY" : "ARNOLD";
    }

    public void render(Canvas canvas, AssetLoader loader, Paint paint) {
        if (!alive || !visible) return;

        // Renderizar satélites
        for (Satellite sat : satellites) {
            sat.render(canvas, loader, paint);
        }

        // Renderizar sprite de Arnold (1P) o Sigourney (2P) con animación de propulsión de jetpack
        String spriteName;
        boolean needsManualFlip = false;
        if (characterId == 2) {
            if (facingRight) {
                int frameIdx = (animFrame % 4) + 1;
                spriteName = "jugador2D" + frameIdx + ".png";
            } else {
                spriteName = "jugador2I1.png";
            }
        } else {
            if (facingRight) {
                int frameIdx = (animFrame % 6) + 1;
                spriteName = "jugador1D" + frameIdx + ".png";
            } else {
                int frameIdx = (animFrame % 3) + 1;
                spriteName = "jugador1I" + frameIdx + ".png";
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
