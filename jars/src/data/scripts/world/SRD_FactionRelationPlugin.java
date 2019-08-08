package data.scripts.world;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorGeneratorPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import data.scripts.campaignPlugins.SRD_CampaignRelationsReadjustmentPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SRD_FactionRelationPlugin implements SectorGeneratorPlugin {
    //Map for faction relations: the player is handled seperately
    private static Map<String, Float> factionRelations = new HashMap<String, Float>();
    static {
        factionRelations.put(Factions.HEGEMONY, -0.20f);
        factionRelations.put(Factions.TRITACHYON, 0.60f);
        factionRelations.put(Factions.PIRATES, -0.70f);
        factionRelations.put(Factions.INDEPENDENT, 0.20f);
        factionRelations.put(Factions.LUDDIC_CHURCH, -0.60f);
        factionRelations.put(Factions.LUDDIC_PATH, -1.00f);
        factionRelations.put(Factions.DIKTAT, -0.10f);
        factionRelations.put(Factions.REMNANTS, 0.00f);
        factionRelations.put("new_galactic_order", -1.00f);
        factionRelations.put("interstellarimperium", -0.60f);
        factionRelations.put("SCY", 0.00f);
        factionRelations.put("shadow_industry", 0.25f);
        factionRelations.put("tiandong", 0.00f);
        factionRelations.put("exigency", -0.90f);
        factionRelations.put("ORA", 0.40f);
        factionRelations.put("Coalition", -0.60f);
        factionRelations.put("nomads", -0.30f);
        factionRelations.put("fringe_defence_syndicate", 0.00f);
        factionRelations.put("fob", 0.40f);
        factionRelations.put("diableavionics", 0.00f);
        factionRelations.put("templars", -0.80f);
        factionRelations.put("junk_pirates", -0.70f);
        factionRelations.put("syndicate_asp", -0.70f);
        factionRelations.put("sad", -0.70f);
        factionRelations.put("dassault_mikoyan", 0.50f);
        factionRelations.put("blade_breakers", -0.70f);
        factionRelations.put("blackrock_driveyards", 0.20f);
        factionRelations.put("crystanite", 0.20f);
        factionRelations.put("vass_shipyards", 0.40f);
        factionRelations.put("exlane", -0.50f);
    }


    //Just call initFactionRelationships: this is only intended as a means to set faction relations at start
    @Override
    public void generate(SectorAPI sector) {
        initFactionRelationships(sector);
    }

    public static void initFactionRelationships(SectorAPI sector) {
        FactionAPI sylphon = sector.getFaction("sylphon");
        FactionAPI player = sector.getFaction(Factions.PLAYER);

        //Sets player relations
        player.setRelationship(sylphon.getId(), RepLevel.SUSPICIOUS);

        //Handles all other relations by iterating through our list
        for (String s : factionRelations.keySet()) {
            FactionAPI faction = sector.getFaction(s);
            if (faction != null) {
                sylphon.setRelationship(faction.getId(), factionRelations.get(s));
            }
        }

        //Adds automatic adjustment of relations, so that Sylphon relations can never pass a certain point with some factions
        sector.addScript(new SRD_CampaignRelationsReadjustmentPlugin());
    }
}
