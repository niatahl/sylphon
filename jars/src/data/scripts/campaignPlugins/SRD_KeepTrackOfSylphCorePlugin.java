//By Nicke535, used to track Sylph Core upgrades
//DEPRECATED: don't use this anymore. Only left for compatibility reasons
package data.scripts.campaignPlugins;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

import java.util.HashMap;
import java.util.Map;

public class SRD_KeepTrackOfSylphCorePlugin implements EveryFrameScript {
    private FleetMemberAPI member;

    public SRD_KeepTrackOfSylphCorePlugin (FleetMemberAPI member) {
        this.member = member;
    }

    @Override
    public void advance( float amount ) {
        SectorAPI sector = Global.getSector();
        if (sector == null) {
            return;
        }

        if (member.getVariant() != null) {
            if (!member.getVariant().getHullMods().contains("SRD_sylph_core")) {
                member.getVariant().addPermaMod("SRD_sylph_core");
            }
        }


    }

    @Override
    public boolean isDone() {
        return !member.isMothballed() && member.getFleetData() == null;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }
}
