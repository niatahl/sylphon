//By Nicke535, turns on and off the ship's main guns while disabling mobility.
package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class SRD_NullspaceAnchor extends BaseShipSystemScript {

    private final float TURN_RATE_MULT = 0.01f;

    //We are not allowed to turn the system off if our main cannon is firing
    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        //If we are already active, get all weapons and check if any of them are stopping us from activating the system
        if (system.isActive()) {
            for (WeaponAPI weapon : ship.getAllWeapons()) {
                if (weapon.getSpec().hasTag("SRD_REQUIRES_ANCHOR") && weapon.isFiring()) {
                    return false;
                }
            }
        }

        //If we didn't find any weapons preventing us from turning off, just go with the default implementation
        return super.isUsable(system, ship);
    }

    //Also indicate if we are prohibited from turning the system off
    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        //If we are already active, get all weapons and check if any of them are stopping us from activating the system
        if (system.isActive()) {
            for (WeaponAPI weapon : ship.getAllWeapons()) {
                if (weapon.getSpec().hasTag("SRD_REQUIRES_ANCHOR") && weapon.isFiring()) {
                    return "LOCKED";
                }
            }
        }

        if (system.isChargedown()) {
            return "DISENGAGING";
        } else if (system.isChargeup()) {
            return "ENGAGING";
        }

        //If we didn't find any weapons preventing us from turning off, and we aren't charging up/down, just go with the default implementation
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

        //If the system is on, we activate our main weapons and stop practically all movement
        if (effectLevel > 0) {
            stats.getTurnAcceleration().modifyMult(id, 1f - effectLevel * (1f - (float)Math.sqrt(TURN_RATE_MULT)));
            stats.getMaxTurnRate().modifyMult(id, 1f - effectLevel * (1f - TURN_RATE_MULT));
            stats.getMaxSpeed().modifyMult(id, 1f - effectLevel);
            if (ship.getVelocity().length() > stats.getMaxSpeed().getModifiedValue()) {
                ship.getVelocity().normalise();
                ship.getVelocity().scale(stats.getMaxSpeed().getModifiedValue());
            }
            if (Math.abs(ship.getAngularVelocity()) > stats.getMaxTurnRate().getModifiedValue()) {
                ship.setAngularVelocity((ship.getAngularVelocity() / Math.abs(ship.getAngularVelocity())) * stats.getMaxTurnRate().getModifiedValue());
            }



            //Finds all weapons that needs the system to be active, and activates them (sets their ammo to 1) if our system is fully charged
            if (effectLevel >= 1f) {
                for (WeaponAPI weapon : ship.getAllWeapons()) {
                    if (weapon.getSpec().hasTag("SRD_REQUIRES_ANCHOR")) {
                        weapon.setAmmo(1);
                    }
                }
            } else {
                for (WeaponAPI weapon : ship.getAllWeapons()) {
                    if (weapon.getSpec().hasTag("SRD_REQUIRES_ANCHOR")) {
                        weapon.setAmmo(0);
                    }
                }
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

        //If the system is off, we disable our main weapon and resume movement
        stats.getTurnAcceleration().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);
        stats.getMaxSpeed().unmodify(id);

        //Finds all weapons that needs the system to be active, and deactivates them (sets their ammo to 0)
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (weapon.getSpec().hasTag("SRD_REQUIRES_ANCHOR")) {
                weapon.setAmmo(0);
            }
        }
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0 && effectLevel >= 1f) {
            return new StatusData("main weapon systems online", false);
        }
        if (index == 1) {
            return new StatusData("locked in place", true);
        }
        return null;
    }
}