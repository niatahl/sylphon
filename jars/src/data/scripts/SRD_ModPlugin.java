package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import data.scripts.world.SRD_FactionRelationPlugin;
import data.scripts.world.SRD_Nym;
import data.scripts.world.SRD_Rofocale;
import org.dark.shaders.light.LightData;
import org.dark.shaders.util.ShaderLib;
import org.dark.shaders.util.TextureData;

import java.util.ArrayList;
import java.util.List;

public class SRD_ModPlugin extends BaseModPlugin {

    //All hullmods related to shields, saved in a convenient list
    public static List<String> SHIELD_HULLMODS = new ArrayList<String>();

    //All hullmods that count as Nullspace Conduits, for use in other scripts
    public static List<String> NULLSPACE_CONDUIT_HULLMODS = new ArrayList<String>();

    //All hullmods that count as Sylph Cores (so, the eccentrics and the Sylph Core), for use in other scripts
    public static List<String> SYLPH_CORE_HULLMODS = new ArrayList<String>();

    @Override
    public void onApplicationLoad() {
        boolean hasLazyLib = Global.getSettings().getModManager().isModEnabled("lw_lazylib");
        if (!hasLazyLib) {
            throw new RuntimeException("Sylphon RnD requires LazyLib by LazyWizard");
        }
        boolean hasMagicLib = Global.getSettings().getModManager().isModEnabled("lw_lazylib");
        if (!hasMagicLib) {
            throw new RuntimeException("Sylphon RnD requires MagicLib!");
        }
        boolean hasSSFX = Global.getSettings().getModManager().isModEnabled("xxx_ss_FX_mod");
        if (hasSSFX) {
            throw new RuntimeException("Sylphon RnD is not compatible with Starsector FX");
        }

        boolean hasGraphicsLib = Global.getSettings().getModManager().isModEnabled("shaderLib");
        if (hasGraphicsLib) {
            ShaderLib.init();
            LightData.readLightDataCSV("data/lights/SRD_lights.csv");
            TextureData.readTextureDataCSV("data/lights/SRD_texture.csv");
        }



        //Adds shield hullmods
        for (HullModSpecAPI hullModSpecAPI : Global.getSettings().getAllHullModSpecs()) {
            if (hullModSpecAPI.hasTag("shields") && !SHIELD_HULLMODS.contains(hullModSpecAPI.getId())) {
                SHIELD_HULLMODS.add(hullModSpecAPI.getId());
            } else if (hullModSpecAPI.getId().contains("shieldbypass") && !SHIELD_HULLMODS.contains(hullModSpecAPI.getId())) {
                SHIELD_HULLMODS.add("shieldbypass"); //Dirty fix for Shield Bypass, since that one is actually not tagged as a Shield mod, apparently
            }
        }

        //Adds nullspace conduit hullmods
        NULLSPACE_CONDUIT_HULLMODS.add("SRD_nullspace_conduits");
        NULLSPACE_CONDUIT_HULLMODS.add("SRD_modular_nullspace_conduits");
        NULLSPACE_CONDUIT_HULLMODS.add("SRD_nullspace_stabilizer");
        NULLSPACE_CONDUIT_HULLMODS.add("SRD_outcast_engineering");

        //Adds sylph core hullmods
        SYLPH_CORE_HULLMODS.add("SRD_sylph_core");
        SYLPH_CORE_HULLMODS.add("SRD_eccentric_core_cieve");
    }


    @Override
    public void onNewGame() {
        SectorAPI sector = Global.getSector();

        new SRD_Nym().generate(sector);
        new SRD_Rofocale().generate(sector);
        SRD_FactionRelationPlugin.initFactionRelationships(sector);
    }

    /*-----------------------------------Convenience functions down here!---------------------------------------------*/
    //Checks a list of hullmods and returns true if any of them is a hullmod classed as a Sylph Core
    public static boolean hasSylphCoreInstalled(List<String> hullmods) {
        for (String s : hullmods) {
            if (SYLPH_CORE_HULLMODS.contains(s)) {
                return true;
            }
        }
        return false;
    }

    //Checks a list of hullmods and returns true if any of them is a hullmod classed as a Nullspace Conduit
    public static boolean hasNullspaceConduits(List<String> hullmods) {
        for (String s : hullmods) {
            if (NULLSPACE_CONDUIT_HULLMODS.contains(s)) {
                return true;
            }
        }
        return false;
    }

    //Checks the games loaded files and sees if a given hullspec ID exists
    public static boolean hullspecExists (String hullspecID) {
        for (ShipHullSpecAPI shipHullSpecAPI : Global.getSettings().getAllShipHullSpecs()) {
            if (shipHullSpecAPI.getHullId().equals(hullspecID)) {
                return true;
            }
        }
        return false;
    }
}
