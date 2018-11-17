//By Nicke535, tracks damage bonuses for the Benediction
package data.scripts.weapons;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class SRD_EndzeitScript implements EveryFrameWeaponEffectPlugin {

    private static float FORCE_AMOUNT = 700f;

    private static float PARTICLE_SPEED = 200f;
    private static float PARTICLE_SPEED_VAR = 0.05f;
    private static float PARTICLE_DURATION_MAX = 1.4f;
    private static float PARTICLE_DURATION_MIN = 1.1f;
    private static float PARTICLE_BRIGHTNESS_MAX = 85f;
    private static float PARTICLE_BRIGHTNESS_MIN = 70f;
    private static float PARTICLE_SIZE_MAX = 23f;
    private static float PARTICLE_SIZE_MIN = 17f;

    public static Color PARTICLE_COLOR = new Color(51, 8, 75, 160);
    
    private Map<DamagingProjectileAPI, List<CombatEntityAPI>> targetList = new HashMap<DamagingProjectileAPI, List<CombatEntityAPI>>();

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        //Don't run if we are paused, or our weapon is null
        if (engine.isPaused() || weapon == null) {
            return;
        }

        //Gets all shots belonging to the Endzeit
        for (DamagingProjectileAPI proj : engine.getProjectiles()) {
            if (proj.getWeapon() == weapon) {
                //If the shot has already hit, don't do anything
                if (proj.didDamage()) {
                    continue;
                }

                //Generates particles from the projectile
                Vector2f speed1 = new Vector2f(0f, 0f);
                speed1.x = PARTICLE_SPEED * (float)FastTrig.cos(Math.toRadians(proj.getFacing() + 160)) * MathUtils.getRandomNumberInRange(1-PARTICLE_SPEED_VAR, 1+PARTICLE_SPEED_VAR);
                speed1.y = PARTICLE_SPEED * (float)FastTrig.sin(Math.toRadians(proj.getFacing() + 160)) * MathUtils.getRandomNumberInRange(1-PARTICLE_SPEED_VAR, 1+PARTICLE_SPEED_VAR);
                Vector2f speed2 = new Vector2f(0f, 0f);
                speed2.x = PARTICLE_SPEED * (float)FastTrig.cos(Math.toRadians(proj.getFacing() + 200)) * MathUtils.getRandomNumberInRange(1-PARTICLE_SPEED_VAR, 1+PARTICLE_SPEED_VAR);
                speed2.y = PARTICLE_SPEED * (float)FastTrig.sin(Math.toRadians(proj.getFacing() + 200)) * MathUtils.getRandomNumberInRange(1-PARTICLE_SPEED_VAR, 1+PARTICLE_SPEED_VAR);
                engine.addHitParticle(proj.getLocation(), speed1, MathUtils.getRandomNumberInRange(PARTICLE_SIZE_MIN, PARTICLE_SIZE_MAX),
                        MathUtils.getRandomNumberInRange(PARTICLE_BRIGHTNESS_MIN, PARTICLE_BRIGHTNESS_MAX),
                        MathUtils.getRandomNumberInRange(PARTICLE_DURATION_MIN, PARTICLE_DURATION_MAX), PARTICLE_COLOR);
                engine.addHitParticle(proj.getLocation(), speed2, MathUtils.getRandomNumberInRange(PARTICLE_SIZE_MIN, PARTICLE_SIZE_MAX),
                        MathUtils.getRandomNumberInRange(PARTICLE_BRIGHTNESS_MIN, PARTICLE_BRIGHTNESS_MAX),
                        MathUtils.getRandomNumberInRange(PARTICLE_DURATION_MIN, PARTICLE_DURATION_MAX), PARTICLE_COLOR);

                //Find all targets near the projectile's path and drag them toward the projectile's center path
                for (CombatEntityAPI target : CombatUtils.getEntitiesWithinRange(proj.getLocation(), proj.getVelocity().length() * 0.1f)) {
                    //Ignore the firing ship and our projectile
                    if (target == proj || target == proj.getSource()) {
                        continue;
                    }
                    //Ignore the target if it's a projectile, but not a missile
                    if (target instanceof DamagingProjectileAPI && !(target instanceof MissileAPI)) {
                        continue;
                    }

                    //Calculates the direction towards the projectile's travel path
                    Vector2f vel = new Vector2f(proj.getVelocity().x, proj.getVelocity().y);
                    vel = (Vector2f)vel.normalise();
                    Vector2f startDir = new Vector2f(0f, 0f);

                    startDir.x = target.getLocation().x - proj.getLocation().x;
                    startDir.y = target.getLocation().y - proj.getLocation().y;

                    Vector2f removeVector = new Vector2f(0f, 0f);
                    removeVector.x = vel.x * Vector2f.dot(startDir, vel);
                    removeVector.y = vel.y * Vector2f.dot(startDir, vel);

                    Vector2f actualDirection = new Vector2f(startDir.x - removeVector.x, startDir.y - removeVector.y);
                    actualDirection = (Vector2f)actualDirection.normalise();
                    actualDirection = (Vector2f)actualDirection.scale(-1f);

                    //Actually applies the force to the target
                    CombatUtils.applyForce(target, actualDirection, FORCE_AMOUNT);
                }
            }
        }
    }
}
