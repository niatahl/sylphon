package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import java.awt.Color;

import org.lwjgl.util.vector.Vector2f;

public class SRD_EquityOnHitEffect implements OnHitEffectPlugin {

    private static final Color COLOR1 = new Color(150, 50, 255);
    private static final Color COLOR2 = new Color(255, 220, 220);

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit,
                      CombatEngineAPI engine) {
        if (point == null) {
            return;
        }

        if (target instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) target;

            float hitLevel = 0f;
            float emp = projectile.getEmpAmount() * 0.33f;
            float dam = projectile.getDamageAmount() * 0.05f;
            for (int x = 0; x < 4; x++) {
                float pierceChance = ((ShipAPI) target).getHardFluxLevel() - 0.1f;
                pierceChance *= ship.getMutableStats().getDynamic().getValue(Stats.SHIELD_PIERCED_MULT);

                boolean piercedShield = shieldHit && (float) Math.random() < pierceChance;
                if (!shieldHit || piercedShield) {
                    hitLevel += 0.25f;
                    ShipAPI empTarget = ship;
                    engine.spawnEmpArcPierceShields(projectile.getSource(), point, empTarget, empTarget,
                            projectile.getDamageType(), dam, emp, 100000f, null, 20f, COLOR1, COLOR2);
                }
            }

            if (hitLevel > 0f) {
                engine.addSmoothParticle(point, new Vector2f(0f, 0f), 300f * hitLevel, hitLevel, 0.75f, COLOR1);
                Global.getSoundPlayer().playSound("tachyon_lance_emp_impact", 1f, 1f * hitLevel, point, new Vector2f(0f, 0f));
            }
        }
    }
}

