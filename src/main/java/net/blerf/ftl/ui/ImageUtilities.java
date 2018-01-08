package net.blerf.ftl.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.font.LineMetrics;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.awt.image.RescaleOp;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.blerf.ftl.constants.Difficulty;
import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.ui.IconCycleButton;


public class ImageUtilities {

	private static final Logger log = LoggerFactory.getLogger( ImageUtilities.class );

	private static final int maxIconWidth = 64;
	private static final int maxIconHeight = 64;

	private static BufferedImage lockImage = null;

	private static GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	private static GraphicsDevice gs = ge.getDefaultScreenDevice();
	private static GraphicsConfiguration gc = gs.getDefaultConfiguration();


	private static Icon dummyIcon = new Icon() {
		@Override
		public int getIconHeight() { return maxIconHeight; }

		@Override
		public int getIconWidth() { return maxIconWidth; }

		@Override
		public void paintIcon( Component c, Graphics g, int x, int y ) {}
	};


	public static int getMaxIconWidth() { return maxIconWidth; }

	public static int getMaxIconHeight() { return maxIconHeight; }

	/**
	 * Sets a small lock image to use when creating cycle buttons.
	 */
	public static void setLockImage( BufferedImage image ) { lockImage = image; }


	/**
	 * Constructs a blank two-state button that ignores clicks.
	 */
	public static IconCycleButton createDummyCycleButton() {
		IconCycleButton result = new IconCycleButton( new Icon[] {dummyIcon, dummyIcon} );
		result.setBorder( BorderFactory.createCompoundBorder( result.getBorder(), BorderFactory.createEtchedBorder() ) );
		result.setEnabled( false );
		return result;
	}


	/**
	 * Constructs a multi-state button.
	 *
	 * The button can either toggle between locked and unlocked, or cycle
	 * through locked/easy/normal/hard.
	 *
	 * @param baseImagePath the innerPath of an image
	 * @param cycleDifficulty
	 */
	public static IconCycleButton createCycleButton( String baseImagePath, boolean cycleDifficulty ) {
		BufferedImage origImage = getProportionallyScaledImage( baseImagePath, maxIconWidth, maxIconHeight, null );

		BufferedImage baseImage;
		if ( origImage.getWidth() == maxIconWidth && origImage.getHeight() == maxIconHeight ) {
			baseImage = origImage;
		}
		else {
			BufferedImage paddedImage = gc.createCompatibleImage( maxIconWidth, maxIconHeight, Transparency.TRANSLUCENT );
			Graphics2D paddedG = paddedImage.createGraphics();
			int padOffsetX = (paddedImage.getWidth() - origImage.getWidth()) / 2;
			int padOffsetY = (paddedImage.getHeight() - origImage.getHeight()) / 2;
			paddedG.drawImage( origImage, padOffsetX, padOffsetY, null );
			paddedG.dispose();
			baseImage = paddedImage;
		}

		return createCycleButton( baseImage, cycleDifficulty );
	}


	public static IconCycleButton createCycleButton( BufferedImage baseImage, boolean cycleDifficulty ) {

		// Create a darkened image with a small lock over the center.
		BufferedImage lockedImage = gc.createCompatibleImage( baseImage.getWidth(), baseImage.getHeight(), Transparency.TRANSLUCENT );
		Graphics2D lockedG = lockedImage.createGraphics();
		lockedG.drawImage( baseImage, 0, 0, null );
		lockedG.setColor( new Color( 0, 0, 0, 150 ) );
		lockedG.fillRect( 0, 0, baseImage.getWidth(), baseImage.getHeight() );
		if ( lockImage != null ) {
			int lockOffsetX = (baseImage.getWidth() - lockImage.getWidth()) / 2;
			int lockOffsetY = (baseImage.getHeight() - lockImage.getHeight()) / 2;
			lockedG.drawImage( lockImage, lockOffsetX, lockOffsetY, null );
		}
		lockedG.dispose();

		String[] labels = null;
		if ( cycleDifficulty == true ) {                  // Locked / Easy / Normal / Hard.
			Difficulty[] difficulties = Difficulty.values();
			labels = new String[ difficulties.length ];
			for ( int i=difficulties.length-1; i >= 0; i-- ) {
				labels[i] = difficulties[i].toString();
			}
		}
		else {                                            // Locked / Unlocked.
			labels = new String[] { null };
		}

		ImageIcon[] icons = new ImageIcon[ 1+labels.length ];
		icons[0] = new ImageIcon( lockedImage );

		// Create the other icons, drawing any non-null labels.
		for ( int i=0; i < labels.length; i++ ) {
			String label = labels[i];
			BufferedImage tempImage = gc.createCompatibleImage( baseImage.getWidth(), baseImage.getHeight(), Transparency.TRANSLUCENT );
			Graphics2D tempG = tempImage.createGraphics();
			tempG.drawImage( baseImage, 0, 0, null );
			if ( label != null ) {
				LineMetrics labelMetrics = tempG.getFontMetrics().getLineMetrics( label, tempG );
				int labelWidth = tempG.getFontMetrics().stringWidth( label );
				int labelHeight = (int)labelMetrics.getAscent() + (int)labelMetrics.getDescent();
				int labelX = tempImage.getWidth()/2 - labelWidth/2;
				int labelY = tempImage.getHeight() - (int)labelMetrics.getDescent();
				tempG.setColor( Color.BLACK );
				tempG.fillRect( labelX-4, tempImage.getHeight() - labelHeight, labelWidth+8, labelHeight );
				tempG.setColor( Color.WHITE );
				tempG.drawString( label, labelX, labelY );
			}
			tempG.dispose();
			icons[1+i] = new ImageIcon( tempImage );
		}

		return new IconCycleButton( icons );
	}


	/**
	 * Returns an image from a class loader's getResource(), or null.
	 */
	public static BufferedImage getBundledImage( String name, ClassLoader classLoader ) {
		BufferedImage result = null;

		java.net.URL imageUrl = classLoader.getResource( name );
		if ( imageUrl != null ) {
			try {
				result = ImageIO.read( imageUrl );
			}
			catch ( IOException e ) {
				log.error( "Error reading bundled image: "+ name );
			}
		}
		else {
			log.error( "Could not find bundled image: "+ name );
		}

		return result;
	}


	/**
	 * Gets an image, stretching if necessary, and caches the result.
	 *
	 * If something goes wrong, a dummy image will be created with the
	 * expected dimensions.
	 *
	 * If the dimensions are negative, the original unscaled image
	 * will be returned if possible, or the absolute values will be
	 * used for the dummy image.
	 *
	 * @param innerPath
	 * @param w
	 * @param h
	 * @param cachedImages an existing cache to use, or null for no caching
	 */
	public static BufferedImage getScaledImage( String innerPath, int w, int h, Map<String, Map<Rectangle, BufferedImage>> cachedImages ) {
		Rectangle keyRect = new Rectangle( 0, 0, w, h );
		BufferedImage result = null;
		Map<Rectangle, BufferedImage> cacheMap = null;

		if ( cachedImages != null ) {
			cacheMap = cachedImages.get( innerPath );
			if ( cacheMap != null ) result = cacheMap.get( keyRect );
			if ( result != null ) return result;
			log.trace( "Image not in cache, loading and scaling...: "+ innerPath );
		}

		InputStream in = null;
		try {
			in = DataManager.get().getResourceInputStream( innerPath );
			BufferedImage origImage = ImageIO.read(in);

			if ( w <= 0 || h <= 0 || (origImage.getWidth() == w && origImage.getHeight() == h) ) {
				result = origImage;
			} else {
				BufferedImage scaledImage = gc.createCompatibleImage( w, h, Transparency.TRANSLUCENT );
				Graphics2D g2d = scaledImage.createGraphics();
				g2d.setRenderingHint( RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR );
				g2d.drawImage( origImage, 0, 0, w, h, null );
				g2d.dispose();
				result = scaledImage;
			}
		}
		catch ( RasterFormatException e ) {
			log.error( "Failed to load and scale image: "+ innerPath, e );
		}
		catch ( FileNotFoundException e ) {
			log.error( String.format( "Failed to load and scale image (\"%s\"): innerPath was not found", innerPath ) );
		}
		catch ( IOException e ) {
			log.error( "Failed to load and scale image: "+ innerPath, e );
		}
		finally {
			try {if ( in != null ) in.close();}
			catch ( IOException e ) {}
		}

		if ( result == null ) {  // Guarantee a returned image, with a stand-in.
			w = Math.abs( w );
			h = Math.abs( h );
			result = gc.createCompatibleImage( w, h, Transparency.OPAQUE );
			Graphics2D g2d = result.createGraphics();
			g2d.setColor( new Color( 150, 150, 200 ) );
			g2d.fillRect( 0, 0, w-1, h-1 );
			g2d.dispose();
		}

		if ( cachedImages != null ) {
			if ( cacheMap == null ) {
				cacheMap = new HashMap<Rectangle, BufferedImage>();
				cachedImages.put( innerPath, cacheMap );
			}
			cacheMap.put( keyRect, result );
		}

		return result;
	}


	/**
	 * Gets an image, scaling if necessary to fit, and caches the result.
	 *
	 * If something goes wrong, a dummy image will be created with the
	 * expected dimensions.
	 *
	 * Both max dimensions must be positive.
	 *
	 * @param innerPath
	 * @param w
	 * @param h
	 * @param cachedImages an existing cache to use, or null for no caching
	 */
	public static BufferedImage getProportionallyScaledImage( String innerPath, int maxW, int maxH, Map<String, Map<Rectangle, BufferedImage>> cachedImages ) {
		Rectangle widthKeyRect = new Rectangle( 0, 0, maxW, 0 );
		Rectangle heightKeyRect = new Rectangle( 0, 0, 0, maxH );
		BufferedImage result = null;
		Map<Rectangle, BufferedImage> cacheMap = null;

		if ( cachedImages != null ) {
			cacheMap = cachedImages.get( innerPath );
			if ( cacheMap != null ) {
				// Look up the smaller axis first.
				Rectangle[] maxRects = ((maxW < maxH) ? new Rectangle[] {widthKeyRect, heightKeyRect} : new Rectangle[] {heightKeyRect, widthKeyRect});
				for ( Rectangle maxRect : maxRects ) {
					result = cacheMap.get( maxRect );
					if ( result != null ) return result;
				}
			}
			log.trace( "Image not in cache, loading and proportionally scaling...: "+ innerPath );
		}

		InputStream in = null;
		try {
			in = DataManager.get().getResourceInputStream( innerPath );
			BufferedImage origImage = ImageIO.read( in );
			int width = origImage.getWidth();
			int height = origImage.getHeight();

			if ( width > height ) {
				height /= width / maxW;
				width = maxW;
			} else {
				width /= height / maxH;
				height = maxH;
			}

			if ( origImage.getWidth() == width && origImage.getHeight() == height ) {
				result = origImage;
			}
			else {
				BufferedImage scaledImage = gc.createCompatibleImage( width, height, Transparency.TRANSLUCENT );
				Graphics2D g2d = scaledImage.createGraphics();
				g2d.setRenderingHint( RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR );
				g2d.drawImage( origImage, 0, 0, width, height, null );
				g2d.dispose();
				result = scaledImage;
			}
		}
		catch ( RasterFormatException e ) {
			log.error( "Failed to load and proportionally scale image: "+ innerPath, e );
		}
		catch ( FileNotFoundException e ) {
			log.error( String.format( "Failed to load and proportionally scale image (\"%s\"): innerPath was not found", innerPath ) );
		}
		catch ( IOException e ) {
			log.error( "Failed to load and proportionally scale image: "+ innerPath, e );
		}
		finally {
			try {if ( in != null ) in.close();}
			catch ( IOException e ) {}
		}

		if ( result == null ) {  // Guarantee a returned image, with a stand-in.
			result = gc.createCompatibleImage( maxW, maxH, Transparency.OPAQUE );
			Graphics2D g2d = result.createGraphics();
			g2d.setColor( new Color( 150, 150, 200 ) );
			g2d.fillRect( 0, 0, result.getWidth()-1, result.getHeight()-1 );
			g2d.dispose();
		}

		if ( cachedImages != null ) {
			if ( cacheMap == null ) {
				cacheMap = new HashMap<Rectangle, BufferedImage>();
				cachedImages.put( innerPath, cacheMap );
			}
			if ( maxW < maxH ) {
				cacheMap.put( widthKeyRect, result );
			} else {
				cacheMap.put( heightKeyRect, result );
			}
		}

		return result;
	}


	/**
	 * Gets a cropped area of an image and caches the result.
	 *
	 * If something goes wrong, a dummy image will be created with
	 * the expected dimensions.
	 *
	 * @param innerPath
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 * @param cachedImages an existing cache to use, or null for no caching
	 */
	public static BufferedImage getCroppedImage( String innerPath, int x, int y, int w, int h, Map<String, Map<Rectangle, BufferedImage>> cachedImages ) {
		Rectangle keyRect = new Rectangle( x, y, w, h );
		BufferedImage result = null;
		Map<Rectangle, BufferedImage> cacheMap = null;

		if ( cachedImages != null ) {
			cacheMap = cachedImages.get( innerPath );
			if ( cacheMap != null ) result = cacheMap.get( keyRect );
			if ( result != null ) return result;
			log.trace( "Image not in cache, loading and cropping...: "+ innerPath );
		}

		InputStream in = null;
		try {
			in = DataManager.get().getResourceInputStream( innerPath );
			BufferedImage bigImage = ImageIO.read( in );
			result = bigImage.getSubimage( x, y, w, h );
		}
		catch ( RasterFormatException e ) {
			log.error( "Failed to load and crop image: "+ innerPath, e );
		}
		catch ( FileNotFoundException e ) {
			log.error( String.format( "Failed to load and crop image (\"%s\"): innerPath was not found", innerPath ) );
		}
		catch ( IOException e ) {
			log.error( "Failed to load and crop image: "+ innerPath, e );
		}
		finally {
			try {if ( in != null ) in.close();}
			catch ( IOException e ) {}
		}

		if ( result == null ) {  // Guarantee a returned image, with a stand-in.
			result = gc.createCompatibleImage( w, h, Transparency.OPAQUE );
			Graphics2D g2d = result.createGraphics();
			g2d.setColor( new Color( 150, 150, 200 ) );
			g2d.fillRect( 0, 0, w-1, h-1 );
			g2d.dispose();
		}

		if ( cachedImages != null ) {
			if ( cacheMap == null ) {
				cacheMap = new HashMap<Rectangle, BufferedImage>();
				cachedImages.put( innerPath, cacheMap );
			}
			cacheMap.put( keyRect, result );
		}

		return result;
	}


	/**
	 * Applies a RescaleOp to the palette of an image, and caches the result.
	 *
	 * @param srcImage
	 * @param tint
	 * @param cachedTintedImages an existing cache to use, or null for no caching
	 */
	public static BufferedImage getTintedImage( BufferedImage srcImage, Tint tint, Map<BufferedImage, Map<Tint, BufferedImage>> cachedTintedImages ) {
		BufferedImage result = null;
		Map<Tint, BufferedImage> cacheMap = null;

		if ( cachedTintedImages != null ) {
			cacheMap = cachedTintedImages.get( srcImage );
			if ( cacheMap != null ) result = cacheMap.get( tint );
			if ( result != null ) return result;
		}

		BufferedImage canvas = gc.createCompatibleImage( srcImage.getWidth(), srcImage.getHeight(), Transparency.TRANSLUCENT );
		Graphics2D g2d = canvas.createGraphics();
		g2d.drawImage( srcImage, 0, 0, null );
		g2d.dispose();
		RescaleOp op = new RescaleOp( tint.scaleFactors, tint.offsets, null );
		result = op.filter( canvas, null );

		if ( cachedTintedImages != null ) {
			if ( cacheMap == null ) {
				cacheMap = new HashMap<Tint, BufferedImage>();
				cachedTintedImages.put( srcImage, cacheMap );
			}
			cacheMap.put( tint, result );
		}

		return result;
	}


	/**
	 * Crops transparent pixels from an image, and caches the result.
	 *
	 * @param srcImage
	 * @param cachedTrimmedImages an existing cache to use, or null for no caching
	 */
	public static BufferedImage getTrimmedImage( BufferedImage srcImage, Map<BufferedImage, BufferedImage> cachedTrimmedImages ) {
		BufferedImage result = null;

		if ( cachedTrimmedImages != null ) {
			result = cachedTrimmedImages.get( srcImage );
			if ( result != null ) return result;
		}

		result = srcImage;

		// Shrink the crop area until non-transparent pixels are hit.
		int origW = srcImage.getWidth(), origH = srcImage.getHeight();
		int lowX = Integer.MAX_VALUE, lowY = Integer.MAX_VALUE;
		int highX = -1, highY = -1;
		for ( int testY=0; testY < origH; testY++ ) {
			for ( int testX=0; testX < origW; testX++ ) {
				int pixel = result.getRGB( testX, testY );
				int alpha = (pixel >> 24) & 0xFF;  // 24:A, 16:R, 8:G, 0:B.
				if ( alpha != 0 ) {
					if ( testX > highX ) highX = testX;
					if ( testY > highY ) highY = testY;
					if ( testX < lowX ) lowX = testX;
					if ( testY < lowY ) lowY = testY;
				}
			}
		}
		log.trace( String.format( "Image Trimmed to Bounds: %d,%d %dx%d", lowX, lowY, highX, highY ) );
		if ( lowX >= 0 && lowY >= 0 && highX < origW && highY < origH && lowX < highX && lowY < highY ) {
			result = result.getSubimage( lowX, lowY, highX-lowX+1, highY-lowY+1 );
		}

		if ( cachedTrimmedImages != null ) {
			cachedTrimmedImages.put( srcImage, result );
		}

		return result;
	}



	public static class Tint {
		public float[] scaleFactors;
		public float[] offsets;

		public Tint( float[] scaleFactors, float[] offsets ) {
			this.scaleFactors = scaleFactors;
			this.offsets = offsets;
		}

		public boolean equals( Object o ) {
			if ( o == this ) return true;
			if ( o instanceof Tint ) return this.hashCode() == o.hashCode();
			return false;
		}

		public int hashCode() {
			return ( java.util.Arrays.hashCode( scaleFactors ) ^ java.util.Arrays.hashCode( offsets ) );
		}
	}
}
