//UNUSED FOR NOW
package data.scripts.economy;

import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin;
import com.fs.starfarer.api.impl.campaign.econ.ConditionData;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;

public class SRD_TechnocraticPopulation extends BaseMarketConditionPlugin {
    //Drugs and organs are not sought after as much; entertainment takes the form of games and simulations, while wide-spread cybernetics has lowered the need for biological organ import
    public static final float DRUG_DEMAND_MULT = 0.75f;
    public static final float ORGAN_DEMAND_MULT = 0.6f;

    //Luxuries are in high demand; they are capitalists through-and-through, after all. Also, these sorta represent game systems and the likes best
    public static final float LUXURY_DEMAND_MULT = 1.1f;

    //Slightly less people are willing to enlist or take up jobs under poor conditions; generally, people are less willing to do what most would consider "work", and generally try
    //to do more recreational jobs when they can (which don't really affect the market at large, thus counting as nothing in the economy calculations)
    public static final float CREW_SUPPLY_MULT = 0.9f;

    @Override
    public void apply(String id) {
        market.getDemand(Commodities.DRUGS).getDemand().modifyMult(id, DRUG_DEMAND_MULT);
        market.getDemand(Commodities.ORGANS).getDemand().modifyMult(id, ORGAN_DEMAND_MULT);
        market.getDemand(Commodities.LUXURY_GOODS).getDemand().modifyMult(id, LUXURY_DEMAND_MULT);

        market.getCommodityData(Commodities.TAG_CREW).getSupply().modifyMult(id, CREW_SUPPLY_MULT);
    }

    @Override
    public void unapply(String id) {
        market.getDemand(Commodities.DRUGS).getDemand().unmodify(id);
        market.getDemand(Commodities.ORGANS).getDemand().unmodify(id);
        market.getDemand(Commodities.LUXURY_GOODS).getDemand().unmodify(id);

        market.getCommodityData(Commodities.TAG_CREW).getSupply().unmodify(id);
    }
}
