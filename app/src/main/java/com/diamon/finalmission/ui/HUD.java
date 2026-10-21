package com.diamon.finalmission.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import com.diamon.finalmission.engine.AssetLoader;
import com.diamon.finalmission.entities.Player;
import com.diamon.finalmission.entities.enemies.MaquinaFinal;

/**
 * Interfaz de usuario (HUD) retro de Final Mission con adaptación a pantalla horizontal widescreen.
 * Renderiza puntuación, salud (con pips auténticos vida1/vida2), vidas,
 * arma actual, etapa activa y barra de salud del jefe final.
 */
public class HUD {

    private final Paint textPaint = new Paint();
    private final Paint shadowPaint = new Paint();
    private final Paint barPaint = new Paint();
    private final Paint barBgPaint = new Paint();

    public HUD() {
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(8f);
        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        textPaint.setAntiAlias(false);

        shadowPaint.setColor(Color.BLACK);
        shadowPaint.setTextSize(8f);
        shadowPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        shadowPaint.setAntiAlias(false);

        barPaint.setColor(Color.RED);
        barBgPaint.setColor(Color.DKGRAY);
    }

    public void render(Canvas canvas, AssetLoader loader, Player player, int score, int highScore,
                       MaquinaFinal boss, float stageTime, int stageNumber) {

        float vw = canvas.getWidth();

        // 1. Puntuación 1P/2P y High Score adaptados a widescreen
        String playerPrefix = (player.characterId == 2) ? "2P" : "1P";
        String scoreStr = String.format("%s %06d", playerPrefix, score);
        String hiStr = String.format("HI %06d", highScore);

        drawShadowText(canvas, scoreStr, 8f, 10f);
        drawShadowText(canvas, hiStr, vw - 84f, 10f);

        // Indicador de Fase
        String stageStr = "STAGE " + stageNumber;
        float stageTextW = textPaint.measureText(stageStr);
        drawShadowText(canvas, stageStr, (vw - stageTextW) / 2f, 10f);

        // 2. Indicador de Vidas y Nombre del Personaje
        String livesStr = player.getCharacterName() + " P=" + Math.max(0, player.lives);
        drawShadowText(canvas, livesStr, 8f, 20f);

        // 3. Indicador de Arma
        String weaponStr = "WPN:" + player.weaponType.getDisplayName().toUpperCase();
        drawShadowText(canvas, weaponStr, 76f, 20f);

        // 4. Barra de Salud del Jugador (usando pips auténticos vida1.png y vida2.png)
        Bitmap fullLife = (player.characterId == 2) ? loader.getBitmap("vida2.png") : loader.getBitmap("vida1.png");
        Bitmap emptyLife = (player.characterId == 2) ? loader.getBitmap("vida1.png") : loader.getBitmap("vida2.png");

        float lifeX = 142f;
        float lifeY = 12f;
        drawShadowText(canvas, "LIFE:", lifeX, 20f);
        lifeX += 28f;

        for (int i = 0; i < Player.MAX_HEALTH; i++) {
            Bitmap pip = (i < player.health) ? fullLife : emptyLife;
            if (pip != null) {
                canvas.drawBitmap(pip, lifeX + (i * 7f), lifeY, null);
            }
        }

        // 5. Barra de Salud del Jefe (cuando está activo en la arena)
        if (boss != null && (boss.alive || boss.isDefeated)) {
            float bossBarW = Math.min(180f, vw * 0.5f);
            float bossBarH = 6f;
            float bossBarX = (vw - bossBarW) / 2f;
            float bossBarY = 214f;

            String bossTitle = "BOSS - FINAL MACHINE";
            float bossTitleW = textPaint.measureText(bossTitle);
            drawShadowText(canvas, bossTitle, (vw - bossTitleW) / 2f, bossBarY - 2f);

            // Fondo barra
            canvas.drawRect(bossBarX, bossBarY, bossBarX + bossBarW, bossBarY + bossBarH, barBgPaint);

            // Relleno proporcional
            float hpRatio = Math.max(0f, boss.hp / boss.maxHp);
            barPaint.setColor(hpRatio > 0.35f ? Color.YELLOW : Color.RED);
            canvas.drawRect(bossBarX, bossBarY, bossBarX + (bossBarW * hpRatio), bossBarY + bossBarH, barPaint);
        }

        // 6. Guía rápida de controles en los primeros segundos
        if (stageTime < 5.0f) {
            float alpha = Math.min(1.0f, (5.0f - stageTime));
            textPaint.setAlpha((int) (alpha * 255));
            shadowPaint.setAlpha((int) (alpha * 255));

            String tip1 = "TOUCH: MOVE & FIRE | 2ND FINGER: 360 AIM";
            String tip2 = "QUICK FLICK: FLIP LEFT/RIGHT";
            drawShadowText(canvas, tip1, (vw - textPaint.measureText(tip1)) / 2f, 32f);
            drawShadowText(canvas, tip2, (vw - textPaint.measureText(tip2)) / 2f, 42f);

            textPaint.setAlpha(255);
            shadowPaint.setAlpha(255);
        }
    }

    private void drawShadowText(Canvas canvas, String text, float x, float y) {
        canvas.drawText(text, x + 1f, y + 1f, shadowPaint);
        canvas.drawText(text, x, y, textPaint);
    }
}
