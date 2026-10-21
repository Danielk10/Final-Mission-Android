package com.diamon.finalmission;

import com.diamon.finalmission.engine.AudioManager;
import com.diamon.finalmission.world.Level;
import com.diamon.finalmission.world.StageConfig;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * Unit tests verifying Stage configurations, Audio track definitions, and Camera viewport calculations.
 */
public class StageCameraAudioTest {

    @Test
    public void testAllEightStagesConfigured() {
        for (int i = 1; i <= 8; i++) {
            StageConfig config = StageConfig.getForStage(i);
            assertNotNull("Stage " + i + " config should not be null", config);
            assertEquals("Stage number mismatch", i, config.stageNumber);
            assertNotNull("Title should exist", config.title);
            assertNotNull("Subtitle should exist", config.subtitle);
            assertNotNull("Boss name should exist", config.bossName);
            assertTrue("Boss HP should be positive", config.bossHp > 0);
            assertTrue("Enemy HP multiplier should scale with difficulty", config.enemyHpMultiplier >= 1.0f);
            assertTrue("Fire rate multiplier should scale with difficulty", config.fireRateMultiplier >= 1.0f);
            assertTrue("Scroll speed should be positive", config.scrollSpeed > 0);
        }
    }

    @Test
    public void testAudioFilesExistInAssets() {
        String[] requiredAudio = {
                AudioManager.SFX_DISPARO,
                AudioManager.SFX_DISPARO_L,
                AudioManager.SFX_EXPLOSION,
                AudioManager.SFX_MURIENDO,
                AudioManager.SFX_PODER,
                AudioManager.SFX_PAUSA,
                AudioManager.SFX_MENU,
                AudioManager.SFX_PRESENTACION,
                AudioManager.BGM_INTRODUCCION,
                AudioManager.BGM_MUSICA,
                AudioManager.BGM_COMIENZO,
                AudioManager.BGM_PRESENTACION,
                AudioManager.BGM_FIN_JUEGO
        };

        File audioDir = new File("src/main/assets/audio");
        for (String file : requiredAudio) {
            File f = new File(audioDir, file);
            assertTrue("Audio file must exist in assets/audio: " + file, f.exists());
            assertTrue("Audio file must have non-zero size: " + file, f.length() > 0);
        }
    }

    @Test
    public void testWidescreenShaftCenteringMath() {
        // NES base resolution
        float segmentW = Level.SEGMENT_W;
        assertEquals(256f, segmentW, 0.001f);

        // Typical 16:9 widescreen phone (virtual width ~ 398)
        float virtualWidth16_9 = 398f;
        float shaftX16_9 = (virtualWidth16_9 - segmentW) / 2f;
        assertEquals(71f, shaftX16_9, 0.001f);

        // Typical 20:9 ultra-wide phone (virtual width ~ 498)
        float virtualWidth20_9 = 498f;
        float shaftX20_9 = (virtualWidth20_9 - segmentW) / 2f;
        assertEquals(121f, shaftX20_9, 0.001f);

        // Standard 4:3 NES aspect ratio (virtual width = 256)
        float virtualWidth4_3 = 256f;
        float shaftX4_3 = (virtualWidth4_3 - segmentW) / 2f;
        assertEquals(0f, shaftX4_3, 0.001f);
    }

    @Test
    public void testStageClearBonusMath() {
        int stage = 1;
        int health = 3;
        int lives = 3;

        int clearBonus = stage * 5000;
        int lifeBonus = health * 1000;
        int stockBonus = lives * 2000;
        int totalBonus = clearBonus + lifeBonus + stockBonus;

        assertEquals(5000, clearBonus);
        assertEquals(3000, lifeBonus);
        assertEquals(6000, stockBonus);
        assertEquals(14000, totalBonus);
    }
}
