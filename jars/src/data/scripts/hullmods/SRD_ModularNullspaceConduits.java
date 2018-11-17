package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import data.scripts.SRD_ModPlugin;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SRD_ModularNullspaceConduits extends BaseHullMod {
    //-------------------------------------------INCOMPATIBLE HULLMOD SOUND---------------------------------------------
    private static final String BAD_HULLMOD_NOTIFICATION_SOUND = "cr_allied_critical";

    //------------------------------------------------PASSIVE BONUSES---------------------------------------------------
    private static final float NULLSPACE_DAMAGE_REDUCTION = 0.3f;

    //--------------------------------------------------SPEED BOOST-----------------------------------------------------
    //Number of seconds required to accelerate to maximum speed
    private static final Map<HullSize, Float> TIME_TO_MAX_SPEED_MAP = new HashMap<HullSize, Float>();
    static {
        TIME_TO_MAX_SPEED_MAP.put(HullSize.FRIGATE, 4f);
        TIME_TO_MAX_SPEED_MAP.put(HullSize.DESTROYER, 6f);
        TIME_TO_MAX_SPEED_MAP.put(HullSize.CRUISER, 8f);
        TIME_TO_MAX_SPEED_MAP.put(HullSize.CAPITAL_SHIP, 10f);
    }

    //How much speed is lost when turning: higher means more speed loss
    private static final Map<HullSize, Float> TURN_REMOVAL_MULT_MAP = new HashMap<HullSize, Float>();
    static {
        TURN_REMOVAL_MULT_MAP.put(HullSize.FRIGATE, 5f);
        TURN_REMOVAL_MULT_MAP.put(HullSize.DESTROYER, 4f);
        TURN_REMOVAL_MULT_MAP.put(HullSize.CRUISER, 3f);
        TURN_REMOVAL_MULT_MAP.put(HullSize.CAPITAL_SHIP, 2f);
    }
    private static final float MAX_SPEED_BONUS_PERCENTAGE = 10f;

    //-------------------------------------------------PASSIVE VISUALS--------------------------------------------------
    private static final Color AFTERIMAGE_COLOR = new Color(0.25f, 0.05f, 0.4f, 0.15f);
    private static final float AFTERIMAGE_THRESHHOLD = 0.2f;

    private static final float SHADOW_DISTANCE_DIFFERENCE = 25f;
    private static final float SHADOW_FLICKER_DIFFERENCE = 10f;
    private static final int SHADOW_FLICKER_CLONES = 3;
    //------------------------------------------------------------------------------------------------------------------

    //Permanent stat bonuses
    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getDynamic().getStat("SRD_NULLSPACE_DAMAGE_MULT").modifyMult(id, 1f - NULLSPACE_DAMAGE_REDUCTION);
    }

    //Removes Safety Overrides
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        //Creates a list for later use
        ArrayList<String> deletionList = new ArrayList<String>();

        //Checks if any given hullmods is Advanced Optics. If it is, remove it
        for (String s : ship.getVariant().getNonBuiltInHullmods()) {
            if (s.contains("safetyoverrides")) {
                deletionList.add(s);
            }
        }

        //Finally, deletes the hullmods we aren't allowed to have
        if (deletionList.size() > 0) {
            ship.getVariant().addMod("SRD_IncompatibleHullmodWarning");
            Global.getSoundPlayer().playUISound(BAD_HULLMOD_NOTIFICATION_SOUND, 1f, 1f);
        }
        for (String s : deletionList) {
            ship.getVariant().removeMod(s);
        }
    }

    //Handles all in-combat effects
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        //Nothing should happen if we are paused
        if (Global.getCombatEngine().isPaused()) {
            return;
        }
        //----------------------------------------------------------------------------HANDLES NULLSPACE STABILIZER-----------------------------------------------------------------------------------
        //Finds if there is an active stabilizer in range, or if we have a local stabilizer
        boolean hasStabilizer = false;
        if (ship.getVariant().getHullMods().contains("SRD_prototype_local_stabilizer")) {
            hasStabilizer = true;
        } else {
            for (ShipAPI testShip : Global.getCombatEngine().getShips()) {
                if (testShip.getVariant().getHullMods().contains("SRD_nullspace_stabilizer") && !testShip.isHulk()) {
                    hasStabilizer = true;
                    break;
                }
            }
        }

        //Applies stabilizer-specific bonuses
        if (hasStabilizer) {
            ship.getMutableStats().getTurnAcceleration().modifyMult("SRD_NullspaceConduitsID", SRD_NullspaceStabilizer.MOBILITY_MULT);
            ship.getMutableStats().getMaxTurnRate().modifyMult("SRD_NullspaceConduitsID", SRD_NullspaceStabilizer.MOBILITY_MULT);
            if (ship == Global.getCombatEngine().getPlayerShip()) {
                Global.getCombatEngine().maintainStatusForPlayerShip("SRD_NullspaceConduitsID", "graphics/icons/hullsys/SRD_nullspace_soother.png",
                        "Nullspace Soother", "mobility increased", false);
            }
        } else {
            ship.getMutableStats().getTurnAcceleration().unmodify("SRD_NullspaceConduitsID");
            ship.getMutableStats().getMaxTurnRate().unmodify("SRD_NullspaceConduitsID");
        }

        //--------------------------------------------------------------------------------HANDLES SPEED BOOST----------------------------------------------------------------------------------------

        //Sets some variables depending on hullsize
        float accelDuration = TIME_TO_MAX_SPEED_MAP.get(ship.getHullSize());
        float turnMult = TURN_REMOVAL_MULT_MAP.get(ship.getHullSize());
        float gainMult = 1f;
        if (hasStabilizer) {
            gainMult = 2f;
        }

        //"Nullifies" the stat, setting it to 0 initially. Should really only run once, but hey, it causes no harm when run multiple times
        ship.getMutableStats().getDynamic().getStat("SRD_NullspaceConduitsTurnCounter").modifyFlat("SRD_NullspaceConduitsNullifierID", -1f);

        //Keeps track of rotation, so that "braking" counts properly as turning the other direction
        boolean turns = false;
        if (ship.getEngineController().isTurningLeft() || ship.getEngineController().isTurningRight() || ship.getAngularVelocity() > 0.3f || ship.getAngularVelocity() < -0.3f) {
            turns = true;
        }

        //Adds amount to counter if we are accelerating in a straight line, otherwise remove counter * TURN_COUNT_MULT
        if (turns) {
            ship.getMutableStats().getDynamic().getStat("SRD_NullspaceConduitsTurnCounter").modifyFlat("SRD_NullspaceConduitsID",
                    ship.getMutableStats().getDynamic().getStat("SRD_NullspaceConduitsTurnCounter").getModifiedValue() - (amount * turnMult));
        } else if (ship.getEngineController().isAccelerating()) {
            ship.getMutableStats().getDynamic().getStat("SRD_NullspaceConduitsTurnCounter").modifyFlat("SRD_NullspaceConduitsID",
                    ship.getMutableStats().getDynamic().getStat("SRD_NullspaceConduitsTurnCounter").getModifiedValue() + (amount * gainMult));
        }


        //Makes sure that the value is never higher than accelDuration or lower than 0
        if (ship.getMutableStats().getDynamic().getStat("SRD_NullspaceConduitsTurnCounter").getModifiedValue() > accelDuration) {
            ship.getMutableStats().getDynamic().getStat("SRD_NullspaceConduitsTurnCounter").modifyFlat("SRD_NullspaceConduitsID", accelDuration);
        } else if (ship.getMutableStats().getDynamic().getStat("SRD_NullspaceConduitsTurnCounter").getModifiedValue() < 0f) {
            ship.getMutableStats().getDynamic().getStat("SRD_NullspaceConduitsTurnCounter").modifyFlat("SRD_NullspaceConduitsID", 0f);
        }

        //Actually applies the bonus, with an additional modifier if we have a Stabilizer in range
        float stabilizerMult = 1f;
        if (hasStabilizer) {
            stabilizerMult = SRD_NullspaceStabilizer.SPEED_BONUS_MULT;
        }
        ship.getMutableStats().getMaxSpeed().modifyPercent("SRD_NullspaceConduitsID",
                MAX_SPEED_BONUS_PERCENTAGE * stabilizerMult * (ship.getMutableStats().getDynamic().getStat("SRD_NullspaceConduitsTurnCounter").getModifiedValue() / accelDuration));

        //---------------------------------------------------------------------------------HANDLES PASSIVE VISUALS-----------------------------------------------------------------------------------

        if (!ship.getFluxTracker().isOverloaded() && !ship.isHulk() && !ship.isPhased()) {
            //Checks whether we should draw visuals at all (only draw when we are close enough to the viewport)
            ViewportAPI viewport = Global.getCombatEngine().getViewport();
            if (!viewport.isNearViewport(ship.getLocation(), ship.getCollisionRadius() * 1.5f)) {
                return;
            }

            //Handles afterimages
            ship.getMutableStats().getDynamic().getStat("SRD_AfterimageTracker").modifyFlat("SRD_AfterimageTrackerNullerID", -1);
            ship.getMutableStats().getDynamic().getStat("SRD_AfterimageTracker").modifyFlat("SRD_AfterimageTrackerID",
                    ship.getMutableStats().getDynamic().getStat("SRD_AfterimageTracker").getModifiedValue() + amount);
            if (ship.getMutableStats().getDynamic().getStat("SRD_AfterimageTracker").getModifiedValue() > AFTERIMAGE_THRESHHOLD) {
                Vector2f initialOffset = MathUtils.getRandomPointInCircle(null, SHADOW_DISTANCE_DIFFERENCE);
                for (int i = 0; i < SHADOW_FLICKER_CLONES; i++) {
                    Vector2f specificOffset = MathUtils.getRandomPointInCircle(initialOffset, SHADOW_FLICKER_DIFFERENCE);
                    ship.addAfterimage(
                            AFTERIMAGE_COLOR,
                            specificOffset.x, //X-location
                            specificOffset.y, //Y-location
                            ship.getVelocity().getX() * (-1f), //X-velocity
                            ship.getVelocity().getY() * (-1f), //Y-velocity
                            1f, //Maximum jitter
                            0.1f, //In duration
                            0f, //Mid duration
                            0.3f, //Out duration
                            true, //Additive blend?
                            true, //Combine with sprite color?
                            false //Above ship?
                    );
                }
                ship.getMutableStats().getDynamic().getStat("SRD_AfterimageTracker").modifyFlat("SRD_AfterimageTrackerID",
                        ship.getMutableStats().getDynamic().getStat("SRD_AfterimageTracker").getModifiedValue() - AFTERIMAGE_THRESHHOLD);
            }
        }
    }

    //Prevents the hullmod from being put on ships with normal Nullspace Conduits already installed, and ships with Safety Overrides
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        boolean canBeApplied = true;
        //Looks for existing nullspace conduit hullmods
        for (String s : ship.getVariant().getHullMods()) {
            if (SRD_ModPlugin.NULLSPACE_CONDUIT_HULLMODS.contains(s) && !s.equals("SRD_modular_nullspace_conduits")) {
                canBeApplied = false;
                break;
            }

            if (s.equals("safetyoverrides")) {
                canBeApplied = false;
                break;
            }
        }
        return canBeApplied;
    }

    public String getUnapplicableReason(ShipAPI ship) {
        for (String s : ship.getVariant().getHullMods()) {
            if (SRD_ModPlugin.NULLSPACE_CONDUIT_HULLMODS.contains(s) && !s.equals("SRD_modular_nullspace_conduits")) {
                return "The ship already has installed Nullspace Conduits!";
            }

            if (s.equals("safetyoverrides")) {
                return "Incompatible with Safety Overrides";
            }
        }
        return "BUG: report to mod author that the file 'SRD_ModularNullspaceConduits' has encountered an error";
    }

    //Adds the description strings
    @Override
    public String getDescriptionParam(int index, HullSize hullSize, ShipAPI ship) {
        if (index == 0) return "Nullspace Conduits";
        if (index == 1) return "" + (int)(MAX_SPEED_BONUS_PERCENTAGE) + "%";
        if (index == 2) return "twice";
        if (index == 3) return "" + (int)Math.ceil(NULLSPACE_DAMAGE_REDUCTION*100f) + "%";
        if (index == 4) return "Safety Overrides";
        return null;
    }
}