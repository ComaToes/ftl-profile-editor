package net.blerf.ftl.parser;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import org.xml.sax.SAXParseException;

import net.vhati.ftldat.FTLDat;
import net.vhati.ftldat.FTLDat.FolderPack;
import net.vhati.ftldat.FTLDat.FTLPack;

import net.blerf.ftl.model.ShipLayout;
import net.blerf.ftl.parser.TextUtilities;
import net.blerf.ftl.xml.Achievement;
import net.blerf.ftl.xml.Achievements;
import net.blerf.ftl.xml.BackgroundImageList;
import net.blerf.ftl.xml.BackgroundImageLists;
import net.blerf.ftl.xml.Blueprints;
import net.blerf.ftl.xml.CrewNameList;
import net.blerf.ftl.xml.CrewNameLists;
import net.blerf.ftl.xml.Encounters;
import net.blerf.ftl.xml.ShipBlueprint;
import net.blerf.ftl.xml.ShipChassis;
import net.blerf.ftl.xml.ShipEvent;
import net.blerf.ftl.xml.ShipEvents;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class DatParser implements Closeable {

	private static final Logger log = LogManager.getLogger(DatParser.class);

	private File datFile = null;
	private FTLDat.FTLPack datP = null;


	public DatParser( File datFile ) throws IOException {
		this.datFile = datFile;
		this.datP = new FTLDat.FTLPack( datFile, false );
	}


	public List<Achievement> readAchievements( InputStream stream, String fileName ) throws IOException, JAXBException {
		log.trace( "Reading achievements XML" );

		// Need to clean invalid XML and comments before JAXB parsing

		String streamText = TextUtilities.decodeText( stream, fileName ).text;
		BufferedReader in = new BufferedReader( new StringReader(streamText) );
		StringBuilder sb = new StringBuilder();
		String line;
		boolean comment = false;

		while( (line = in.readLine()) != null ) {
			line = line.replaceAll( "<[?]xml [^>]*[?]>", "" );
			line = line.replaceAll( "<!--.*?-->", "" );
			line = line.replaceAll( "<desc>([^<]*)</name>", "<desc>$1</desc>" );

			// Remove multiline comments
			if (comment && line.contains("-->"))
				comment = false;
			else if (line.contains("<!--"))
				comment = true;
			else if (!comment)
				sb.append(line).append("\n");
		}
		in.close();

		// XML has multiple root nodes so need to wrap.
		sb.insert( 0, "<achievements>\n" );
		sb.append( "</achievements>\n" );

		// Add the xml header.
		sb.insert( 0, "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" );

		// Parse cleaned XML
		Achievements ach = (Achievements)unmarshalFromSequence( Achievements.class, sb );

		return ach.getAchievements();
	}


	public Blueprints readBlueprints( InputStream stream, String fileName ) throws IOException, JAXBException {
		log.trace( "Reading blueprints XML" );

		// Need to clean invalid XML and comments before JAXB parsing

		String streamText = TextUtilities.decodeText( stream, fileName ).text;
		BufferedReader in = new BufferedReader( new StringReader(streamText) );
		StringBuilder sb = new StringBuilder();
		String line;
		boolean comment = false;
		String ptn; Pattern p; Matcher m;

		while( (line = in.readLine()) != null ) {
			line = line.replaceAll( "<[?]xml [^>]*[?]>", "" );
			line = line.replaceAll( "<!--.*?-->", "" );

			if ( "blueprints.xml".equals(fileName) ) {
				// blueprints.xml: PLAYER_SHIP_CRYSTAL_2 shipBlueprint (FTL 1.03.1)
				line = line.replaceAll( "^<!-- sardonyx$", "" );

				// blueprints.xml: LONG_ELITE_MED shipBlueprint (FTL 1.03.1)
				// blueprints.xml: LONG_ELITE_HARD shipBlueprint (FTL 1.03.1)
				line = line.replaceAll( " img=\"rebel_long_hard\"", " img=\"rebel_long_elite\"" );

				// blueprints.xml: PLAYER_SHIP_STEALTH shipBlueprint (FTL 1.03.1)
				// blueprints.xml: PLAYER_SHIP_ROCK shipBlueprint (FTL 1.03.1)
				// blueprints.xml: PLAYER_SHIP_ROCK_2 shipBlueprint (FTL 1.03.1)
				line = line.replaceAll( "\"img=", "\" img=" );

				// blueprints.xml: LASER_BURST_5 weaponBlueprint (FTL 1.02.6)
				line = line.replaceAll( "<tooltip>([^<]*)</desc>(-->)?", "<tooltip>$1</tooltip>" );

				// blueprints.xml: oxygen, teleporter, cloaking systemBlueprint (FTL 1.02.6)
				// blueprints.xml: pilot, medbay, shields systemBlueprint (FTL 1.02.6)
				// blueprints.xml: engines, weapons, drones systemBlueprint (FTL 1.02.6)
				// blueprints.xml: sensors, doors systemBlueprint (FTL 1.02.6)
				line = line.replaceAll( "<title>([^<]*)</type>", "<title>$1</title>" );

				// blueprints.xml: fuel, drones, missiles itemBlueprint (FTL 1.02.6)
				line = line.replaceAll( "<title>([^<]*)</ship>", "<title>$1</title>" );

				// blueprints.xml: LASER_HULL_1 weaponBlueprint (FTL 1.02.6)
				// blueprints.xml: LASER_HULL_2 weaponBlueprint (FTL 1.02.6)
				line = line.replaceAll( "<speed>([^<]*)</image>", "<speed>$1</speed>" );  // Error in weaponBlueprint.
			}

			if ( "autoBlueprints.xml".equals(fileName) ) {
				// autoBlueprints.xml: JELLY_TRUFFLE (FTL 1.03.1)
				// autoBlueprints.xml: ROCK_SCOUT, ROCK_FIGHT, ROCK_ASSAULT, ROCK_ASSAULT_ELITE (FTL 1.03.1)
				// autoBlueprints.xml: MANTIS_SCOUT, MANTIS_FIGHTER, MANTIS_BOMBER (FTL 1.03.1)
				// autoBlueprints.xml: FED_SCOUT, FED_BOMBER (FTL 1.03.1)
				// autoBlueprints.xml: CIRCLE_SCOUT, CIRCLE_BOMBER (FTL 1.03.1)
				// autoBlueprints.xml: ZOLTAN_FIGHTER, ZOLTAN_BOMBER, ZOLTAN_PEACE (FTL 1.03.1)
				// autoBlueprints.xml: CRYSTAL_SCOUT, CRYSTAL_BOMBER (FTL 1.03.1)
				line = line.replaceAll( "\"max=", "\" max=" );

				// autoBlueprints.xml: JELLY_CROISSANT (FTL 1.03.1)
				line = line.replaceAll( "\"room=", "\" room=" );
			}

			// Remove multiline comments
			if ( comment && line.contains( "-->" ) )
				comment = false;
			else if ( line.contains( "<!--" ) )
				comment = true;
			else if ( !comment )
				sb.append(line).append( "\n" );
		}
		in.close();

		// XML has multiple root nodes so need to wrap.
		sb.insert( 0, "<blueprints>\n" );
		sb.append( "</blueprints>\n" );

		// Add the xml header.
		sb.insert( 0, "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" );

		if ( "blueprints.xml".equals(fileName) ) {
			// blueprints.xml: PLAYER_SHIP_HARD_2 shipBlueprint (FTL 1.03.1)
			ptn = "";
			ptn += "(<shields *(?: [^>]*)?>\\s*";
			ptn +=   "<slot *(?: [^>]*)?>\\s*";
			ptn +=     "(?:<direction>[^<]*</direction>\\s*)?";
			ptn +=     "(?:<number>[^<]*</number>\\s*)?";
			ptn +=   "</slot>\\s*)";
			ptn += "</slot>"; // Wrong closing tag.

			p = Pattern.compile(ptn);
			m = p.matcher(sb);
			if ( m.find() ) {
				sb.replace( m.start(), m.end(), m.group(1)+"</shields>" );
				m.reset();
			}

			// blueprints.xml: SYSTEM_CASING augBlueprint (FTL 1.02.6)
			ptn = "";
			ptn += "\\s*<title>Reinforced System Casing</title>"; // Extra title.
			ptn += "(\\s*<title>Titanium System Casing</title>)";

			p = Pattern.compile(ptn);
			m = p.matcher(sb);
			if ( m.find() ) {
				sb.replace( m.start(), m.end(), m.group(1) );
				m.reset();
			}
		}

		if ( "blueprints.xml".equals(fileName) || "autoBlueprints.xml".equals(fileName) ) {
			// blueprints.xml: DEFAULT shipBlueprint (FTL 1.03.1)
			// autoBlueprints.xml: AUTO_BASIC shipBlueprint (FTL 1.03.1)
			// autoBlueprints.xml: AUTO_ASSAULT shipBlueprint (FTL 1.03.1)
			ptn = "";
			ptn += "(<shipBlueprint *(?: [^>]*)?>\\s*";
			ptn +=   "<class>[^<]*</class>\\s*";
			ptn +=   "<systemList *(?: [^>]*)?>\\s*";
			ptn +=      "(?:<[a-zA-Z]+ *(?: [^>]*)?/>\\s*)*";
			ptn +=   "</systemList>\\s*";
			ptn +=   "(?:<droneList *(?: [^>]*)?>\\s*";
			ptn +=      "(?:<[a-zA-Z]+ *(?: [^>]*)?/>\\s*)*";
			ptn +=   "</droneList>\\s*)?";
			ptn +=   "(?:<weaponList *(?: [^>]*)?>\\s*";
			ptn +=      "(?:<[a-zA-Z]+ *(?: [^>]*)?/>\\s*)*";
			ptn +=   "</weaponList>\\s*)?";
			ptn +=   "(?:<[a-zA-Z]+ *(?: [^>]*)?/>\\s*)*)";
			ptn += "</ship>"; // Wrong closing tag.

			p = Pattern.compile(ptn);
			m = p.matcher(sb);
			while ( m.find() ) {
				sb.replace( m.start(), m.end(), m.group(1)+"</shipBlueprint>" );
				m.reset();
			}
		}

		// Parse cleaned XML
		Blueprints bps = (Blueprints)unmarshalFromSequence( Blueprints.class, sb.toString() );

		return bps;
	}


	public ShipLayout readLayout( InputStream stream, String fileName ) throws IOException {

		String streamText = TextUtilities.decodeText( stream, fileName ).text;
		BufferedReader in = new BufferedReader( new StringReader(streamText) );

		ShipLayout shipLayout = new ShipLayout();

		String line = null;
		boolean firstLine = true;
		boolean comment = false;

		while ( (line = in.readLine()) != null ) {
			if ( line.length() == 0 ) continue;

			if ( line.equals( "X_OFFSET" ) ) {
				shipLayout.setOffsetX( Integer.parseInt( in.readLine() ) );
			}
			else if ( line.equals( "Y_OFFSET" ) ) {
				shipLayout.setOffsetY( Integer.parseInt( in.readLine() ) );
			}
			else if ( line.equals( "HORIZONTAL" ) ) {
				shipLayout.setHorizontal( Integer.parseInt( in.readLine() ) );
			}
			else if ( line.equals( "VERTICAL" ) ) {
				shipLayout.setVertical( Integer.parseInt( in.readLine() ) );
			}
			else if ( line.equals( "ELLIPSE" ) ) {
				int w = Integer.parseInt( in.readLine() );
				int h = Integer.parseInt( in.readLine() );
				int x = Integer.parseInt( in.readLine() );
				int y = Integer.parseInt( in.readLine() );
				shipLayout.setShieldEllipse( w, h, x, y );
			}
			else if ( line.equals( "ROOM" ) ) {
				int roomId = Integer.parseInt( in.readLine() );
				int alpha = Integer.parseInt( in.readLine() );
				int beta = Integer.parseInt( in.readLine() );
				int hSquares = Integer.parseInt( in.readLine() );
				int vSquares = Integer.parseInt( in.readLine() );
				shipLayout.setRoom( roomId, alpha, beta, hSquares, vSquares );
			}
			else if ( line.equals( "DOOR" ) ) {
				int wallX = Integer.parseInt( in.readLine() );
				int wallY = Integer.parseInt( in.readLine() );
				int roomIdA = Integer.parseInt( in.readLine() );
				int roomIdB = Integer.parseInt( in.readLine() );
				int vertical = Integer.parseInt( in.readLine() );
				shipLayout.setDoor( wallX, wallY, vertical, roomIdA, roomIdB );
			}
		}
		return shipLayout;
	}


	public ShipChassis readChassis( InputStream stream, String fileName ) throws IOException, JAXBException {
		log.trace( "Reading ship chassis XML" );

		// Need to clean invalid XML and comments before JAXB parsing

		String streamText = TextUtilities.decodeText( stream, fileName ).text;
		BufferedReader in = new BufferedReader( new StringReader(streamText) );
		StringBuilder sb = new StringBuilder();
		String line;

		boolean comment = false;
		while( (line = in.readLine()) != null ) {
			line = line.replaceAll( "<[?]xml [^>]*[?]>", "" );
			line = line.replaceAll( "<!--.*?-->", "" );
			line = line.replaceAll( "<(/?)gib[0-9]*>", "<$1gib>" );

			// Remove multiline comments
			if ( comment && line.contains( "-->" ) )
				comment = false;
			else if ( line.contains( "<!--" ) )
				comment = true;
			else if ( !comment )
				sb.append(line).append( "\n" );
		}
		in.close();

		// XML has multiple root nodes so need to wrap.
		sb.insert( 0, "<shipChassis>\n" );
		sb.append( "</shipChassis>\n" );

		// Add the xml header.
		sb.insert( 0, "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" );

		// Parse cleaned XML
		ShipChassis sch = (ShipChassis)unmarshalFromSequence( ShipChassis.class, sb.toString() );

		return sch;
	}

	public List<CrewNameList> readCrewNames( InputStream stream, String fileName ) throws IOException, JAXBException {
		log.trace( "Reading crew name list XML" );

		// Need to clean invalid XML and comments before JAXB parsing

		String streamText = TextUtilities.decodeText( stream, fileName ).text;
		BufferedReader in = new BufferedReader( new StringReader(streamText) );
		StringBuilder sb = new StringBuilder();
		String line;
		boolean comment = false;

		while( (line = in.readLine()) != null ) {
			line = line.replaceAll( "<[?]xml [^>]*[?]>", "" );
			line = line.replaceAll( "<!--.*?-->", "" );

			// Remove multiline comments
			if ( comment && line.contains( "-->" ) )
				comment = false;
			else if ( line.contains("<!--") )
				comment = true;
			else if ( !comment )
				sb.append(line).append( "\n" );
		}
		in.close();

		// XML has multiple root nodes so need to wrap.
		sb.insert( 0, "<nameLists>\n" );
		sb.append( "</nameLists>\n" );

		// Add the xml header.
		sb.insert( 0, "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" );

		// Parse cleaned XML
		CrewNameLists cnl = (CrewNameLists)unmarshalFromSequence( CrewNameLists.class, sb.toString() );

		return cnl.getCrewNameLists();
	}


	public Encounters readEvents( InputStream stream, String fileName ) throws IOException, JAXBException {
		log.trace( "Reading events XML" );

		// Need to clean invalid XML and comments before JAXB parsing

		String streamText = TextUtilities.decodeText( stream, fileName ).text;
		BufferedReader in = new BufferedReader( new StringReader(streamText) );
		StringBuilder sb = new StringBuilder();
		String line;
		boolean comment = false;
		String ptn; Pattern p; Matcher m;

		while( (line = in.readLine()) != null ) {
			line = line.replaceAll( "<[?]xml [^>]*[?]>", "" );
			line = line.replaceAll( "<!--.*?-->", "" );

			if ( "events.xml".equals(fileName) || "events_nebula.xml".equals(fileName) ) {
				// events.xml: PIRATE_CIVILIAN_BEACON (FTL 1.03.1)
				// events_nebula.xml: NEBULA_REBEL_UNDETECTED (FTL 1.03.1)
				line = line.replaceAll( " hidden=\"true\" hidden=\"true\"", " hidden=\"true\"" );
			}

			if ( "events_engi.xml".equals(fileName) || "events_mantis.xml".equals(fileName) || "events_crystal.xml".equals(fileName) ) {
				// events_engi.xml: DISTRESS_ENGI_REACTOR_LIST1 (FTL 1.03.1)
				// events_mantis.xml: MANTIS_NAMED_THIEF_DEFEAT (FTL 1.03.1)
				// events_crystal.xml: CRYSTAL_CACHE_BREAK (FTL 1.01)
				line = line.replaceAll( "(.*<text *( [^\\/>]*)?>[^<]*)</event>.*", "$1</text>" );
			}

			// Remove multiline comments
			if ( comment && line.contains( "-->" ) )
				comment = false;
			else if ( line.contains( "<!--" ) )
				comment = true;
			else if ( !comment )
				sb.append(line).append( "\n" );
		}
		in.close();

		// XML has multiple root nodes so need to wrap.
		sb.insert( 0, "<events>\n" );
		sb.append( "</events>\n" );

		// Add the xml header.
		sb.insert( 0, "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" );

		if ( "events.xml".equals(fileName) ) {
			// events.xml: STORE_TEXT (FTL 1.03.1)
			ptn = "";
			ptn += "(<textList *(?: [^>]*)?>\\s*";
			ptn += "(?:<text *(?: [^>]*)?>[^<]*</text>\\s*)*)";
			ptn += "</text>"; // Wrong closing tag.

			p = Pattern.compile(ptn);
			m = p.matcher(sb);
			if ( m.find() ) {
				sb.replace( m.start(), m.end(), m.group(1)+"</textList>" );
				m.reset();
			}
		}

		if ( "events_fuel.xml".equals(fileName) ) {
			// events_fuel.xml: FUEL_ON_MANTIS_ATTACK (FTL 1.03.1)
			// events_fuel.xml: FUEL_ON_REBEL_ATTACK (FTL 1.03.1)
			ptn = "";
			ptn += "(<textList *(?: [^>]*)?>\\s*";
			ptn += "(?:<text *(?: [^>]*)?>[^<]*</text>\\s*)*)";
			ptn += "</event>"; // Wrong closing tag.

			p = Pattern.compile(ptn);
			m = p.matcher(sb);
			while ( m.find() ) {
				sb.replace( m.start(), m.end(), m.group(1)+"</textList>" );
				m.reset();
			}
		}

		if ( "events_engi.xml".equals(fileName) ) {
			// events_engi.xml: DISTRESS_ENGI_REACTOR (FTL 1.03.1)
			ptn = "";
			ptn += "(<event *(?: [^>]*)?>\\s*";
			ptn +=   "<text>[^<]*</text>\\s*";
			ptn +=   "<distressBeacon *(?: [^>]*)?/>\\s*";
			ptn +=   "<choice *(?: [^>]*)?>\\s*";
			ptn +=     "<text>[^<]*</text>\\s*";
			ptn +=     "<event *(?: [^>]*)?/>\\s*";
			ptn +=   "</choice>\\s*";
			ptn +=   "<choice *(?: [^>]*)?>\\s*";
			ptn +=     "<text>[^<]*</text>\\s*";
			ptn +=     "<event>\\s*";
			ptn +=       "<text>[^<]*</text>\\s*";
			ptn +=     "</event>\\s*";
			ptn +=   "</choice>\\s*";
			ptn +=   "<choice *(?: [^>]*)?>\\s*";
			ptn +=     "<text>[^<]*</text>\\s*";
			ptn +=     "<event *(?: [^>]*)?/>\\s*";
			ptn +=   "</choice>\\s*";
			ptn +=   "<choice *(?: [^>]*)?>\\s*";
			ptn +=     "<text>[^<]*</text>\\s*";
			ptn +=     "<event *(?: [^>]*)?/>\\s*";
			ptn +=   "</choice>\\s*)";
			ptn += "</choice>"; // Wrong closing tag.

			p = Pattern.compile(ptn);
			m = p.matcher(sb);
			if ( m.find() ) {
				sb.replace( m.start(), m.end(), m.group(1)+"</event>" );
				m.reset();
			}

			// events_engi: ENGI_UNLOCK_2REAL_SURRENDER (FTL 1.03.1)
			ptn = "";
			ptn += "(<event>\\s*";
			ptn +=   "<text>[^<]*</text>\\s*";
			ptn +=   "<quest *(?: [^>]*)?/>\\s*";
			ptn +=   "<choice>\\s*";
			ptn +=     "<text>[^<]*</text>\\s*";
			ptn +=     "<event>\\s*";
			ptn +=       "<text>[^<]*</text>\\s*";
			ptn +=       "<ship *(?: [^>]*)?/>\\s*";
			ptn +=     "</event>\n)";
			//        </choice> tag is missing.
			ptn += "(\\s*</event>)";

			p = Pattern.compile(ptn);
			m = p.matcher(sb);
			if ( m.find() ) {
				sb.replace( m.start(), m.end(), m.group(1)+"</choice>\n"+ m.group(2) );
				m.reset();
			}
		}

		if ( "events_mantis.xml".equals(fileName) ) {
			// events_mantis.xml: MANTIS_NAMED_THIEF (FTL 1.03.1)
			ptn = "";
			ptn += "(<event *(?: [^>]*)?>\\s*";
			ptn +=   "<ship *(?: [^>]*)?/>\\s*";
			ptn +=   "<text>[^<]*</text>\\s*";
			ptn +=   "<choice *(?: [^>]*)?>\\s*";
			ptn +=     "<text>[^<]*</text>\\s*";
			ptn +=     "<event>\\s*";
			ptn +=       "<text>[^<]*</text>\\s*";
			ptn +=       "<ship *(?: [^>]*)?/>\\s*";
			ptn +=     "</event>\\s*";
			ptn +=   "</choice>\\s*";
			ptn +=   "<choice *(?: [^>]*)?>\\s*";
			ptn +=     "<text>[^<]*</text>\\s*";
			ptn +=     "<event>\\s*";
			ptn +=       "<ship *(?: [^>]*)?/>\\s*";
			ptn +=     "</event>\\s*";
			ptn +=   "</choice>\\s*)";
			ptn += "</text>"; // Wrong closing tag.

			p = Pattern.compile(ptn);
			m = p.matcher(sb);
			if ( m.find() ) {
				sb.replace( m.start(), m.end(), m.group(1)+"</event>" );
				m.reset();
			}
		}

		if ( "events_slug.xml".equals(fileName) ) {
			// events_slug.xml: NEBULA_SLUG_CHOOSE_DEATH (FTL 1.03.1)
			ptn = "";
			ptn += "(<choice *(?: [^>]*)?>\\s*";
			ptn +=   "<text>[^<]*</text>\\s*";
			ptn +=   "<event>\\s*";
			ptn +=     "<text>[^<]*</text>\\s*";
			ptn +=     "<ship *(?: [^>]*)?/>\\s*";
			ptn +=     "<status *(?: [^>]*)?/>\\s*";
			ptn +=   "</event>\\s*)";
			ptn += "</event>"; // Wrong closing tag.

			p = Pattern.compile(ptn);
			m = p.matcher(sb);
			if ( m.find() ) {
				sb.replace( m.start(), m.end(), m.group(1)+"</choice>\n" );
				m.reset();
			}
		}

		if ( "events_zoltan.xml".equals(fileName) ) {
			// events_zoltan.xml: ZOLTAN_LIFERAFT_HIRE (FTL 1.03.1)
			ptn = "";
			ptn += "(<eventList *(?: [^>]*)?>\\s*";
			ptn +=   "<event>\\s*";
			ptn +=     "<text>[^<]*</text>\\s*";
			ptn +=   "</event>\\s*";
			ptn +=   "<event>\\s*";
			ptn +=     "<text>[^<]*</text>\\s*";
			ptn +=     "<item_modify>\\s*";
			ptn +=       "<item *(?: [^>]*)?/>\\s*";
			ptn +=     "</item_modify>\\s*";
			ptn +=     "<crewMember *(?: [^>]*)?/>\\s*";
			ptn +=   "</event>\\s*)";
			ptn += "</event>"; // Wrong closing tag.

			p = Pattern.compile(ptn);
			m = p.matcher(sb);
			if ( m.find() ) {
				sb.replace( m.start(), m.end(), m.group(1)+"</eventList>\n" );
				m.reset();
			}
		}

		if ( "events_pirate.xml".equals(fileName) ) {
			// events_pirate.xml: PIRATE_ASTEROID (FTL 1.03.1)
			ptn = "";
			ptn += "(<event *(?: [^>]*)?>\\s*";
			ptn +=   "<img *(?: [^>]*)?/>\\s*";
			ptn +=   "<environment *(?: [^>]*)?/>\\s*";
			ptn +=   "<text>[^<]*</text>\\s*";
			ptn +=   "<ship *(?: [^>]*)?/>\\s*";
			ptn +=   "<choice>\\s*";
			ptn +=     "<text>[^<]*</text>\\s*";
			ptn +=     "<event *(?: [^>]*)?/>\n)";
			//        </choice> tag is missing.
			ptn += "(\\s*</event>)";

			p = Pattern.compile(ptn);
			m = p.matcher(sb);
			if ( m.find() ) {
				sb.replace( m.start(), m.end(), m.group(1)+"</choice>\n"+ m.group(2) );
				m.reset();
			}
		}

		// Parse cleaned XML
		Encounters evts = (Encounters)unmarshalFromSequence( Encounters.class, sb );

		return evts;
	}


	public List<ShipEvent> readShipEvents( InputStream stream, String fileName ) throws IOException, JAXBException {
		log.trace( "Reading ship events XML" );

		// Need to clean invalid XML and comments before JAXB parsing

		String streamText = TextUtilities.decodeText( stream, fileName ).text;
		BufferedReader in = new BufferedReader( new StringReader(streamText) );
		StringBuilder sb = new StringBuilder();
		String line;
		boolean comment = false;
		String ptn; Pattern p; Matcher m;

		while( (line = in.readLine()) != null ) {
			line = line.replaceAll( "<[?]xml [^>]*[?]>", "" );
			line = line.replaceAll( "<!--.*?-->", "" );

			// Remove multiline comments
			if ( comment && line.contains( "-->" ) )
				comment = false;
			else if ( line.contains( "<!--" ) )
				comment = true;
			else if ( !comment )
				sb.append(line).append( "\n" );
		}
		in.close();

		// XML has multiple root nodes so need to wrap.
		sb.insert( 0, "<shipEvents>\n" );
		sb.append( "</shipEvents>\n" );

		// Add the xml header.
		sb.insert( 0, "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" );

		if ( "events_ships.xml".equals(fileName) ) {
			// events_ships.xml: REBEL_AUTO_HACKSHIELDS (FTL 1.03.1)
			ptn = "";
			ptn += "(<deadCrew>\\s*";
			ptn +=   "<text>[^<]*</text>\\s*";
			ptn +=   "<autoReward *(?: [^>]*)?>[^<]*</autoReward>\\s*";
			ptn +=   "<status *(?: [^>]*)?/>\\s*)";
			ptn += "</destroyed>"; // Wrong closing tag.

			p = Pattern.compile(ptn);
			m = p.matcher(sb);
			if ( m.find() ) {
				sb.replace( m.start(), m.end(), m.group(1)+"\n</deadCrew>" );
				m.reset();
			}
		}

		// Parse cleaned XML
		ShipEvents shvts = (ShipEvents)unmarshalFromSequence( ShipEvents.class, sb.toString() );

		return shvts.getShipEvents();
	}


	public List<BackgroundImageList> readImageLists( InputStream stream, String fileName ) throws IOException, JAXBException {
		log.trace( "Reading background images XML" );

		// Need to clean invalid XML and comments before JAXB parsing

		String streamText = TextUtilities.decodeText( stream, fileName ).text;
		BufferedReader in = new BufferedReader( new StringReader(streamText) );
		StringBuilder sb = new StringBuilder();
		String line;
		boolean comment = false;

		while( (line = in.readLine()) != null ) {
			line = line.replaceAll( "<[?]xml [^>]*[?]>", "" );
			line = line.replaceAll( "<!--.*?-->", "" );

			// Remove multiline comments
			if ( comment && line.contains( "-->" ) )
				comment = false;
			else if ( line.contains( "<!--" ) )
				comment = true;
			else if ( !comment )
				sb.append(line).append( "\n" );
		}
		in.close();

		// XML has multiple root nodes so need to wrap.
		sb.insert( 0, "<imageLists>\n" );
		sb.append( "</imageLists>\n" );

		// Add the xml header.
		sb.insert( 0, "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" );

		// Parse cleaned XML
		BackgroundImageLists imgs = (BackgroundImageLists)unmarshalFromSequence( BackgroundImageLists.class, sb.toString() );

		return imgs.getImageLists();
	}


	/**
	 * Parse XML from a CharSequence into an arbitrary class.
	 * Besides throwing the usual exception, the logger will
	 * print what the invalid line was.
	 */
	private Object unmarshalFromSequence( Class c, CharSequence seq ) throws JAXBException {
		Object result = null;
		try {
			JAXBContext jc = JAXBContext.newInstance(c);
			Unmarshaller u = jc.createUnmarshaller();
			result = u.unmarshal( new StreamSource(new StringReader(seq.toString())) );
		}
		catch ( JAXBException e ) {
			log.debug( "Error during xml parsing. Dumping document for review..." );
			log.debug(seq); // Dump all the xml to the log.

			Throwable linkedException = e.getLinkedException();
			if ( linkedException instanceof SAXParseException ) {
				// Get the 1-based line number where the problem was.
				int exLineNum = ((SAXParseException)linkedException).getLineNumber();

				String badLine = "";
				try { badLine = getLineFromSequence( seq, exLineNum-1 ); }
				catch (IndexOutOfBoundsException f) {}

				log.error( c.getSimpleName() +" parsing failed at line "+ exLineNum +" (1-based) of xml: "+ badLine );
			}
			throw e;
		}

		return result;
	}

	/**
	 * Returns a specific line from a CharSequence.
	 *
	 * @param seq a sequence to search
	 * @param lineNum a 0-based line number
	 * @throws IndexOutOfBoundsException if lineNum is greater
	 *         than the total available lines in seq, or negative.
	 */
	private String getLineFromSequence( CharSequence seq, int lineNum ) throws IndexOutOfBoundsException {
		if ( lineNum < 0 )
			throw new IndexOutOfBoundsException( "Attempted to get a negative line ("+ lineNum +") from a char sequence" );

		int charCount = seq.length();
		int currentLineNum = 0;
		int prevBreak = -1;
		int c = 0;
		for (; c < charCount; c++) {
			if ( seq.charAt(c) == '\n' ) {
				if ( currentLineNum == lineNum ) {
					break;
				} else {
					prevBreak = c;
					currentLineNum++;
				}
			}
		}
		if ( currentLineNum != lineNum )
			throw new IndexOutOfBoundsException( "Attempted to get line "+ lineNum +" (0-based) from a char sequence but only "+ currentLineNum +" lines were present" );

		return seq.subSequence( prevBreak+1, c ).toString();
	}


	public InputStream getInputStream(String innerPath) throws FileNotFoundException, IOException {
		return datP.getInputStream( innerPath );
	}


	public void unpackDat( File extractDir ) throws IOException {
		log.trace( "Unpacking dat file " + datFile.getPath() + " into " + extractDir.getPath() );


		FTLDat.FolderPack dstP = null;
		InputStream is = null;
		try {
			if ( !extractDir.exists() ) extractDir.mkdirs();

			dstP = new FTLDat.FolderPack( extractDir );

			List<String> innerPaths = datP.list();
			for ( String innerPath : innerPaths ) {
				if ( dstP.contains( innerPath ) ) {
					log.info( "While extracting resources, this file was overwritten: "+ innerPath );
					dstP.remove( innerPath );
				}
				is = datP.getInputStream( innerPath );
				dstP.add( innerPath, is );
			}
		}
		finally {
			try {if ( is != null ) is.close();}
			catch ( IOException ex ) {}

			try {if ( dstP != null ) dstP.close();}
			catch ( IOException ex ) {}
		}
	}

	public void close() throws IOException {
		if ( datP != null ) datP.close();
	}
}
