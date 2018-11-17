//By Nicke535, causes the beams on a weapon to converge
package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;

public class SRD_GotterdammerungScript implements EveryFrameWeaponEffectPlugin {

    //Note that the angles are in *either* direction, so a value of 20 creates a 40 degree cone in front of the weapon
    public static final float ANGLE_MAX   = 30f;
    public static final float ANGLE_MIN   = 0.0f;
    public static final float ROTATION_SPEED    = 4f;

    private float counter = 0f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        //Don't run if we are paused, or our weapon is null
        if (engine.isPaused() || weapon == null) {
            return;
        }

        //Increases our counter
        counter += amount * ROTATION_SPEED;

        //---------------------While we don't have multiple weapon sizes for this, we might want it later---------------
        //Handles hardpoint offsets
        for (int i = 1; i < weapon.getSpec().getHardpointAngleOffsets().size(); i++) {
            float angleToSet = (float)Math.sin(counter + (i * 2 * Math.PI / (weapon.getSpec().getHardpointAngleOffsets().size())));
            angleToSet *= ANGLE_MIN * weapon.getChargeLevel() + ANGLE_MAX * (1 - weapon.getChargeLevel());
            weapon.getSpec().getHardpointAngleOffsets().set(i, angleToSet);
        }

        //Handles hidden offsets
        for (int i = 0; i < weapon.getSpec().getHiddenAngleOffsets().size(); i++) {
            float angleToSet = (float)Math.sin(counter + (i * 2 * Math.PI / (weapon.getSpec().getHiddenAngleOffsets().size())));
            angleToSet *= ANGLE_MIN * weapon.getChargeLevel() + ANGLE_MAX * (1 - weapon.getChargeLevel());
            weapon.getSpec().getHiddenAngleOffsets().set(i, angleToSet);
        }

        //Handles turret offsets
        for (int i = 0; i < weapon.getSpec().getTurretAngleOffsets().size(); i++) {
            float angleToSet = (float)Math.sin(counter + (i * 2 * Math.PI / (weapon.getSpec().getTurretAngleOffsets().size())));
            angleToSet *= ANGLE_MIN * weapon.getChargeLevel() + ANGLE_MAX * (1 - weapon.getChargeLevel());
            weapon.getSpec().getTurretAngleOffsets().set(i, angleToSet);
        }
    }
}