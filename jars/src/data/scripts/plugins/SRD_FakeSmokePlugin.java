//Based on the FakeBeam script by Tartiflette with DeathFly help, rebuilt and modified by Nicke535
//Creates fake smoke to circumvent the vanilla limit on allowed smoke particles
package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

public class SRD_FakeSmokePlugin extends BaseEveryFrameCombatPlugin {
    //SMOKE are the core of the script, storing all smoke particles' different attributes
    //A SMOKE Map, when added, contains:
    //  t - time, how long it takes for the smoke to fully dissipate
    //  st - start time, so we can know how much opacity has been lost
    //  s - size
    //  x - x-position
    //  y - y-position
    //  vx - x-velocity
    //  vy - y-velocity
    //  a - angle
    //  va - angular velocity
    //  o - starting opacity
    //  g - either 1, 2, 3 or 4: which sprite we want to draw
    //  cr - color (red)
    //  cg - color (green)
    //  cb - color (blue)
    public static List<Map<String,Float>> SMOKE = new ArrayList<Map<String,Float>>();

    //Set the function to access SMOKE from other scripts directly
    public static void addMemberDirectly(Map<String,Float> data) {
        SMOKE.add(data);
    }

    //A shorthand function for more "logical" adding to the script
    public static void addFakeSmoke(float time, float size, Vector2f position, Vector2f velocity, float angularVelocity, float opacity, Color renderColor) {
        Map<String, Float> data = new HashMap<>();
        data.put("t", time);
        data.put("st", time);
        data.put("s", size);
        data.put("x", position.x);
        data.put("y", position.y);
        data.put("vx", velocity.x);
        data.put("vy", velocity.y);
        data.put("a", MathUtils.getRandomNumberInRange(0f, 360f));
        data.put("va", angularVelocity);
        data.put("o", opacity);
        data.put("g", (float)MathUtils.getRandomNumberInRange(1, 5));
        data.put("cr", (renderColor.getRed() / 255f));
        data.put("cg", (renderColor.getGreen() / 255f));
        data.put("cb", (renderColor.getBlue() / 255f));
        SMOKE.add(data);
    }

    private List<Map<String,Float>> toRemove = new ArrayList<>();

    private SpriteAPI smoke1 = Global.getSettings().getSprite("SRD_fake_smoke", "1");
    private SpriteAPI smoke2 = Global.getSettings().getSprite("SRD_fake_smoke", "2");
    private SpriteAPI smoke3 = Global.getSettings().getSprite("SRD_fake_smoke", "3");
    private SpriteAPI smoke4 = Global.getSettings().getSprite("SRD_fake_smoke", "4");
    private SpriteAPI smoke5 = Global.getSettings().getSprite("SRD_fake_smoke", "5");

    @Override
    public void init(CombatEngineAPI engine) {
        //reinitialize the map
        SMOKE.clear();
    }

    @Override
    public void renderInWorldCoords(ViewportAPI view) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null){return;}

        if (!SMOKE.isEmpty()){
            float amount = (engine.isPaused() ? 0f : engine.getElapsedInLastFrame());

            //dig through the SMOKE
            for (Map< String,Float > entry : SMOKE) {

                //Time calculation
                float time = entry.get("t");
                time -= amount;

                //Adjusts position and angle
                float angleChange = entry.get("va") * amount;
                entry.put("a", entry.get("a") + angleChange);
                float xChange = entry.get("vx") * amount;
                entry.put("x", entry.get("x") + xChange);
                float yChange = entry.get("vy") * amount;
                entry.put("y", entry.get("y") + yChange);

                //If our time is up, remove the smoke
                if (time <= 0){
                    toRemove.add(entry);
                } else {

                    //Otherwise, check if we should be drawing the smoke based on current screen position
                    if (view.isNearViewport(new Vector2f(entry.get("x"), entry.get("y")), entry.get("s"))) {
                        float opacity = entry.get("o") * (entry.get("t") / entry.get("st"));
                        float sizeMod = 1f + (2f * (entry.get("st") - entry.get("t"))/entry.get("st"));

                        //Picks which graphics the smoke should have
                        SpriteAPI sprite = smoke1;
                        if (entry.get("g") == 2f) {
                            sprite = smoke2;
                        } else if (entry.get("g") == 3f) {
                            sprite = smoke3;
                        } else if (entry.get("g") == 4f) {
                            sprite = smoke4;
                        } else if (entry.get("g") == 5f) {
                            sprite = smoke5;
                        }

                        render(
                                sprite, //Sprite to draw
                                entry.get("s") * sizeMod, //Width entry
                                entry.get("s") * sizeMod, //Height entry
                                entry.get("a"), //Angle entry
                                opacity, //opacity duh!
                                entry.get("x"), //X position entry
                                entry.get("y"), //Y position entry
                                new Color(entry.get("cr"), entry.get("cg"), entry.get("cb"))
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
                    SMOKE.remove(w);
                }
                toRemove.clear();
            }
        }
    }

    //The render function
    private void render ( SpriteAPI sprite, float width, float height, float angle, float opacity, float posX, float posY, Color renderColor){
        //This part instantiates OpenGL
        glEnable(GL_BLEND);
        glEnable(GL_TEXTURE_2D);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glBindTexture(GL_TEXTURE_2D, sprite.getTextureId());
        glBegin(GL_QUADS);

        //Calculates widely-used variables
        float size = width / 2f;
        Vector2f position = new Vector2f(posX, posY);

        //Sets the current render color
        glColor4ub((byte)renderColor.getRed(),(byte)renderColor.getGreen(),(byte)renderColor.getBlue(),(byte)(renderColor.getAlpha() * opacity));

        //Sets corner 1
        glTexCoord2f(0, 1);
        Vector2f vec = MathUtils.getPointOnCircumference(position, size, angle + 315f);
        glVertex2f(vec.getX(),vec.getY());

        //Sets corner 2
        glTexCoord2f(0, 0);
        vec = MathUtils.getPointOnCircumference(position, size, angle + 225f);
        glVertex2f(vec.getX(),vec.getY());

        //Sets corner 3
        glTexCoord2f(1, 0);
        vec = MathUtils.getPointOnCircumference(position, size, angle + 135f);
        glVertex2f(vec.getX(),vec.getY());

        //Sets corner 4
        glTexCoord2f(1, 1);
        vec = MathUtils.getPointOnCircumference(position, size, angle + 45f);
        glVertex2f(vec.getX(),vec.getY());

        //And finally stops OpenGL
        glEnd();
    }
}