//By Nicke535, edited from the vanilla shipsystem AI by Alex Mosolov
package data.scripts.shipsystemAI;

import java.util.List;

import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.util.IntervalUtil;

public class SRD_NullspaceShuntAI implements ShipSystemAIScript {

	private ShipAPI ship;
	private CombatEngineAPI engine;
	private ShipwideAIFlags flags;
	private ShipSystemAPI system;
	
	private IntervalUtil tracker = new IntervalUtil(0.1f, 0.15f);
	
	//This is to make sure we only use the system if our "window of oppurtunity" lasts two separate calculations
	//After all, a window of opportunity 0.15 seconds long is hardly a window of opportunity
	private boolean targetIsVulnerablePrevious = false;
	
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
		if (!tracker.intervalElapsed()) {return;}

		//Don't even bother checking if the system is already active (or is on cooldown)
		if ((system.getCooldownRemaining() > 0.1f && system.isCoolingDown()) || system.isActive()) {
			targetIsVulnerablePrevious = false;
			return;
		}

		//Only check for target killing if we *have* a target
		boolean targetIsVulnerableNow = false;
		if (target != null) {
			//Calculate our total DPS against the target with fairly rudimentary calculations: ignore armor altogether (in return, we are a lot more careful about using the thing)
			float calcDPSvsTarget = 0f;
			for (WeaponAPI wpn : ship.getAllWeapons()) {
				//If our weapon doesn't have any ammo, don't count it
				if (wpn.getAmmo() <= 0) {continue;}

				//Only count the DPS of weapons our target is within arc and range of
				if (wpn.distanceFromArc(target.getLocation()) <= 0f && MathUtils.getDistance(target, wpn.getLocation()) <= wpn.getRange()) {
					float dpsFromThisWeapon = wpn.getDamage().computeDamageDealt(3f);
					if (wpn.getDamageType() == DamageType.ENERGY) {
						dpsFromThisWeapon *= target.getMutableStats().getEnergyDamageTakenMult().getModifiedValue();
					} else if (wpn.getDamageType() == DamageType.HIGH_EXPLOSIVE) {
						dpsFromThisWeapon *= target.getMutableStats().getHighExplosiveDamageTakenMult().getModifiedValue();
					} else if (wpn.getDamageType() == DamageType.KINETIC) {
						dpsFromThisWeapon *= target.getMutableStats().getKineticDamageTakenMult().getModifiedValue();
					} else if (wpn.getDamageType() == DamageType.FRAGMENTATION) {
						dpsFromThisWeapon *= target.getMutableStats().getFragmentationDamageTakenMult().getModifiedValue();
					}
					calcDPSvsTarget +=  dpsFromThisWeapon * target.getMutableStats().getHullDamageTakenMult().getModifiedValue();
				}
			}


			//If our target is vulnerable, we might use the system to kill it before it can have a chance to use our imminent overload to its own advantage
			targetIsVulnerableNow = !target.isPhased() && 														//Make sure our target isn't phased...
					target.getCollisionClass() != CollisionClass.NONE &&										//..AND they are not untargetable...
					(target.getFluxTracker().getOverloadTimeRemaining() > 4f || 								//...AND they are overloading with at least 4 seconds left
					(target.getFluxTracker().getTimeToVent() > 4f && target.getFluxTracker().isVenting()) || 	//...OR they are venting with more than 4 seconds left
					(target.getHardFluxLevel()) >  0.75f) && 			//...OR they have over 75% hard flux
					(target.getHullLevel() < 0.35f ||															//...AND they have only 35% hull left...
					target.getHitpoints() < calcDPSvsTarget);													//...OR they have less than 3 of our seconds of DPS of health left
		}

		//Now that we know the situation, determine if our flux is even high enough to bother using the system
		if (targetIsVulnerableNow && targetIsVulnerablePrevious) {
			if (ship.getFluxTracker().getFluxLevel() > 0.65f) {
				ship.useSystem();
			}
		}
		//We can also use the system "defensively", meaning when we use it to sustain more shield damage/avoid an overload (85% hard flux or 95% total flux, respectively)
		if ((ship.getHardFluxLevel()) >= 0.85f || ship.getFluxTracker().getFluxLevel() >= 0.95f) {
			ship.useSystem();
		}

		//Finally, set our "previous tick" variable to our current tick one
		targetIsVulnerablePrevious = targetIsVulnerableNow;
	}
}
