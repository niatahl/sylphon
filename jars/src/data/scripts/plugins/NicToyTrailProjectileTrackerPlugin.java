//By Nicke535, an example plugin for automatically tracking projectiles and handling their trails automatically
//You can use this script as-is if you feel like it, but modifying it can give much more interesting and unique results
package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;

public class NicToyTrailProjectileTrackerPlugin extends BaseEveryFrameCombatPlugin {

    //A map of all the trail data for each projectile
    private Map<DamagingProjectileAPI, String> SPRITE_CATEGORIES = new HashMap<DamagingProjectileAPI, String>();
    private Map<DamagingProjectileAPI, String> SPRITE_IDS = new HashMap<DamagingProjectileAPI, String>();
    private Map<DamagingProjectileAPI, Float> DURATIONS = new HashMap<DamagingProjectileAPI, Float>();
    private Map<DamagingProjectileAPI, Float> START_SIZES = new HashMap<DamagingProjectileAPI, Float>();
    private Map<DamagingProjectileAPI, Float> END_SIZES = new HashMap<DamagingProjectileAPI, Float>();
    private Map<DamagingProjectileAPI, Float> START_SPEEDS = new HashMap<DamagingProjectileAPI, Float>();
    private Map<DamagingProjectileAPI, Float> END_SPEEDS = new HashMap<DamagingProjectileAPI, Float>();
    private Map<DamagingProjectileAPI, Float> ANGLE_OFFSETS = new HashMap<DamagingProjectileAPI, Float>();
    private Map<DamagingProjectileAPI, Color> START_COLORS = new HashMap<DamagingProjectileAPI, Color>();
    private Map<DamagingProjectileAPI, Color> END_COLORS = new HashMap<DamagingProjectileAPI, Color>();
    private Map<DamagingProjectileAPI, Float> OPACITIES = new HashMap<DamagingProjectileAPI, Float>();
    private Map<DamagingProjectileAPI, Boolean> ADDITIVES = new HashMap<DamagingProjectileAPI, Boolean>();
    private Map<DamagingProjectileAPI, Float> LOOP_LENGTHS = new HashMap<DamagingProjectileAPI, Float>();
    private Map<DamagingProjectileAPI, Float> SCROLL_SPEEDS = new HashMap<DamagingProjectileAPI, Float>();
    private Map<DamagingProjectileAPI, Float> AUTO_CUT_LENGTHS = new HashMap<DamagingProjectileAPI, Float>();

    //Tracks the previous location for each trail: used for auto-cutting
    private Map<DamagingProjectileAPI, Vector2f> previousLocation = new HashMap<DamagingProjectileAPI, Vector2f>();

    //A map for known projectiles and their IDs
    private Map<DamagingProjectileAPI, Float> projectileTrailIDs = new HashMap<DamagingProjectileAPI, Float>();

    @Override
    public void init(CombatEngineAPI engine) {
        //Stores our plugin in an easy-to-reach location, so we can access it in different places
        engine.getCustomData().put("NicToyTrailProjectileTrackerPlugin", this);

        //Reinitialize all maps
        SPRITE_CATEGORIES.clear();
        SPRITE_IDS.clear();
        DURATIONS.clear();
        START_SIZES.clear();
        END_SIZES.clear();
        START_SPEEDS.clear();
        END_SPEEDS.clear();
        ANGLE_OFFSETS.clear();
        START_COLORS.clear();
        END_COLORS.clear();
        OPACITIES.clear();
        ADDITIVES.clear();
        LOOP_LENGTHS.clear();
        SCROLL_SPEEDS.clear();
        projectileTrailIDs.clear();
        previousLocation.clear();
    }

    @Override
    public void advance (float amount, List<InputEventAPI> events) {
        if (Global.getCombatEngine() == null || Global.getCombatEngine().isPaused()) {
            return;
        }

        //Runs once on each projectile we are currently tracking. Remove any projectile that is "gone" (collided or faded out)
        List<DamagingProjectileAPI> removeList = new ArrayList<DamagingProjectileAPI>();
        for (DamagingProjectileAPI proj : projectileTrailIDs.keySet()) {
            //Finds "gone" projectiles
            if (proj == null || proj.didDamage()) {
                removeList.add(proj);
                continue;
            }

            //Gets the sprite we want
            SpriteAPI spriteToUse = Global.getSettings().getSprite(SPRITE_CATEGORIES.get(proj), SPRITE_IDS.get(proj));

            //If we have auto-cutting enabled, check if we should auto-cut. If we should, simply allocate a new ID to the trail
            if (AUTO_CUT_LENGTHS.get(proj) > 0) {
                if (AUTO_CUT_LENGTHS.get(proj) < MathUtils.getDistance(proj, previousLocation.get(proj))) {
                    projectileTrailIDs.put(proj, NicToyCustomTrailPlugin.getUniqueID());
                }
            }

            //Determines blend modes
            int blendSRC = GL_SRC_ALPHA;
            int blendDEST = GL_ONE_MINUS_SRC_ALPHA;
            if (ADDITIVES.get(proj)) {
                blendDEST = GL_ONE;
            }

            //Then, actually spawn a trail
            NicToyCustomTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs.get(proj), spriteToUse, proj.getLocation(), START_SPEEDS.get(proj), END_SPEEDS.get(proj),
                    proj.getFacing() - 180f + ANGLE_OFFSETS.get(proj),0f, 0f, START_SIZES.get(proj), END_SIZES.get(proj),
                    START_COLORS.get(proj), END_COLORS.get(proj), OPACITIES.get(proj), 0f, 0f, DURATIONS.get(proj), blendSRC, blendDEST,
                    LOOP_LENGTHS.get(proj), SCROLL_SPEEDS.get(proj));

            //And finally sets our previous location
            previousLocation.put(proj, proj.getLocation());
        }

        //Actually removes the projectiles we no longer want to track
        for (DamagingProjectileAPI proj : removeList) {
            SPRITE_CATEGORIES.remove(proj);
            SPRITE_IDS.remove(proj);
            DURATIONS.remove(proj);
            START_SIZES.remove(proj);
            END_SIZES.remove(proj);
            START_SPEEDS.remove(proj);
            END_SPEEDS.remove(proj);
            ANGLE_OFFSETS.remove(proj);
            START_COLORS.remove(proj);
            END_COLORS.remove(proj);
            OPACITIES.remove(proj);
            ADDITIVES.remove(proj);
            LOOP_LENGTHS.remove(proj);
            SCROLL_SPEEDS.remove(proj);
            previousLocation.remove(proj);
            projectileTrailIDs.remove(proj);
        }
    }

    //All settings except autoCutDistance works exactly like in the main plugin (with spriteCategory/spriteID representing the sprite "path" in settings.json): autoCutDistance means that if
    //a projectile moves further than this distance in a single frame, the trail should be cut automatically (this helps with teleporters, for example). Setting it to 0 or below disables
    //this functionality altogether
    public static void AutoTrackNewTrail (DamagingProjectileAPI proj, String spriteCategory, String spriteID, float duration, float startSize, float endSize, float startSpeed, float endSpeed,
                                      float angleOffset, Color startColor, Color endColor, float opacity, boolean additive, float loopLength, float scrollSpeed, float autoCutDistance) {
        //First, find the plugin
        if (Global.getCombatEngine() == null) {
            return;
        } else if (!(Global.getCombatEngine().getCustomData().get("NicToyTrailProjectileTrackerPlugin") instanceof NicToyTrailProjectileTrackerPlugin)) {
            return;
        }
        NicToyTrailProjectileTrackerPlugin plugin = (NicToyTrailProjectileTrackerPlugin)Global.getCombatEngine().getCustomData().get("NicToyTrailProjectileTrackerPlugin");

        //Then, allocate our values
        plugin.projectileTrailIDs.put(proj, NicToyCustomTrailPlugin.getUniqueID());
        plugin.SPRITE_CATEGORIES.put(proj, spriteCategory);
        plugin.SPRITE_IDS.put(proj, spriteID);
        plugin.DURATIONS.put(proj, duration);
        plugin.START_SIZES.put(proj, startSize);
        plugin.END_SIZES.put(proj, endSize);
        plugin.START_SPEEDS.put(proj, startSpeed);
        plugin.END_SPEEDS.put(proj, endSpeed);
        plugin.ANGLE_OFFSETS.put(proj, angleOffset);
        plugin.START_COLORS.put(proj, startColor);
        plugin.END_COLORS.put(proj, endColor);
        plugin.OPACITIES.put(proj, opacity);
        plugin.ADDITIVES.put(proj, additive);
        plugin.LOOP_LENGTHS.put(proj, loopLength);
        plugin.SCROLL_SPEEDS.put(proj, scrollSpeed);
        plugin.AUTO_CUT_LENGTHS.put(proj, autoCutDistance);
    }
}