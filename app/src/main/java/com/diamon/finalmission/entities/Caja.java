package com.diamon.finalmission.entities;

/**
 * Contenedor de Poder flotante (Caja) de Final Mission:
 * - Flota a través de la pantalla con suave movimiento oscilatorio.
 * - Utiliza la secuencia de cuadros cajaPoder1..4.png.
 * - Al ser destruida por disparos, se abre y libera una cápsula de poder (Poder).
 */
public class Caja extends PowerUp {

    public Caja(float x, float y) {
        super(x, y, Type.CAJA);
    }

    public Caja(float x, float y, Type dropType) {
        super(x, y, Type.CAJA, dropType);
    }
}
