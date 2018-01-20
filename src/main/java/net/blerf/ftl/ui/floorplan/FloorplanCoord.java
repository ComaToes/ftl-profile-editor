package net.blerf.ftl.ui.floorplan;


public class FloorplanCoord {

	public final int bundleId;
	public final int roomId;
	public final int squareId;
	public final int hash;


	public FloorplanCoord( int bundleId, int roomId, int squareId ) {
		this.bundleId = bundleId;
		this.roomId = roomId;
		this.squareId = squareId;

		// Pre-calculate the hashCode, since this is immutable.
		int z = 79;
		int sum = 0;
		sum = 37 * sum + bundleId;
		sum = 37 * sum + roomId;
		sum = 37 * sum + squareId;
		hash = sum;
	}

	@Override
	public boolean equals( Object o ) {
		if ( !(o instanceof FloorplanCoord) ) return false;
		FloorplanCoord otherCoord = (FloorplanCoord)o;
		return (bundleId == otherCoord.bundleId && roomId == otherCoord.roomId && squareId == otherCoord.squareId);
	}

	/**
	 * Returns a hash code value for the object.
	 *
	 * The algorithm is copied from the book "Effective Java", suggested here:
	 *   https://stackoverflow.com/a/113600
	 */
	@Override
	public int hashCode() {
		return hash;
	}
}
