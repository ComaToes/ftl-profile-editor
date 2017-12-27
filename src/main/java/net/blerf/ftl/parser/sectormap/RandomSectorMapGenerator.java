package net.blerf.ftl.parser.sectormap;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.blerf.ftl.parser.sectormap.GeneratedBeacon;
import net.blerf.ftl.parser.sectormap.GeneratedSectorMap;
import net.blerf.ftl.parser.random.RandRNG;


/**
 * A generator to create a GeneratedSectorMap object from a seed as FTL would.
 *
 * The rebelFleetFudge will be set, though the SavedGameState's value overrides
 * it.
 *
 * This class iterates over a rectangular grid randomly skipping cells. Each
 * beacon's throbTicks will be random. Each beacon's x/y location will be
 * random within cells. The result will not be rectangular. Maintaining the
 * grid should not be necessary.
 *
 * FTL 1.03.3's map was 530x346, with its origin at (438, 206) (on a 1286x745
 * screenshot including +3,+22 padding from the OS Window decorations).
 *
 * FTL 1.5.4 changed the algorithm from what had been used previously. It also
 * enlarged the map's overall dimensions: 640x488, with its origin at
 * (389, 146) (on a 1286x745 screenshot including +3,+22 padding).
 *
 * @see net.blerf.ftl.parser.SavedGameParser.SavedGameState#getFileFormat()
 */
public class RandomSectorMapGenerator {

	private static final Logger log = LoggerFactory.getLogger( RandomSectorMapGenerator.class );

	/**
	 * The threshold for re-rolling a map with disconnected beacons.
	 *
	 * This value is a guess, but it seems to work.
	 *
	 * @see #calculateIsolation(GeneratedSectorMap)
	 */
	public static final double ISOLATION_THRESHOLD = 150d;


	/**
	 * Generates the sector map.
	 *
	 * Note: The RNG needs to be seeded immediately before calling this method.
	 *
	 * @see net.blerf.ftl.parser.SavedGameParser.SavedGameState#getFileFormat()
	 * @throws IllegalStateException if a valid map isn't generated after 50 attempts
	 */
	public GeneratedSectorMap generateSectorMap( RandRNG rng, int fileFormat ) {

		if ( fileFormat == 2 ) {
			// FTL 1.01-1.03.3

			int columns = 6;  // TODO: Magic numbers.
			int rows = 4;

			GeneratedSectorMap genMap = new GeneratedSectorMap();
			genMap.setPreferredSize( new Dimension( 530, 346 ) );  // TODO: Magic numbers.

			int n;

			n = rng.rand();
			genMap.setRebelFleetFudge( new Integer( n % 294 + 50 ) );

			List<GeneratedBeacon> genBeaconList = new ArrayList<GeneratedBeacon>();
			int skipInclusiveCount = 0;
			int z = 0;

			for ( int c=0; c < columns; c++ ) {

				for ( int r=0; r < rows; r++ ) {
					n = rng.rand();
					if ( n % 5 == 0 ) {
						z++;

						if ( (int)(skipInclusiveCount / z) > 4 ) {  // Skip this cell.
							skipInclusiveCount++;
							continue;
						}
					}
					GeneratedBeacon genBeacon = new GeneratedBeacon();

					n = rng.rand();
					genBeacon.setThrobTicks( n % 2001 );

					n = rng.rand();
					int locX = n % 66 + c*86 + 10;
					n = rng.rand();
					int locY = n % 66 + r*86 + 10;

					if ( c == 5 && locX > 450 ) {  // Yes, this really was FTL's logic.
						locX -= 10;
					}
					if ( r == 3 && locY > 278 ) {  // Yes, this really was FTL's logic.
						locY -= 10;
					}

					genBeacon.setLocation( locX, locY );

					genBeaconList.add( genBeacon );
					skipInclusiveCount++;
				}
			}

			genMap.setGeneratedBeaconList( genBeaconList );

			return genMap;
		}
		else if ( fileFormat == 7 || fileFormat == 8 || fileFormat == 9 ) {
			// FTL 1.5.4/1.5.10, 1.5.12, or 1.5.13.

			int columns = 6;  // TODO: Magic numbers.
			int rows = 4;

			GeneratedSectorMap genMap = new GeneratedSectorMap();
			genMap.setPreferredSize( new Dimension( 640, 488 ) );  // TODO: Magic numbers.

			int n;
			int generations = 0;

			n = rng.rand();
			genMap.setRebelFleetFudge( new Integer( n % 250 + 50 ) );

			while ( generations < 50 ) {
				List<GeneratedBeacon> genBeaconList = new ArrayList<GeneratedBeacon>();
				int skipInclusiveCount = 0;
				int z = 0;

				for ( int c=0; c < columns; c++ ) {

					for ( int r=0; r < rows; r++ ) {
						n = rng.rand();
						if ( n % 5 == 0 ) {
							z++;

							if ( (int)(skipInclusiveCount / z) > 4 ) {  // Skip this cell.
								skipInclusiveCount++;
								continue;
							}
						}
						GeneratedBeacon genBeacon = new GeneratedBeacon();

						n = rng.rand();
						genBeacon.setThrobTicks( n % 2001 );

						n = rng.rand();
						int locX = n % 90 + c*110 + 10;
						n = rng.rand();
						int locY = n % 90 + r*110 + 10;
						locY = Math.min( locY, 415 );

						if ( c > 3 && r == 0 ) {  // Yes, this really was FTL's logic.
							locY = Math.max( locY, 30 );
						}

						genBeacon.setLocation( locX, locY );

						genBeaconList.add( genBeacon );
						skipInclusiveCount++;
					}
				}

				genMap.setGeneratedBeaconList( genBeaconList );
				generations++;

				double isolation = calculateIsolation( genMap );
				if ( isolation > ISOLATION_THRESHOLD ) {
					log.info( String.format( "Re-rolling sector map because attempt #%d has isolated beacons (threshold dist %5.2f): %5.2f", generations, ISOLATION_THRESHOLD, isolation ) );
					genMap.setGeneratedBeaconList( null );
				}
				else {
					break;  // Success!
				}
			}

			if ( genMap.getGeneratedBeaconList() == null ) {
				throw new IllegalStateException( String.format( "No valid map was produced after %d attempts!?", generations ) );
			}

			return genMap;
		}
		else {
			throw new UnsupportedOperationException( String.format( "Random sector maps for fileFormat (%d) have not been implemented", fileFormat ) );
		}
	}

	/**
	 * Returns the most isolated beacon's distance to its nearest neighbor.
	 *
	 * FTL 1.5.4 introduced a check to re-generate invalid maps. The changelog
	 * said, "Maps will no longer have disconnected beacons, everything will be
	 * accessible."
	 *
	 * TODO: This code's a guess. The exact algorithm and threshold have not
	 * been verified, but it seems to work.
	 */
	public double calculateIsolation( GeneratedSectorMap genMap ) {
		double result = 0;

		List<GeneratedBeacon> genBeaconList = genMap.getGeneratedBeaconList();

		for ( int i=0; i < genBeaconList.size(); i++ ) {
			double minDist = 0d;
			boolean measured = false;

			for ( int j=0; j < genBeaconList.size(); j++ ) {
				if ( i == j ) continue;

				GeneratedBeacon a = genBeaconList.get( i );
				GeneratedBeacon b = genBeaconList.get( j );
				Point aLoc = a.getLocation();
				Point bLoc = b.getLocation();

				double d = Math.hypot( aLoc.x - bLoc.x, aLoc.y - bLoc.y );
				if ( !measured ) {
					minDist = d;
					measured = true;
				} else {
					minDist = Math.min( minDist, d );
				}
			}

			//if ( measured ) log.info( String.format( "%5.2f", minDist ) );

			if ( measured ) {
				result = Math.max( result, minDist );
			}
		}

		return result;
	}
}
