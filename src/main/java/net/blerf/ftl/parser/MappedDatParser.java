package net.blerf.ftl.parser;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.blerf.ftl.model.ShipLayout;
import net.blerf.ftl.xml.Achievement;
import net.blerf.ftl.xml.Achievements;
import net.blerf.ftl.xml.Blueprints;
import net.blerf.ftl.xml.CrewNameList;
import net.blerf.ftl.xml.CrewNameLists;
import net.blerf.ftl.xml.Encounters;
import net.blerf.ftl.xml.FTLEvent;
import net.blerf.ftl.xml.FTLEventList;
import net.blerf.ftl.xml.ShipBlueprint;
import net.blerf.ftl.xml.ShipChassis;


public class MappedDatParser extends Parser implements Closeable {

	private static final Logger log = LogManager.getLogger(MappedDatParser.class);
	private static final String BOM_UTF8 = "\uFEFF";

	private HashMap<String,InnerFileInfo> innerFilesMap = new HashMap<String,InnerFileInfo>();
	private File datFile = null;
	private RandomAccessFile randomDatFile = null;

	public MappedDatParser(File datFile) throws IOException {
		this.datFile = datFile;

		FileInputStream in = null;
		try {
			in = new FileInputStream(datFile);

			int headerSize = readInt(in);
			int[] header = new int[headerSize];
			for (int i = 0; i < header.length; i++) {
				header[i] = readInt(in);
			}
			for (int i = 0; i < header.length && header[i] != 0; i++) {
				in.getChannel().position(header[i]);

				long dataSize = (long)readInt(in);
				String innerPath = readString(in);
				long dataOffset = in.getChannel().position();

				InnerFileInfo info = new InnerFileInfo(dataOffset, dataSize);
				innerFilesMap.put(innerPath, info);
			}
			in.close();

			randomDatFile = new RandomAccessFile(datFile, "r");
		}
		catch (IOException e) {
			try {if (randomDatFile != null) randomDatFile.close();}
			catch (IOException f) {}
			throw e;
		}
		finally {
			try {if (in != null) in.close();}
			catch (IOException f) {}
		}
	}

	public List<Achievement> readAchievements(InputStream stream) throws IOException, JAXBException {
		log.trace("Reading achievements XML");

		// Need to clean invalid XML and comments before JAXB parsing

		BufferedReader in = new BufferedReader(new InputStreamReader(stream, "UTF8"));
		StringBuilder sb = new StringBuilder();
		String line;
		boolean comment = false;

		while( (line = in.readLine()) != null ) {
			line = line.replaceAll("<[?]xml [^>]*[?]>", "");
			line = line.replaceAll("<!--.*?-->", "");
			line = line.replaceAll("<desc>([^<]*)</name>", "<desc>$1</desc>");

			// Remove multiline comments
			if (comment && line.contains("-->"))
				comment = false;
			else if (line.contains("<!--"))
				comment = true;
			else if (!comment)
				sb.append(line).append("\n");
		}
		in.close();

		if ( sb.substring(0, BOM_UTF8.length()).equals(BOM_UTF8) )
			sb.replace(0, BOM_UTF8.length(), "");

		// XML has multiple root nodes so need to wrap.
		sb.insert(0, "<achievements>\n");
		sb.append("</achievements>\n");

		// Add the xml header.
		sb.insert(0, "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");

		// Parse cleaned XML
		Achievements ach = (Achievements)unmarshalFromSequence( Achievements.class, sb );

		return ach.getAchievements();
	}

	public Blueprints readBlueprints(InputStream stream) throws IOException, JAXBException {
		log.trace("Reading blueprints XML");

		// Need to clean invalid XML and comments before JAXB parsing

		BufferedReader in = new BufferedReader(new InputStreamReader(stream, "UTF8"));
		StringBuilder sb = new StringBuilder();
		String line;
		boolean comment = false;
		String ptn; Pattern p; Matcher m;

		while( (line = in.readLine()) != null ) {
			line = line.replaceAll("<[?]xml [^>]*[?]>", "");
			line = line.replaceAll("<!--.*?-->", "");

			// blueprints.xml
			line = line.replaceAll("^<!-- sardonyx$", "");  // Error above one shipBlueprint.
			line = line.replaceAll("<title>([^<]*)</[^>]*>", "<title>$1</title>");  // Error in systemBlueprint and itemBlueprint.
			line = line.replaceAll("<tooltip>([^<]*)</[^>]*>(-->)?", "<tooltip>$1</tooltip>");  // Error in weaponBlueprint.
			line = line.replaceAll("<speed>([^<]*)</[^>]*>", "<speed>$1</speed>");  // Error in weaponBlueprint.
			line = line.replaceAll("\"img=", "\" img=");  // ahhhh.
			line = line.replaceAll("</ship>", "</shipBlueprint>");  // Error in one shipBlueprint.
			line = line.replaceAll(" img=\"rebel_long_hard\"", " img=\"rebel_long_elite\"");  // Error in two shipBlueprints.

			// autoBlueprints.xml
			line = line.replaceAll("\"max=", "\" max=");  // ahhhh
			line = line.replaceAll("\"room=", "\" room=");  // ahhhh

			// Remove multiline comments
			if (comment && line.contains("-->"))
				comment = false;
			else if (line.contains("<!--"))
				comment = true;
			else if (!comment)
				sb.append(line).append("\n");
		}
		in.close();

		if ( sb.substring(0, BOM_UTF8.length()).equals(BOM_UTF8) )
			sb.replace(0, BOM_UTF8.length(), "");

		// XML has multiple root nodes so need to wrap.
		sb.insert(0, "<blueprints>\n");
		sb.append("</blueprints>\n");

		// Add the xml header.
		sb.insert(0, "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");

		if ( 1 == 1) {
			// blueprints.xml: PLAYER_SHIP_HARD_2 (FTL 1.03.1)
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
				sb.replace(m.start(), m.end(), m.group(1)+"</shields>");
				m.reset();
			}
		}

		// Parse cleaned XML
		Blueprints bps = (Blueprints)unmarshalFromSequence( Blueprints.class, sb.toString() );

		return bps;
	}

	public ShipLayout readLayout( InputStream stream ) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(stream, "UTF8"));
		ShipLayout shipLayout = new ShipLayout();

		String line = null;
		boolean firstLine = true;
		boolean comment = false;

		while ( (line = in.readLine()) != null ) {
			if ( firstLine ) {
				if ( line.startsWith(BOM_UTF8) )
					line = line.substring( BOM_UTF8.length() );
				firstLine = false;
			}
			if ( line.length() == 0 ) continue;

			if ( line.equals("X_OFFSET") ) {
				shipLayout.setOffsetX( Integer.parseInt( in.readLine() ) );
			}
			else if ( line.equals("Y_OFFSET") ) {
				shipLayout.setOffsetY( Integer.parseInt( in.readLine() ) );
			}
			else if ( line.equals("HORIZONTAL") ) {
				shipLayout.setHorizontal( Integer.parseInt( in.readLine() ) );
			}
			else if ( line.equals("VERTICAL") ) {
				shipLayout.setVertical( Integer.parseInt( in.readLine() ) );
			}
			else if ( line.equals("ELLIPSE") ) {
				int w = Integer.parseInt( in.readLine() );
				int h = Integer.parseInt( in.readLine() );
				int x = Integer.parseInt( in.readLine() );
				int y = Integer.parseInt( in.readLine() );
				shipLayout.setShieldEllipse( w, h, x, y );
			}
			else if ( line.equals("ROOM") ) {
				int roomId = Integer.parseInt( in.readLine() );
				int alpha = Integer.parseInt( in.readLine() );
				int beta = Integer.parseInt( in.readLine() );
				int hSquares = Integer.parseInt( in.readLine() );
				int vSquares = Integer.parseInt( in.readLine() );
				shipLayout.setRoom( roomId, alpha, beta, hSquares, vSquares );
			}
			else if ( line.equals("DOOR") ) {
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

	public ShipChassis readChassis(InputStream stream) throws IOException, JAXBException {
		log.trace("Reading ship chassis XML");

		// Need to clean invalid XML and comments before JAXB parsing

		BufferedReader in = new BufferedReader(new InputStreamReader(stream, "UTF8"));
		StringBuilder sb = new StringBuilder();
		String line;

		boolean comment = false;
		while( (line = in.readLine()) != null ) {
			line = line.replaceAll("<[?]xml [^>]*[?]>", "");
			line = line.replaceAll("<!--.*?-->", "");
			line = line.replaceAll("<(/?)gib[0-9]*>", "<$1gib>");

			// Remove multiline comments
			if (comment && line.contains("-->"))
				comment = false;
			else if (line.contains("<!--"))
				comment = true;
			else if (!comment)
				sb.append(line).append("\n");
		}
		in.close();

		if ( sb.substring(0, BOM_UTF8.length()).equals(BOM_UTF8) )
			sb.replace(0, BOM_UTF8.length(), "");

		// XML has multiple root nodes so need to wrap.
		sb.insert(0, "<shipChassis>\n");
		sb.append("</shipChassis>\n");

		// Add the xml header.
		sb.insert(0, "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");

		// Parse cleaned XML
		ShipChassis sch = (ShipChassis)unmarshalFromSequence( ShipChassis.class, sb.toString() );

		return sch;
	}

	public List<CrewNameList> readCrewNames(InputStream stream) throws IOException, JAXBException {
		log.trace("Reading crew name list XML");

		// Need to clean invalid XML and comments before JAXB parsing

		BufferedReader in = new BufferedReader(new InputStreamReader(stream, "UTF8"));
		StringBuilder sb = new StringBuilder();
		String line;
		boolean comment = false;

		while( (line = in.readLine()) != null ) {
			line = line.replaceAll("<[?]xml [^>]*[?]>", "");
			line = line.replaceAll("<!--.*?-->", "");

			// Remove multiline comments
			if (comment && line.contains("-->"))
				comment = false;
			else if (line.contains("<!--"))
				comment = true;
			else if (!comment)
				sb.append(line).append("\n");
		}
		in.close();

		if ( sb.substring(0, BOM_UTF8.length()).equals(BOM_UTF8) )
			sb.replace(0, BOM_UTF8.length(), "");

		// XML has multiple root nodes so need to wrap.
		sb.insert(0, "<nameLists>\n");
		sb.append("</nameLists>\n");

		// Add the xml header.
		sb.insert(0, "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");

		// Parse cleaned XML
		CrewNameLists cnl = (CrewNameLists)unmarshalFromSequence( CrewNameLists.class, sb.toString() );

		return cnl.getCrewNameLists();
	}

	public Encounters readEvents(InputStream stream, String fileName) throws IOException, JAXBException {
		log.trace("Reading events XML");

		// Need to clean invalid XML and comments before JAXB parsing

		BufferedReader in = new BufferedReader(new InputStreamReader(stream, "UTF8"));
		StringBuilder sb = new StringBuilder();
		String line;
		boolean comment = false;
		String ptn; Pattern p; Matcher m;

		while( (line = in.readLine()) != null ) {
			line = line.replaceAll("<[?]xml [^>]*[?]>", "");
			line = line.replaceAll("<!--.*?-->", "");

			if ( "events.xml".equals(fileName) || "events_nebula.xml".equals(fileName) ) {
				// events.xml: PIRATE_CIVILIAN_BEACON (FTL 1.03.1)
				// events_nebula.xml: NEBULA_REBEL_UNDETECTED (FTL 1.03.1)
				line = line.replaceAll(" hidden=\"true\" hidden=\"true\"", " hidden=\"true\"");
			}

			if ( "events_engi.xml".equals(fileName) || "events_mantis.xml".equals(fileName) || "events_crystal.xml".equals(fileName) ) {
				// events_engi.xml: DISTRESS_ENGI_REACTOR_LIST1 (FTL 1.03.1)
				// events_mantis.xml: MANTIS_NAMED_THIEF_DEFEAT (FTL 1.03.1)
				// events_crystal.xml: CRYSTAL_CACHE_BREAK (FTL 1.01)
				line = line.replaceAll("(.*<text *( [^\\/>]*)?>[^<]*)</event>.*", "$1</text>");
			}

			// Remove multiline comments
			if (comment && line.contains("-->"))
				comment = false;
			else if (line.contains("<!--"))
				comment = true;
			else if (!comment)
				sb.append(line).append("\n");
		}
		in.close();

		if ( sb.substring(0, BOM_UTF8.length()).equals(BOM_UTF8) )
			sb.replace(0, BOM_UTF8.length(), "");

		// XML has multiple root nodes so need to wrap.
		sb.insert(0, "<events>\n");
		sb.append("</events>\n");

		// Add the xml header.
		sb.insert(0, "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");

		if ( "events.xml".equals(fileName) ) {
			// events.xml: STORE_TEXT (FTL 1.03.1)
			ptn = "";
			ptn += "(<textList *(?: [^>]*)?>\\s*";
			ptn += "(?:<text *(?: [^>]*)?>[^<]*</text>\\s*)*)";
			ptn += "</text>"; // Wrong closing tag.

			p = Pattern.compile(ptn);
			m = p.matcher(sb);
			if ( m.find() ) {
				sb.replace(m.start(), m.end(), m.group(1)+"</textList>");
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
				sb.replace(m.start(), m.end(), m.group(1)+"</textList>");
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
				sb.replace(m.start(), m.end(), m.group(1)+"</event>");
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
				sb.replace(m.start(), m.end(), m.group(1)+"\n</choice>\n"+ m.group(2));
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
				sb.replace(m.start(), m.end(), m.group(1)+"</event>");
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
			ptn += "</event>";

			p = Pattern.compile(ptn);
			m = p.matcher(sb);
			if ( m.find() ) {
				sb.replace(m.start(), m.end(), m.group(1)+"\n</choice>\n");
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
			ptn += "</event>";

			p = Pattern.compile(ptn);
			m = p.matcher(sb);
			if ( m.find() ) {
				sb.replace(m.start(), m.end(), m.group(1)+"\n</eventList>\n");
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
			ptn +=     "<event *(?: [^>]*)?/>\\s*)";
			//        </choice> tag is missing.
			ptn += "(\\s*</event>)";

			p = Pattern.compile(ptn);
			m = p.matcher(sb);
			if ( m.find() ) {
				sb.replace(m.start(), m.end(), m.group(1)+"\n</choice>\n"+ m.group(2));
				m.reset();
			}
		}

		// Parse cleaned XML
		Encounters evts = (Encounters)unmarshalFromSequence( Encounters.class, sb );

		return evts;
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

		} catch (JAXBException e) {
			log.debug("Error during xml parsing. Dumping document for review...");
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
		InnerFileInfo info = innerFilesMap.get(innerPath);
		if (info == null) throw new FileNotFoundException("The path ("+ innerPath +") was not found in "+ datFile.getName());

		MappedByteBuffer buf = randomDatFile.getChannel().map(FileChannel.MapMode.READ_ONLY, info.dataOffset, info.dataSize);
		buf.load();
		InputStream stream = new ByteBufferBackedInputStream(buf);
		return stream;
	}

	public void unpackDat(File outFolder) throws IOException {
		log.trace("Unpacking dat file " + datFile.getPath() + " into " + outFolder.getPath());

		byte[] buffer = new byte[4096];
		int bytesRead;

		outFolder.mkdirs();
		for (Map.Entry<String, InnerFileInfo> entry : innerFilesMap.entrySet()) {
			String innerPath = entry.getKey();
			InnerFileInfo info = entry.getValue();

			log.trace("Unpacking: " + innerPath + " ("+ info.dataSize +"b)");

			File outFile = new File(outFolder, innerPath);
			outFile.getParentFile().mkdirs();
			InputStream in = null;
			FileOutputStream out = null;
			try {
				in = getInputStream(innerPath);
				out = new FileOutputStream(outFile);

				while ((bytesRead = in.read(buffer)) != -1) {
					out.write(buffer, 0, bytesRead);
				}

			} finally {
				try {if (in != null) in.close();}
				catch (IOException e) {}

				try {if (out != null) out.close();}
				catch (IOException e) {}
			}
		}
	}

	public void close() throws IOException {
		if (randomDatFile != null) randomDatFile.close();
	}



	private class InnerFileInfo {
		public long dataOffset = 0;
		public long dataSize = 0;

		public InnerFileInfo(long dataOffset, long dataSize) {
			this.dataOffset = dataOffset;
			this.dataSize = dataSize;
		}
	}



	public class ByteBufferBackedInputStream extends InputStream {
		ByteBuffer buf;
		public ByteBufferBackedInputStream(ByteBuffer buf) {
			this.buf = buf;
		}
		@Override
		public synchronized int available() throws IOException {
			if (!buf.hasRemaining()) return 0;
			return buf.remaining();
		}
		@Override
		public synchronized int read() throws IOException {
			if (!buf.hasRemaining()) return -1;
			return buf.get() & 0xFF;
		}
		@Override
		public synchronized int read(byte[] bytes, int off, int len) throws IOException {
			if (!buf.hasRemaining()) return -1;
			len = Math.min(len, buf.remaining());
			buf.get(bytes, off, len);
			return len;
		}
	}
}

