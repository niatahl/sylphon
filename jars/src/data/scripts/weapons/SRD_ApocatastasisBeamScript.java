//By Nicke535, help script for the Apocatastasis
package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;

public class SRD_ApocatastasisBeamScript implements BeamEffectPlugin {
    //How much damage increase do we add per second when we hit hull?
    private static final float DAMAGE_INCREASE_PER_SECOND_HULL = 0.3f;

    //How much damage increase do we add per second when we hit shields?
    private static final float DAMAGE_INCREASE_PER_SECOND_SHIELDS = 0.15f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        // Don't bother with any unecessary checks
        if (beam.getWeapon().getShip() == null || beam.getDamageTarget() == null) {
            return;
        }

        if (beam.didDamageThisFrame()) {

            //Saves well-used variables beforehand
            WeaponAPI weapon = beam.getWeapon();
            ShipAPI ship = weapon.getShip();
            CombatEntityAPI target = beam.getDamageTarget();

            //Only care about ships being hit
            if (!(target instanceof ShipAPI)) {
                return;
            }
            ShipAPI beamTarget = (ShipAPI) target;

            //Checks if we hit shields or not
            boolean hitShields = false;
            if (beamTarget.getShield() != null && beamTarget.getShield().isOn() && beamTarget.getShield().isWithinArc(beam.getTo())) {
                hitShields = true;
            }

            //Handle the effect via our everyFrameScript on the weapon
            float effectToAdd = amount * DAMAGE_INCREASE_PER_SECOND_HULL;
            if (hitShields) {
                effectToAdd = amount * DAMAGE_INCREASE_PER_SECOND_SHIELDS;
            }
            ((SRD_ApocatastasisEveryFrameScript) weapon.getEffectPlugin()).addEffect(beamTarget, (effectToAdd+(SRD_ApocatastasisEveryFrameScript.DAMAGE_BONUS_LOSS_PER_SECOND*amount))/2f); //Divide by 2: 2 beams on the thing!
        }
    }
}
