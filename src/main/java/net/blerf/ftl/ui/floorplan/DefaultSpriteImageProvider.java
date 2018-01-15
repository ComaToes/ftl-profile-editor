package net.blerf.ftl.ui.floorplan;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GraphicsDevice;
import java.awt.GraphicsConfiguration;
import java.awt.Point;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.parser.SavedGameParser.CrewType;
import net.blerf.ftl.parser.SavedGameParser.DroneType;
import net.blerf.ftl.parser.SavedGameParser.SystemType;
import net.blerf.ftl.ui.floorplan.AnimAtlas;
import net.blerf.ftl.ui.floorplan.DoorAtlas;
import net.blerf.ftl.xml.Anim;
import net.blerf.ftl.xml.AnimSheet;


public class DefaultSpriteImageProvider {

	private static final String BREACH_ANIM = "breach";
	private static final String FIRE_ANIM = "fire_large";

	private static final Logger log = LoggerFactory.getLogger( DefaultSpriteImageProvider.class );

	private final Color dummyColor = new Color( 150, 150, 200 );

	private GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	private GraphicsDevice gs = ge.getDefaultScreenDevice();
	private GraphicsConfiguration gc = gs.getDefaultConfiguration();

	private DoorAtlas cachedDoorAtlas = null;
	private Map<SystemType, BufferedImage> cachedSystemRoomsMap = new EnumMap<SystemType, BufferedImage>( SystemType.class );
	private Map<String, AnimAtlas> cachedAnimAtlasMap = new HashMap<String, AnimAtlas>();


	/**
	 * Returns graphics for use in DoorSprites.
	 *
	 * FTL 1.01-1.03.3: The Doors system had 3 levels.
	 * FTL 1.5.4+: The Doors system had 5 levels.
	 */
	public DoorAtlas getDoorAtlas() {
		if ( cachedDoorAtlas != null ) return cachedDoorAtlas;

		int frameW = 35;
		int frameH = 35;
		String sheetPath = "img/effects/door_sheet.png";

		BufferedImage sheetImage = null;
		int levelCount = 0;
		int colCount = 5;  // TODO: Magic number.

		try {
			sheetImage = readResourceImage( sheetPath );

			levelCount = sheetImage.getHeight() / frameH;

			if ( sheetImage.getWidth() / frameW < colCount ) {
				throw new IOException( String.format( "Too few columns (expected %d)", colCount ) );
			}
		}
		catch ( FileNotFoundException e ) {
			log.error( "Could not find door sheet innerPath: "+ sheetPath );
		}
		catch ( IOException e ) {
			log.error( "Failed to read door sheet image: "+ sheetPath, e );
		}

		if ( sheetImage == null ) {
			int dummyW = 5 * frameW;
			int dummyH = 5 * frameH;
			sheetImage = createDummyImage( dummyW, dummyH );
		}

		Point[][] framesets = new Point[levelCount][];

		for ( int level=0; level < levelCount; level++ ) {
			framesets[level] = new Point[colCount];

			for ( int c=0; c < colCount; c++ ) {
				framesets[level][c] = new Point( c * frameW, level * frameH );
			}
		}

		cachedDoorAtlas = new DoorAtlas( sheetImage, frameW, frameH, framesets );

		return cachedDoorAtlas;
	}

	/**
	 * Returns the system icon to overlay on a room floor.
	 *
	 * It will be a black outline with white fill, to be tinted afterward.
	 */
	public BufferedImage getSystemRoomImage( SystemType systemType ) {
		if ( cachedSystemRoomsMap.containsKey( systemType ) ) return cachedSystemRoomsMap.get( systemType );

		String overlayPath = "img/icons/s_"+ systemType.getId() +"_overlay.png";

		BufferedImage overlayImage = null;

		try {
			overlayImage = readResourceImage( overlayPath );
		}
		catch ( FileNotFoundException e ) {
			log.error( "Could not find system room innerPath: "+ overlayPath );
		}
		catch ( IOException e ) {
			log.error( "Failed to read system room image: "+ overlayPath, e );
		}

		if ( overlayImage == null ) {
			int dummyW = 32;
			int dummyH = 32;
			overlayImage = createDummyImage( dummyW, dummyH );
		}

		cachedSystemRoomsMap.put( systemType, overlayImage );

		return overlayImage;
	}

	/**
	 * Returns graphics for use in BreachSprites.
	 *
	 * Typical innerPath: "img/effects/breach.png".
	 */
	public AnimAtlas getBreachAtlas() {
		int frameW = 19;
		int frameH = 19;

		AnimAtlas animAtlas = null;

		Anim breachAnim = DataManager.get().getAnim( BREACH_ANIM );
		if ( breachAnim != null ) {
			animAtlas = getAnimAtlas( breachAnim.getSheetId() );
		}

		if ( animAtlas == null ) {
			BufferedImage sheetImage = createDummyImage( frameW, frameH );
			Map<String, Point[]> framesetsMap = new HashMap<String, Point[]>();
			animAtlas = new AnimAtlas( sheetImage, frameW, frameH, framesetsMap );
		}

		return animAtlas;
	}

	public AnimAtlas getFireAtlas() {
		int frameW = 32;
		int frameH = 32;

		AnimAtlas animAtlas = null;

		Anim fireAnim = DataManager.get().getAnim( FIRE_ANIM );
		if ( fireAnim != null ) {
			animAtlas = getAnimAtlas( fireAnim.getSheetId() );
		}

		if ( animAtlas == null ) {
			BufferedImage sheetImage = createDummyImage( frameW, frameH );
			Map<String, Point[]> framesetsMap = new HashMap<String, Point[]>();
			animAtlas = new AnimAtlas( sheetImage, frameW, frameH, framesetsMap );
		}

		return animAtlas;
	}

	/**
	 * Returns an image, unmodified, from FTL's resources.
	 *
	 * This will either return the requested image or throw an exception.
	 *
	 * The result will NOT be cached.
	 */
	private BufferedImage readResourceImage( String innerPath ) throws FileNotFoundException, IOException {
		InputStream in = null;
		try {
			in = DataManager.get().getResourceInputStream( innerPath );
			BufferedImage result = ImageIO.read( in );

			if ( result == null ) throw new IOException( "ImageIO did not recognize the file type" );

			return result;
		}
		finally {
			try {if ( in != null ) in.close();}
			catch ( IOException e ) {}
		}
	}

	/**
	 * Returns a solid rectangle for use as a fallback image.
	 *
	 * The result will NOT be cached.
	 */
	private BufferedImage createDummyImage( int width, int height ) {
		BufferedImage dummyImage = gc.createCompatibleImage( width, height, Transparency.OPAQUE );
		Graphics2D g2d = dummyImage.createGraphics();
		g2d.setColor( dummyColor );
		g2d.fillRect( 0, 0, width-1, height-1 );
		g2d.dispose();

		return dummyImage;
	}

	/**
	 * Returns an AnimAtlas, including all Anims that appear on an AnimSheet.
	 *
	 * The result will be cached.
	 */
	public AnimAtlas getAnimAtlas( String sheetId ) {
		if ( cachedAnimAtlasMap.containsKey( sheetId ) ) return cachedAnimAtlasMap.get( sheetId );

		AnimSheet sheet = DataManager.get().getAnimSheet( sheetId );

		int frameW = -1;
		int frameH = -1;
		BufferedImage sheetImage = null;

		if ( sheet != null ) {
			frameW = sheet.getFrameWidth();
			frameH = sheet.getFrameHeight();
			String sheetPath = sheet.getInnerPath();

			if ( !sheetPath.isEmpty() ) {
				sheetPath = "img/"+ sheetPath;  // Sheets omit the top-level dir.

				try {
					sheetImage = readResourceImage( sheetPath );
				}
				catch ( FileNotFoundException e ) {
					log.error( "Could not find AnimSheet innerPath: "+ sheetPath );
				}
				catch ( IOException e ) {
					log.error( "Failed to read AnimSheet image: "+ sheetPath, e );
				}
			}
		}

		if ( sheetImage == null ) {
			frameW = 1;  // Future-proof against divide-by-zero errors.
			frameH = 1;  // But sheet pixels must be greater than requested rows/cols.
			int dummyW = 35;
			int dummyH = 35;
			sheetImage = createDummyImage( dummyW, dummyH );
		}

		AnimAtlas animAtlas;

		List<Anim> animsList = DataManager.get().getAnimsBySheetId( sheetId );
		Map<String, Point[]> framesetsMap = new HashMap<String, Point[]>( animsList.size() );

		for ( Anim anim : animsList ) {
			int frameCount = anim.getAnimSpec().frameCount;
			int fromCol = anim.getAnimSpec().column;
			int fromRow = anim.getAnimSpec().row;
			Point[] frameset = new Point[frameCount];

			for ( int i=0; i < frameCount; i++ ) {
				int frameX = (fromCol + i) * frameW;  // TODO: Row wrap?
				int frameY = sheetImage.getHeight() - (fromRow+1) * frameH;  // +1 to get top-edge, counting from bottom.
				frameset[i] = new Point( frameX, frameY );
			}
			framesetsMap.put( anim.getId(), frameset );
		}

		animAtlas = new AnimAtlas( sheetImage, frameW, frameH, framesetsMap );

		cachedAnimAtlasMap.put( sheetId, animAtlas );

		return animAtlas;
	}

	// For an Anim, get the Sheet.
	// Fet all Anims that appear on the Sheet.
	// For each Anim, generate frameset Points.
	// Put the frameset in an animId-to-frameset Map.
	// Create an AnimAtlas with the sheetImage, frameW+H, and framesetsMap.
	// Cache the AnimAtlas using the sheetId.
}
