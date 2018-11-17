//By Nicke535, a script that absorbs damage into the shield
package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SRD_NullpointBarrier extends BaseShipSystemScript {

    public static float SHIELD_DAMAGE_REDUCTION_MULT = 0.75f;
    public static float DAMAGE_REFLECT_MULT = 1f;

    public static float DAMAGE_LIGHTNING_THRESHHOLD = 200f;
    public static float LIGHTNING_RANGE = 600f;
    public static float LIGHTNING_HALF_ARC = 20f;

    public static Color LIGHTNING_CORE_COLOR = new Color(0.37f, 0.08f, 0.60f, 0.8f);
    public static Color LIGHTNING_FRINGE_COLOR = new Color(0.20f, 0.03f, 0.30f, 0.55f);
    public static float LIGHTNING_BASE_SIZE = 7f;

    private boolean runOnce = true;
    private float hardFluxSinceLast = 0f;
    private float damageStored = 0f;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        //Reduces damage taken by shield - ALSO RUNS WHEN PAUSED TO WORK WITH AI
        stats.getShieldDamageTakenMult().modifyMult(id, 1 - (SHIELD_DAMAGE_REDUCTION_MULT * effectLevel));
        stats.getShieldUpkeepMult().modifyMult(id, 0f);

        //Don't run the rest of the code when paused
        if (Global.getCombatEngine().isPaused()) {
            return;
        }

        //Ensures we have a ship
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            id = id + "_" + ship.getId();
        } else {
            return;
        }

        //Stores our original hard flux level upon activating the system
        if (runOnce) {
            runOnce = false;
            hardFluxSinceLast = ship.getFluxTracker().getHardFlux();
        }

        //Tracks how much damage our shield has taken since the last frame
        if (ship.getFluxTracker().getHardFlux() < hardFluxSinceLast) {
            hardFluxSinceLast = ship.getFluxTracker().getHardFlux();
        } else {
            //Note: the 100 flux per second is due to the ship system's native Hard Flux Cost
            float newHardFlux = ship.getFluxTracker().getHardFlux() - hardFluxSinceLast - (100f * Global.getCombatEngine().getElapsedInLastFrame());
            damageStored += DAMAGE_REFLECT_MULT * (newHardFlux / (1 - (SHIELD_DAMAGE_REDUCTION_MULT * effectLevel))) * SHIELD_DAMAGE_REDUCTION_MULT;
            hardFluxSinceLast = ship.getFluxTracker().getHardFlux();

            //If damage is below 1, it should just be removed: it's from botched math, and not taken damage
            if (damageStored < 1f) {
                damageStored = 0f;
            }
        }

        //If we have enough damage stored up, fire a lightning-bolt
        if (damageStored > DAMAGE_LIGHTNING_THRESHHOLD) {
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
                sourcePoint.x = (float)FastTrig.cos(Math.toRadians(angleToTarget)) * ship.getShield().getRadius() + ship.getShield().getLocation().x;
                sourcePoint.y = (float)FastTrig.sin(Math.toRadians(angleToTarget)) * ship.getShield().getRadius() + ship.getShield().getLocation().y;
                Global.getCombatEngine().spawnEmpArc(ship, sourcePoint, ship, new SimpleEntity(targetPoint),
                        DamageType.ENERGY, //Damage type
                        damageStored, //Damage
                        damageStored, //Emp
                        100000f, //Max range
                        "SRD_nullspace_lightning_impact", //Impact sound
                        LIGHTNING_BASE_SIZE * (damageStored / DAMAGE_LIGHTNING_THRESHHOLD), // thickness of the lightning bolt
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
                        damageStored * (target instanceof ShipAPI ? ((ShipAPI)target).getMutableStats().getDynamic().getStat("SRD_NULLSPACE_DAMAGE_MULT").getModifiedValue() : 1f), //Damage
                        damageStored * (target instanceof ShipAPI ? ((ShipAPI)target).getMutableStats().getDynamic().getStat("SRD_NULLSPACE_DAMAGE_MULT").getModifiedValue() : 1f), //Emp
                        100000f, //Max range
                        "SRD_nullspace_lightning_impact", //Impact sound
                        LIGHTNING_BASE_SIZE * (damageStored / DAMAGE_LIGHTNING_THRESHHOLD), // thickness of the lightning bolt
                        LIGHTNING_CORE_COLOR, //Central color
                        LIGHTNING_FRINGE_COLOR //Fringe Color
                );
            }

            //And reset our damage, since we have fired it all at something
            damageStored = 0f;
        }

        //Render particles on our shield, if it's active
        if (ship.getShield().isOn()) {
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
        }
    }


    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = null;
        boolean player = false;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            player = ship == Global.getCombatEngine().getPlayerShip();
            id = id + "_" + ship.getId();
        } else {
            return;
        }

        runOnce = true;
        damageStored = 0f;
        hardFluxSinceLast = 0f;
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("" + (int)damageStored + " damage stored", false);
        }
        if (index == 1) {
            return new StatusData("absorbing incoming damage", false);
        }
        return null;
    }
}