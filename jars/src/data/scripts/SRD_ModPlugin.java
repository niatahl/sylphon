package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaignPlugins.SRD_AIConversionNexerelinAdderPlugin;
import data.scripts.campaignPlugins.SRD_KeepTrackOfSylphCorePlugin;
import data.scripts.world.SRD_FactionRelationPlugin;
import data.scripts.world.SRD_Nym;
import data.scripts.world.SRD_Rofocale;

import java.util.ArrayList;
import java.util.List;

public class SRD_ModPlugin extends BaseModPlugin {

    public static List<String> SHIELD_HULLMODS = new ArrayList<String>();
    public static List<String> NULLSPACE_CONDUIT_HULLMODS = new ArrayList<String>();

    @Override
    public void onApplicationLoad() {
        boolean hasLazyLib = Global.getSettings().getModManager().isModEnabled("lw_lazylib");
        if (!hasLazyLib) {
            throw new RuntimeException("Sylphon RnD requires LazyLib by LazyWizard");
        }
        boolean hasSSFX = Global.getSettings().getModManager().isModEnabled("xxx_ss_FX_mod");
        if (hasSSFX) {
            throw new RuntimeException("Sylphon RnD is not compatible with Starsector FX");
        }


        for (HullModSpecAPI hullModSpecAPI : Global.getSettings().getAllHullModSpecs()) {
            if (hullModSpecAPI.hasTag("shields") && !SHIELD_HULLMODS.contains(hullModSpecAPI.getId())) {
                SHIELD_HULLMODS.add(hullModSpecAPI.getId());
            } else if (hullModSpecAPI.getId().contains("shieldbypass") && !SHIELD_HULLMODS.contains(hullModSpecAPI.getId())) {
                SHIELD_HULLMODS.add("shieldbypass"); //Dirty fix for Shield Bypass, since that one is actually not tagged as a Shield mod, apparently
            }
        }

        NULLSPACE_CONDUIT_HULLMODS.add("SRD_nullspace_conduits");
        NULLSPACE_CONDUIT_HULLMODS.add("SRD_modular_nullspace_conduits");
        NULLSPACE_CONDUIT_HULLMODS.add("SRD_nullspace_stabilizer");
        NULLSPACE_CONDUIT_HULLMODS.add("SRD_outcast_engineering");
    }


    @Override
    public void onNewGame() {
        SectorAPI sector = Global.getSector();

        new SRD_Nym().generate(sector);
        new SRD_Rofocale().generate(sector);
        SRD_FactionRelationPlugin.initFactionRelationships(sector);
    }
}
