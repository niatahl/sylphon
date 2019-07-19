//By Nicke535
//Controls special "nanobot swarms" that are created by a certain Sylphon ship
package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;

public class SRD_NanobotsPlugin extends BaseEveryFrameCombatPlugin {

    /* I WAS TOO LAZY TO SET UP PROPER TRAIL VARIABLES: EDIT THIS FUNCTION MANUALLY INSTEAD */
    private void spawnTrailPiece (NanobotData bot) {
        //Oh, and remember that this is one of those unique times where the trail is un-cutable; null as linked entity
        //  And if you're wondering: Misc.ZERO is just "new Vector2f(0f, 0f)" more compactly written
        MagicTrailPlugin.AddTrailMemberAdvanced(null, nanobotTrailMap.get(bot), Global.getSettings().getSprite("SRD_fx", "nanobot_trail"),
                bot.position, 0f, 0f, bot.currentAngle, 0f, 0f, bot.carriedMass*SIZE_MULT,
                bot.carriedMass*SIZE_MULT, Color.WHITE, Color.WHITE, 0.8f, 0f, 0.30f, 0.1f, GL_SRC_ALPHA,
                GL_ONE_MINUS_SRC_ALPHA,128f, -300f, Misc.ZERO, null, CombatEngineLayers.CONTRAILS_LAYER);
    }

    //The sound bots make when arriving at their home ship and deliver their cargo
    private static final String ARRIVAL_SOUND = "ui_cargo_ore_drop";

    //The volume of the arrival sound, this is then multiplied by x/100, where x is the amount of "metal"
    //carried by the swarm
    private static final float SOUND_VOLUME_SCALE = 0.07f;

    //The size of a given nanobot swarm's width, in SU, per piece of "metal" it has
    private static final float SIZE_MULT = 0.9f;

    //The ID used for storing all "metal" as used in the plugin
    public static final String EFFECT_METAL_ID = "SRD_NanobotsMetalEffectID";

    //In-script variable; keeps track of our nanobots
    private List<NanobotData> nanobotList = new ArrayList<>();

    //In-script variable; keeps track of trail IDs for all the bots
    private Map<NanobotData, Float> nanobotTrailMap = new HashMap<>();

    @Override
    public void init(CombatEngineAPI engine) {
        //Stores our plugin in an easy-to-reach location, so we can access in in different places
        engine.getCustomData().put("SRD_NanobotsPlugin", this);

        //Clean up our nanobot list, if it for some reason has been contaminated
        nanobotList.clear();
    }

    @Override
    public void advance (float amount, List<InputEventAPI> events) {
        //Basic sanity and sanitation checks
        if (Global.getCombatEngine() == null || Global.getCombatEngine().isPaused()) {
            return;
        }
        CombatEngineAPI engine = Global.getCombatEngine();

        //Iterates through all our current nanobot swarms
        List<NanobotData> toRemoveBots = new ArrayList<>();
        for (NanobotData bot : nanobotList) {
            //Run normal tick function
            bot.tick(engine, amount);

            //Spawns trails for each bot
            spawnTrailPiece(bot);

            //Remove bots that should disappear
            if (bot.shouldDisappear) {
                toRemoveBots.add(bot);
            }
        }

        //Cleans up variables that are unused
        for (NanobotData bot : toRemoveBots) {
            nanobotList.remove(bot);
            nanobotTrailMap.remove(bot);
        }
    }

    //Static function for adding a new nanobot to the plugin
    public static void SpawnNanobotSwarm (WeaponAPI associatedWeapon, float carriedMass, float angle, float turnRate, float speed, Vector2f position) {
        //Gets our plugin
        SRD_NanobotsPlugin plugin = (SRD_NanobotsPlugin) Global.getCombatEngine().getCustomData().get("SRD_NanobotsPlugin");
        if (plugin == null) {return;}

        //Adds a new nanobot swarm, with all the data that entails
        NanobotData bot = new NanobotData(associatedWeapon, carriedMass, speed, angle, turnRate, position);
        plugin.nanobotList.add(bot);
        plugin.nanobotTrailMap.put(bot, MagicTrailPlugin.getUniqueID());
    }


    /*--- Class for storing nanobot data ---*/
    public static class NanobotData {
        WeaponAPI associatedWeapon;
        float carriedMass;
        float speed;
        float currentAngle;
        float maxTurnRate;
        Vector2f position;

        private boolean shouldDisappear = false;

        //Instantiation function
        NanobotData (WeaponAPI associatedWeapon, float carriedMass, float speed, float startAngle, float maxTurnRate, Vector2f position) {
            this.associatedWeapon = associatedWeapon;
            this.carriedMass = carriedMass;
            this.speed = speed;
            this.currentAngle = startAngle;
            this.maxTurnRate = maxTurnRate;
            this.position = position;
        }

        //Main apply function, run once per frame (but not when paused)
        private void tick (CombatEngineAPI engine, float amount) {
            //Don't do anything if we're about to disappear
            if (shouldDisappear) {return;}

            //If our main ship has disappeared, we should just disappear too
            if (associatedWeapon.getShip() == null || associatedWeapon.getShip().isHulk()) {
                shouldDisappear = true;
                return;
            }
            ShipAPI ship = associatedWeapon.getShip();

            //Turn towards our weapon
            float targetAngle = VectorUtils.getAngle(position, associatedWeapon.getLocation());

            //If we're really close to the weapon, increase our turn rate to more easily hit it
            float distanceToGun = MathUtils.getDistance(position, associatedWeapon.getLocation());
            float extraTurnMult = 1f;
            if (distanceToGun < 300f) {
                extraTurnMult = 4f - 3f*((distanceToGun)/300f);
            }

            //Gets the shortest rotation to our target, and clamps that rotation to our current turn rate
            float shortestRotation = MathUtils.getShortestRotation(currentAngle, targetAngle);
            shortestRotation = MathUtils.clamp(shortestRotation, -maxTurnRate*amount*extraTurnMult, maxTurnRate*amount*extraTurnMult);

            //Modify our current angle
            currentAngle += shortestRotation;

            //Can we return to our ship this frame? If so, we move to it instantly rather than move normally
            if (MathUtils.getDistance(position, associatedWeapon.getLocation()) < (speed * amount)) {
                position.x = associatedWeapon.getLocation().x;
                position.y = associatedWeapon.getLocation().y;
                returnToShip(engine);
            }

            //If we couldn't return, just move straight ahead
            else {
                position.x += FastTrig.cos(Math.toRadians(currentAngle)) * speed * amount;
                position.y += FastTrig.sin(Math.toRadians(currentAngle)) * speed * amount;
            }
        }

        //Function for returning to the mothership: increase the metal of our mothership by the metal we carry
        private void returnToShip (CombatEngineAPI engine) {
            //Set ourselves to disappear
            shouldDisappear = true;

            //And increase the metal of the mothership, if it exists (if not, we wouldn't be left anyhow)
            ShipAPI ship = associatedWeapon.getShip();
            if (ship != null) {
                if (engine.getCustomData().get(ship.getId()+EFFECT_METAL_ID) instanceof Float) {
                    engine.getCustomData().put(ship.getId()+EFFECT_METAL_ID, carriedMass+(float)engine.getCustomData().get(ship.getId()+EFFECT_METAL_ID));
                } else {
                    engine.getCustomData().put(ship.getId()+EFFECT_METAL_ID, carriedMass);
                }

                //Also, spawn a little sound effect
                Global.getSoundPlayer().playSound(ARRIVAL_SOUND, MathUtils.clamp(1.3f-(carriedMass/10f),0.5f, 1.3f), SOUND_VOLUME_SCALE, position, ship.getVelocity());
            }
        }
    }
}