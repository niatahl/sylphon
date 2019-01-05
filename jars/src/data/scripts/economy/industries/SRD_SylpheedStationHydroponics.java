package data.scripts.economy.industries;

import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Pair;


public class SRD_SylpheedStationHydroponics extends BaseIndustry {

	public void apply() {
		super.apply(true);

		int size = market.getSize();

		//Produces enough food to feed the population that works it, but has a bottom-line minimum machinery upkeep
		demand(Commodities.HEAVY_MACHINERY, Math.max(4, size - 1));

		supply(Commodities.FOOD, size-1);

		Pair<String, Integer> deficit = getMaxDeficit(Commodities.HEAVY_MACHINERY);
		applyDeficitToProduction(1, deficit, Commodities.FOOD);

		if (!isFunctional()) {
			supply.clear();
		}
	}

	@Override
	public void unapply() {
		super.unapply();
	}

	//Only show on Sylpheed Station
	@Override
	public boolean showWhenUnavailable() {
		return false;
	}

	//Can only be built on Sylpheed Station
	@Override
	public boolean isAvailableToBuild() {
		if (!super.isAvailableToBuild()) return false;
		if (market.getPrimaryEntity().getCustomEntitySpec() != null && market.getPrimaryEntity().getCustomEntitySpec().getId().contains("SRD_sylpheed_station")) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public String getUnavailableReason() {
		if (!super.isAvailableToBuild()) return super.getUnavailableReason();
		return "Can only be built on Sylpheed Station";
	}


	@Override
	public void createTooltip(IndustryTooltipMode mode, TooltipMakerAPI tooltip, boolean expanded) {
		super.createTooltip(mode, tooltip, expanded);
	}
}







