package net.blerf.ftl.parser;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import net.blerf.ftl.model.ShipLayout;
import net.blerf.ftl.xml.Achievement;
import net.blerf.ftl.xml.Blueprints;
import net.blerf.ftl.xml.ShipBlueprint;
import net.blerf.ftl.xml.WeaponBlueprint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataManager implements Closeable {
	
	private static final Logger log = LogManager.getLogger(DataManager.class);

	private static DataManager instance;
	
	public static DataManager get() {
		return instance;
	}
	
	public static void init(File ftlFolder) throws IOException, JAXBException {
		instance = new DataManager(ftlFolder);	
	}

	private List<Achievement> achievements;
	private List<Achievement> generalAchievements;
	private Blueprints blueprints;

	private Map<String, WeaponBlueprint> weapons;	
	private Map<String, ShipBlueprint> ships;
	private List<ShipBlueprint> playerShips; // Type A's
	private Map<ShipBlueprint, List<Achievement>> shipAchievements;
	private Map<String, ShipLayout> shipLayouts;
	
	private	MappedDatParser dataParser = null;
	private	MappedDatParser resourceParser = null;
	
	private DataManager(File ftlFolder) throws IOException, JAXBException {
		
		log.trace("DataManager initialising");
		
		InputStream ach_stream = null;
		InputStream blue_stream = null;
		
		try {
			dataParser = new MappedDatParser( new File(ftlFolder, "resources/data.dat") );
	 		resourceParser = new MappedDatParser( new File(ftlFolder, "resources/resource.dat") );
		
			ach_stream = dataParser.getInputStream( "data/achievements.xml" );
			achievements = dataParser.readAchievements( ach_stream );
			blue_stream = dataParser.getInputStream( "data/blueprints.xml" );
			blueprints = dataParser.readBlueprints( blue_stream );
		
			generalAchievements = new ArrayList<Achievement>();
			for( Achievement ach : achievements )
				if ( ach.getShipId() == null )
					generalAchievements.add(ach);

			weapons = new HashMap<String, WeaponBlueprint>();
			for ( WeaponBlueprint weapon : blueprints.getWeaponBlueprint() )
				weapons.put( weapon.getId(), weapon );

			ships = new HashMap<String, ShipBlueprint>();
			for ( ShipBlueprint ship : blueprints.getShipBlueprint() )
				ships.put( ship.getId(), ship );
		
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
			for (ShipBlueprint ship: playerShips) {
				List<Achievement> shipAchs = new ArrayList<Achievement>();
				for ( Achievement ach : achievements )
					if ( ship.getId().equals( ach.getShipId() ) )
						shipAchs.add(ach);
				shipAchievements.put( ship, shipAchs );
			}

			// This'll populate as layouts are requested.
			shipLayouts = new HashMap<String, ShipLayout>();
		
		} catch (IOException e) {
			log.error( "Error while constructing DataManager", e );
			this.close();

		} finally {
			try {if (ach_stream != null) ach_stream.close();}
			catch (IOException f) {}
			try {if (blue_stream != null) blue_stream.close();}
			catch (IOException f) {}
		}
	}

	public void close() {
		try {if (dataParser != null) dataParser.close();}
		catch (IOException e) {}

		try {if (resourceParser != null) resourceParser.close();}
		catch (IOException e) {}
	}
	
	public InputStream getDataInputStream( String innerPath ) throws IOException {
		return dataParser.getInputStream( innerPath );
	}
	
	public InputStream getResourceInputStream( String innerPath ) throws IOException {
		return resourceParser.getInputStream( innerPath );
	}
	
	public void unpackData( File outFolder ) throws IOException {
		dataParser.unpackDat( outFolder );
	}

	public void unpackResources( File outFolder ) throws IOException {
		resourceParser.unpackDat( outFolder );
	}
	
	public List<Achievement> getAchievements() {
		return achievements;
	}
	
	public WeaponBlueprint getWeapon( String id ) {
		WeaponBlueprint result = weapons.get(id);
		if ( result == null )
			log.error( "No WeaponBlueprint found for id: "+ id );
		return result;
	}

	public ShipBlueprint getShip( String id ) {
		ShipBlueprint result = ships.get(id);
		if ( result == null )
			log.error( "No ShipBlueprint found for id: "+ id );
		return result;
	}
	
	public List<ShipBlueprint> getPlayerShips() {
		return playerShips;
	}
	
	public List<Achievement> getShipAchievements(ShipBlueprint ship) {
		return shipAchievements.get(ship);
	}
	
	public List<Achievement> getGeneralAchievements() {
		return generalAchievements;
	}

	public ShipLayout getShipLayout(String id) {
		ShipLayout result = shipLayouts.get(id);

		if ( result == null ) {  // Wasn't cached; try parsing it.
			InputStream in = null;
			try {
				in = getDataInputStream("data/"+ id +".txt");
				result = dataParser.readLayout(in);
				shipLayouts.put( id, result );

			} catch (FileNotFoundException e) {
				log.error( "No ShipLayout found for id: "+ id );

			} catch (IOException e) {
				log.error( "An error occurred while parsing ShipLayout: "+ id, e );

			} finally {
				try {if (in != null) in.close();}
				catch (IOException f) {}
			}
		}

		return result;
	}
}
