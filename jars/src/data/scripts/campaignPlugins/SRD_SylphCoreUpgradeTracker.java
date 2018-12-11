//By Nicke535, used to track Sylph Core upgrade time remaining
package data.scripts.campaignPlugins;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.fleet.FleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;

public class SRD_SylphCoreUpgradeTracker implements EveryFrameScript {

    private final CargoAPI cargo;
    private final FleetMemberAPI member;
    private String variantToUpgradeTo = "ERROR";
    private boolean started = false;
    private String marketName = "ERROR";
    private float time;

    //Initializer, called when creating the script
    public SRD_SylphCoreUpgradeTracker(float days, FleetMemberAPI member, String variantToUpgradeTo, CargoAPI cargo, String marketName) {
        time = days;
        this.member = member;
        this.variantToUpgradeTo = variantToUpgradeTo;
        this.cargo = cargo;
        this.marketName = marketName;
    }


    @Override
    public void advance(float amount) {
        if (!Global.getSector().isPaused() && !started) {
            started = true;
            Global.getSector().getCampaignUI().addMessage(
                    marketName + ": " + member.getShipName() + " Sylph Core installation will be done in " + Integer.toString(
                            Math.round(time)) + " days",
                    Global.getSettings().getColor("standardTextColor"),
                    member.getShipName(), Integer.toString(Math.round(time)),
                    Global.getSettings().getColor("textFriendColor"),
                    Global.getSettings().getColor("yellowTextColor"));
        }
        if (time > 0f) {
            time -= Global.getSector().getClock().convertToDays(amount);
            if (time <= 0f) {
                //Generate a new ship
                FleetMemberAPI newMember = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variantToUpgradeTo);
                newMember.setShipName(member.getShipName());

                //Then, add it to the market and alert the player of this
                Global.getSector().getCampaignUI().addMessage(
                        marketName + ": " + newMember.getShipName() + " has completed installation of its Sylph Core",
                        Global.getSettings().getColor("standardTextColor"),
                        newMember.getShipName(), "Sylph Core",
                        Global.getSettings().getColor("textFriendColor"),
                        Global.getSettings().getColor("yellowTextColor"));
                cargo.getMothballedShips().addFleetMember(newMember);
            }
        }
    }

    @Override
    public boolean isDone() {
        return time <= 0f;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }
}
