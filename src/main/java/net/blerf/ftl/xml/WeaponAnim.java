package net.blerf.ftl.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import net.blerf.ftl.xml.AnimSpec;
import net.blerf.ftl.xml.Offset;


@XmlRootElement(name="weaponAnim")
@XmlAccessorType(XmlAccessType.FIELD)
public class WeaponAnim {

	@XmlAttribute(name="name")
	private String id;

	@XmlElement(name="sheet")
	private String sheetId;

	@XmlElement(name="desc")
	private AnimSpec spec;

	private int chargedFrame;
	private int fireFrame;

	private Offset firePoint;
	private Offset mountPoint;

	@XmlElement(name="delayChargeAnim",required=false)
	private Float chargeDelay;

	@XmlElement(name="chargeImage",required=false)
	private String chargeImagePath;

	public void setId( String id ) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setSheetId( String sheetId ) {
		this.sheetId = sheetId;
	}

	public String getSheetId() {
		return sheetId;
	}

	public void setSpec( AnimSpec spec ) {
		this.spec = spec;
	}

	public AnimSpec getSpec() {
		return spec;
	}

	public void setChargedFrame( int chargedFrame ) {
		this.chargedFrame = chargedFrame;
	}

	public int getChargedFrame() {
		return chargedFrame;
	}

	public void setFireFrame( int fireFrame ) {
		this.fireFrame = fireFrame;
	}

	public int getFireFrame() {
		return fireFrame;
	}

	public void setFirePoint( Offset firePoint ) {
		this.firePoint = firePoint;
	}

	public Offset getFirePoint() {
		return firePoint;
	}

	public void setMountPoint( Offset mountPoint ) {
		this.mountPoint = mountPoint;
	}

	public Offset getMountPoint() {
		return mountPoint;
	}

	public void setChargeDelay( Float chargeDelay ) {
		this.chargeDelay = chargeDelay;
	}

	public Float getChargeDelay() {
		return chargeDelay;
	}

	public void setChargeImagePath( String chargeImagePath ) {
		this.chargeImagePath = chargeImagePath;
	}

	public String getChargeImagePath() {
		return chargeImagePath;
	}

	@Override
	public String toString() {
		return ""+id;
	}
}
