package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AsteroidAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import data.scripts.plugins.MagicTrailPlugin;
import data.scripts.util.MagicRender;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;

public class SRD_NullspaceStabilizer extends BaseHullMod {
    private static final String BAD_HULLMOD_NOTIFICATION_SOUND = "cr_allied_critical";
    //PASSIVE VISUALS
    private static final float SMOKE_MAXIMUM_SPEED = 400f;
    private static final float SMOKE_ANGLE = 0.9f;
    private static final float SMOKE_OPACITY = 0.6f;
    private static final float SMOKE_START_SIZE = 23f;
    private static final float SMOKE_END_SIZE = 67f;

    private static final Color SMOKE_COLOR = new Color(0, 0, 0);
    private static final Color SMOKE_VENT_COLOR = new Color(116, 50, 145);

    private static final Color AFTERIMAGE_COLOR = new Color(0.25f, 0.05f, 0.4f, 0.15f);
    private static final float AFTERIMAGE_THRESHHOLD = 0.2f;

    private static final float SHADOW_DISTANCE_DIFFERENCE = 25f;
    private static final float SHADOW_FLICKER_DIFFERENCE = 10f;
    private static final int SHADOW_FLICKER_CLONES = 3;

    private Map<ShipAPI, Map<WeaponSlotAPI, Float>> associatedIDs = new HashMap<ShipAPI, Map<WeaponSlotAPI, Float>>();    //This handles our trail IDs

    private static final Color PULSE_COLOR = new Color(0.45f, 0.10f, 0.80f, 0.85f);
    private static final float PULSE_INTERVAL = 1.65f;
    private static final Vector2f PULSE_STARTSIZE = new Vector2f(180f, 373f);
    private static final Vector2f PULSE_GROWTH = new Vector2f(360f, 746f);

    private Map<ShipAPI, Float> pulseTimers = new HashMap<ShipAPI, Float>();

    //ACTUAL STATS
    public static final float MOBILITY_MULT = 1.2f;
    public static final float SPEED_BONUS_MULT = 2f;
    private static final int BURN_BONUS = 1;
    private static final float SUPPLY_USE_MULT = 1.5f;

    //Any effects applied before the ship is generated: such as supply use, burn speed bonus, energy weapon cost mult and nullspace damage reduction
    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getDynamic().getMod(Stats.FLEET_BURN_BONUS).modifyFlat(id, BURN_BONUS);
        stats.getSuppliesPerMonth().modifyMult(id, SUPPLY_USE_MULT);
        stats.getEnergyWeaponFluxCostMod().modifyMult("SRD_NullspaceConduitsID", SRD_NullspaceConduits.ENERGY_FLUX_COST_PERCENT_DIFF);
        stats.getBeamWeaponFluxCostMult().modifyMult("SRD_NullspaceConduitsID", SRD_NullspaceConduits.BEAM_FLUX_COST_PERCENT_DIFF / SRD_NullspaceConduits.ENERGY_FLUX_COST_PERCENT_DIFF);
        stats.getDynamic().getStat("SRD_NULLSPACE_DAMAGE_MULT").modifyMult(id, 1f - SRD_NullspaceConduits.NULLSPACE_DAMAGE_REDUCTION);
    }

    //Removes Advanced Optics
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        //Creates a list for later use
        ArrayList<String> deletionList = new ArrayList<String>();

        //Checks if any given hullmods is Advanced Optics. If it is, remove it
        for (String s : ship.getVariant().getHullMods()) {
            if (s.contains("advancedoptics")) {
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

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        //--------------------------------------------------------------------------------HANDLES PASSIVE PULSES-------------------------------------------------------------------------------------
        //Adjusts time difference to consider pausing and if we are using our shipsystem (or if we are a hulk)
        float actualAmount = amount;
        if (Global.getCombatEngine().isPaused() || ship.getSystem().getEffectLevel() > 0f || ship.isHulk()) {
            actualAmount = 0f;
        }

        //Handles timers
        if (pulseTimers.get(ship) == null) {
            pulseTimers.put(ship, 0f);
        }
        pulseTimers.put(ship, pulseTimers.get(ship) + actualAmount);

        //Render the pulse effect
        if (pulseTimers.get(ship) > PULSE_INTERVAL) {
            pulseTimers.put(ship, pulseTimers.get(ship) - PULSE_INTERVAL);
            MagicRender.objectspace(Global.getSettings().getSprite("SRD_fx", "seresvalla_pulse"), ship, new Vector2f(0f, 0f),
                    new Vector2f(0f, 0f), PULSE_STARTSIZE, PULSE_GROWTH, 180f, 0f, true, PULSE_COLOR, true, 0.05f, 0f, 0.7f, true);
        }


        //----------------------------------------------------------------------------HANDLES NULLSPACE STABILIZER-----------------------------------------------------------------------------------
        //Applies stabilizer-specific bonuses
        ship.getMutableStats().getTurnAcceleration().modifyMult("SRD_NullspaceConduitsID", SRD_NullspaceStabilizer.MOBILITY_MULT);
        ship.getMutableStats().getMaxTurnRate().modifyMult("SRD_NullspaceConduitsID", SRD_NullspaceStabilizer.MOBILITY_MULT);
        if (ship == Global.getCombatEngine().getPlayerShip()) {
            Global.getCombatEngine().maintainStatusForPlayerShip("SRD_NullspaceConduitsID", "graphics/sylphon/icons/hullsys/SRD_nullspace_soother.png",
                    "Nullspace Soother", "mobility increased", false);
        }


        //---------------------------------------------------------------------------HANDLES DANGEROUS OVERLOAD--------------------------------------------------------------------------------------

        //Sets our hullsize-dependant variables
        float actualLightningRange = SRD_NullspaceConduits.OVERLOAD_LIGHTNING_RANGE_MAP.get(ship.getHullSize());
        float actualLightingDamage = SRD_NullspaceConduits.OVERLOAD_LIGHTNING_DAMAGE.get(ship.getHullSize());
        float actualLightningEMP = SRD_NullspaceConduits.OVERLOAD_LIGHTNING_EMP.get(ship.getHullSize());

        //When overloading... well, all hell breaks loose
        if (ship.getFluxTracker().isOverloaded()) {
            //Checks if we should send lightning this frame
            if (Math.random() < (1f - Math.pow(1 - SRD_NullspaceConduits.OVERLOAD_LIGHTNING_CHANCE_PER_SECOND, amount))) {
                //Choose a random vent port to send lightning from
                List<WeaponSlotAPI> vents = new ArrayList<WeaponSlotAPI>();
                for (WeaponSlotAPI weaponSlotAPI : ship.getHullSpec().getAllWeaponSlotsCopy()) {
                    if (weaponSlotAPI.isSystemSlot()) {
                        vents.add(weaponSlotAPI);
                    }
                }

                //If we have no vents, we can't do a dangerous overload this frame; ignore the rest of the code
                if (!vents.isEmpty()) {
                    Vector2f sourcePoint = vents.get(MathUtils.getRandomNumberInRange(0, vents.size()-1)).computePosition(ship);

                    //Then, find all valid targets: we can only shoot missiles, ships and asteroids [including ourselves]
                    List<CombatEntityAPI> validTargets = new ArrayList<CombatEntityAPI>();
                    for (CombatEntityAPI entityToTest : CombatUtils.getEntitiesWithinRange(sourcePoint, actualLightningRange)) {
                        if (entityToTest instanceof ShipAPI || entityToTest instanceof AsteroidAPI || entityToTest instanceof MissileAPI) {
                            //Phased targets, and targets with no collision, are ignored
                            if (entityToTest instanceof ShipAPI) {
                                if (((ShipAPI)entityToTest).isPhased()) {
                                    continue;
                                }
                            }
                            if (entityToTest.getCollisionClass().equals(CollisionClass.NONE)) {
                                continue;
                            }

                            validTargets.add(entityToTest);
                        }
                    }

                    //If we have no valid targets, do nothing
                    if (!validTargets.isEmpty()) {
                        //And finally, fire at a random valid target
                        CombatEntityAPI target = validTargets.get(MathUtils.getRandomNumberInRange(0, validTargets.size() - 1));
                        Global.getCombatEngine().spawnEmpArc(ship, sourcePoint, ship, target,
                                DamageType.ENERGY, //Damage type
                                MathUtils.getRandomNumberInRange(0.8f, 1.2f) * actualLightingDamage * (target instanceof ShipAPI ? ((ShipAPI)target).getMutableStats().getDynamic().getStat("SRD_NULLSPACE_DAMAGE_MULT").getModifiedValue() : 1f), //Damage
                                MathUtils.getRandomNumberInRange(0.8f, 1.2f) * actualLightningEMP * (target instanceof ShipAPI ? ((ShipAPI)target).getMutableStats().getDynamic().getStat("SRD_NULLSPACE_DAMAGE_MULT").getModifiedValue() : 1f), //Emp
                                100000f, //Max range
                                "SRD_nullspace_lightning_impact", //Impact sound
                                14f * actualLightingDamage / 50f, // thickness of the lightning bolt
                                SRD_NullspaceConduits.LIGHTNING_CORE_COLOR, //Central color
                                SRD_NullspaceConduits.LIGHTNING_FRINGE_COLOR //Fringe Color
                        );
                    }
                }
            }
        }

        //--------------------------------------------------------------------------------HANDLES SPEED BOOST----------------------------------------------------------------------------------------

        //Sets some variables depending on hullsize
        float accelDuration = SRD_NullspaceConduits.TIME_TO_MAX_SPEED_MAP.get(ship.getHullSize());
        float turnMult = SRD_NullspaceConduits.TURN_REMOVAL_MULT_MAP.get(ship.getHullSize());
        float gainMult = 2f; //CHANGED, since we always have a stabilizer: we ARE the stabilizer

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

        //Actually applies the bonus, with an additional modifier since we *are* the stabilizer
        float stabilizerMult = SPEED_BONUS_MULT;
        ship.getMutableStats().getMaxSpeed().modifyPercent("SRD_NullspaceConduitsID",
                SRD_NullspaceConduits.MAX_SPEED_BONUS_PERCENTAGE * stabilizerMult * (ship.getMutableStats().getDynamic().getStat("SRD_NullspaceConduitsTurnCounter").getModifiedValue() / accelDuration));


        //--------------------------------------------------------------------------HANDLES PASSIVE VISUALS FROM CONDUITS-----------------------------------------------------------------------------------

        if (!ship.getFluxTracker().isOverloaded() && !ship.isHulk() && !ship.isPhased()) {
            //Checks whether we should draw visuals at all (only draw when we are close enough to the viewport)
            ViewportAPI viewport = Global.getCombatEngine().getViewport();
            if (!viewport.isNearViewport(ship.getLocation(), ship.getCollisionRadius() * 1.5f)) {
                return;
            }

            //Double-checks our ID map
            if (associatedIDs.get(ship) == null) {
                associatedIDs.put(ship, new HashMap<WeaponSlotAPI, Float>());
            }

            //Main part handling smoke: emits a CustomTrail from each System weapon mount
            SpriteAPI spriteToUse = Global.getSettings().getSprite("SRD_fx", "conduit_trails_stabilizer");
            for (WeaponSlotAPI testSlot : ship.getHullSpec().getAllWeaponSlotsCopy()) {
                if (!testSlot.isSystemSlot()) {
                    continue;
                }

                //Gets the angle of our weapon slot, so we can adjust our size from that
                float sizeMod = testSlot.getArc() / 60f;

                //ID handling
                if (associatedIDs.get(ship).get(testSlot) == null) {
                    associatedIDs.get(ship).put(testSlot, MagicTrailPlugin.getUniqueID());
                }

                //Handles actual contrail rendering
                float smokeSpeed = SMOKE_MAXIMUM_SPEED;
                Color actualSmokeColor = SMOKE_COLOR;

                //When venting, we use a different color for our smoke
                if (ship.getFluxTracker().isVenting()) {
                    actualSmokeColor = new Color((int)(SMOKE_VENT_COLOR.getRed() * ship.getFluxTracker().getFluxLevel()), (int)(SMOKE_VENT_COLOR.getGreen() * ship.getFluxTracker().getFluxLevel()),
                            (int)(SMOKE_VENT_COLOR.getBlue() * ship.getFluxTracker().getFluxLevel()));
                }

                Vector2f positionOfSmoke = testSlot.computePosition(ship);
                float angleOfSmoke = testSlot.getAngle() + ship.getFacing() + MathUtils.getRandomNumberInRange(-SMOKE_ANGLE, SMOKE_ANGLE);

                //Generates the trail
                float endAngleVel = -10f;
                if (testSlot.getAngle() > 180 || testSlot.getAngle() < 0) {
                    endAngleVel = 10f;
                } else if (testSlot.getAngle() % 180f == 0f) {
                    endAngleVel = 0;
                }
                MagicTrailPlugin.AddTrailMemberAdvanced((CombatEntityAPI)ship, associatedIDs.get(ship).get(testSlot), spriteToUse, positionOfSmoke, smokeSpeed, smokeSpeed * 0.5f, angleOfSmoke,
                        0f, endAngleVel, SMOKE_START_SIZE * sizeMod, SMOKE_END_SIZE * sizeMod, actualSmokeColor, SMOKE_COLOR, SMOKE_OPACITY, 0f,
                        0.10f, 0.4f, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, 128f, 1950f, new Vector2f(0f, 0f), null,
                        CombatEngineLayers.CRUISERS_LAYER); //Note the higher-than-ship layer; this is so it spawns from the "top" of the ring, and not the bottom
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
        } else {
            //Cuts our custom trails
            SpriteAPI spriteToUse = Global.getSettings().getSprite("SRD_fx", "conduit_trails_standard");
            for (WeaponSlotAPI testSlot : ship.getHullSpec().getAllWeaponSlotsCopy()) {
                if (!testSlot.isSystemSlot()) {
                    continue;
                }
                //ID handling, to cut the trails
                if (associatedIDs.get(ship) == null) {
                    associatedIDs.put(ship, new HashMap<WeaponSlotAPI, Float>());
                }
                associatedIDs.get(ship).put(testSlot, MagicTrailPlugin.getUniqueID());
            }
        }
    }

    //Prevents the hullmod from being put on ships
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        boolean canBeApplied = false;
        return canBeApplied;
    }

    //Adds the description strings
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return "" + BURN_BONUS;
        if (index == 1) return "Nullspace Conduits";
        if (index == 2) return "" + (int)((MOBILITY_MULT-1f) * 100f) + "%";
        if (index == 3) return "" + (int)((SPEED_BONUS_MULT -1f) * 100f) + "%";
        if (index == 4) return "" + (int)((SUPPLY_USE_MULT-1f) * 100f) + "%";
        if (index == 5) return "Nullspace Conduits";
        return null;
    }
}