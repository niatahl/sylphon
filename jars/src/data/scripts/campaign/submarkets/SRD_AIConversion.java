//By Nicke535, reverse-engineered from the Interstellar Imperium submarket of similar function
//A submarket to which you can sell sylphon ships, and them back with an installed Sylph Core
package data.scripts.campaign.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.Highlights;
import com.fs.starfarer.api.util.Misc;
import data.scripts.SRD_ModPlugin;
import data.scripts.campaignPlugins.SRD_SylphCoreUpgradeTracker;

import java.util.*;

public class SRD_AIConversion extends BaseSubmarketPlugin {

    //Table for the different hull sizes available and which reputation you need to convert them. Note that fighters are not allowed to be sold to the market, but are there as a fail-safe anyway
    private static final Map<ShipAPI.HullSize, RepLevel> REPUTATION_TABLE = new HashMap<>(5);
    static {
        REPUTATION_TABLE.put(ShipAPI.HullSize.FIGHTER, RepLevel.FRIENDLY);
        REPUTATION_TABLE.put(ShipAPI.HullSize.FRIGATE, RepLevel.FRIENDLY);
        REPUTATION_TABLE.put(ShipAPI.HullSize.DESTROYER, RepLevel.FRIENDLY);
        REPUTATION_TABLE.put(ShipAPI.HullSize.CRUISER, RepLevel.COOPERATIVE);
        REPUTATION_TABLE.put(ShipAPI.HullSize.CAPITAL_SHIP, RepLevel.COOPERATIVE);
    }

    //Table for the different hull sizes available and which commodity you need to convert them. Note that fighters are not allowed to be sold to the market, but are there as a fail-safe anyway
    private static final Map<ShipAPI.HullSize, String> COST_TABLE = new HashMap<>(5);
    static {
        COST_TABLE.put(ShipAPI.HullSize.FIGHTER, Commodities.GAMMA_CORE);
        COST_TABLE.put(ShipAPI.HullSize.FRIGATE, Commodities.BETA_CORE);
        COST_TABLE.put(ShipAPI.HullSize.DESTROYER, Commodities.BETA_CORE);
        COST_TABLE.put(ShipAPI.HullSize.CRUISER, Commodities.ALPHA_CORE);
        COST_TABLE.put(ShipAPI.HullSize.CAPITAL_SHIP, Commodities.ALPHA_CORE);
    }

    //Table for the different hull sizes available and how many days it takes to refit them. Note that fighters are not allowed to be sold to the market, but are there as a fail-safe anyway
    private static final Map<ShipAPI.HullSize, Float> TIME_TABLE = new HashMap<>(5);
    static {
        TIME_TABLE.put(ShipAPI.HullSize.FIGHTER, 1f);
        TIME_TABLE.put(ShipAPI.HullSize.FRIGATE, 5f);
        TIME_TABLE.put(ShipAPI.HullSize.DESTROYER, 10f);
        TIME_TABLE.put(ShipAPI.HullSize.CRUISER, 15f);
        TIME_TABLE.put(ShipAPI.HullSize.CAPITAL_SHIP, 25f);
    }

    //Minimum standing for the market to be open: pretty high, since they need to be sure you're not trying to enslave AI cores
    private static final RepLevel MIN_STANDING = RepLevel.FRIENDLY;


    //Initializer function; just run the original init
    @Override
    public void init(SubmarketAPI submarket) {
        super.init(submarket);
    }

    //This submarket is ignored in the economy
    @Override
    public boolean isParticipatesInEconomy() {
        return false;
    }

    //Simply returns the tariff of the submarket (this is only half as much as normal, but since we pay with AI cores, it shouldn't cause any notable issues unless *extremely* expensive ships are being sold)
    @Override
    public float getTariff() {
        return (market.getTariff().getModifiedValue()*0.5f);
    }

    //No commodities may be sold on the market
    @Override
    public boolean isIllegalOnSubmarket(String commodityId, TransferAction action) {
        return true;
    }

    //Prevent selling actions; really, this is the same as above, but required
    @Override
    public boolean isIllegalOnSubmarket(CargoStackAPI stack, TransferAction action) {
        return action == TransferAction.PLAYER_SELL;
    }

    //Changes the verb for selling/bying ships (go figure!)
    @Override
    public String getSellVerb() {
        return "Convert";
    }
    @Override
    public String getBuyVerb() {
        return "Retrieve";
    }

    //The text that is returned when you are informed *why* you aren't allowed to sell stuff on the normal market
    @Override
    public String getIllegalTransferText(CargoStackAPI stack, TransferAction action) {
        if (action == TransferAction.PLAYER_SELL) {
            return "Sylph-core installation services does not conduct commodity trade.";
        }

        return "AN ERROR OCURRED IN SRD_AIConversion.java: CONTACT THE MOD AUTHOR (Nicke535)";
    }

    //Description text for trying to sell a ship to the submarket that you, for some reason, can't
    @Override
    public String getIllegalTransferText(FleetMemberAPI member, TransferAction action) {
        //Checks if the member being sold already has a Sylph Core
        if (SRD_ModPlugin.hasSylphCoreInstalled(new ArrayList<>(member.getVariant().getHullMods()))) {
            return "This ship already has a Sylph Core installed";
        }

        //Check if we are not a proper sylphon design
        if (!isValidSylphonDesign(member)) {
            return "You can only mount Sylph Cores on Sylphon designs";
        }

        //Check if the design simply doesn't have an upgrade
        if (!hasSylphCoreUpgrade(member)) {
            return "This ship cannot be upgraded with a Sylph Core";
        }

        //Check if we have a proper ship, but not the proper reputation level to upgrade it
        if (isValidSylphonDesign(member) && !Global.getSector().getPlayerFaction().isAtWorst(submarket.getFaction(), REPUTATION_TABLE.get(member.getHullSpec().getHullSize()))) {
            return "Req: " + submarket.getFaction().getDisplayName() + " - " +
                    REPUTATION_TABLE.get(member.getHullSpec().getHullSize()).getDisplayName().toLowerCase();
        }

        //If we can't afford the conversion, tell the player
        if (!playerCanAffordShip(member)) {
            return "You need 1 " + Global.getSettings().getCommoditySpec(COST_TABLE.get(member.getHullSpec().getHullSize())).getName() + " for this transaction, but you have none.";
        }

        //Otherwise, something has gone wrong: notify the end user
        return "AN ERROR OCURRED IN SRD_AIConversion.java: CONTACT THE MOD AUTHOR";
    }

    //Checks if the market should be enabled; in our case, we just check if our reputation is good enough AND that the market is owned by the Sylphon
    @Override
    public boolean isEnabled(CoreUIAPI ui) {
        RepLevel level = submarket.getFaction().getRelationshipLevel(Global.getSector().getFaction(Factions.PLAYER));
        return level.isAtWorst(MIN_STANDING) && this.market.getFactionId().equals("sylphon");
    }

    //An appendix to the tooltip: I honestly don't know exactly how this works, so trial-and-error it is
    @Override
    public String getTooltipAppendix(CoreUIAPI ui) {
        //If the ui isn't enabled (we can't enter the market) there are two possibilities: the market isn't owned by the Sylphon, or we don't have enough standing. Tell the player that.
        if (!isEnabled(ui)) {
            if (!this.market.getFactionId().equals("sylphon")) {
                return "With the Sylphon driven off from this market, the Sylph Core installation facilities are unavailable.";
            }
            return "Requires: " + submarket.getFaction().getDisplayName() + " - " +
                    MIN_STANDING.getDisplayName().toLowerCase();
        }

        //Otherwise, we don't add any appendix (this may change later, for clarity)
        return null;
    }

    //Indicates which part of the appendix should be highlighted
    @Override
    public Highlights getTooltipAppendixHighlights(CoreUIAPI ui) {
        String appendix = getTooltipAppendix(ui);
        //If we don't have an appandix, we don't highlight any part of it (obviously!)
        if (appendix == null) {
            return null;
        }

        //Initialize the highlights
        Highlights h = new Highlights();

        //If we don't have the ui enabled (meaning we can't open the submarket) highlight our entire appendix in red
        if (!isEnabled(ui)) {
            h.setText(appendix);
            h.setColors(Misc.getNegativeHighlightColor());
        }

        //Returns our highlights
        return h;
    }

    //Checks if a ship is allowed to be sold to the submarket; this should match up with the related description earlier in the script
    @Override
    public boolean isIllegalOnSubmarket(FleetMemberAPI member, TransferAction action) {
        if (action == TransferAction.PLAYER_SELL) {
            //If the member already has a sylph core, it's illegal
            if (SRD_ModPlugin.hasSylphCoreInstalled(new ArrayList<>(member.getVariant().getHullMods()))) {
                return true;
            }

            //If we have too little reputation level to upgrade the ship, it's illegal
            if (!Global.getSector().getPlayerFaction().isAtWorst(submarket.getFaction(), REPUTATION_TABLE.get(member.getHullSpec().getHullSize()))) {
                return true;
            }

            //If the ship isn't a valid Sylphon design, it's illegal
            if (!isValidSylphonDesign(member)) {
                return true;
            }

            //If the design doesn't have an upgrade, it's illegal
            if (!hasSylphCoreUpgrade(member)) {
                return true;
            }

            //Otherwise, check if we can afford it: if we can, it's legal. If we can't, it's illegal
            return !playerCanAffordShip(member);
        }

        //If this wasn't a player selling something, the transaction is legal: we're just getting our stuff back!
        return false;
    }

    //This is the most important part; this gets triggered after a transaction, so we can properly handle the adding of a Sylph Core. Please let this work (>_<)
    @Override
    public void reportPlayerMarketTransaction(PlayerMarketTransaction transaction) {
        for (PlayerMarketTransaction.ShipSaleInfo info : transaction.getShipsSold()) {
            //Saves the member we are currently manipulating, and ignores it if it is null for any reason
            FleetMemberAPI member = info.getMember();
            if (member == null) {continue;}

            //Add a permanent hullmod to the member (namely, the Sylph Core). Remember to clone the variant first, or it won't be permanent properly
            ShipVariantAPI variant = member.getVariant();
            variant = variant.clone();
            variant.addPermaMod("SRD_sylph_core");
            variant.setSource(VariantSource.REFIT);
            member.setVariant(variant, false, false);

            //...and remove a core from the player's inventory
            Global.getSector().getPlayerFleet().getCargo().removeCommodity(COST_TABLE.get(member.getHullSpec().getHullSize()), 1);

            //Then, add it to the "delay" plugin, so it takes a while for the refit to complete (while also removing the member from the market)
            Global.getSector().addScript(new SRD_SylphCoreUpgradeTracker(TIME_TABLE.get(member.getHullSpec().getHullSize()), member, submarket.getCargo(), market.getPrimaryEntity().getName()));
            submarket.getCargo().getMothballedShips().removeFleetMember(member);
        }
    }

    //Shorthand function to check if a ship is *actually* a Sylphon ship, and not an Outcast or Null Order ship (Null Order not yet implemented)
    private static boolean isValidSylphonDesign(FleetMemberAPI member) {
        if (!member.getHullSpec().getBaseHullId().contains("SRD_")) {
            return false;
        } else if (member.getVariant().getHullMods().contains("SRD_outcast_engineering")) {
            return false;
        } else {
            return true;
        }
    }

    //Shorthand function to check if a ship has the skin required to be upgraded with a Sylph Core
    private static boolean hasSylphCoreUpgrade (FleetMemberAPI member) {
        if (SRD_ModPlugin.hasSylphCoreInstalled(new ArrayList<>(member.getVariant().getHullMods()))) {
            return false;
        } else if (Global.getSettings().getHullSpec(member.getHullSpec().getBaseHullId()+"_sc") == null) {
            return false;
        } else {
            return true;
        }
    }

    //Shorthand function to check if the player can afford upgrading a specific ship
    private static boolean playerCanAffordShip(FleetMemberAPI member) {
        if (Global.getSector().getPlayerFleet().getCargo().getCommodityQuantity(COST_TABLE.get(member.getHullSpec().getHullSize())) >= 1) {
            return true;
        } else {
            return false;
        }
    }
}