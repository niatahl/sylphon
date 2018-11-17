package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import data.scripts.plugins.SRD_SpriteRenderPlugin;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class SRD_NullspaceShunt extends BaseShipSystemScript {

    private static float MAX_COST_REDUCTION = 0.5f;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        //Don't run when paused
        if (Global.getCombatEngine().isPaused()) {
            return;
        }

        //Reduce weapon flux costs
        stats.getEnergyWeaponFluxCostMod().modifyMult(id,1f - (MAX_COST_REDUCTION * effectLevel));
        stats.getBallisticWeaponFluxCostMod().modifyMult(id,1f - (MAX_COST_REDUCTION * effectLevel));
        stats.getMissileWeaponFluxCostMod().modifyMult(id,1f - (MAX_COST_REDUCTION * effectLevel));
    }


    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getEnergyWeaponFluxCostMod().unmodify(id);
        stats.getBallisticWeaponFluxCostMod().unmodify(id);
        stats.getMissileWeaponFluxCostMod().unmodify(id);
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("" + (int)(MAX_COST_REDUCTION * effectLevel * 100f) + "% reduced weapon flux cost", false);
        }
        return null;
    }
}