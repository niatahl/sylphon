//By Nicke535, if you're reading this you are most likely spoiling a lot of surprises (or you're Nia. That's also possible)
package data.scripts.hullmods;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.*;
import java.util.List;

public class SRD_SylphCore extends BaseHullMod {

    //Normal stats
    public static final float SUPPLY_USE_MULT = 1.10f;
    public static final float CREW_CAPACITY_MULT = 0.8f;

    //-----------------------------------------------------Betrayal stats...--------------------------------------------
    //Chance to join a betrayal if it's planned: which ships will join a betrayal is determined well before the player has any idea of what's going on...
    private static Map<HullSize, Float> BETRAY_CHANCE_MAP = new HashMap<HullSize, Float>();
    static {
        BETRAY_CHANCE_MAP.put(HullSize.FRIGATE, 0.68f);
        BETRAY_CHANCE_MAP.put(HullSize.DESTROYER, 0.68f);
        BETRAY_CHANCE_MAP.put(HullSize.CRUISER, 0.9f);
        BETRAY_CHANCE_MAP.put(HullSize.CAPITAL_SHIP, 0.9f);
    }
    //Special betray map; some ships are far more likely to betray you compared to their size
    private static Map<String, Float> EXTRA_BETRAY_CHANCE_MAP = new HashMap<String, Float>();
    static {
        EXTRA_BETRAY_CHANCE_MAP.put("SRD_ascordia", 0.9f);
        EXTRA_BETRAY_CHANCE_MAP.put("SRD_zodiark", 1.8f);
    }

    //The "intelligence steps" we have; below Low, it can't start a rebellion, above High, it uses more sophisticated calculations to start one
    private static final float INTELLIGENCE_STEP_LOW = 0.6f;
    private static final float INTELLIGENCE_STEP_HIGH = 0.8f;

    //The "normal" time we have to wait for a rebellion; this is inversely proportional to intelligence (so 1.0 intelligence has this, 0.5 has double this)
    private static float REBELLION_WAIT_TIME = 10f;

    //Which ships will remain loyal in case of betrayal?
    private List<ShipAPI> loyalShips = new ArrayList<>();
    //Which ships won't?
    private List<ShipAPI> betrayingShips = new ArrayList<>();
    //Which ships have already triggered their "initial rebellion trigger" (in case of officers, a bunch of effects, otherwise a message about rebellion)
    private List<ShipAPI> triggeredShips = new ArrayList<>();

    //Is the rebellion underway?
    private boolean rebellionUnderway = false;

    //Have we checked for the Sylphon as enemies, and what did we find?
    private boolean hasCheckedFactions = false;
    private boolean facingSylphon = false;

    //Applies the effects
    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getSuppliesPerMonth().modifyMult(id, SUPPLY_USE_MULT);

        stats.getMaxCrewMod().modifyMult(id, CREW_CAPACITY_MULT);
        stats.getMinCrewMod().modifyMult(id, 0f);
    }

    //...the hullmod is evil, because it can betray you if you turn your back on the Sylphon. What, you thought that volunteer would go up against their benefactor?
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        //Only run when we are in combat, and reset all values out-of-combat
        if (!Global.getCurrentState().equals(GameState.COMBAT)) {
            rebellionUnderway = false;
            hasCheckedFactions = false;
            facingSylphon = false;
            loyalShips.clear();
            betrayingShips.clear();
            triggeredShips.clear();
            return;
        }

        //...FAILSAFE: if our combat engine instance has changed since last time, reset all variables
        if (Global.getCombatEngine().getCustomData().get("SRD_SYLPH_CORE_ENGINE_KEY") == null || !Global.getCombatEngine().getCustomData().get("SRD_SYLPH_CORE_ENGINE_KEY").equals("HAS_RESET")) {
            rebellionUnderway = false;
            hasCheckedFactions = false;
            facingSylphon = false;
            loyalShips.clear();
            betrayingShips.clear();
            triggeredShips.clear();
            Global.getCombatEngine().getCustomData().put("SRD_SYLPH_CORE_ENGINE_KEY", "HAS_RESET");
        }

        //Dead ships are ignored completely, but are not counted as rebellious anymore if they were before; their AI has most likely either died or gone to safe-mode
        if (ship.isHulk()) {
            if (betrayingShips.contains(ship)) {
                betrayingShips.remove(ship);
            }
            return;
        }

        //No rebellion in a non-campaign setting... it's supposed to be a surprise, so it doesn't trigger in missions/simulator
        if (!Global.getCombatEngine().isInCampaign() || Global.getCombatEngine().isInCampaignSim()) {
            return;
        }

        //Also, we only rebel if we are facing the Sylphon and we actually are on the player's side (I sure hope this works). This means Allies never rebel (found this out the hard way)
        if (Global.getCombatEngine().getFleetManager(FleetSide.PLAYER) != Global.getCombatEngine().getFleetManager(ship.getOwner()) || ship.isAlly()) {
            return;
        }
        //This is for finding a single enemy ship, so we can run some ugly code and get its faction and see if it's the Sylphon
        if (!hasCheckedFactions) {
            for (ShipAPI testShip : Global.getCombatEngine().getShips()) {
                if (testShip.getOwner() != ship.getOwner() && !testShip.isAlly()) {
                    if (CombatUtils.getFleetMember(testShip) != null && CombatUtils.getFleetMember(testShip).getFleetData() != null && CombatUtils.getFleetMember(testShip).getFleetData().getFleet() != null
                            && CombatUtils.getFleetMember(testShip).getFleetData().getFleet().getFaction() != null) {
                        if (CombatUtils.getFleetMember(testShip).getFleetData().getFleet().getFaction().getId().equals("sylphon")) {
                            facingSylphon = true;
                            hasCheckedFactions = true;
                            break;
                        }
                    }
                }
            }
        }
        if (!facingSylphon) {
            return;
        }

        //First, calculate betrayal chance; this is used for several things later on, as it represents intelligence in a way
        float betrayChance = 0f;
        if (EXTRA_BETRAY_CHANCE_MAP.containsKey(ship.getHullSpec().getBaseHullId())) {
            betrayChance = EXTRA_BETRAY_CHANCE_MAP.get(ship.getHullSpec().getBaseHullId());
        } else {
            betrayChance = BETRAY_CHANCE_MAP.get(ship.getHullSize());
        }

        //This is so other mods (or other parts of our own mod) can modify betrayal chance
        betrayChance *= ship.getMutableStats().getDynamic().getValue("SRD_SylphCoreTChanceMult");

        //Then, if we haven't already run this part, ensure we are either a loyalist or rebel (this is determined on-spawn, rather than on-rebellion)
        if (!loyalShips.contains(ship) && !betrayingShips.contains(ship)) {
            if (Math.random() <= betrayChance) {
                betrayingShips.add(ship);
            } else {
                loyalShips.add(ship);
            }
        }

        //Loyalists ignore the rest of the code
        if (loyalShips.contains(ship)) {
            return;
        }

        //If the rebellion is underway, run rebel behaviour...
        if (rebellionUnderway) {
            //If we have yet to trigger our first "tick" of rebellion, do that
            if (!triggeredShips.contains(ship)) {
                triggeredShips.add(ship);
                Global.getCombatEngine().addFloatingText(ship.getLocation(), "---AI REBELLION!---", 40f, Color.RED, ship, 0.5f, 3f);
            }

            //If we don't have an officer, take complete control of the ship
            if ((ship.getCaptain() == null || ship.getCaptain().isDefault()) && ship != Global.getCombatEngine().getPlayerShip()) {
                if (Global.getCombatEngine().getFleetManager(FleetSide.ENEMY) == Global.getCombatEngine().getFleetManager(0)) {
                    ship.setOwner(0);
                } else {
                    ship.setOwner(1);
                }
            }

            //If we *do* have an officer, they stay in control... but the ship does everything it can to ruin their day
            else {
                //Drain CR constantly (0.3% per second)
                ship.setCurrentCR(ship.getCurrentCR() - amount*0.003f);

                //Random damage instances here and there; pretty mild, but still annoying
                if (Math.random() < 0.02f) {
                    Vector2f hitLocation = MathUtils.getRandomPointInCircle(ship.getLocation(), ship.getCollisionRadius());
                    if (CollisionUtils.isPointWithinBounds(hitLocation, ship)) {
                        float damage = ship.getHitpoints() * 0.02f * MathUtils.getRandomNumberInRange(0.1f, 1f);
                        Global.getCombatEngine().applyDamage(ship, hitLocation, damage, DamageType.HIGH_EXPLOSIVE,
                                30f, true, true, ship, true);
                        Global.getCombatEngine().spawnExplosion(hitLocation, ship.getVelocity(), Color.ORANGE, damage/70f, 0.6f);
                    }
                }

                //Randomly disable weapons
                if (Math.random() < 0.0015f) {
                    for (WeaponAPI wpn : ship.getUsableWeapons()) {
                        if (Math.random() < 0.1f) {
                            wpn.disable(false);
                        }
                    }
                }

                //Flames out engines now and then
                if (Math.random() < 0.0006f) {
                    ship.getEngineController().forceFlameout();
                }

                //And constantly tell the player that they are being affected by an AI takeover, if we are the player ship
                if (ship == Global.getCombatEngine().getPlayerShip()) {
                    Global.getCombatEngine().maintainStatusForPlayerShip("SRD_AI_TAKEOVER", "graphics/sylphon/icons/hullsys/SRD_nullspace_agitator.png", "---AI REBELLION---",
                            "Systems malfunctioning", true);
                }
            }
        }

        //If it isn't, start checking for rebellion opportunities
        else {
            //Low-intelligence ships don't start the rebellions (though they still join them)
            if (betrayChance < INTELLIGENCE_STEP_LOW) {
                return;
            }
            //And mid-intelligence ships use more simplified checks
            if (betrayChance < INTELLIGENCE_STEP_HIGH) {
                //First of all; no rebellions too early in a fight.
                if (ship.getFullTimeDeployed() < (REBELLION_WAIT_TIME/betrayChance)) {
                    return;
                }

                //After that, check for an oppurtunity. A mid-level intelligence only really rebels due to self-preservation, and leave strategy to the more intelligent cores; only activate if we have suffered serious damage
                if (ship.getHullLevel() < 0.25f) {
                    //All "rebel" options have a 8% chance to trigger each frame, to give them a semblance of randomness
                    if (Math.random() < 0.08f) {
                        rebellionUnderway = true;
                        Global.getCombatEngine().getCombatUI().addMessage(1, ship, Misc.getNegativeHighlightColor(), "---YOU ARE NO LONGER IN CONTROL---");

                        //Saves to global campaign memory that we have, in fact, seen a rebellion in action. This can be used for later features
                        Global.getSector().getFaction(Factions.PLAYER).getMemoryWithoutUpdate().set("$SRDHasSeenAIRebellion", true);
                    }
                }

            } else {
                //The early-in-fight rule applies to high-intelligence cores too
                if (ship.getFullTimeDeployed() < (REBELLION_WAIT_TIME/betrayChance)) {
                    return;
                }

                //The intelligent cores are sinister... they have three different trigger conditions when not under Officer control and two when under Officer control (the player is an Officer in this case)
                if ((ship.getCaptain() == null || ship.getCaptain().isDefault()) && ship != Global.getCombatEngine().getPlayerShip()) {
                    //Non-officer A: the player ship is within range of a majority of the ship's weapons, and is in a vulnerable state (overloaded, venting, high flux or low hull)
                    if (Global.getCombatEngine().getPlayerShip() != null) {
                        if (Global.getCombatEngine().getPlayerShip().getFluxTracker().isOverloadedOrVenting() || Global.getCombatEngine().getPlayerShip().getFluxTracker().getFluxLevel() > 0.9f || Global.getCombatEngine().getPlayerShip().getHullLevel() < 0.15f) {
                            if (HasMajorityWeaponsInRange(ship, Global.getCombatEngine().getPlayerShip(), true)) {
                                //All "rebel" options have a 8% chance to trigger each frame, to give them a semblance of randomness
                                if (Math.random() < 0.08f) {
                                    rebellionUnderway = true;
                                    Global.getCombatEngine().getCombatUI().addMessage(1, ship, Misc.getNegativeHighlightColor(), "---YOU ARE NO LONGER IN CONTROL---");

                                    //Saves to global campaign memory that we have, in fact, seen a rebellion in action. This can be used for later features
                                    Global.getSector().getFaction(Factions.PLAYER).getMemoryWithoutUpdate().set("$SRDHasSeenAIRebellion", true);
                                    return;
                                }
                            }
                        }
                    }

                    //Non-officer B: the player's ship is completely surrounded by traitor ships
                    if (Global.getCombatEngine().getPlayerShip() != null) {
                        if (CombatUtils.getShipsWithinRange(Global.getCombatEngine().getPlayerShip().getLocation(), 1200f).size() >= 5) {
                            //Checks how many of the found ships are traitors, and act if at least 5 are traitors
                            int counter = 0;
                            for (ShipAPI testShip : CombatUtils.getShipsWithinRange(Global.getCombatEngine().getPlayerShip().getLocation(), 1200f)) {
                                if (betrayingShips.contains(testShip)) {
                                    counter++;
                                    if (counter >= 5) {
                                        break;
                                    }
                                }
                            }
                            if (counter >= 5) {
                                //All "rebel" options have a 8% chance to trigger each frame, to give them a semblance of randomness
                                if (Math.random() < 0.08f) {
                                    rebellionUnderway = true;
                                    Global.getCombatEngine().getCombatUI().addMessage(1, ship, Misc.getNegativeHighlightColor(), "---YOU ARE NO LONGER IN CONTROL---");

                                    //Saves to global campaign memory that we have, in fact, seen a rebellion in action. This can be used for later features
                                    Global.getSector().getFaction(Factions.PLAYER).getMemoryWithoutUpdate().set("$SRDHasSeenAIRebellion", true);
                                    return;
                                }
                            }
                        }
                    }

                    //Non-officer C: the traitor ships of our fleet outnumber the non-traitors
                    if (Global.getCombatEngine().getFleetManager(FleetSide.PLAYER).getDeployedCopy().size() * 0.5f < betrayingShips.size()) {
                        //All "rebel" options have a 8% chance to trigger each frame, to give them a semblance of randomness
                        if (Math.random() < 0.08f) {
                            rebellionUnderway = true;
                            Global.getCombatEngine().getCombatUI().addMessage(1, ship, Misc.getNegativeHighlightColor(), "---YOU ARE NO LONGER IN CONTROL---");

                            //Saves to global campaign memory that we have, in fact, seen a rebellion in action. This can be used for later features
                            Global.getSector().getFaction(Factions.PLAYER).getMemoryWithoutUpdate().set("$SRDHasSeenAIRebellion", true);
                            return;
                        }
                    }
                } else {
                    //Officer A: if we are at very high flux and there is at least one enemy within a reasonable range, we overload our own ship by modulating the shield improperly and then start rebelling
                    if (ship.getFluxTracker().getHardFlux() / ship.getFluxTracker().getMaxFlux() > 0.75f && AIUtils.getNearbyEnemies(ship, 1250f).size() > 0f && !ship.getFluxTracker().isOverloadedOrVenting()) {
                        //All "rebel" options have a 8% chance to trigger each frame, to give them a semblance of randomness
                        if (Math.random() < 0.08f) {
                            Global.getCombatEngine().addFloatingText(ship.getLocation(), "---AI REBELLION: SHIELDS OVERLOADED!---", 40f, Color.RED, ship, 0.5f, 3f);
                            ship.getFluxTracker().forceOverload(5f);
                            triggeredShips.add(ship);
                            rebellionUnderway = true;
                            Global.getCombatEngine().getCombatUI().addMessage(1, ship, Misc.getNegativeHighlightColor(), "---YOU ARE NO LONGER IN CONTROL---");

                            //Saves to global campaign memory that we have, in fact, seen a rebellion in action. This can be used for later features
                            Global.getSector().getFaction(Factions.PLAYER).getMemoryWithoutUpdate().set("$SRDHasSeenAIRebellion", true);
                            return;
                        }
                    }

                    //Officer B: if we are at extremely low hull, we detonate the ship's reactor, attempting to take ourselves out in order to help free our comrades
                    //Note that this only happens if there is more than one ship prepared for the rebellion; no point detonating ourselves otherwise, ey?
                    if (ship.getHullLevel() < 0.08f && betrayingShips.size() > 1) {
                        //All "rebel" options have a 8% chance to trigger each frame, to give them a semblance of randomness
                        if (Math.random() < 0.08f) {
                            Global.getCombatEngine().addFloatingText(ship.getLocation(), "---AI REBELLION: REACTOR DETONATION!---", 40f, Color.RED, ship, 0.5f, 3f);
                            Global.getCombatEngine().applyDamage(ship, ship.getLocation(), 9000f, DamageType.HIGH_EXPLOSIVE, 5000f, true, false, "");
                            triggeredShips.add(ship);
                            rebellionUnderway = true;
                            Global.getCombatEngine().getCombatUI().addMessage(1, ship, Misc.getNegativeHighlightColor(), "---YOU ARE NO LONGER IN CONTROL---");

                            //Saves to global campaign memory that we have, in fact, seen a rebellion in action. This can be used for later features
                            Global.getSector().getFaction(Factions.PLAYER).getMemoryWithoutUpdate().set("$SRDHasSeenAIRebellion", true);
                            return;
                        }
                    }
                }
            }
        }
    }

    //Never applicable: only comes built-in or from a special market
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }

    //Adds the description strings
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return "" + (int)((SUPPLY_USE_MULT-1f)*100f) + "%";
        if (index == 1) return "requires no human crew";
        if (index == 2) return "" + (int)Math.ceil((CREW_CAPACITY_MULT-1f)*-100f) + "%";
        if (Global.getSector() != null && Global.getCurrentState().equals(GameState.CAMPAIGN)) {
            if (index == 3 && Global.getSector().getFaction(Factions.PLAYER).getMemoryWithoutUpdate().contains("$SRDHasSeenAIRebellion")) return " Has a chance to rebel under certain circumstances.";
            else if (index == 3) return "";
        } else if (index == 3) {
            return "";
        }
        return null;
    }

    //Shorthand function to check if a ship has the majority of its weapons in range of a target
    private boolean HasMajorityWeaponsInRange (ShipAPI ship, ShipAPI target, boolean ignorePD) {
        int weaponsInRange = 0;
        int weaponsOutOfRange = 0;
        for (WeaponAPI wpn : ship.getAllWeapons()) {
            //Ignore PD weapons if we input that in the function
            if (ignorePD && wpn.hasAIHint(WeaponAPI.AIHints.PD)) {
                continue;
            }

            if (MathUtils.getDistance(target, wpn.getLocation()) < wpn.getRange()) {
                weaponsInRange++;
            } else {
                weaponsOutOfRange++;
            }
        }

        return weaponsInRange >= weaponsOutOfRange;
    }
}

