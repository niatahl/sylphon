package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AsteroidAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import data.scripts.plugins.MagicTrailPlugin;
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

public class SRD_OutcastEngineering extends BaseHullMod {
    //------------------------------------------------PASSIVE BONUSES---------------------------------------------------
    private static final float NULLSPACE_DAMAGE_REDUCTION = 0.4f;

    //--------------------------------------------ARMOR BONUS AT HIGH FLUX----------------------------------------------
    public static final float MAX_DAMAGE_REDUCTION = 0.3f;
    public static final float MAX_EMP_REDUCTION = 0.6f;

    //--------------------------------------------------SPEED BOOST-----------------------------------------------------
    //Number of seconds required to accelerate to maximum speed
    public static final Map<HullSize, Float> TIME_TO_MAX_SPEED_MAP = new HashMap<HullSize, Float>();
    static {
        TIME_TO_MAX_SPEED_MAP.put(HullSize.FRIGATE, 2f);
        TIME_TO_MAX_SPEED_MAP.put(HullSize.DESTROYER, 3f);
        TIME_TO_MAX_SPEED_MAP.put(HullSize.CRUISER, 4f);
        TIME_TO_MAX_SPEED_MAP.put(HullSize.CAPITAL_SHIP, 5f);
    }

    //How much speed is lost when turning: higher means more speed loss
    public static final Map<HullSize, Float> TURN_REMOVAL_MULT_MAP = new HashMap<HullSize, Float>();
    static {
        TURN_REMOVAL_MULT_MAP.put(HullSize.FRIGATE, 2.5f);
        TURN_REMOVAL_MULT_MAP.put(HullSize.DESTROYER, 2f);
        TURN_REMOVAL_MULT_MAP.put(HullSize.CRUISER, 1.5f);
        TURN_REMOVAL_MULT_MAP.put(HullSize.CAPITAL_SHIP, 1f);
    }
    public static final float MAX_SPEED_BONUS_PERCENTAGE = 15f;

    //-------------------------------------------------PASSIVE VISUALS--------------------------------------------------
    private static final float SMOKE_MAXIMUM_SPEED_NORMAL = 240f;
    private static final float SMOKE_MAXIMUM_SPEED_VENTING = 240f;
    private static final float SMOKE_ANGLE = 0.9f;
    private static final float SMOKE_OPACITY = 0.6f;
    private static final float SMOKE_START_SIZE = 17f;
    private static final float SMOKE_END_SIZE = 34f;

    private static final Color SMOKE_COLOR = new Color(0, 0, 0);
    private static final Color SMOKE_VENT_COLOR = new Color(255, 20, 0);

    private static final Color AFTERIMAGE_COLOR = new Color(255f/255f, 20f/255f, 0f/255f, 35f/255f);
    private static final float AFTERIMAGE_THRESHHOLD = 0.2f;

    private static final float SHADOW_DISTANCE_DIFFERENCE = 25f;
    private static final float SHADOW_FLICKER_DIFFERENCE = 10f;
    private static final int SHADOW_FLICKER_CLONES = 3;

    //Handles our trail IDs
    public Map<ShipAPI, Map<WeaponSlotAPI, Float>> associatedIDs = new HashMap<ShipAPI, Map<WeaponSlotAPI, Float>>();

    //A list for preventing certain shipsystems from spawning smoke during their activation:
    // -1 means no trails when !shipsystem.isActive()
    // 0 means no trails when shipsystem.isActive()
    // 1 means no trails when shipsystem.getEffectLevel >= 1f
    public static final Map<String, Integer> FORBIDDEN_SHIPSYSTEMS = new HashMap<String, Integer>();
    static {
        FORBIDDEN_SHIPSYSTEMS.put("SRD_nullspace_anchor", 0);
    }

    //-------------------------------------OVERLOAD STATS AND TRACKERS--------------------------------------------------
    public static final float OVERLOAD_LIGHTNING_CHANCE_PER_SECOND = 0.99f;
    public static final Map<HullSize, Float> OVERLOAD_LIGHTNING_RANGE_MAP = new HashMap<HullSize, Float>();
    static {
        OVERLOAD_LIGHTNING_RANGE_MAP.put(HullSize.FRIGATE, 500f);
        OVERLOAD_LIGHTNING_RANGE_MAP.put(HullSize.DESTROYER, 600f);
        OVERLOAD_LIGHTNING_RANGE_MAP.put(HullSize.CRUISER, 800f);
        OVERLOAD_LIGHTNING_RANGE_MAP.put(HullSize.CAPITAL_SHIP, 1000f);
    }

    public static final Map<HullSize, Float> OVERLOAD_LIGHTNING_DAMAGE = new HashMap<HullSize, Float>();
    static {
        OVERLOAD_LIGHTNING_DAMAGE.put(HullSize.FRIGATE, 35f);
        OVERLOAD_LIGHTNING_DAMAGE.put(HullSize.DESTROYER, 50f);
        OVERLOAD_LIGHTNING_DAMAGE.put(HullSize.CRUISER, 70f);
        OVERLOAD_LIGHTNING_DAMAGE.put(HullSize.CAPITAL_SHIP, 150f);
    }

    public static final Map<HullSize, Float> OVERLOAD_LIGHTNING_EMP = new HashMap<HullSize, Float>();
    static {
        OVERLOAD_LIGHTNING_EMP.put(HullSize.FRIGATE, 75f);
        OVERLOAD_LIGHTNING_EMP.put(HullSize.DESTROYER, 100f);
        OVERLOAD_LIGHTNING_EMP.put(HullSize.CRUISER, 200f);
        OVERLOAD_LIGHTNING_EMP.put(HullSize.CAPITAL_SHIP, 300f);
    }

    public static Color LIGHTNING_CORE_COLOR = new Color(0.45f, 0.10f, 0.80f, 0.8f);
    public static Color LIGHTNING_FRINGE_COLOR = new Color(0.20f, 0.03f, 0.30f, 0.5f);
    //------------------------------------------------------------------------------------------------------------------

    //Permanent stat bonuses
    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getDynamic().getStat("SRD_NULLSPACE_DAMAGE_MULT").modifyMult(id, 1f - NULLSPACE_DAMAGE_REDUCTION);
    }

    //Handles all in-combat effects
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        //Nothing should happen if we are paused
        if (Global.getCombatEngine().isPaused()) {
            return;
        }
        //----------------------------------------------------------------------------HANDLES ARMOR BONUS--------------------------------------------------------------------------------------------
        //We get damage/EMP reduction for our armor depending on flux level, starting at 30% flux and maxing out at 90% flux
        float currentEffect = ((Math.max(0.3f, Math.min(0.9f, ship.getFluxTracker().getFluxLevel())) - 0.3f) / 0.6f);

        //Nia: Basic damage reduction variant. Possibly with a higher bonus to armor damage reduction than hull damage reduction?
        //			Sometimes simplest is best and it does match with the damper-field like visuals.
        //			EMP resist seems like a good addition to this. The Metafalica in particular will like this with how easy those broadside guns get shut down by EMP
		//Nicke: Agreed; using the code, but fixing the values to utilize actual variables
        ship.getMutableStats().getArmorDamageTakenMult().modifyMult("SRD_NullspaceConduitsID", 1f - (MAX_DAMAGE_REDUCTION*currentEffect));
        ship.getMutableStats().getHullDamageTakenMult().modifyMult("SRD_NullspaceConduitsID", 1f - (MAX_DAMAGE_REDUCTION*currentEffect));
        ship.getMutableStats().getEmpDamageTakenMult().modifyMult("SRD_NullspaceConduitsID", 1f - (MAX_EMP_REDUCTION*currentEffect));

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
                Global.getCombatEngine().maintainStatusForPlayerShip("SRD_NullspaceConduitsID", "graphics/sylphon/icons/hullsys/SRD_nullspace_soother.png",
                        "Nullspace Soother", "mobility increased", false);
            }
        } else {
            ship.getMutableStats().getTurnAcceleration().unmodify("SRD_NullspaceConduitsID");
            ship.getMutableStats().getMaxTurnRate().unmodify("SRD_NullspaceConduitsID");
        }

        //---------------------------------------------------------------------------HANDLES DANGEROUS OVERLOAD--------------------------------------------------------------------------------------

        //Sets our hullsize-dependant variables
        float actualLightningRange = OVERLOAD_LIGHTNING_RANGE_MAP.get(ship.getHullSize());
        float actualLightingDamage = OVERLOAD_LIGHTNING_DAMAGE.get(ship.getHullSize());
        float actualLightningEMP = OVERLOAD_LIGHTNING_EMP.get(ship.getHullSize());

        //When overloading... well, all hell breaks loose
        if (ship.getFluxTracker().isOverloaded()) {
            //Checks if we should send lightning this frame
            if (Math.random() < (1f - Math.pow(1 - OVERLOAD_LIGHTNING_CHANCE_PER_SECOND, amount))) {
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
                                LIGHTNING_CORE_COLOR, //Central color
                                LIGHTNING_FRINGE_COLOR //Fringe Color
                        );
                    }
                }
            }
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

        //Checks for any shipsystems that should turn off our trails
        boolean hasForbiddenShipsystemActive = false;
        if (FORBIDDEN_SHIPSYSTEMS.get(ship.getSystem().getId()) != null) {
            if (FORBIDDEN_SHIPSYSTEMS.get(ship.getSystem().getId()) == -1 && !ship.getSystem().isActive()) {
                hasForbiddenShipsystemActive = true;
            } else if (FORBIDDEN_SHIPSYSTEMS.get(ship.getSystem().getId()) == 0 && ship.getSystem().isActive()) {
                hasForbiddenShipsystemActive = true;
            } else if (FORBIDDEN_SHIPSYSTEMS.get(ship.getSystem().getId()) == 1 && ship.getSystem().getEffectLevel() >= 1f) {
                hasForbiddenShipsystemActive = true;
            }
        }

        if (!ship.getFluxTracker().isOverloaded() && !ship.isHulk() && !ship.isPhased() && !hasForbiddenShipsystemActive) {
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
            SpriteAPI spriteToUse = Global.getSettings().getSprite("SRD_fx", "conduit_trails_standard");
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
                float smokeSpeed = SMOKE_MAXIMUM_SPEED_NORMAL;
                Color actualSmokeColor = SMOKE_COLOR;

                //When venting, we use a different color for our smoke and can use a different speed
                if (ship.getFluxTracker().isVenting()) {
                    smokeSpeed = SMOKE_MAXIMUM_SPEED_VENTING;
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
                        0.20f, 0.75f, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, 128f, 1950f, new Vector2f(0f, 0f), null);
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
    @Override
    public String getDescriptionParam(int index, HullSize hullSize, ShipAPI ship) {
        if (index == 0) return "Nullspace Conduits";
        if (index == 1) return "" + (int)(MAX_SPEED_BONUS_PERCENTAGE) + "%";
        if (index == 2) return "nullspace energy discharges from the conduits will harm the ship and any nearby objects";
        if (index == 3) return "" + (int)(MAX_DAMAGE_REDUCTION * 100f) + "%";
        if (index == 4) return "" + (int)(MAX_EMP_REDUCTION * 100f) + "%";
        if (index == 5) return "30%";
        if (index == 6) return "90%";
        if (index == 7) return "" + (int)Math.ceil(NULLSPACE_DAMAGE_REDUCTION*100f) + "%";
        return null;
    }
}