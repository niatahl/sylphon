package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import data.scripts.SRD_ModPlugin;

import java.util.ArrayList;

public class SRD_PrototypeMiniatureNullspaceShunt extends BaseHullMod {
    private static final String BAD_HULLMOD_NOTIFICATION_SOUND = "cr_allied_critical";

    private static final float CONVERSION_EFFICIENCY = 0.25f;
    private static final float DISSIPATION_BONUS = 0.5f;

    //Remove this mod if we don't have any equipped Nullspace Conduits
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        //Creates a list for later use
        ArrayList<String> deletionList = new ArrayList<String>();

        //Checks if any given hullmods is our own, or a conduit
        for (String s : ship.getVariant().getHullMods()) {
            if (s.contains("SRD_prototype_minitature_nullspace_shunt")) {
                deletionList.add(s);
            }

            //If we find a conduit, we don't need the rest of the code: just stop the function
            else if (SRD_ModPlugin.NULLSPACE_CONDUIT_HULLMODS.contains(s)) {
                return;
            }
        }

        //Finally, deletes our hullmod if we didn't find any conduits
        if (deletionList.size() > 0) {
            ship.getVariant().addMod("SRD_IncompatibleHullmodWarning");
            Global.getSoundPlayer().playUISound(BAD_HULLMOD_NOTIFICATION_SOUND, 1f, 1f);
        }
        for (String s : deletionList) {
            ship.getVariant().removeMod(s);
        }
    }

    //Main conversion cycle
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        float fluxToConvert = ship.getFluxTracker().getCurrFlux() - ship.getFluxTracker().getHardFlux();

        if (fluxToConvert > (1f + DISSIPATION_BONUS) * ship.getMutableStats().getFluxDissipation().getModifiedValue() * amount) {   //This is if we have more flux then we can get rid of in a frame
            fluxToConvert = ship.getMutableStats().getFluxDissipation().getModifiedValue() * amount * DISSIPATION_BONUS;
        } else if (fluxToConvert <= ship.getMutableStats().getFluxDissipation().getModifiedValue() * amount) {                      //This is if our normal dissipation is enough to handle it
            return;
        } else {                                                                                                                    //This is if we have enough flux to get rid of it in one frame, but too much for normal dissipation
            fluxToConvert -= ship.getMutableStats().getFluxDissipation().getModifiedValue() * amount;
        }

        ship.getFluxTracker().decreaseFlux(fluxToConvert);
        ship.getFluxTracker().increaseFlux(fluxToConvert * CONVERSION_EFFICIENCY, true);
    }


    //Handles applicability
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        boolean canBeApplied = false;
        for (String s : ship.getVariant().getHullMods()) {
            if ((s.contains("SRD_prototype_") && !s.equals("SRD_prototype_miniature_nullspace_shunt"))) {
                canBeApplied = false;
                break;
            } else if (SRD_ModPlugin.NULLSPACE_CONDUIT_HULLMODS.contains(s)) {
                canBeApplied = true;
            }
        }
        return canBeApplied;
    }

    public String getUnapplicableReason(ShipAPI ship) {
        boolean hasConduit = false;
        for (String s : ship.getVariant().getHullMods()) {
            if (s.contains("SRD_prototype_") && !s.equals("SRD_prototype_miniature_nullspace_shunt")) {
                return "You can only mount a single Sylphon prototype per ship";
            } else if (SRD_ModPlugin.NULLSPACE_CONDUIT_HULLMODS.contains(s)) {
                hasConduit = true;
            }
        }
        if (!hasConduit) {
            return "The ship needs to have some form of Nullspace Conduits to mount this hullmod";
        } else {
            return "BUG: report to mod author that the file 'SRD_PrototypeMiniatureNullspaceShunt' has encountered an error";
        }
    }

    //Adds the description strings
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return "" + (int)(DISSIPATION_BONUS*100f) + "%";
        if (index == 1) return "converting soft flux into hard flux";
        if (index == 2) return "1 hard flux for every " + (int)(1/CONVERSION_EFFICIENCY) + " soft flux consumed";
        if (index == 3) return "Nullspace Conduits";
        if (index == 4) return "Sylphon Prototype";
        return null;
    }
}

