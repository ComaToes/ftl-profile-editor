package net.blerf.ftl.ui.floorplan;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.Map;


public class AnimAtlas {

	BufferedImage sheetImage;
	int frameWidth;
	int frameHeight;
	Map<String, Point[]> framesetsMap;


	public AnimAtlas( BufferedImage sheetImage, int frameWidth, int frameHeight, Map<String, Point[]> framesetsMap ) {
		this.sheetImage = sheetImage;
		this.frameWidth = frameWidth;
		this.frameHeight = frameHeight;
		this.framesetsMap = framesetsMap;
	}

	public BufferedImage getSheetImage() {
		return sheetImage;
	}

	public Map<String, Point[]> getFramesetsMap() {
		return framesetsMap;
	}

	public int getFrameWidth() {
		return frameWidth;
	}

	public int getFrameHeight() {
		return frameHeight;
	}

	/**
	 * Returns the top-left points of each frame in an Anim, or null.
	 *
	 * Combine with framwWidth and frameHeight.
	 */
	public Point[] getFrameset( String animId ) {
		return framesetsMap.get( animId );
	}
}
