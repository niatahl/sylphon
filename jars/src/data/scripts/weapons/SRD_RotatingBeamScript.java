//By Nicke535, causes the beams on a weapon to rotate around the fire offset, and converge over time
//Also accepts the following weapon tags:
//  SRD_ROTATING_BEAM_WIDE :            Doubles the final convergence radius
//  SRD_ROTATING_BEAM_FULL_CONVERGE :   Causes the beam to completely converge at full charge
//  SRD_ROTATING_BEAM_CENTER_BEAM :     Causes the last beam offset to not spin
package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class SRD_RotatingBeamScript implements EveryFrameWeaponEffectPlugin {
    //Note that the angles are in *either* direction, so a value of 20 creates a 40 degree cone in front of the weapon
    public static final float ANGLE_MAX_LARGE   = 30f;
    public static final float ANGLE_MAX_MEDIUM  = 22f;
    public static final float ANGLE_MAX_SMALL   = 12f;

    public static final float ANGLE_MIN_LARGE   = 2.0f;
    public static final float ANGLE_MIN_MEDIUM  = 1.0f;
    public static final float ANGLE_MIN_SMALL   = 2.5f;

    public static final float ROTATION_SPEED    = 1.8f;

    private float counter = 0f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        //Don't run if we are paused, or our weapon is null
        if (engine.isPaused() || weapon == null) {
            return;
        }

        counter += amount * ROTATION_SPEED;

        //Sets which angles we should use depending on weapon size, and also detects if we should use a firing offset
        boolean physicalOffset = true;
        float angleMax = ANGLE_MAX_LARGE;
        float angleMin = ANGLE_MIN_LARGE;
        if (weapon.getSize() == WeaponAPI.WeaponSize.MEDIUM) {
            physicalOffset = false;
            angleMax = ANGLE_MAX_MEDIUM;
            angleMin = ANGLE_MIN_MEDIUM;
        } else if (weapon.getSize() == WeaponAPI.WeaponSize.SMALL) {
            physicalOffset = false;
            angleMax = ANGLE_MAX_SMALL;
            angleMin = ANGLE_MIN_SMALL;
        }

        //Checks for tags affecting convergence and beam placement
        //Startmod affects whether we should ignore the last beam offset for our calculations
        int startMod = 0;
        if (weapon.getSpec().getTags().contains("SRD_ROTATING_BEAM_WIDE")) {
            angleMin *= 2f;
        } else if (weapon.getSpec().getTags().contains("SRD_ROTATING_BEAM_FULL_CONVERGE")) {
            angleMin = 0f;
            physicalOffset = false;
        } else if (weapon.getSpec().getTags().contains("SRD_ROTATING_BEAM_CENTER_BEAM")) {
            startMod = 1;
        }

        //Handles hardpoint offsets
        for (int i = 0; i < weapon.getSpec().getHardpointAngleOffsets().size() - startMod; i++) {
            float angleToSet = (float)Math.sin(counter + (i * 2 * Math.PI / (weapon.getSpec().getHardpointAngleOffsets().size() - startMod)));
            //Handles physical offset, if we have it
            if (physicalOffset) {
                Vector2f offsetToSet = weapon.getSpec().getHardpointFireOffsets().get(i);
                offsetToSet.y = angleToSet * 6f;
                weapon.getSpec().getHardpointFireOffsets().set(i, offsetToSet);
            }
            angleToSet *= angleMin * weapon.getChargeLevel() + angleMax * (1 - weapon.getChargeLevel());
            weapon.getSpec().getHardpointAngleOffsets().set(i, angleToSet);
        }

        //Handles hidden offsets
        for (int i = 0; i < weapon.getSpec().getHiddenAngleOffsets().size() - startMod; i++) {
            float angleToSet = (float)Math.sin(counter + (i * 2 * Math.PI / (weapon.getSpec().getHiddenAngleOffsets().size() - startMod)));
            //Handles physical offset, if we have it
            if (physicalOffset) {
                Vector2f offsetToSet = weapon.getSpec().getHiddenFireOffsets().get(i);
                offsetToSet.y = angleToSet * 6f;
                weapon.getSpec().getHiddenFireOffsets().set(i, offsetToSet);
            }
            angleToSet *= angleMin * weapon.getChargeLevel() + angleMax * (1 - weapon.getChargeLevel());
            weapon.getSpec().getHiddenAngleOffsets().set(i, angleToSet);
        }

        //Handles turret offsets
        for (int i = 0; i < weapon.getSpec().getTurretAngleOffsets().size() - startMod; i++) {
            float angleToSet = (float)Math.sin(counter + (i * 2 * Math.PI / (weapon.getSpec().getTurretAngleOffsets().size() - startMod)));
            //Handles physical offset, if we have it
            if (physicalOffset) {
                Vector2f offsetToSet = weapon.getSpec().getTurretFireOffsets().get(i);
                offsetToSet.y = angleToSet * 6f;
                weapon.getSpec().getTurretFireOffsets().set(i, offsetToSet);
            }
            angleToSet *= angleMin * weapon.getChargeLevel() + angleMax * (1 - weapon.getChargeLevel());
            weapon.getSpec().getTurretAngleOffsets().set(i, angleToSet);
        }
    }
}
