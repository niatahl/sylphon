//By Nicke535, causes the beams on a weapon to rotate around the fire offset, and converge over time
//Also accepts the following weapon tags:
//  SRD_ROTATING_BEAM_WIDE :            Doubles the final convergence radius
//  SRD_ROTATING_BEAM_FULL_CONVERGE :   Causes the beam to completely converge at full charge
//  SRD_ROTATING_BEAM_CENTER_BEAM :     Causes the last beam offset to not spin
package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lazywizard.lazylib.FastTrig;
import org.lwjgl.util.vector.Vector2f;

public class SRD_LucidasScript implements EveryFrameWeaponEffectPlugin {
    //Note that the angles are in *either* direction, so a value of 20 creates a 40 degree cone in front of the weapon
    private static final float ANGLE_MAX   = 12f;
    private static final float ANGLE_MIN   = 2.5f;
    private static final float ANGLE_WAVE   = 1f;

    private static final float ROTATION_SPEED    = 3.4f;

    private float counter = 0f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        //Don't run if we are paused, or our weapon is null
        if (engine.isPaused() || weapon == null) {
            return;
        }

        counter += amount * ROTATION_SPEED;

        //Sets some variables
        float angleMax = ANGLE_MAX;
        float angleMin = ANGLE_MIN;

        //Checks for tags affecting convergence and beam placement
        //Startmod affects whether we should ignore the last beam offset for our calculations
        int startMod = 0;
        if (weapon.getSpec().getTags().contains("SRD_ROTATING_BEAM_WIDE")) {
            angleMin *= 2f;
        } else if (weapon.getSpec().getTags().contains("SRD_ROTATING_BEAM_FULL_CONVERGE")) {
            angleMin = 0f;
        } else if (weapon.getSpec().getTags().contains("SRD_ROTATING_BEAM_CENTER_BEAM")) {
            startMod = 1;
        }

        //We want a sine-shaped wave to affect our minimum angle, which loops every 0.95175 / 2 seconds
        float loopProgress = (counter / (0.95175f * ROTATION_SPEED)) * (float)Math.PI;
        angleMax += Math.abs(FastTrig.sin(loopProgress) * ANGLE_WAVE);
        angleMin += Math.abs(FastTrig.sin(loopProgress) * ANGLE_WAVE);

        //Handles hardpoint offsets
        for (int i = 0; i < weapon.getSpec().getHardpointAngleOffsets().size() - startMod; i++) {
            float angleToSet = (float)Math.sin(counter + (i * 2 * Math.PI / (weapon.getSpec().getHardpointAngleOffsets().size() - startMod)));
            angleToSet *= angleMin * weapon.getChargeLevel() + angleMax * (1 - weapon.getChargeLevel());
            weapon.getSpec().getHardpointAngleOffsets().set(i, angleToSet);
        }

        //Handles hidden offsets
        for (int i = 0; i < weapon.getSpec().getHiddenAngleOffsets().size() - startMod; i++) {
            float angleToSet = (float)Math.sin(counter + (i * 2 * Math.PI / (weapon.getSpec().getHiddenAngleOffsets().size() - startMod)));
            angleToSet *= angleMin * weapon.getChargeLevel() + angleMax * (1 - weapon.getChargeLevel());
            weapon.getSpec().getHiddenAngleOffsets().set(i, angleToSet);
        }

        //Handles turret offsets
        for (int i = 0; i < weapon.getSpec().getTurretAngleOffsets().size() - startMod; i++) {
            float angleToSet = (float)Math.sin(counter + (i * 2 * Math.PI / (weapon.getSpec().getTurretAngleOffsets().size() - startMod)));
            angleToSet *= angleMin * weapon.getChargeLevel() + angleMax * (1 - weapon.getChargeLevel());
            weapon.getSpec().getTurretAngleOffsets().set(i, angleToSet);
        }
    }
}
