//Based on the FakeBeam script by Tartiflette with DeathFly help, rebuilt and modified by Nicke535
//Tracks "Spikes" in relation to enemy ships, and handles their detonation and rendering. The spikes need to be spawned by some other script
//
//Also has an additional code for replacing all veritas_temp_shot with their actual projectiles
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

public class SRD_VeritasTrackerPlugin extends BaseEveryFrameCombatPlugin {
    //The color of the explosions
    private static Color EXPLOSION_COLOR = new Color(0.20f, 0.03f, 0.30f, 1f);

    //SPLINTERS and SHIPS are the core of the script, storing all spikes's different attributes and which ship each spike belongs to
    //A SPIKES Map, when added, contains:
    //  t - time, how long it takes for the spike to detonate
    //  w - width
    //  h - height
    //  x - x-position (RELATIVE TO THE ENEMY SHIP)
    //  y - y-position (RELATIVE TO THE ENEMY SHIP)
    //  a - angle (RELATIVE TO THE ENEMY SHIP)
    //  d - damage when detonated
    //  g - either 1, 2, 3 or 4: which sprite we want to draw
    private List<Map<String,Float>> SPIKES = new ArrayList<Map<String,Float>>();
    private Map<Float,ShipAPI> SHIPS = new HashMap<>();
    private float CURRENT_ID = 0f;

    //Set the function to access SPIKES from the weapons scripts
    public static void addMember(Map<String,Float> data, ShipAPI ship) {
        //First, find the plugin
        if (Global.getCombatEngine() == null) {
            return;
        } else if (!(Global.getCombatEngine().getCustomData().get("SRD_VeritasTrackerPlugin") instanceof SRD_VeritasTrackerPlugin)) {
            return;
        }
        SRD_VeritasTrackerPlugin plugin = (SRD_VeritasTrackerPlugin)Global.getCombatEngine().getCustomData().get("SRD_VeritasTrackerPlugin");

        data.put("SHIP", plugin.CURRENT_ID);
        plugin.SHIPS.put(plugin.CURRENT_ID, ship);
        plugin.CURRENT_ID += 0.1f;
        plugin.SPIKES.add(data);

        if (plugin.CURRENT_ID > 2000f) {
            plugin.CURRENT_ID = 0f;
        }
    }

    private List<Map<String,Float>> toRemove = new ArrayList<>();

    private SpriteAPI splinter1 = Global.getSettings().getSprite("SRD_veritas_shots", "1");
    private SpriteAPI splinter2 = Global.getSettings().getSprite("SRD_veritas_shots", "2");
    private SpriteAPI splinter3 = Global.getSettings().getSprite("SRD_veritas_shots", "3");
    private SpriteAPI splinter4 = Global.getSettings().getSprite("SRD_veritas_shots", "4");

    @Override
    public void init(CombatEngineAPI engine) {
        //Stores our plugin in an easy-to-reach location, so we can access in in different places
        engine.getCustomData().put("SRD_VeritasTrackerPlugin", this);

        //reinitialize the map
        SPIKES.clear();
        SHIPS.clear();
        CURRENT_ID = 0f;
    }

    @Override
    public void advance (float amount, List<InputEventAPI> events) {
        if (Global.getCombatEngine() == null || Global.getCombatEngine().isPaused()) {
            return;
        }
        CombatEngineAPI engine = Global.getCombatEngine();

        //Removes all varitas_temp_shot and replace them with the real projectiles, while adjusting damage accordingly
        List<DamagingProjectileAPI> removeList = new ArrayList<DamagingProjectileAPI>();
        for (DamagingProjectileAPI proj : engine.getProjectiles()) {
            if (proj.getProjectileSpecId() == null) {
                continue;
            }
            if (proj.getProjectileSpecId().contains("SRD_veritas_temp_shot")) {
                DamagingProjectileAPI newProj = (DamagingProjectileAPI) engine.spawnProjectile(proj.getSource(), proj.getWeapon(), "SRD_veritas_fake", proj.getLocation(), proj.getFacing(), proj.getSource().getVelocity());
                newProj.setDamageAmount(proj.getDamageAmount() / 2f);
                removeList.add(proj);
            }
        }
        for (DamagingProjectileAPI proj : removeList) {
            engine.removeEntity(proj);
        }
    }

    @Override
    public void renderInWorldCoords(ViewportAPI view) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null){return;}

        if (!SPIKES.isEmpty()){
            float amount = (engine.isPaused() ? 0f : engine.getElapsedInLastFrame());

            //dig through the SPLINTERS
            for (Map< String,Float > entry : SPIKES) {

                //Time calculation
                float time = entry.get("t");
                time -= amount;

                //Saves the ship to a variable to save some code later on
                ShipAPI ship = SHIPS.get(entry.get("SHIP"));

                //If we don't have a ship, remove the spike
                if (ship == null) {
                    toRemove.add(entry);
                    SHIPS.remove(entry.get("SHIP"));
                    continue;
                }

                //If our time is up OR our ship was just blown to pieces OR the ship started phasing, detonate the spike and remove it
                if (time <= 0 || ship.isPiece() || ship.isPhased()){
                    handleExplosion(entry, ship);
                    toRemove.add(entry);
                    SHIPS.remove(entry.get("SHIP"));
                } else {
                    //Otherwise, we start drawing the spike
                    float opacity = 0.8f;

                    //Picks which graphics the spike should have
                    SpriteAPI sprite = splinter1;
                    if (entry.get("g") == 2f) {
                        sprite = splinter2;
                    } else if (entry.get("g") == 3f) {
                        sprite = splinter3;
                    } else if (entry.get("g") == 4f) {
                        sprite = splinter4;
                    }

                    render(
                            sprite, //Sprite to draw
                            entry.get("w"), //Width entry
                            entry.get("h"), //Height entry
                            entry.get("a"), //Angle entry, relative to ship
                            opacity, //opacity duh!
                            entry.get("x"), //X position entry, relative to ship
                            entry.get("y"), //Y position entry, relative to ship
                            ship
                    );

                    //and store the new time value
                    entry.put("t", time);
                }
            }
            //Remove the splinters that faded out
            //Can't be done from within the iterator or it will fail when members will be missing
            if (!toRemove.isEmpty()){
                for(Map< String,Float > w : toRemove ){
                    SPIKES.remove(w);
                }
                toRemove.clear();
            }
        }
    }

    //The render function
    private void render ( SpriteAPI sprite, float width, float height, float angle, float opacity, float posX, float posY, ShipAPI ship){
        sprite.setAlphaMult(opacity);
        sprite.setSize(width, height);
        sprite.setAngle(angle-90 + ship.getFacing());

        Vector2f renderPos = new Vector2f(posX, posY);
        renderPos = VectorUtils.rotateAroundPivot(renderPos, new Vector2f(0f, 0f), ship.getFacing(), new Vector2f(0f, 0f));
        renderPos.x += ship.getLocation().x;
        renderPos.y += ship.getLocation().y;

        sprite.renderAtCenter(renderPos.x, renderPos.y);
    }

    //Handles the explosion-damage of the spikes. Should ALWAYS be called just before removing a spike (unless the ship it was attached to suddenly disappears)
    private void handleExplosion ( Map< String,Float > spike, ShipAPI ship){
        if (ship != null) {
            float posX = spike.get("x");
            float posY = spike.get("y");
            Vector2f damagePos = new Vector2f(posX, posY);
            damagePos = VectorUtils.rotateAroundPivot(damagePos, new Vector2f(0f, 0f), ship.getFacing(), new Vector2f(0f, 0f));
            damagePos.x += ship.getLocation().x;
            damagePos.y += ship.getLocation().y;

            Global.getCombatEngine().applyDamage(ship, damagePos, spike.get("d"), DamageType.HIGH_EXPLOSIVE, 0, true, false, null, true);
            Global.getCombatEngine().spawnExplosion(damagePos, new Vector2f(0f, 0f), EXPLOSION_COLOR ,130f, 0.7f);
            for (int i = 0; i < 3; i++) {
                Global.getCombatEngine().addHitParticle(damagePos, MathUtils.getRandomPointInCircle(null, 2f), MathUtils.getRandomNumberInRange(8f, 12f), 90f , MathUtils.getRandomNumberInRange(0.2f, 0.5f), EXPLOSION_COLOR);
            }
            //Global.getSoundPlayer().playSound("nictoy_piercer_detonation", 1f, 1f, damagePos, new Vector2f(0f, 0f));
        }
    }
}