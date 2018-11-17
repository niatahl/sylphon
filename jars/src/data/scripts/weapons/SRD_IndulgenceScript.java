//By Nicke535, causes a weapon's barrels to diverge and then reconverge during its burst
package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lwjgl.util.vector.Vector2f;

public class SRD_IndulgenceScript implements EveryFrameWeaponEffectPlugin {
    //Note that the angles are in *either* direction, so a value of 20 creates a 40 degree cone in front of the weapon
    public static final float ANGLE_MAX  = 8f;

    private float timeSpentInBurst = 0f;

    //For reasons unknown, the weapon's derived burst duration is too short; this is multiplied with the burst fire duration to alleviate the issue
    private float graceMult = 1.4f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        //Don't run if we are paused, or our weapon is null
        if (engine.isPaused() || weapon == null || weapon.getShip() == null) {
            return;
        }

        //Lessens the angle if we have a lot of range: increases efficiency on the Paragon, for example
        float angleLessener = 1f;
        if (weapon.getRange() > 500f) {
            angleLessener = weapon.getRange() / 500f;
        }

        //Gets how far in we are during the burst
        if (weapon.getChargeLevel() > 0f) {
            timeSpentInBurst += amount;
        } else {
            timeSpentInBurst = amount;
            return;
        }

        float burstProgress = timeSpentInBurst / (weapon.getSpec().getDerivedStats().getBurstFireDuration()*graceMult / weapon.getShip().getMutableStats().getEnergyRoFMult().getModifiedValue());

        if (burstProgress < 0.5f) {
            burstProgress *= 2f;
        } else {
            burstProgress = 1f - burstProgress;
            burstProgress *= 2f;
        }
        //Equalises the burst progress
        burstProgress = Math.max(0f, Math.min(1f, burstProgress));

        //Now that we have a 1-0 value for burst progress, we can convert that into an angle
        float angleToSet = ANGLE_MAX * burstProgress / angleLessener;

        //Handles all the offsets: note that the weapon must have a number of offsets dividable by 2 to function as intended
        for (int i = 0; i+1 < weapon.getSpec().getHardpointAngleOffsets().size(); i+=2) {
            weapon.getSpec().getHardpointAngleOffsets().set(i, angleToSet);
            weapon.getSpec().getHardpointAngleOffsets().set(i+1, -angleToSet);
        }
        for (int i = 0; i+1 < weapon.getSpec().getHiddenFireOffsets().size(); i+=2) {
            weapon.getSpec().getHiddenAngleOffsets().set(i, angleToSet);
            weapon.getSpec().getHiddenAngleOffsets().set(i+1, -angleToSet);
        }
        for (int i = 0; i+1 < weapon.getSpec().getTurretAngleOffsets().size(); i+=2) {
            weapon.getSpec().getTurretAngleOffsets().set(i, angleToSet);
            weapon.getSpec().getTurretAngleOffsets().set(i+1, -angleToSet);
        }
    }
}
