//Script to generate the Rofocale system, the main HQ of Sylphon RnD
package data.scripts.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.impl.campaign.econ.AICoreAdmin;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.ids.Terrain;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.procgen.PlanetConditionGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.AICores;
import com.fs.starfarer.api.impl.campaign.terrain.AsteroidFieldTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaignPlugins.SRD_AIConversionFleetUpgraderPlugin;
import org.jetbrains.annotations.Nullable;
import org.lazywizard.lazylib.MathUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

public class SRD_Rofocale {
    //Main generation function
    public void generate(SectorAPI sector) {
		StarSystemAPI system = sector.createStarSystem("Rofocale");
        system.getLocation().set(-14800,25100);
		LocationAPI hyper = Global.getSector().getHyperspace();
		system.setBackgroundTextureFilename("graphics/sylphon/backgrounds/SRD_rofocale.jpg");
		
		// Create the star and generate the hyperspace anchor for this system
		// Rofocale, a dangerous neutron star which has ended any hope for life near the system's center.
		PlanetAPI rofocale_star = system.initStar("SRD_rofocale", // unique id for this star
										    "star_neutron",  // id in planets.json
										    300f, // radius (in pixels at default zoom)
										    2100f, // corona radius, from star edge
										    2.3f, // solar wind burn level
                                            2.1f, // flare probability
                                            1.9f); // CR loss multiplier, good values are in the range of 1-5

        system.setLightColor(new Color(255, 255, 255)); // light color in entire system, affects all entities


        // Some debris in the inner system: at least one fleet was wiped out from a flare
        DebrisFieldTerrainPlugin.DebrisFieldParams params1 = new DebrisFieldTerrainPlugin.DebrisFieldParams(
                120f, // field radius - should not go above 1000 for performance reasons
                1.4f, // density, visual - affects number of debris pieces
                10000000f, // duration in days
                100000000f); // days the field will keep generating glowing pieces
        params1.source = DebrisFieldTerrainPlugin.DebrisFieldSource.BATTLE;
        params1.baseSalvageXP = 650; // base XP for scavenging in field
        params1.glowColor = Color.white;
        SectorEntityToken debrisInner1 = Misc.addDebrisField(system, params1, StarSystemGenerator.random);
        debrisInner1.setSensorProfile(1800f);
        debrisInner1.setDiscoverable(true);
        debrisInner1.setCircularOrbit(rofocale_star, 360*(float)Math.random(), 600, 300f);
        debrisInner1.setId("SRD_rofocale_debrisInner1");

        //Inner planets 1, 2 and 3; have auto-generated conditions
        PlanetAPI rofocale1 = system.addPlanet("SRD_planet_rofocale1", rofocale_star, "Cinderhulk", "irradiated", 360f*(float)Math.random(), 80, 900, 120);
        PlanetConditionGenerator.generateConditionsForPlanet(rofocale1, StarAge.YOUNG);
        PlanetAPI rofocale2 = system.addPlanet("SRD_planet_rofocale2", rofocale_star, "Kugane", "barren3", 360f*(float)Math.random(), 215, 2100, 150);
        PlanetConditionGenerator.generateConditionsForPlanet(rofocale2, StarAge.YOUNG);
        PlanetAPI rofocale3 = system.addPlanet("SRD_planet_rofocale3", rofocale_star, "Doma", "barren", 360f*(float)Math.random(), 135, 4000, 310);
        PlanetConditionGenerator.generateConditionsForPlanet(rofocale3, StarAge.YOUNG);

        // A dense asteroid field shielding outer planets from most of the radiation; also has several ring bands to give a nice feeling
        system.addAsteroidBelt(rofocale_star, 3000, 8100, 1000, 170, 490, Terrain.ASTEROID_BELT, "The Great Divide");
        system.addRingBand(rofocale_star, "misc", "rings_asteroids0", 256f, 1, Color.gray, 256f, 7730, 200f);
        system.addRingBand(rofocale_star, "misc", "rings_asteroids0", 256f, 0, Color.gray, 256f, 7970, 400f);
        system.addRingBand(rofocale_star, "misc", "rings_asteroids0", 256f, 3, Color.gray, 256f, 8100, 220f);
        system.addRingBand(rofocale_star, "misc", "rings_asteroids0", 256f, 0, Color.gray, 256f, 8228, 370f);
        system.addRingBand(rofocale_star, "misc", "rings_asteroids0", 256f, 2, Color.gray, 256f, 8350, 235f);
        system.addRingBand(rofocale_star, "misc", "rings_asteroids0", 256f, 2, Color.gray, 256f, 8500, 320f);

        //Adds a communications relay *inside* the asteroid belt
        SectorEntityToken relay = system.addCustomEntity("SRD_rofocale_relay", // unique id
                "Rofocale Relay", // name - if null, defaultName from custom_entities.json will be used
                "comm_relay", // type of object, defined in custom_entities.json
                "sylphon"); // faction
        relay.setCircularOrbitPointingDown( rofocale_star, 360f*(float)Math.random(), 8100, MathUtils.getRandomNumberInRange(250, 410));
        relay.setCustomDescriptionId("SRD_rofocale_relay");

        // The industrial center of the Sylphon, shielded by a giant Geofront
        PlanetAPI rofocale4 = system.addPlanet("SRD_planet_rofocale4", rofocale_star, "Castrum", "cryovolcanic", 300, 200, 13000, 480);
        rofocale4.setCustomDescriptionId("SRD_planet_castrum");
        MarketAPI rofocale4_market = addMarketplace("sylphon", rofocale4, null,
                "Castrum", // name of the market
                6, // size of the market
                new ArrayList<>(
                        Arrays.asList(
                                Conditions.POPULATION_6,
                                Conditions.ORE_SPARSE,
                                Conditions.RARE_ORE_MODERATE,
                                Conditions.COLD)),
                new ArrayList<>(
                        Arrays.asList( // Which submarkets to generate
                                Submarkets.GENERIC_MILITARY,
                                Submarkets.SUBMARKET_OPEN,
                                "SRD_ai_conversion", //Special submarket for upgrading ships with Sylph Cores
                                Submarkets.SUBMARKET_STORAGE)),
                new ArrayList<String>(
                        Arrays.asList( // Which industries we have on the market
                                Industries.POPULATION,
                                Industries.HEAVYINDUSTRY,
                                Industries.LIGHTINDUSTRY,
                                Industries.MINING,
                                Industries.REFINING,
                                Industries.MEGAPORT,
                                Industries.BATTLESTATION_HIGH,
                                Industries.HEAVYBATTERIES,
                                Industries.ORBITALWORKS)),
                0.3f, // tariff amount
                false); // Free Port

        // The moon Sylphon HQ orbits
        PlanetAPI rofocale4a = system.addPlanet("SRD_planet_rofocale4a", rofocale4, "Praetorium", "barren", 360*(float)Math.random(), 80, 680, 95f);
        rofocale4a.getSpec().setTexture(Global.getSettings().getSpriteName("planets", "barren02"));
        // Add fixed conditions to the moon
        Misc.initConditionMarket(rofocale4a);
        rofocale4a.getMarket().addCondition(Conditions.NO_ATMOSPHERE);
        rofocale4a.getMarket().addCondition(Conditions.LOW_GRAVITY);
        rofocale4a.getMarket().addCondition(Conditions.ORE_SPARSE);
        rofocale4a.getMarket().addCondition(Conditions.RARE_ORE_SPARSE);
        rofocale4a.getMarket().addCondition(Conditions.RUINS_WIDESPREAD);
        rofocale4a.getMarket().addCondition(Conditions.VERY_COLD);
        //rofocale4a.setCustomDescriptionId("SRD_planet_praetorium");

        // Adds the main base of the Sylphon
        SectorEntityToken sylpheed_station = system.addCustomEntity("SRD_sylpheed_station", "Sylpheed Station", "SRD_sylpheed_station", "sylphon");
        sylpheed_station.setCircularOrbitPointingDown(system.getEntityById("SRD_planet_rofocale4a"), 360*(float)Math.random(), 210, 40);

        // Add the marketplace to Sylpheed Station ---------------
        MarketAPI sylpheed_station_market = addMarketplace("sylphon", sylpheed_station, null,
                "Sylpheed Station", // name of the market
                6, // size of the market
                new ArrayList<>(
                        Arrays.asList(
                                Conditions.POPULATION_6)),
                new ArrayList<>(
                        Arrays.asList( // which submarkets to generate
                                Submarkets.GENERIC_MILITARY,
                                Submarkets.SUBMARKET_BLACK,
                                Submarkets.SUBMARKET_OPEN,
                                "SRD_ai_conversion", //Special submarket for upgrading ships with Sylph Cores
                                Submarkets.SUBMARKET_STORAGE)),
                new ArrayList<String>(
                        Arrays.asList( // Which industries we have on the market
                                Industries.POPULATION,
                                Industries.FARMING,
                                Industries.MEGAPORT,
                                Industries.STARFORTRESS_HIGH,
                                Industries.HIGHCOMMAND,
                                Industries.ORBITALWORKS,
                                Industries.HEAVYBATTERIES,
                                "SRD_sylpheed_station_tech_mining")),
                0.3f, // tariff amount
                false); // Free Port
        sylpheed_station.setCustomDescriptionId("SRD_sylpheed_station");
        sylpheed_station_market.getMemoryWithoutUpdate().set("$SRD_SylpheedTechMiningPlanet", rofocale4a.getMarket());


        // Some trojans for the inhabited planet
        SectorEntityToken rofocale4_troj = system.addTerrain(Terrain.ASTEROID_FIELD,
                new AsteroidFieldTerrainPlugin.AsteroidFieldParams(
                        620f, // min radius
                        980f, // max radius
                        32, // min asteroid count
                        63, // max asteroid count
                        7f, // min asteroid radius
                        17f, // max asteroid radius
                        "")); // null for default name
        rofocale4_troj.setCircularOrbit(rofocale_star, 60f, 6500, 480);

        // Jump point for the main base ---------------
        JumpPointAPI jumpPoint1 = Global.getFactory().createJumpPoint("SRD_rofocale_base_jump", "Rofocale Rift");
        jumpPoint1.setCircularOrbit( system.getEntityById("SRD_rofocale"), 290, 13000, 480);
        jumpPoint1.setRelatedPlanet(rofocale4);
        system.addEntity(jumpPoint1);

        // Registers the system's LocationAPI for our special fleet upgrading script
        sector.addScript(new SRD_AIConversionFleetUpgraderPlugin(sylpheed_station.getContainingLocation()));

        // generates hyperspace destinations for in-system jump points
        system.autogenerateHyperspaceJumpPoints(true, true);

        //Finally cleans up hyperspace
        cleanup(system);
    }

    //Shorthand function for cleaning up hyperspace
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
                                           boolean freePort) {
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

        globalEconomy.addMarket(newMarket, true);
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
