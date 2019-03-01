//By Nicke535, makes a weapon fire a different weapon's projectile (with weapon "yourweapon_EMP") every X shots
package data.scripts.weapons;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.combat.CombatUtils;

import java.util.ArrayList;
import java.util.List;

public class SRD_EMPCyclerScript implements EveryFrameWeaponEffectPlugin {

    public static final int SHOTS_FOR_EMP = 5;

    private List<DamagingProjectileAPI> registeredProjectiles = new ArrayList<DamagingProjectileAPI>();

    private int shotCounter = 0;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        //Don't run if we are paused, or our weapon is null
        if (engine.isPaused() || weapon == null) {
            return;
        }

        //Gets nearby shots, and checks if we should replace them
        for (DamagingProjectileAPI proj : CombatUtils.getProjectilesWithinRange(weapon.getLocation(), 200f)) {
            if (proj.getWeapon() == weapon && !registeredProjectiles.contains(proj)) {
                shotCounter++;
                registeredProjectiles.add(proj);

                //Spawns new projectile every X shots
                if (shotCounter >= SHOTS_FOR_EMP) {
                    //Plays sound, since the implementation requires script-sided sound effects
                    Global.getSoundPlayer().playSound("SRD_embrace_charged_fire", 1f, 1f, proj.getLocation(), weapon.getShip().getVelocity());

                    //Handles the actual shot-switch
                    shotCounter = 0;
                    DamagingProjectileAPI newProj = (DamagingProjectileAPI) engine.spawnProjectile(weapon.getShip(), weapon, weapon.getId() + "_EMP", proj.getLocation(),
                            proj.getFacing(), weapon.getShip().getVelocity());
                    Global.getCombatEngine().removeEntity(proj);
                    registeredProjectiles.add(newProj);
                } else {
                    //Plays sound, since the implementation requires script-sided sound effects
                    Global.getSoundPlayer().playSound("SRD_embrace_fire", 1f, 1f, proj.getLocation(), weapon.getShip().getVelocity());
                }
            }
        }
    }
}
