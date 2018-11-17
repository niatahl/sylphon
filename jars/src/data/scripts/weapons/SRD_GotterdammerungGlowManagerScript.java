package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;

public class SRD_GotterdammerungGlowManagerScript implements EveryFrameWeaponEffectPlugin {

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        // Don't bother with any unecessary checks
        if (weapon.getShip() == null) {
            return;
        }

        ShipAPI ship = weapon.getShip();

        //Saves some values for adjusting the ship's global glow sprites
        engine.getCustomData().put(ship.getId() + "SRD_GOETTER_GLOW_DATA", weapon.getChargeLevel());
    }
}