package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AsteroidAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.MultiHashtable;
import data.scripts.SRD_ModPlugin;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SRD_NullwaveEmitter extends BaseHullMod {
    private static final String BAD_HULLMOD_NOTIFICATION_SOUND = "cr_allied_critical";

    private static final float SECONDS_TO_LOSE_BONUS = 1.5f;
    private static final int NUMBER_OF_BOLTS = 22;
    private static final float LIGHTNING_RANGE = 800f;

    private static final float DECO_LIGHTNING_BOLTS_PER_SECOND = 1.7f;
    private static final float DECO_LIGHTNING_BOLTS_LENGTH = 90f;

    public static Color LIGHTNING_CORE_COLOR = new Color(0.25f, 0.05f, 0.40f, 0.5f);
    public static Color LIGHTNING_FRINGE_COLOR = new Color(0.20f, 0.03f, 0.30f, 0.3f);

    public Map<ShipAPI, Float> counters = new HashMap<ShipAPI, Float>();
    public Map<ShipAPI, Boolean> hasPopped = new HashMap<ShipAPI, Boolean>();

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        //Creates a list for later use
        ArrayList<String> deletionList = new ArrayList<String>();

        //Checks if any given hullmods is a shield-related hullmod
        for (String s : ship.getVariant().getNonBuiltInHullmods()) {
            if (SRD_ModPlugin.SHIELD_HULLMODS.contains(s)) {
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
        //Do not run when paused, or if our ship is nonexistant/a hulk
        if (Global.getCombatEngine().isPaused() || ship == null || ship.isHulk()) {
            return;
        }

        if (!counters.containsKey(ship)) {
            counters.put(ship, SECONDS_TO_LOSE_BONUS);
            hasPopped.put(ship, true);
        }

        if (ship.getSystem().getEffectLevel() >= 1f) {
            counters.put(ship, 0f);
            hasPopped.put(ship, false);
            return;
        } else {
            counters.put(ship, counters.get(ship) + amount);
        }

        if (counters.get(ship) > SECONDS_TO_LOSE_BONUS || hasPopped.get(ship)) {
            ship.getMutableStats().getShieldUnfoldRateMult().unmodify("SRD_NULLWAVE_EMITTER_ID");
            return;
        } else {
            //If it's the player is in the ship, give a small indicator
            if (ship == Global.getCombatEngine().getPlayerShip()) {
                Global.getCombatEngine().maintainStatusForPlayerShip("SRD_NULLWAVE_EMITTER_ID", "graphics/icons/hullsys/high_energy_focus.png", "Nullwave Emitter", "residual nullspace forces present", false);
            }

            //Emits tiny lightning bolts from all smoke vents
            for (WeaponSlotAPI testSlot : ship.getHullSpec().getAllWeaponSlotsCopy()) {
                if (!testSlot.isSystemSlot()) {
                    continue;
                }

                //Main part handling the small lightnings: keeps a counter which depends on how many sparks we want per second
                ship.getMutableStats().getDynamic().getStat("SRD_NullwaveTinyLightningEffectTracker").modifyFlat("SRD_NullwaveTinyLightningEffectTrackerNullerID", -1);
                ship.getMutableStats().getDynamic().getStat("SRD_NullwaveTinyLightningEffectTracker").modifyFlat("SRD_NullwaveTinyLightningEffectTrackerID", ship.getMutableStats().getDynamic().getStat("SRD_NullwaveTinyLightningEffectTracker").getModifiedValue() + amount);
                if (ship.getMutableStats().getDynamic().getStat("SRD_NullwaveTinyLightningEffectTracker").getModifiedValue() > (1f / DECO_LIGHTNING_BOLTS_PER_SECOND)) {
                    Vector2f positionOfBolt = MathUtils.getRandomPointInCircle(testSlot.computePosition(ship), 1f);
                    Vector2f targetPoint = MathUtils.getRandomPointInCone(positionOfBolt, DECO_LIGHTNING_BOLTS_LENGTH, testSlot.getAngle() - 12f + ship.getFacing(), testSlot.getAngle() + 12f + ship.getFacing());
                    Global.getCombatEngine().spawnEmpArc(ship, positionOfBolt, ship, new SimpleEntity(targetPoint),
                            DamageType.ENERGY, //Damage type
                            ship.getFluxTracker().getMaxFlux() * MathUtils.getRandomNumberInRange(0.01f, 0.03f), //Damage
                            ship.getFluxTracker().getMaxFlux() * MathUtils.getRandomNumberInRange(0.02f, 0.05f), //Emp
                            100000f, //Max range
                            "SRD_nullspace_lightning_impact", //Impact sound
                            MathUtils.getRandomNumberInRange(11f, 18f), // thickness of the lightning bolt
                            LIGHTNING_CORE_COLOR, //Central color
                            LIGHTNING_FRINGE_COLOR //Fringe Color
                    );

                    ship.getMutableStats().getDynamic().getStat("SRD_NullwaveTinyLightningEffectTracker").modifyFlat("SRD_NullwaveTinyLightningEffectTrackerID", ship.getMutableStats().getDynamic().getStat("SRD_NullwaveTinyLightningEffectTracker").getModifiedValue() - (1f / DECO_LIGHTNING_BOLTS_PER_SECOND));
                }
            }
        }

        //Shield-related bonuses: first, increase deployment speed drastically
        ship.getMutableStats().getShieldUnfoldRateMult().modifyMult("SRD_NULLWAVE_EMITTER_ID", 1000f);

        //Then, if we have not yet deployed our shield, and just deployed it, unleash a wave of destruction
        if (ship.getShield().isOn() && ship.getShield().getActiveArc() > 10f) {
            hasPopped.put(ship, true);

            //First, find all valid targets: we can only shoot missiles, ships and asteroids, and we can't shoot ourselves or anything within our shield radius
            List<CombatEntityAPI> validTargets = new ArrayList<CombatEntityAPI>();
            for (CombatEntityAPI entityToTest : CombatUtils.getEntitiesWithinRange(ship.getLocation(), LIGHTNING_RANGE)) {
                if (entityToTest != ship && MathUtils.getDistance(entityToTest.getLocation(), ship.getShield().getLocation()) > ship.getShield().getRadius()) {
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

                        //Checks if the target is within the ship's shield arc
                        float angleBetween = VectorUtils.getAngle(ship.getShield().getLocation(), entityToTest.getLocation());
                        if (angleBetween >= ship.getFacing() - (ship.getShield().getArc() / 2f) && angleBetween <= ship.getFacing() + (ship.getShield().getArc() / 2f)) {
                            validTargets.add(entityToTest);
                        }
                    }
                }
            }
            for (int i = 0; i < NUMBER_OF_BOLTS; i++) {
                //Get a source point on the shield
                Vector2f sourcePoint = MathUtils.getPointOnCircumference(ship.getShield().getLocation(), ship.getShield().getRadius(), MathUtils.getRandomNumberInRange(ship.getFacing() - (ship.getShield().getArc() / 2f), ship.getFacing() + (ship.getShield().getArc() / 2f)));

                //If there are no valid targets in range left, fire lightning randomly in the area
                if (validTargets.isEmpty()) {
                    Vector2f targetPoint = MathUtils.getRandomPointInCone(ship.getShield().getLocation(), LIGHTNING_RANGE,ship.getFacing() - (ship.getShield().getArc() / 2f), ship.getFacing() + (ship.getShield().getArc() / 2f));
                    Global.getCombatEngine().spawnEmpArc(ship, sourcePoint, ship, new SimpleEntity(targetPoint),
                            DamageType.ENERGY, //Damage type
                            ship.getFluxTracker().getMaxFlux() * MathUtils.getRandomNumberInRange(0.01f, 0.03f), //Damage
                            ship.getFluxTracker().getMaxFlux() * MathUtils.getRandomNumberInRange(0.02f, 0.05f), //Emp
                            100000f, //Max range
                            "tachyon_lance_emp_impact", //Impact sound
                            MathUtils.getRandomNumberInRange(11f, 18f), // thickness of the lightning bolt
                            LIGHTNING_CORE_COLOR, //Central color
                            LIGHTNING_FRINGE_COLOR //Fringe Color
                    );
                    continue;
                }

                //Otherwise, we fire at a random target in the list
                CombatEntityAPI target = validTargets.get(MathUtils.getRandomNumberInRange(0, validTargets.size() - 1));
                Global.getCombatEngine().spawnEmpArc(ship, sourcePoint, ship, target,
                        DamageType.ENERGY, //Damage type
                        ship.getFluxTracker().getMaxFlux() * MathUtils.getRandomNumberInRange(0.01f, 0.03f), //Damage
                        ship.getFluxTracker().getMaxFlux() * MathUtils.getRandomNumberInRange(0.02f, 0.05f), //Emp
                        100000f, //Max range
                        "tachyon_lance_emp_impact", //Impact sound
                        MathUtils.getRandomNumberInRange(11f, 18f), // thickness of the lightning bolt
                        LIGHTNING_CORE_COLOR, //Central color
                        LIGHTNING_FRINGE_COLOR //Fringe Color
                );

                //Ensures each target can only be hit once per activation
                validTargets.remove(target);
            }
        }
    }

    //Prevents the hullmod from being put on ships
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }

    //Adds the description strings
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return "instantly deploy its shield";
        if (index == 1) return "creates a cone of destruction similar to the ship overloading";
        if (index == 2) return "unable to mount any shield-related hullmods";
        return null;
    }
}

