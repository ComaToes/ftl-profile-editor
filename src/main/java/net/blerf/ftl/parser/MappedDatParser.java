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
import net.blerf.ftl.xml.ShipBlueprint;


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
		while( (line = in.readLine()) != null ) {
			line = line.replaceAll("<!--[^>]*-->", "");  // TODO need a proper Matcher for multiline comments
			line = line.replaceAll("<desc>([^<]*)</name>", "<desc>$1</desc>");
			sb.append(line).append("\n");
		}
		in.close();
		if ( sb.substring(0, BOM_UTF8.length()).equals(BOM_UTF8) )
			sb.replace(0, BOM_UTF8.length(), "");

		// XML has multiple root nodes so need to wrap.
		sb.insert(0, "<achievements>\n");
		sb.append("</achievements>\n");

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

		boolean comment = false, inShipShields = false, inSlot = false;
		while( (line = in.readLine()) != null ) {
			// blueprints.xml
			line = line.replaceAll("^<!-- sardonyx$", "<!-- sardonyx -->");  // Error above one shipBlueprint
			line = line.replaceAll("<!--.*-->", "");
			line = line.replaceAll("<\\?xml[^>]*>", "");
			line = line.replaceAll("<title>([^<]*)</[^>]*>", "<title>$1</title>");  // Error present in systemBlueprint and itemBlueprint
			line = line.replaceAll("<tooltip>([^<]*)</[^>]*>(-->)?", "<tooltip>$1</tooltip>");  // Error present in weaponBlueprint
			line = line.replaceAll("<speed>([^<]*)</[^>]*>", "<speed>$1</speed>");  // Error present in weaponBlueprint
			line = line.replaceAll("\"img=", "\" img=");  // ahhhh
			line = line.replaceAll("</ship>", "</shipBlueprint>");  // Error in one shipBlueprint

			// Multi-line error in shipBlueprint
			if ( line.matches(".*<shields [^\\/>]*>") ) {
				inShipShields = true;
			} else if (inShipShields) {
				if (line.contains("<slot>"))
					inSlot = true;
				else if (line.contains("</slot>")) {
					if (!inSlot) {
						line = line.replace("</slot>", "</shields>");
						inShipShields = false;
					}
					inSlot = false;
				} else if (line.contains("</shields>"))
					inShipShields = false;
			}

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

		// Parse cleaned XML
		Blueprints bps = (Blueprints)unmarshalFromSequence( Blueprints.class, sb.toString() );

		return bps;
	}

	public ShipLayout readLayout( InputStream stream ) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(stream, "UTF8"));
		ShipLayout shipLayout = new ShipLayout();

		String line = null;
		boolean firstLine = true;
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

