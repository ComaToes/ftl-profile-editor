package net.blerf.ftl.ui.floorplan;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GraphicsDevice;
import java.awt.GraphicsConfiguration;
import java.awt.Rectangle;
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
import net.blerf.ftl.ui.ImageUtilities;
import net.blerf.ftl.ui.ImageUtilities.Tint;
import net.blerf.ftl.ui.floorplan.AnimAtlas;
import net.blerf.ftl.ui.floorplan.DoorAtlas;
import net.blerf.ftl.xml.Anim;
import net.blerf.ftl.xml.AnimSheet;


public class DefaultSpriteImageProvider implements SpriteImageProvider {

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

	private Map<String, Map<Rectangle, BufferedImage>> cachedImagesMap = new HashMap<String, Map<Rectangle, BufferedImage>>();
	private Map<BufferedImage, Map<Tint, BufferedImage>> cachedTintedImagesMap = new HashMap<BufferedImage, Map<Tint, BufferedImage>>();


	public DefaultSpriteImageProvider() {
	}

	/**
	 * Returns a default image of a drone body, possibly with a colored outline.
	 *
	 * The result will be cached.
	 *
	 * @see #getCrewBodyImage( CrewType, boolean, boolean)
	 */
	@Override
	public BufferedImage getDroneBodyImage( DroneType droneType, boolean playerControlled ) {
		BufferedImage result = null;
		String imgRace = "";
		String originalSuffix = "";

		// Drone bodies are unselectble in-game, hence no alternate color variants.

		if ( DroneType.BATTLE.equals( droneType ) ) {  // Anti-personnel, always local to the ship.
			imgRace = "battle";
			originalSuffix = (( playerControlled ) ? "_sheet" : "_enemy_sheet");
		}
		else if ( DroneType.REPAIR.equals( droneType ) ) {  // Always local to the ship.
			imgRace = "repair";
			originalSuffix = (( playerControlled ) ? "_sheet" : "_enemy_sheet");
		}
		else {
			throw new IllegalArgumentException( "Requested a body for a DroneType that doesn't need one: "+ droneType.getId() );
		}

		int offsetX = 0, offsetY = 0, w = 35, h = 35;
		String basePath = "img/people/"+ imgRace +"_base.png";
		String originalPath = "img/people/"+ imgRace + originalSuffix +".png";

		if ( DataManager.get().hasResourceInputStream( basePath ) ) {
			// FTL 1.5.4+
			result = ImageUtilities.getCroppedImage( basePath, offsetX, offsetY, w, h, cachedImagesMap );
		}
		else if ( DataManager.get().hasResourceInputStream( originalPath ) ) {
			// FTL 1.01-1.03.3
			result = ImageUtilities.getCroppedImage( originalPath, offsetX, offsetY, w, h, cachedImagesMap );
		}
		else {
			log.error( String.format( "No body image found for drone: %s, %s", droneType.getId(), (playerControlled ? "playerControlled" : "NPC") ) );

			result = gc.createCompatibleImage( w, h, Transparency.OPAQUE );
			Graphics2D g2d = result.createGraphics();
			g2d.setColor( new Color( 150, 150, 200 ) );
			g2d.fillRect( 0, 0, result.getWidth()-1, result.getHeight()-1 );
			g2d.dispose();
		}

		return result;
	}

	/**
	 * Returns a default image of crew, with a friend-or-foe colored outline.
	 *
	 * Generally the image name is related to the raceId, with two exceptions.
	 * Humans have a separate female image. Ghost crew have human images (with
	 * programmatically reduced opacity).
	 *
	 * Image names have varied:
	 *   FTL 1.01: Drones had "X_sheet" / "X_enemy_sheet".
	 *   FTL 1.01: Crew had "X_player_[green|yellow]" / "X_enemy_red".
	 *   FTL 1.03.1: Drones could also be "X_player[no color]" / "X_enemy_red".
	 *   FTL 1.5.4: Crew had "X_base" overlaid on a tinted mask "X_color".
	 *   FTL 1.5.4: Drone had "X_base" with no color possibility.
	 *
	 * The result will be cached.
	 */
	@Override
	public BufferedImage getCrewBodyImage( CrewType crewType, boolean male, boolean playerControlled ) {
		BufferedImage result = null;
		String imgRace = "";
		String originalSuffix = "";

		if ( CrewType.HUMAN.equals( crewType ) ) {
			imgRace = (( male ) ? CrewType.HUMAN.getId() : "female");

			originalSuffix = (( playerControlled ) ? "_player_yellow" : "_enemy_red");
		}
		else if ( CrewType.GHOST.equals( crewType ) ) {
			imgRace = (( male ) ? CrewType.HUMAN.getId() : "female");

			originalSuffix = (( playerControlled ) ? "_player_yellow" : "_enemy_red");
		}
		else if ( CrewType.BATTLE.equals( crewType ) ) {  // Boarder, always foreign to the ship.
			imgRace = "battle";
			originalSuffix = (( playerControlled ) ? "_sheet" : "_enemy_sheet");
		}
		else {
			imgRace = crewType.getId();
			originalSuffix = (( playerControlled ) ? "_player_yellow" : "_enemy_red");
		}

		int offsetX = 0, offsetY = 0, w = 35, h = 35;
		String basePath = "img/people/"+ imgRace +"_base.png";
		String colorPath = "img/people/"+ imgRace +"_color.png";
		String originalPath = "img/people/"+ imgRace + originalSuffix +".png";

		if ( DataManager.get().hasResourceInputStream( basePath ) ) {
			// FTL 1.5.4+
			BufferedImage baseImage = ImageUtilities.getCroppedImage( basePath, offsetX, offsetY, w, h, cachedImagesMap );

			// Ghosts have reduced opacity.
			if ( CrewType.GHOST.equals( crewType ) ) {
				// Not an exact color match, but close enough.
				Tint ghostTint = new Tint( new float[] { 1f, 1f, 1f, 0.6f }, new float[] { 0, 0, 0, 0 } );

				// TODO: This may need to be moved when crew tint layers are
				// implemented.
				ImageUtilities.getTintedImage( baseImage, ghostTint, cachedTintedImagesMap );
			}

			if ( DataManager.get().hasResourceInputStream( colorPath ) ) {
				BufferedImage colorImage = ImageUtilities.getCroppedImage( colorPath, offsetX, offsetY, w, h, cachedImagesMap );
				float[] yellow = new float[] { 0.957f, 0.859f, 0.184f, 1f };
				float[] red = new float[] { 1.0f, 0.286f, 0.145f, 1f };
				Tint colorTint = new Tint( (playerControlled ? yellow: red), new float[] { 0, 0, 0, 0 } );
				colorImage = ImageUtilities.getTintedImage( colorImage, colorTint, cachedTintedImagesMap );

				result = gc.createCompatibleImage( w, h, Transparency.TRANSLUCENT );
				Graphics2D g2d = result.createGraphics();
				g2d.drawImage( colorImage, 0, 0, null );
				g2d.drawImage( baseImage, 0, 0, null );
				g2d.dispose();
			}
			else {
				result = baseImage;  // No colorImage to tint & outline the sprite, probably a drone.
			}
		}
		else if ( DataManager.get().hasResourceInputStream( originalPath ) ) {
			// FTL 1.01-1.03.3
			result = ImageUtilities.getCroppedImage( originalPath, offsetX, offsetY, w, h, cachedImagesMap );
		}
		else {
			log.error( String.format( "No body image found for crew: %s, %s, %s", crewType.getId(), (male ? "male" : "female"), (playerControlled ? "playerControlled" : "NPC") ) );

			result = gc.createCompatibleImage( w, h, Transparency.OPAQUE );
			Graphics2D g2d = result.createGraphics();
			g2d.setColor( new Color( 150, 150, 200 ) );
			g2d.fillRect( 0, 0, result.getWidth()-1, result.getHeight()-1 );
			g2d.dispose();
		}

		return result;
	}

	/**
	 * Returns graphics for use in DoorSprites.
	 *
	 * FTL 1.01-1.03.3: The Doors system had 3 levels.
	 * FTL 1.5.4+: The Doors system had 5 levels.
	 *
	 * The result will be cached.
	 */
	@Override
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
	 * It will be a black outline with white fill, in need of tinting
	 / afterward.
	 *
	 * The result will be cached.
	 */
	@Override
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
	@Override
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

	/**
	 * Returns graphics for use in FireSprites.
	 *
	 * Typical innerPath: "img/effects/fire_L1_strip8.png".
	 */
	@Override
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
	public BufferedImage readResourceImage( String innerPath ) throws FileNotFoundException, IOException {
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
	public BufferedImage createDummyImage( int width, int height ) {
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
