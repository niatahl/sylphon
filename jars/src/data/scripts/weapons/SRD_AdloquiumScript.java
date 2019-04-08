//By Nicke535, handles the empowered final shots in an Adloquium burst
package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SRD_AdloquiumScript implements EveryFrameWeaponEffectPlugin {

    public static final int SHOTS_FOR_SWITCH_1 = 5;
    public static final int SHOTS_FOR_SWITCH_2 = 6;

    public static final float DAMAGE_MULT_SUPERCHARGE = 1.5f;
    public static final float DAMAGE_MULT_NORMAL = 1f;

    private static final Color EXPLOSION_COLOR = new Color(116, 50, 145);
    private static final Color PARTICLE_COLOR = new Color(214, 122, 231);

    private List<DamagingProjectileAPI> registeredProjectiles = new ArrayList<DamagingProjectileAPI>();

    private int shotCounter = 0;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        //Don't run if we are paused, or our weapon is null
        if (engine.isPaused() || weapon == null) {
            return;
        }

        //As soon as our cooldown is over, reset our shot counter
        if (weapon.getCooldownRemaining() <= 0f && weapon.getChargeLevel() <= 0f) {
            shotCounter = 0;
        }

        //Gets nearby shots, and checks if we should replace them
        for (DamagingProjectileAPI proj : CombatUtils.getProjectilesWithinRange(weapon.getLocation(), 400f)) {
            if (proj.getWeapon() == weapon && !registeredProjectiles.contains(proj)) {
                shotCounter++;
                registeredProjectiles.add(proj);

                //Spawns a different projectile at shot number A and B
                if (shotCounter >= SHOTS_FOR_SWITCH_2) {
                    shotCounter = 0;
                    DamagingProjectileAPI newProj = (DamagingProjectileAPI) engine.spawnProjectile(weapon.getShip(), weapon, weapon.getId() + "_fake", proj.getLocation(),
                            proj.getFacing(), weapon.getShip().getVelocity());
                    newProj.setDamageAmount(proj.getDamageAmount() * DAMAGE_MULT_SUPERCHARGE);
                    Global.getSoundPlayer().playSound("SRD_adloquium_fire_supercharged", 1f, 1f, proj.getLocation(), weapon.getShip().getVelocity());
                    spawnDecoParticles(proj.getLocation(), proj.getFacing(), weapon.getShip().getVelocity(), engine);
                    Global.getCombatEngine().removeEntity(proj);
                    registeredProjectiles.add(newProj);
                } else if (shotCounter >= SHOTS_FOR_SWITCH_1) {
                    DamagingProjectileAPI newProj = (DamagingProjectileAPI) engine.spawnProjectile(weapon.getShip(), weapon, weapon.getId() + "_fake", proj.getLocation(),
                            proj.getFacing(), weapon.getShip().getVelocity());
                    newProj.setDamageAmount(proj.getDamageAmount() * DAMAGE_MULT_SUPERCHARGE);
                    Global.getSoundPlayer().playSound("SRD_adloquium_fire_supercharged", 1f, 1f, proj.getLocation(), weapon.getShip().getVelocity());
                    spawnDecoParticles(proj.getLocation(), proj.getFacing(), weapon.getShip().getVelocity(), engine);
                    Global.getCombatEngine().removeEntity(proj);
                    registeredProjectiles.add(newProj);
                } else {
                    proj.setDamageAmount(proj.getDamageAmount() * DAMAGE_MULT_NORMAL);
                    Global.getSoundPlayer().playSound("SRD_adloquium_fire", 1f, 1f, proj.getLocation(), weapon.getShip().getVelocity());
                }
            }
        }
    }

    //For spawning the decorative particles on the third shot
    private void spawnDecoParticles(Vector2f point, float angle, Vector2f offsetVelocity, CombatEngineAPI engine) {
        //Spawns four explosions with offset
        for (int i = 0; i < 4; i++) {
            Vector2f velocity = MathUtils.getPointOnCircumference(new Vector2f(0f, 0f), 15f + (80f * i), angle);
            velocity = Vector2f.add(velocity, offsetVelocity, null);
            engine.spawnExplosion(point, velocity, EXPLOSION_COLOR, 15f - (2.5f * i), 0.6f);
        }

        //Spawns glowy particles, in both directions
        Vector2f offsetPoint = MathUtils.getPointOnCircumference(point, 60f, angle);
        for (int i = 0; i < 25; i++) {
            Vector2f spawnPointStart = MathUtils.getRandomPointOnLine(point, offsetPoint);
            Vector2f spawnPoint = MathUtils.getRandomPointInCircle(spawnPointStart, 8f);

            engine.addHitParticle(spawnPoint, offsetVelocity, MathUtils.getRandomNumberInRange(5f, 8f), 25f, MathUtils.getRandomNumberInRange(0.8f, 1.1f), PARTICLE_COLOR);
        }
        offsetPoint = MathUtils.getPointOnCircumference(point, 40f, angle + 180);
        for (int i = 0; i < 15; i++) {
            Vector2f spawnPointStart = MathUtils.getRandomPointOnLine(point, offsetPoint);
            Vector2f spawnPoint = MathUtils.getRandomPointInCircle(spawnPointStart, 8f);

            engine.addHitParticle(spawnPoint, offsetVelocity, MathUtils.getRandomNumberInRange(5f, 8f), 25f, MathUtils.getRandomNumberInRange(0.8f, 1.1f), PARTICLE_COLOR);
        }
    }
}
