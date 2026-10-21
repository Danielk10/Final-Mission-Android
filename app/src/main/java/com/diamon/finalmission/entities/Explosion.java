package com.diamon.finalmission.entities;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.diamon.finalmission.engine.AssetLoader;

/**
 * Efecto visual de explosión de proyectiles, enemigos y jefes.
 */
public class Explosion {

    public enum Type {
        NORMAL,
        MISSILE,
        BOSS,
        MISSILE_SMOKE,
        SMOKE
    }

    public float x;
    public float y;
    public Type type;
    public boolean alive = true;

    private float timer = 0f;
    private int currentFrame = 1;
    private final int maxFrames;
    private final float frameDuration;

    public Explosion(float x, float y, Type type) {
        this.x = x;
        this.y = y;
        this.type = type;

        if (type == Type.BOSS) {
            maxFrames = 5;
            frameDuration = 0.08f;
        } else if (type == Type.MISSILE_SMOKE || type == Type.SMOKE) {
            maxFrames = 3;
            frameDuration = 0.05f;
        } else {
            maxFrames = 4;
            frameDuration = 0.06f;
        }
    }

    public void update(float dt) {
        if (!alive) return;
        timer += dt;
        if (timer >= frameDuration) {
            timer = 0f;
            currentFrame++;
            if (currentFrame > maxFrames) {
                alive = false;
            }
        }
    }

    public void render(Canvas canvas, AssetLoader loader, Paint paint) {
        if (!alive) return;

        String spriteName;
        switch (type) {
            case MISSILE:
                spriteName = "explosionMisil" + currentFrame + ".png";
                break;
            case BOSS:
                spriteName = "explosionB" + currentFrame + ".png";
                break;
            case MISSILE_SMOKE:
            case SMOKE:
                spriteName = "humoMisil" + currentFrame + ".png";
                break;
            case NORMAL:
            default:
                spriteName = "explosion" + currentFrame + ".png";
                break;
        }

        Bitmap bmp = loader.getBitmap(spriteName);
        if (bmp != null) {
            float drawX = x - bmp.getWidth() / 2f;
            float drawY = y - bmp.getHeight() / 2f;
            canvas.drawBitmap(bmp, drawX, drawY, paint);
        }
    }
}
