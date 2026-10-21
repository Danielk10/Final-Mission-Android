package com.diamon.finalmission.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;

import com.diamon.finalmission.engine.AssetLoader;
import com.diamon.finalmission.engine.TouchController;
import com.diamon.finalmission.entities.Player;
import com.diamon.finalmission.entities.WeaponType;
import com.diamon.finalmission.entities.enemies.FinalMachineBoss;

/**
 * Authentic Japanese Final Mission NES Head-Up Display (HUD) for Widescreen Landscape:
 * - 3 Health blocks (strictly authentic Japanese version: max 3 HP, unlike US 6 HP).
 * - Character name: Sparky (1P) or Sergei Missionov (2P).
 * - Lives / Stock indicator: P=02.
 * - Current Stage & Hi-Score counters.
 * - Power-Up Badges: 'W' (Wide), 'L' (Laser), 'S' (Speed), 'B' (Recovery).
 * - Interactive Satellite Lock Button with crisp NES pixel border.
 * - Boss health bar for FinalMachineBoss with dynamic phase colors.
 */
public class HUD {

    private final Paint textPaint = new Paint();
    private final Paint shadowPaint = new Paint();
    private final Paint badgeTextPaint = new Paint();
    private final Paint barPaint = new Paint();
    private final Paint barBgPaint = new Paint();
    private final Paint boxPaint = new Paint();
    private final Paint boxStrokePaint = new Paint();

    private final Rect pipSrc = new Rect();
    private final RectF pipDst = new RectF();

    public HUD() {
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(8f);
        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        textPaint.setAntiAlias(false);

        shadowPaint.setColor(Color.BLACK);
        shadowPaint.setTextSize(8f);
        shadowPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        shadowPaint.setAntiAlias(false);

        badgeTextPaint.setColor(Color.WHITE);
        badgeTextPaint.setTextSize(7f);
        badgeTextPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        badgeTextPaint.setAntiAlias(false);

        barPaint.setColor(Color.RED);
        barBgPaint.setColor(0xCC181818);

        boxPaint.setStyle(Paint.Style.FILL);
        boxStrokePaint.setStyle(Paint.Style.STROKE);
        boxStrokePaint.setStrokeWidth(1f);
    }

    public void render(Canvas canvas, AssetLoader loader, Player player, int score, int highScore,
                       FinalMachineBoss boss, float stageTime, int stageNumber, TouchController touchController) {

        float vw = canvas.getWidth();

        // 1. Japanese Final Mission: 3 Health Blocks (vida1.png = full, vida2.png = empty)
        Bitmap lifeFull = loader.getBitmap("vida1.png");
        Bitmap lifeEmpty = loader.getBitmap("vida2.png");

        float pipW = 8f;
        float pipH = 16f;
        float pipStartX = 12f;
        float pipStartY = 4f;

        for (int i = 0; i < Player.MAX_HEALTH; i++) {
            Bitmap pip = (i < player.health) ? lifeFull : lifeEmpty;
            if (pip != null) {
                pipSrc.set(0, 0, pip.getWidth(), pip.getHeight());
                float px = pipStartX + (i * (pipW + 3f));
                pipDst.set(px, pipStartY, px + pipW, pipStartY + pipH);
                canvas.drawBitmap(pip, pipSrc, pipDst, null);
            }
        }

        // 2. Character Name: SPARKY (1P) or SERGEI (2P)
        String charName = player.getShortCharacterName();
        drawShadowText(canvas, charName, pipStartX + (Player.MAX_HEALTH * (pipW + 3f)) + 4f, 10f);

        // 3. Lives Stock Indicator: P=02
        String livesStr = "P=" + String.format("%02d", Math.max(0, player.lives));
        drawShadowText(canvas, livesStr, pipStartX + (Player.MAX_HEALTH * (pipW + 3f)) + 4f, 19f);

        // 4. Power-Up Status Indicators: 'W', 'L', 'S', 'B'
        float badgeStartX = pipStartX + (Player.MAX_HEALTH * (pipW + 3f)) + 42f;
        float badgeY = 5f;
        float badgeSize = 13f;
        float badgeGap = 3f;

        // Wide badge 'W'
        boolean wideActive = (player.weaponType == WeaponType.WIDE);
        drawPowerUpBadge(canvas, "W", badgeStartX, badgeY, badgeSize,
                wideActive ? 0xFF00E8D8 : 0x66333333,
                wideActive ? Color.WHITE : 0xFF777777);

        // Laser badge 'L'
        boolean laserActive = (player.weaponType == WeaponType.LASER);
        drawPowerUpBadge(canvas, "L", badgeStartX + (badgeSize + badgeGap), badgeY, badgeSize,
                laserActive ? 0xFFFCD800 : 0x66333333,
                laserActive ? Color.WHITE : 0xFF777777);

        // Speed badge 'S'
        boolean speedActive = (player.speedMultiplier > 1.05f);
        drawPowerUpBadge(canvas, "S", badgeStartX + (badgeSize + badgeGap) * 2f, badgeY, badgeSize,
                speedActive ? 0xFF00F800 : 0x66333333,
                speedActive ? Color.WHITE : 0xFF777777);

        // Life recovery badge 'B'
        boolean lifeMaxed = (player.health == Player.MAX_HEALTH);
        drawPowerUpBadge(canvas, "B", badgeStartX + (badgeSize + badgeGap) * 3f, badgeY, badgeSize,
                lifeMaxed ? 0xFFE04040 : 0x66333333,
                lifeMaxed ? Color.WHITE : 0xFF777777);

        // 5. STAGE indicator at top center
        String stageStr = "STAGE " + stageNumber;
        float stageW = textPaint.measureText(stageStr);
        drawShadowText(canvas, stageStr, (vw - stageW) / 2f, 10f);

        // 6. 1P Score and HI Score at top right
        String playerPrefix = (player.characterId == 2) ? "2P" : "1P";
        String scoreStr = String.format("%s %06d", playerPrefix, score);
        drawShadowText(canvas, scoreStr, vw - 146f, 10f);

        String hiStr = String.format("HI %06d", highScore);
        drawShadowText(canvas, hiStr, vw - 76f, 10f);

        // 7. Interactive On-Screen Satellite Lock Button (Bottom-Right)
        if (touchController != null) {
            touchController.updateLockButtonBounds(vw, canvas.getHeight());
            RectF btnRect = touchController.lockButtonBounds;

            boolean isLocked = touchController.satelliteLock;
            int btnBg = isLocked ? 0xDD003858 : 0xAA202020;
            int borderCol = isLocked ? 0xFF00E8D8 : 0xFFFCD800;
            int textCol = isLocked ? 0xFFE0F8FF : 0xFFFFFFFF;

            boxPaint.setColor(btnBg);
            canvas.drawRect(btnRect, boxPaint);

            boxStrokePaint.setColor(borderCol);
            canvas.drawRect(btnRect, boxStrokePaint);

            String satLabel = isLocked ? "SAT:LOCKED" : "SAT:ORBIT";
            float satW = badgeTextPaint.measureText(satLabel);
            float satX = btnRect.left + (btnRect.width() - satW) / 2f;
            float satY = btnRect.centerY() + 3f;

            shadowPaint.setTextSize(7f);
            canvas.drawText(satLabel, satX + 1f, satY + 1f, shadowPaint);
            badgeTextPaint.setColor(textCol);
            canvas.drawText(satLabel, satX, satY, badgeTextPaint);
        }

        // 8. Boss Health Bar (FinalMachineBoss)
        if (boss != null && (boss.alive || boss.isDefeated)) {
            float barW = Math.min(180f, vw * 0.45f);
            float barH = 6f;
            float barX = (vw - barW) / 2f;
            float barY = 214f;

            String name = (boss.bossName != null && !boss.bossName.isEmpty()) ? boss.bossName : "FINAL MACHINE";
            String bossTitle = "BOSS: " + name;
            float titleW = textPaint.measureText(bossTitle);
            drawShadowText(canvas, bossTitle, (vw - titleW) / 2f, barY - 3f);

            // Background frame
            canvas.drawRect(barX - 1f, barY - 1f, barX + barW + 1f, barY + barH + 1f, barBgPaint);

            // Proportional filled bar with phase-based color
            float hpRatio = Math.max(0f, boss.hp / boss.maxHp);
            if (hpRatio > 0.5f) {
                barPaint.setColor(0xFF00E8D8); // Cyan Phase 1
            } else if (hpRatio > 0.25f) {
                barPaint.setColor(0xFFFCD800); // Yellow Phase 2
            } else {
                barPaint.setColor(0xFFF83800); // Red Phase 3
            }
            canvas.drawRect(barX, barY, barX + (barW * hpRatio), barY + barH, barPaint);
        }
    }

    // Overload for backward compatibility with FinalMachineBoss
    public void render(Canvas canvas, AssetLoader loader, Player player, int score, int highScore,
                       FinalMachineBoss boss, float stageTime, int stageNumber) {
        render(canvas, loader, player, score, highScore, boss, stageTime, stageNumber, null);
    }

    private void drawPowerUpBadge(Canvas canvas, String letter, float x, float y, float size,
                                  int bgColor, int textColor) {
        RectF r = new RectF(x, y, x + size, y + size);

        boxPaint.setColor(bgColor);
        canvas.drawRect(r, boxPaint);

        boxStrokePaint.setColor(0xFF555555);
        canvas.drawRect(r, boxStrokePaint);

        badgeTextPaint.setColor(textColor);
        float tw = badgeTextPaint.measureText(letter);
        float tx = x + (size - tw) / 2f;
        float ty = y + size - 3f;

        shadowPaint.setTextSize(7f);
        canvas.drawText(letter, tx + 1f, ty + 1f, shadowPaint);
        canvas.drawText(letter, tx, ty, badgeTextPaint);
    }

    private void drawShadowText(Canvas canvas, String text, float x, float y) {
        canvas.drawText(text, x + 1f, y + 1f, shadowPaint);
        canvas.drawText(text, x, y, textPaint);
    }
}
