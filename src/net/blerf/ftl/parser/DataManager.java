package net.blerf.ftl.parser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import net.blerf.ftl.xml.Achievement;
import net.blerf.ftl.xml.Blueprints;
import net.blerf.ftl.xml.ShipBlueprint;

public class DataManager {

	private static DataManager instance;
	
	public static DataManager get() {
		return instance;
	}
	
	// TODO handle exceptions better
	public static void init() throws IOException, JAXBException {
		instance = new DataManager();	
	}

	private List<Achievement> achievements;
	private Blueprints blueprints;
	
	private Map<String, ShipBlueprint> ships;
	private List<ShipBlueprint> playerShips; // Type As
	
	private DataManager() throws IOException, JAXBException {
		
		DatParser datParser = new DatParser();
		
		File dataFolder = new File("ftldata");
		
		if( !dataFolder.exists() )
			datParser.unpackDat( new File(".") , dataFolder ); // TODO locate data.dat via dialog (store loc somewhere)
		
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
		
	}
	
	public ShipBlueprint getShip(String id) {
		return ships.get(id);
	}
	
	public List<ShipBlueprint> getPlayerShips() {
		return playerShips;
	}
	
}
