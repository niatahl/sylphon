//By Nicke535, essentially the Nullspace Skip, but for fighters
package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.scripts.plugins.NicToyCustomTrailPlugin;
import data.scripts.plugins.SRD_FakeSmokePlugin;
import data.scripts.plugins.SRD_SpriteRenderPlugin;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class SRD_NullspaceEntry extends BaseShipSystemScript {

    public static Color AFTERIMAGE_COLOR = new Color(0.25f, 0.05f, 0.40f, 0.3f);
    public static float SHADOW_DELAY = 0.07f;
    public static float SHADOW_ANGLE_DIFFERENCE = 4f;
    public static float SHADOW_DISTANCE_DIFFERENCE = 15f;
    public static float SHADOW_FLICKER_DIFFERENCE = 4f;
    public static int SHADOW_FLICKER_CLONES = 3;

    private static final float SPEED_BONUS_MULT = 4f;
    private static final float MOBILITY_BONUS_MULT = 1f;

    private Vector2f shadowPos = new Vector2f(0f, 0f);

    private float shadowDelayCounter = 0f;
    private boolean runOnce = true;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        //Don't run when paused
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

        //If the system is turning off, start braking and spawn a puff of smoke
        if (state == State.OUT && runOnce) {
            runOnce = false;
            stats.getMaxSpeed().unmodify(id);
            stats.getMaxTurnRate().unmodify(id);
            if (ship.getAngularVelocity() > stats.getMaxTurnRate().getModifiedValue()) {
                ship.setAngularVelocity((ship.getAngularVelocity() / Math.abs(ship.getAngularVelocity())) * stats.getMaxTurnRate().getModifiedValue());
            }
            if (ship.getVelocity().length() > stats.getMaxSpeed().getModifiedValue()) {
                ship.getVelocity().set((ship.getVelocity().x / ship.getVelocity().length()) * stats.getMaxSpeed().getModifiedValue(), (ship.getVelocity().y / ship.getVelocity().length()) * stats.getMaxSpeed().getModifiedValue());
            }

            //Render a puff of smoke to hide that we just appear out of nowhere
            for (int i = 0; i < 55; i++) {
                SRD_FakeSmokePlugin.addFakeSmoke(MathUtils.getRandomNumberInRange(0.1f, 0.58f), MathUtils.getRandomNumberInRange(ship.getCollisionRadius() * 0.5f, ship.getCollisionRadius() * 0.8f),
                        MathUtils.getRandomPointInCircle(ship.getLocation(), ship.getCollisionRadius() * 1.1f), MathUtils.getRandomPointInCircle(null, 7f),
                        MathUtils.getRandomNumberInRange(-9f, 9f), 1f, new Color(0f, 0f, 0f));
            }
            return;
        } else if (state == State.OUT) {
            ship.setExtraAlphaMult(1f - effectLevel);
            return;
        }

        //Phases us and drastically increases mobility
        //(also cuts our trails, since we are technically entering another dimension)
        NicToyCustomTrailPlugin.cutTrailsOnEntity(ship);
        ship.setPhased(true);
        ship.setExtraAlphaMult(0f);
        float speedBonus = SPEED_BONUS_MULT;
        float mobilityBonus = MOBILITY_BONUS_MULT;
        stats.getMaxSpeed().modifyMult(id, speedBonus);
        stats.getAcceleration().modifyMult(id, mobilityBonus);
        stats.getDeceleration().modifyMult(id, mobilityBonus);
        stats.getMaxTurnRate().modifyMult(id, (float)Math.sqrt(speedBonus));
        stats.getTurnAcceleration().modifyMult(id, mobilityBonus);

        //Moves the shadow to its appropriate location
        shadowPos = MathUtils.getRandomPointInCircle(null, SHADOW_DISTANCE_DIFFERENCE);
        shadowPos.x += ship.getLocation().x;
        shadowPos.y += ship.getLocation().y;

        //If we are outside the screenspace, don't do the visual effects
        if (!Global.getCombatEngine().getViewport().isNearViewport(shadowPos, ship.getCollisionRadius() * 1.5f)) {
            return;
        }

        //If enough time has passed, render a new shadow
        shadowDelayCounter += Global.getCombatEngine().getElapsedInLastFrame();
        if (shadowDelayCounter > SHADOW_DELAY) {
            float angleDifference = MathUtils.getRandomNumberInRange(-SHADOW_ANGLE_DIFFERENCE, SHADOW_ANGLE_DIFFERENCE) - 90f;

            for (int i = 0; i < SHADOW_FLICKER_CLONES; i++) {
                Vector2f modifiedShadowPos = new Vector2f(MathUtils.getRandomNumberInRange(-SHADOW_FLICKER_DIFFERENCE, SHADOW_FLICKER_DIFFERENCE), MathUtils.getRandomNumberInRange(-SHADOW_FLICKER_DIFFERENCE, SHADOW_FLICKER_DIFFERENCE));
                modifiedShadowPos.x += shadowPos.x;
                modifiedShadowPos.y += shadowPos.y;
                SRD_SpriteRenderPlugin.battlespaceRender(Global.getSettings().getSprite("SRD_fx", "" + ship.getHullSpec().getBaseHullId() + "_phantom"), modifiedShadowPos, new Vector2f(0f, 0f),
                        new Vector2f(ship.getSpriteAPI().getWidth(), ship.getSpriteAPI().getHeight()),
                        new Vector2f(0f, 0f), ship.getFacing() + angleDifference,
                        0f, AFTERIMAGE_COLOR, true, 0.1f, 0f, 0.3f);
            }

            shadowDelayCounter -= SHADOW_DELAY;
        }

        //Always render smoke at the shadow's position...
        for (int i = 0; i < 200 * Global.getCombatEngine().getElapsedInLastFrame(); i++) {
            SRD_FakeSmokePlugin.addFakeSmoke(MathUtils.getRandomNumberInRange(0.29f, 0.52f), MathUtils.getRandomNumberInRange(ship.getCollisionRadius() * 0.4f, ship.getCollisionRadius() * 0.7f),
                    MathUtils.getRandomPointInCircle(ship.getLocation(), ship.getCollisionRadius()), MathUtils.getRandomPointInCircle(null, 7f),
                    MathUtils.getRandomNumberInRange(-9f, 9f), 1f, new Color(0f, 0f, 0f));
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

        if (ship.getSystem().getEffectLevel() <= 0f) {
            runOnce = true;
            shadowPos = new Vector2f(0f, 0f);
            shadowDelayCounter = 0f;
            ship.setPhased(false);
            ship.setExtraAlphaMult(1f);
            stats.getMaxSpeed().unmodify(id);
            stats.getAcceleration().unmodify(id);
            stats.getDeceleration().unmodify(id);
            stats.getMaxTurnRate().unmodify(id);
            stats.getTurnAcceleration().unmodify(id);

            if (Math.abs(ship.getAngularVelocity()) > stats.getMaxTurnRate().getModifiedValue()) {
                ship.setAngularVelocity((ship.getAngularVelocity() / Math.abs(ship.getAngularVelocity())) * stats.getMaxTurnRate().getModifiedValue());
            }
            if (ship.getVelocity().length() > stats.getMaxSpeed().getModifiedValue()) {
                ship.getVelocity().set((ship.getVelocity().x / ship.getVelocity().length()) * stats.getMaxSpeed().getModifiedValue(), (ship.getVelocity().y / ship.getVelocity().length()) * stats.getMaxSpeed().getModifiedValue());
            }
        }
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        return null;
    }
}