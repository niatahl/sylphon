package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import data.scripts.SRD_ModPlugin;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SRD_PrototypeNullpointShield extends BaseHullMod {
    private static final String BAD_HULLMOD_NOTIFICATION_SOUND = "cr_allied_critical";

    public static float SHIELD_DAMAGE_REDUCTION_MULT = 0.5f;
    public static float DAMAGE_REFLECT_MULT = 1f;

    public static float DAMAGE_LIGHTNING_THRESHHOLD = 200f;
    public static float LIGHTNING_RANGE = 700f;
    public static float LIGHTNING_HALF_ARC = 20f;

    public static Color LIGHTNING_CORE_COLOR = new Color(0.37f, 0.08f, 0.60f, 0.8f);
    public static Color LIGHTNING_FRINGE_COLOR = new Color(0.20f, 0.03f, 0.30f, 0.55f);
    public static float LIGHTNING_BASE_SIZE = 6f;

    public static float UPKEEP_INCREASE = 0.75f;
    public static float ARC_MULT_FRONT = 0.5f;

    private Map<ShipAPI, Float> hardFluxSinceLast = new HashMap<ShipAPI, Float>();
    private Map<ShipAPI, Float> damageStored = new HashMap<ShipAPI, Float>();

    //Any effects applied before the ship is generated, so in this case change shield damage taken and shield upkeep
    //  (it should be noted that these are removed while the ship's shipsystem is active, by running the inverted multiplier on-top)
    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getShieldDamageTakenMult().modifyMult(id, 1f - SHIELD_DAMAGE_REDUCTION_MULT);
        stats.getShieldUpkeepMult().modifyMult(id, 1f + UPKEEP_INCREASE);
    }

    //Remove this mod if we don't have any equipped Nullspace Conduits, and lock the shield in a forward position
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        //Creates a list for later use
        ArrayList<String> deletionList = new ArrayList<String>();

        //Checks if any given hullmods is our own, or a conduit
        boolean shouldRemoveSelf = true;
        for (String s : ship.getVariant().getHullMods()) {
            //Remove shield hullmods
            if (SRD_ModPlugin.SHIELD_HULLMODS.contains(s) && !(s.contains("SRD_prototype_nullpoint_shield"))) {
                deletionList.add(s);
            }

            //If we find a conduit, we don't need to remove ourselves
            else if (SRD_ModPlugin.NULLSPACE_CONDUIT_HULLMODS.contains(s)) {
                shouldRemoveSelf = false;
            }
        }

        //Delete ourselves, if we need to
        if (shouldRemoveSelf) {
            ship.getVariant().addMod("SRD_IncompatibleHullmodWarning");
            ship.getVariant().removeMod("SRD_prototype_nullpoint_shield");
            Global.getSoundPlayer().playUISound(BAD_HULLMOD_NOTIFICATION_SOUND, 1f, 1f);
            return;
        }
        //Finally, deletes hullmods we should get rid of
        if (deletionList.size() > 0) {
            ship.getVariant().addMod("SRD_IncompatibleHullmodWarning");
            Global.getSoundPlayer().playUISound(BAD_HULLMOD_NOTIFICATION_SOUND, 1f, 1f);
        }
        for (String s : deletionList) {
            ship.getVariant().removeMod(s);
        }

        //Changes shield type to Frontal, if it isn't already. If it is already frontal, we decrease its arc
        if (ship.getShield() != null && ship.getShield().getType() != ShieldAPI.ShieldType.PHASE && ship.getShield().getType() != ShieldAPI.ShieldType.NONE) {
            if (ship.getShield().getType() == ShieldAPI.ShieldType.OMNI) {
                ship.getShield().setType(ShieldAPI.ShieldType.FRONT);
            } else if (ship.getShield().getType() == ShieldAPI.ShieldType.FRONT) {
                ship.getShield().setArc(ship.getShield().getArc() * ARC_MULT_FRONT);
            }
        }
    }

    //Main handling cycle
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        //Sanity check
        if (ship.getShield() == null || ship.getShield().getType() == ShieldAPI.ShieldType.PHASE || ship.getShield().getType() == ShieldAPI.ShieldType.NONE) {
            return;
        }
        //Don't run the rest of the code when paused
        if (Global.getCombatEngine().isPaused()) {
            return;
        }
        //Removes potential "residual" stat changes
        ship.getMutableStats().getShieldDamageTakenMult().unmodify("SRD_PrototypeNullpointShieldStatRemover");
        ship.getMutableStats().getShieldUpkeepMult().unmodify("SRD_PrototypeNullpointShieldStatRemover");

        //Also don't run if our ship is using its ship system, or if our shield is off
        if (ship.getSystem() != null) {
            if (ship.getSystem().getEffectLevel() > 0) {
                damageStored.put(ship,0f);
                hardFluxSinceLast.put(ship,ship.getFluxTracker().getHardFlux());
                //Since the ship's shipsystem is on, we remove its modified shield upkeep and damage taken
                ship.getMutableStats().getShieldDamageTakenMult().modifyMult("SRD_PrototypeNullpointShieldStatRemover", 1f / (1f - SHIELD_DAMAGE_REDUCTION_MULT));
                ship.getMutableStats().getShieldUpkeepMult().modifyMult("SRD_PrototypeNullpointShieldStatRemover", 1f / (1f + UPKEEP_INCREASE));
                return;
            }
        }
        if (ship.getShield().isOff() || ship.getShield().getActiveArc() <= 0f) {
            damageStored.put(ship,0f);
            hardFluxSinceLast.put(ship,ship.getFluxTracker().getHardFlux());
            return;
        }

        //Instantiates maps, if necessary
        if (hardFluxSinceLast.get(ship) == null) {
            hardFluxSinceLast.put(ship, 0f);
        }
        if (damageStored.get(ship) == null) {
            damageStored.put(ship, 0f);
        }

        //Tracks how much damage our shield has taken since the last frame
        if (ship.getFluxTracker().getHardFlux() < hardFluxSinceLast.get(ship)) {
            hardFluxSinceLast.put(ship,ship.getFluxTracker().getHardFlux());
        } else {
            //Note: the 100 flux per second is due to the ship system's native Hard Flux Cost
            float newHardFlux = ship.getFluxTracker().getHardFlux() - hardFluxSinceLast.get(ship);
            damageStored.put(ship, damageStored.get(ship) + DAMAGE_REFLECT_MULT * (newHardFlux / (1 - (SHIELD_DAMAGE_REDUCTION_MULT))) * SHIELD_DAMAGE_REDUCTION_MULT);
            hardFluxSinceLast.put(ship, ship.getFluxTracker().getHardFlux());

            //If damage is below 1, it should just be removed: it's from botched math, and not taken damage
            if (damageStored.get(ship) < 1f) {
                damageStored.put(ship, 0f);
            }
        }

        //If we have enough damage stored up, fire a lightning-bolt
        if (damageStored.get(ship) > DAMAGE_LIGHTNING_THRESHHOLD) {
            List<CombatEntityAPI> validTargets = new ArrayList<CombatEntityAPI>();
            for (CombatEntityAPI potentialTarget : CombatUtils.getEntitiesWithinRange(ship.getShield().getLocation(), LIGHTNING_RANGE + ship.getShield().getRadius())) {
                if (potentialTarget instanceof MissileAPI || potentialTarget instanceof ShipAPI) {
                    //Ignore our own ship and anything within our shield radius
                    if (potentialTarget == ship || MathUtils.getDistance(potentialTarget.getLocation(), ship.getShield().getLocation()) < ship.getShield().getRadius()) {
                        continue;
                    }

                    //Phased targets, and targets with no collision, are ignored
                    if (potentialTarget instanceof ShipAPI) {
                        if (((ShipAPI)potentialTarget).isPhased()) {
                            continue;
                        }
                    }
                    if (potentialTarget.getCollisionClass().equals(CollisionClass.NONE)) {
                        continue;
                    }

                    //Checks whether the target is in the correct arc
                    float angleToTarget = VectorUtils.getAngle(ship.getShield().getLocation(), potentialTarget.getLocation());
                    if (angleToTarget <= ship.getFacing() + LIGHTNING_HALF_ARC && angleToTarget >= ship.getFacing() - LIGHTNING_HALF_ARC) {
                        validTargets.add(potentialTarget);
                    }
                }
            }

            //If there are no valid targets in range, fire lightning randomly in the area
            if (validTargets.isEmpty()) {
                //Finds a random point
                Vector2f targetPoint = MathUtils.getRandomPointInCone(ship.getShield().getLocation(), LIGHTNING_RANGE + ship.getShield().getRadius(),ship.getFacing() - LIGHTNING_HALF_ARC, ship.getFacing() + LIGHTNING_HALF_ARC);

                //Gets a point on our shield directly pointing to the target
                float angleToTarget = VectorUtils.getAngle(ship.getShield().getLocation(), targetPoint);
                Vector2f sourcePoint = new Vector2f(0f, 0f);
                sourcePoint.x = (float) FastTrig.cos(Math.toRadians(angleToTarget)) * ship.getShield().getRadius() + ship.getShield().getLocation().x;
                sourcePoint.y = (float)FastTrig.sin(Math.toRadians(angleToTarget)) * ship.getShield().getRadius() + ship.getShield().getLocation().y;
                Global.getCombatEngine().spawnEmpArc(ship, sourcePoint, ship, new SimpleEntity(targetPoint),
                        DamageType.ENERGY, //Damage type
                        damageStored.get(ship), //Damage
                        damageStored.get(ship), //Emp
                        100000f, //Max range
                        "SRD_nullspace_lightning_impact", //Impact sound
                        LIGHTNING_BASE_SIZE * (damageStored.get(ship) / DAMAGE_LIGHTNING_THRESHHOLD), // thickness of the lightning bolt
                        LIGHTNING_CORE_COLOR, //Central color
                        LIGHTNING_FRINGE_COLOR //Fringe Color
                );
            } else {
                //Otherwise, we fire at a the closest valid target
                CombatEntityAPI target = validTargets.get(0);
                for (CombatEntityAPI potentialTarget : validTargets) {
                    if (MathUtils.getDistance(potentialTarget, ship.getShield().getLocation()) < MathUtils.getDistance(target, ship.getShield().getLocation())) {
                        target = potentialTarget;
                    }
                }

                //Gets a point on our shield directly pointing to the target
                float angleToTarget = VectorUtils.getAngle(ship.getShield().getLocation(), target.getLocation());
                Vector2f sourcePoint = new Vector2f(0f, 0f);
                sourcePoint.x = (float)FastTrig.cos(Math.toRadians(angleToTarget)) * ship.getShield().getRadius() + ship.getShield().getLocation().x;
                sourcePoint.y = (float)FastTrig.sin(Math.toRadians(angleToTarget)) * ship.getShield().getRadius() + ship.getShield().getLocation().y;
                Global.getCombatEngine().spawnEmpArc(ship, sourcePoint, ship, target,
                        DamageType.ENERGY, //Damage type
                        damageStored.get(ship) * (target instanceof ShipAPI ? ((ShipAPI)target).getMutableStats().getDynamic().getStat("SRD_NULLSPACE_DAMAGE_MULT").getModifiedValue() : 1f), //Damage
                        damageStored.get(ship) * (target instanceof ShipAPI ? ((ShipAPI)target).getMutableStats().getDynamic().getStat("SRD_NULLSPACE_DAMAGE_MULT").getModifiedValue() : 1f), //Emp
                        100000f, //Max range
                        "SRD_nullspace_lightning_impact", //Impact sound
                        LIGHTNING_BASE_SIZE * (damageStored.get(ship) / DAMAGE_LIGHTNING_THRESHHOLD), // thickness of the lightning bolt
                        LIGHTNING_CORE_COLOR, //Central color
                        LIGHTNING_FRINGE_COLOR //Fringe Color
                );
            }

            //And reset our damage, since we have fired it all at something
            damageStored.put(ship,0f);
        }

        //Render particles on our shield
        for (int i = 0; i < (400 * Global.getCombatEngine().getElapsedInLastFrame()); i++) {
            Vector2f targetPoint = MathUtils.getPointOnCircumference(ship.getShield().getLocation(), ship.getShield().getRadius(),MathUtils.getRandomNumberInRange(ship.getShield().getFacing() - (ship.getShield().getActiveArc() / 2f), ship.getShield().getFacing() + (ship.getShield().getActiveArc() / 2f)));

            //Only draw particles on-screen
            if (!Global.getCombatEngine().getViewport().isNearViewport(targetPoint, 100f)) {
                continue;
            }
            Global.getCombatEngine().addSmoothParticle(targetPoint,
                    Vector2f.add(MathUtils.getRandomPointInCircle(null, 15f), ship.getVelocity(), null), MathUtils.getRandomNumberInRange(14f, 37f), 0.3f,
                    MathUtils.getRandomNumberInRange(0.22f, 0.47f), LIGHTNING_FRINGE_COLOR);
        }

        //Maintain a message with how much absorbed damage we have, if we are the player ship
        if (Global.getCombatEngine().getPlayerShip() == ship) {
            Global.getCombatEngine().maintainStatusForPlayerShip("SRD_PrototypeNullpointShieldStatus", "graphics/sylphon/icons/hullsys/SRD_nullpoint_barrier.png", "Nullpoint Shield", "" + (int)Math.round(damageStored.get(ship)) + " damage stored", false);
        }
    }


    //Handles applicability
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        //Standard Prototype applicability...
        boolean canBeApplied = false;
        for (String s : ship.getVariant().getHullMods()) {
            if (s.contains("SRD_prototype_") && !s.equals("SRD_prototype_nullpoint_shield")) {
                canBeApplied = false;
                break;
            } else if (SRD_ModPlugin.NULLSPACE_CONDUIT_HULLMODS.contains(s)) {
                canBeApplied = true;
            }

            //Special for the Nullpoint Barrier: we need to have no shield-related hullmods
            if (SRD_ModPlugin.SHIELD_HULLMODS.contains(s) && !s.equals("SRD_prototype_nullpoint_shield")) {
                canBeApplied = false;
                break;
            }
        }

        //Special for Nullpoint Barrier: we need to have a shield
        if (ship.getShield() == null || ship.getShield().getType().equals(ShieldAPI.ShieldType.NONE) || ship.getShield().getType().equals(ShieldAPI.ShieldType.PHASE)) {
            canBeApplied = false;
        }

        return canBeApplied;
    }

    public String getUnapplicableReason(ShipAPI ship) {
        boolean hasConduit = false;
        for (String s : ship.getVariant().getHullMods()) {
            if (s.contains("SRD_prototype_") && !s.equals("SRD_prototype_nullpoint_shield")) {
                return "You can only mount a single Sylphon prototype per ship";
            } else if (SRD_ModPlugin.NULLSPACE_CONDUIT_HULLMODS.contains(s)) {
                hasConduit = true;
            }

            if (SRD_ModPlugin.SHIELD_HULLMODS.contains(s) && !s.equals("SRD_prototype_nullpoint_shield")) {
                return "Incompatible with " + Global.getSettings().getHullModSpec(s).getDisplayName();
            }
        }
        if (!hasConduit) {
            return "The ship needs to have some form of Nullspace Conduits to mount this hullmod";
        } else if (ship.getShield() == null || ship.getShield().getType().equals(ShieldAPI.ShieldType.NONE) || ship.getShield().getType().equals(ShieldAPI.ShieldType.PHASE)) {
            return "The ship needs to have shields to mount this hullmod";
        } else {
            return "BUG: report to mod author that the file 'SRD_PrototypeNullpointShield' has encountered an error";
        }
    }

    //Adds the description strings
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return "" + (int)(SHIELD_DAMAGE_REDUCTION_MULT * 100f) + "%";
        if (index == 1) return "absorbed";
        if (index == 2) return "is lost while the ship's shipsystem is active";
        if (index == 3) return "locked in a forward position";
        if (index == 4) return "" + (int)((1f - ARC_MULT_FRONT) * 100f) + "%";
        if (index == 5) return "" + (int)(UPKEEP_INCREASE * 100f) + "%";
        if (index == 6) return "Nullspace Conduits";
        if (index == 7) return "Sylphon Prototype";
        return null;
    }
}

