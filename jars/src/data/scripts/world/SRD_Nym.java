//Script to generate the Nym system, containing a small reseller of Sylphon goods
//
//Originally by Soren, modified by Nicke535
package data.scripts.world;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.DefenderDataOverride;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial.ShipCondition;
import com.fs.starfarer.api.impl.campaign.terrain.*;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;

import java.util.ArrayList;
import java.util.Arrays;

public class SRD_Nym {
    //Main generation function
    public void generate(SectorAPI sector) {
		StarSystemAPI system = sector.createStarSystem("Nym");
        system.getLocation().set(-10000,17600);
		LocationAPI hyper = Global.getSector().getHyperspace();
		
		system.setBackgroundTextureFilename("graphics/sylphon/backgrounds/SRD_nym.jpg");
		
		// Create the star and generate the hyperspace anchor for this system
		// Nym, a swollen supergiant.
		PlanetAPI nym_star = system.initStar("SRD_nym", // unique id for this star
										    "star_red_giant",  // id in planets.json
										    1200f, // radius (in pixels at default zoom)
										    800, // corona radius, from star edge
										    4.5f, // solar wind burn level
                                            0.6f, // flare probability
                                            2.0f); // CR loss multiplier, good values are in the range of 1-5
                                                                                    
        system.setLightColor(new Color(255, 210, 200)); // light color in entire system, affects all entities


        // Some sparse asteroids.
        system.addAsteroidBelt(nym_star, 150, 1440, 120, 220, 290, Terrain.ASTEROID_BELT, "");


        // Some debris in the inner system.
        DebrisFieldTerrainPlugin.DebrisFieldParams params1 = new DebrisFieldTerrainPlugin.DebrisFieldParams(
                520f, // field radius - should not go above 1000 for performance reasons
                1.6f, // density, visual - affects number of debris pieces
                10000000f, // duration in days
                0f); // days the field will keep generating glowing pieces
        params1.source = DebrisFieldTerrainPlugin.DebrisFieldSource.MIXED;
        params1.baseSalvageXP = 750; // base XP for scavenging in field
        SectorEntityToken debrisInner1 = Misc.addDebrisField(system, params1, StarSystemGenerator.random);
        debrisInner1.setSensorProfile(1200f);
        debrisInner1.setDiscoverable(true);
        debrisInner1.setCircularOrbit(nym_star, 360*(float)Math.random(), 1600, 300f);
        debrisInner1.setId("SRD_nym_debrisInner1");

        DebrisFieldTerrainPlugin.DebrisFieldParams params2 = new DebrisFieldTerrainPlugin.DebrisFieldParams(
                360f, // field radius - should not go above 1000 for performance reasons
                1.2f, // density, visual - affects number of debris pieces
                10000000f, // duration in days
                0f); // days the field will keep generating glowing pieces
        params2.source = DebrisFieldTerrainPlugin.DebrisFieldSource.MIXED;
        params2.baseSalvageXP = 500; // base XP for scavenging in field
        SectorEntityToken debrisInner2 = Misc.addDebrisField(system, params2, StarSystemGenerator.random);
        debrisInner2.setSensorProfile(800f);
        debrisInner2.setDiscoverable(true);
        debrisInner2.setCircularOrbit(nym_star, 360*(float)Math.random(), 2000, 330f);
        debrisInner2.setId("SRD_nym_debrisInner2");

        //Adds a communications relay
        SectorEntityToken relay = system.addCustomEntity("SRD_nym_relay", // unique id
                "Nym Relay", // name - if null, defaultName from custom_entities.json will be used
                "comm_relay", // type of object, defined in custom_entities.json
                "sylphon"); // faction
        relay.setCircularOrbit( nym_star, 210, 2400, 90);


        // Talloss, a Tritach agcorp world ---------------
        PlanetAPI nym1 = system.addPlanet("SRD_planet_nym1", nym_star, "Talloss", "terran", 30, 90, 2400, 90);
        nym1.getSpec().setPitch(-15f);
        nym1.getSpec().setTilt(12f);
        nym1.applySpecChanges();
        nym1.setInteractionImage("illustrations", "terran_orbit");

        // add the marketplace to Talloss ---------------
        MarketAPI nym1Market = addMarketplace("tritachyon", nym1, null,
                "Talloss", // name of the market
                3, // size of the market (from the JSON)
                new ArrayList<>(
                        Arrays.asList( // list of market conditions from martinique.json
                                Conditions.FARMLAND_RICH,
                                Conditions.POPULATION_3)),
                new ArrayList<>(
                        Arrays.asList( // which submarkets to generate
                                Submarkets.SUBMARKET_BLACK,
                                Submarkets.SUBMARKET_OPEN,
                                Submarkets.SUBMARKET_STORAGE)),
                new ArrayList<>(
                        Arrays.asList( // Which industries we have on the market
                                Industries.POPULATION,
                                Industries.SPACEPORT,
                                Industries.FARMING)),
                0.3f, // tariff amount
                false, // Free Port
                true); //Has junk and chatter

        nym1.setCustomDescriptionId("SRD_planet_talloss");

        // Gas giant.
        PlanetAPI nym2 = system.addPlanet("SRD_planet_nym2", nym_star, "Mhach", "gas_giant", 300, 400, 3600, 240);
        // Add fixed conditions to Mhach.
        Misc.initConditionMarket(nym2);
        nym2.getMarket().addCondition(Conditions.HOT);
        nym2.getMarket().addCondition(Conditions.EXTREME_WEATHER);
        nym2.getMarket().addCondition(Conditions.DENSE_ATMOSPHERE);
        nym2.getMarket().addCondition(Conditions.HIGH_GRAVITY);
        nym2.getMarket().addCondition(Conditions.VOLATILES_DIFFUSE);
        nym2.getMarket().addCondition(Conditions.ORGANICS_TRACE);
        nym2.setCustomDescriptionId("SRD_planet_mhach");
        //This is for theft prevention!
        nym2.getTags().add("SRD_mhach");

        // And a station in orbit.
        SectorEntityToken ozmaStation = system.addCustomEntity("SRD_ozma_station", "Ozma Station", "SRD_ozma_station", "sylphon");
        ozmaStation.setCircularOrbitPointingDown(system.getEntityById("SRD_planet_nym2"), 30, 475, 30);

        // Add the marketplace to Ozma Station ---------------
        MarketAPI ozmaMarket = addMarketplace("sylphon", ozmaStation, null,
                "Ozma Station", // name of the market
                5, // size of the market
                new ArrayList<>(
                        Arrays.asList( // list of market conditions
                                Conditions.OUTPOST,
                                Conditions.POPULATION_5)),
                new ArrayList<>(
                        Arrays.asList( // which submarkets to generate
                                Submarkets.GENERIC_MILITARY,
                                Submarkets.SUBMARKET_BLACK,
                                Submarkets.SUBMARKET_OPEN,
                                Submarkets.SUBMARKET_STORAGE)),
                new ArrayList<>(
                        Arrays.asList( // Which industries we have on the market
                                Industries.POPULATION,
                                Industries.SPACEPORT,
                                Industries.FUELPROD,
                                Industries.PATROLHQ,
                                Industries.HEAVYBATTERIES,
                                Industries.WAYSTATION,
                                Industries.BATTLESTATION)),
                0.3f, // tariff amount
                true, // Free Port
                true); //Has junk and chatter

        ozmaStation.setCustomDescriptionId("SRD_ozma_station");

        // First moon.
        PlanetAPI nym2a = system.addPlanet("SRD_planet_nym2a", nym2, "Mhach A", "barren", 360*(float)Math.random(), 90, 540, 90f);
        nym2a.getSpec().setTexture(Global.getSettings().getSpriteName("planets", "barren02"));
        // Add fixed conditions to Mhach A.
        Misc.initConditionMarket(nym2a);
        nym2a.getMarket().addCondition(Conditions.NO_ATMOSPHERE);
        nym2a.getMarket().addCondition(Conditions.LOW_GRAVITY);
        nym2a.getMarket().addCondition(Conditions.ORE_MODERATE);
        //nym2a.setCustomDescriptionId("planet_nym2a");

        // Second moon.
        PlanetAPI silver2b = system.addPlanet("SRD_planet_nym2b", nym2, "Mhach B", "barren-desert", 360*(float)Math.random(), 60, 1080, 144f);
        // Add fixed conditions to Mhach B.
        Misc.initConditionMarket(silver2b);
        silver2b.getMarket().addCondition(Conditions.THIN_ATMOSPHERE);
        silver2b.getMarket().addCondition(Conditions.ORE_SPARSE);
        silver2b.getMarket().addCondition(Conditions.RUINS_SCATTERED);
        silver2b.getMarket().addCondition(Conditions.POLLUTION);
        //nym2a.setCustomDescriptionId("planet_nym2b");

        // Mhach's trojans.
        SectorEntityToken nymL4 = system.addTerrain(Terrain.ASTEROID_FIELD,
                new AsteroidFieldTerrainPlugin.AsteroidFieldParams(
                        920f, // min radius
                        1280f, // max radius
                        40, // min asteroid count
                        72, // max asteroid count
                        8f, // min asteroid radius
                        24f, // max asteroid radius
                        "Yod L4 Shoal Zone")); // null for default name

        SectorEntityToken nymL5 = system.addTerrain(Terrain.ASTEROID_FIELD,
                new AsteroidFieldTerrainPlugin.AsteroidFieldParams(
                        920f, // min radius
                        1280f, // max radius
                        40, // min asteroid count
                        72, // max asteroid count
                        8f, // min asteroid radius
                        24f, // max asteroid radius
                        "Yod L5 Shoal Zone")); // null for default name

        nymL4.setCircularOrbit(nym_star, 360f, 3600, 300);
        nymL5.setCircularOrbit(nym_star, 240f, 3600, 300);

        // Inner jump point ---------------
        JumpPointAPI jumpPoint1 = Global.getFactory().createJumpPoint("SRD_nym_inner_jump", "Nym Rift");
        jumpPoint1.setCircularOrbit( system.getEntityById("SRD_planet_nym1"), 360f*(float)Math.random(), 900, 110);
        jumpPoint1.setRelatedPlanet(nym1);
        system.addEntity(jumpPoint1);

        //More debris
        DebrisFieldTerrainPlugin.DebrisFieldParams params3 = new DebrisFieldTerrainPlugin.DebrisFieldParams(
                480f, // field radius - should not go above 1000 for performance reasons
                1.5f, // density, visual - affects number of debris pieces
                10000000f, // duration in days
                0f); // days the field will keep generating glowing pieces
        params3.source = DebrisFieldTerrainPlugin.DebrisFieldSource.MIXED;
        params3.baseSalvageXP = 750; // base XP for scavenging in field
        SectorEntityToken debris3 = Misc.addDebrisField(system, params3, StarSystemGenerator.random);
        debris3.setSensorProfile(1000f);
        debris3.setDiscoverable(true);
        debris3.setCircularOrbit(nym_star, 360*(float)Math.random(), 4800, 270f);
        debris3.setId("SRD_nym_debris3");

        // Some procgen can go out here.
        float radiusAfter = StarSystemGenerator.addOrbitingEntities(system, nym_star, StarAge.AVERAGE,
                3, 5, // min/max entities to add
                5200, // radius to start adding at
                3, // name offset - next planet will be <system name> <roman numeral of this parameter + 1>
                true); // whether to use custom or system-name based names

        //Stable location outside the procgen
        SectorEntityToken stableLoc1 = system.addCustomEntity("SRD_nym_stable_location", "Stable Location", "stable_location", Factions.NEUTRAL);
        stableLoc1.setCircularOrbit(nym_star, MathUtils.getRandomNumberInRange(0, 360f), radiusAfter+MathUtils.getRandomNumberInRange(700f, 1100f), MathUtils.getRandomNumberInRange(900, 1200));

        // generates hyperspace destinations for in-system jump points
        system.autogenerateHyperspaceJumpPoints(true, true);

        //Finally cleans up hyperspace
        cleanup(system);
    }

    private void cleanup(StarSystemAPI system){
        HyperspaceTerrainPlugin plugin = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
        NebulaEditor editor = new NebulaEditor(plugin);
        float minRadius = plugin.getTileSize() * 2f;

        float radius = system.getMaxRadiusInHyperspace();
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius * 0.5f, 0f, 360f);
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0f, 360f, 0.25f);
    }


    //Shorthand function for adding a market
    public static MarketAPI addMarketplace(String factionID, SectorEntityToken primaryEntity, ArrayList<SectorEntityToken> connectedEntities, String name,
                                           int size, ArrayList<String> marketConditions, ArrayList<String> submarkets, ArrayList<String> industries, float tarrif,
                                           boolean freePort, boolean withJunkAndChatter) {
        EconomyAPI globalEconomy = Global.getSector().getEconomy();
        String planetID = primaryEntity.getId();
        String marketID = planetID + "_market";

        MarketAPI newMarket = Global.getFactory().createMarket(marketID, name, size);
        newMarket.setFactionId(factionID);
        newMarket.setPrimaryEntity(primaryEntity);
        newMarket.getTariff().modifyFlat("generator", tarrif);

        //Adds submarkets
        if (null != submarkets) {
            for (String market : submarkets) {
                newMarket.addSubmarket(market);
            }
        }

        //Adds market conditions
        for (String condition : marketConditions) {
            newMarket.addCondition(condition);
        }

        //Add market industries
        for (String industry : industries) {
            newMarket.addIndustry(industry);
        }

        //Sets us to a free port, if we should
        newMarket.setFreePort(freePort);

        //Adds our connected entities, if any
        if (null != connectedEntities) {
            for (SectorEntityToken entity : connectedEntities) {
                newMarket.getConnectedEntities().add(entity);
            }
        }

        globalEconomy.addMarket(newMarket, withJunkAndChatter);
        primaryEntity.setMarket(newMarket);
        primaryEntity.setFaction(factionID);

        if (null != connectedEntities) {
            for (SectorEntityToken entity : connectedEntities) {
                entity.setMarket(newMarket);
                entity.setFaction(factionID);
            }
        }

        //Finally, return the newly-generated market
        return newMarket;
    }
}
