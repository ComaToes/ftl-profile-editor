package net.blerf.ftl.parser;

import java.io.Closeable;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.blerf.ftl.model.ShipLayout;
import net.blerf.ftl.xml.Achievement;
import net.blerf.ftl.xml.AugBlueprint;
import net.blerf.ftl.xml.BackgroundImageList;
import net.blerf.ftl.xml.Blueprints;
import net.blerf.ftl.xml.CrewNameList;
import net.blerf.ftl.xml.DroneBlueprint;
import net.blerf.ftl.xml.Encounters;
import net.blerf.ftl.xml.FTLEvent;
import net.blerf.ftl.xml.FTLEventList;
import net.blerf.ftl.xml.ShipBlueprint;
import net.blerf.ftl.xml.ShipEvent;
import net.blerf.ftl.xml.ShipEvents;
import net.blerf.ftl.xml.ShipChassis;
import net.blerf.ftl.xml.SystemBlueprint;
import net.blerf.ftl.xml.WeaponBlueprint;

public abstract class DataManager implements Closeable {

	private static DataManager instance = null;


	public static void setInstance( DataManager dataManager ) {
		instance = dataManager;
	}

	public static DataManager getInstance() {
		return instance;
	}

	public static DataManager get() {
		return instance;
	}


	@Override
	public void close() {
	}

	public InputStream getDataInputStream( String innerPath ) throws IOException {
		throw new UnsupportedOperationException();
	}

	public InputStream getResourceInputStream( String innerPath ) throws IOException {
		throw new UnsupportedOperationException();
	}

	public void extractDataDat( File extractDir ) throws IOException {
		throw new UnsupportedOperationException();
	}

	public void extractResourceDat( File extractDir ) throws IOException {
		throw new UnsupportedOperationException();
	}

	public List<Achievement> getAchievements() {
		throw new UnsupportedOperationException();
	}

	public AugBlueprint getAugment( String id ) {
		throw new UnsupportedOperationException();
	}

	public Map<String, AugBlueprint> getAugments() {
		throw new UnsupportedOperationException();
	}

	public DroneBlueprint getDrone( String id ) {
		throw new UnsupportedOperationException();
	}

	public Map<String, DroneBlueprint> getDrones() {
		throw new UnsupportedOperationException();
	}

	public SystemBlueprint getSystem( String id ) {
		throw new UnsupportedOperationException();
	}

	public WeaponBlueprint getWeapon( String id ) {
		throw new UnsupportedOperationException();
	}

	public Map<String, WeaponBlueprint> getWeapons() {
		throw new UnsupportedOperationException();
	}

	public ShipBlueprint getShip( String id ) {
		throw new UnsupportedOperationException();
	}

	public Map<String, ShipBlueprint> getShips() {
		throw new UnsupportedOperationException();
	}

	public Map<String, ShipBlueprint> getAutoShips() {
		throw new UnsupportedOperationException();
	}

	public List<ShipBlueprint> getPlayerShips() {
		throw new UnsupportedOperationException();
	}

	public List<Achievement> getShipAchievements( ShipBlueprint ship ) {
		throw new UnsupportedOperationException();
	}

	public List<Achievement> getGeneralAchievements() {
		throw new UnsupportedOperationException();
	}

	public ShipLayout getShipLayout( String id ) {
		throw new UnsupportedOperationException();
	}

	public ShipChassis getShipChassis( String id ) {
		throw new UnsupportedOperationException();
	}

	public boolean getCrewSex() {
		throw new UnsupportedOperationException();
	}

	public String getCrewName( boolean isMale ) {
		throw new UnsupportedOperationException();
	}

	public FTLEvent getEventById( String id ) {
		throw new UnsupportedOperationException();
	}

	public FTLEventList getEventListById( String id ) {
		throw new UnsupportedOperationException();
	}

	public Map<String, Encounters> getEncounters() {
		throw new UnsupportedOperationException();
	}

	public ShipEvent getShipEventById( String id ) {
		throw new UnsupportedOperationException();
	}

	public Map<String, ShipEvent> getShipEvents() {
		throw new UnsupportedOperationException();
	}

	public Map<String, BackgroundImageList> getBackgroundImageLists() {
		throw new UnsupportedOperationException();
	}
}
