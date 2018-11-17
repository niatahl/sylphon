//By Nicke535, absorbs flux into the ship's internal buffer and then releases it
package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import data.scripts.SRD_ModPlugin;
import data.scripts.plugins.NicToyCustomTrailPlugin;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;

public class SRD_FluxExchanger extends BaseShipSystemScript {

    //Actual stats
    public static final float EFFECT_RANGE = 2750f;
    private static final float PART_OF_CAPACITY_ABSORBED = 0.02f;
    private static final float MAX_FLUX_ABSORBTION_PER_SECOND = 2000f;
    public static final float LIGHTNING_HALF_ARC = 35f;
    private static final float DAMAGE_PER_BOLT = 150f;

    private boolean runOnce = true;
    private float absorbedFlux = 0f;
    private boolean hasReachedFullCharge = false;

    public static Color LIGHTNING_CORE_COLOR = new Color(0.45f, 0.10f, 0.80f, 0.8f);
    public static Color LIGHTNING_FRINGE_COLOR = new Color(0.20f, 0.03f, 0.30f, 0.5f);

    //For visuals when eating flux
    private Map<ShipAPI, Float> associatedIDs = new HashMap<ShipAPI, Float>();    //This handles our trail IDs

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
        float amount = Global.getCombatEngine().getElapsedInLastFrame();

        //Saves data to be accessed by our AI script: this is done before calculations, but that shouldn't be a problem seeing as the AI script does not run every frame
        Global.getCombatEngine().getCustomData().put(ship.getId() + "SRD_FluxExchangerDataID", absorbedFlux);

        //Checks if we are at full charge
        if (effectLevel >= 1f) {
            hasReachedFullCharge = true;
        }

        //If we are charging the system, absorb flux from nearby ships with Nullspace Conduits and prevent dissipation
        if ((effectLevel >= 1f && hasReachedFullCharge) || (!hasReachedFullCharge && effectLevel >= 0)) {
            runOnce = true;
            stats.getFluxDissipation().modifyMult(id,1f - effectLevel);
            //First, get the appropriate targets
            List<ShipAPI> targetList = new ArrayList<ShipAPI>();
            for (ShipAPI target : CombatUtils.getShipsWithinRange(ship.getLocation(), EFFECT_RANGE)) {
                //Disregard any target without a nullspace conduit (and our own ship!)... and any phased target
                if (target.getVariant() == null || target == ship || target.isPhased()) {
                    continue;
                }
                if (!target.getVariant().getHullMods().contains("SRD_nullspace_stabilizer") && !target.getVariant().getHullMods().contains("SRD_nullspace_conduits") && !target.getVariant().getHullMods().contains("SRD_modular_nullspace_conduits")) {
                    continue;
                }

                //If the target is an allowed target (not overloading) we add them to our true target list
                if (!target.getFluxTracker().isOverloaded()) {
                    targetList.add(target);
                }
            }

            //Then, calculate our total dissipation this frame (as it is a percentage of all targets' flux capacity in range)
            float dissipationThisTick = 0f;
            for (ShipAPI target : targetList) {
                dissipationThisTick += target.getFluxTracker().getMaxFlux() * PART_OF_CAPACITY_ABSORBED * amount * effectLevel;

                if (dissipationThisTick >= MAX_FLUX_ABSORBTION_PER_SECOND * amount * effectLevel) {
                    dissipationThisTick = MAX_FLUX_ABSORBTION_PER_SECOND * amount * effectLevel;
                    break;
                }
            }

            //Finally, actually eat the flux of nearby ships
            float dissipationPerTarget = dissipationThisTick / (float)targetList.size();
            for (ShipAPI target : targetList) {
                if (target.getFluxTracker().getCurrFlux() < dissipationPerTarget) {
                    absorbedFlux += target.getFluxTracker().getCurrFlux();
                    target.getFluxTracker().setCurrFlux(0f);
                } else {
                    absorbedFlux += dissipationPerTarget;
                    target.getFluxTracker().decreaseFlux(dissipationPerTarget);
                }

                //Spawns some visuals to indicate we are actually eating flux from the thing: spawn a quad trail
                SpriteAPI spriteToUse = Global.getSettings().getSprite("SRD_fx", "flux_exchanger_strip");
                if (associatedIDs.get(target) == null) {
                    associatedIDs.put(target, NicToyCustomTrailPlugin.getUniqueID());
                }

                //If we are almost out of range, start fading out the trail visuals (also do this if we have very low flux)
                float opacityMult = 1f;
                if (MathUtils.getDistance(target.getLocation(), ship.getLocation()) > EFFECT_RANGE * 0.95f) {
                    opacityMult *= (MathUtils.getDistance(target.getLocation(), ship.getLocation()) - (EFFECT_RANGE * 0.95f)) / (EFFECT_RANGE * 0.05f);
                }
                if (target.getFluxTracker().getFluxLevel() < 0.1f) {
                    opacityMult *= 0.3f + (target.getFluxTracker().getFluxLevel() * 7f);
                }

                float stripSpeed = MathUtils.getDistance(target.getLocation(), ship.getLocation()) / 0.11f;
                float angleOfStrip = VectorUtils.getAngle(target.getLocation(), ship.getLocation());
                NicToyCustomTrailPlugin.AddTrailMemberAdvanced(target, associatedIDs.get(target), spriteToUse, target.getLocation(), stripSpeed, stripSpeed, angleOfStrip,
                        0f, 0f, target.getCollisionRadius() * 2f, ship.getCollisionRadius() * 4f, target.getVentCoreColor(), target.getVentFringeColor(),
                        effectLevel * opacityMult, 0.1f,0f, 0.01f, GL_SRC_ALPHA, GL_ONE, 512f, 1300f);
            }
        }

        //If we are powering the system down, and have actually powered it up before, trigger our unapply and fire away!
        else if (runOnce && effectLevel < 1f && hasReachedFullCharge) {
            unapply(stats, id);
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


        //If we have yet to fire our pulse, fire the pulse
        if (runOnce) {
            runOnce = false;

            //First, get the appropriate targets
            List<ShipAPI> targetList = new ArrayList<ShipAPI>();
            for (ShipAPI target : CombatUtils.getShipsWithinRange(ship.getLocation(), EFFECT_RANGE)) {
                //Disregard any target with a nullspace conduit (and our own ship!)... and any phased target
                if (target.getVariant() == null || target == ship || target.isPhased()) {
                    continue;
                }
                boolean shouldContinue = false;
                for (String s : target.getVariant().getHullMods()) {
                    if (SRD_ModPlugin.NULLSPACE_CONDUIT_HULLMODS.contains(s)) {
                        shouldContinue = true;
                        break;
                    }
                }
                if (shouldContinue) {
                    continue;
                }

                //Add our target to the list, if it is in a frontal arc
                float angleToTarget = VectorUtils.getAngle(ship.getLocation(), target.getLocation());
                if (angleToTarget <= ship.getFacing() + LIGHTNING_HALF_ARC && angleToTarget >= ship.getFacing() - LIGHTNING_HALF_ARC) {
                    targetList.add(target);
                }
            }

            //After that, determine how many bolts of energy we will fire (this is rounded down, since if we have too little energy to fire a bolt, we shouldn't fire one)
            int boltsToFire = (int)Math.floor(absorbedFlux / DAMAGE_PER_BOLT);
            absorbedFlux = 0f;

            //Lastly, enter a loop until all our bolts have been used up; pick a random target and fire a bolt at it, and then restart the loop
            while (boltsToFire > 0) {
                boltsToFire--;

                //If we don't have any targets, we simply stop the script here
                if (targetList.isEmpty()) {break;}

                ShipAPI target = targetList.get(MathUtils.getRandomNumberInRange(0, targetList.size()-1));
                if (target != null) {
                    //Gets a random vent to fire from
                    Vector2f pointToShootFrom = getRandomVent(ship);

                    //And then actually spawns the lightning
                    Global.getCombatEngine().spawnEmpArc(ship, pointToShootFrom, ship, target,
                            DamageType.ENERGY, //Damage type
                            MathUtils.getRandomNumberInRange(0.98f, 1.02f) * DAMAGE_PER_BOLT * target.getMutableStats().getDynamic().getStat("SRD_NULLSPACE_DAMAGE_MULT").getModifiedValue(), //Damage
                            MathUtils.getRandomNumberInRange(0.98f, 1.02f) * DAMAGE_PER_BOLT * target.getMutableStats().getDynamic().getStat("SRD_NULLSPACE_DAMAGE_MULT").getModifiedValue(), //Emp
                            100000f, //Max range
                            "SRD_nullspace_lightning_impact", //Impact sound
                            (float)Math.sqrt(DAMAGE_PER_BOLT), // thickness of the lightning bolt
                            LIGHTNING_CORE_COLOR, //Central color
                            LIGHTNING_FRINGE_COLOR//Fringe Color
                    );
                }
            }
        }

        //Reset variables
        absorbedFlux = 0f;
        associatedIDs.clear();
        stats.getFluxDissipation().unmodify(id);
        hasReachedFullCharge = false;
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0 && effectLevel >= 1f) {
            return new StatusData("" + (int)absorbedFlux + " flux absorbed", false);
        } else if (index == 0 && effectLevel < 1f && state.equals(State.OUT)){
            return new StatusData("unleashing flux pulse", false);
        }
        return null;
    }

    //Tiny utility function to get a random nullspace vent
    private Vector2f getRandomVent(ShipAPI ship) {
        List<WeaponSlotAPI> slots = new ArrayList<WeaponSlotAPI>();
        for (WeaponSlotAPI slot : ship.getHullSpec().getAllWeaponSlotsCopy()) {
            if (slot.isSystemSlot()) {
                slots.add(slot);
            }
        }

        return slots.get(MathUtils.getRandomNumberInRange(0, slots.size()-1)).computePosition(ship);
    }
}