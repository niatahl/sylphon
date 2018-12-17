//By Nicke535
//Spawns tiny, cute nanobot swarms from a beam's target and deals a bit of extra damage
package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import data.scripts.plugins.SRD_NanobotsPlugin;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class SRD_NanobotBeamEffect implements BeamEffectPlugin {
    //How large a percentage of our damage is returned as "metal"
    private static final float LOOT_MULT = 0.6f;

    //How much less damage do we do to "proper" (non-fighter, non-missile, non-asteroid) ships?
    private static final float BIG_DAMAGE_MULT = 0.35f;

    //How much extra damage does our beam do as "disassembly damage" per second
    private static final float EXTRA_DPS = 400f;

    //The shortest allowed time between two nanobot swarms spawning
    private static final float MIN_WAIT = 0.10f;
    //The longest allowed time between two nanobot swarms spawning
    private static final float MAX_WAIT = 0.18f;

    //Counters, to ensure we don't trigger too often, but still keep our DPS
    private float counter = 0f;
    private float waitNextTime = MathUtils.getRandomNumberInRange(MIN_WAIT, MAX_WAIT);

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        // Don't bother with any unecessary checks
        if (beam.getWeapon().getShip() == null || beam.getDamageTarget() == null) {
            return;
        }
        if (beam.didDamageThisFrame()) {
            //Only trigger now-and-then to avoid clutter with too many nanobot swarms
            counter += amount;
            if (counter < waitNextTime) {
                return;
            } else {
                waitNextTime = MathUtils.getRandomNumberInRange(MIN_WAIT, MAX_WAIT);
            }

            //Saves well-used variables beforehand
            WeaponAPI weapon = beam.getWeapon();
            ShipAPI ship = weapon.getShip();
            CombatEntityAPI target = beam.getDamageTarget();

            //Ships being hit use different calculations, due to shields and stuff
            if (target instanceof ShipAPI) {
                ShipAPI beamTarget = (ShipAPI) target;

                //Checks if we hit shields or not
                boolean hitShields = false;
                if (beamTarget.getShield() != null && beamTarget.getShield().isOn() && beamTarget.getShield().isWithinArc(beam.getTo())) {
                    hitShields = true;
                }

                //If we hit shields, we do *nothing*
                if (hitShields) {
                    counter = 0f;
                    return;
                }

                //If we didn't, we do damage based on if we hit a fighter or not
                float damageMult = 1f;
                if (beamTarget.getHullSize() != ShipAPI.HullSize.FIGHTER) {
                    damageMult = BIG_DAMAGE_MULT;
                }
                engine.applyDamage(target, beam.getTo(), damageMult*EXTRA_DPS*counter, DamageType.FRAGMENTATION, 0f, true,
                        false, ship, true);

                //And spawn a swarm with our tasty, tasty metal
                float angleToSpawnAt = VectorUtils.getAngle(beam.getTo(), beam.getFrom());
                angleToSpawnAt += MathUtils.getRandomNumberInRange(-65f, 65f);
                SRD_NanobotsPlugin.SpawnNanobotSwarm(weapon, damageMult*EXTRA_DPS*counter*LOOT_MULT, angleToSpawnAt, MathUtils.getRandomNumberInRange(150f, 190f),
                        MathUtils.getRandomNumberInRange(550f, 700f), new Vector2f(beam.getTo()));

                //Oh, and reset that counter. Important.
                counter = 0f;
            } else {
                //This is if we hit a combat entity which *isn't* a ship (so a missile or an asteroid... or some hacked-together code abomination)
                //Just deal some damage...
                engine.applyDamage(target, beam.getTo(), EXTRA_DPS*counter, DamageType.FRAGMENTATION, 0f, true,
                        false, ship, true);

                //And spawn a swarm
                float angleToSpawnAt = VectorUtils.getAngle(beam.getTo(), beam.getFrom());
                angleToSpawnAt += MathUtils.getRandomNumberInRange(-65f, 65f);
                SRD_NanobotsPlugin.SpawnNanobotSwarm(weapon, EXTRA_DPS*counter*LOOT_MULT, angleToSpawnAt, MathUtils.getRandomNumberInRange(150f, 190f),
                        MathUtils.getRandomNumberInRange(550f, 700f), new Vector2f(beam.getTo()));

                //Oh, and reset that counter. Important.
                counter = 0f;
            }
        }
    }
}