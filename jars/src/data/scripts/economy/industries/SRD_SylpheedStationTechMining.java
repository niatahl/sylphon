package data.scripts.economy.industries;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketImmigrationModifier;
import com.fs.starfarer.api.campaign.impl.items.BlueprintProviderItem;
import com.fs.starfarer.api.campaign.impl.items.ModSpecItemPlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.population.PopulationComposition;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec.DropData;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageEntity;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import data.scripts.plugins.MagicTrailPlugin;


public class SRD_SylpheedStationTechMining extends BaseIndustry implements MarketImmigrationModifier {

	//Only show on Sylpheed Station
	@Override
	public boolean isHidden() {
		return !market.getName().contains("Sylpheed Station");
	}
	public boolean showWhenUnavailable() {
		return false;
	}

	public static final String TECH_MINING_MULT = "$core_techMiningMult";
	
	public void apply() {
		super.apply(false);

		//We don't check the conditions on *this* planet, we check on Preatorium.
		MemoryAPI mem = market.getMemoryWithoutUpdate();
		MarketAPI targetMarket = market;
		if (mem.get("$SRD_SylpheedTechMiningPlanet") instanceof MarketAPI) {
			targetMarket = (MarketAPI) mem.get("$SRD_SylpheedTechMiningPlanet");
		}
		
		int size = market.getSize();

		int max = 0;

		//No resource mining if Preatorium is owned by another faction. Otherwise, depends on ruin size of Preatorium
		if (!targetMarket.isPlanetConditionMarketOnly() && market.getFaction() != market.getFaction()) {
			max = 0;
		} else if (targetMarket.hasCondition(Conditions.RUINS_VAST)) {
			max = 4;
		} else if (targetMarket.hasCondition(Conditions.RUINS_EXTENSIVE)) {
			max = 3;
		} else if (targetMarket.hasCondition(Conditions.RUINS_WIDESPREAD)) {
			max = 2;
		} else if (targetMarket.hasCondition(Conditions.RUINS_SCATTERED)) {
			max = 1;
		}
		
		size = Math.min(size, max);
		
		applyIncomeAndUpkeep(size);
		
		supply(Commodities.METALS, size + 2);
		supply(Commodities.HEAVY_MACHINERY, size);
		supply(Commodities.FUEL, size);
		supply(Commodities.SUPPLIES, size);
//		supply(Commodities.HAND_WEAPONS, size);
//		supply(Commodities.RARE_METALS, size - 2);
//		supply(Commodities.VOLATILES, size - 2);

		if (!isFunctional()) {
			supply.clear();
		}
		
		market.addTransientImmigrationModifier(this);
	}

	
	@Override
	public void unapply() {
		market.removeTransientImmigrationModifier(this);
	}


	@Override
	public boolean isAvailableToBuild() {
		if (!super.isAvailableToBuild()) return false;
		/* Should be replaced so to only be available for Sylpheed Station
		if (market.hasCondition(Conditions.RUINS_VAST) ||
				market.hasCondition(Conditions.RUINS_EXTENSIVE) ||
				market.hasCondition(Conditions.RUINS_WIDESPREAD) ||
				market.hasCondition(Conditions.RUINS_SCATTERED)) {
			return true;
		}
		*/
		if (market.getName().contains("Sylpheed Station")) { return true;}
		return true;
	}

	@Override
	public String getUnavailableReason() {
		if (!super.isAvailableToBuild()) return super.getUnavailableReason();
		return "Can only be built on Sylpheed Station";
	}

	//Changed; the TRI-TACHYON don't mind *quite* as much as usual
	public void modifyIncoming(MarketAPI market, PopulationComposition incoming) {
		incoming.add(Factions.TRITACHYON, 5f);
	}

	//Changed: the pathers don't like us
	public float getPatherInterest() {
		float base = 2f;
		return base + super.getPatherInterest();
	}
	
	
	protected boolean hasPostDemandSection(boolean hasDemand, IndustryTooltipMode mode) {
		return true;
	}
	
	@Override
	protected void addPostDemandSection(TooltipMakerAPI tooltip, boolean hasDemand, IndustryTooltipMode mode) {
		float opad = 10f;
		tooltip.addPara("Tapping into an old AI network hidden within the ruins of Praetorium, specialized Sylph Cores " +
				"are tirelessly reverse engineering old domain code. This is also supplemented by normal tech-mining on the " +
				"planet's surface.", opad);
		MemoryAPI mem = market.getMemoryWithoutUpdate();
		MarketAPI targetMarket = market;
		if (mem.get("$SRD_SylpheedTechMiningPlanet") instanceof MarketAPI) {
			targetMarket = (MarketAPI) mem.get("$SRD_SylpheedTechMiningPlanet");
		}
		
		if (targetMarket.hasCondition(Conditions.RUINS_VAST)) {
			tooltip.addPara("The vast ruins on Preatorium offer incredible potential for valuable finds.", opad);
		} else if (targetMarket.hasCondition(Conditions.RUINS_EXTENSIVE)) {
			tooltip.addPara("The extensive ruins on Preatorium offer the opportunity for valuable finds.", opad);
		} else if (targetMarket.hasCondition(Conditions.RUINS_WIDESPREAD)) {
			tooltip.addPara("The widespread ruins on Preatorium offer a solid chance of finding something valuable, given time.", opad);
		} else if (targetMarket.hasCondition(Conditions.RUINS_SCATTERED)) {
			tooltip.addPara("The scattered ruins on Preatorium offer only a slight chance of finding something valuable, though one never knows what might be located given enough time.", opad);
		}
		
		float mult = getTechMiningMult();
		if (mult >= .9f) {
			tooltip.addPara("The ruins are largely untapped.", opad);
		} else if (mult >= .5f) {
			tooltip.addPara("The ruins have been stripped of easy pickings, but the more difficult areas remain, filled with promise.", opad);
		} else if (mult >= 0.25f) {
			tooltip.addPara("The ruins have been combed through, though the chance for a few new finds still remains.", opad);
		} else {
			tooltip.addPara("The ruins have been comprehensively combed over multiple times, and there is little chance of a new valuable find.", opad);
		}
		// add something re: size of ruins and chance to find stuff
		
	}

	//Changed: now looks at Preatorium for tech mining
	public float getTechMiningMult() {
		MemoryAPI mem = market.getMemoryWithoutUpdate();
		MarketAPI targetMarket = market;
		if (mem.get("$SRD_SylpheedTechMiningPlanet") instanceof MarketAPI) {
			targetMarket = (MarketAPI) mem.get("$SRD_SylpheedTechMiningPlanet");
		}
		if (targetMarket.getMemoryWithoutUpdate().contains(TECH_MINING_MULT)) {
			return targetMarket.getMemoryWithoutUpdate().getFloat(TECH_MINING_MULT);
		}
		targetMarket.getMemoryWithoutUpdate().set(TECH_MINING_MULT, 1f);
		return 1f;
	}

	//Changed: now looks at Preatorium for tech mining
	public void setTechMiningMult(float value) {
		MemoryAPI mem = market.getMemoryWithoutUpdate();
		MarketAPI targetMarket = market;
		if (mem.get("$SRD_SylpheedTechMiningPlanet") instanceof MarketAPI) {
			targetMarket = (MarketAPI) mem.get("$SRD_SylpheedTechMiningPlanet");
		}
		targetMarket.getMemoryWithoutUpdate().set(TECH_MINING_MULT, value);
	}
	
	public float getTechMiningRuinSizeModifier() {
		return getTechMiningRuinSizeModifier(market);
	}

	//Changed; now looks at Preatorium for tech mining
	public static float getTechMiningRuinSizeModifier(MarketAPI market) {
		MemoryAPI mem = market.getMemoryWithoutUpdate();
		MarketAPI targetMarket = market;
		if (mem.get("$SRD_SylpheedTechMiningPlanet") instanceof MarketAPI) {
			targetMarket = (MarketAPI) mem.get("$SRD_SylpheedTechMiningPlanet");
		}

		float mod = 0f;
		if (targetMarket.hasCondition(Conditions.RUINS_VAST)) {
			mod = 1;
		} else if (targetMarket.hasCondition(Conditions.RUINS_EXTENSIVE)) {
			mod = 0.5f;
		} else if (targetMarket.hasCondition(Conditions.RUINS_WIDESPREAD)) {
			mod = 0.25f;
		} else if (targetMarket.hasCondition(Conditions.RUINS_SCATTERED)) {
			mod = 0.125f;
		}

		//Addition: if someone else owns Preatorium, we find *much* less stuff
		if (!targetMarket.isPlanetConditionMarketOnly() && targetMarket.getFaction() != market.getFaction()) {
			mod *= 0.01f;
		}

		return mod;
	}
	
	public CargoAPI generateCargoForGatheringPoint(Random random) {
		
		float mult = getTechMiningMult();
		float decay = Global.getSettings().getFloat("techMiningDecay");
		float base = getTechMiningRuinSizeModifier();

		//If Preatorium is not under our control, we decay the place much slower
		MemoryAPI mem = market.getMemoryWithoutUpdate();
		MarketAPI targetMarket = market;
		if (mem.get("$SRD_SylpheedTechMiningPlanet") instanceof MarketAPI) {
			targetMarket = (MarketAPI) mem.get("$SRD_SylpheedTechMiningPlanet");
		}
		if (!targetMarket.isPlanetConditionMarketOnly() && market.getFaction() != targetMarket.getFaction()) {
			setTechMiningMult(mult * (0.99f + (0.01f*(float)Math.sqrt(decay)))); //Changed to square root, because Sylpheed Station doesn't work through Preatorium's ruins all that fast; they are meticulous to a fault.
		} else {
			setTechMiningMult(mult * (float)Math.sqrt(decay)); //Changed to square root, because Sylpheed Station doesn't work through Preatorium's ruins all that fast; they are meticulous to a fault.
		}
		
		
		List<DropData> dropRandom = new ArrayList<DropData>();
		List<DropData> dropValue = new ArrayList<DropData>();

		//Blueprints are more common, due to downloading from AI networks
		DropData d = new DropData();
		d.chances = 2;
		d.group = "blueprints_low";
		//d.addCustom("item_:{tags:[single_bp], p:{tags:[rare_bp]}}", 1f);
		dropRandom.add(d);

		d = new DropData();
		d.chances = 1;
		d.group = "rare_tech_low";
		d.valueMult = 0.1f;
		dropRandom.add(d);

		//They're also extra good at finding the locations of AI cores, since they have their own
		d = new DropData();
		d.chances = 2;
		d.group = "ai_cores3";
		//d.valueMult = 0.1f; // already a high chance to get nothing due to group setup, so don't reduce further
		dropRandom.add(d);

		d = new DropData();
		d.chances = 2;
		d.group = "any_hullmod_low";
		dropRandom.add(d);
		
		d = new DropData();
		d.chances = 5;
		d.group = "weapons2";
		dropRandom.add(d);
		
		d = new DropData();
		//d.chances = 100;
		d.group = "basic";
		d.value = 10000;
		dropValue.add(d);
		
		if (mult >= 1) {
			float num = base * (5f + random.nextFloat() * 2f);
			if (base < 1) base = 1;
			
			d = new DropData();
			d.chances = (int) Math.round(num);
			d.group = "techmining_first_find";
			dropRandom.add(d);
		}
		
		CargoAPI result = SalvageEntity.generateSalvage(random, 1f, 1f, base * mult, 1f, dropValue, dropRandom);
		
		FactionAPI pf = Global.getSector().getPlayerFaction();
		OUTER: for (CargoStackAPI stack : result.getStacksCopy()) {
			if (stack.getPlugin() instanceof BlueprintProviderItem) {
				BlueprintProviderItem bp = (BlueprintProviderItem) stack.getPlugin();
				List<String> list = bp.getProvidedShips();
				if (list != null) {
					for (String id : list) {
						if (!pf.knowsShip(id)) continue OUTER;
					}
				}
				
				list = bp.getProvidedWeapons();
				if (list != null) {
					for (String id : list) {
						if (!pf.knowsWeapon(id)) continue OUTER;
					}
				}
				
				list = bp.getProvidedFighters();
				if (list != null) {
					for (String id : list) {
						if (!pf.knowsFighter(id)) continue OUTER;
					}
				}
				
				list = bp.getProvidedIndustries();
				if (list != null) {
					for (String id : list) {
						if (!pf.knowsIndustry(id)) continue OUTER;
					}
				}
				result.removeStack(stack);
			} else if (stack.getPlugin() instanceof ModSpecItemPlugin) {
				ModSpecItemPlugin mod = (ModSpecItemPlugin) stack.getPlugin();
				if (!pf.knowsHullMod(mod.getModId())) continue OUTER;
				result.removeStack(stack);
			}
		}
		
		return result;
	}
	
}







