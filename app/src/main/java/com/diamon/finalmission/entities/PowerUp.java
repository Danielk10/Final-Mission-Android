package com.diamon.finalmission.entities;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.diamon.finalmission.engine.AssetLoader;
import com.diamon.finalmission.engine.AudioManager;

/**
 * Cápsulas de poder y potenciadores fieles a NES Final Mission:
 * - 'W': Wide Spread Shot
 * - 'L': Laser Beam
 * - 'S': Speed Up
 * - LIFE: Recuperación de salud / 1-Up
 * Además incluye las cajas de poder flotantes (cajaPoder1..4) que al destruirse liberan un ítem.
 */
public class PowerUp {

    public enum Type {
        POD_CONTAINER, // Caja que al ser impactada libera el ítem
        WIDE,          // 'W'
        LASER,         // 'L'
        SPEED,         // 'S'
        LIFE           // Recuperación de vida
    }

    public float x;
    public float y;
    public float vx = -20f;
    public float vy = 0f;
    public Type type;
    public boolean alive = true;
    public int containerHp = 3;

    private float animTimer = 0f;
    private int animFrame = 0;

    public PowerUp(float x, float y, Type type) {
        this.x = x;
        this.y = y;
        this.type = type;
        if (type == Type.POD_CONTAINER) {
            this.vx = -15f;
        } else {
            this.vx = -25f;
        }
    }

    public void update(float dt) {
        if (!alive) return;

        animTimer += dt;
        if (animTimer >= 0.12f) {
            animTimer = 0f;
            animFrame = (animFrame + 1) % 4;
        }

        x += vx * dt;
        // Suave ondulación flotante
        y += (float) Math.sin(animTimer * 6.0) * 0.25f;

        if (x < -30) {
            alive = false;
        }
    }

    public void applyTo(Player player, AudioManager audio) {
        audio.playSound(AudioManager.SFX_PODER);
        switch (type) {
            case WIDE:
                player.upgradeWeapon(WeaponType.WIDE);
                player.addSatellite();
                break;
            case LASER:
                player.upgradeWeapon(WeaponType.LASER);
                player.addSatellite();
                break;
            case SPEED:
                player.increaseSpeed();
                break;
            case LIFE:
                player.heal(2);
                if (player.health >= Player.MAX_HEALTH) {
                    player.addSatellite();
                }
                break;
            default:
                break;
        }
        alive = false;
    }

    public RectF getBounds() {
        return new RectF(x - 8, y - 8, x + 8, y + 8);
    }

    public void render(Canvas canvas, AssetLoader loader, Paint paint) {
        if (!alive) return;

        Bitmap bmp = null;
        switch (type) {
            case POD_CONTAINER:
                int pFrame = (animFrame % 4) + 1;
                bmp = loader.getBitmap("cajaPoder" + pFrame + ".png");
                break;
            case WIDE:
                bmp = loader.getBitmap("poderW.png");
                break;
            case LASER:
                bmp = loader.getBitmap("poderL.png");
                break;
            case SPEED:
                bmp = loader.getBitmap("poderS.png");
                break;
            case LIFE:
                bmp = loader.getBitmap("vida1.png");
                break;
        }

        if (bmp != null) {
            float drawX = x - bmp.getWidth() / 2f;
            float drawY = y - bmp.getHeight() / 2f;
            canvas.drawBitmap(bmp, drawX, drawY, paint);
        }
    }
}
