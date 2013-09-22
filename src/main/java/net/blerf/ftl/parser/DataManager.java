package net.blerf.ftl.parser;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import net.blerf.ftl.model.ShipLayout;
import net.blerf.ftl.parser.DatParser;
import net.blerf.ftl.xml.Achievement;
import net.blerf.ftl.xml.AugBlueprint;
import net.blerf.ftl.xml.BackgroundImage;
import net.blerf.ftl.xml.BackgroundImageList;
import net.blerf.ftl.xml.Blueprints;
import net.blerf.ftl.xml.CrewNameList;
import net.blerf.ftl.xml.DroneBlueprint;
import net.blerf.ftl.xml.Encounters;
import net.blerf.ftl.xml.FTLEvent;
import net.blerf.ftl.xml.FTLEventList;
import net.blerf.ftl.xml.ShipBlueprint;
import net.blerf.ftl.xml.ShipEvent;
import net.blerf.ftl.xml.ShipEvents;
import net.blerf.ftl.xml.ShipChassis;
import net.blerf.ftl.xml.SystemBlueprint;
import net.blerf.ftl.xml.WeaponBlueprint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class DataManager implements Closeable {
	
	private static final Logger log = LogManager.getLogger(DataManager.class);

	private static DataManager instance;


	public static DataManager get() {
		return instance;
	}

	public static void init( File datsDir ) throws IOException, JAXBException {
		instance = new DataManager( datsDir );
	}


	private List<Achievement> achievements;
	private List<Achievement> generalAchievements;
	private Blueprints blueprints;
	private Blueprints autoBlueprints;
	private Map<String, Encounters> events;
	private Map<String, ShipEvent> shipEvents;
	private Map<String, BackgroundImageList> backgroundImageLists;

	private Map<String, AugBlueprint> augments;
	private Map<String, DroneBlueprint> drones;
	private Map<String, SystemBlueprint> systems;
	private Map<String, WeaponBlueprint> weapons;
	private Map<String, ShipBlueprint> ships;
	private Map<String, ShipBlueprint> autoShips;
	private List<ShipBlueprint> playerShips; // Type A's
	private Map<ShipBlueprint, List<Achievement>> shipAchievements;
	private Map<String, ShipLayout> shipLayouts;
	private Map<String, ShipChassis> shipChassisMap;
	private List<CrewNameList.CrewName> crewNamesMale;
	private List<CrewNameList.CrewName> crewNamesFemale;

	private	DatParser dataParser = null;
	private	DatParser resourceParser = null;


	private DataManager( File datsDir ) throws IOException, JAXBException {
		
		log.trace( "DataManager initialising" );
		
		boolean meltdown = false;
		ArrayList<InputStream> streams = new ArrayList<InputStream>();

		try {
			dataParser = new DatParser( new File( datsDir, "data.dat" ) );
	 		resourceParser = new DatParser( new File( datsDir, "resource.dat" ) );

			log.info( "Reading Achievements..." );
			log.debug( "Reading 'data/achievements.xml'" );
			InputStream achStream = dataParser.getInputStream( "data/achievements.xml" );
			streams.add(achStream);
			achievements = dataParser.readAchievements( achStream );

			log.info( "Reading Blueprints..." );
			log.debug( "Reading 'data/blueprints.xml'" );
			InputStream blueStream = dataParser.getInputStream( "data/blueprints.xml" );
			streams.add(blueStream);
			blueprints = dataParser.readBlueprints( blueStream, "blueprints.xml" );

			log.debug( "Reading 'data/autoBlueprints.xml'" );
			InputStream autoBlueStream = dataParser.getInputStream( "data/autoBlueprints.xml" );
			streams.add(autoBlueStream);
			autoBlueprints = dataParser.readBlueprints( autoBlueStream, "autoBlueprints.xml" );

			log.info( "Reading Events..." );
			String[] eventsFileNames = new String[] { "events.xml", "newEvents.xml",
				"events_crystal.xml", "events_engi.xml", "events_mantis.xml",
				"events_rock.xml", "events_slug.xml", "events_zoltan.xml",
				"events_nebula.xml", "events_pirate.xml", "events_rebel.xml",
				"nameEvents.xml", "events_fuel.xml", "events_boss.xml" };

			events = new LinkedHashMap<String, Encounters>();
			for ( String eventsFileName : eventsFileNames ) {
				log.debug("Reading 'data/"+ eventsFileName +"'");
				InputStream tmpStream = dataParser.getInputStream( "data/"+ eventsFileName );
				streams.add(tmpStream);
				Encounters tmpEncounters = dataParser.readEvents( tmpStream, eventsFileName );
				events.put( eventsFileName, tmpEncounters );
			}

			log.info( "Reading Crew Names..." );
			log.debug( "Reading 'data/names.xml'" );
			InputStream crewNamesStream = dataParser.getInputStream( "data/names.xml" );
			streams.add(crewNamesStream);
			List<CrewNameList> crewNameLists = dataParser.readCrewNames( crewNamesStream );

			log.info( "Reading Ship Events..." );
			log.debug( "Reading 'data/events_ships.xml'" );
			InputStream shipEventsStream = dataParser.getInputStream( "data/events_ships.xml" );
			streams.add(shipEventsStream);
			List<ShipEvent> shipEventList = dataParser.readShipEvents( shipEventsStream, "events_ships.xml" );

			log.info( "Reading Background Image Lists..." );
			log.debug( "Reading 'data/events_imageList.xml'" );
			InputStream imageListsStream = dataParser.getInputStream( "data/events_imageList.xml" );
			streams.add(imageListsStream);
			List<BackgroundImageList> imageLists = dataParser.readImageLists( imageListsStream );

			log.info( "Finished reading game resources." );

			generalAchievements = new ArrayList<Achievement>();
			for( Achievement ach : achievements )
				if ( ach.getShipId() == null )
					generalAchievements.add(ach);

			augments = new LinkedHashMap<String, AugBlueprint>();
			for ( AugBlueprint augment : blueprints.getAugBlueprint() )
				augments.put( augment.getId(), augment );

			drones = new LinkedHashMap<String, DroneBlueprint>();
			for ( DroneBlueprint drone : blueprints.getDroneBlueprint() )
				drones.put( drone.getId(), drone );

			systems = new LinkedHashMap<String, SystemBlueprint>();
			for ( SystemBlueprint system : blueprints.getSystemBlueprint() )
				systems.put( system.getId(), system );

			weapons = new LinkedHashMap<String, WeaponBlueprint>();
			for ( WeaponBlueprint weapon : blueprints.getWeaponBlueprint() )
				weapons.put( weapon.getId(), weapon );

			ships = new LinkedHashMap<String, ShipBlueprint>();
			for ( ShipBlueprint ship : blueprints.getShipBlueprint() )
				ships.put( ship.getId(), ship );

			autoShips = new LinkedHashMap<String, ShipBlueprint>();
			for ( ShipBlueprint ship : autoBlueprints.getShipBlueprint() )
				autoShips.put( ship.getId(), ship );

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
			for ( ShipBlueprint ship : playerShips ) {
				List<Achievement> shipAchs = new ArrayList<Achievement>();
				for ( Achievement ach : achievements )
					if ( ship.getId().equals( ach.getShipId() ) )
						shipAchs.add(ach);
				shipAchievements.put( ship, shipAchs );
			}

			// These'll populate as files are requested.
			shipLayouts = new HashMap<String, ShipLayout>();
			shipChassisMap = new HashMap<String, ShipChassis>();

			crewNamesMale = new ArrayList<CrewNameList.CrewName>();
			crewNamesFemale = new ArrayList<CrewNameList.CrewName>();
			for ( CrewNameList crewNameList : crewNameLists ) {
				if ( "male".equals( crewNameList.getSex() ) )
					crewNamesMale.addAll( crewNameList.getNames() );
				else
					crewNamesFemale.addAll( crewNameList.getNames() );
			}

			shipEvents = new LinkedHashMap<String, ShipEvent>();
			for ( ShipEvent shipEvent : shipEventList )
				shipEvents.put( shipEvent.getId(), shipEvent );

			backgroundImageLists = new LinkedHashMap<String, BackgroundImageList>();
			for ( BackgroundImageList imageList : imageLists )
				backgroundImageLists.put( imageList.getId(), imageList );
		}
		catch ( JAXBException e ) {
			meltdown = true;
			throw e;
		}
		catch ( IOException e ) {
			meltdown = true;
			throw e;
		}
		finally {
			for ( InputStream stream : streams ) {
				try {if ( stream != null ) stream.close();}
				catch ( IOException f ) {}
			}

			if ( meltdown ) this.close();
		}
	}

	public void close() {
		try {if (dataParser != null) dataParser.close();}
		catch ( IOException e ) {}

		try {if (resourceParser != null) resourceParser.close();}
		catch ( IOException e ) {}
	}
	
	public InputStream getDataInputStream( String innerPath ) throws IOException {
		return dataParser.getInputStream( innerPath );
	}
	
	public InputStream getResourceInputStream( String innerPath ) throws IOException {
		return resourceParser.getInputStream( innerPath );
	}
	
	public void unpackData( File extractDir ) throws IOException {
		dataParser.unpackDat( extractDir );
	}

	public void unpackResources( File extractDir ) throws IOException {
		resourceParser.unpackDat( extractDir );
	}
	
	public List<Achievement> getAchievements() {
		return achievements;
	}

	public AugBlueprint getAugment( String id ) {
		AugBlueprint result = augments.get(id);
		if ( result == null )
			log.error( "No AugBlueprint found for id: "+ id );
		return result;
	}

	public Map<String, AugBlueprint> getAugments() {
		return augments;
	}

	public DroneBlueprint getDrone( String id ) {
		DroneBlueprint result = drones.get(id);
		if ( result == null )
			log.error( "No DroneBlueprint found for id: "+ id );
		return result;
	}

	public Map<String, DroneBlueprint> getDrones() {
		return drones;
	}

	public SystemBlueprint getSystem( String id ) {
		SystemBlueprint result = systems.get(id);
		if ( result == null )
			log.error( "No SystemBlueprint found for id: "+ id );
		return result;
	}

	public WeaponBlueprint getWeapon( String id ) {
		WeaponBlueprint result = weapons.get(id);
		if ( result == null )
			log.error( "No WeaponBlueprint found for id: "+ id );
		return result;
	}

	public Map<String, WeaponBlueprint> getWeapons() {
		return weapons;
	}

	public ShipBlueprint getShip( String id ) {
		ShipBlueprint result = ships.get(id);
		if ( result == null )  // TODO: Auto ships might need their own method.
			result = autoShips.get(id);
		if ( result == null )
			log.error( "No ShipBlueprint found for id: "+ id );
		return result;
	}
	
	public Map<String, ShipBlueprint> getShips() {
		return ships;
	}

	public Map<String, ShipBlueprint> getAutoShips() {
		return autoShips;
	}

	public List<ShipBlueprint> getPlayerShips() {
		return playerShips;
	}
	
	public List<Achievement> getShipAchievements( ShipBlueprint ship ) {
		return shipAchievements.get(ship);
	}
	
	public List<Achievement> getGeneralAchievements() {
		return generalAchievements;
	}

	public ShipLayout getShipLayout( String id ) {
		ShipLayout result = shipLayouts.get(id);

		if ( result == null ) {  // Wasn't cached; try parsing it.
			InputStream in = null;
			try {
				in = getDataInputStream("data/"+ id +".txt");
				result = dataParser.readLayout(in);
				shipLayouts.put( id, result );
			}
			catch ( FileNotFoundException e ) {
				log.error( "No ShipLayout found for id: "+ id );
			}
			catch ( IOException e ) {
				log.error( "An error occurred while parsing ShipLayout: "+ id, e );
			}
			finally {
				try {if ( in != null ) in.close();}
				catch ( IOException f ) {}
			}
		}

		return result;
	}

	public ShipChassis getShipChassis( String id ) {
		ShipChassis result = shipChassisMap.get(id);

		if ( result == null ) {  // Wasn't cached; try parsing it.
			InputStream in = null;
			try {
				in = getDataInputStream( "data/"+ id +".xml" );
				result = dataParser.readChassis(in);
				shipChassisMap.put( id, result );
			}
			catch ( JAXBException e ) {
				log.error( "Parsing XML failed for ShipChassis id: "+ id );
			}
			catch ( FileNotFoundException e ) {
				log.error( "No ShipChassis found for id: "+ id );
			}
			catch ( IOException e ) {
				log.error( "An error occurred while parsing ShipChassis: "+ id, e );
			}
			finally {
				try {if ( in != null ) in.close();}
				catch ( IOException f ) {}
			}
		}

		return result;
	}

	/**
	 * Returns true (male) or false (female).
	 * All possible names have equal
	 * probability, which will skew the
	 * male-to-female ratio.
	 */
	public boolean getCrewSex() {
		int n = (int)(Math.random()*(crewNamesMale.size()+crewNamesFemale.size()));
		boolean result = (n < crewNamesMale.size());
		return result;
	}

	/**
	 * Returns a random name for a given sex.
	 */
	public String getCrewName( boolean isMale ) {
		List<CrewNameList.CrewName> crewNames = (isMale ? crewNamesMale : crewNamesFemale);
		int n = (int)(Math.random()*crewNames.size());
		return crewNames.get(n).name;
	}

	/**
	 * Returns an Event with a given id.
	 * All event xml files are searched.
	 *
	 * Events and EventLists share a namespace,
	 * so an id could belong to either.
	 */
	public FTLEvent getEventById( String id ) {
		for ( Map.Entry<String, Encounters> entry : events.entrySet() ) {
			FTLEvent tmpEvent = entry.getValue().getEventById(id);
			if ( tmpEvent != null ) return tmpEvent;
		}
		return null;
	}

	/**
	 * Returns an EventList with a given id.
	 * All event xml files are searched.
	 *
	 * Events and EventLists share a namespace,
	 * so an id could belong to either.
	 */
	public FTLEventList getEventListById( String id ) {
		for ( Map.Entry<String, Encounters> entry : events.entrySet() ) {
			FTLEventList tmpEventList = entry.getValue().getEventListById(id);
			if ( tmpEventList != null ) return tmpEventList;
		}
		return null;
	}

	/**
	 * Returns all Encounters objects, mapped to xml file names.
	 *
	 * Each can be queried for its FTLEvent or FTLEventList members.
	 */
	public Map<String, Encounters> getEncounters() {
		return events;
	}

	public ShipEvent getShipEventById( String id ) {
		ShipEvent result = shipEvents.get(id);
		if ( result == null )
			log.error( "No ShipEvent found for id: "+ id );
		return result;
	}

	public Map<String, ShipEvent> getShipEvents() {
		return shipEvents;
	}

	/**
	 * Returns all BackgroundImageList objects, mapped to ids.
	 *
	 * When unspecified, images are randomly chosen from the
	 * "PLANET" and "BACKGROUND" lists for sprites and backgrounds.
	 */
	public Map<String, BackgroundImageList> getBackgroundImageLists() {
		return backgroundImageLists;
	}
}
