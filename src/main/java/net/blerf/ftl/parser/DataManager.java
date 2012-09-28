package net.blerf.ftl.parser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.blerf.ftl.FTLFrame;
import net.blerf.ftl.xml.Achievement;
import net.blerf.ftl.xml.Blueprints;
import net.blerf.ftl.xml.ShipBlueprint;

public class DataManager {
	
	private static final Logger log = LogManager.getLogger(DataManager.class);

	private static DataManager instance;
	
	public static DataManager get() {
		return instance;
	}
	
	public static void init(File ftlFolder, File dataFolder) throws IOException, JAXBException {
		instance = new DataManager(ftlFolder, dataFolder);	
	}

	private List<Achievement> achievements;
	private Blueprints blueprints;
	
	private Map<String, ShipBlueprint> ships;
	private List<ShipBlueprint> playerShips; // Type A's
	private Map<ShipBlueprint, List<Achievement>> shipAchievements;
	
	private File dataFolder;
	
	private DataManager(File ftlFolder, File dataFolder) throws IOException, JAXBException {
		
		this.dataFolder = dataFolder;
		
		log.trace("DataManager initialising");
		
		DatParser datParser = new DatParser();
		
		File datFolder = new File( dataFolder, "data" );
		File imgFolder = new File( dataFolder, "img" );
		
		boolean unpackData = !dataFolder.exists() || !datFolder.exists();
		boolean unpackRes = !dataFolder.exists() || !imgFolder.exists();
		
		if( unpackData )
			datParser.unpackDat( new File(ftlFolder, "resources/data.dat") , dataFolder );
		if( unpackRes )
			datParser.unpackDat( new File(ftlFolder, "resources/resource.dat") , dataFolder );
		
		achievements = datParser.readAchievements( new File( dataFolder, "data/achievements.xml") );
		blueprints = datParser.readBlueprints( new File( dataFolder, "data/blueprints.xml" ) );
		
		ships = new HashMap<String, ShipBlueprint>();
		for( ShipBlueprint ship: blueprints.getShipBlueprint() )
			ships.put( ship.getId() , ship );
		
		playerShips = new ArrayList<ShipBlueprint>();
		playerShips.add( ships.get("PLAYER_SHIP_HARD") );
		playerShips.add( ships.get("PLAYER_SHIP_STEALTH") );
		playerShips.add( ships.get("PLAYER_SHIP_MANTIS") );
		playerShips.add( ships.get("PLAYER_SHIP_CIRCLE") );
		playerShips.add( ships.get("PLAYER_SHIP_FED") );
		playerShips.add( ships.get("PLAYER_SHIP_JELLY") );
		playerShips.add( ships.get("PLAYER_SHIP_ROCK") );
		playerShips.add( ships.get("PLAYER_SHIP_ENERGY") );
		playerShips.add( ships.get("PLAYER_SHIP_CRYSTAL") );
		
		shipAchievements = new HashMap<ShipBlueprint, List<Achievement>>();
		for(ShipBlueprint ship: playerShips) {
			List<Achievement> shipAchs = new ArrayList<Achievement>();
			for( Achievement ach: achievements )
				if( ship.getId().equals( ach.getShipId() ) )
					shipAchs.add(ach);
			shipAchievements.put( ship, shipAchs );
		}
		
	}
	
	public File getDataFolder() {
		return dataFolder;
	}
	
	public List<Achievement> getAchievements() {
		return achievements;
	}
	
	public ShipBlueprint getShip(String id) {
		return ships.get(id);
	}
	
	public List<ShipBlueprint> getPlayerShips() {
		return playerShips;
	}
	
	public List<Achievement> getShipAchievements(ShipBlueprint ship) {
		return shipAchievements.get(ship);
	}
	
}
