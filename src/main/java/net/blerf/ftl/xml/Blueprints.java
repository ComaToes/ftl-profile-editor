package net.blerf.ftl.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement( name = "blueprints" )
@XmlAccessorType( XmlAccessType.FIELD )
public class Blueprints {

	@XmlElement( name = "blueprintList", required = false )
	private List<BlueprintList> blueprintLists = new ArrayList<BlueprintList>();

	@XmlElement( name = "crewBlueprint", required = false )
	private List<CrewBlueprint> crewBlueprints = new ArrayList<CrewBlueprint>();

	@XmlElement( name = "systemBlueprint", required = false )
	private List<SystemBlueprint> systemBlueprints = new ArrayList<SystemBlueprint>();

	@XmlElement( name = "weaponBlueprint", required = false )
	private List<WeaponBlueprint> weaponBlueprints = new ArrayList<WeaponBlueprint>();

	@XmlElement( name = "droneBlueprint", required = false )
	private List<DroneBlueprint> droneBlueprints = new ArrayList<DroneBlueprint>();

	@XmlElement( name = "augBlueprint", required = false )
	private List<AugBlueprint> augBlueprints = new ArrayList<AugBlueprint>();

	@XmlElement( name = "shipBlueprint", required = false )
	private List<ShipBlueprint> shipBlueprints = new ArrayList<ShipBlueprint>();


	public void setBlueprintLists( List<BlueprintList> blueprintLists ) {
		this.blueprintLists = blueprintLists;
	}

	public List<BlueprintList> getBlueprintLists() {
		return blueprintLists;
	}

	public void setCrewBlueprints( List<CrewBlueprint> crewBlueprints ) {
		this.crewBlueprints = crewBlueprints;
	}

	public List<CrewBlueprint> getCrewBlueprints() {
		return crewBlueprints;
	}

	public void setSystemBlueprints( List<SystemBlueprint> systemBlueprints ) {
		this.systemBlueprints = systemBlueprints;
	}

	public List<SystemBlueprint> getSystemBlueprints() {
		return systemBlueprints;
	}

	public void setWeaponBlueprints( List<WeaponBlueprint> weaponBlueprints ) {
		this.weaponBlueprints = weaponBlueprints;
	}

	public List<WeaponBlueprint> getWeaponBlueprints() {
		return weaponBlueprints;
	}

	public void setDroneBlueprints( List<DroneBlueprint> droneBlueprints ) {
		this.droneBlueprints = droneBlueprints;
	}

	public List<DroneBlueprint> getDroneBlueprints() {
		return droneBlueprints;
	}

	public void setAugBlueprints( List<AugBlueprint> augBlueprints ) {
		this.augBlueprints = augBlueprints;
	}

	public List<AugBlueprint> getAugBlueprints() {
		return augBlueprints;
	}

	public void setShipBlueprint( List<ShipBlueprint> shipBlueprints ) {
		this.shipBlueprints = shipBlueprints;
	}

	public List<ShipBlueprint> getShipBlueprints() {
		return shipBlueprints;
	}
}
