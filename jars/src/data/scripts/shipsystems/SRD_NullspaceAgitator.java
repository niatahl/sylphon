//By Nicke535, a script that eats all ship flux and spits it back at you as hard flux once the system is turned off
package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.scripts.plugins.SRD_SpriteRenderPlugin;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SRD_NullspaceAgitator extends BaseShipSystemScript {

    private static final float EFFECT_RANGE = 4500f;
    private static final float MOBILITY_REDUCTION = 0.2f;
    private static final float SPEED_REDUCTION = 0.1f;

    private static final Color PULSE_COLOR = new Color(0.45f, 0.10f, 0.80f, 0.95f);
    private static final Vector2f PULSE_STARTSIZE = new Vector2f(100f, 100f);
    private static final Vector2f PULSE_GROWTH = new Vector2f(5500f, 5500f);

    private List<ShipAPI> targetList = new ArrayList<ShipAPI>();

    private boolean runOnce = true;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        //Don't run when paused
        if (Global.getCombatEngine().isPaused()) {
            return;
        }

        ShipAPI ship = null;
        boolean player = false;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            player = ship == Global.getCombatEngine().getPlayerShip();
            id = id + "_" + ship.getId();
        } else {
            return;
        }

        if (runOnce) {
            runOnce = false;
            SRD_SpriteRenderPlugin.objectspaceRender(Global.getSettings().getSprite("SRD_fx", "seresvalla_wave"), ship, new Vector2f(0f, 0f),
                    new Vector2f(0f, 0f), PULSE_STARTSIZE, PULSE_GROWTH, 180f, 0f, true, PULSE_COLOR, true, 0f, 0.2f, 0.9f, true);
            for (ShipAPI testShip : CombatUtils.getShipsWithinRange(ship.getLocation(), EFFECT_RANGE)) {
                if (!testShip.getVariant().getHullMods().contains("SRD_nullspace_conduits")) {
                    targetList.add(testShip);
                }
            }
        }

        for (ShipAPI target : targetList) {
            if (target != null) {
                target.getMutableStats().getMaxTurnRate().modifyMult(id, 1f - (MOBILITY_REDUCTION * effectLevel));
                target.getMutableStats().getTurnAcceleration().modifyMult(id, 1f - (MOBILITY_REDUCTION * effectLevel));
                target.getMutableStats().getTurnAcceleration().modifyMult(id, 1f - (MOBILITY_REDUCTION * effectLevel));
                target.getMutableStats().getMaxSpeed().modifyMult(id, 1f - (SPEED_REDUCTION * effectLevel));
                target.getMutableStats().getAcceleration().modifyMult(id, 1f - (MOBILITY_REDUCTION * effectLevel));
                target.getMutableStats().getDeceleration().modifyMult(id, 1f - (MOBILITY_REDUCTION * effectLevel));

                if (target == Global.getCombatEngine().getPlayerShip()) {
                    String textToDisplay = "something is disrupting your thrusters!";
                    Global.getCombatEngine().maintainStatusForPlayerShip(id, "graphics/icons/hullsys/SRD_nullspace_agitator.png",
                            "Nullspace Anomaly", textToDisplay, true);
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

        runOnce = true;
        for (ShipAPI target : targetList) {
            if (target != null) {
                target.getMutableStats().getMaxTurnRate().unmodify(id);
                target.getMutableStats().getTurnAcceleration().unmodify(id);
                target.getMutableStats().getTurnAcceleration().unmodify(id);
                target.getMutableStats().getMaxSpeed().unmodify(id);
                target.getMutableStats().getAcceleration().unmodify(id);
                target.getMutableStats().getDeceleration().unmodify(id);
            }
        }
        targetList.clear();
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("agitation pulse deployed", false);
        }
        return null;
    }
}