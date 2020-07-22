package net.blerf.ftl.ui.floorplan;

import java.awt.Point;
import java.awt.image.BufferedImage;


public class DoorAtlas {

	BufferedImage sheetImage;
	int frameWidth;
	int frameHeight;
	Point[][] framesets;


	public DoorAtlas( BufferedImage sheetImage, int frameWidth, int frameHeight, Point[][] framesets ) {
		this.sheetImage = sheetImage;
		this.frameWidth = frameWidth;
		this.frameHeight = frameHeight;
		this.framesets = framesets;
	}

	public BufferedImage getSheetImage() {
		return sheetImage;
	}

	public int getFrameWidth() {
		return frameWidth;
	}

	public int getFrameHeight() {
		return frameHeight;
	}

	/**
	 * Returns the top-left points of each frame in an opening anim.
	 *
	 * Combine with framwWidth and frameHeight.
	 */
	public Point[] getOpeningFrameset( int level ) {
		if ( level >= framesets.length ) return null;

		return framesets[level];
	}
}
