//By Nicke535, tracks damage bonuses for the Benediction
package data.scripts.weapons;


import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.combat.CombatUtils;

import java.util.HashMap;
import java.util.Map;

public class SRD_BenedictionWeaponScript implements EveryFrameWeaponEffectPlugin {

    public Map<MissileAPI, Integer> missileTracker = new HashMap<MissileAPI, Integer>();
    public Map<Integer, Map<ShipAPI, Float>> comboMap = new HashMap<Integer, Map<ShipAPI, Float>>();

    private int currentBurst = 0;
    private boolean hasFired = false;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        //Don't run if we are paused, or our weapon is null
        if (engine.isPaused() || weapon == null) {
            return;
        }

        //If we are currently firing, indicate that a new "burst" has begun: increase our counter by 1 and clear the old map that used to inhibit this position
        if (weapon.getChargeLevel() > 0.5f && !hasFired) {
            //Iterates our burst, and resets if it gets really high
            currentBurst++;
            if (currentBurst >= 1000) {
                currentBurst = 0;
            }

            //Clears the map: unlikely we'll ever pass 1000 fire sequences, but it doesn't hurt to be on the safe side
            if (comboMap.get(currentBurst) == null) {
                comboMap.put(currentBurst, new HashMap<ShipAPI, Float>());
            } else {
                comboMap.get(currentBurst).clear();
            }
            hasFired = true;
        } else if (weapon.getChargeLevel() < 0.5f) {
            hasFired = false;
        }

        //Gets nearby missiles, and stores them for ease of access
        for (MissileAPI msl : CombatUtils.getMissilesWithinRange(weapon.getLocation(), 200f)) {
            if (msl.getWeapon() == weapon && !missileTracker.keySet().contains(msl)) {
                missileTracker.put(msl, currentBurst);
            }
        }
    }
}
