package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AsteroidAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import data.scripts.SRD_ModPlugin;
import data.scripts.shipsystems.SRD_ArmorPolarization;
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

public class SRD_NanolaminateArmor extends BaseHullMod {

    private static final float ARMOR_FRACTION_LOST_TO_ACTIVATE = 0.10f;

    private static final float SECONDS_TO_LOSE_BONUS = 3.5f;
    private static final int NUMBER_OF_BOLTS = 13;
    private static final float LIGHTNING_RANGE = 800f;

    private static final float DECO_LIGHTNING_BOLTS_PER_SECOND = 1.7f;
    private static final float DECO_LIGHTNING_BOLTS_LENGTH = 75f;

    public static final Color LIGHTNING_CORE_COLOR = new Color(0.25f, 0.05f, 0.40f, 0.45f);
    public static final Color LIGHTNING_FRINGE_COLOR = new Color(0.20f, 0.03f, 0.30f, 0.26f);

    private Map<ShipAPI, Float> counters = new HashMap<ShipAPI, Float>();
    private Map<ShipAPI, Boolean> hasPopped = new HashMap<ShipAPI, Boolean>();
    private Map<ShipAPI, Float> armorFractionLostSinceLast = new HashMap<ShipAPI, Float>();
    private Map<ShipAPI, Float> armorValueLastFrame = new HashMap<ShipAPI, Float>();

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        //Do not run when paused, or if our ship is nonexistant/a hulk, or if we have no shield
        if (Global.getCombatEngine().isPaused() || ship == null || ship.isHulk() || ship.getShield() == null || ship.getShield().getType() == ShieldAPI.ShieldType.NONE || ship.getShield().getType() == ShieldAPI.ShieldType.PHASE) {
            return;
        }

        //Instantiates our maps
        if (!counters.containsKey(ship)) {
            counters.put(ship, SECONDS_TO_LOSE_BONUS);
            hasPopped.put(ship, true);
            armorFractionLostSinceLast.put(ship, 0f);
            armorValueLastFrame.put(ship, 1f);
        }

        //--------------------------------------------------------------------------------TRACKS LOST ARMOR------------------------------------------------------------------------------------------

        //Starts by determining our number of armor cells
        int maxX = ship.getArmorGrid().getLeftOf() + ship.getArmorGrid().getRightOf();
        int maxY = ship.getArmorGrid().getAbove() + ship.getArmorGrid().getBelow();

        //Then, iterate over each cell on the armor and add its current armor fraction
        float totalArmorFraction = 0f;
        for (int ix = 0; ix < maxX; ix++) {
            for (int iy = 0; iy < maxY; iy++) {
                totalArmorFraction += ship.getArmorGrid().getArmorFraction(ix, iy);
            }
        }

        //After that, we divide by the number of armor cells and compare to our previous-frame value
        totalArmorFraction /= (float)(maxX * maxY);
        float armorLoss = Math.max(0f, armorValueLastFrame.get(ship) - totalArmorFraction); //Math.max is there, so we don't count negative when regenerating armor
        armorValueLastFrame.put(ship, totalArmorFraction);

        //Also, we ignore the damage reduction from Armor Polarization: adjust accordingly
        if (ship.getSystem().isActive()) {
            armorLoss /= SRD_ArmorPolarization.INCOMING_DAMAGE_MULT;
        }

        //Finally, we add the new armor loss to our existing armor loss, and compare; if it is high enough, we prepare ourselves for a new shield unfolding
        armorFractionLostSinceLast.put(ship, armorFractionLostSinceLast.get(ship) + armorLoss);
        if (armorFractionLostSinceLast.get(ship) >= ARMOR_FRACTION_LOST_TO_ACTIVATE) {
            counters.put(ship, 0f);
            hasPopped.put(ship, false);
            armorFractionLostSinceLast.put(ship, armorFractionLostSinceLast.get(ship) - ARMOR_FRACTION_LOST_TO_ACTIVATE);
        }

        //------------------------------------------------------------------------------TRACKS RESIDUAL ENERGY---------------------------------------------------------------------------------------

        //Handles "residual energy", meaning if we have gotten the bonus but not deployed our shield
        if (counters.get(ship) > SECONDS_TO_LOSE_BONUS || hasPopped.get(ship)) {
            ship.getMutableStats().getShieldUnfoldRateMult().unmodify("SRD_NANOLATTICE_ID");
            return;
        } else {
            //If it's the player is in the ship, give a small indicator
            if (ship == Global.getCombatEngine().getPlayerShip()) {
                Global.getCombatEngine().maintainStatusForPlayerShip("SRD_NANOLATTICE_ID", "graphics/icons/hullsys/high_energy_focus.png", "Nanolattice Armor", "shield capacitors supercharged", false);
            }

            //Emits tiny lightning bolts from all smoke vents
            for (WeaponSlotAPI testSlot : ship.getHullSpec().getAllWeaponSlotsCopy()) {
                if (!testSlot.isSystemSlot()) {
                    continue;
                }

                //Main part handling the small lightnings: keeps a counter which depends on how many sparks we want per second
                ship.getMutableStats().getDynamic().getStat("SRD_NanolatticeTinyLightningEffectTracker").modifyFlat("SRD_NanolatticeTinyLightningEffectTrackerNullerID", -1);
                ship.getMutableStats().getDynamic().getStat("SRD_NanolatticeTinyLightningEffectTracker").modifyFlat("SRD_NanolatticeTinyLightningEffectTrackerID", ship.getMutableStats().getDynamic().getStat("SRD_NanolatticeTinyLightningEffectTracker").getModifiedValue() + amount);
                if (ship.getMutableStats().getDynamic().getStat("SRD_NanolatticeTinyLightningEffectTracker").getModifiedValue() > (1f / DECO_LIGHTNING_BOLTS_PER_SECOND)) {
                    Vector2f positionOfBolt = MathUtils.getRandomPointInCircle(testSlot.computePosition(ship), 1f);
                    Vector2f targetPoint = MathUtils.getRandomPointInCone(positionOfBolt, DECO_LIGHTNING_BOLTS_LENGTH, testSlot.getAngle() - 12f + ship.getFacing(), testSlot.getAngle() + 12f + ship.getFacing());
                    Global.getCombatEngine().spawnEmpArc(ship, positionOfBolt, ship, new SimpleEntity(targetPoint),
                            DamageType.ENERGY, //Damage type
                            ship.getFluxTracker().getMaxFlux() * MathUtils.getRandomNumberInRange(0.01f, 0.03f), //Damage
                            ship.getFluxTracker().getMaxFlux() * MathUtils.getRandomNumberInRange(0.02f, 0.05f), //Emp
                            100000f, //Max range
                            "SRD_nullspace_lightning_impact", //Impact sound
                            MathUtils.getRandomNumberInRange(9f, 16f), // thickness of the lightning bolt
                            LIGHTNING_CORE_COLOR, //Central color
                            LIGHTNING_FRINGE_COLOR //Fringe Color
                    );

                    ship.getMutableStats().getDynamic().getStat("SRD_NanolatticeTinyLightningEffectTracker").modifyFlat("SRD_NanolatticeTinyLightningEffectTrackerID", ship.getMutableStats().getDynamic().getStat("SRD_NanolatticeTinyLightningEffectTracker").getModifiedValue() - (1f / DECO_LIGHTNING_BOLTS_PER_SECOND));
                }
            }
        }

        //----------------------------------------------------------------------------HANDLES SHIELD UNFOLDING---------------------------------------------------------------------------------------

        //Shield-related bonuses: first, increase deployment speed drastically
        ship.getMutableStats().getShieldUnfoldRateMult().modifyMult("SRD_NANOLATTICE_ID", 1000f);

        //Then, if we have just deployed our shield, unleash a wave of destruction
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
                        //No more friendly fire cause I'm sick of the whining
                        if (entityToTest.getOwner() == ship.getOwner()) {
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
                            ship.getFluxTracker().getMaxFlux() * MathUtils.getRandomNumberInRange(0.007f, 0.02f), //Damage
                            ship.getFluxTracker().getMaxFlux() * MathUtils.getRandomNumberInRange(0.015f, 0.035f), //Emp
                            100000f, //Max range
                            "tachyon_lance_emp_impact", //Impact sound
                            MathUtils.getRandomNumberInRange(9f, 16f), // thickness of the lightning bolt
                            LIGHTNING_CORE_COLOR, //Central color
                            LIGHTNING_FRINGE_COLOR //Fringe Color
                    );
                    continue;
                }

                //Otherwise, we fire at a random target in the list
                CombatEntityAPI target = validTargets.get(MathUtils.getRandomNumberInRange(0, validTargets.size() - 1));
                Global.getCombatEngine().spawnEmpArc(ship, sourcePoint, ship, target,
                        DamageType.ENERGY, //Damage type
                        ship.getFluxTracker().getMaxFlux() * MathUtils.getRandomNumberInRange(0.007f, 0.02f), //Damage
                        ship.getFluxTracker().getMaxFlux() * MathUtils.getRandomNumberInRange(0.015f, 0.035f), //Emp
                        100000f, //Max range
                        "tachyon_lance_emp_impact", //Impact sound
                        MathUtils.getRandomNumberInRange(9f, 16f), // thickness of the lightning bolt
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
        if (index == 0) return "" + (int)(ARMOR_FRACTION_LOST_TO_ACTIVATE * 100f) + "%";
        if (index == 1) return "Armor Polarization";
        if (index == 2) return "" + (int)(SECONDS_TO_LOSE_BONUS) + " seconds";
        if (index == 3) return "instantly unfold";
        return null;
    }
}

