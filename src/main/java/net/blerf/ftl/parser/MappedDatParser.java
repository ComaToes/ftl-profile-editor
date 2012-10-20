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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.blerf.ftl.model.ShipLayout;
import net.blerf.ftl.xml.Achievement;
import net.blerf.ftl.xml.Achievements;
import net.blerf.ftl.xml.Blueprints;
import net.blerf.ftl.xml.ShipBlueprint;


public class MappedDatParser extends Parser implements Closeable {

	private static final Logger log = LogManager.getLogger(MappedDatParser.class);

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

		BufferedReader in = new BufferedReader(new InputStreamReader(stream));
		StringBuilder sb = new StringBuilder();
		String line;
		sb.append("<achievements>").append("\n");  // XML has multiple root nodes so need to wrap
		while( (line = in.readLine()) != null ) {
			line = line.replaceAll("<!--[^>]*-->", "");  // TODO need a proper Matcher for multiline comments
			line = line.replaceAll("<desc>([^<]*)</name>", "<desc>$1</desc>");
			sb.append(line).append("\n");
		}
		in.close();
		sb.append("</achievements>").append("\n");

		// Parse cleaned XML
		JAXBContext jc = JAXBContext.newInstance(Achievements.class);
		Unmarshaller u = jc.createUnmarshaller();
		Achievements ach = (Achievements)u.unmarshal( new StreamSource(new StringReader(sb.toString())) );

		return ach.getAchievements();
	}

	public Blueprints readBlueprints(InputStream stream) throws IOException, JAXBException {
		log.trace("Reading blueprints XML");

		// Need to clean invalid XML and comments before JAXB parsing

		BufferedReader in = new BufferedReader(new InputStreamReader(stream));
		StringBuilder sb = new StringBuilder();
		String line;
		sb.append("<blueprints>").append("\n");  // XML has multiple root nodes so need to wrap
		boolean comment = false, inShipShields = false, inSlot = false;
		while( (line = in.readLine()) != null ) {
			line = line.replaceAll("^<!-- sardonyx$", "<!-- sardonyx -->");  // Error above one shipBlueprint
			line = line.replaceAll("<!--.*-->", "");
			line = line.replaceAll("<\\?xml[^>]*>", "");
			line = line.replaceAll("<title>([^<]*)</[^>]*>", "<title>$1</title>");  // Error present in systemBlueprint and itemBlueprint
			line = line.replaceAll("<tooltip>([^<]*)</[^>]*>(-->)?", "<tooltip>$1</tooltip>");  // Error present in weaponBlueprint
			line = line.replaceAll("<speed>([^<]*)</[^>]*>", "<speed>$1</speed>");  // Error present in weaponBlueprint
			line = line.replaceAll("\"img=", "\" img=");  // ahhhh
			line = line.replaceAll("</ship>", "</shipBlueprint>");  // Error in one shipBlueprint

			// Multi-line error in shipBlueprint
			if( line.matches(".*<shields [^\\/>]*>") ) {
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

			// Remove multiline comments
			if (comment && line.contains("-->"))
				comment = false;
			else if(line.contains("<!--"))
				comment = true;
			else if(!comment)
				sb.append(line).append("\n");
		}
		in.close();
		sb.append("</blueprints>").append("\n");

		// Parse cleaned XML
		JAXBContext jc = JAXBContext.newInstance(Blueprints.class);
		Unmarshaller u = jc.createUnmarshaller();
		Blueprints bps = (Blueprints)u.unmarshal( new StreamSource(new StringReader(sb.toString())) );

		return bps;
	}

	public ShipLayout readLayout( InputStream stream ) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(stream));
		ShipLayout shipLayout = new ShipLayout();

		String line = null;
		while ( (line = in.readLine()) != null ) {
			if ( line.length() == 0 ) break;

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
		public synchronized int available() throws IOException {
			if (!buf.hasRemaining()) return 0;
			return buf.remaining();
		}
		public synchronized int read() throws IOException {
			if (!buf.hasRemaining()) return -1;
			return buf.get() & 0xFF;
		}
		public synchronized int read(byte[] bytes, int off, int len) throws IOException {
			if (!buf.hasRemaining()) return -1;
			len = Math.min(len, buf.remaining());
			buf.get(bytes, off, len);
			return len;
		}
	}
}

