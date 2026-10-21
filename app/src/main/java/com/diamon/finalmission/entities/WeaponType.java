package com.diamon.finalmission.entities;

/**
 * Tipos de arma auténticos de Final Mission NES.
 */
public enum WeaponType {
    NORMAL("Normal", 1),
    WIDE("Wide", 2),
    LASER("Laser", 3);

    private final String displayName;
    private final int level;

    WeaponType(String displayName, int level) {
        this.displayName = displayName;
        this.level = level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getLevel() {
        return level;
    }
}
