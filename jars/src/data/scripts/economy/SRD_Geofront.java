package data.scripts.economy;

import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin;
import com.fs.starfarer.api.impl.campaign.econ.ConditionData;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;

public class SRD_Geofront extends BaseMarketConditionPlugin {
    public static final float STABILITY_INCREASE = 2f;

    //How much better is this place than a normal autofactory?
    public static final float EXTRA_PRODUCTION_MULT = 1.15f;

    @Override
    public void apply(String id) {
        //Gets a base multiplier from our market size
        float mult = getBaseSizeMult();

        //Increased stability
        market.getStability().modifyFlat(id, STABILITY_INCREASE, "Geofront");

        //Needs a steady supply of refined metals, organics and heavy machinery. Also supports ore intake, to compensate partially for its own metal usage
        market.getDemand(Commodities.ORGANICS).getDemand().modifyFlat(id, mult * ConditionData.AUTOFAC_HEAVY_ORGANICS);
        market.getDemand(Commodities.METALS).getDemand().modifyFlat(id, mult * ConditionData.AUTOFAC_HEAVY_METALS);
        market.getDemand(Commodities.RARE_METALS).getDemand().modifyFlat(id, mult * ConditionData.AUTOFAC_HEAVY_RARE_METALS);
        market.getDemand(Commodities.HEAVY_MACHINERY).getDemand().modifyFlat(id, mult * ConditionData.AUTOFAC_HEAVY_MACHINERY_DEMAND);
        market.getDemand(Commodities.ORE).getDemand().modifyFlat(id, mult * ConditionData.AUTOFAC_HEAVY_METALS * 0.6f);
        market.getDemand(Commodities.RARE_ORE).getDemand().modifyFlat(id, mult * ConditionData.AUTOFAC_HEAVY_RARE_METALS * 0.8f);

        //Gets a production multiplier for our production, depending on satisfied demand (note that the ore refining runs seperately)
        float productionMult = getProductionMult(market, Commodities.HEAVY_MACHINERY, Commodities.ORGANICS, Commodities.VOLATILES, Commodities.METALS, Commodities.RARE_METALS) * EXTRA_PRODUCTION_MULT;
        float metalProductionMult = getProductionMult(market, Commodities.ORE, Commodities.RARE_ORE, Commodities.HEAVY_MACHINERY) * EXTRA_PRODUCTION_MULT;

        //Produces a whole bunch of supplies, heavy machinery, handguns and even refined metal to some extent (though not nearly as much as it consumes)
        market.getCommodityData(Commodities.HEAVY_MACHINERY).getSupply().modifyFlat(id, mult * ConditionData.AUTOFAC_HEAVY_MACHINERY * productionMult);
        market.getCommodityData(Commodities.SUPPLIES).getSupply().modifyFlat(id, mult * ConditionData.AUTOFAC_HEAVY_SUPPLIES * productionMult);
        market.getCommodityData(Commodities.HAND_WEAPONS).getSupply().modifyFlat(id, mult * ConditionData.AUTOFAC_HEAVY_HAND_WEAPONS * productionMult);
        market.getCommodityData(Commodities.METALS).getSupply().modifyFlat(id, mult * ConditionData.AUTOFAC_HEAVY_METALS * 0.3f * metalProductionMult);
        market.getCommodityData(Commodities.RARE_METALS).getSupply().modifyFlat(id, mult * ConditionData.AUTOFAC_HEAVY_RARE_METALS * 0.4f * metalProductionMult);
    }

    @Override
    public void unapply(String id) {
        market.getStability().unmodify(id);

        market.getDemand(Commodities.ORGANICS).getDemand().unmodify(id);
        market.getDemand(Commodities.METALS).getDemand().unmodify(id);
        market.getDemand(Commodities.RARE_METALS).getDemand().unmodify(id);
        market.getDemand(Commodities.HEAVY_MACHINERY).getDemand().unmodify(id);
        market.getDemand(Commodities.ORE).getDemand().unmodify(id);
        market.getDemand(Commodities.RARE_ORE).getDemand().unmodify(id);

        market.getCommodityData(Commodities.HEAVY_MACHINERY).getSupply().unmodify(id);
        market.getCommodityData(Commodities.SUPPLIES).getSupply().unmodify(id);
        market.getCommodityData(Commodities.HAND_WEAPONS).getSupply().unmodify(id);
        market.getCommodityData(Commodities.METALS).getSupply().unmodify(id);
        market.getCommodityData(Commodities.RARE_METALS).getSupply().unmodify(id);
    }
}
