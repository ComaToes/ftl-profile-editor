package net.blerf.ftl.constants;

import net.blerf.ftl.constants.FTLConstants;


/**
 * Constants for FTL 1.5.4.
 */
public class AdvancedFTLConstants implements FTLConstants {

	@Override
	public int getMaxReservePower() { return 25; }


	@Override
	public int getMaxIonizedBars() { return 9; }


	@Override
	public int getMasteryIntervalPilot() { return 13; }

	@Override
	public int getMasteryIntervalEngine() { return 13; }

	@Override
	public int getMasteryIntervalShield() { return 50; }

	@Override
	public int getMasteryIntervalWeapon() { return 58; }

	@Override
	public int getMasteryIntervalRepair() { return 16; }

	@Override
	public int getMasteryIntervalCombat() { return 7; }
}
