package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

import java.awt.*;

public class SRD_FinisAstraLights1Script implements EveryFrameWeaponEffectPlugin {
    private static final float[] COLORS_BRIGHT = {1f, 0.3f, 0.7f};
    private static final float[] COLORS_DULL = {1f, 0f, 0.2f};

    private static final float MAX_OPACITY_FADE_PER_SECOND = 0.13f;

    private float previousBrightness = 0f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI ship = weapon.getShip();
        if (ship == null) {
            return;
        }
        if (engine.isPaused()) {
            amount = 0f;
        }

        //Brightness is based on the main weapon, but also glows during cooldown of the weapon
        if (!(engine.getCustomData().get(ship.getId() + "SRD_GOETTER_GLOW_DATA") instanceof Float)) {
            engine.getCustomData().put(ship.getId() + "SRD_GOETTER_GLOW_DATA", 0f);
        }

        //Quadratic scaling, to give more "umphf"
        float currentBrightness = (float)engine.getCustomData().get(ship.getId() + "SRD_GOETTER_GLOW_DATA") * (float)engine.getCustomData().get(ship.getId() + "SRD_GOETTER_GLOW_DATA");

        //We only "lose" brightness at a certain speed: if our current brightness is lower than the "calculated" fade-out brightness, use the calculated one instead
        if (currentBrightness < (previousBrightness - (MAX_OPACITY_FADE_PER_SECOND * amount))) {
            currentBrightness = (previousBrightness - (MAX_OPACITY_FADE_PER_SECOND * amount));
        }
        previousBrightness = currentBrightness;

        //A piece should never have glowing lights
        if (ship.isPiece()) {
            currentBrightness = 0f;
        }

        //Now, set the color to the one we want (sliding gradually between our two colors), and include opacity
        Color colorToUse = new Color(currentBrightness * COLORS_BRIGHT[0] + (1f - currentBrightness) * COLORS_DULL[0],
                currentBrightness * COLORS_BRIGHT[1] + (1f - currentBrightness) * COLORS_DULL[1],
                currentBrightness * COLORS_BRIGHT[2] + (1f - currentBrightness) * COLORS_DULL[2], currentBrightness);

        //And finally actually apply the color
        weapon.getSprite().setColor(colorToUse);
    }
}