package com.diamon.finalmission.entities;

/**
 * Floating Item Box (Canister) for Final Mission NES:
 * - Floats across the screen with gentle sinusoidal oscillation.
 * - Uses animated 4-frame rotation sequence (cajaPoder1..4.png).
 * - When destroyed by player gunfire, bursts open and releases a power-up capsule (PowerUpCapsule).
 */
public class ItemBox extends PowerUp {

    public ItemBox(float x, float y) {
        super(x, y, Type.ITEM_BOX);
    }

    public ItemBox(float x, float y, Type dropType) {
        super(x, y, Type.ITEM_BOX, dropType);
    }
}
