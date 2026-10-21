package com.diamon.finalmission;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import java.util.List;
import java.util.Map;

/**
 * Sistema de Satélites orbitales de Final Mission.
 * Los 2 satélites orbitan de forma sincronizada alrededor del jugador y se pueden
 * orientar/bloquear en 360 grados mediante el segundo dedo (dedo libre).
 */
public class Satellite {

    private final int id; // 0 = Satélite Alpha, 1 = Satélite Beta
    private float orbitAngle = 0f;
    private final float orbitRadius = 24f; // Radio orbital en píxeles NES
    private final float orbitSpeed = 2.4f; // Velocidad de rotación libre

    private float x = 0f;
    private float y = 0f;
    private float currentAimAngle = 0f;

    private float fireCooldown = 0f;
    private static final float FIRE_RATE = 0.16f;

    private final Paint paint = new Paint();

    public Satellite(int id) {
        this.id = id;
        this.orbitAngle = id * 180f; // Satélites en posiciones diametralmente opuestas
        this.currentAimAngle = 0f;
        paint.setFilterBitmap(false);
    }

    public void update(float dt, Player player, TouchController touch, List<Bullet> bullets) {
        float centerX = player.getX() + player.getWidth() / 2f;
        float centerY = player.getY() + player.getHeight() / 2f;

        if (!touch.isSatelliteLocked()) {
            // Órbita libre rotacional
            orbitAngle = (orbitAngle + orbitSpeed * 60f * dt) % 360f;
            currentAimAngle = orbitAngle;
        } else {
            // Orientación bloqueada por el segundo dedo
            currentAimAngle = touch.getSatelliteTargetAngle();
            float baseAngle = touch.getSatelliteTargetAngle() + (id == 0 ? 45f : -45f);
            orbitAngle = baseAngle;
        }

        // Posición orbital
        double rad = Math.toRadians(orbitAngle);
        x = centerX + (float) Math.cos(rad) * orbitRadius - 4f;
        y = centerY + (float) Math.sin(rad) * orbitRadius - 4f;

        // Disparo coordinado
        fireCooldown -= dt;
        if (touch.isFiring() && fireCooldown <= 0f) {
            fireCooldown = FIRE_RATE;
            double aimRad = Math.toRadians(currentAimAngle);
            float vx = (float) Math.cos(aimRad) * 5.0f;
            float vy = (float) Math.sin(aimRad) * 5.0f;
            bullets.add(new Bullet(x + 2f, y + 2f, vx, vy, Bullet.TYPE_SATELLITE, vx >= 0));
        }
    }

    public void draw(Canvas canvas, Map<String, Bitmap> spriteMap) {
        // Seleccionar sprite según dirección aproximada (HD, HI, VA, VB, DD, DI)
        String spriteKey = "sateliteHD1.png";
        float angle = (currentAimAngle % 360f + 360f) % 360f;

        if (angle >= 337.5f || angle < 22.5f) {
            spriteKey = "sateliteHD1.png";
        } else if (angle >= 22.5f && angle < 67.5f) {
            spriteKey = "sateliteDD1.png";
        } else if (angle >= 67.5f && angle < 112.5f) {
            spriteKey = "sateliteVB1.png";
        } else if (angle >= 112.5f && angle < 157.5f) {
            spriteKey = "sateliteDI1.png";
        } else if (angle >= 157.5f && angle < 202.5f) {
            spriteKey = "sateliteHI1.png";
        } else if (angle >= 202.5f && angle < 247.5f) {
            spriteKey = "sateliteDI2.png";
        } else if (angle >= 247.5f && angle < 292.5f) {
            spriteKey = "sateliteVA1.png";
        } else {
            spriteKey = "sateliteDD2.png";
        }

        Bitmap sprite = spriteMap.get(spriteKey);
        if (sprite != null) {
            Rect dst = new Rect((int) x, (int) y, (int) (x + 8f), (int) (y + 8f));
            canvas.drawBitmap(sprite, null, dst, paint);
        }
    }
}
