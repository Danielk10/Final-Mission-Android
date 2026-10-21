package com.diamon.finalmission.entities;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.diamon.finalmission.engine.AssetLoader;

/**
 * Pod de satélite orbital auténtico de Final Mission NES.
 * Orbita libremente alrededor del jugador o se bloquea y orienta en 360 grados
 * mediante el dedo libre secundario.
 */
public class Satellite {

    public final int podIndex;
    public float x;
    public float y;
    public float orbitAngle = 0f;
    public float orbitRadius = 22f;
    public float orbitSpeed = 3.6f;

    public boolean isLocked = false;
    public float aimAngle = 0f;

    private float animTimer = 0f;
    private int animFrame = 0;

    public Satellite(int podIndex) {
        this.podIndex = podIndex;
        this.orbitAngle = (float) (podIndex * Math.PI);
        this.aimAngle = 0f;
    }

    public void update(float dt, float playerCenterX, float playerCenterY, boolean lockActive, Float targetAngle) {
        animTimer += dt;
        if (animTimer >= 0.10f) {
            animTimer = 0f;
            animFrame = (animFrame + 1) % 2;
        }

        this.isLocked = lockActive;

        if (lockActive && targetAngle != null) {
            this.aimAngle = targetAngle;
            // Cuando están bloqueados se colocan en los flancos perpendiculares al vector de disparo
            float flankOffset = (podIndex == 0 ? 1f : -1f) * (float) (Math.PI / 2.0);
            float posAngle = aimAngle + flankOffset;
            this.x = playerCenterX + (float) Math.cos(posAngle) * orbitRadius;
            this.y = playerCenterY + (float) Math.sin(posAngle) * orbitRadius;
        } else {
            // Órbita fluida continua
            orbitAngle += orbitSpeed * dt;
            this.aimAngle = orbitAngle;
            float currentAngle = (float) (orbitAngle + (podIndex * Math.PI));
            this.x = playerCenterX + (float) Math.cos(currentAngle) * orbitRadius;
            this.y = playerCenterY + (float) Math.sin(currentAngle) * orbitRadius;
        }
    }

    public Bullet fire() {
        return Bullet.createSatellite(x, y, aimAngle);
    }

    public void render(Canvas canvas, AssetLoader loader, Paint paint) {
        // Normalizar ángulo entre -PI y PI
        double normAngle = Math.atan2(Math.sin(aimAngle), Math.cos(aimAngle));
        double deg = Math.toDegrees(normAngle);

        int frame = animFrame + 1; // 1 o 2
        String spriteName = "sateliteHD" + frame + ".png";
        boolean flipY = false;

        if (deg >= -22.5 && deg < 22.5) {
            spriteName = "sateliteHD" + frame + ".png";
        } else if (deg >= 22.5 && deg < 67.5) {
            spriteName = "sateliteDD" + frame + ".png";
        } else if (deg >= 67.5 && deg < 112.5) {
            spriteName = "sateliteVB" + frame + ".png";
        } else if (deg >= 112.5 && deg < 157.5) {
            spriteName = "sateliteDI" + frame + ".png";
        } else if (deg >= 157.5 || deg < -157.5) {
            spriteName = "sateliteHI" + frame + ".png";
        } else if (deg >= -157.5 && deg < -112.5) {
            spriteName = "sateliteDI" + frame + ".png";
            flipY = true;
        } else if (deg >= -112.5 && deg < -67.5) {
            spriteName = "sateliteVA" + frame + ".png";
        } else if (deg >= -67.5 && deg < -22.5) {
            spriteName = "sateliteDD" + frame + ".png";
            flipY = true;
        }

        Bitmap bmp = loader.getBitmap(spriteName);
        if (bmp != null) {
            if (flipY) {
                bmp = loader.getFlippedBitmap(bmp, false, true);
            }
            float drawX = x - bmp.getWidth() / 2f;
            float drawY = y - bmp.getHeight() / 2f;
            canvas.drawBitmap(bmp, drawX, drawY, paint);
        }
    }
}
