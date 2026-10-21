package com.diamon.finalmission.entities.enemies;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.diamon.finalmission.engine.AssetLoader;
import com.diamon.finalmission.engine.AudioManager;
import com.diamon.finalmission.entities.Bullet;
import com.diamon.finalmission.entities.Enemy;
import com.diamon.finalmission.entities.Explosion;
import com.diamon.finalmission.entities.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Jefe Final de la Fase 1: Máquina Gran Final (MaquinaFinal).
 * Fortaleza biomecánica anclada en la pantalla 21 (fondo21MGF.png):
 * - Blindaje acorazado y torretas destruibles (maquinas.png, maquinaParedD/I).
 * - Orbes de plasma defensivos rotatorios (bola1..16.png) que protegen el núcleo y desvían disparos.
 * - 3 Fases de combate dinámicas:
 *   * Fase 1 (HP > 50): Ráfagas láser dirigidas de 3 vías (3-way aimed laser volleys).
 *   * Fase 2 (20 < HP <= 50): Andanadas de esferas de energía destruibles (destructible energy spheres).
 *   * Fase 3 (HP <= 20): Ráfagas rápidas de desesperación y sobrecarga en arco (desperate bullet barrages).
 * - Secuencia de explosiones en cadena multifase (explosionB1..5.png) al ser derrotado.
 */
public class MaquinaFinal extends Enemy {

    public static final float BOSS_MAX_HP = 80f;

    // Torretas de pared destructibles integradas
    public final MaquinaPared turretTop;
    public final MaquinaPared turretBottom;

    // Orbes defensivos de plasma giratorios (bola1..16.png)
    public static class DefensiveOrb {
        public float angle;
        public float radius = 34f;
        public float x;
        public float y;
        public float animTimer = 0f;

        public DefensiveOrb(float startAngle) {
            this.angle = startAngle;
        }

        public void update(float dt, float centerX, float centerY, float rotSpeed) {
            angle += rotSpeed * dt;
            animTimer += dt;
            x = centerX + (float) Math.cos(angle) * radius;
            y = centerY + (float) Math.sin(angle) * radius;
        }

        public RectF getBounds() {
            return new RectF(x - 6f, y - 6f, x + 6f, y + 6f);
        }

        public void render(Canvas canvas, AssetLoader loader, Paint paint) {
            int frame = ((int) (animTimer * 16f) % 16) + 1;
            Bitmap orbBmp = loader.getBitmap("bola" + frame + ".png");
            if (orbBmp != null) {
                float drawX = x - orbBmp.getWidth() / 2f;
                float drawY = y - orbBmp.getHeight() / 2f;
                canvas.drawBitmap(orbBmp, drawX, drawY, paint);
            }
        }
    }

    public final List<DefensiveOrb> orbs = new ArrayList<>();

    // Variables de ataque y oscilación
    private float hoverTimer = 0f;
    private float attackTimer = 1.2f;
    private float coreAnimTimer = 0f;
    public final float startY;

    // Estado de derrota y cadena de explosiones
    public boolean isDefeated = false;
    public float defeatTimer = 0f;
    private float explosionInterval = 0.15f;
    private float nextExplosionTimer = 0f;

    public MaquinaFinal(float x, float y) {
        super(x, y, 36f, 44f, BOSS_MAX_HP, 20000);
        this.startY = y;

        // Instanciar torretas de pared superior e inferior
        this.turretTop = new MaquinaPared(x + 8f, 26f, true);
        this.turretBottom = new MaquinaPared(x + 8f, 172f, true);

        // Inicializar 3 orbes defensivos distribuidos a 120 grados
        for (int i = 0; i < 3; i++) {
            float orbAngle = (float) (i * 2.0 * Math.PI / 3.0);
            orbs.add(new DefensiveOrb(orbAngle));
        }
    }

    public int getPhase() {
        if (hp > 50f) return 1;
        if (hp > 20f) return 2;
        return 3;
    }

    @Override
    public void update(float dt, Player player, List<Bullet> enemyBullets, float scrollSpeed) {
        if (hitFlashTimer > 0f) {
            hitFlashTimer -= dt;
        }

        coreAnimTimer += dt;

        // Actualizar torretas de pared
        if (turretTop.alive) {
            turretTop.update(dt, player, enemyBullets, scrollSpeed);
        }
        if (turretBottom.alive) {
            turretBottom.update(dt, player, enemyBullets, scrollSpeed);
        }

        // Si el jefe está derrotado, gestionar la cadena de explosiones
        if (isDefeated) {
            defeatTimer += dt;
            nextExplosionTimer -= dt;
            return;
        }

        // 1. Oscilación y flotación vertical biomecánica
        hoverTimer += dt * 2.0f;
        float hoverRange = (getPhase() == 3) ? 30f : 18f;
        y = startY + (float) Math.sin(hoverTimer) * hoverRange;

        // 2. Actualizar orbes defensivos de plasma
        float coreCenterX = x + 16f;
        float coreCenterY = y + height / 2f;
        float orbSpeed = (getPhase() == 3) ? 3.6f : 2.2f;

        for (DefensiveOrb orb : orbs) {
            orb.update(dt, coreCenterX, coreCenterY, orbSpeed);
        }

        // 3. Fases de ataque
        attackTimer -= dt;
        if (attackTimer <= 0f) {
            executeAttackPhase(player, enemyBullets, coreCenterX, coreCenterY);
        }
    }

    private void executeAttackPhase(Player player, List<Bullet> enemyBullets, float coreX, float coreY) {
        if (!player.alive) return;

        int phase = getPhase();

        switch (phase) {
            case 1: {
                // FASE 1: Ráfagas láser dirigidas de 3 vías (3-way aimed laser volleys)
                attackTimer = 1.5f;
                float angleToPlayer = (float) Math.atan2(player.y - coreY, player.x - coreX);

                enemyBullets.add(Bullet.createBossLaser(coreX - 8f, coreY, angleToPlayer, 160f));
                enemyBullets.add(Bullet.createBossLaser(coreX - 8f, coreY, angleToPlayer - 0.28f, 155f));
                enemyBullets.add(Bullet.createBossLaser(coreX - 8f, coreY, angleToPlayer + 0.28f, 155f));
                break;
            }
            case 2: {
                // FASE 2: Andanadas de esferas de energía destruibles (destructible energy spheres)
                attackTimer = 1.7f;
                // Disparo de esferas ondulantes desde troneras superior e inferior
                enemyBullets.add(Bullet.createBossEnergySphere(coreX - 10f, coreY - 20f, -120f, -25f));
                enemyBullets.add(Bullet.createBossEnergySphere(coreX - 10f, coreY + 20f, -120f, 25f));

                // Disparo central de apoyo
                float angleToPlayer = (float) Math.atan2(player.y - coreY, player.x - coreX);
                enemyBullets.add(Bullet.createBossLaser(coreX - 8f, coreY, angleToPlayer, 150f));
                break;
            }
            case 3: {
                // FASE 3 (SOBRECARGA): Ráfagas rápidas de desesperación en abanico denso
                attackTimer = 0.85f;
                float baseAngle = (float) Math.atan2(player.y - coreY, player.x - coreX);

                for (int i = -2; i <= 2; i++) {
                    float a = baseAngle + (i * 0.22f);
                    enemyBullets.add(Bullet.createBossBarrage(coreX - 6f, coreY, a, 150f));
                }
                break;
            }
        }
    }

    /**
     * Comprueba si un proyectil del jugador es interceptado y desviado por los orbes defensivos.
     * Retorna true si el orbe absorbió o desvió el disparo, protegiendo al jefe.
     */
    public boolean checkOrbDeflection(Bullet bullet, AudioManager audio) {
        if (isDefeated || orbs.isEmpty()) return false;

        RectF bulletRect = bullet.getBounds();
        for (DefensiveOrb orb : orbs) {
            if (RectF.intersects(bulletRect, orb.getBounds())) {
                bullet.deflect();
                audio.playSound(AudioManager.SFX_DISPARO_L);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean takeDamage(float amount) {
        if (isDefeated) return false;
        hp -= amount;
        hitFlashTimer = 0.08f;
        if (hp <= 0f) {
            hp = 0f;
            isDefeated = true;
            alive = true; // Se mantiene viva durante la cadena de explosiones
            defeatTimer = 0f;
            nextExplosionTimer = 0f;
            orbs.clear(); // Los orbes se disipan
            return true;
        }
        return false;
    }

    public Explosion checkSpawnChainExplosion(AudioManager audio) {
        if (!isDefeated || defeatTimer >= 3.4f) return null;

        if (nextExplosionTimer <= 0f) {
            nextExplosionTimer = explosionInterval;
            audio.playSound(AudioManager.SFX_EXPLOSION);

            // Coordenadas escalonadas aleatorias sobre la estructura de la fortaleza
            float offsetX = (float) ((Math.random() - 0.5) * 44f);
            float offsetY = (float) ((Math.random() - 0.5) * 80f);

            return new Explosion(x + 16f + offsetX, y + height / 2f + offsetY, Explosion.Type.BOSS);
        }
        return null;
    }

    @Override
    public RectF getBounds() {
        // Hitbox del núcleo vulnerable
        return new RectF(x + 4f, y + 4f, x + width, y + height - 4f);
    }

    @Override
    public void render(Canvas canvas, AssetLoader loader, Paint paint) {
        Paint activePaint = getActivePaint(paint);

        // 1. Blindaje acorazado y estructura de la fortaleza (maquinas.png)
        Bitmap plating = loader.getBitmap("maquinas.png");
        if (plating != null) {
            // Se posiciona como mamparo acorazado del fondo
            float plateX = x - 24f;
            float plateY = y - 40f;
            canvas.drawBitmap(plating, plateX, plateY, activePaint);
        }

        // 2. Torretas de pared superior e inferior
        if (turretTop.alive) {
            turretTop.render(canvas, loader, paint);
        }
        if (turretBottom.alive) {
            turretBottom.render(canvas, loader, paint);
        }

        // 3. Núcleo central biomecánico (maquinaFinal.png)
        Bitmap core = loader.getBitmap("maquinaFinal.png");
        if (core != null) {
            float drawX = x + (width - core.getWidth()) / 2f;
            float drawY = y + (height - core.getHeight()) / 2f;
            canvas.drawBitmap(core, drawX, drawY, activePaint);
        }

        // 4. Orbes de plasma defensivos rotatorios (bola1..16.png)
        if (!isDefeated) {
            for (DefensiveOrb orb : orbs) {
                orb.render(canvas, loader, paint);
            }
        }
    }
}
