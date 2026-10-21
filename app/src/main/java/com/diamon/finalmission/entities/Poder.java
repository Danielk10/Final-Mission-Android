package com.diamon.finalmission.entities;

/**
 * Cápsula de poder flotante (Poder) de Final Mission:
 * - W = Wide spread wave shot (poderW.png)
 * - L = Piercing laser beam (poderL.png)
 * - S = Speed boost (poderS.png)
 * - B = Life energy recovery (poderB.png)
 * - Estrictamente SIN BOMBAS (fiel al juego original de NES).
 * - Flota y oscila con la física auténtica portada de com.diamon.actor.Poder.
 */
public class Poder extends PowerUp {

    public Poder(float x, float y, Type type) {
        super(x, y, type);
    }
}
