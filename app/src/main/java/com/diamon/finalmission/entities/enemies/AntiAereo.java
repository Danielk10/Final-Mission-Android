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
 * Enemigo AntiAéreo de Final Mission NES:
 * Torretas estacionarias ancladas a suelo, techo o paredes que rastrean al jugador.
 */
public class AntiAereo extends Enemy {

    public enum Mount {
        GROUND,  // Suelo (H)
        CEILING, // Techo (V)
        DIAGONAL // Pared / Esquina (D)
    }

    private final Mount mount;
    private float shootTimer = 1.0f;
    private float animTimer = 0f;
    private int animFrame = 0;

    public AntiAereo(float x, float y, Mount mount) {
        super(x, y, 15f, 16f, 3f, 300);
        this.mount = mount;
    }

    @Override
    public void update(float dt, Player player, List<Bullet> enemyBullets, float scrollSpeed) {
        if (!alive) return;

        // Se desplaza con el scroll del escenario
        x -= scrollSpeed * dt;

        if (hitFlashTimer > 0f) {
            hitFlashTimer -= dt;
        }

        animTimer += dt;
        if (animTimer >= 0.2f) {
            animTimer = 0f;
            animFrame = (animFrame + 1) % 2;
        }

        // Disparo periódico hacia la posición del jugador
        shootTimer -= dt;
        if (shootTimer <= 0f) {
            shootTimer = 1.6f + (float) (Math.random() * 0.6f);
            if (x > 0 && x < 250 && player.alive) {
                float barrelX = x + width / 2f;
                float barrelY = (mount == Mount.GROUND) ? y : (y + height);
                enemyBullets.add(Bullet.createEnemyNormal(barrelX, barrelY, player.x + player.width / 2f, player.y + player.height / 2f, 105f));
            }
        }

        if (x < -30) {
            alive = false;
        }
    }

    @Override
    public void render(Canvas canvas, AssetLoader loader, Paint paint) {
        if (!alive) return;

        int frame = animFrame + 1;
        String spriteName;
        switch (mount) {
            case CEILING:
                spriteName = "antiAreoV" + frame + ".png";
                break;
            case DIAGONAL:
                spriteName = "antiAreoD" + frame + ".png";
                break;
            case GROUND:
            default:
                spriteName = "antiAreoH" + frame + ".png";
                break;
        }

        Bitmap bmp = loader.getBitmap(spriteName);
        if (bmp != null) {
            canvas.drawBitmap(bmp, x, y, getActivePaint(paint));
        }
    }
}
