//By Nicke535, handles the stranger projectiles in the mod... really, i wanted to split this up, but we are running way too many plugins as it is
package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;

public class SRD_TEMPSlowbeamPlugin extends BaseEveryFrameCombatPlugin {
    //A map for known projectiles and their IDs: should be cleared in init
    private Map<DamagingProjectileAPI, Float> projectileTrailIDs1 = new HashMap<DamagingProjectileAPI, Float>();
    private Map<DamagingProjectileAPI, Float> projectileTrailIDs2 = new HashMap<DamagingProjectileAPI, Float>();
    private Map<DamagingProjectileAPI, Float> projectileTrailIDs3 = new HashMap<DamagingProjectileAPI, Float>();

    @Override
    public void init(CombatEngineAPI engine) {
        //Reinitialize the lists
        projectileTrailIDs1.clear();
        projectileTrailIDs2.clear();
        projectileTrailIDs3.clear();
    }

    @Override
    public void advance (float amount, List<InputEventAPI> events) {
        if (Global.getCombatEngine() == null || Global.getCombatEngine().isPaused()) {
            return;
        }
        CombatEngineAPI engine = Global.getCombatEngine();

        //Runs once on each projectile that matches one of the IDs specified in our maps
        for (DamagingProjectileAPI proj : engine.getProjectiles()) {
            //Ignore already-collided projectiles, and projectiles that don't match our IDs
            if (proj.getProjectileSpecId() == null || proj.didDamage()) {
                continue;
            }

            //-------------------------------------------For visual effects---------------------------------------------
            String specID = proj.getProjectileSpecId();
            if (!specID.contains("SRD_TEMPslowbeam_shot")) { continue; }

            SpriteAPI spriteToUse = Global.getSettings().getSprite("SRD_fx", "projectile_trail_fuzzy");

            //If we haven't already started a trail for this projectile, get an ID for it
            if (projectileTrailIDs1.get(proj) == null) {
                projectileTrailIDs1.put(proj, NicToyCustomTrailPlugin.getUniqueID());
            }
            if (projectileTrailIDs2.get(proj) == null) {
                projectileTrailIDs2.put(proj, NicToyCustomTrailPlugin.getUniqueID());
            }
            if (projectileTrailIDs3.get(proj) == null) {
                projectileTrailIDs3.put(proj, NicToyCustomTrailPlugin.getUniqueID());
            }

            Color colorToUse = new Color((float)Math.abs(FastTrig.sin(proj.getElapsed() * 2f)), (float)Math.abs(FastTrig.sin(2f * proj.getElapsed() + 1.025f)), (float)Math.abs(FastTrig.sin(2f * proj.getElapsed() + 2.05f)), 1f);
            float angle = proj.getElapsed() * 700f + proj.getFacing();
            float angleVel = proj.getElapsed() * -400f;

            //Then, actually spawn a trail
            NicToyCustomTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs1.get(proj), spriteToUse, proj.getLocation(), 300f, 100f, angle,
                    0f, angleVel, 15f, 5f, colorToUse, colorToUse,
                    1f, 0f, 0f, 0.75f, GL_SRC_ALPHA, GL_ONE,
                    500f, -3000f);

            //Then, actually spawn a trail
            NicToyCustomTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs2.get(proj), spriteToUse, proj.getLocation(), 300f, 100f, angle + 120f,
                    0f, angleVel, 15f, 5f, colorToUse, colorToUse,
                    1f, 0f, 0f, 0.75f, GL_SRC_ALPHA, GL_ONE,
                    500f, -3000f);

            //Then, actually spawn a trail
            NicToyCustomTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs3.get(proj), spriteToUse, proj.getLocation(), 300f, 100f, angle + 240f,
                    0f, angleVel, 15f, 5f, colorToUse, colorToUse,
                    1f, 0f, 0f, 0.75f, GL_SRC_ALPHA, GL_ONE,
                    500f, -3000f);
        }
    }
}