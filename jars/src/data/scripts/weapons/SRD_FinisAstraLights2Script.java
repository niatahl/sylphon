package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

import java.awt.*;

public class SRD_FinisAstraLights2Script implements EveryFrameWeaponEffectPlugin {
    private static final float[] COLORS = {1f, 0.3f, 0.7f};

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI ship = weapon.getShip();
        if (ship == null) {
            return;
        }

        //Brightness is based on weapon state and shipsystem state in combination
        if (!(engine.getCustomData().get(ship.getId() + "SRD_GOETTER_GLOW_DATA") instanceof Float)) {
            engine.getCustomData().put(ship.getId() + "SRD_GOETTER_GLOW_DATA", 0f);
        }

        //Scales up one two thrids from shipsystem and one third from the weapon
        float currentBrightness = 0.66f * weapon.getShip().getSystem().getEffectLevel() + 0.33f * (float)engine.getCustomData().get(ship.getId() + "SRD_GOETTER_GLOW_DATA");

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