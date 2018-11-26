//By Nicke535, causes the beams on a weapon to quickly go back-and-forth like a scanner. The weapon is assumed to only have 2 beams
//Also handles applying the effects of the Apocatastasis on-hit
package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public class SRD_ApocatastasisEveryFrameScript implements EveryFrameWeaponEffectPlugin {
    //The maximum distance the beams can go towards each side
    public static final float DISTANCE_MAX = 6f;

    //Speed at which the beams move back-and-forth
    public static final float SLIDE_SPEED = 24f;

    //How fast the damage bonus from our on-hit script fades: defined in percent-per-second
    public static final float DAMAGE_BONUS_LOSS_PER_SECOND = 0.15f;

    //Maximum damage increase we can have at a time
    public static final float MAX_DAMAGE_BONUS = 1f;

    //Instantiates some small in-script variables
    private float moveDirection = 1f;
    private float currentPos = 0f;

    //A map of ships and their current damage-taken-increase, due to the on-hit effect of the weapon
    private Map<ShipAPI, Float> mainEffectMap = new HashMap<>();

    //The key we use to identify effects from this script on MutableStats
    private static final String EFFECT_KEY = "SRD_KEY_FOR_APOCATASTASIS";

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        //Don't run if we are paused, or our weapon is null
        if (engine.isPaused() || weapon == null) {
            return;
        }

        /*  ---ON-HIT EFFECT HANDLING---  */
        for (ShipAPI key : mainEffectMap.keySet()) {
            //Ignore if the key doesn't have an associated value
            if (mainEffectMap.get(key) == null) {
                return;
            }

            //Tick down the damage bonus, and remove the effect if it's too low
            mainEffectMap.put(key, mainEffectMap.get(key)-(DAMAGE_BONUS_LOSS_PER_SECOND*amount));
            if (mainEffectMap.get(key) <= 0f) {
                key.getMutableStats().getKineticDamageTakenMult().unmodify(EFFECT_KEY+weapon.getId());
                key.getMutableStats().getEnergyDamageTakenMult().unmodify(EFFECT_KEY+weapon.getId());
                key.getMutableStats().getHighExplosiveDamageTakenMult().unmodify(EFFECT_KEY+weapon.getId());
                key.getMutableStats().getFragmentationDamageTakenMult().unmodify(EFFECT_KEY+weapon.getId());
                key.getMutableStats().getShieldDamageTakenMult().unmodify(EFFECT_KEY+weapon.getId());
                mainEffectMap.remove(key);
                continue;
            }

            //Otherwise, we apply the damage bonus
            key.getMutableStats().getKineticDamageTakenMult().modifyMult(EFFECT_KEY+weapon.getId(), mainEffectMap.get(key)+1f);
            key.getMutableStats().getEnergyDamageTakenMult().modifyMult(EFFECT_KEY+weapon.getId(), mainEffectMap.get(key)+1f);
            key.getMutableStats().getHighExplosiveDamageTakenMult().modifyMult(EFFECT_KEY+weapon.getId(), mainEffectMap.get(key)+1f);
            key.getMutableStats().getFragmentationDamageTakenMult().modifyMult(EFFECT_KEY+weapon.getId(), mainEffectMap.get(key)+1f);
            key.getMutableStats().getShieldDamageTakenMult().modifyMult(EFFECT_KEY+weapon.getId(), (mainEffectMap.get(key)/2f)+1f);

            //If this key is also the player ship, indicate that we're taking more damage
            if (key == engine.getPlayerShip()) {
                Global.getCombatEngine().maintainStatusForPlayerShip(EFFECT_KEY + "_TOOLTIP", "graphics/icons/hullsys/entropy_amplifier.png", "Analyzed", "Taking additional damage", true);
            }
        }

        /*  ---MOVING OFFSET HANDLING---  */
        //Slide along our position
        currentPos+=amount*SLIDE_SPEED*moveDirection;
        if (currentPos > DISTANCE_MAX) {
            currentPos = DISTANCE_MAX;
            moveDirection = -1f;
        } else if (currentPos < (DISTANCE_MAX * -1f)) {
            currentPos = -DISTANCE_MAX;
            moveDirection = 1f;
        }


        //Handles hardpoint offsets
        for (int i = 0; i < weapon.getSpec().getHardpointAngleOffsets().size(); i++) {
            weapon.getSpec().getHardpointFireOffsets().get(i).y = currentPos * (1f + (-2f * (i % 2)));
        }

        //Handles hidden offsets
        for (int i = 0; i < weapon.getSpec().getHiddenAngleOffsets().size(); i++) {
            weapon.getSpec().getHiddenFireOffsets().get(i).y = currentPos * (1f + (-2f * (i % 2)));
        }

        //Handles turret offsets
        for (int i = 0; i < weapon.getSpec().getTurretAngleOffsets().size(); i++) {
            weapon.getSpec().getTurretFireOffsets().get(i).y = currentPos * (1f + (-2f * (i % 2)));
        }
    }

    //This is for adding our on-hit effects to the script
    public void addEffect (ShipAPI target, float amountToAdd) {
        if (mainEffectMap.get(target) != null) {
            mainEffectMap.put(target, mainEffectMap.get(target)+amountToAdd);
        } else {
            mainEffectMap.put(target, amountToAdd);
        }

        //Don't increase above our cap!
        if (mainEffectMap.get(target) > MAX_DAMAGE_BONUS) {
            mainEffectMap.put(target, MAX_DAMAGE_BONUS);
        }
    }
}
