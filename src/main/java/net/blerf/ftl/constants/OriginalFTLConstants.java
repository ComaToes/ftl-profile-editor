package net.blerf.ftl.constants;

import net.blerf.ftl.constants.FTLConstants;


/**
 * Constants for FTL 1.01-1.03.3.
 */
public class OriginalFTLConstants implements FTLConstants {

	@Override
	public int getMaxReservePower() { return 25; }


	@Override
	public int getMaxIonizedBars() { return 9; }


	@Override
	public int getMasteryIntervalPilot() { return 15; }

	@Override
	public int getMasteryIntervalEngine() { return 15; }

	@Override
	public int getMasteryIntervalShield() { return 55; }

	@Override
	public int getMasteryIntervalWeapon() { return 65; }

	@Override
	public int getMasteryIntervalRepair() { return 18; }

	@Override
	public int getMasteryIntervalCombat() { return 8; }
}
