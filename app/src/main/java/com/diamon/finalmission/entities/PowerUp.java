package com.diamon.finalmission.entities;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.diamon.finalmission.engine.AssetLoader;
import com.diamon.finalmission.engine.AudioManager;

/**
 * Sistema de Potenciadores y Cajas de Final Mission NES:
 * - CAJA (Canister): Flota suavemente por la pantalla con animación cajaPoder1..4.png.
 *   Al ser destruida por disparos, se rompe y libera una cápsula de poder auténtica.
 * - PODER (Cápsula):
 *   * 'W' (poderW.png): Wide Spread Shot (disparo amplio en abanico)
 *   * 'L' (poderL.png): Laser Beam (rayo láser penetrante)
 *   * 'S' (poderS.png): Speed Up (acelerador de velocidad)
 *   * 'B' (poderB.png): Life Energy Recovery (recuperación de salud - estrictamente SIN BOMBAS)
 *
 * Física de oscilación y desplazamiento auténtica portada de com.diamon.actor.Poder.
 */
public class PowerUp {

    public enum Type {
        CAJA,          // Contenedor flotante (cajaPoder1..4.png)
        POD_CONTAINER, // Alias para compatibilidad
        WIDE,          // 'W'
        LASER,         // 'L'
        SPEED,         // 'S'
        LIFE           // 'B' (Recuperación de vida - sin bombas)
    }

    public float x;
    public float y;
    public float baseY;
    public float vx = -20f;
    public float vy = 0f;
    public Type type;
    public Type dropType = Type.WIDE;
    public boolean alive = true;

    // Resistencia del contenedor Caja
    public int containerHp = 2;
    public float hitFlashTimer = 0f;

    // Animación y oscilación
    private float animTimer = 0f;
    private int animFrame = 0;
    private float floatTimer = 0f;

    // Ciclo de desplazamiento auténtico de com.diamon.actor.Poder (150 ciclos)
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
            // Velocidad base auténtica (~16 px/s a resolución retro)
            this.vx = -16f;
        }
    }

    public boolean isContainer() {
        return type == Type.CAJA || type == Type.POD_CONTAINER;
    }

    public static Type chooseRandomDrop() {
        double r = Math.random();
        if (r < 0.35) return Type.WIDE;
        if (r < 0.65) return Type.LASER;
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
            // 1. Contenedor Caja: animación de 4 cuadros (cajaPoder1..4.png)
            if (animTimer >= 0.12f) {
                animTimer = 0f;
                animFrame = (animFrame + 1) % 4;
            }

            // Movimiento suave y flotación oscilatoria vertical
            x += vx * dt;
            y = baseY + (float) Math.sin(floatTimer * 2.4f) * 6f;
        } else {
            // 2. Cápsula Poder: física auténtica de com.diamon.actor.Poder
            // Cada ~2.5 segundos (150 ciclos a 60 FPS), activa un desplazamiento impulsivo
            cycleTimer += dt;
            if (cycleTimer >= 2.5f) {
                cycleTimer = 0f;
                impulseActive = true;
                impulseDuration = 0.8f;
            }

            float currentVx = vx;
            if (impulseActive) {
                impulseDuration -= dt;
                currentVx -= 28f; // Impulso extra de velocidad
                if (impulseDuration <= 0f) {
                    impulseActive = false;
                }
            }

            x += currentVx * dt;
            // Suave oscilación sinusoidal en el aire
            y = baseY + (float) Math.sin(floatTimer * 3.2f) * 5f;
        }

        // Límite fuera de pantalla izquierda
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
                // 'B': Recuperación de energía vital (estrictamente SIN BOMBAS)
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
                case SPEED:
                    bmp = loader.getBitmap("poderS.png");
                    break;
                case LIFE:
                    // Ítem auténtico 'B' (poderB.png)
                    bmp = loader.getBitmap("poderB.png");
                    break;
                default:
                    break;
            }
        }

        if (bmp != null) {
            float drawX = x - bmp.getWidth() / 2f;
            float drawY = y - bmp.getHeight() / 2f;
            canvas.drawBitmap(bmp, drawX, drawY, paint);
        }
    }
}
