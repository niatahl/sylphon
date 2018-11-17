//By Nicke535, edited from the vanilla shipsystem AI by Alex Mosolov
//
//This AI is designed for "siege" use: it only activates with a station or capital ship in front of it, and
package data.scripts.shipsystemAI;

import com.fs.starfarer.api.characters.PersonalityAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.shipsystems.SRD_FluxExchanger;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class SRD_NullspaceAnchorAI implements ShipSystemAIScript {

	private static final float TIMID_AVOID_RANGE = 1000f;
	private static final float CAUTIOUS_AVOID_RANGE = 500f;
	private static final int REQUIRED_FAILED_CHECKS = 3;
	private static final int LOOPS_TO_WAIT_ON_ESCAPE = 35;
	private static final int LOOPS_TO_WAIT_ON_NO_TARGET = 10;

	private ShipAPI ship;
	private CombatEngineAPI engine;
	private ShipwideAIFlags flags;
	private ShipSystemAPI system;
	
	private IntervalUtil tracker = new IntervalUtil(0.25f, 0.33f);

	private int failedChecks = 0;
	private int loopsToWait = 0;
	
	public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
		this.ship = ship;
		this.flags = flags;
		this.engine = engine;
		this.system = system;
	}

	//Main loop
	@SuppressWarnings("unchecked")
	public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
		//Only run after our tracker has passed
		tracker.advance(amount);
		if (tracker.intervalElapsed()) {
			//If we are currently "waiting" with the system, don't do anything
			if (loopsToWait > 0) {
				loopsToWait--;
				return;
			}

			//Simple first check: if we are falling back, we *definitely* don't want this thing active
			if (ship.isRetreating()) {
				switchSystemStateTo(false);
				loopsToWait = LOOPS_TO_WAIT_ON_ESCAPE;
				return;
			}

			//---------------------------------"Fear" check: if we are in danger from things nearby, we deactivate. Note that Officers affect this--------------------------------------------------
			String personality = "NORMAL";
			if (ship.getCaptain() != null) {
				personality = ship.getCaptain().getPersonalityAPI().getId();
			}

			//Reckless code: ignore the fear calculation altogether!
			if (!personality.equals(Personalities.RECKLESS)) {
				//If we are timid or cautious, check so no-one is too close for comfort (ignoring fighters and ships that are overloaded, venting or dead) [cautious ignores frigates, too]
				if (personality.equals(Personalities.TIMID)) {
					for (ShipAPI potEnemy : AIUtils.getNearbyEnemies(ship, TIMID_AVOID_RANGE)) {
						if (potEnemy.getHullSize() == ShipAPI.HullSize.FIGHTER || potEnemy.getFluxTracker().isOverloadedOrVenting() || potEnemy.isHulk()) {
							continue;
						} else {
							switchSystemStateTo(false);
							loopsToWait = LOOPS_TO_WAIT_ON_ESCAPE;
							return;
						}
					}
				} else if (personality.equals(Personalities.CAUTIOUS)) {
					for (ShipAPI potEnemy : AIUtils.getNearbyEnemies(ship, CAUTIOUS_AVOID_RANGE)) {
						if (potEnemy.getHullSize() == ShipAPI.HullSize.FIGHTER || potEnemy.getHullSize() == ShipAPI.HullSize.FRIGATE || potEnemy.getFluxTracker().isOverloadedOrVenting() || potEnemy.isHulk()) {
							continue;
						} else {
							switchSystemStateTo(false);
							loopsToWait = LOOPS_TO_WAIT_ON_ESCAPE;
							return;
						}
					}
				}

				//If we are not reckless, run normal threat analysis
				if (flags.hasFlag(ShipwideAIFlags.AIFlags.IN_CRITICAL_DPS_DANGER)) {
					switchSystemStateTo(false);
					loopsToWait = LOOPS_TO_WAIT_ON_ESCAPE;
					return;
				}
			}

			//-----------------------------------------------------------------------END OF FEAR CHECK---------------------------------------------------------------------------------------------
			//Then, our "offensive" check: if we don't find any targets here, there is no reason to even keep the system active. This one is a bit slower, though: it requires several subsequent
			//failed checks to truly return false
			for (WeaponAPI weapon : ship.getAllWeapons()) {
				if (weapon.getSpec().hasTag("SRD_REQUIRES_ANCHOR")) {
					if (isTargetInLine(weapon, ship.getOwner())) {
						switchSystemStateTo(true);
						failedChecks = 0;
						return;
					}
				}
			}

			//If we get here, we failed all checks for the weapons: thus, increment our counter for failed checks
			failedChecks++;

			//Finally, if we actually have enough failed checks, deactivate
			if (failedChecks >= REQUIRED_FAILED_CHECKS) {
				switchSystemStateTo(false);
				loopsToWait = LOOPS_TO_WAIT_ON_NO_TARGET;
			}
		}
	}


	//Minor function for finding if there are valid targets in front of a weapon
	private boolean isTargetInLine (WeaponAPI weapon, int side) {
		//Get our reference points
		Vector2f wepLoc = new Vector2f(weapon.getLocation().x, weapon.getLocation().y);
		Vector2f targetLoc = MathUtils.getPointOnCircumference(wepLoc, weapon.getRange(), weapon.getCurrAngle());
		for (ShipAPI potEnemy : CombatUtils.getShipsWithinRange(wepLoc, weapon.getRange())) {
			//Ignore allies, and any ship that isn't either a station or a capital ship
			if (potEnemy.getOwner() == side || (potEnemy.getHullSize() != ShipAPI.HullSize.CAPITAL_SHIP && potEnemy.getParentStation() == null && !ship.getVariant().getHullMods().contains("vastbulk"))) {
				continue;
			}

			//Also ignore ships our fleetside cannot see
			if (!engine.getFogOfWar(side).isVisible(potEnemy)) {continue;}

			//ALSO ignore targets that are phased or un-targetable
			if (potEnemy.isPhased() || potEnemy.getCollisionClass().equals(CollisionClass.NONE)) {continue;}

			//Check angle first, as that is slightly cheaper. Ignore anything more than 60 degrees from our weapon; it's either too close or definitely too far from our line to care about
			if (Math.abs(Vector2f.angle(wepLoc, potEnemy.getLocation())) > 60f) {continue;}

			//Then, if the enemy is directly in front of the weapon, return true
			if (CollisionUtils.getCollides(wepLoc, targetLoc, potEnemy.getLocation(), potEnemy.getCollisionRadius())) {
				return true;
			}
		}
		//If no target was found, return false
		return false;
	}

	//Minor function to make it easier to switch the system on/off
	private void switchSystemStateTo (boolean shouldBeOn) {
		if (system.isActive() && !shouldBeOn) {
			ship.useSystem();
		} else if (!system.isActive() && shouldBeOn) {
			ship.useSystem();
		}
	}
}
