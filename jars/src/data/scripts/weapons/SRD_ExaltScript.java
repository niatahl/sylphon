//By Nicke535, makes a weapon fire a different weapon's projectile (with weapon "yourweapon_EMP") every X shots
package data.scripts.weapons;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import data.scripts.plugins.SRD_SpriteRenderPlugin;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SRD_ExaltScript implements EveryFrameWeaponEffectPlugin {

    private List<DamagingProjectileAPI> registeredMissiles = new ArrayList<DamagingProjectileAPI>();

    //For laser visuals: first number is small mount, second medium and third large
    private static final Float[] X_OFFSETS_TURRET = {-6.5f, 0f, 0f};
    private static final Float[] Y_OFFSETS_TURRET = {5f, 3f, 5f};
    private static final Float[] X_OFFSETS_HARDPOINT = {-6.5f, 0f, 0f};
    private static final Float[] Y_OFFSETS_HARDPOINT = {8f, 6.5f, 8f};

    //For laser visuals: first is short, second medium and third long
    private static final Color[] COLORS = {new Color(255, 0, 0, 200), new Color(255,100,255, 200), new Color(100, 100, 255, 200)};

    //For actual script stats and stuff
    private static final String ID_MAIN = "SRD_exalt";
    private static final String[] ID_ADDONS = {"_SHORT", "_MEDIUM", "_LONG"};
    private static final Float[] RANGE_BRACKET_LIMITS = {750f, 2000f};

    //Used so our missiles can find the correct target when fired
    public ShipAPI currentTarget = null;

    //For targeting
    private float rangeToTarget = 0f;
    private int rangeBracket = 0;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        //Don't run if our weapon is null
        if (weapon == null) {
            return;
        }

        //Show a targeting laser if we are currently firing, or have less than 2 seconds left of our cooldown (also, don't show a laser when we are disabled, such as during phase or overload)
        if ((weapon.getCooldownRemaining() < 2f || weapon.getChargeLevel() >= 1f) && !weapon.getShip().isPhased() && !weapon.getShip().getFluxTracker().isOverloadedOrVenting() && !weapon.getShip().isHulk()) {
            SpriteAPI spriteToUse = Global.getSettings().getSprite("SRD_fx", "laser_targeter");
            int size = 0;
            if (weapon.getSize() == WeaponAPI.WeaponSize.MEDIUM) {
                size = 1;
            } else if (weapon.getSize() == WeaponAPI.WeaponSize.LARGE) {
                size = 2;
            }

            //Gets our laser offset
            Vector2f laserLocation = weapon.getLocation();
            if (weapon.getSlot().isHardpoint()) {
                laserLocation.y += X_OFFSETS_HARDPOINT[size]; //Somewhat misleading, since the weapon is turned 90 degrees incorrectly when considering coordinates
                laserLocation.x += Y_OFFSETS_HARDPOINT[size];
            } else {
                laserLocation.y += X_OFFSETS_TURRET[size];
                laserLocation.x += Y_OFFSETS_TURRET[size];
            }
            laserLocation = VectorUtils.rotateAroundPivot(laserLocation, weapon.getLocation(), weapon.getCurrAngle(), weapon.getLocation());

            SRD_SpriteRenderPlugin.singleFrameRender(spriteToUse, laserLocation, new Vector2f(9f, 210f), weapon.getCurrAngle() - 90f,
                    COLORS[rangeBracket], true);
        }

        //Run targeting when we aren't firing: once we fire, the targeting is "locked in"
        if (weapon.getChargeLevel() < 1f) {
            //Detects the closest enemy in a straight line
            rangeToTarget = 0f;
            rangeBracket = 0;
            Vector2f pointToLookAt = MathUtils.getPointOnCircumference(weapon.getLocation(), weapon.getRange(), weapon.getCurrAngle());
            currentTarget = null;
            for (ShipAPI potTarget : CombatUtils.getShipsWithinRange(weapon.getLocation(), weapon.getRange())) {
                //Ignore our allies, and any wrecks we come across. Also ignore any phased enemies and enemies with no collision class: lasers don't track through phase!
                if (potTarget.getOwner() == weapon.getShip().getOwner() || potTarget.isHulk() || potTarget.isPhased() || potTarget.getCollisionClass() == CollisionClass.NONE) {
                    continue;
                }
                //Also, ignore fighters: we just don't want to shoot them with this thing anyway
                if (potTarget.getHullSize().equals(ShipAPI.HullSize.FIGHTER)) {
                    continue;
                }

                //Only care about targets in a straight line from the weapon
                if (CollisionUtils.getCollides(weapon.getLocation(), pointToLookAt, potTarget.getLocation(), potTarget.getCollisionRadius())) {
                    //Choose the closest target of the valid ones
                    if (currentTarget == null) {
                        currentTarget = potTarget;
                    } else if (MathUtils.getDistance(potTarget, weapon.getLocation()) < MathUtils.getDistance(currentTarget, weapon.getLocation())) {
                        currentTarget = potTarget;
                    }
                }
            }

            //If we didn't find a target, set our range bracket to 2 and our range to 1 above our maximum range bracket
            if (currentTarget == null) {
                rangeBracket = 2;
                rangeToTarget = RANGE_BRACKET_LIMITS[1] + 1f;
            } else {
                //Otherwise, we determine the range to the target and calculate from that (it can't be below 0; if it is, we are within the enemy's collision circle, IE 0 range)
                rangeToTarget = Math.max(0f, (MathUtils.getDistance(currentTarget.getLocation(), weapon.getLocation()) - currentTarget.getCollisionRadius()));

                //Determines our current range bracket
                if (rangeToTarget > RANGE_BRACKET_LIMITS[1]) {
                    rangeBracket = 2;
                } else if (rangeToTarget > RANGE_BRACKET_LIMITS[0]) {
                    rangeBracket = 1;
                } else {
                    rangeBracket = 0;
                }
            }
        }
        //Gets nearby missiles, and checks if we should replace them; this only runs when firing, so we don't have to find iterate quite as many times
        else {
            for (MissileAPI missile : CombatUtils.getMissilesWithinRange(weapon.getLocation(), 200f)) {
                //Only care about our own missiles, and only those we haven't already handled
                if (missile.getWeapon() == weapon && !registeredMissiles.contains(missile)) {
                    registeredMissiles.add(missile);

                    //Replaces the missile in question depending on range-finding
                    MissileAPI newMissile = (MissileAPI)engine.spawnProjectile(weapon.getShip(), weapon, ID_MAIN + ID_ADDONS[rangeBracket], missile.getLocation(),
                            missile.getFacing(), weapon.getShip().getVelocity());
                    Global.getCombatEngine().removeEntity(missile);

                    //If we have a target "painted", set the missile's target to that
                    if (currentTarget != null) {
                        GuidedMissileAI mslAI = (GuidedMissileAI)newMissile.getMissileAI();
                        mslAI.setTarget(currentTarget);
                    }

                    //Finally, registers the new missile
                    registeredMissiles.add(newMissile);
                }
            }
        }
    }
}
