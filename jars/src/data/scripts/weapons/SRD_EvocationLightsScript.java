package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import data.scripts.shipsystems.SRD_NullspaceShunt;

import java.awt.*;

public class SRD_EvocationLightsScript implements EveryFrameWeaponEffectPlugin {
    private static final float[] COLORS = {0.73f, 0.13f, 0.86f};

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI ship = weapon.getShip();
        if (ship == null) {
            return;
        }

        //Brightness is based on current flux, but doesn't get quite as glowy as when you use the shipsystem
        float currentBrightness = ship.getFluxTracker().getFluxLevel() * 0.75f;

        //If we have our system active, glow according to that too
        if (ship.getSystem().getEffectLevel() > currentBrightness) {
            currentBrightness = ship.getSystem().getEffectLevel();
        }

        //A piece should never have glowing lights
        if (ship.isPiece()) {
            currentBrightness = 0f;
        }

        //Now, set the color to the one we want, and include opacity
        Color colorToUse = new Color(COLORS[0], COLORS[1], COLORS[2], currentBrightness);

        //And finally actually apply the color
        weapon.getSprite().setColor(colorToUse);
    }
}