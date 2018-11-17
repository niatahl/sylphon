//By Nicke535, handles the stranger projectiles in the mod... really, i wanted to split this up, but we are running way too many plugins as it is
package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.*;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;

public class SRD_ProjectileTrailHandlerPlugin extends BaseEveryFrameCombatPlugin {

    //A map of all the trail sprites used (note that all the sprites must be under SRD_fx): ensure this one has the same keys as the other maps
    private static final Map<String, String> TRAIL_SPRITES = new HashMap<>();
    static {
        TRAIL_SPRITES.put("SRD_adloquium_shot", "projectile_trail_standard");
        TRAIL_SPRITES.put("SRD_adloquium_fake_shot", "projectile_trail_standard");
        TRAIL_SPRITES.put("SRD_skalla_shot", "projectile_trail_fuzzy");
        TRAIL_SPRITES.put("SRD_phira_shock_shot", "projectile_trail_fuzzy");
        TRAIL_SPRITES.put("SRD_benediction_msl", "projectile_trail_standard");
        TRAIL_SPRITES.put("SRD_divinity_shot", "projectile_trail_fuzzy");
        TRAIL_SPRITES.put("SRD_equity_shot", "projectile_trail_standard");
        TRAIL_SPRITES.put("SRD_phira_impact_shot", "projectile_trail_standard");
        TRAIL_SPRITES.put("SRD_phira_burst_shot", "projectile_trail_fuzzy");
        TRAIL_SPRITES.put("SRD_excogitation_shot", "projectile_trail_standard");
        TRAIL_SPRITES.put("SRD_arphage_shot", "projectile_trail_standard");
        TRAIL_SPRITES.put("SRD_veda_shot", "projectile_trail_standard");
        TRAIL_SPRITES.put("SRD_qoga_shot", "projectile_trail_standard");
        TRAIL_SPRITES.put("SRD_arciel_shot", "projectile_trail_standard");
        TRAIL_SPRITES.put("SRD_harmonius_shot", "projectile_trail_core");
    }

    //A map for known projectiles and their IDs: should be cleared in init
    private Map<DamagingProjectileAPI, Float> projectileTrailIDs = new WeakHashMap<>();

    //Used when doing dual-core sprites
    private Map<DamagingProjectileAPI, Float> projectileTrailIDs2 = new WeakHashMap<>();

    //Used for the Equity
    private Map<DamagingProjectileAPI, Float> projectileTrailIDs3 = new WeakHashMap<>();

    //--------------------------------------THESE ARE ALL MAPS FOR DIFFERENT VISUAL STATS FOR THE TRAILS: THEIR NAMES ARE FAIRLY SELF_EXPLANATORY---------------------------------------------------
    private static final Map<String, Float> TRAIL_DURATIONS_IN = new HashMap<>();
    static {
        TRAIL_DURATIONS_IN.put("SRD_adloquium_shot", 0f);
        TRAIL_DURATIONS_IN.put("SRD_adloquium_fake_shot", 0f);
        TRAIL_DURATIONS_IN.put("SRD_skalla_shot", 0f);
        TRAIL_DURATIONS_IN.put("SRD_phira_shock_shot", 0f);
        TRAIL_DURATIONS_IN.put("SRD_benediction_msl", 0.1f);
        TRAIL_DURATIONS_IN.put("SRD_divinity_shot", 0f);
        TRAIL_DURATIONS_IN.put("SRD_equity_shot", 0f);
        TRAIL_DURATIONS_IN.put("SRD_phira_impact_shot", 0f);
        TRAIL_DURATIONS_IN.put("SRD_phira_burst_shot", 0f);
        TRAIL_DURATIONS_IN.put("SRD_excogitation_shot", 0f);
        TRAIL_DURATIONS_IN.put("SRD_arphage_shot", 0f);
        TRAIL_DURATIONS_IN.put("SRD_veda_shot", 0f);
        TRAIL_DURATIONS_IN.put("SRD_qoga_shot", 0f);
        TRAIL_DURATIONS_IN.put("SRD_arciel_shot", 0f);
        TRAIL_DURATIONS_IN.put("SRD_harmonius_shot", 0f);
    }
    private static final Map<String, Float> TRAIL_DURATIONS_MAIN = new HashMap<>();
    static {
        TRAIL_DURATIONS_MAIN.put("SRD_adloquium_shot", 0f);
        TRAIL_DURATIONS_MAIN.put("SRD_adloquium_fake_shot", 0f);
        TRAIL_DURATIONS_MAIN.put("SRD_skalla_shot", 0.50f);
        TRAIL_DURATIONS_MAIN.put("SRD_phira_shock_shot", 0.50f);
        TRAIL_DURATIONS_MAIN.put("SRD_benediction_msl", 0.1f);
        TRAIL_DURATIONS_MAIN.put("SRD_divinity_shot", 0.08f);
        TRAIL_DURATIONS_MAIN.put("SRD_equity_shot", 0.2f);
        TRAIL_DURATIONS_MAIN.put("SRD_phira_impact_shot", 0f);
        TRAIL_DURATIONS_MAIN.put("SRD_phira_burst_shot", 0f);
        TRAIL_DURATIONS_MAIN.put("SRD_excogitation_shot", 0f);
        TRAIL_DURATIONS_MAIN.put("SRD_arphage_shot", 0f);
        TRAIL_DURATIONS_MAIN.put("SRD_veda_shot", 0f);
        TRAIL_DURATIONS_MAIN.put("SRD_qoga_shot", 0f);
        TRAIL_DURATIONS_MAIN.put("SRD_arciel_shot", 0f);
        TRAIL_DURATIONS_MAIN.put("SRD_harmonius_shot", 0f);
    }
    private static final Map<String, Float> TRAIL_DURATIONS_OUT = new HashMap<>();
    static {
        TRAIL_DURATIONS_OUT.put("SRD_adloquium_shot", 0.6f);
        TRAIL_DURATIONS_OUT.put("SRD_adloquium_fake_shot", 1f);
        TRAIL_DURATIONS_OUT.put("SRD_skalla_shot", 0.4f);
        TRAIL_DURATIONS_OUT.put("SRD_phira_shock_shot", 0.4f);
        TRAIL_DURATIONS_OUT.put("SRD_benediction_msl", 0.3f);
        TRAIL_DURATIONS_OUT.put("SRD_divinity_shot", 0.08f);
        TRAIL_DURATIONS_OUT.put("SRD_equity_shot", 0.4f);
        TRAIL_DURATIONS_OUT.put("SRD_phira_impact_shot", 1f);
        TRAIL_DURATIONS_OUT.put("SRD_phira_burst_shot", 0.5f);
        TRAIL_DURATIONS_OUT.put("SRD_excogitation_shot", 0.4f);
        TRAIL_DURATIONS_OUT.put("SRD_arphage_shot", 0.7f);
        TRAIL_DURATIONS_OUT.put("SRD_veda_shot", 0.5f);
        TRAIL_DURATIONS_OUT.put("SRD_qoga_shot", 1f);
        TRAIL_DURATIONS_OUT.put("SRD_arciel_shot", 1.3f);
        TRAIL_DURATIONS_OUT.put("SRD_harmonius_shot", 1.5f);
    }
    private static final Map<String, Float> START_SIZES = new HashMap<>();
    static {
        START_SIZES.put("SRD_adloquium_shot", 13f);
        START_SIZES.put("SRD_adloquium_fake_shot", 13f);
        START_SIZES.put("SRD_skalla_shot", 9f);
        START_SIZES.put("SRD_phira_shock_shot", 9f);
        START_SIZES.put("SRD_benediction_msl", 9f);
        START_SIZES.put("SRD_divinity_shot", 14f);
        START_SIZES.put("SRD_equity_shot", 18f);
        START_SIZES.put("SRD_phira_impact_shot", 8f);
        START_SIZES.put("SRD_phira_burst_shot", 12f);
        START_SIZES.put("SRD_excogitation_shot", 5f);
        START_SIZES.put("SRD_arphage_shot", 8f);
        START_SIZES.put("SRD_veda_shot", 5f);
        START_SIZES.put("SRD_qoga_shot", 2f);
        START_SIZES.put("SRD_arciel_shot", 4f);
        START_SIZES.put("SRD_harmonius_shot", 20f);
    }
    private static final Map<String, Float> END_SIZES = new HashMap<>();
    static {
        END_SIZES.put("SRD_adloquium_shot", 7f);
        END_SIZES.put("SRD_adloquium_fake_shot", 7f);
        END_SIZES.put("SRD_skalla_shot", 9f);
        END_SIZES.put("SRD_phira_shock_shot", 9f);
        END_SIZES.put("SRD_benediction_msl", 5f);
        END_SIZES.put("SRD_divinity_shot", 3f);
        END_SIZES.put("SRD_equity_shot", 7f);
        END_SIZES.put("SRD_phira_impact_shot", 4f);
        END_SIZES.put("SRD_phira_burst_shot", 8f);
        END_SIZES.put("SRD_excogitation_shot", 2f);
        END_SIZES.put("SRD_arphage_shot", 4f);
        END_SIZES.put("SRD_veda_shot", 2f);
        END_SIZES.put("SRD_qoga_shot", 1f);
        END_SIZES.put("SRD_arciel_shot", 2f);
        END_SIZES.put("SRD_harmonius_shot", 20f);
    }
    private static final Map<String, Color> TRAIL_START_COLORS = new HashMap<>();
    static {
        TRAIL_START_COLORS.put("SRD_adloquium_shot", new Color(255,55,185));
        TRAIL_START_COLORS.put("SRD_adloquium_fake_shot", new Color(255,25,145));
        TRAIL_START_COLORS.put("SRD_skalla_shot", new Color(70,150,255));
        TRAIL_START_COLORS.put("SRD_phira_shock_shot", new Color(255,100,50));
        TRAIL_START_COLORS.put("SRD_benediction_msl", new Color(255,55,185));
        TRAIL_START_COLORS.put("SRD_divinity_shot", new Color(130,230,255));
        TRAIL_START_COLORS.put("SRD_equity_shot", new Color(150,50,255));
        TRAIL_START_COLORS.put("SRD_phira_impact_shot", new Color(255,50,50));
        TRAIL_START_COLORS.put("SRD_phira_burst_shot", new Color(255,100,30));
        TRAIL_START_COLORS.put("SRD_excogitation_shot", new Color(155,0,255));
        TRAIL_START_COLORS.put("SRD_arphage_shot", new Color(255,50,0));
        TRAIL_START_COLORS.put("SRD_veda_shot", new Color(255,50,50));
        TRAIL_START_COLORS.put("SRD_qoga_shot", new Color(255,220,220));
        TRAIL_START_COLORS.put("SRD_arciel_shot", new Color(255,255,255));
        TRAIL_START_COLORS.put("SRD_harmonius_shot", new Color(205,130,255));
    }
    private static final Map<String, Color> TRAIL_END_COLORS = new HashMap<>();
    static {
        TRAIL_END_COLORS.put("SRD_adloquium_shot", new Color(255,55,185));
        TRAIL_END_COLORS.put("SRD_adloquium_fake_shot", new Color(255,25,145));
        TRAIL_END_COLORS.put("SRD_skalla_shot", new Color(70,150,255));
        TRAIL_END_COLORS.put("SRD_phira_shock_shot", new Color(255,100,50));
        TRAIL_END_COLORS.put("SRD_benediction_msl", new Color(255,55,185));
        TRAIL_END_COLORS.put("SRD_divinity_shot", new Color(130,230,255));
        TRAIL_END_COLORS.put("SRD_equity_shot", new Color(150,50,255));
        TRAIL_END_COLORS.put("SRD_phira_impact_shot", new Color(255,0,0));
        TRAIL_END_COLORS.put("SRD_phira_burst_shot", new Color(255,0,0));
        TRAIL_END_COLORS.put("SRD_excogitation_shot", new Color(185,125,255));
        TRAIL_END_COLORS.put("SRD_arphage_shot", new Color(255,0,0));
        TRAIL_END_COLORS.put("SRD_veda_shot", new Color(255,0,0));
        TRAIL_END_COLORS.put("SRD_qoga_shot", new Color(255,0,0));
        TRAIL_END_COLORS.put("SRD_arciel_shot", new Color(255,0,0));
        TRAIL_END_COLORS.put("SRD_harmonius_shot", new Color(205,130,255));
    }
    private static final Map<String, Float> TRAIL_OPACITIES = new HashMap<>();
    static {
        TRAIL_OPACITIES.put("SRD_adloquium_shot", 0.8f);
        TRAIL_OPACITIES.put("SRD_adloquium_fake_shot", 1f);
        TRAIL_OPACITIES.put("SRD_skalla_shot", 0.9f);
        TRAIL_OPACITIES.put("SRD_phira_shock_shot", 0.9f);
        TRAIL_OPACITIES.put("SRD_benediction_msl", 0.9f);
        TRAIL_OPACITIES.put("SRD_divinity_shot", 0.7f);
        TRAIL_OPACITIES.put("SRD_equity_shot", 0.7f);
        TRAIL_OPACITIES.put("SRD_phira_impact_shot", 0.5f);
        TRAIL_OPACITIES.put("SRD_phira_burst_shot", 0.7f);
        TRAIL_OPACITIES.put("SRD_excogitation_shot", 0.7f);
        TRAIL_OPACITIES.put("SRD_arphage_shot", 0.8f);
        TRAIL_OPACITIES.put("SRD_veda_shot", 0.5f);
        TRAIL_OPACITIES.put("SRD_qoga_shot", 0.5f);
        TRAIL_OPACITIES.put("SRD_arciel_shot", 0.5f);
        TRAIL_OPACITIES.put("SRD_harmonius_shot", 0.5f);
    }
    private static final Map<String, Integer> TRAIL_BLEND_SRC = new HashMap<>();
    static {
        TRAIL_BLEND_SRC.put("SRD_adloquium_shot", GL_SRC_ALPHA);
        TRAIL_BLEND_SRC.put("SRD_adloquium_fake_shot", GL_SRC_ALPHA);
        TRAIL_BLEND_SRC.put("SRD_skalla_shot", GL_SRC_ALPHA);
        TRAIL_BLEND_SRC.put("SRD_phira_shock_shot", GL_SRC_ALPHA);
        TRAIL_BLEND_SRC.put("SRD_benediction_msl", GL_SRC_ALPHA);
        TRAIL_BLEND_SRC.put("SRD_divinity_shot", GL_SRC_ALPHA);
        TRAIL_BLEND_SRC.put("SRD_equity_shot", GL_SRC_ALPHA);
        TRAIL_BLEND_SRC.put("SRD_phira_impact_shot", GL_SRC_ALPHA);
        TRAIL_BLEND_SRC.put("SRD_phira_burst_shot", GL_SRC_ALPHA);
        TRAIL_BLEND_SRC.put("SRD_excogitation_shot", GL_SRC_ALPHA);
        TRAIL_BLEND_SRC.put("SRD_arphage_shot", GL_SRC_ALPHA);
        TRAIL_BLEND_SRC.put("SRD_veda_shot", GL_SRC_ALPHA);
        TRAIL_BLEND_SRC.put("SRD_qoga_shot", GL_SRC_ALPHA);
        TRAIL_BLEND_SRC.put("SRD_arciel_shot", GL_SRC_ALPHA);
        TRAIL_BLEND_SRC.put("SRD_harmonius_shot", GL_SRC_ALPHA);
    }
    private static final Map<String, Integer> TRAIL_BLEND_DEST = new HashMap<>();
    static {
        TRAIL_BLEND_DEST.put("SRD_adloquium_shot", GL_ONE);
        TRAIL_BLEND_DEST.put("SRD_adloquium_fake_shot", GL_ONE);
        TRAIL_BLEND_DEST.put("SRD_skalla_shot", GL_ONE);
        TRAIL_BLEND_DEST.put("SRD_phira_shock_shot", GL_ONE);
        TRAIL_BLEND_DEST.put("SRD_benediction_msl", GL_ONE);
        TRAIL_BLEND_DEST.put("SRD_divinity_shot", GL_ONE);
        TRAIL_BLEND_DEST.put("SRD_equity_shot", GL_ONE);
        TRAIL_BLEND_DEST.put("SRD_phira_impact_shot", GL_ONE);
        TRAIL_BLEND_DEST.put("SRD_phira_burst_shot", GL_ONE);
        TRAIL_BLEND_DEST.put("SRD_excogitation_shot", GL_ONE);
        TRAIL_BLEND_DEST.put("SRD_arphage_shot", GL_ONE);
        TRAIL_BLEND_DEST.put("SRD_veda_shot", GL_ONE);
        TRAIL_BLEND_DEST.put("SRD_qoga_shot", GL_ONE);
        TRAIL_BLEND_DEST.put("SRD_arciel_shot", GL_ONE);
        TRAIL_BLEND_DEST.put("SRD_harmonius_shot", GL_ONE);
    }
    private static final Map<String, Float> TRAIL_LOOP_LENGTHS = new HashMap<>();
    static {
        TRAIL_LOOP_LENGTHS.put("SRD_adloquium_shot", -1f);
        TRAIL_LOOP_LENGTHS.put("SRD_adloquium_fake_shot", -1f);
        TRAIL_LOOP_LENGTHS.put("SRD_skalla_shot", 500f);
        TRAIL_LOOP_LENGTHS.put("SRD_phira_shock_shot", 500f);
        TRAIL_LOOP_LENGTHS.put("SRD_benediction_msl", 400f);
        TRAIL_LOOP_LENGTHS.put("SRD_divinity_shot", 700f);
        TRAIL_LOOP_LENGTHS.put("SRD_equity_shot", -1f);
        TRAIL_LOOP_LENGTHS.put("SRD_phira_impact_shot", -1f);
        TRAIL_LOOP_LENGTHS.put("SRD_phira_burst_shot", 100f);
        TRAIL_LOOP_LENGTHS.put("SRD_excogitation_shot", -1f);
        TRAIL_LOOP_LENGTHS.put("SRD_arphage_shot", -1f);
        TRAIL_LOOP_LENGTHS.put("SRD_veda_shot", -1f);
        TRAIL_LOOP_LENGTHS.put("SRD_qoga_shot", -1f);
        TRAIL_LOOP_LENGTHS.put("SRD_arciel_shot", 200f);
        TRAIL_LOOP_LENGTHS.put("SRD_harmonius_shot", 200f);
    }
    private static final Map<String, Float> TRAIL_SCROLL_SPEEDS = new HashMap<>();
    static {
        TRAIL_SCROLL_SPEEDS.put("SRD_adloquium_shot", 0f);
        TRAIL_SCROLL_SPEEDS.put("SRD_adloquium_fake_shot", 0f);
        TRAIL_SCROLL_SPEEDS.put("SRD_skalla_shot", 400f);
        TRAIL_SCROLL_SPEEDS.put("SRD_phira_shock_shot", 400f);
        TRAIL_SCROLL_SPEEDS.put("SRD_benediction_msl", 600f);
        TRAIL_SCROLL_SPEEDS.put("SRD_divinity_shot", 300f);
        TRAIL_SCROLL_SPEEDS.put("SRD_equity_shot", 0f);
        TRAIL_SCROLL_SPEEDS.put("SRD_phira_impact_shot", 0f);
        TRAIL_SCROLL_SPEEDS.put("SRD_phira_burst_shot", 300f);
        TRAIL_SCROLL_SPEEDS.put("SRD_excogitation_shot", 0f);
        TRAIL_SCROLL_SPEEDS.put("SRD_arphage_shot", 0f);
        TRAIL_SCROLL_SPEEDS.put("SRD_veda_shot", 0f);
        TRAIL_SCROLL_SPEEDS.put("SRD_qoga_shot", 0f);
        TRAIL_SCROLL_SPEEDS.put("SRD_arciel_shot", 300f);
        TRAIL_SCROLL_SPEEDS.put("SRD_harmonius_shot", 300f);
    }
    private static final Map<String, Float> TRAIL_SPAWN_OFFSETS = new HashMap<>();
    static {
        TRAIL_SPAWN_OFFSETS.put("SRD_adloquium_shot", 0f);
        TRAIL_SPAWN_OFFSETS.put("SRD_adloquium_fake_shot", 0f);
        TRAIL_SPAWN_OFFSETS.put("SRD_skalla_shot", 0f);
        TRAIL_SPAWN_OFFSETS.put("SRD_phira_shock_shot", 0f);
        TRAIL_SPAWN_OFFSETS.put("SRD_benediction_msl", 0f);
        TRAIL_SPAWN_OFFSETS.put("SRD_divinity_shot", 0f);
        TRAIL_SPAWN_OFFSETS.put("SRD_equity_shot", 0f);
        TRAIL_SPAWN_OFFSETS.put("SRD_phira_impact_shot", 0f);
        TRAIL_SPAWN_OFFSETS.put("SRD_phira_burst_shot", 0f);
        TRAIL_SPAWN_OFFSETS.put("SRD_excogitation_shot", 0f);
        TRAIL_SPAWN_OFFSETS.put("SRD_arphage_shot", 0f);
        TRAIL_SPAWN_OFFSETS.put("SRD_veda_shot", 0f);
        TRAIL_SPAWN_OFFSETS.put("SRD_qoga_shot", 0f);
        TRAIL_SPAWN_OFFSETS.put("SRD_arciel_shot", 0f);
        TRAIL_SPAWN_OFFSETS.put("SRD_harmonius_shot", 0f);
    }

    @Override
    public void init(CombatEngineAPI engine) {
        //Reinitialize the lists
        projectileTrailIDs.clear();
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
            if (!TRAIL_SPRITES.keySet().contains(proj.getProjectileSpecId())) {
                continue;
            }
            String specID = proj.getProjectileSpecId();
            SpriteAPI spriteToUse = Global.getSettings().getSprite("SRD_fx", TRAIL_SPRITES.get(specID));

            //If we haven't already started a trail for this projectile, get an ID for it
            if (projectileTrailIDs.get(proj) == null) {
                projectileTrailIDs.put(proj, MagicTrailPlugin.getUniqueID());
            }

            //Gets a custom "offset" position, so we can slightly alter the spawn location to account for "natural fade-in", and add that to our spawn position
            Vector2f offsetPoint = new Vector2f((float)Math.cos(Math.toRadians(proj.getFacing())) * TRAIL_SPAWN_OFFSETS.get(specID), (float)Math.sin(Math.toRadians(proj.getFacing())) * TRAIL_SPAWN_OFFSETS.get(specID));
            Vector2f spawnPosition = new Vector2f(offsetPoint.x + proj.getLocation().x, offsetPoint.y + proj.getLocation().y);

            //CUSTOM: The Benediciton's missiles use a different trail, with randomness to the end
            if (specID.contains("SRD_benediction_msl")) {
                MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs.get(proj), spriteToUse, spawnPosition, 0f, MathUtils.getRandomNumberInRange(0f, 105f), proj.getFacing() - 180f,
                        0f, MathUtils.getRandomNumberInRange(-330f, 330f), START_SIZES.get(specID), END_SIZES.get(specID), TRAIL_START_COLORS.get(specID), TRAIL_END_COLORS.get(specID),
                        TRAIL_OPACITIES.get(specID), TRAIL_DURATIONS_IN.get(specID), TRAIL_DURATIONS_MAIN.get(specID), TRAIL_DURATIONS_OUT.get(specID), TRAIL_BLEND_SRC.get(specID),
                        TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID), new Vector2f(0f, 0f), null);
            } else {
                //Then, actually spawn a trail
                MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs.get(proj), spriteToUse, spawnPosition, 0f, 0f, proj.getFacing() - 180f,
                        0f, 0f, START_SIZES.get(specID), END_SIZES.get(specID), TRAIL_START_COLORS.get(specID), TRAIL_END_COLORS.get(specID),
                        TRAIL_OPACITIES.get(specID), TRAIL_DURATIONS_IN.get(specID), TRAIL_DURATIONS_MAIN.get(specID), TRAIL_DURATIONS_OUT.get(specID), TRAIL_BLEND_SRC.get(specID),
                        TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID), new Vector2f(0f, 0f), null);

                //The Purgatory uses dual-core blending
                if (specID.contains("SRD_skalla_shot")) {
                    //If we haven't already started a second trail for this projectile, get an ID for it
                    if (projectileTrailIDs2.get(proj) == null) {
                        projectileTrailIDs2.put(proj, MagicTrailPlugin.getUniqueID());
                    }
                    MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs2.get(proj), spriteToUse, spawnPosition, 0f, 0f, proj.getFacing() - 180f,
                            0f, 0f, START_SIZES.get(specID) / 2f, END_SIZES.get(specID) / 2f, Color.white, Color.white,
                            TRAIL_OPACITIES.get(specID), TRAIL_DURATIONS_IN.get(specID), TRAIL_DURATIONS_MAIN.get(specID), TRAIL_DURATIONS_OUT.get(specID), TRAIL_BLEND_SRC.get(specID),
                            TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID), new Vector2f(0f, 0f), null);
                }

                //The Phira is an evil Purgatory that I just hacked together from the purgatory trail code  -Nia
                if (specID.contains("SRD_phira_shock_shot")) {
                    //If we haven't already started a second trail for this projectile, get an ID for it
                    if (projectileTrailIDs2.get(proj) == null) {
                        projectileTrailIDs2.put(proj, MagicTrailPlugin.getUniqueID());
                    }
                    MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs2.get(proj), spriteToUse, spawnPosition, 0f, 0f, proj.getFacing() - 180f,
                            0f, 0f, START_SIZES.get(specID) / 2f, END_SIZES.get(specID) / 2f, Color.white, Color.white,
                            TRAIL_OPACITIES.get(specID), TRAIL_DURATIONS_IN.get(specID), TRAIL_DURATIONS_MAIN.get(specID), TRAIL_DURATIONS_OUT.get(specID), TRAIL_BLEND_SRC.get(specID),
                            TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID), new Vector2f(0f, 0f), null);
                }

                //The Equity has a whopping 3 trails, with custom behaviour to boot!
                if (specID.contains("SRD_equity_shot")) {
                    //If we haven't already started a second trail for this projectile, get an ID for it
                    if (projectileTrailIDs2.get(proj) == null) {
                        projectileTrailIDs2.put(proj, MagicTrailPlugin.getUniqueID());
                    }
                    MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs2.get(proj), spriteToUse, spawnPosition, MathUtils.getRandomNumberInRange(0f, 200f), MathUtils.getRandomNumberInRange(0f, 500f), proj.getFacing() - 180f,
                            MathUtils.getRandomNumberInRange(-200f, 200f), MathUtils.getRandomNumberInRange(-500f, 500f), START_SIZES.get(specID) * 2f, END_SIZES.get(specID) * 2f, TRAIL_START_COLORS.get(specID), TRAIL_END_COLORS.get(specID),
                            TRAIL_OPACITIES.get(specID), TRAIL_DURATIONS_IN.get(specID) * 0.4f, TRAIL_DURATIONS_MAIN.get(specID) * 0.5f, TRAIL_DURATIONS_OUT.get(specID) * 0.5f, TRAIL_BLEND_SRC.get(specID),
                            TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID), new Vector2f(0f, 0f), null);

                    //If we haven't already started a third trail for this projectile, get an ID for it
                    if (projectileTrailIDs3.get(proj) == null) {
                        projectileTrailIDs3.put(proj, MagicTrailPlugin.getUniqueID());
                    }
                    MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs3.get(proj), spriteToUse, spawnPosition, MathUtils.getRandomNumberInRange(0f, 220f), MathUtils.getRandomNumberInRange(0f, 600f), proj.getFacing() - 180f,
                            MathUtils.getRandomNumberInRange(-220f, 220f), MathUtils.getRandomNumberInRange(-500f, 500f), START_SIZES.get(specID) * 2f, END_SIZES.get(specID) * 2f, TRAIL_START_COLORS.get(specID), TRAIL_END_COLORS.get(specID),
                            TRAIL_OPACITIES.get(specID), TRAIL_DURATIONS_IN.get(specID) * 0.25f, TRAIL_DURATIONS_MAIN.get(specID) * 0.25f, TRAIL_DURATIONS_OUT.get(specID) * 0.25f, TRAIL_BLEND_SRC.get(specID),
                            TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID), new Vector2f(0f, 0f), null);
                }

                //The Ar Ciel is one angry gun it so needs some angry trails
                if (specID.contains("SRD_arciel_shot")) {
                    if (projectileTrailIDs2.get(proj) == null) {
                        projectileTrailIDs2.put(proj, MagicTrailPlugin.getUniqueID());
                    }
                    MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs2.get(proj), spriteToUse, spawnPosition, 20f, MathUtils.getRandomNumberInRange(0f, 105f), proj.getFacing() - 160f,
                            0f, MathUtils.getRandomNumberInRange(-330f, 330f), START_SIZES.get(specID), END_SIZES.get(specID), TRAIL_START_COLORS.get(specID), TRAIL_END_COLORS.get(specID),
                            0.4f, TRAIL_DURATIONS_IN.get(specID), TRAIL_DURATIONS_MAIN.get(specID), TRAIL_DURATIONS_OUT.get(specID), TRAIL_BLEND_SRC.get(specID),
                            TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID), new Vector2f(0f, 0f), null);

                    if (projectileTrailIDs3.get(proj) == null) {
                        projectileTrailIDs3.put(proj, MagicTrailPlugin.getUniqueID());
                    }
                    MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs3.get(proj), spriteToUse, spawnPosition, 20f, MathUtils.getRandomNumberInRange(0f, 105f), proj.getFacing() - 200f,
                            0f, MathUtils.getRandomNumberInRange(-330f, 330f), START_SIZES.get(specID), END_SIZES.get(specID), TRAIL_START_COLORS.get(specID), TRAIL_END_COLORS.get(specID),
                            0.4f, TRAIL_DURATIONS_IN.get(specID), TRAIL_DURATIONS_MAIN.get(specID), TRAIL_DURATIONS_OUT.get(specID), TRAIL_BLEND_SRC.get(specID),
                            TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID), new Vector2f(0f, 0f), null);

                }

                //The Qoga is a smaller Ar Ciel so we do the same trail diffusion here cause it looks sexy af for these kinda weapons
                if (specID.contains("SRD_qoga_shot")) {
                    if (projectileTrailIDs2.get(proj) == null) {
                        projectileTrailIDs2.put(proj, MagicTrailPlugin.getUniqueID());
                    }
                    MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs2.get(proj), spriteToUse, spawnPosition, 0f, MathUtils.getRandomNumberInRange(0f, 105f), proj.getFacing() - 180f,
                            0f, MathUtils.getRandomNumberInRange(-330f, 330f), START_SIZES.get(specID), END_SIZES.get(specID), TRAIL_START_COLORS.get(specID), TRAIL_END_COLORS.get(specID),
                            TRAIL_OPACITIES.get(specID), TRAIL_DURATIONS_IN.get(specID), TRAIL_DURATIONS_MAIN.get(specID), TRAIL_DURATIONS_OUT.get(specID), TRAIL_BLEND_SRC.get(specID),
                            TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID), new Vector2f(0f, 0f), null);

                }

                //Harmonius is a biiiig gun so it needs some fancy shit
                if (specID.contains("SRD_harmonius_shot")) {
                    if (projectileTrailIDs2.get(proj) == null) {
                        projectileTrailIDs2.put(proj, MagicTrailPlugin.getUniqueID());
                    }
                    MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs2.get(proj), spriteToUse, spawnPosition, 0f, MathUtils.getRandomNumberInRange(0f, 105f), proj.getFacing() - 180f,
                            0f, MathUtils.getRandomNumberInRange(-330f, 330f), 10f, 10f, new Color(205,100,255), new Color(205,100,255),
                            TRAIL_OPACITIES.get(specID), TRAIL_DURATIONS_IN.get(specID), 0.5f, TRAIL_DURATIONS_OUT.get(specID), TRAIL_BLEND_SRC.get(specID),
                            TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID), new Vector2f(0f, 0f), null);
                }
            }
        }
    }
}