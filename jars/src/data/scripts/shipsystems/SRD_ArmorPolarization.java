package data.scripts.shipsystems;

import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.scripts.plugins.NicToyCustomTrailPlugin;
import org.lazywizard.lazylib.combat.CombatUtils;

public class SRD_ArmorPolarization extends BaseShipSystemScript {

    public static final float INCOMING_DAMAGE_MULT = 0.1f;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        effectLevel = 1f;
        stats.getHullDamageTakenMult().modifyMult(id, 1f - (1f - INCOMING_DAMAGE_MULT) * effectLevel);
        stats.getArmorDamageTakenMult().modifyMult(id, 1f - (1f - INCOMING_DAMAGE_MULT) * effectLevel);
        stats.getEmpDamageTakenMult().modifyMult(id, 1f - effectLevel);
    }
    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getHullDamageTakenMult().unmodify(id);
        stats.getArmorDamageTakenMult().unmodify(id);
        stats.getEmpDamageTakenMult().unmodify(id);
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        effectLevel = 1f;
        float percent = (1f - INCOMING_DAMAGE_MULT) * effectLevel * 100;
        if (index == 0) {
            return new StatusData((int) percent + "% less damage taken", false);
        } else if (index == 1) {
            return new StatusData("Immune to EMP", false);
        }
        return null;
    }
}
