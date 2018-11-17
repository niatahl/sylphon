//By Nicke535, spawns several wings of fighters to attack a target
package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.mission.FleetSide;
import data.scripts.plugins.SRD_FakeSmokePlugin;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SRD_PurgatoryDrones extends BaseShipSystemScript {
    private final static int    WINGS_TO_DEPLOY = 6;
    private final static float  WING_SPAWN_DISTANCE_MIN = 600f;
    private final static float  WING_SPAWN_DISTANCE_MAX = 700f;
    private final static float  WING_SPAWN_ANGLE_DIFF = 45f;
    private final static float  MAX_RANGE = 6000f;

    private boolean hasDeployed = false;
    private List<ShipAPI> leadersToDespawn = new ArrayList<ShipAPI>();

    //Can't use our system if we have no valid target, or our target is out of range
    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        if (ship.getShipTarget() == null || ship.getShipTarget().getOwner() == ship.getOwner()) {
            return false;
        }
        if (MathUtils.getDistance(ship.getShipTarget(), ship.getLocation()) > MAX_RANGE) {
            return false;
        }
        //On fallthrough, use default implementation
        return super.isUsable(system, ship);
    }

    //Can't use our system if we have no valid target, or our target is out of range
    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        if (ship.getShipTarget() == null) {
            return "NO TARGET";
        }
        if (ship.getShipTarget().getOwner() == ship.getOwner()) {
            return "INVALID TARGET";
        }
        if (MathUtils.getDistance(ship.getShipTarget(), ship.getLocation()) > MAX_RANGE) {
            return "OUT OF RANGE";
        }
        //On fallthrough, use default implementation
        return super.getInfoText(system, ship);
    }

    //Main apply loop
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        //Don't run when paused
        if (Global.getCombatEngine().isPaused()) {
            return;
        }

        //Ensures we have a ship
        ShipAPI ship = null;
        boolean player = false;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            player = ship == Global.getCombatEngine().getPlayerShip();
            id = id + "_" + ship.getId();
        } else {
            return;
        }

        //If our system is on, and we haven't spawned a wing yet, spawn wings
        if (!hasDeployed && effectLevel >= 1f) {
            hasDeployed = true;

            //Dumb way to find the correct fleetside
            FleetSide rightFleetSide = FleetSide.ENEMY;
            if (Global.getCombatEngine().getFleetManager(ship.getOwner()) == Global.getCombatEngine().getFleetManager(FleetSide.PLAYER)) {
                rightFleetSide = FleetSide.PLAYER;
            }

            //Suppresses extra messages until we are done with deployment
            Global.getCombatEngine().getFleetManager(rightFleetSide).setSuppressDeploymentMessages(true);

            //Spawns several wings around the enemy target
            for (int i = 0; i < WINGS_TO_DEPLOY; i++) {
                //Gets a random location to spawn the wing
                Vector2f loc = MathUtils.getPointOnCircumference(ship.getShipTarget().getLocation(), ship.getShipTarget().getCollisionRadius() + MathUtils.getRandomNumberInRange(WING_SPAWN_DISTANCE_MIN, WING_SPAWN_DISTANCE_MAX),
                        VectorUtils.getAngle(ship.getShipTarget().getLocation(), ship.getLocation()) + MathUtils.getRandomNumberInRange(-WING_SPAWN_ANGLE_DIFF, WING_SPAWN_ANGLE_DIFF));
                //Gets the angle to the target from our location
                float facing = VectorUtils.getAngle(loc, ship.getShipTarget().getLocation());

                //Spawns the wing, sets correct variables and registers its leader in our list
                ShipAPI wingLeader = CombatUtils.spawnShipOrWingDirectly("SRD_Skallic_drone_cannon_wing", FleetMemberType.FIGHTER_WING, rightFleetSide, 1f, loc, facing);
                wingLeader.setShipTarget(ship.getShipTarget());
                if (ship.isAlly()) {
                    wingLeader.setAlly(true);
                }
                leadersToDespawn.add(wingLeader);

                //If the wing leader has the Nullspace Entry system, use that immediately
                if (wingLeader.getSystem() != null && wingLeader.getSystem().getId().contains("SRD_nullspace_entry")) {
                    wingLeader.useSystem();
                }
            }
            Global.getCombatEngine().getFleetManager(rightFleetSide).setSuppressDeploymentMessages(false);
        }

        //While the system is in its "out" state, activate the shipsystem on our drones to fake an exit
        if (state.equals(State.OUT) && effectLevel > 0f && effectLevel < 1f) {
            for (ShipAPI wingLeader : leadersToDespawn) {
                if (wingLeader.getSystem() != null && wingLeader.getSystem().getId().contains("SRD_nullspace_entry")) {
                    wingLeader.getSystem().setAmmo(1);
                    wingLeader.useSystem();
                }
            }
        }

        //If our system is off, run unapply() once
        if (effectLevel <= 0f && hasDeployed) {
            unapply(stats, id);
        }
    }

    //Fixes variables and removes fighters
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

        hasDeployed = false;

        //Fighter removal code
        List<ShipAPI> removals = new ArrayList<ShipAPI>();
        for (ShipAPI leader : leadersToDespawn) {
            removals.add(leader);
        }
        for (ShipAPI leader : removals) {
            for (int i = 0; i < 55; i++) {
                SRD_FakeSmokePlugin.addFakeSmoke(MathUtils.getRandomNumberInRange(0.1f, 0.58f), MathUtils.getRandomNumberInRange(leader.getCollisionRadius() * 0.5f, leader.getCollisionRadius() * 0.8f),
                        MathUtils.getRandomPointInCircle(leader.getLocation(), leader.getCollisionRadius() * 1.1f), MathUtils.getRandomPointInCircle(null, 7f),
                        MathUtils.getRandomNumberInRange(-9f, 9f), 1f, new Color(0f, 0f, 0f));
            }
            Global.getCombatEngine().removeEntity(leader);
        }

        leadersToDespawn.clear();
        removals.clear();
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0 && effectLevel > 0f) {
            return new StatusData("deploying nullspace drones", false);
        }
        return null;
    }
}