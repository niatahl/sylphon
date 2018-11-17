package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.lang.Math;

public class SRD_DignityWeaponScript implements EveryFrameWeaponEffectPlugin {

    private static final String PROJ_WPN_ID_ADDON = "_secondary";
    private static final float COOLDOWN_MIN = 0.10f;
    private static final float COOLDOWN_MAX = 0.9f;
    private static final float INACCURACY = 1f;
    private static final float[] X_OFFSETS = {-8f, 8f};
    private static final float[] Y_OFFSETS = {8f, 8f};
    private static final float CHARGEUP_TIME = 3f;
    private static final float CHARGE_FADE_MULT = 3f;

    private static final int PARTICLE_COUNT = 10;
    private static final float PARTICLE_SIZE_MIN = 3f;
    private static final float PARTICLE_SIZE_MAX = 8f;
    private static final float PARTICLE_SPEED_MIN = 40f;
    private static final float PARTICLE_SPEED_MAX = 210f;
    private static final float PARTICLE_BRIGHTNESS_MIN = 1f;
    private static final float PARTICLE_BRIGHTNESS_MAX = 3f;
    private static final Color PARTICLE_COLOR = new Color(195, 76, 204, 230);

    private float counter = 0f;
    private float chargeUpCounter = 0f; //This one keeps track of how long we have been firing for this particular burst
    private int currBarrel = 0;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        //If we don't have a ship, or the engine is paused, do nothing
        if (weapon.getShip().isHulk() || engine.isPaused() || weapon.getShip() == null) {
            return;
        } else if (weapon.getChargeLevel() <= 0 || weapon.isDisabled() || weapon.getShip().getFluxTracker().isOverloadedOrVenting() || weapon.getShip().isPhased()) {
            //If we aren't firing the weapon, reset our shot cooldown and start ticking down our spin-up
            counter = 0f;
            chargeUpCounter -= amount * CHARGE_FADE_MULT * weapon.getShip().getMutableStats().getEnergyRoFMult().getModifiedValue();
            if (chargeUpCounter < 0f) { chargeUpCounter = 0f; }
            return;
        }

        //Tracks our time counters
        counter += amount * weapon.getShip().getMutableStats().getEnergyRoFMult().getModifiedValue();
        chargeUpCounter += amount * weapon.getShip().getMutableStats().getEnergyRoFMult().getModifiedValue();
        if (chargeUpCounter > CHARGEUP_TIME) { chargeUpCounter = CHARGEUP_TIME; }

        //Calculates our current cooldown
        float speedMult = chargeUpCounter / CHARGEUP_TIME;
        speedMult = (float)Math.sqrt(speedMult);
        float currentCooldown = COOLDOWN_MAX * (1f - speedMult) + (COOLDOWN_MIN * speedMult);

        //If we have waited longer than our cooldown, fire away!
        while (counter >= currentCooldown) {
            currBarrel++;
            if (currBarrel >= Y_OFFSETS.length) {
                currBarrel = 0;
            }
            counter -= currentCooldown;

            Vector2f spawnLocation = weapon.getLocation();
            spawnLocation.y += X_OFFSETS[currBarrel]; //Somewhat misleading, since the weapon is turned 90 degrees incorrectly when considering coordinates
            spawnLocation.x += Y_OFFSETS[currBarrel];
            spawnLocation = VectorUtils.rotateAroundPivot(spawnLocation, weapon.getLocation(), weapon.getCurrAngle(), weapon.getLocation());

            engine.spawnProjectile(weapon.getShip(), weapon, weapon.getSpec().getWeaponId() + PROJ_WPN_ID_ADDON, spawnLocation,
                    weapon.getCurrAngle() + (MathUtils.getRandomNumberInRange(-INACCURACY, INACCURACY)), weapon.getShip().getVelocity());
            //Plays sound associated with the weapon
            Global.getSoundPlayer().playSound("SRD_dignity_sub_fire", 1f, 1f, spawnLocation, weapon.getShip().getVelocity());
            //Creates barrel particles, to simulate muzzle flash
            int i = PARTICLE_COUNT;
            while (i > 0) {
                i--;
                Vector2f velocity = new Vector2f((float)Math.cos(Math.toRadians((double) weapon.getCurrAngle())), (float)Math.sin(Math.toRadians((double)weapon.getCurrAngle())));
                velocity = (Vector2f)velocity.scale(MathUtils.getRandomNumberInRange(PARTICLE_SPEED_MIN, PARTICLE_SPEED_MAX));
                velocity = Vector2f.add(velocity, weapon.getShip().getVelocity(), new Vector2f(0f, 0f));
                engine.addSmoothParticle(spawnLocation, velocity, MathUtils.getRandomNumberInRange(PARTICLE_SIZE_MIN, PARTICLE_SIZE_MAX),
                        MathUtils.getRandomNumberInRange(PARTICLE_BRIGHTNESS_MIN, PARTICLE_BRIGHTNESS_MAX), (float)(currentCooldown * Math.random()), PARTICLE_COLOR);
            }
        }
    }
}
