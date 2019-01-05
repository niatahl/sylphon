//By Nicke535, automatically adds the AI Conversions submarket to the nexerelin Sylphon headquarters
package data.scripts.campaignPlugins;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.fleet.FleetAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.util.Misc;

import java.util.HashMap;
import java.util.Map;

public class SRD_AIConversionNexerelinAdderPlugin implements EveryFrameScript {
    //Have we successfully added the market?
    private boolean hasCreatedMarket = false;

    @Override
    public void advance( float amount ) {
        SectorAPI sector = Global.getSector();
        if (sector == null) {
            return;
        }

        //Search the entire sector until we find the Sylphon homeworld; do this only if the Sylphon exist
        if (sector.getAllFactions().contains(sector.getFaction("sylphon"))) {
            for (LocationAPI loc : sector.getAllLocations()) {
                //If we have found the market, stop searching
                if (hasCreatedMarket) {break;}

                for (SectorEntityToken token : loc.getAllEntities()) {
                    //If we have found the market, stop searching
                    if (hasCreatedMarket) {break;}

                    if (token.getMarket() != null && token.getMarket().getFactionId().equals("sylphon")) {
                        //We have found a Sylphon market; add the submarket to it and be on our jolly way
                        token.getMarket().addSubmarket("SRD_ai_conversion");
                        hasCreatedMarket = true;
                        //sector.addScript(new SRD_AIConversionFleetUpgraderPlugin(loc)); DEPRECATED; USE VARIANTS NOW
                    }
                }
            }
        }
    }

    @Override
    public boolean isDone() {
        return hasCreatedMarket;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }
}
