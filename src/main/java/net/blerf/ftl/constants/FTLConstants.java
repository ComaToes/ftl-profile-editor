package net.blerf.ftl.constants;

import java.util.List;

import net.blerf.ftl.parser.SavedGameParser.CrewType;
import net.blerf.ftl.parser.SavedGameParser.StationDirection;
import net.blerf.ftl.parser.SavedGameParser.SystemType;


public interface FTLConstants {

	// ShipState constants.

	int getMaxReservePoolCapacity();


	// SystemState constants.

	/**
	 * Returns the bonus system bars produced by a Battery system.
	 *
	 * @param batterySystemCapacity the capacity of the system itself (its level)
	 */
	int getBatteryPoolCapacity( int batterySystemCapacity );

	int getMaxIonizedBars();


	// CrewState constants.

	List<CrewType> getCrewTypes();

	int getMasteryIntervalPilot( String race );
	int getMasteryIntervalEngine( String race );
	int getMasteryIntervalShield( String race );
	int getMasteryIntervalWeapon( String race );
	int getMasteryIntervalRepair( String race );
	int getMasteryIntervalCombat( String race );


	// System-related constants.

	List<SystemType> getSystemTypes();

	int getDefaultSystemRoomSlotSquare( SystemType systemType );

	StationDirection getDefaultSystemRoomSlotDirection( SystemType systemType );
}
