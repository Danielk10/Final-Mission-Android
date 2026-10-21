package com.diamon.finalmission.entities;

/**
 * Floating Item Box (Canister) for Final Mission NES:
 * - Floats across the screen with gentle sinusoidal oscillation.
 * - Uses animated 4-frame rotation sequence (cajaPoder1..4.png).
 * - When destroyed by player gunfire, bursts open and releases a power-up capsule (PowerUpCapsule).
 * - Item drops include: WIDE, LASER, BOMB, SPEED, and LIFE.
 */
public class ItemBox extends PowerUp {

    public ItemBox(float x, float y) {
        super(x, y, Type.ITEM_BOX, chooseRandomDrop());
    }

    public ItemBox(float x, float y, Type dropType) {
        super(x, y, Type.ITEM_BOX, dropType);
    }

    public static Type getRandomDrop() {
        return chooseRandomDrop();
    }
}
