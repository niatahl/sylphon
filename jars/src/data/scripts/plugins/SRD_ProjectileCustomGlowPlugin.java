//By Nicke535, handles the custom glow for certain projectiles
package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import data.scripts.util.MagicRender;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;

public class SRD_ProjectileCustomGlowPlugin extends BaseEveryFrameCombatPlugin {

    //A map of all the sprites used (note that all the sprites must be under SRD_fx): ensure this one has the same keys as the other maps
    private static final Map<String, String> SPRITES = new HashMap<String, String>();
    static {
        SPRITES.put("SRD_divinity_shot", "divinity_shot_glow");
    }

    //--------------------------------------THESE ARE ALL MAPS FOR DIFFERENT VISUAL STATS FOR THE TRAILS: THEIR NAMES ARE FAIRLY SELF_EXPLANATORY---------------------------------------------------
    private static final Map<String, Float> SIZES_X = new HashMap<String, Float>();
    static {
        SIZES_X.put("SRD_divinity_shot", 40f);
    }
    private static final Map<String, Float> SIZES_Y = new HashMap<String, Float>();
    static {
        SIZES_Y.put("SRD_divinity_shot", 175f);
    }
    private static final Map<String, Color> COLORS = new HashMap<String, Color>();
    static {
        COLORS.put("SRD_divinity_shot", new Color(250f/255f,65f/255f,255f/255f, 95f/255f));
    }

    @Override
    public void advance (float amount, List<InputEventAPI> events) {
        if (Global.getCombatEngine() == null) {
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
            if (!SPRITES.keySet().contains(proj.getProjectileSpecId())) {
                continue;
            }
            String specID = proj.getProjectileSpecId();
            SpriteAPI spriteToUse = Global.getSettings().getSprite("SRD_fx", SPRITES.get(specID));

            //An attempt at making the glow slightly related to fading
            float opacity = COLORS.get(specID).getAlpha() / 255f;
            if (proj.isFading() && proj.getWeapon() != null) {
                opacity *= (MathUtils.getDistance(proj, proj.getWeapon().getLocation()) - proj.getWeapon().getRange()) / (proj.getWeapon().getProjectileFadeRange() - proj.getWeapon().getRange());
            }
            //Extra insurance
            if (opacity < 0f) {opacity = 0f;}
            if (opacity > 1f) {opacity = 1f;}

            Color colorToUse = new Color(COLORS.get(specID).getRed()/255f, COLORS.get(specID).getGreen()/255f, COLORS.get(specID).getBlue()/255f, opacity);

            MagicRender.singleframe(spriteToUse, proj.getLocation(), new Vector2f(SIZES_X.get(specID), SIZES_Y.get(specID)), proj.getFacing() - 90f, colorToUse, true);
        }
    }
}