//By Nicke535, handles the stranger projectiles in the mod... really, i wanted to split this up, but we are running way too many plugins as it is
package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.ProjectileSpawnType;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
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
        TRAIL_SPRITES.put("SRD_adloquium_shot", "projectile_trail_core");
        TRAIL_SPRITES.put("SRD_adloquium_fake_shot", "projectile_trail_core");
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
        TRAIL_SPRITES.put("SRD_enochian_shot", "projectile_trail_zappy");
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
        TRAIL_DURATIONS_IN.put("SRD_enochian_shot", 0f);
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
        TRAIL_DURATIONS_MAIN.put("SRD_enochian_shot", 0f);
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
        TRAIL_DURATIONS_OUT.put("SRD_enochian_shot", 0.4f);
    }
    private static final Map<String, Float> START_SIZES = new HashMap<>();
    static {
        START_SIZES.put("SRD_adloquium_shot", 10f);
        START_SIZES.put("SRD_adloquium_fake_shot", 10f);
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
        START_SIZES.put("SRD_enochian_shot", 8f);
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
        END_SIZES.put("SRD_enochian_shot", 5f);
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
        TRAIL_START_COLORS.put("SRD_enochian_shot", new Color(115,185,255));
    }
    private static final Map<String, Color> TRAIL_END_COLORS = new HashMap<>();
    static {
        TRAIL_END_COLORS.put("SRD_adloquium_shot", new Color(255,75,205));
        TRAIL_END_COLORS.put("SRD_adloquium_fake_shot", new Color(255,45,165));
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
        TRAIL_END_COLORS.put("SRD_enochian_shot", new Color(85,185,255));
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
        TRAIL_OPACITIES.put("SRD_enochian_shot", 0.6f);
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
        TRAIL_BLEND_SRC.put("SRD_enochian_shot", GL_SRC_ALPHA);
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
        TRAIL_BLEND_DEST.put("SRD_enochian_shot", GL_ONE);
    }
    private static final Map<String, Float> TRAIL_LOOP_LENGTHS = new HashMap<>();
    static {
        TRAIL_LOOP_LENGTHS.put("SRD_adloquium_shot", 400f);
        TRAIL_LOOP_LENGTHS.put("SRD_adloquium_fake_shot", 300f);
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
        TRAIL_LOOP_LENGTHS.put("SRD_enochian_shot", 400f);
    }
    private static final Map<String, Float> TRAIL_SCROLL_SPEEDS = new HashMap<>();
    static {
        TRAIL_SCROLL_SPEEDS.put("SRD_adloquium_shot", 400f);
        TRAIL_SCROLL_SPEEDS.put("SRD_adloquium_fake_shot", 400f);
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
        TRAIL_SCROLL_SPEEDS.put("SRD_enochian_shot", 400f);
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
        TRAIL_SPAWN_OFFSETS.put("SRD_enochian_shot", 5f);
    }
    //NEW: compensates for lateral movement of a projectile. Should generally be 0f in most cases, due to some oddities
    //in behaviour with direction-changing scripts, but can be helpful for aligning certain projectiles
    private static final Map<String, Float> LATERAL_COMPENSATION_MULT = new HashMap<>();
    static {
        LATERAL_COMPENSATION_MULT.put("SRD_adloquium_shot", 0f);
        LATERAL_COMPENSATION_MULT.put("SRD_adloquium_fake_shot", 0f);
        LATERAL_COMPENSATION_MULT.put("SRD_skalla_shot", 1f);
        LATERAL_COMPENSATION_MULT.put("SRD_phira_shock_shot", 1f);
        LATERAL_COMPENSATION_MULT.put("SRD_benediction_msl", 0f);
        LATERAL_COMPENSATION_MULT.put("SRD_divinity_shot", 1f);
        LATERAL_COMPENSATION_MULT.put("SRD_equity_shot", 1f);
        LATERAL_COMPENSATION_MULT.put("SRD_phira_impact_shot", 0f);
        LATERAL_COMPENSATION_MULT.put("SRD_phira_burst_shot", 0f);
        LATERAL_COMPENSATION_MULT.put("SRD_excogitation_shot", 0f);
        LATERAL_COMPENSATION_MULT.put("SRD_arphage_shot", 0f);
        LATERAL_COMPENSATION_MULT.put("SRD_veda_shot", 0f);
        LATERAL_COMPENSATION_MULT.put("SRD_qoga_shot", 0f);
        LATERAL_COMPENSATION_MULT.put("SRD_arciel_shot", 0f);
        LATERAL_COMPENSATION_MULT.put("SRD_harmonius_shot", 1f);
        LATERAL_COMPENSATION_MULT.put("SRD_enochian_shot", 0f);
    }
    //NEW: whether a shot's trail loses opacity as the projectile fades out. Should generally be true, but may need to
    //be set to false on some scripted weapons. Has no real effect on flak rounds or missiles, and should thus be set
    //false for those
    private static final Map<String, Boolean> FADE_OUT_FADES_TRAIL = new HashMap<>();
    static {
        FADE_OUT_FADES_TRAIL.put("SRD_adloquium_shot", true);
        FADE_OUT_FADES_TRAIL.put("SRD_adloquium_fake_shot", true);
        FADE_OUT_FADES_TRAIL.put("SRD_skalla_shot", true);
        FADE_OUT_FADES_TRAIL.put("SRD_phira_shock_shot", true);
        FADE_OUT_FADES_TRAIL.put("SRD_benediction_msl", false);
        FADE_OUT_FADES_TRAIL.put("SRD_divinity_shot", true);
        FADE_OUT_FADES_TRAIL.put("SRD_equity_shot", true);
        FADE_OUT_FADES_TRAIL.put("SRD_phira_impact_shot", true);
        FADE_OUT_FADES_TRAIL.put("SRD_phira_burst_shot", false);
        FADE_OUT_FADES_TRAIL.put("SRD_excogitation_shot", true);
        FADE_OUT_FADES_TRAIL.put("SRD_arphage_shot", true);
        FADE_OUT_FADES_TRAIL.put("SRD_veda_shot", true);
        FADE_OUT_FADES_TRAIL.put("SRD_qoga_shot", true);
        FADE_OUT_FADES_TRAIL.put("SRD_arciel_shot", true);
        FADE_OUT_FADES_TRAIL.put("SRD_harmonius_shot", true);
        FADE_OUT_FADES_TRAIL.put("SRD_enochian_shot", true);
    }
    //NEW: whether a shot should have its direction adjusted to face the same way as its velocity vector, thus
    //helping with trail alignment for projectiles without using lateral compensation. DOES NOT WORK FOR
    //PROJECTILES SPAWNED WITH BALLISTIC_AS_BEAM AS SPAWNTYPE, and should not be used on missiles
    private static final Map<String, Boolean> PROJECTILE_ANGLE_ADJUSTMENT = new HashMap<>();
    static {
        PROJECTILE_ANGLE_ADJUSTMENT.put("SRD_adloquium_shot", true);
        PROJECTILE_ANGLE_ADJUSTMENT.put("SRD_adloquium_fake_shot", true);
        PROJECTILE_ANGLE_ADJUSTMENT.put("SRD_skalla_shot", false);
        PROJECTILE_ANGLE_ADJUSTMENT.put("SRD_phira_shock_shot", false);
        PROJECTILE_ANGLE_ADJUSTMENT.put("SRD_benediction_msl", false);
        PROJECTILE_ANGLE_ADJUSTMENT.put("SRD_divinity_shot", false);
        PROJECTILE_ANGLE_ADJUSTMENT.put("SRD_equity_shot", false);
        PROJECTILE_ANGLE_ADJUSTMENT.put("SRD_phira_impact_shot", true);
        PROJECTILE_ANGLE_ADJUSTMENT.put("SRD_phira_burst_shot", true);
        PROJECTILE_ANGLE_ADJUSTMENT.put("SRD_excogitation_shot", true);
        PROJECTILE_ANGLE_ADJUSTMENT.put("SRD_arphage_shot", true);
        PROJECTILE_ANGLE_ADJUSTMENT.put("SRD_veda_shot", true);
        PROJECTILE_ANGLE_ADJUSTMENT.put("SRD_qoga_shot", true);
        PROJECTILE_ANGLE_ADJUSTMENT.put("SRD_arciel_shot", true);
        PROJECTILE_ANGLE_ADJUSTMENT.put("SRD_harmonius_shot", false);
        PROJECTILE_ANGLE_ADJUSTMENT.put("SRD_enochian_shot", true);
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
            if (!TRAIL_SPRITES.keySet().contains(proj.getProjectileSpecId())) {
                continue;
            }

            //-------------------------------------------For visual effects---------------------------------------------
            String specID = proj.getProjectileSpecId();
            SpriteAPI spriteToUse = Global.getSettings().getSprite("SRD_fx", TRAIL_SPRITES.get(specID));
            Vector2f projVel = new Vector2f(proj.getVelocity());

            //If we use angle adjustment, do that here
            if (PROJECTILE_ANGLE_ADJUSTMENT.get(specID) && projVel.length() > 0.1f && !proj.getSpawnType().equals(ProjectileSpawnType.BALLISTIC_AS_BEAM)) {
                proj.setFacing(VectorUtils.getAngle(new Vector2f(0f, 0f), projVel));
            }

            //If we haven't already started a trail for this projectile, get an ID for it
            if (projectileTrailIDs.get(proj) == null) {
                projectileTrailIDs.put(proj, MagicTrailPlugin.getUniqueID());

                //Fix for some first-frame error shenanigans
                if (projVel.length() < 0.1f && proj.getSource() != null) {
                    projVel = new Vector2f(proj.getSource().getVelocity());
                }
            }

            //Gets a custom "offset" position, so we can slightly alter the spawn location to account for "natural fade-in", and add that to our spawn position
            Vector2f offsetPoint = new Vector2f((float)Math.cos(Math.toRadians(proj.getFacing())) * TRAIL_SPAWN_OFFSETS.get(specID), (float)Math.sin(Math.toRadians(proj.getFacing())) * TRAIL_SPAWN_OFFSETS.get(specID));
            Vector2f spawnPosition = new Vector2f(offsetPoint.x + proj.getLocation().x, offsetPoint.y + proj.getLocation().y);

            //Sideway offset velocity, for projectiles that use it
            Vector2f projBodyVel = VectorUtils.rotate(projVel, -proj.getFacing());
            Vector2f projLateralBodyVel = new Vector2f(0f, projBodyVel.getY());
            Vector2f sidewayVel = (Vector2f)VectorUtils.rotate(projLateralBodyVel, proj.getFacing()).scale(LATERAL_COMPENSATION_MULT.get(specID));

            //Opacity adjustment for fade-out, if the projectile uses it
            float opacityMult = 1f;
            if (FADE_OUT_FADES_TRAIL.get(specID) && proj.isFading()) {
                opacityMult = proj.getDamageAmount() / proj.getBaseDamageAmount();
            }

            //CUSTOM: The Benediciton's missiles use a different trail, with randomness to the end
            if (specID.contains("SRD_benediction_msl")) {
                MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs.get(proj), spriteToUse, spawnPosition, 0f, MathUtils.getRandomNumberInRange(0f, 105f), proj.getFacing() - 180f,
                        0f, MathUtils.getRandomNumberInRange(-330f, 330f), START_SIZES.get(specID), END_SIZES.get(specID), TRAIL_START_COLORS.get(specID), TRAIL_END_COLORS.get(specID),
                        TRAIL_OPACITIES.get(specID) * opacityMult, TRAIL_DURATIONS_IN.get(specID), TRAIL_DURATIONS_MAIN.get(specID), TRAIL_DURATIONS_OUT.get(specID), TRAIL_BLEND_SRC.get(specID),
                        TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID),sidewayVel, null);
            } else {
                //Then, actually spawn a trail
                MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs.get(proj), spriteToUse, spawnPosition, 0f, 0f, proj.getFacing() - 180f,
                        0f, 0f, START_SIZES.get(specID), END_SIZES.get(specID), TRAIL_START_COLORS.get(specID), TRAIL_END_COLORS.get(specID),
                        TRAIL_OPACITIES.get(specID) * opacityMult, TRAIL_DURATIONS_IN.get(specID), TRAIL_DURATIONS_MAIN.get(specID), TRAIL_DURATIONS_OUT.get(specID), TRAIL_BLEND_SRC.get(specID),
                        TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID), sidewayVel, null);

                //The Purgatory uses dual-core blending
                if (specID.contains("SRD_skalla_shot")) {
                    //If we haven't already started a second trail for this projectile, get an ID for it
                    if (projectileTrailIDs2.get(proj) == null) {
                        projectileTrailIDs2.put(proj, MagicTrailPlugin.getUniqueID());
                    }
                    MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs2.get(proj), spriteToUse, spawnPosition, 0f, 0f, proj.getFacing() - 180f,
                            0f, 0f, START_SIZES.get(specID) / 2f, END_SIZES.get(specID) / 2f, Color.white, Color.white,
                            TRAIL_OPACITIES.get(specID) * opacityMult, TRAIL_DURATIONS_IN.get(specID), TRAIL_DURATIONS_MAIN.get(specID), TRAIL_DURATIONS_OUT.get(specID), TRAIL_BLEND_SRC.get(specID),
                            TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID), sidewayVel, null);
                }

                //Making the Adlo a bit fancier
                if (specID.contains("SRD_adloquium_shot")) {
                    //If we haven't already started a second trail for this projectile, get an ID for it
                    if (projectileTrailIDs2.get(proj) == null) {
                        projectileTrailIDs2.put(proj, MagicTrailPlugin.getUniqueID());
                    }
                    MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs2.get(proj), Global.getSettings().getSprite("SRD_fx", "projectile_trail_fringe"), spawnPosition, 0f, 0f, proj.getFacing() - 180f,
                            0f, 0f, START_SIZES.get(specID) * 2f, END_SIZES.get(specID) * 2f, new Color(67, 53,215), new Color(67,53,215),
                            0.4f * opacityMult, TRAIL_DURATIONS_IN.get(specID), TRAIL_DURATIONS_MAIN.get(specID), TRAIL_DURATIONS_OUT.get(specID)*1.5f, TRAIL_BLEND_SRC.get(specID),
                            TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID), sidewayVel, null);
                }

                if (specID.contains("SRD_adloquium_fake_shot")) {
                    //If we haven't already started a second trail for this projectile, get an ID for it
                    if (projectileTrailIDs2.get(proj) == null) {
                        projectileTrailIDs2.put(proj, MagicTrailPlugin.getUniqueID());
                    }
                    MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs2.get(proj), Global.getSettings().getSprite("SRD_fx", "projectile_trail_fringe"), spawnPosition, 0f, 0f, proj.getFacing() - 180f,
                            0f, 0f, START_SIZES.get(specID) * 2f, END_SIZES.get(specID) * 2f, new Color(67,53,215), new Color(67,53,215),
                            0.4f * opacityMult, TRAIL_DURATIONS_IN.get(specID), TRAIL_DURATIONS_MAIN.get(specID), TRAIL_DURATIONS_OUT.get(specID)*1.5f, TRAIL_BLEND_SRC.get(specID),
                            TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID), sidewayVel, null);
                }

                //The Phira is an evil Purgatory that I just hacked together from the purgatory trail code  -Nia
                if (specID.contains("SRD_phira_shock_shot")) {
                    //If we haven't already started a second trail for this projectile, get an ID for it
                    if (projectileTrailIDs2.get(proj) == null) {
                        projectileTrailIDs2.put(proj, MagicTrailPlugin.getUniqueID());
                    }
                    MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs2.get(proj), spriteToUse, spawnPosition, 0f, 0f, proj.getFacing() - 180f,
                            0f, 0f, START_SIZES.get(specID) / 2f, END_SIZES.get(specID) / 2f, Color.white, Color.white,
                            TRAIL_OPACITIES.get(specID) * opacityMult, TRAIL_DURATIONS_IN.get(specID), TRAIL_DURATIONS_MAIN.get(specID), TRAIL_DURATIONS_OUT.get(specID), TRAIL_BLEND_SRC.get(specID),
                            TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID), sidewayVel, null);
                }

                //The Equity has a whopping 3 trails, with custom behaviour to boot!
                if (specID.contains("SRD_equity_shot")) {
                    //If we haven't already started a second trail for this projectile, get an ID for it
                    if (projectileTrailIDs2.get(proj) == null) {
                        projectileTrailIDs2.put(proj, MagicTrailPlugin.getUniqueID());
                    }
                    MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs2.get(proj), spriteToUse, spawnPosition, MathUtils.getRandomNumberInRange(0f, 200f), MathUtils.getRandomNumberInRange(0f, 500f), proj.getFacing() - 180f,
                            MathUtils.getRandomNumberInRange(-200f, 200f), MathUtils.getRandomNumberInRange(-500f, 500f), START_SIZES.get(specID) * 2f, END_SIZES.get(specID) * 2f, TRAIL_START_COLORS.get(specID), TRAIL_END_COLORS.get(specID),
                            TRAIL_OPACITIES.get(specID) * opacityMult, TRAIL_DURATIONS_IN.get(specID) * 0.4f, TRAIL_DURATIONS_MAIN.get(specID) * 0.5f, TRAIL_DURATIONS_OUT.get(specID) * 0.5f, TRAIL_BLEND_SRC.get(specID),
                            TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID), sidewayVel, null);

                    //If we haven't already started a third trail for this projectile, get an ID for it
                    if (projectileTrailIDs3.get(proj) == null) {
                        projectileTrailIDs3.put(proj, MagicTrailPlugin.getUniqueID());
                    }
                    MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs3.get(proj), spriteToUse, spawnPosition, MathUtils.getRandomNumberInRange(0f, 220f), MathUtils.getRandomNumberInRange(0f, 600f), proj.getFacing() - 180f,
                            MathUtils.getRandomNumberInRange(-220f, 220f), MathUtils.getRandomNumberInRange(-500f, 500f), START_SIZES.get(specID) * 2f, END_SIZES.get(specID) * 2f, TRAIL_START_COLORS.get(specID), TRAIL_END_COLORS.get(specID),
                            TRAIL_OPACITIES.get(specID) * opacityMult, TRAIL_DURATIONS_IN.get(specID) * 0.25f, TRAIL_DURATIONS_MAIN.get(specID) * 0.25f, TRAIL_DURATIONS_OUT.get(specID) * 0.25f, TRAIL_BLEND_SRC.get(specID),
                            TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID), sidewayVel, null);
                }

                //The Ar Ciel is one angry gun it so needs some angry trails
                if (specID.contains("SRD_arciel_shot")) {
                    if (projectileTrailIDs2.get(proj) == null) {
                        projectileTrailIDs2.put(proj, MagicTrailPlugin.getUniqueID());
                    }
                    MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs2.get(proj), spriteToUse, spawnPosition, 20f, MathUtils.getRandomNumberInRange(0f, 105f), proj.getFacing() - 160f,
                            0f, MathUtils.getRandomNumberInRange(-330f, 330f), START_SIZES.get(specID), END_SIZES.get(specID), TRAIL_START_COLORS.get(specID), TRAIL_END_COLORS.get(specID),
                            0.4f * opacityMult, TRAIL_DURATIONS_IN.get(specID), TRAIL_DURATIONS_MAIN.get(specID), TRAIL_DURATIONS_OUT.get(specID), TRAIL_BLEND_SRC.get(specID),
                            TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID), sidewayVel, null);

                    if (projectileTrailIDs3.get(proj) == null) {
                        projectileTrailIDs3.put(proj, MagicTrailPlugin.getUniqueID());
                    }
                    MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs3.get(proj), spriteToUse, spawnPosition, 20f, MathUtils.getRandomNumberInRange(0f, 105f), proj.getFacing() - 200f,
                            0f, MathUtils.getRandomNumberInRange(-330f, 330f), START_SIZES.get(specID), END_SIZES.get(specID), TRAIL_START_COLORS.get(specID), TRAIL_END_COLORS.get(specID),
                            0.4f * opacityMult, TRAIL_DURATIONS_IN.get(specID), TRAIL_DURATIONS_MAIN.get(specID), TRAIL_DURATIONS_OUT.get(specID), TRAIL_BLEND_SRC.get(specID),
                            TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID), sidewayVel, null);

                }

                //The Qoga is a smaller Ar Ciel so we do the same trail diffusion here cause it looks sexy af for these kinda weapons
                if (specID.contains("SRD_qoga_shot")) {
                    if (projectileTrailIDs2.get(proj) == null) {
                        projectileTrailIDs2.put(proj, MagicTrailPlugin.getUniqueID());
                    }
                    MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs2.get(proj), spriteToUse, spawnPosition, 0f, MathUtils.getRandomNumberInRange(0f, 105f), proj.getFacing() - 180f,
                            0f, MathUtils.getRandomNumberInRange(-330f, 330f), START_SIZES.get(specID), END_SIZES.get(specID), TRAIL_START_COLORS.get(specID), TRAIL_END_COLORS.get(specID),
                            TRAIL_OPACITIES.get(specID) * opacityMult, TRAIL_DURATIONS_IN.get(specID), TRAIL_DURATIONS_MAIN.get(specID), TRAIL_DURATIONS_OUT.get(specID), TRAIL_BLEND_SRC.get(specID),
                            TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID), sidewayVel, null);

                }

                //Harmonius is a biiiig gun so it needs some fancy shit
                if (specID.contains("SRD_harmonius_shot")) {
                    if (projectileTrailIDs2.get(proj) == null) {
                        projectileTrailIDs2.put(proj, MagicTrailPlugin.getUniqueID());
                    }
                    MagicTrailPlugin.AddTrailMemberAdvanced(proj, projectileTrailIDs2.get(proj), spriteToUse, spawnPosition, 0f, MathUtils.getRandomNumberInRange(0f, 105f), proj.getFacing() - 180f,
                            0f, MathUtils.getRandomNumberInRange(-330f, 330f), 10f, 10f, new Color(205,100,255), new Color(205,100,255),
                            TRAIL_OPACITIES.get(specID) * opacityMult, TRAIL_DURATIONS_IN.get(specID), 0.5f, TRAIL_DURATIONS_OUT.get(specID), TRAIL_BLEND_SRC.get(specID),
                            TRAIL_BLEND_DEST.get(specID), TRAIL_LOOP_LENGTHS.get(specID), TRAIL_SCROLL_SPEEDS.get(specID), sidewayVel, null);
                }
            }
        }
    }
}