package data.scripts.campaign.barEvents.meetCieve;

import com.fs.starfarer.api.impl.campaign.intel.bar.PortsideBarEvent;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventCreator;

public class SRD_MeetEccentricCieveBarEventCreator extends BaseBarEventCreator {

    public PortsideBarEvent createBarEvent() {
        return new SRD_MeetEccentricCieveBarEvent();
    }

    @Override
    public float getBarEventAcceptedTimeoutDuration() {
        return 10000000000f; // One-time-only: takes a good 27.7 or so MILLION in-game years to respawn
    }

    //How often the bar event appears, relative to other bar events. The standard value is 10f
    @Override
    public float getBarEventFrequencyWeight() {
        return 5000f;
    }

    //Whether the event should practically always appear, due to being game-critical. Set to true for testing purposes
    @Override
    public boolean isPriority() {
        return true;
    }
}