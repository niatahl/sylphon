package data.scripts.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;

public class SRD_AmbushSpec extends BaseHullMod {

    public static final float VENT_BONUS = 50f;

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.getMutableStats().getVentRateMult().modifyPercent(id,VENT_BONUS);
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "" + (int)VENT_BONUS + "%";
        return null;
    }

}
