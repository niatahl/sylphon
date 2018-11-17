//By Nicke535, similar to the Nullspace Skip, but applies to the ship's fighters instead and doesn't phase them.
//Since most fighters don't have a glowmap (they don't technically have Nullspace Conduits) we use a more generic glow sprite
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

public class SRD_NullfieldCatalyst extends BaseShipSystemScript {

    public static Color AFTERIMAGE_COLOR = new Color(0.25f, 0.05f, 0.40f, 0.5f);
    public static float SHADOW_DELAY = 0.09f;
    public static float SHADOW_ANGLE_DIFFERENCE = 6f;
    public static float SHADOW_DISTANCE_DIFFERENCE_MULT = 0.7f;  //As multiplier for ship collision radius
    public static float SHADOW_FLICKER_DIFFERENCE = 3f;
    public static int SHADOW_FLICKER_CLONES = 3;

    private static final float SPEED_BONUS_MULT = 2f;
    private static final float MOBILITY_BONUS_MULT = 10f;

    private int lastMessage = 0;
    private float shadowDelayCounter = 0f;
    private boolean runOnce = true;

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

        //Time tracking
        shadowDelayCounter += Global.getCombatEngine().getElapsedInLastFrame();
        boolean shouldResetCounter = false;

        //Gets a list of all our fighters, and iterate through that to apply all effects
        for (ShipAPI fighter : Global.getCombatEngine().getShips()) {
            if (fighter.getWing() == null || fighter.getWing().getSourceShip() != ship || fighter.isHulk()) {
                continue;
            }

            //Saves our stats for easy access
            MutableShipStatsAPI fighterStats = fighter.getMutableStats();

            //If the system is turning off, start braking and spawn a puff of smoke
            if (state == State.OUT && runOnce) {
                runOnce = false;
                fighterStats.getMaxSpeed().unmodify(id);
                fighterStats.getMaxTurnRate().unmodify(id);
                if (fighter.getAngularVelocity() > fighterStats.getMaxTurnRate().getModifiedValue()) {
                    fighter.setAngularVelocity((fighter.getAngularVelocity() / Math.abs(fighter.getAngularVelocity())) * fighterStats.getMaxTurnRate().getModifiedValue());
                }
                if (fighter.getVelocity().length() > fighterStats.getMaxSpeed().getModifiedValue()) {
                    fighter.getVelocity().set((fighter.getVelocity().x / fighter.getVelocity().length()) * fighterStats.getMaxSpeed().getModifiedValue(), (fighter.getVelocity().y / fighter.getVelocity().length()) * fighterStats.getMaxSpeed().getModifiedValue());
                }

                //Render a puff of smoke to hide that we just appear out of nowhere
                for (int i = 0; i < 55; i++) {
                    SRD_FakeSmokePlugin.addFakeSmoke(MathUtils.getRandomNumberInRange(0.12f, 0.61f), MathUtils.getRandomNumberInRange(fighter.getCollisionRadius()*0.5f, fighter.getCollisionRadius()*0.9f),
                            MathUtils.getRandomPointInCircle(fighter.getLocation(), fighter.getCollisionRadius() * 1.1f), MathUtils.getRandomPointInCircle(null, 7f),
                            MathUtils.getRandomNumberInRange(-9f, 9f), 0.9f, new Color(0f, 0f, 0f));
                }
                return;
            } else if (state == State.OUT) {
                fighter.setExtraAlphaMult(1f - effectLevel);
                return;
            }

            //Drastically increases mobility and shifts us toward being phased, but not entirely
            //(also cuts our trails, since we are technically entering another dimension)
            NicToyCustomTrailPlugin.cutTrailsOnEntity(fighter);
            fighter.setExtraAlphaMult(0.3f);
            float speedBonus = SPEED_BONUS_MULT;
            float mobilityBonus = MOBILITY_BONUS_MULT;
            fighterStats.getMaxSpeed().modifyMult(id, speedBonus);
            fighterStats.getAcceleration().modifyMult(id, mobilityBonus);
            fighterStats.getDeceleration().modifyMult(id, mobilityBonus);
            fighterStats.getMaxTurnRate().modifyMult(id, (float)Math.sqrt(speedBonus));
            fighterStats.getTurnAcceleration().modifyMult(id, mobilityBonus);

            //Test to see if I can stop a fighter from shooting in nullspace
            fighter.setHoldFireOneFrame(true);

            //Moves the shadow to its appropriate location
            Vector2f shadowPos = MathUtils.getRandomPointInCircle(null, SHADOW_DISTANCE_DIFFERENCE_MULT);
            shadowPos.x += fighter.getLocation().x;
            shadowPos.y += fighter.getLocation().y;

            //If we are outside the screenspace, don't do the visual effects
            if (!Global.getCombatEngine().getViewport().isNearViewport(shadowPos, fighter.getCollisionRadius() * 1.5f)) {
                continue;
            }

            //If enough time has passed, render a new shadow
            if (shadowDelayCounter > SHADOW_DELAY) {
                float angleDifference = MathUtils.getRandomNumberInRange(-SHADOW_ANGLE_DIFFERENCE, SHADOW_ANGLE_DIFFERENCE) - 90f;

                for (int i = 0; i < SHADOW_FLICKER_CLONES; i++) {
                    Vector2f modifiedShadowPos = new Vector2f(MathUtils.getRandomNumberInRange(-SHADOW_FLICKER_DIFFERENCE, SHADOW_FLICKER_DIFFERENCE), MathUtils.getRandomNumberInRange(-SHADOW_FLICKER_DIFFERENCE, SHADOW_FLICKER_DIFFERENCE));
                    modifiedShadowPos.x += shadowPos.x;
                    modifiedShadowPos.y += shadowPos.y;
                    SRD_SpriteRenderPlugin.battlespaceRender(Global.getSettings().getSprite("SRD_fx", "SRD_generic_fighter_phantom_" + MathUtils.getRandomNumberInRange((int)0, (int)2)), modifiedShadowPos, new Vector2f(0f, 0f),
                            new Vector2f(fighter.getSpriteAPI().getWidth(), fighter.getSpriteAPI().getHeight()),
                            new Vector2f(0f, 0f), fighter.getFacing() + angleDifference,
                            0f, AFTERIMAGE_COLOR, true, 0.1f, 0f, 0.3f);
                }

                shouldResetCounter = true;
            }

            //Always render smoke at the shadow's position...
            for (int i = 0; i < (100 * Global.getCombatEngine().getElapsedInLastFrame()); i++) {
                SRD_FakeSmokePlugin.addFakeSmoke(MathUtils.getRandomNumberInRange(0.32f, 0.77f), MathUtils.getRandomNumberInRange(45f, 70f),
                        MathUtils.getRandomPointInCircle(shadowPos, fighter.getCollisionRadius()), MathUtils.getRandomPointInCircle(null, 8f),
                        MathUtils.getRandomNumberInRange(-15f, 15f), 0.4f, new Color(0f, 0f, 0f));
            }
        }

        if (shouldResetCounter) {
            shadowDelayCounter -= SHADOW_DELAY;
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

        for (ShipAPI fighter : Global.getCombatEngine().getShips()) {
            if (fighter.getWing() == null || fighter.getWing().getSourceShip() != ship || fighter.isHulk()) {
                continue;
            }

            MutableShipStatsAPI fighterStats = fighter.getMutableStats();
            runOnce = true;
            shadowDelayCounter = 0f;
            fighter.setPhased(false);
            fighter.setExtraAlphaMult(1f);
            fighterStats.getMaxSpeed().unmodify(id);
            fighterStats.getAcceleration().unmodify(id);
            fighterStats.getDeceleration().unmodify(id);
            fighterStats.getMaxTurnRate().unmodify(id);
            fighterStats.getTurnAcceleration().unmodify(id);

            if (Math.abs(fighter.getAngularVelocity()) > fighterStats.getMaxTurnRate().getModifiedValue()) {
                fighter.setAngularVelocity((fighter.getAngularVelocity() / Math.abs(fighter.getAngularVelocity())) * fighterStats.getMaxTurnRate().getModifiedValue());
            }
            if (fighter.getVelocity().length() > fighterStats.getMaxSpeed().getModifiedValue()) {
                fighter.getVelocity().set((fighter.getVelocity().x / fighter.getVelocity().length()) * fighterStats.getMaxSpeed().getModifiedValue(), (fighter.getVelocity().y / fighter.getVelocity().length()) * fighterStats.getMaxSpeed().getModifiedValue());
            }
        }
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0 && (Math.random() < (1f / 37f) || lastMessage == 1 || lastMessage == 4)) {
            if (lastMessage == 0) {
                lastMessage = 1;
            } else if (lastMessage == 1) {
                lastMessage = 4;
            } else {
                lastMessage = 0;
            }
            return new StatusData("it screams", false);
        } else if (index == 0 && (Math.random() < (1f / 36f) || lastMessage == 2 || lastMessage == 5)) {
            if (lastMessage == 0) {
                lastMessage = 2;
            } else if (lastMessage == 2) {
                lastMessage = 5;
            } else {
                lastMessage = 0;
            }
            return new StatusData("it hates", false);
        } else if (index == 0 && (Math.random() < (1f / 35f) || lastMessage == 3 || lastMessage == 6)) {
            if (lastMessage == 0) {
                lastMessage = 3;
            } else if (lastMessage == 3) {
                lastMessage = 6;
            } else {
                lastMessage = 0;
            }
            return new StatusData("it rejects", false);
        } else if (index == 0) {
            lastMessage = 0;
            return new StatusData("breaching nullspace", false);
        }
        return null;
    }
}