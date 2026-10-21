package com.diamon.finalmission.world;

/**
 * Authentic Stage configuration and metadata for all 5 stages of NES Final Mission (Natsume 1990):
 * - STAGE 1: Destroyed City / Astro Pipe Outer Perimeter (Boss: Final Machine)
 * - STAGE 2: Aerial Battleship Fleet (Boss: Heavy Cruiser Core)
 * - STAGE 3: Subterranean Magma Caverns (Boss: Magma Drill Core)
 * - STAGE 4: Orbital Space Elevator (Boss: Orbital Mech Guardian)
 * - STAGE 5: Alien Mothership / Mother Core (Boss: Emperor Overmind Core)
 */
public class StageConfig {

    public static final int TOTAL_STAGES = 5;

    public final int stageNumber;
    public final String title;
    public final String subtitle;
    public final String bossName;
    public final float bossHp;
    public final float enemyHpMultiplier;
    public final float fireRateMultiplier;
    public final float scrollSpeed;

    public StageConfig(int stageNumber, String title, String subtitle, String bossName,
                       float bossHp, float enemyHpMultiplier, float fireRateMultiplier, float scrollSpeed) {
        this.stageNumber = stageNumber;
        this.title = title;
        this.subtitle = subtitle;
        this.bossName = bossName;
        this.bossHp = bossHp;
        this.enemyHpMultiplier = enemyHpMultiplier;
        this.fireRateMultiplier = fireRateMultiplier;
        this.scrollSpeed = scrollSpeed;
    }

    /**
     * Returns authentic configuration for the requested stage number (1 to 5).
     * Automatically clamps stage numbers below 1 to 1, and above 5 to 5.
     *
     * @param stageNumber Stage index (1 to 5).
     * @return StageConfig instance configured with authentic parameters.
     */
    public static StageConfig getForStage(int stageNumber) {
        if (stageNumber < 1) {
            stageNumber = 1;
        } else if (stageNumber > TOTAL_STAGES) {
            stageNumber = TOTAL_STAGES;
        }

        switch (stageNumber) {
            case 1:
                return new StageConfig(1, "STAGE 1", "DESTROYED CITY",
                        "FINAL MACHINE", 60f, 1.0f, 1.0f, 30f);
            case 2:
                return new StageConfig(2, "STAGE 2", "AERIAL BATTLESHIP FLEET",
                        "HEAVY CRUISER CORE", 80f, 1.15f, 1.15f, 32f);
            case 3:
                return new StageConfig(3, "STAGE 3", "SUBTERRANEAN MAGMA CAVERNS",
                        "MAGMA DRILL CORE", 100f, 1.30f, 1.25f, 32f);
            case 4:
                return new StageConfig(4, "STAGE 4", "ORBITAL SPACE ELEVATOR",
                        "ORBITAL MECH GUARDIAN", 125f, 1.45f, 1.35f, 34f);
            case 5:
            default:
                return new StageConfig(5, "STAGE 5", "ALIEN MOTHERSHIP",
                        "EMPEROR OVERMIND CORE", 160f, 1.65f, 1.50f, 36f);
        }
    }
}
