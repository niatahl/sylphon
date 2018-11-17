//By Nicke535, a hullmod which buffs the ship's autofire accuracy and turret turn rate depending on how many other allies have this hullmod, while also including the functionality of IPDAI
package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SRD_AdvisorSubroutines extends BaseHullMod {

    public static final Map<HullSize, Float> BONUS_PERCENTAGES = new HashMap<HullSize, Float>();
    static {
        BONUS_PERCENTAGES.put(HullSize.FIGHTER, 0.2f);
        BONUS_PERCENTAGES.put(HullSize.FRIGATE, 0.2f);
        BONUS_PERCENTAGES.put(HullSize.DESTROYER, 0.3f);
        BONUS_PERCENTAGES.put(HullSize.CRUISER, 0.55f);
        BONUS_PERCENTAGES.put(HullSize.CAPITAL_SHIP, 0.75f);
    }
    public static final String ID_FOR_BONUS = "SRD_AdvisorSubroutinesID";
    public static final String DATA_SAVE_KEY = "SRD_AdvisorSubroutinesDataID";

    //Handles our permanent, non-changing bonuses
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (weapon.getSize() == WeaponSize.SMALL && weapon.getType() != WeaponType.MISSILE) {
                weapon.setPD(true);
            }
        }
    }

    //Handles all in-combat effects
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        //Nothing should happen if we are paused
        if (Global.getCombatEngine().isPaused()) {
            return;
        }

        //If this ship is on the same side as the player ship (and the player ship exists), we store how much bonus we give to the player in the global data storage
        if (Global.getCombatEngine().getPlayerShip() == null) {
            Global.getCombatEngine().getCustomData().put(DATA_SAVE_KEY + ship.getId(), 0f);
            return;
        } else if (Global.getCombatEngine().getPlayerShip().getOwner() == ship.getOwner() && !ship.isAlly()) {
            Global.getCombatEngine().getCustomData().put(DATA_SAVE_KEY + ship.getId(), BONUS_PERCENTAGES.get(ship.getHullSize()));
        } else {
            Global.getCombatEngine().getCustomData().put(DATA_SAVE_KEY + ship.getId(), 0f);
        }

        //If this ship is the player ship, we apply the bonus command point regen, and also supply... uhm... "advise"... to the player
        if (ship == Global.getCombatEngine().getPlayerShip()) {
            //Gets our total bonus to command point regen
            float bonusRegen = 0f;
            for (String key : Global.getCombatEngine().getCustomData().keySet()) {
                if (key.contains(DATA_SAVE_KEY) && Global.getCombatEngine().getCustomData().get(key) instanceof Float) {
                    bonusRegen += (float)Global.getCombatEngine().getCustomData().get(key);
                }
            }
            ship.getMutableStats().getDynamic().getMod(Stats.COMMAND_POINT_RATE_FLAT).modifyFlat(ID_FOR_BONUS, bonusRegen);

            //Handles "tactical advice"
            handleTacticalAdvice(ship, Global.getCombatEngine(), Global.getCombatEngine().getElapsedInLastFrame());
        }
    }


    //Handles applicability
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        //If we don't have a Sylph Core, we can't equip the hullmod
        return ship.getVariant().getHullMods().contains("SRD_sylph_core");
    }

    public String getUnapplicableReason(ShipAPI ship) {
        if (!ship.getVariant().getHullMods().contains("SRD_sylph_core")) {
            return "Requires an installed Sylph Core";
        } else {
            return "BUG: report to mod author that the file 'SRD_AdvisorSubroutines' has encountered an error";
        }
    }

    //Adds the description strings
    @Override
    public String getDescriptionParam(int index, HullSize hullSize, ShipAPI ship) {
        if (index == 0) return "tactical advice";
        if (index == 1) return "" + (int)(BONUS_PERCENTAGES.get(HullSize.FRIGATE) * 100f) + "%";
        if (index == 2) return "" + (int)(BONUS_PERCENTAGES.get(HullSize.DESTROYER) * 100f) + "%";
        if (index == 3) return "" + (int)(BONUS_PERCENTAGES.get(HullSize.CRUISER) * 100f) + "%";
        if (index == 4) return "" + (int)(BONUS_PERCENTAGES.get(HullSize.CAPITAL_SHIP) * 100f) + "%";
        if (index == 5) return "flagship also has this mod installed";
        return null;
    }


    //--------------------------------------------TACTICAL ADVICE HANDLING----------------------------------------------
    //How many seconds was it since we last complained?
    private float timeSinceLastComplaint = 0f;

    //How many seconds must we *at least* wait between two complaints?
    private static final float MIN_COMPLAINT_DELAY = 10f;

    //How much hull did we have last frame?
    private float hullLevelLastFrame = 1f;

    //How much hull must we lose in one frame to trigger a "heavy blow" complaint?
    private static final float HULL_LOSS_COMPLAINT_LEVEL = 0.15f;

    //Function for providing "advice" to the player. Maybe this should be locked behind a config setting? Who knows.
    private void handleTacticalAdvice (ShipAPI ship, CombatEngineAPI engine, float amount) {
        //Tick our timer, and if it has ticked enough we check for complaint opportunities
        timeSinceLastComplaint += amount;
        if (timeSinceLastComplaint >= MIN_COMPLAINT_DELAY) {
            //Very simplistic at the moment: if we take heavy damage, complain
            if (hullLevelLastFrame - ship.getHullLevel() >= HULL_LOSS_COMPLAINT_LEVEL) {
                String textToPrint = DAMAGE_TAKEN_COMPLAINTS.get(MathUtils.getRandomNumberInRange(0, DAMAGE_TAKEN_COMPLAINTS.size()-1));
                Global.getCombatEngine().getCombatUI().addMessage(1, ship, Misc.getTextColor(), "" + textToPrint);
                hullLevelLastFrame = ship.getHullLevel();
                timeSinceLastComplaint = 0f;
                handleEndOfFrame(ship, engine, amount);
                return;
            }
        }

        //If no complaint was made, we handle end-of-frame stuff here instead of when complaining
        handleEndOfFrame(ship, engine, amount);
    }

    //Handles anything related to complaining that needs to be done at the end of a frame
    private void handleEndOfFrame (ShipAPI ship, CombatEngineAPI engine, float amount) {
        hullLevelLastFrame = ship.getHullLevel();
    }


    //-----------------------------------------------COMPLAINT TEXTS----------------------------------------------------
    //Complaint dialogue list: taking heavy damage
    private static final List<String> DAMAGE_TAKEN_COMPLAINTS = new ArrayList<>();
    static {
        DAMAGE_TAKEN_COMPLAINTS.add("May I recommend avoiding incoming ordinance?");
        DAMAGE_TAKEN_COMPLAINTS.add("Beautiful dodging, commander.");
    }
}