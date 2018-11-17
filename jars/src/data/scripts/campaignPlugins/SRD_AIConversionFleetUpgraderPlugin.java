//By Nicke535, upgrades the
package data.scripts.campaignPlugins;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;

import java.util.ArrayList;
import java.util.List;

public class SRD_AIConversionFleetUpgraderPlugin implements EveryFrameScript {
    //How large a portion of ships should be allowed to be automated, on average (this is then modified by stability and market sizes)
    //At 0.5, each ship has a 50% chance to be automated when stability and market size is at 10
    private static final float MAX_MODDING_PERCENTAGE = 0.3f;

    //Which location are we tracking?
    private LocationAPI location;

    //Don't run all the time, to save memory
    IntervalUtil tracker = new IntervalUtil(20f, 30f);

    //Only run once per fleet
    List<String> checkedFleets = new ArrayList<String>();

    @Override
    public void advance( float amount ) {
        SectorAPI sector = Global.getSector();
        if (sector == null) {
            return;
        }

        //Only run once

        //Equips some ships in fleets from AI-conversion markets with Sylph cores
        for (CampaignFleetAPI fleet : location.getFleets()) {
            //Only check a given fleet *once*, ever. They only get one chance to be improved!
            if (checkedFleets.contains(fleet.getId())) {
                continue;
            }
            checkedFleets.add(fleet.getId());

            //Check so the source market of the fleet is a Sylphon market with the AI conversion submarket
            if (Misc.getSourceMarket(fleet) != null && Misc.getSourceMarket(fleet).getFactionId().equals("sylphon") && Misc.getSourceMarket(fleet).hasSubmarket("SRD_ai_conversion")) {
                //Since our source market can upgrade us, upgrade a percentage of our ships depending on market size and stability
                MarketAPI market = Misc.getSourceMarket(fleet);
                float moddingPercentage = MAX_MODDING_PERCENTAGE * (market.getStability().getModifiedValue()/10f) * (float)Math.sqrt((float)market.getSize()/10f);

                //Have a chance to add the Sylph Core mod to each ship in the fleet
                for (FleetMemberAPI fleetMember : fleet.getMembersWithFightersCopy()) {
                    //Ignore fighters
                    if (fleetMember.isFighterWing()) {
                        continue;
                    }

                    //And any ships with a Sylph Core
                    if (fleetMember.getVariant().getHullMods().contains("SRD_sylph_core")) {
                        continue;
                    }

                    //Otherwise, we have a chance to add a hullmod to it
                    if (Math.random() < moddingPercentage) {
                        fleetMember.getVariant().addPermaMod("SRD_sylph_core");
                        Global.getSector().addScript(new SRD_KeepTrackOfSylphCorePlugin(fleetMember));
                    }
                }
            }
        }
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    public SRD_AIConversionFleetUpgraderPlugin(LocationAPI location) {
        this.location = location;
    }
}
