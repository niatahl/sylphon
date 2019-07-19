//Based on the FakeBeam script by Tartiflette with DeathFly help, rebuilt and modified by Nicke535
//Handles custom lens flare graphics
package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.*;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

public class SRD_LensFlarePlugin extends BaseEveryFrameCombatPlugin {
    //LENS_FLARES are the core of the script, storing all smoke particles' different attributes
    //A LENS_FLARES Map, when added, contains:
    //  it - in time
    //  mt - main time
    //  ot - out time
    //  st - start time, so we can know how much time has passed
    //  s - size
    //  x - x-position
    //  y - y-position
    //  o - maximum opacity
    //  cr - color (red)
    //  cg - color (green)
    //  cb - color (blue)
    private static List<Map<String,Float>> LENS_FLARES = new ArrayList<Map<String,Float>>();

    //Set the function to access LENS_FLARES from other scripts directly
    public static void addMemberDirectly(Map<String,Float> data) {
        LENS_FLARES.add(data);
    }

    //A shorthand function for more "logical" adding to the script
    public static void addMemberSimple(float inTime, float mainTime, float outTime, float size, Vector2f position, float opacity, Color renderColor) {
        Map<String, Float> data = new HashMap<>();
        data.put("it", inTime);
        data.put("mt", mainTime);
        data.put("ot", outTime);
        data.put("t", inTime+mainTime+outTime);
        data.put("s", size);
        data.put("x", position.x);
        data.put("y", position.y);
        data.put("o", opacity);
        data.put("cr", (renderColor.getRed() / 255f));
        data.put("cg", (renderColor.getGreen() / 255f));
        data.put("cb", (renderColor.getBlue() / 255f));
        LENS_FLARES.add(data);
    }

    private List<Map<String,Float>> toRemove = new ArrayList<>();

    private SpriteAPI sprite = Global.getSettings().getSprite("SRD_fx", "lens_flare");

    @Override
    public void init(CombatEngineAPI engine) {
        //reinitialize the map
        LENS_FLARES.clear();

        //Creates our layered render plugin and adds it to the engine
        SRD_LensFlareRenderer renderer = new SRD_LensFlareRenderer(this);
        engine.addLayeredRenderingPlugin(renderer);
    }

    void renderAllFlares(ViewportAPI view) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null){return;}

        if (!LENS_FLARES.isEmpty()){
            float amount = (engine.isPaused() ? 0f : engine.getElapsedInLastFrame());

            //dig through the flares
            for (Map< String,Float > entry : LENS_FLARES) {

                //Time calculation
                float time = entry.get("t");
                time -= amount;

                //If our time is up, remove the smoke
                if (time <= 0){
                    toRemove.add(entry);
                } else {

                    //Otherwise, check if we should be drawing the lens flare based on current screen position
                    if (view.isNearViewport(new Vector2f(entry.get("x"), entry.get("y")), entry.get("s") * 300f)) {
                        //Math for a smooth effect
                        float opacity = entry.get("o");
                        float sizeMod = 1f;
                        if (time > (entry.get("ot") + entry.get("mt"))) {
                            float timePart = (time - (entry.get("ot") + entry.get("mt"))) / entry.get("it");
                            timePart = 1f - timePart;
                            sizeMod = 0.35f + (0.65f * timePart * timePart * timePart * timePart);
                            opacity *= (float)Math.sqrt(timePart);
                        } else if (time < entry.get("ot")) {
                            float timePart = time / entry.get("ot");
                            sizeMod = 0.35f + (0.65f * timePart * timePart * timePart);
                            opacity *= timePart * timePart;
                        }

                        //Some jitter for good measure
                        sizeMod += MathUtils.getRandomNumberInRange(-0.06f, 0.06f);
                        opacity += MathUtils.getRandomNumberInRange(-0.08f, 0.08f);
                        if (opacity >= 1f) {
                            opacity = 1f;
                        } else if (opacity <= 0f) {
                            opacity = 0f;
                        }

                        SpriteAPI sprite = this.sprite;
                        render(
                                sprite, //Sprite to draw
                                entry.get("s") * sizeMod * 740f, //Width entry
                                entry.get("s") * sizeMod * 16f, //Height entry
                                opacity,        //opacity duh!
                                entry.get("x"), //X position entry
                                entry.get("y"), //Y position entry
                                entry.get("cr"),//R-color
                                entry.get("cg"),//G-color
                                entry.get("cb") //B-color
                        );
                    }

                    //Store the new time value
                    entry.put("t", time);
                }
            }
            //Remove the smoke that has faded out
            //Can't be done from within the iterator or it will fail when members will be missing
            if (!toRemove.isEmpty()){
                for(Map< String,Float > w : toRemove ){
                    LENS_FLARES.remove(w);
                }
                toRemove.clear();
            }
        }
    }

    //The render function
    private void render ( SpriteAPI sprite, float width, float height, float opacity, float posX, float posY, float renderColorR, float renderColorG, float renderColorB){
        //This part instantiates OpenGL
        glEnable(GL_BLEND);
        glEnable(GL_TEXTURE_2D);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);
        glBindTexture(GL_TEXTURE_2D, sprite.getTextureId());
        glBegin(GL_QUADS);

        //Sets the current render color
        Color renderColor = new Color(renderColorR, renderColorG, renderColorB, opacity);
        glColor4ub((byte)renderColor.getRed(),(byte)renderColor.getGreen(),(byte)renderColor.getBlue(),(byte)renderColor.getAlpha());

        //Randomizes a "flip" direction
        float flipX = 1f;
        float flipY = 1f;
        if (Math.random() < 0.5f) {
            flipX = -1f;
        }
        if (Math.random() < 0.5f) {
            flipY = -1f;
        }

        //Sets corner 1
        glTexCoord2f(0, 1);
        glVertex2f(posX-flipX*width/2f,posY+flipY*height/2f);

        //Sets corner 2
        glTexCoord2f(0, 0);
        glVertex2f(posX-flipX*width/2f,posY-flipY*height/2f);

        //Sets corner 3
        glTexCoord2f(1, 0);
        glVertex2f(posX+flipX*width/2f,posY-flipY*height/2f);

        //Sets corner 4
        glTexCoord2f(1, 1);
        glVertex2f(posX+flipX*width/2f,posY+flipY*height/2f);

        //And finally stops OpenGL
        glEnd();
    }
}

class SRD_LensFlareRenderer extends BaseCombatLayeredRenderingPlugin {
    private SRD_LensFlarePlugin parentPlugin;

    //Constructor
    SRD_LensFlareRenderer (SRD_LensFlarePlugin parentPlugin) {
        this.parentPlugin = parentPlugin;
    }

    //Render function; just here to time rendering and tell the main loop to run with a specific layer
    @Override
    public void render (CombatEngineLayers layer, ViewportAPI view) {
        //Initial checks to see if required components exist
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null){
            return;
        }

        //Only render on the proper layer
        if (getActiveLayers().contains(layer))

            //Calls our parent plugin's rendering function
            parentPlugin.renderAllFlares(view);
    }

    //We render everywhere, and on all layers (since we can't change these at runtime)
    @Override
    public float getRenderRadius() {
        return 999999999999999f;
    }
    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
    }
}