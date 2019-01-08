package data.scripts.hullmods;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import data.scripts.SRD_ModPlugin;

public class SRD_PrototypeLocalStabilizer extends BaseHullMod {

    private static int BURN_LEVEL_BONUS = 1;

    //Any effects applied before the ship is generated, so in this case change shield damage taken
    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getMaxBurnLevel().modifyFlat(id, BURN_LEVEL_BONUS);
    }

    //Remove this mod if we don't have any equipped Nullspace Conduits, and lock the shield in a forward position
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        //Checks if any given hullmods is a conduit
        boolean shouldRemoveSelf = true;
        for (String s : ship.getVariant().getHullMods()) {
            //If we find a conduit, we don't need to remove ourselves (note that we don't count a Stabilizer as a conduit, since we can't be put on a ship with one of those)
            if (SRD_ModPlugin.NULLSPACE_CONDUIT_HULLMODS.contains(s) && s != "SRD_nullspace_stabilizer") {
                shouldRemoveSelf = false;
                break;
            }
        }

        //Delete ourselves, if we need to
        if (shouldRemoveSelf) {
            ship.getVariant().addMod("SRD_IncompatibleHullmodWarning");
            ship.getVariant().removeMod("SRD_prototype_local_stabilizer");
        }
    }

    //Handles applicability
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        //Standard Prototype applicability...
        boolean canBeApplied = true;
        for (String s : ship.getVariant().getHullMods()) {
            if (s.contains("SRD_prototype_") && !s.equals("SRD_prototype_local_stabilizer")) {
                canBeApplied = false;
                break;
            } if (SRD_ModPlugin.NULLSPACE_CONDUIT_HULLMODS.contains(s) && s != "SRD_nullspace_stabilizer") {
                canBeApplied = true;
            }
        }

        //Special for Local Stabilizer: we can't have a proper stabilizer
        if (ship.getVariant().getHullMods().contains("SRD_nullspace_stabilizer")) {
            canBeApplied = false;
        }

        return canBeApplied;
    }

    public String getUnapplicableReason(ShipAPI ship) {
        boolean hasConduit = false;
        for (String s : ship.getVariant().getHullMods()) {
            if (s.contains("SRD_prototype_") && !s.equals("SRD_prototype_local_stabilizer")) {
                return "You can only mount a single Sylphon prototype per ship";
            } else if (SRD_ModPlugin.NULLSPACE_CONDUIT_HULLMODS.contains(s)) {
                hasConduit = true;
            }
        }

        if (ship.getVariant().getHullMods().contains("SRD_nullspace_stabilizer")) {
            return "The ship already has a proper Nullspace Soother installed!";
        } else if (!hasConduit) {
            return "The ship needs to have some form of Nullspace Conduits to mount this hullmod";
        } else {
            return "BUG: report to mod author that the file 'SRD_PrototypeLocalStabilizer' has encountered an error";
        }
    }

    //Adds the description strings
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return "Nullspace Conduits";
        if (index == 1) return "Nullspace Soother";
        if (index == 2) return "" + (int)((SRD_NullspaceStabilizer.MOBILITY_MULT-1f)*100f) + "%";
        if (index == 3) return "" + (int)((SRD_NullspaceStabilizer.SPEED_BONUS_MULT-1f)*100f) + "%";
        if (index == 4) return "" + BURN_LEVEL_BONUS;
        if (index == 5) return "Nullspace Soother";
        if (index == 6) return "Nullspace Conduits";
        if (index == 7) return "Sylphon Prototype";
        return null;
    }
}

