//NOTE TO SELF: timer ticking actually happens in SRD_BenedictionWeaponScript

package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import data.scripts.plugins.MagicTrailPlugin;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;

import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;

public class SRD_BenedictionOnHitEffect implements OnHitEffectPlugin {

    private static final float MODIFYING_MULT = 1.2f;
    private static Color ONHIT_COLOR_BASE = new Color(220,70,235);
    private static Color ONHIT_COLOR_FINAL = new Color(255,25,145);

    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, CombatEngineAPI engine) {
        if (target == null) {
            return;
        }

        if (target instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI)target;
            WeaponAPI weap = projectile.getWeapon();
            if (!(weap.getEffectPlugin() instanceof SRD_BenedictionWeaponScript)) { return; }
            SRD_BenedictionWeaponScript script = (SRD_BenedictionWeaponScript) weap.getEffectPlugin();
            int burstID = script.missileTracker.get((MissileAPI)projectile);

            //Ensures our maps are filled with the necessary values
            if (script.comboMap.get(burstID) == null) {
                script.comboMap.put(burstID, new HashMap<ShipAPI, Float>());
            }
            if (script.comboMap.get(burstID).get(ship) == null) {
                script.comboMap.get(burstID).put(ship, 1f);
            }

            //Spawns a whole bunch of wierd trails as an on-hit visual. Gotta make use of those trails somewhere!
            float currentMult = script.comboMap.get(burstID).get(ship);
            Color colorToUse = new Color((int)(ONHIT_COLOR_BASE.getRed() * ((2.48832f - currentMult)/1.48832f) + ONHIT_COLOR_FINAL.getRed() * ((currentMult - 1f)/1.48832f)),
                    (int)(ONHIT_COLOR_BASE.getGreen() * ((2.48832f - currentMult)/1.48832f) + ONHIT_COLOR_FINAL.getGreen() * ((currentMult - 1f)/1.48832f)),
                    (int)(ONHIT_COLOR_BASE.getBlue() * ((2.48832f - currentMult)/1.48832f) + ONHIT_COLOR_FINAL.getBlue() * ((currentMult - 1f)/1.48832f)));
            SpriteAPI spriteToUse = Global.getSettings().getSprite("SRD_fx","projectile_trail_fuzzy");
            for (int i1 = 0; i1 < 6 * currentMult; i1++) {
                float id = MagicTrailPlugin.getUniqueID();
                float angle = MathUtils.getRandomNumberInRange(projectile.getFacing() - 40f, projectile.getFacing() + 40f);
                float startSpeed = MathUtils.getRandomNumberInRange(50f, 500f) * currentMult;
                float startAngularVelocity = MathUtils.getRandomNumberInRange(-75f, 75f);
                float startSize = MathUtils.getRandomNumberInRange(12f, 45f);
                float lifetimeMult = MathUtils.getRandomNumberInRange(0.3f, 1f);
                for (int i2 = 0; i2 < 70; i2++) {
                    //This is for "end fizzle"
                    float fizzleConstantSpeed = MathUtils.getRandomNumberInRange(-10f, 10f) * (currentMult - 1f);
                    float fizzleConstantAngle = MathUtils.getRandomNumberInRange(-20f, 20f) * (currentMult - 1f);
                    MagicTrailPlugin.AddTrailMemberAdvanced(null, id, spriteToUse, projectile.getLocation(),
                            startSpeed * ((float)i2 / 70f), fizzleConstantSpeed * (1f - (float)i2 / 70f),
                            angle, startAngularVelocity * ((float)i2 / 70f), fizzleConstantAngle * (1f - (float)i2 / 70f), startSize, 0f,
                            colorToUse, colorToUse,0.45f, 0f, 0.5f * ((float)i2 / 70f) * lifetimeMult, 1.1f * ((float)i2 / 70f) * lifetimeMult,
                            GL_SRC_ALPHA, GL_ONE,500f, 600f, new Vector2f(0f, 0f), null);
                }
            }
            engine.addHitParticle(projectile.getLocation(), new Vector2f(0f, 0f), 250f * (float)Math.sqrt(currentMult), (float)Math.sqrt(currentMult), 0.4f, colorToUse);

            //Now, deal bonus damage from our combo map, and increase the combo map for our current target by MODIFYING_MULT
            engine.applyDamage(ship, point, projectile.getDamageAmount() * (script.comboMap.get(burstID).get(ship) - 1), projectile.getDamageType(),
                    0f, false, false, projectile.getSource(), false);
            script.comboMap.get(burstID).put(ship, script.comboMap.get(burstID).get(ship) * MODIFYING_MULT);
        }
    }
}
