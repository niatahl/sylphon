//By Nicke535, automatically reduces relations with certain factions should they ever get above an allowed level
package data.scripts.campaignPlugins;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SRD_CampaignRelationsReadjustmentPlugin implements EveryFrameScript {
    //Map for faction relation limits
    private static Map<String, Float> factionRelationMaximums = new HashMap<String, Float>();
    static {
        factionRelationMaximums.put(Factions.HEGEMONY, 0.00f);
        factionRelationMaximums.put(Factions.TRITACHYON, 0.7f);
        factionRelationMaximums.put("new_galactic_order", -1.00f);
        factionRelationMaximums.put("interstellarimperium", 0.00f);
        factionRelationMaximums.put("exigency", -0.6f);
    }

    @Override
    public void advance( float amount ) {
        SectorAPI sector = Global.getSector();
        if (sector == null) {
            return;
        }

        FactionAPI sylphon = sector.getFaction("sylphon");

        for (String s : factionRelationMaximums.keySet()) {
            FactionAPI faction = sector.getFaction(s);
            if (faction == null) { continue; }

            //Adjusts relations downward for any faction that is too high
            if (sylphon.getRelationship(faction.getId()) > factionRelationMaximums.get(s)) {
                sylphon.setRelationship(faction.getId(), factionRelationMaximums.get(s));
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
}
