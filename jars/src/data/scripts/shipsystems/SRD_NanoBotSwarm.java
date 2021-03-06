//By Nicke535
//Generates a swarm of nanobots around a ship, eating anything that gets too close
package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.Misc;
import data.scripts.plugins.SRD_NanobotsPlugin;
import data.scripts.util.MagicRender;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SRD_NanoBotSwarm extends BaseShipSystemScript {
    /* - GENERAL STATS FOR NANOBOTS - */
    //How much "base" metal do we need per OP of missile weapon refilled? THe "final" refill cost depends on the maximum
    //ammo of the missile; more ammo=less cost per missile (but not linearly, only x^0.75 effect).
    private static final float BASE_METAL_COST_PER_OP = 180f;

    //How much "base" metal do we need per point of armor restored (in a single armor cell)?
    private static final float BASE_METAL_COST_PER_ARMOR = 0.25f;

    //How high can the armor be repaired, at most, in an armor cell? Counted as a fraction
    private static final float MAX_ARMOR_REPAIR = 0.5f;


    /* - SWARM STATS - */
    //How big is the effect's AoE (in radius)? This both affects visuals and actual calculations
    private static final float NANOBOT_AOE = 370f;

    //How "thick" is the AoE? IE how far inwards does it go from its outer limit?
    private static final float NANOBOT_AOE_THICKNESS = 160f;

    //How much less damage do we do to "proper" (non-fighter, non-missile, non-asteroid) ships?
    private static final float BIG_DAMAGE_MULT = 0.75f;

    //How large a percentage of our damage is returned as "metal"
    private static final float LOOT_MULT = 0.35f;

    //How much less metal do we get from ships and fighters?
    private static final float SHIP_LOOT_MULT = 0.30f;

    //Damage per second against each target
    private static final float NANOBOT_DPS = 400f;

    //How much the damage varies; it feels more natural this way.
    private static final float NANOBOT_DAMAGE_VARIATION = 0.25f;

    //The chance to hit each frame, setting this higher indirectly lowers metal per individual return swarm
    private static final float HIT_RATE_MULT = 0.13f;

    //The angle between individual hit checks against non-fighter ships: higher means less accurate hit-checking, less
    //damage to large ships (should be handled in BIG_DAMAGE_MULT instead) and bigger distance between hit locations
    private static final float ANGLE_HITCHECK_STEP = 4f;

    //The maximum angle the swarm will apply damage in against any single ship. Increasing this above 90 is usually
    //overkill, since the ship would have to be effectively *on top* of you for you to notice a difference
    private static final float ANGLE_HITCHECK_SIZE = 80f;

    //The sound made when a missile weapon gets a new missile reloaded. Plays at lower volume and higher pitch for high-ammo weapons
    private static final String REFILL_SOUND = "ui_cargo_raremetals";


    /* - VISUALS - */
    //How much "bigger" is the total sprite compared to the actual area encompassed by the swarm?
    private static final float VISUAL_SIZE_INCREASE = 40f;

    //How fast does the bottom layer spin (in degrees/second)?
    private static final float VISUAL_SPIN_BOT = 200f;

    //How fast does the middle layer spin (in degrees/second)?
    private static final float VISUAL_SPIN_MID = 320f;

    //How fast does the top layer spin (in degrees/second)?
    private static final float VISUAL_SPIN_TOP = 250f;


    /* - CODE-SPECIFIC, DON'T TOUCH - */
    //Keeps track of "leftover metal" between frames
    private Map<WeaponAPI, Float> leftoverAmmo = new HashMap<>();

    //Spin counters
    private float spinCounterBot = 0f;
    private float spinCounterMid = 0f;
    private float spinCounterTop = 0f;


    //Main apply function
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        //Don't run if our engine is nonexistant
        if (Global.getCombatEngine() == null) {
            return;
        }

        ShipAPI ship = null;
        boolean player = false;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            player = ship == Global.getCombatEngine().getPlayerShip();
            id = id + "_" + ship.getId();
        } else {
            return;
        }

        //Time counter
        float amount = Global.getCombatEngine().getElapsedInLastFrame();
        if (Global.getCombatEngine().isPaused()) {
            amount = 0;
        }

        //If we aren't using the system, we *only* run the metal-reclamation code
        handleMetal(ship, amount);
        if (state == State.COOLDOWN || state == State.IDLE) {
            unapply(stats, id);
            return;
        }


        //Otherwise... we run stuff. First, visuals: simply spawn one-frame effects that circle
        spinCounterBot += amount * VISUAL_SPIN_BOT;
        spinCounterMid += amount * VISUAL_SPIN_MID;
        spinCounterTop += amount * VISUAL_SPIN_TOP;
        Color colorToUse = new Color (1f, 1f, 1f, effectLevel*0.5f);
        MagicRender.objectspace(Global.getSettings().getSprite("SRD_fx", "nanobot_swarm_circle_low"), ship, Misc.ZERO,
                Misc.ZERO, new Vector2f((NANOBOT_AOE+VISUAL_SIZE_INCREASE)*2f*effectLevel, NANOBOT_AOE*2f*effectLevel), Misc.ZERO,
                spinCounterBot, VISUAL_SPIN_BOT, true, colorToUse, false, 0f, 0.02f, 0.04f, true);
        MagicRender.objectspace(Global.getSettings().getSprite("SRD_fx", "nanobot_swarm_circle_mid"), ship, Misc.ZERO,
                Misc.ZERO, new Vector2f((NANOBOT_AOE+VISUAL_SIZE_INCREASE)*2f*effectLevel, NANOBOT_AOE*2f*effectLevel), Misc.ZERO,
                spinCounterMid, VISUAL_SPIN_MID, true, colorToUse, false, 0f, 0.02f, 0.04f, true);
        MagicRender.objectspace(Global.getSettings().getSprite("SRD_fx", "nanobot_swarm_circle_top"), ship, Misc.ZERO,
                Misc.ZERO, new Vector2f((NANOBOT_AOE+VISUAL_SIZE_INCREASE)*2f*effectLevel, NANOBOT_AOE*2f*effectLevel), Misc.ZERO,
                spinCounterTop, VISUAL_SPIN_TOP, true, colorToUse, false, 0f, 0.02f, 0.04f, true);
        /* OLD, DEPRECATED VISUALS
        MagicRender.singleframe(Global.getSettings().getSprite("SRD_fx", "nanobot_swarm_circle_low"), ship.getLocation(),
                new Vector2f(NANOBOT_AOE*2f*effectLevel, NANOBOT_AOE*2f*effectLevel), spinCounterBot, colorToUse, false);
        MagicRender.singleframe(Global.getSettings().getSprite("SRD_fx", "nanobot_swarm_circle_mid"), ship.getLocation(),
                new Vector2f(NANOBOT_AOE*2f*effectLevel, NANOBOT_AOE*2f*effectLevel), spinCounterMid, colorToUse, false);
        MagicRender.singleframe(Global.getSettings().getSprite("SRD_fx", "nanobot_swarm_circle_top"), ship.getLocation(),
                new Vector2f(NANOBOT_AOE*2f*effectLevel, NANOBOT_AOE*2f*effectLevel), spinCounterTop, colorToUse, false);
        */

        //Then, get all targets in range and apply effects to them
        for (CombatEntityAPI target : CombatUtils.getEntitiesWithinRange(ship.getLocation(), NANOBOT_AOE*effectLevel)) {
            //Ignore collision-less targets
            if (target.getCollisionClass() == CollisionClass.NONE) {
                continue;
            }

            //If the target is *too* close, we ignore it
            if (MathUtils.getDistance(target.getLocation(), ship.getLocation()) + target.getCollisionRadius() < (NANOBOT_AOE-NANOBOT_AOE_THICKNESS)*effectLevel) {
                continue;
            }

            //Otherwise, we can hit it: check if it's a ship beforehand, since those use different calculations
            if (target instanceof ShipAPI && ((ShipAPI) target).getHullSize() != ShipAPI.HullSize.FIGHTER) {
                makeAttackAgainstBigTarget((ShipAPI)target, ship, amount);
            } else {
                makeAttackAgainstSmallTarget(target, ship, amount);
            }
        }
    }


    //We never call this. Ignore it.
    public void unapply(MutableShipStatsAPI stats, String id) {

    }


    //Status data : we don't really have any to display, just return null
    public StatusData getStatusData(int index, State state, float effectLevel) {
        return null;
    }


    //Handles all metal-reclamation effects
    private void handleMetal (ShipAPI ship, float amount) {
        //First, get how much metal we currently have: if we have none, don't run
        if (Global.getCombatEngine().getCustomData().get(ship.getId()+SRD_NanobotsPlugin.EFFECT_METAL_ID) instanceof Float) {
            float metalToDispose = (float)Global.getCombatEngine().getCustomData().get(ship.getId()+SRD_NanobotsPlugin.EFFECT_METAL_ID);
            Global.getCombatEngine().getCustomData().put(ship.getId()+SRD_NanobotsPlugin.EFFECT_METAL_ID, 0f);

            //Get all our missile weapons
            List<WeaponAPI> mslWeapons = new ArrayList<>();
            for (WeaponAPI wep : ship.getAllWeapons()) {
                //Ignore non-missiles
                if (wep.getType() != WeaponAPI.WeaponType.MISSILE) {
                    continue;
                }

                //Ignore missiles with unlimited max ammo/no max ammo
                if (wep.getMaxAmmo() <= 0 || wep.getMaxAmmo() > 99999f) {
                    continue;
                }

                //Ignore missiles with maximum ammo
                if (wep.getAmmo() >= wep.getMaxAmmo()) {
                    continue;
                }

                //Otherwise, we keep the weapon
                mslWeapons.add(wep);
            }

            //If we have no weapons needing ammo, start repairing armor at 100% metal (up to a maximum), instead
            //of using 70% for ammo instead
            float metalToDisposeArmor = metalToDispose * 0.3f;
            float metalToDisposeAmmo = metalToDispose * 0.7f;
            if (mslWeapons.isEmpty()) {
                metalToDisposeArmor = metalToDispose;
                metalToDisposeAmmo = 0f;
            }

            //Armor repair; gets the size of the armor grid
            ArmorGridAPI grid = ship.getArmorGrid();
            int maxX = grid.getLeftOf() + grid.getRightOf();
            int maxY = grid.getAbove() + grid.getBelow();

            //And iterate through the entire grid
            for (int i1 = 0 ; i1 < maxX ; i1++) {
                for (int i2 = 0 ; i2 < maxY ; i2++) {
                    //If this armor grid is below the repair maximum, repair
                    if (grid.getArmorValue(i1, i2) < grid.getMaxArmorInCell()*MAX_ARMOR_REPAIR) {
                        //Calculates a new value, but ensure it's within 0f and MAX_ARMOR_REPAIRED
                        float newValue = grid.getArmorValue(i1, i2) + metalToDisposeArmor / (BASE_METAL_COST_PER_ARMOR*maxX*maxY);
                        newValue = Math.max(0f, Math.min(grid.getMaxArmorInCell()*MAX_ARMOR_REPAIR, newValue));
                        grid.setArmorValue(i1, i2, newValue);
                    }
                }
            }

            //Don't try to iterate through missile weapons if we don't have any
            if (!mslWeapons.isEmpty()) {
                for (WeaponAPI wep : mslWeapons) {
                    //Bonus partial ammo left over from previous frames
                    float previousAmmo = 0f;
                    if (leftoverAmmo.get(wep) != null) {
                        previousAmmo = leftoverAmmo.get(wep);
                    }

                    //Get the OP cost and max ammo of the weapon: these are the core factors for determining metal cost
                    //  OP is bottom-clamped to ensure it's not too beneficial to bring reapers and the likes
                    float OP = Math.max(wep.getSpec().getOrdnancePointCost(null), 5f);
                    float maxAmmoCalc = wep.getMaxAmmo();
                    if (ship.getVariant().getHullMods().contains("missleracks")) { maxAmmoCalc /= 2f; }

                    //Now, refill ammo by a fraction depending on our our metal cost
                    float ammoToAdd = (maxAmmoCalc * metalToDisposeAmmo) / (mslWeapons.size() * BASE_METAL_COST_PER_OP * OP) + previousAmmo;
                    float newAmmo = wep.getAmmo() + ammoToAdd;
                    if (newAmmo > wep.getMaxAmmo()) { newAmmo = wep.getMaxAmmo(); }
                    wep.setAmmo((int)Math.floor(newAmmo));

                    //We can reload "partial" shots in a frame, store those
                    float leftOver = newAmmo - (float)Math.floor(newAmmo);
                    leftoverAmmo.put(wep, leftOver);

                    //If we refilled at least one missile this frame, play a small sound depending on the weapon's max ammo
                    if (ammoToAdd >= 1f) {
                        float volumeMult = MathUtils.clamp((OP/10f)/(float)Math.pow(maxAmmoCalc, 0.25),0.05f, 0.75f);
                        float pitchMult = (0.9f - volumeMult) * 3f;
                        Global.getSoundPlayer().playSound(REFILL_SOUND, pitchMult, volumeMult, wep.getLocation(), ship.getVelocity());
                    }
                }
            }
        }
    }


    //Function for calculating hits against non-big ship targets
    private void makeAttackAgainstSmallTarget (CombatEntityAPI target, ShipAPI ship, float amount) {
        //We only have a chance to hit each frame
        if (Math.random() > HIT_RATE_MULT) {
            return;
        }

        //Calculates damage for the hit
        float damageToDeal = MathUtils.getRandomNumberInRange(1f-NANOBOT_DAMAGE_VARIATION, 1f+NANOBOT_DAMAGE_VARIATION)*NANOBOT_DPS*amount/(HIT_RATE_MULT);

        //If the target isn't a ship, check: if it's a non-missile projectile, reduce its damage by a portion of the damage we
        //would have dealt, but don't steal metal from it (it might have been an energy projectile!). Otherwise, just
        //deal damage and spawn a swarm
        if (!(target instanceof ShipAPI)) {
            if (target instanceof DamagingProjectileAPI && !(target instanceof MissileAPI)) {
                //Calculates how much damage to remove; this is reduced by Projectile Health, if any, to make it more fair
                float damageReduction = damageToDeal;
                if (target.getMaxHitpoints() > 1f) {
                    damageReduction /= (target.getMaxHitpoints());
                } else {
                    damageReduction /= 10f;
                }

                //And reduce projectile damage; note that we can never reduce projectiles entirely, just down to 10% of their max
                spawnSpark(damageReduction, MathUtils.getRandomPointInCircle(target.getLocation(), target.getCollisionRadius()), new Vector2f(target.getVelocity()));
                ((DamagingProjectileAPI) target).setDamageAmount(Math.max(((DamagingProjectileAPI) target).getBaseDamageAmount()*0.1f,
                        ((DamagingProjectileAPI) target).getDamageAmount()-damageReduction));
            } else {
                //Cap for how much loot we can get from a small target
                float gainCap = target.getHitpoints()*LOOT_MULT;

                //Actually deal damage
                Global.getCombatEngine().applyDamage(target, target.getLocation(), damageToDeal, DamageType.FRAGMENTATION, 0f, true,
                        false, ship, true);

                //If our cap is too low, don't even bother spawning a swarm
                if (gainCap > 20f) {
                    float angleToSpawnAt = VectorUtils.getAngle(target.getLocation(), ship.getLocation());
                    angleToSpawnAt += MathUtils.getRandomNumberInRange(-55f, 55f);
                    SRD_NanobotsPlugin.SpawnNanobotSwarm(getRandomNanobotWeapon(ship), Math.min(gainCap, damageToDeal*LOOT_MULT), angleToSpawnAt, MathUtils.getRandomNumberInRange(150f, 190f),
                            MathUtils.getRandomNumberInRange(475f, 600f), new Vector2f(target.getLocation()));
                }
            }
        } else {
            //Against fighters, we first check if they have nearly 360-shields: if not, the swarm *will* find a way
            if (target.getShield() != null && target.getShield().getType() != ShieldAPI.ShieldType.PHASE && target.getShield().isOn() && target.getShield().getActiveArc() >= 340f) {
                return;
            }

            //Alright, no shields... just deal damage and spawn swarms!
            spawnSpark(damageToDeal, MathUtils.getRandomPointInCircle(target.getLocation(), target.getCollisionRadius()), new Vector2f(target.getVelocity()));
            Global.getCombatEngine().applyDamage(target, target.getLocation(), damageToDeal, DamageType.FRAGMENTATION, 0f, true,
                    false, ship, true);

            float angleToSpawnAt = VectorUtils.getAngle(target.getLocation(), ship.getLocation());
            angleToSpawnAt += MathUtils.getRandomNumberInRange(-55f, 55f);
            SRD_NanobotsPlugin.SpawnNanobotSwarm(getRandomNanobotWeapon(ship), damageToDeal*LOOT_MULT*SHIP_LOOT_MULT, angleToSpawnAt, MathUtils.getRandomNumberInRange(190f, 240f),
                    MathUtils.getRandomNumberInRange(475f, 600f), new Vector2f(target.getLocation()));
        }
    }


    //Function for calculating hits against a proper, big ship (not a fighter)
    private void makeAttackAgainstBigTarget (ShipAPI target, ShipAPI ship, float amount) {
        //Ignore phased, collision-less and allied targets
        if (target.getOwner() == ship.getOwner() || target.isPhased() || target.getCollisionClass().equals(CollisionClass.NONE)) {
            return;
        }

        //Gets if a given ship is shielded by another ship, and cancels if so
        //Does this by going through all nearby ships
        for (ShipAPI shielder : CombatUtils.getShipsWithinRange(target.getLocation(), target.getCollisionRadius())) {
            //Ignore allies and stuff with no collision
            if (shielder.getOwner() == ship.getOwner() || shielder.isPhased() || shielder.getCollisionClass().equals(CollisionClass.NONE)) {
                continue;
            }

            //If the ship has no shield, or the shield is on, continue
            if (shielder.getShield() == null || shielder.getShield().getType() == ShieldAPI.ShieldType.PHASE || shielder.getShield().isOff()) {
                continue;
            }

            //Otherwise, check if the "shielded" ship is under the shield; we don't want more advanced checking than this,
            //it'll eat too much memory and only affect edge-cases. If it is, don't do any attacks
            if (shielder.getShield().isWithinArc(ship.getLocation())) {
                return;
            }
        }

        //Gets an angle to the target
        float angleToTarget = VectorUtils.getAngle(ship.getLocation(), target.getLocation());

        //Gets the target's shield status
        boolean hasValidShield = false;
        if (target.getShield() != null && target.getShield().getType() != ShieldAPI.ShieldType.PHASE && target.getShield().isOn()) {
            hasValidShield = true;
        }

        //Run the hit-checking several times due to our "spread" of hits
        for (float ang = -(ANGLE_HITCHECK_SIZE/2f) ; ang < (ANGLE_HITCHECK_SIZE/2f) ; ang += ANGLE_HITCHECK_STEP){
            //We only have a chance to hit each frame
            if (Math.random() > HIT_RATE_MULT) {
                continue;
            }

            //Gets a point on our arc and compare against the enemy's location
            Vector2f hitLocation = MathUtils.getPoint(ship.getLocation(), NANOBOT_AOE-NANOBOT_AOE_THICKNESS, angleToTarget+ang);

            //If this hit missed completely, try another point further away in the AOE
            if (!CollisionUtils.isPointWithinBounds(hitLocation, target)) {
                hitLocation = MathUtils.getPoint(ship.getLocation(), NANOBOT_AOE-(NANOBOT_AOE_THICKNESS*0.5f), angleToTarget+ang);
            }

            //If this hit *also* missed completely, try *another* point *even further* away in the AOE
            if (!CollisionUtils.isPointWithinBounds(hitLocation, target)) {
                hitLocation = MathUtils.getPoint(ship.getLocation(), NANOBOT_AOE, angleToTarget+ang);
            }

            //If this *still* misses completely, we stop here and don't deal damage
            if (!CollisionUtils.isPointWithinBounds(hitLocation, target)) {
                continue;
            }

            //If this hit is within the shield of our target (and our target *has* shields), don't do damage
            if (hasValidShield) {
                if (target.getShield().isWithinArc(hitLocation)) {
                    continue;
                }
            }

            //Now, *finally*, deal the damage and spawn nanobots
            float damageToDeal = MathUtils.getRandomNumberInRange(1f-NANOBOT_DAMAGE_VARIATION, 1f+NANOBOT_DAMAGE_VARIATION)*NANOBOT_DPS*BIG_DAMAGE_MULT*amount/(HIT_RATE_MULT);
            float angleToSpawnAt = VectorUtils.getAngle(hitLocation, ship.getLocation());
            angleToSpawnAt += MathUtils.getRandomNumberInRange(-55f, 55f);
            SRD_NanobotsPlugin.SpawnNanobotSwarm(getRandomNanobotWeapon(ship), damageToDeal*LOOT_MULT*SHIP_LOOT_MULT, angleToSpawnAt, MathUtils.getRandomNumberInRange(190f, 240f),
                    MathUtils.getRandomNumberInRange(475f, 600f), hitLocation);
            spawnSpark(damageToDeal, hitLocation, new Vector2f(target.getVelocity()));
            Global.getCombatEngine().applyDamage(target, hitLocation, damageToDeal, DamageType.FRAGMENTATION, 0f, true,
                    false, ship, true);
        }
    }


    //Helper function: gets a weapon on the ship to "fly towards". For now, fly to the built-in looting guns
    private WeaponAPI getRandomNanobotWeapon (ShipAPI ship) {
        //Iterates through all weapons, and in the end selects a random one with the correct ID
        List<WeaponAPI> validGuns = new ArrayList<>();
        for (WeaponAPI wep : ship.getAllWeapons()) {
            if (wep.getSpec().getWeaponId().equals("SRD_dekris")) {
                validGuns.add(wep);
            }
        }

        return (validGuns.get(MathUtils.getRandomNumberInRange(0, validGuns.size()-1)));
    }

    //Mini-function for spawning a spark. I just wanted to out-source the thing, there's no good reason for having it out here
    private void spawnSpark (float damage, Vector2f position, Vector2f targetVelocity) {
        Global.getCombatEngine().addHitParticle(position, targetVelocity, (float)Math.sqrt(damage), 1f, 0.1f, Color.ORANGE);
    }
}