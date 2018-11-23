//By Nicke535
//"Eats" the flux of the beam target to deal additional damage which is hard-flux on shields and ignores armor against hull
package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

import static org.lwjgl.opengl.GL11.*;

public class NOR_NullRayEffect implements BeamEffectPlugin {
    //Color of our damage numbers we display
    private static final Color DAMAGE_NUMBER_COLOR = new Color(185, 50, 180, 255);

    //Which efficiency is the flux conversion at? Going with 30% for now
    private static final float FLUX_CONVERSION_RATIO = 0.30f;

    //How much flux per second can we remove? Keep this pretty high due to efficiency above
    private static final float MAX_FLUX_CONVERTED_PER_SECOND = 800f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        // Don't bother with any unecessary checks
        if (beam.getWeapon().getShip() == null || beam.getDamageTarget() == null) {
            return;
        }
        if (beam.didDamageThisFrame()) {
            //Saves well-used variables beforehand
            WeaponAPI weapon = beam.getWeapon();
            ShipAPI ship = weapon.getShip();
            CombatEntityAPI target = beam.getDamageTarget();

            //Only care about ships being hit
            if (!(target instanceof ShipAPI)) {
                return;
            }
            ShipAPI beamTarget = (ShipAPI) target;

            //Checks if we hit shields or not
            boolean hitShields = false;
            if (beamTarget.getShield() != null && beamTarget.getShield().isOn() && beamTarget.getShield().isWithinArc(beam.getTo())) {
                hitShields = true;
            }

            //Then, we check how much flux we can eat (and eats it, of course). Hard Flux counts for twice as much, and isn't eaten against shields
            float fluxToEatSoft = 0f;
            float fluxToEatHard = 0f;
            if (beamTarget.getFluxTracker().getCurrFlux() - beamTarget.getFluxTracker().getHardFlux() > (MAX_FLUX_CONVERTED_PER_SECOND*amount)) {
                fluxToEatSoft = (MAX_FLUX_CONVERTED_PER_SECOND*amount);
            } else if (beamTarget.getFluxTracker().getCurrFlux() - beamTarget.getFluxTracker().getHardFlux() > 0) {
                fluxToEatSoft = beamTarget.getFluxTracker().getCurrFlux() - beamTarget.getFluxTracker().getHardFlux();
            }
            //If we haven't eaten our fill and have used up the soft flux, eat hard flux!
            if (fluxToEatSoft < MAX_FLUX_CONVERTED_PER_SECOND*amount && !hitShields) {
                if (beamTarget.getFluxTracker().getHardFlux() > ((MAX_FLUX_CONVERTED_PER_SECOND*amount)-fluxToEatSoft)/2f) {
                    fluxToEatHard = ((MAX_FLUX_CONVERTED_PER_SECOND*amount)-fluxToEatSoft)/2f;
                } else if (beamTarget.getFluxTracker().getHardFlux() > 0f) {
                    fluxToEatHard = beamTarget.getHardFluxLevel();
                }
            }

            //If we ate *any* flux last frame, we either deal damage or add hard flux (depends on what we hit), while displaying damage numbers
            //Also spawn neat particles, i guess
            if (fluxToEatSoft > 0f || fluxToEatHard > 0f) {
                float kindaDamageThisFrame = (fluxToEatSoft + (fluxToEatHard*2f)) * FLUX_CONVERSION_RATIO;
                engine.addFloatingDamageText(beam.getTo(), (int)kindaDamageThisFrame, DAMAGE_NUMBER_COLOR, beamTarget, ship);
                if (hitShields) {
                    beamTarget.getFluxTracker().increaseFlux(kindaDamageThisFrame, true);
                } else {
                    beamTarget.setHitpoints(beamTarget.getHitpoints() - kindaDamageThisFrame);
                }

                //The particle-spawning
                for (int i = 0; i < kindaDamageThisFrame * 0.2f; i++) {
                    if ((kindaDamageThisFrame*0.2f)-(float)i > Math.random()) {
                        Vector2f vel = MathUtils.getPoint(beam.getTo(), MathUtils.getRandomNumberInRange(5f, 400f),
                                MathUtils.getRandomNumberInRange(-15f, 15f) + VectorUtils.getAngle(beam.getTo(), beam.getFrom()));
                        engine.addHitParticle(MathUtils.getRandomPointInCircle(beam.getTo(), 5f), vel, MathUtils.getRandomNumberInRange(5f, 17f), 2f,
                                MathUtils.getRandomNumberInRange(0.1f, 0.2f), DAMAGE_NUMBER_COLOR);
                    }
                }
            }
        }
    }
}