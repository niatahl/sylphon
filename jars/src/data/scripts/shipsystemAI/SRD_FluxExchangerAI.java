//By Nicke535, edited from the vanilla shipsystem AI by Alex Mosolov
package data.scripts.shipsystemAI;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.shipsystems.SRD_FluxExchanger;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class SRD_FluxExchangerAI implements ShipSystemAIScript {

	private ShipAPI ship;
	private CombatEngineAPI engine;
	private ShipwideAIFlags flags;
	private ShipSystemAPI system;
	
	private IntervalUtil tracker = new IntervalUtil(0.06f, 0.10f);
	
	public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
		this.ship = ship;
		this.flags = flags;
		this.engine = engine;
		this.system = system;
	}
	
	@SuppressWarnings("unchecked")
	public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
		//Only run after our tracker has passed
		tracker.advance(amount);
		if (tracker.intervalElapsed()) {

			//Shorthand declaration
			FluxTrackerAPI ftrack = ship.getFluxTracker();

			//If the system is inactive, we run pre-activation checks
			if (!system.isActive()) {
				//Never start the system if we have more than 80% flux: it just won't earn us anything at that point
				if (ftrack.getFluxLevel() > 0.8f) { return; }

				//Also avoid using the system above 70% hard flux: we don't have much time to charge, so it's probably a lot more risky than we would like
				if (ftrack.getHardFlux() / ftrack.getMaxFlux() > 0.7f) { return; }

				//Otherwise, calculate a weight, depending on nearby ships
				float weight = 0;
				for (ShipAPI potTarget : CombatUtils.getShipsWithinRange(ship.getLocation(), SRD_FluxExchanger.EFFECT_RANGE)) {
					//Ignore anything without Nullspace Conduits equipped (and ourselves)
					if (!potTarget.getVariant().getHullMods().contains("SRD_nullspace_stabilizer") && !potTarget.getVariant().getHullMods().contains("SRD_nullspace_conduits") && !potTarget.getVariant().getHullMods().contains("SRD_modular_nullspace_conduits")) {
						continue;
					}
					if (potTarget == ship) {
						continue;
					}

					//If our target is an enemy, remove 0.3 from the counter: if it's an ally, add 1 (ignore fighters entirely)
					//Also drastically reduce the weight addition if the ship in question has very low flux (<10%) and remove it entirely if it has no flux
					if (potTarget.getOwner() == ship.getOwner()) {
						if (potTarget.getHullSize() != ShipAPI.HullSize.FIGHTER) {
							if (potTarget.getFluxTracker().getFluxLevel() < 0.1f) {
								weight += 1f * potTarget.getFluxTracker().getFluxLevel();
							} else {
								weight += 1f;
							}
						}
					} else {
						if (potTarget.getHullSize() != ShipAPI.HullSize.FIGHTER) {
							if (potTarget.getFluxTracker().getFluxLevel() < 0.1f) {
								weight -= 0.15f;
							} else {
								weight -= 0.3f;
							}
						}
					}
				}

				//If we have zero-flux bonus, we lower our weight considerably, to avoid losing it unnecessarily
				if (ftrack.getFluxLevel() <= 0 && ship.getShield().isOff()) {
					weight -= 5f;
				}

				//Then, compare to our current hard flux: we should activate at higher flux the more ships we have in range of the thing
				float levelToStart = 0.1f;
				levelToStart *= Math.sqrt(Math.abs(weight)) * Math.signum(weight);

				//If we have low enough hard flux, we have a chance to activate
				if (ftrack.getHardFlux() / ftrack.getMaxFlux() < levelToStart) {
					//Each time the system detects a favorable activation opportunity, it has a 10% chance of activating, so the AI might decide not to use it even when favorable
					if (Math.random() < 0.1f) {
						ship.useSystem();
					}
				}
			}

			//If the system is already active, we instead run checks to determine when we de-activate the thing
			else {
				//If we are above 95% hard flux, immediately disengage the system; it's simply too high for our own good, regardless of circumstance
				if (ftrack.getHardFlux() / ftrack.getMaxFlux() > 0.95f) {
					ship.useSystem();
					return;
				}

				//Otherwise, we wait for a good oppurtunity: find all targets in a cone in front of us
				float weight = 0;
				for (ShipAPI potEnemy : CombatUtils.getShipsWithinRange(ship.getLocation(), SRD_FluxExchanger.EFFECT_RANGE)) {
					//Ignore anything with Nullspace Conduits equipped
					if (potEnemy.getVariant().getHullMods().contains("SRD_nullspace_stabilizer") || potEnemy.getVariant().getHullMods().contains("SRD_nullspace_conduits") || potEnemy.getVariant().getHullMods().contains("SRD_modular_nullspace_conduits")) {
						continue;
					}

					//Ensures the target in question is in the correct firing cone
					float angleToEnemy = VectorUtils.getAngle(ship.getLocation(), potEnemy.getLocation());
					if (angleToEnemy <= ship.getFacing() + SRD_FluxExchanger.LIGHTNING_HALF_ARC && angleToEnemy >= ship.getFacing() - SRD_FluxExchanger.LIGHTNING_HALF_ARC) {
						//If our target is an enemy, add 1 to the counter (0.2 for fighters): if it's an ally, remove 6 (0.3 for fighters)
						if (potEnemy.getOwner() != ship.getOwner()) {
							if (potEnemy.getHullSize() != ShipAPI.HullSize.FIGHTER) {
								weight += 1f;
							} else {
								weight += 0.2f;
							}
						} else {
							if (potEnemy.getHullSize() != ShipAPI.HullSize.FIGHTER) {
								weight -= 6f;
							} else {
								weight -= 0.3f;
							}
						}
					}
				}
				//If our enemies outnumber our allies pretty convincingly, their numbers don't really matter: set it to 4f maximum
				if (weight > 4f) {
					weight = 4f;
				}

				//Now that we have a weight, we can start comparing it to the damage we have
				//First, check our map: we can't really use the system without it, so if it doesn't exist, just skip this execution
				float storedFlux = 0f;
				if (engine.getCustomData().get(ship.getId() + "SRD_FluxExchangerDataID") instanceof Float) {
					storedFlux = (float)engine.getCustomData().get(ship.getId() + "SRD_FluxExchangerDataID");
				} else {
					return;
				}

				//Then, we determine which flux level the system should be activated at using some weird mathematical formula
				float levelToUseAt = 0.80f;

				//This piece means that at 40000 (200^2) flux stored, and only one target, we activate at 80% - 60% = 20% hard flux
				float mathMod = (float) Math.sqrt(Math.abs(storedFlux));
				mathMod *= Math.signum(storedFlux) * 0.6f * weight;
				mathMod /= 200f;
				levelToUseAt -= mathMod;

				//Then, actually compare our result to our flux level: 3 cases possible
				//Case 1: we have less than 10% hard flux; we have barely begun charging! Don't deactivate quite yet
				if (ftrack.getHardFlux() / ftrack.getMaxFlux() < 0.1f) {
					return;
				}
				//Case 2: we don't have enough flux to warrant using the system with our current targets (maybe there are too many friendlies in the area?). Don't deactivate
				if (ftrack.getHardFlux() / ftrack.getMaxFlux() < levelToUseAt) {
					return;
				}
				//Case 3: we have enough hard flux to warrant a deactivation: deactivate and fire away!
				ship.useSystem();
			}
		}
	}
}
