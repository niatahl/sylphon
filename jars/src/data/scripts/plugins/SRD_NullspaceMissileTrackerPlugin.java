//By Nicke535, spawns smoke and afterimages on all missiles with a matching ID
//Also spawns trails in a "drill" pattern
package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import data.scripts.util.MagicRender;
import data.scripts.weapons.SRD_BenedictionOnHitEffect;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.*;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;

public class SRD_NullspaceMissileTrackerPlugin extends BaseEveryFrameCombatPlugin {

    private static final Color AFTERIMAGE_COLOR = new Color(0.25f, 0.05f, 0.40f, 0.3f);
    private static final float SHADOW_DELAY = 0.05f;
    private static final float SHADOW_ANGLE_DIFFERENCE = 1f;
    private static final float SHADOW_DISTANCE_DIFFERENCE = 10f;
    private static final float SHADOW_FLICKER_DIFFERENCE = 2f;
    private static final int SHADOW_FLICKER_CLONES = 3;

    private static final List<String> matchingMissileIDs = new ArrayList<String>();
    static {
        matchingMissileIDs.add("SRD_selene_msl");
    }

    //Since you can't directly get the sprite size of a missile... this map will have to do
    private static final Map<String, Vector2f> missileSizes = new HashMap<String, Vector2f>();
    static {
        missileSizes.put("SRD_selene_msl", new Vector2f(20, 40));
    }

    private Map<DamagingProjectileAPI, Float> shadowDelayCounters = new WeakHashMap<DamagingProjectileAPI, Float>();
    private Map<DamagingProjectileAPI, Float> trailIDs1 = new WeakHashMap<DamagingProjectileAPI, Float>();
    private Map<DamagingProjectileAPI, Float> trailIDs2 = new WeakHashMap<DamagingProjectileAPI, Float>();
    private Map<DamagingProjectileAPI, Float> trailIDs3 = new WeakHashMap<DamagingProjectileAPI, Float>();

    @Override
    public void init(CombatEngineAPI engine) {
        //reinitialize the lists
        shadowDelayCounters.clear();
        trailIDs1.clear();
        trailIDs2.clear();
        trailIDs3.clear();
    }

    @Override
    public void advance (float amount, List<InputEventAPI> events) {
        if (Global.getCombatEngine() == null || Global.getCombatEngine().isPaused()) {
            return;
        }
        CombatEngineAPI engine = Global.getCombatEngine();

        //Runs once on each projectile that matches one of the IDs
        List<DamagingProjectileAPI> removeList = new ArrayList<DamagingProjectileAPI>();
        for (DamagingProjectileAPI proj : engine.getProjectiles()) {
            if (proj.getProjectileSpecId() == null) {
                continue;
            }
            if (matchingMissileIDs.contains(proj.getProjectileSpecId())) {
                //Phases the missile
                proj.setCollisionClass(CollisionClass.NONE);

                //Checks whether we should draw visuals at all (only draw when we are close enough to the viewport)
                ViewportAPI viewport = Global.getCombatEngine().getViewport();
                if (!viewport.isNearViewport(proj.getLocation(), proj.getCollisionRadius() * 1.5f)) {
                    continue;
                }

                //Moves the shadow to its appropriate location
                Vector2f shadowPos = MathUtils.getRandomPointInCircle(null, SHADOW_DISTANCE_DIFFERENCE);
                shadowPos.x += proj.getLocation().x;
                shadowPos.y += proj.getLocation().y;

                //If enough time has passed, render a new shadow
                if (shadowDelayCounters.get(proj) == null) {
                    shadowDelayCounters.put(proj, 0f);
                }
                shadowDelayCounters.put(proj, shadowDelayCounters.get(proj) + amount);
                if (shadowDelayCounters.get(proj) > SHADOW_DELAY) {

                    float angleDifference = MathUtils.getRandomNumberInRange(-SHADOW_ANGLE_DIFFERENCE, SHADOW_ANGLE_DIFFERENCE) - 90f;

                    for (int i = 0; i < SHADOW_FLICKER_CLONES; i++) {
                        Vector2f modifiedShadowPos = new Vector2f(MathUtils.getRandomNumberInRange(-SHADOW_FLICKER_DIFFERENCE, SHADOW_FLICKER_DIFFERENCE), MathUtils.getRandomNumberInRange(-SHADOW_FLICKER_DIFFERENCE, SHADOW_FLICKER_DIFFERENCE));
                        modifiedShadowPos.x += shadowPos.x;
                        modifiedShadowPos.y += shadowPos.y;
                        MagicRender.battlespace(Global.getSettings().getSprite("SRD_fx", "" + proj.getProjectileSpecId() + "_phantom"), modifiedShadowPos, new Vector2f(0f, 0f),
                                missileSizes.get(proj.getProjectileSpecId()),
                                new Vector2f(0f, 0f), proj.getFacing() + angleDifference,
                                0f, AFTERIMAGE_COLOR, true, 0.05f, 0f, 0.2f);
                    }

                    shadowDelayCounters.put(proj, shadowDelayCounters.get(proj) - SHADOW_DELAY);
                }

                //Always render smoke at the shadow's position
                for (int i = 0; i < (600 * amount); i++) {
                    SRD_FakeSmokePlugin.addFakeSmoke(MathUtils.getRandomNumberInRange(0.31f, 0.78f), MathUtils.getRandomNumberInRange(18f, 31f), MathUtils.getRandomPointInCircle(shadowPos,
                            proj.getCollisionRadius() * 0.5f), MathUtils.getRandomPointInCircle(null, 10f), MathUtils.getRandomNumberInRange(-13f, 13f), 0.65f,
                            new Color(0f, 0f, 0f));
                }

                //-------------------------------Render a "triple trail" for the missile, so it looks sort of like its "drilling" through nullspace----------------------------------------------
                //Gets IDs
                if (trailIDs1.get(proj) == null) {
                    trailIDs1.put(proj, MagicTrailPlugin.getUniqueID());
                }
                if (trailIDs2.get(proj) == null) {
                    trailIDs2.put(proj, MagicTrailPlugin.getUniqueID());
                }
                if (trailIDs3.get(proj) == null) {
                    trailIDs3.put(proj, MagicTrailPlugin.getUniqueID());
                }

                //Spawns three trails, with a 120 degree offset
                SpriteAPI spriteToUse = Global.getSettings().getSprite("SRD_fx", "conduit_trails_standard");
                MagicTrailPlugin.AddTrailMemberAdvanced(proj, trailIDs1.get(proj), spriteToUse, proj.getLocation(), 100f, 0f,
                        (float)(FastTrig.sin(6f * proj.getElapsed()) * 40f) + proj.getFacing() + 180f, 0f, 0f, 14f,
                        25f, AFTERIMAGE_COLOR, Color.BLACK, 1f, 0f, 0f, 1.5f, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA,
                        64f, 1550f, new Vector2f(0f, 0f), null);

                MagicTrailPlugin.AddTrailMemberAdvanced(proj, trailIDs2.get(proj), spriteToUse, proj.getLocation(), 100f, 0f,
                        (float)(FastTrig.sin(6f * proj.getElapsed() + (Math.toRadians(120))) * 40f) + proj.getFacing() + 180f, 0f, 0f, 14f,
                        25f, AFTERIMAGE_COLOR, Color.BLACK,1f, 0f, 0f, 1.5f, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA,
                        64f, 1550f, new Vector2f(0f, 0f), null);

                MagicTrailPlugin.AddTrailMemberAdvanced(proj, trailIDs3.get(proj), spriteToUse, proj.getLocation(), 100f, 0f,
                        (float)(FastTrig.sin(6f * proj.getElapsed() + (Math.toRadians(240))) * 40f) + proj.getFacing() + 180f, 0f, 0f, 14f,
                        25f, AFTERIMAGE_COLOR, Color.BLACK, 1f, 0f, 0f, 1.5f, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA,
                        64f, 1550f, new Vector2f(0f, 0f), null);
            }
        }
    }
}