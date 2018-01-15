package net.blerf.ftl.ui.floorplan;

import java.awt.image.BufferedImage;

import net.blerf.ftl.parser.SavedGameParser.CrewType;
import net.blerf.ftl.parser.SavedGameParser.DroneType;
import net.blerf.ftl.parser.SavedGameParser.SystemType;
import net.blerf.ftl.ui.floorplan.AnimAtlas;
import net.blerf.ftl.ui.floorplan.DoorAtlas;


public interface SpriteImageProvider {

	BufferedImage getDroneBodyImage( DroneType droneType, boolean playerControlled );

	BufferedImage getCrewBodyImage( CrewType crewType, boolean male, boolean playerControlled );

	DoorAtlas getDoorAtlas();

	BufferedImage getSystemRoomImage( SystemType systemType );

	AnimAtlas getBreachAtlas();

	AnimAtlas getFireAtlas();
}
