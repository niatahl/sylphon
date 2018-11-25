package data.scripts.utils;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin.DerelictShipData;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.procgen.DefenderDataOverride;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec.DropData;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial.ShipCondition;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial.PerShipData;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SRD_CampaignUtils {

    //Originally by Vayra, modified by Nicke535
    //Spawns a derelict ship and assigns some defenders to it (if enabled)
    public static SectorEntityToken addDerelictShip(StarSystemAPI system, SectorEntityToken focus, String variantId,
                                                ShipCondition condition, float orbitRadius, float daysToOrbit,
                                                float startOrbitAngle, boolean recoverable, boolean hasDefenders,
                                                DefenderDataOverride defenders) {

        DerelictShipData params = new DerelictShipData(new PerShipData(variantId, condition), false);
        SectorEntityToken ship = BaseThemeGenerator.addSalvageEntity(system, Entities.WRECK, Factions.NEUTRAL, params);
        ship.setDiscoverable(true);

        ship.setCircularOrbit(focus, startOrbitAngle, orbitRadius, daysToOrbit);

        if (recoverable) {
            SalvageSpecialAssigner.ShipRecoverySpecialCreator creator = new SalvageSpecialAssigner.ShipRecoverySpecialCreator(null, 0, 0, false, null, null);
            Misc.setSalvageSpecial(ship, creator.createSpecial(ship, null));
        }
        if (hasDefenders) {
            Misc.setDefenderOverride(ship, defenders);
        }
        return ship;
    }
}
