package net.blerf.ftl.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.blerf.ftl.xml.Achievement;
import net.blerf.ftl.xml.Achievements;
import net.blerf.ftl.xml.Blueprints;
import net.blerf.ftl.xml.ShipBlueprint;

public class DatParser extends Parser {
	
	private static final Logger log = LogManager.getLogger(DatParser.class);
	
	private byte[] buf = new byte[1024];
	
	public List<Achievement> readAchievements( File xmlFile ) throws IOException, JAXBException {
		
		log.trace("Reading achievements XML from: " + xmlFile.getPath());
		
		// Need to clean invalid XML and comments - write to temp file before JAXB parsing
		File tempFile = File.createTempFile("ftledit", ".tmp");
		tempFile.deleteOnExit();
		
		BufferedReader in = new BufferedReader( new FileReader(xmlFile) );
		PrintWriter out = new PrintWriter(tempFile);
		
		String line;
		out.println( "<achievements>" ); // XML has multiple root nodes so need to wrap
		while( (line = in.readLine()) != null ) {
			line = line.replaceAll( "<!--[^>]*-->" , "" ); // TODO need a proper Matcher for multiline comments
			line = line.replaceAll( "<desc>([^<]*)</name>" , "<desc>$1</desc>" );
			out.println( line );
		}
		in.close();
		out.println( "</achievements>" );
		out.close();
		
		// Parse cleaned XML
		InputStream cleanIn = new FileInputStream( tempFile );
		
		JAXBContext jc = JAXBContext.newInstance( Achievements.class );
	    Unmarshaller u = jc.createUnmarshaller();
	    Achievements ach = (Achievements)u.unmarshal( cleanIn );
	    
	    tempFile.delete();
	    
	    return ach.getAchievements();

	}
	
	public Blueprints readBlueprints( File xmlFile ) throws IOException, JAXBException {
		
		log.trace("Reading blueprints XML from: " + xmlFile.getPath());
		
		// Need to clean invalid XML and comments - write to temp file before JAXB parsing
		File tempFile = File.createTempFile("ftledit", ".tmp");
		tempFile.deleteOnExit();
		
		BufferedReader in = new BufferedReader( new InputStreamReader( new FileInputStream(xmlFile) ) );
		PrintWriter out = new PrintWriter(tempFile);
		
		String line;
		out.println( "<blueprints>" ); // XML has multiple root nodes so need to wrap
		boolean comment = false, inShipShields = false, inSlot = false;
		while( (line = in.readLine()) != null ) {
			
			line = line.replaceAll( "<!--.*-->" , "" );
			line = line.replaceAll( "<\\?xml[^>]*>" , "" );
			line = line.replaceAll( "<title>([^<]*)</[^>]*>" , "<title>$1</title>" ); // Error present in systemBlueprint and itemBlueprint
			line = line.replaceAll( "<tooltip>([^<]*)</[^>]*>(-->)?" , "<tooltip>$1</tooltip>" ); // Error present in weaponBlueprint
			line = line.replaceAll( "<speed>([^<]*)</[^>]*>" , "<speed>$1</speed>" ); // Error present in weaponBlueprint
			line = line.replaceAll( "\"img=" , "\" img=" ); // ahhhh 
			line = line.replaceAll( "</ship>" , "</shipBlueprint>" ); // Error in one shipBlueprint
			
			// Multi-line error in shipBlueprint			
			if( line.matches(".*<shields [^\\/>]*>") ) {
				inShipShields = true;
			} else if( inShipShields ) {
				if( line.contains("<slot>") )
					inSlot = true;
				else if( line.contains("</slot>") ) {
					if( !inSlot ) {
						line = line.replace("</slot>", "</shields>");
						inShipShields = false;
					}
					inSlot = false;
				} else if( line.contains("</shields>") )
					inShipShields = false;
			}

			// Remove multiline comments
			if( comment && line.contains("-->") )
				comment = false;
			else if( line.contains("<!--") )
				comment = true;
			else if( !comment )
				out.println( line );
			
		}
		in.close();
		out.println( "</blueprints>" );
		out.close();
		
		// Parse cleaned XML
		InputStream cleanIn = new FileInputStream( tempFile );
		
		JAXBContext jc = JAXBContext.newInstance( Blueprints.class );
	    Unmarshaller u = jc.createUnmarshaller();
	    Blueprints bps = (Blueprints)u.unmarshal( cleanIn );
	    
	    tempFile.delete();
	    
	    return bps;

	}

	public void unpackDat( File datFile, File outFolder ) throws IOException {
		
		log.trace("Unpacking dat file " + datFile.getPath() + " into " + outFolder.getPath());
		
		InputStream in = new FileInputStream(datFile);
		
		outFolder.mkdirs();
		
		int[] header = readHeader(in);
		
		int i = 0;
		while( i < header.length && header[i++] != 0 )
			readFile(in, outFolder);
		
		in.close();
		
	}
	
	private int[] readHeader(InputStream in) throws IOException {
		
		log.trace("Reading dat header");
		
		int headerSize = readInt(in);
		
		int[] header = new int[headerSize];
		
		for (int i = 0; i < header.length; i++) {
			header[i] = readInt(in);
		}
		
		return header;
		
	}
	
	private void readFile(InputStream in, File outFolder) throws IOException {

		log.trace("Reading packaged file");
		
		int dataSize = readInt(in);
		String fileName = readString(in);
		
		log.trace("Filename: " + fileName + " ("+dataSize+"b)");
		
		File outFile = new File( outFolder, fileName );
		outFile.getParentFile().mkdirs();
		OutputStream out = new FileOutputStream(outFile);
		
		while( dataSize > 0 ) {
			int count = in.read(buf, 0, dataSize > buf.length ? buf.length : dataSize);
			if( count < 0 )
				throw new RuntimeException(); // TODO make checked
			dataSize -= count;
			out.write(buf, 0, count);
		}
		
		out.close();
		
	}

}
