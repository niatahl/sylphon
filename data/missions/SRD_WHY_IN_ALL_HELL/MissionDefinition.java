package data.missions.SRD_WHY_IN_ALL_HELL;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.StarTypes;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;

public class MissionDefinition implements MissionDefinitionPlugin {

	public void defineMission(MissionDefinitionAPI api) {

		// Set up the fleets so we can add ships and fighter wings to them.
		// In this scenario, the fleets are attacking each other, but
		// in other scenarios, a fleet may be defending or trying to escape
		api.initFleet(FleetSide.PLAYER, "OwO", FleetGoal.ATTACK, false);
		api.initFleet(FleetSide.ENEMY, "OP", FleetGoal.ATTACK, true);

//		api.getDefaultCommander(FleetSide.PLAYER).getStats().setSkillLevel(Skills.COORDINATED_MANEUVERS, 3);
//		api.getDefaultCommander(FleetSide.PLAYER).getStats().setSkillLevel(Skills.ELECTRONIC_WARFARE, 3);
		
		// Set a small blurb for each fleet that shows up on the mission detail and
		// mission results screens to identify each side.
		api.setFleetTagline(FleetSide.PLAYER, "Hegemony 12th recon fleet");
		api.setFleetTagline(FleetSide.ENEMY, "The most ridiculous station yet");
		
		// These show up as items in the bulleted list under 
		// "Tactical Objectives" on the mission detail screen
		api.addBriefingItem("Defeat the enemy.");
		
		// Set up the player's fleet.  Variant names come from the
		// files in data/variants and data/variants/fighters
		//api.addToFleet(FleetSide.PLAYER, "SRD_Equilibrium_standard", FleetMemberType.SHIP, true);

		api.addToFleet(FleetSide.PLAYER, "onslaught_Standard", FleetMemberType.SHIP, "I want to go home", true);
		api.addToFleet(FleetSide.PLAYER, "onslaught_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "onslaught_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "mora_Support", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "mora_Support", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "mora_Support", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "mora_Support", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "eagle_Balanced", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "eagle_Balanced", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "eagle_Balanced", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "eagle_Balanced", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "enforcer_Escort", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "enforcer_Escort", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "enforcer_Escort", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "enforcer_Escort", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "enforcer_Escort", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "enforcer_Escort", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "enforcer_Escort", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "enforcer_Escort", FleetMemberType.SHIP, false);
		
		// Set up the enemy fleet.
		//api.addToFleet(FleetSide.ENEMY, "remnant_station2_sylphon_joke", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "remnant_station2_sylphon_mis_joke", FleetMemberType.SHIP, false);
		
		// Set up the map.
		float width = 9000f;
		float height = 11000f;
		api.initMap((float)-width/2f, (float)width/2f, (float)-height/2f, (float)height/2f);
		
		float minX = -width/2;
		float minY = -height/2;
		
		for (int i = 0; i < 15; i++) {
			float x = (float) Math.random() * width - width/2;
			float y = (float) Math.random() * height - height/2;
			float radius = 100f + (float) Math.random() * 900f; 
			api.addNebula(x, y, radius);
		}
		api.setBackgroundSpriteName("graphics/backgrounds/hyperspace_bg_cool.jpg");
		//api.setBackgroundSpriteName("graphics/backgrounds/background2.jpg");
		
		//system.setBackgroundTextureFilename("graphics/backgrounds/background2.jpg");
		//api.setBackgroundSpriteName();
		
		// Add an asteroid field going diagonally across the
		// battlefield, 2000 pixels wide, with a maximum of 
		// 100 asteroids in it.
		// 20-70 is the range of asteroid speeds.
		api.addAsteroidField(0f, 0f, (float) Math.random() * 360f, width,
									20f, 70f, 100);
	}
}
