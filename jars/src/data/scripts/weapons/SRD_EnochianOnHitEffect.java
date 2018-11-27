package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class SRD_EnochianOnHitEffect implements OnHitEffectPlugin {

    //Percentage of the projectile's original damage dealt as bonus damage on hull hit: too high and AI issues start appearing
    //private static final float DAMAGE_MULT = 0.3f;

    //Variables for explosion visuals
    private static final Color EXPLOSION_COLOR = new Color(45/255f, 135f/255f, 255f/255f, 130f/255f);
    private static final float EXPLOSION_SIZE = 130f;
    private static final float EXPLOSION_DURATION_MIN = 0.3f;
    private static final float EXPLOSION_DURATION_MAX = 0.7f;

    //Variables for the small particles generated with the explosion
    private static final int PARTICLE_COUNT = 4;
    private static final Color PARTICLE_COLOR = new Color(85f/255f, 205f/255f, 255f/255f, 130f/255f);
    private static final float PARTICLE_SIZE_MIN = 8f;
    private static final float PARTICLE_SIZE_MAX = 12f;
    private static final float PARTICLE_SPEED_MAX = 3f;
    private static final float PARTICLE_BRIGHTNESS_MIN = 60f;
    private static final float PARTICLE_BRIGHTNESS_MAX = 90f;

    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, CombatEngineAPI engine) {
        if (shieldHit || target == null) {
            return;
        }

        Global.getCombatEngine().applyDamage(target, point, 100f, DamageType.HIGH_EXPLOSIVE, 0, true, false, null, true);
        Global.getCombatEngine().spawnExplosion(point, new Vector2f(0f, 0f), EXPLOSION_COLOR ,EXPLOSION_SIZE, EXPLOSION_DURATION_MAX);
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            Global.getCombatEngine().addHitParticle(point, MathUtils.getRandomPointInCircle(null, PARTICLE_SPEED_MAX), MathUtils.getRandomNumberInRange(PARTICLE_SIZE_MIN, PARTICLE_SIZE_MAX),
                    MathUtils.getRandomNumberInRange(PARTICLE_BRIGHTNESS_MIN, PARTICLE_BRIGHTNESS_MAX), MathUtils.getRandomNumberInRange(EXPLOSION_DURATION_MIN, EXPLOSION_DURATION_MAX), PARTICLE_COLOR);
        }

        //Commented out, but plays a sound when exploding if un-commented
        //Global.getSoundPlayer().playSound("SRD_ArCielExplosion", 1f, 1f, point, new Vector2f(0f, 0f));
    }
}
