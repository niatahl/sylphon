//By Nicke535
//A "fake" hullmod used to give fighter bays to a ship from other hullmods, when said hullmods are incapable of modifying that stat due to call order.
//Be careful with this thing...
package data.scripts.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public class SRD_FAKEMOD_HiddenExtraCarrierBays2 extends BaseHullMod {
    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getNumFighterBays().modifyFlat(id, 2);
    }

    //Always applicable
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return true;
    }

    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        return null;
    }
}

