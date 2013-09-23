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

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.output.DOMOutputter;

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

	private Pattern xmlDeclPtn = Pattern.compile( "<[?]xml [^>]*?[?]>\n*" );

	private File datFile = null;
	private FTLDat.FTLPack datP = null;


	public DatParser( File datFile ) throws IOException {
		this.datFile = datFile;
		this.datP = new FTLDat.FTLPack( datFile, false );
	}


	public List<Achievement> readAchievements( InputStream stream, String fileName ) throws IOException, JAXBException, JDOMException {
		log.trace( "Reading achievements XML" );

		String streamText = TextUtilities.decodeText( stream, fileName ).text;
		streamText = xmlDeclPtn.matcher(streamText).replaceFirst( "" );
		streamText = "<achievements>"+ streamText +"</achievements>";
		Document doc = TextUtilities.parseStrictOrSloppyXML( streamText, fileName );
		DOMOutputter domOutputter = new DOMOutputter();

		JAXBContext jc = JAXBContext.newInstance( Achievements.class );
		Unmarshaller u = jc.createUnmarshaller();
		Achievements ach = (Achievements)u.unmarshal( domOutputter.output( doc ) );

		return ach.getAchievements();
	}


	public Blueprints readBlueprints( InputStream stream, String fileName ) throws IOException, JAXBException, JDOMException {
		log.trace( "Reading blueprints XML" );

		String streamText = TextUtilities.decodeText( stream, fileName ).text;
		streamText = xmlDeclPtn.matcher(streamText).replaceFirst( "" );
		streamText = "<blueprints>"+ streamText +"</blueprints>";
		StringBuilder sb = new StringBuilder( streamText );
		String ptn; Pattern p; Matcher m;

		if ( "blueprints.xml".equals(fileName) ) {
			// blueprints.xml: LONG_ELITE_MED shipBlueprint (FTL 1.03.1)
			// blueprints.xml: LONG_ELITE_HARD shipBlueprint (FTL 1.03.1)
			streamText = streamText.replaceAll( " img=\"rebel_long_hard\"", " img=\"rebel_long_elite\"" );
		}

		if ( "blueprints.xml".equals(fileName) ) {
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

		Document doc = TextUtilities.parseStrictOrSloppyXML( sb.toString(), fileName );
		DOMOutputter domOutputter = new DOMOutputter();

		JAXBContext jc = JAXBContext.newInstance( Blueprints.class );
		Unmarshaller u = jc.createUnmarshaller();
		Blueprints bps = (Blueprints)u.unmarshal( domOutputter.output( doc ) );

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


	public ShipChassis readChassis( InputStream stream, String fileName ) throws IOException, JAXBException, JDOMException {
		log.trace( "Reading ship chassis XML" );

		String streamText = TextUtilities.decodeText( stream, fileName ).text;
		streamText = xmlDeclPtn.matcher(streamText).replaceFirst( "" );
		streamText = "<shipChassis>"+ streamText +"</shipChassis>";
		Document doc = TextUtilities.parseStrictOrSloppyXML( streamText, fileName );
		DOMOutputter domOutputter = new DOMOutputter();

		JAXBContext jc = JAXBContext.newInstance( ShipChassis.class );
		Unmarshaller u = jc.createUnmarshaller();
		ShipChassis sch = (ShipChassis)u.unmarshal( domOutputter.output( doc ) );

		return sch;
	}

	public List<CrewNameList> readCrewNames( InputStream stream, String fileName ) throws IOException, JAXBException, JDOMException {
		log.trace( "Reading crew name list XML" );

		String streamText = TextUtilities.decodeText( stream, fileName ).text;
		streamText = xmlDeclPtn.matcher(streamText).replaceFirst( "" );
		streamText = "<nameLists>"+ streamText  +"</nameLists>";
		Document doc = TextUtilities.parseStrictOrSloppyXML( streamText, fileName );
		DOMOutputter domOutputter = new DOMOutputter();

		JAXBContext jc = JAXBContext.newInstance( CrewNameLists.class );
		Unmarshaller u = jc.createUnmarshaller();
		CrewNameLists cnl = (CrewNameLists)u.unmarshal( domOutputter.output( doc ) );

		return cnl.getCrewNameLists();
	}


	public Encounters readEvents( InputStream stream, String fileName ) throws IOException, JAXBException, JDOMException {
		log.trace( "Reading events XML" );

		String streamText = TextUtilities.decodeText( stream, fileName ).text;
		streamText = xmlDeclPtn.matcher(streamText).replaceFirst( "" );
		streamText = "<events>"+ streamText  +"</events>";
		Document doc = TextUtilities.parseStrictOrSloppyXML( streamText, fileName );
		DOMOutputter domOutputter = new DOMOutputter();

		JAXBContext jc = JAXBContext.newInstance( Encounters.class );
		Unmarshaller u = jc.createUnmarshaller();
		Encounters evts = (Encounters)u.unmarshal( domOutputter.output( doc ) );

		return evts;
	}


	public List<ShipEvent> readShipEvents( InputStream stream, String fileName ) throws IOException, JAXBException, JDOMException {
		log.trace( "Reading ship events XML" );

		String streamText = TextUtilities.decodeText( stream, fileName ).text;
		streamText = xmlDeclPtn.matcher(streamText).replaceFirst( "" );
		streamText = "<shipEvents>"+ streamText  +"</shipEvents>";
		Document doc = TextUtilities.parseStrictOrSloppyXML( streamText, fileName );
		DOMOutputter domOutputter = new DOMOutputter();

		JAXBContext jc = JAXBContext.newInstance( ShipEvents.class );
		Unmarshaller u = jc.createUnmarshaller();
		ShipEvents shvts = (ShipEvents)u.unmarshal( domOutputter.output( doc ) );

		return shvts.getShipEvents();
	}


	public List<BackgroundImageList> readImageLists( InputStream stream, String fileName ) throws IOException, JAXBException, JDOMException {
		log.trace( "Reading background images XML" );

		String streamText = TextUtilities.decodeText( stream, fileName ).text;
		streamText = xmlDeclPtn.matcher(streamText).replaceFirst( "" );
		streamText = "<imageLists>"+ streamText  +"</imageLists>";
		Document doc = TextUtilities.parseStrictOrSloppyXML( streamText, fileName );
		DOMOutputter domOutputter = new DOMOutputter();

		JAXBContext jc = JAXBContext.newInstance( BackgroundImageLists.class );
		Unmarshaller u = jc.createUnmarshaller();
		BackgroundImageLists imgs = (BackgroundImageLists)u.unmarshal( domOutputter.output( doc ) );

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
