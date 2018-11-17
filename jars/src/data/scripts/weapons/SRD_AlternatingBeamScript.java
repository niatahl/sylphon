//By Nicke535, this script allows a beam weapon to fire from alternating barrels
//To make the beams converge, set convergeOnPoint to true on the weapon, and add *2 offsets on the weapon at 0, 0*
//To make the beams not converge, set convergeOnPoint to false on the weapon, an add *1 offset to the weapon at 0, 0*
package data.scripts.weapons;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class SRD_AlternatingBeamScript implements EveryFrameWeaponEffectPlugin {
    //----------------This area is for setting all offsets for the barrels: note that the turret and hardpoint version of the weapon *must* have an equal amount of offsets--------------------
    //Offsets for small weapons
    private static Map<Integer, Vector2f> smallHardpointOffsets = new HashMap<Integer, Vector2f>();
    static {
        smallHardpointOffsets.put(0, new Vector2f(23f, 6f));
        smallHardpointOffsets.put(1, new Vector2f(24f, 3f));
        smallHardpointOffsets.put(2, new Vector2f(25f, 0f));
        smallHardpointOffsets.put(3, new Vector2f(24f, -3f));
        smallHardpointOffsets.put(4, new Vector2f(23f, -6f));
    }
    private static Map<Integer, Vector2f> smallTurretOffsets = new HashMap<Integer, Vector2f>();
    static {
        smallTurretOffsets.put(0, new Vector2f(19f, 6f));
        smallTurretOffsets.put(1, new Vector2f(20f, 3f));
        smallTurretOffsets.put(2, new Vector2f(21f, 0f));
        smallTurretOffsets.put(3, new Vector2f(20f, -3f));
        smallTurretOffsets.put(4, new Vector2f(19f, -6f));
    }

    //Offsets for medium weapons
    private static Map<Integer, Vector2f> mediumHardpointOffsets = new HashMap<Integer, Vector2f>();
    static {
        mediumHardpointOffsets.put(0, new Vector2f(23f, 6f));
        mediumHardpointOffsets.put(1, new Vector2f(24f, 3f));
        mediumHardpointOffsets.put(2, new Vector2f(25f, 0f));
        mediumHardpointOffsets.put(3, new Vector2f(24f, -3f));
        mediumHardpointOffsets.put(4, new Vector2f(23f, -6f));
    }
    private static Map<Integer, Vector2f> mediumTurretOffsets = new HashMap<Integer, Vector2f>();
    static {
        mediumTurretOffsets.put(0, new Vector2f(19f, 6f));
        mediumTurretOffsets.put(1, new Vector2f(20f, 3f));
        mediumTurretOffsets.put(2, new Vector2f(21f, 0f));
        mediumTurretOffsets.put(3, new Vector2f(20f, -3f));
        mediumTurretOffsets.put(4, new Vector2f(19f, -6f));
    }

    //Offsets for large weapons
    private static Map<Integer, Vector2f> largeHardpointOffsets = new HashMap<Integer, Vector2f>();
    static {
        largeHardpointOffsets.put(0, new Vector2f(23f, 6f));
        largeHardpointOffsets.put(1, new Vector2f(24f, 3f));
        largeHardpointOffsets.put(2, new Vector2f(25f, 0f));
        largeHardpointOffsets.put(3, new Vector2f(24f, -3f));
        largeHardpointOffsets.put(4, new Vector2f(23f, -6f));
    }
    private static Map<Integer, Vector2f> largeTurretOffsets = new HashMap<Integer, Vector2f>();
    static {
        largeTurretOffsets.put(0, new Vector2f(19f, 6f));
        largeTurretOffsets.put(1, new Vector2f(20f, 3f));
        largeTurretOffsets.put(2, new Vector2f(21f, 0f));
        largeTurretOffsets.put(3, new Vector2f(20f, -3f));
        largeTurretOffsets.put(4, new Vector2f(19f, -6f));
    }
    //-----------------------------------------------------------------------------END OF OFFSET SPECIFICATIONS---------------------------------------------------------------------------------

    //Instantiates variables we will use later
    private int counter = 0;
    private boolean runOnce = true;
    private Map<Integer, BeamAPI> beamMap = new HashMap<Integer, BeamAPI>();

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        //Don't run if we are paused, or our if weapon is null
        if (engine.isPaused() || weapon == null) {
            return;
        }

        //Resets the beam map and variables if we are not firing
        if (weapon.getChargeLevel() <= 0) {
            beamMap.clear();
            runOnce = true;
            return;
        }

        //If we are firing, start the code and change variables
        if (weapon.getChargeLevel() > 0f && runOnce) {
            runOnce = false;
            int counterForBeams = 0;
            for (BeamAPI beam : engine.getBeams()) {
                if (beam.getWeapon() == weapon) {
                    if (!beamMap.containsValue(beam)) {
                        beamMap.put(counterForBeams, beam);
                        counterForBeams++;
                    }
                }
            }
        } else {
            return;
        }

        //For converge code: hide the first beam by making it invisible, and ensure all further operations are done on the second beam
        int numOffset = 0;
        if (beamMap.get(1) != null) {
            beamMap.get(0).setCoreColor(new Color(0f, 0f, 0f));
            beamMap.get(0).setFringeColor(new Color(0f, 0f, 0f));
            numOffset = 1;
        }

        //The big if-block where the magic happens: change a weapon's fireOffset via the alternating pattern specified by small/medium/large Turret/Hardpoint Offsets
        if (weapon.getSize() == WeaponAPI.WeaponSize.SMALL) {
            counter++;
            if (!smallHardpointOffsets.containsKey(counter)) {
                counter = 0;
            }

            weapon.getSpec().getHardpointFireOffsets().set(numOffset, smallHardpointOffsets.get(counter));
            weapon.getSpec().getHiddenFireOffsets().set(numOffset, smallTurretOffsets.get(counter));
            weapon.getSpec().getTurretFireOffsets().set(numOffset, smallTurretOffsets.get(counter));
        } if (weapon.getSize() == WeaponAPI.WeaponSize.MEDIUM) {
            counter++;
            if (!mediumHardpointOffsets.containsKey(counter)) {
                counter = 0;
            }

            weapon.getSpec().getHardpointFireOffsets().set(numOffset, mediumHardpointOffsets.get(counter));
            weapon.getSpec().getHiddenFireOffsets().set(numOffset, mediumTurretOffsets.get(counter));
            weapon.getSpec().getTurretFireOffsets().set(numOffset, mediumTurretOffsets.get(counter));
        } else if (weapon.getSize() == WeaponAPI.WeaponSize.LARGE) {
            counter++;
            if (!largeHardpointOffsets.containsKey(counter)) {
                counter = 0;
            }

            weapon.getSpec().getHardpointFireOffsets().set(numOffset, largeHardpointOffsets.get(counter));
            weapon.getSpec().getHiddenFireOffsets().set(numOffset, largeTurretOffsets.get(counter));
            weapon.getSpec().getTurretFireOffsets().set(numOffset, largeTurretOffsets.get(counter));
        }
    }
}
