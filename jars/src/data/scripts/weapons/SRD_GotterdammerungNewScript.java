package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import data.scripts.plugins.MagicTrailPlugin;
import data.scripts.plugins.SRD_LensFlarePlugin;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

import static org.lwjgl.opengl.GL11.*;

public class SRD_GotterdammerungNewScript implements BeamEffectPlugin {

    private final String CHARGE_SOUND_ID = "SRD_gotterdammerung_chargeup";
    private final String FIRE_SOUND_ID = "SRD_gotterdammerung_fire";
    private final String ONHIT_SOUND_ID = "SRD_gotterdammerung_onhit";

    private final Color MUZZLE_FLASH_COLOR = new Color(255, 50, 240, 255);
    private final Color MUZZLE_FLASH_COLOR_BRIGHT = new Color(255, 150, 240, 255);

    private boolean runOnce = false;
    private float pptloss = 0f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        // Don't bother with any unecessary checks
        if (beam.getWeapon().getShip() == null) {
            return;
        }

        WeaponAPI weapon = beam.getWeapon();
        ShipAPI ship = weapon.getShip();

        //Plays charge sound and spawn some charge-y particles
        if (!runOnce && !engine.isPaused()) {
            Global.getSoundPlayer().playLoop(CHARGE_SOUND_ID, ship, (0.65f + weapon.getChargeLevel()*2f), (0.5f + (weapon.getChargeLevel() * 0.5f)), beam.getFrom(), new Vector2f(0f, 0f));
            Vector2f middleVector = Vector2f.add(beam.getTo(), beam.getFrom(), new Vector2f(0f, 0f));
            middleVector.x = middleVector.x / 2f;
            middleVector.y = middleVector.y / 2f;
            Global.getSoundPlayer().playLoop(CHARGE_SOUND_ID, ship, (0.65f + weapon.getChargeLevel()*2f), (0.5f + (weapon.getChargeLevel() * 0.5f)), middleVector, new Vector2f(0f, 0f));
            Global.getSoundPlayer().playLoop(CHARGE_SOUND_ID, ship, (0.65f + weapon.getChargeLevel()*2f), (0.5f + (weapon.getChargeLevel() * 0.5f)), beam.getTo(), new Vector2f(0f, 0f));

            //Some size-adjustments for the beam, and minor lens flare
            float chargeLevel = weapon.getChargeLevel();
            SRD_LensFlarePlugin.addMemberSimple(0f, engine.getElapsedInLastFrame(), engine.getElapsedInLastFrame(), 0.4f * chargeLevel, beam.getFrom(), 0.7f * chargeLevel, MUZZLE_FLASH_COLOR);
            engine.addHitParticle(beam.getFrom(), ship.getVelocity(), 200f * chargeLevel, 0.15f * chargeLevel, engine.getElapsedInLastFrame()*2f, MUZZLE_FLASH_COLOR);
            engine.addHitParticle(beam.getFrom(), ship.getVelocity(), 70f * chargeLevel, 0.20f * chargeLevel, engine.getElapsedInLastFrame()*2f, MUZZLE_FLASH_COLOR);
            beam.setWidth(10f);
        }

        //Actually fire the weapon!
        if (weapon.getChargeLevel() >= 1f && !runOnce) {
            runOnce = true;
            //For spawning the sounds at the end, middle and start of the beam
            Global.getSoundPlayer().playSound(FIRE_SOUND_ID, 1f, 1.25f, beam.getTo(), new Vector2f(0f, 0f));
            Vector2f middleVector = Vector2f.add(beam.getTo(), beam.getFrom(), new Vector2f(0f, 0f));
            middleVector.x = middleVector.x / 2f;
            middleVector.y = middleVector.y / 2f;
            Global.getSoundPlayer().playSound(FIRE_SOUND_ID, 1f, 1.25f, middleVector, new Vector2f(0f, 0f));
            Global.getSoundPlayer().playSound(FIRE_SOUND_ID, 1f, 1.25f, beam.getFrom(), new Vector2f(0f, 0f));
            beam.setWidth(40f);

            //Muzzle flash
            SRD_LensFlarePlugin.addMemberSimple(0.1f, 0.4f, 1.4f, 0.9f, beam.getFrom(), 0.8f, MUZZLE_FLASH_COLOR_BRIGHT);
            engine.addHitParticle(beam.getFrom(), ship.getVelocity(), 325f, 0.22f, 1.9f, MUZZLE_FLASH_COLOR_BRIGHT);
            engine.addHitParticle(beam.getFrom(), ship.getVelocity(), 850f, 0.13f, 2f, MUZZLE_FLASH_COLOR);

            //Deals immense damage based on maximum enemy hull (20%) once the weapon finishes charging
            CombatEntityAPI target = beam.getDamageTarget();

            //If we have a target, deal the damage, and spawn a bunch of vfx
            if (target != null) {
                //Handles the extra damage
                engine.applyDamage(
                        target, //enemy target
                        beam.getTo(), //Our 2D vector to the exact world-position
                        target.getMaxHitpoints() * 0.25f, //Damage
                        DamageType.KINETIC, //Using the damage type here.
                        0f, //EMP
                        false, //Does not bypass shields.
                        true, //Does Soft Flux damage
                        beam.getSource()  //Who owns this beam?
                );

                //Spawns decorative EMP arcs to the enemy, with *extremely* token damage (around 1 damage)
                //No longer token damage, flood them with EMP instead - Nia
                for (int i = 0; i < 10; i++) {
                    engine.spawnEmpArcPierceShields(ship, beam.getTo(), target, target,
                            DamageType.ENERGY, //Damage type
                            MathUtils.getRandomNumberInRange(0.5f, 2f), //Damage
                            200f, //Emp
                            150f * (float)i, //Max range, increases gradually
                            null, //Impact sound
                            MathUtils.getRandomNumberInRange(3f, 7f), // thickness of the lightning bolt
                            MUZZLE_FLASH_COLOR_BRIGHT, //Central color
                            MUZZLE_FLASH_COLOR //Fringe Color
                    );
                }

                //TEST: spawns wierd trails in random directions
                SpriteAPI spriteToUse = Global.getSettings().getSprite("SRD_fx","projectile_trail_standard");
                for (int i1 = 0; i1 < 80; i1++) {
                    float id = MagicTrailPlugin.getUniqueID();
                    float angle = MathUtils.getRandomNumberInRange(0f, 360f);
                    float startSpeed = MathUtils.getRandomNumberInRange(50f, 500f);
                    float startAngularVelocity = MathUtils.getRandomNumberInRange(-40f, 40f);
                    float startSize = MathUtils.getRandomNumberInRange(17f, 50f);
                    float lifetimeMult = MathUtils.getRandomNumberInRange(0.3f, 1.2f);
                    for (int i2 = 0; i2 < 100; i2++) {
                        MagicTrailPlugin.AddTrailMemberAdvanced(null, id, spriteToUse, beam.getTo(), startSpeed * ((float)i2 / 100f), 0f,
                                angle, startAngularVelocity * ((float)i2 / 100f), 0f, startSize, 0f, MUZZLE_FLASH_COLOR, MUZZLE_FLASH_COLOR,
                                0.45f, 0f, 0.6f * ((float)i2 / 100f) * lifetimeMult, 1.4f * ((float)i2 / 100f) * lifetimeMult, GL_SRC_ALPHA, GL_ONE,
                                500f, 0f, new Vector2f(0f, 0f), null);
                    }
                }

                //Spawns a bigger on-hit particle
                engine.addHitParticle(beam.getTo(), new Vector2f(0f, 0f), 400f, 0.2f, 0.35f, MUZZLE_FLASH_COLOR);

                //TEST: And plays a neat sound for the on-hit
                Global.getSoundPlayer().playSound(ONHIT_SOUND_ID, 1f, 1.25f, beam.getTo(), new Vector2f(0f, 0f));

                //Finally, reduce remaining ppt due to stress induced by firing the weapon

                if ( !(engine.getCustomData().get(ship.getId()+"pptloss") instanceof Float) )
                    engine.getCustomData().put(ship.getId()+"pptloss",0f);
                pptloss = (float)engine.getCustomData().get(ship.getId()+"pptloss")+30f;
                engine.getCustomData().put(ship.getId()+"pptloss",pptloss);
                ship.getMutableStats().getPeakCRDuration().modifyFlat("GoetterdaemmerungID",-pptloss);
            }
        }

        if (!weapon.isFiring()) {
            runOnce = false;
        }
    }
}