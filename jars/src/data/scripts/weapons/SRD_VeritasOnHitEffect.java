package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import data.scripts.plugins.SRD_VeritasTrackerPlugin;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashMap;
import java.util.Map;

public class SRD_VeritasOnHitEffect implements OnHitEffectPlugin {

    private static final float BASE_DURATION = 0.35f;
    private static final float MAX_DURATION_INCREASE = 0.1f;
    private static final float PROJ_BASE_HEIGHT = 22f;
    private static final float PROJ_BASE_WIDTH = 7f;

    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, CombatEngineAPI engine) {
        if (shieldHit || target == null) {
            return;
        }

        if (target instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI)target;

            if (ship.isPiece()) {
                return;
            }

            Vector2f relativePoint = VectorUtils.rotateAroundPivot(point, ship.getLocation(), -ship.getFacing(), new Vector2f(0f, 0f));
            relativePoint.x -= ship.getLocation().x;
            relativePoint.y -= ship.getLocation().y;

            Map<String,Float> VALUES = new HashMap<>();
            VALUES.put("t", BASE_DURATION + (float)(MAX_DURATION_INCREASE * Math.random())); //duration
            VALUES.put("w", PROJ_BASE_WIDTH); //width
            VALUES.put("h", PROJ_BASE_HEIGHT); //length
            VALUES.put("x", relativePoint.x); //origin X, relative to enemy ship
            VALUES.put("y", relativePoint.y); //origin Y, relative to enemy ship
            VALUES.put("a", projectile.getFacing() - ship.getFacing() + MathUtils.getRandomNumberInRange(-2f, 2f)); //angle, relative to enemy ship
            VALUES.put("g", (float)Math.floor(MathUtils.getRandomNumberInRange(1f, 4.99f))); //graphics to use : either 1, 2, 3 or 4
            VALUES.put("d", projectile.getDamageAmount() * 1f); //damage on detonation: in this case, 100% of projectile damage

            //Add the spike to the plugin
            SRD_VeritasTrackerPlugin.addMember(VALUES, ship);
        }
    }
}
