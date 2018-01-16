package net.blerf.ftl.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import net.blerf.ftl.xml.ExplosionAdapter;
import net.blerf.ftl.xml.Offset;


@XmlRootElement( name = "shipChassis" )
@XmlAccessorType( XmlAccessType.FIELD )
public class ShipChassis {

	@XmlElement( name = "img" )
	private ChassisImageBounds imageBounds;

	@XmlElement( required = false )
	private Offsets offsets;  // FTL 1.5.4 introduced floor/cloak offsets.

	@XmlElementWrapper( name = "weaponMounts" )
	@XmlElement( name = "mount" )
	private List<WeaponMount> weaponMountList;

	private Explosion explosion;


	@XmlAccessorType( XmlAccessType.FIELD )
	public static class ChassisImageBounds {
		@XmlAttribute
		public int x, y, w, h;
	}

	@XmlAccessorType( XmlAccessType.FIELD )
	public static class Offsets {

		@XmlElement( name = "floor" )
		public Offset floorOffset;

		@XmlElement( name = "cloak" )
		public Offset cloakOffset;
	}

	@XmlAccessorType( XmlAccessType.FIELD )
	public static class WeaponMount {

		@XmlAttribute
		public int x, y, gib;

		@XmlAttribute
		public boolean rotate, mirror;

		@XmlAttribute
		public String slide;
	}

	@XmlJavaTypeAdapter( ExplosionAdapter.class )
	public static class Explosion {
		public List<Gib> gibs = new ArrayList<Gib>();
	}

	@XmlAccessorType( XmlAccessType.FIELD )
	public static class Gib {
		public FloatRange velocity;
		public FloatRange direction;
		public FloatRange angular;
		public int x, y;
	}

	@XmlAccessorType( XmlAccessType.FIELD )
	public static class FloatRange {
		@XmlAttribute
		public float min, max;
	}


	public void setImageBounds( ChassisImageBounds imageBounds ) {
		this.imageBounds = imageBounds;
	}

	public ChassisImageBounds getImageBounds() {
		return imageBounds;
	}

	public void setOffsets( Offsets offsets ) {
		this.offsets = offsets;
	}

	public Offsets getOffsets() {
		return offsets;
	}

	public void setWeaponMountList( List<WeaponMount> weaponMountList ) {
		this.weaponMountList = weaponMountList;
	}

	public List<WeaponMount> getWeaponMountList() {
		return weaponMountList;
	}

	public void setExplosion( Explosion explosion ) {
		this.explosion = explosion;
	}

	public Explosion getExplosion() {
		return explosion;
	}
}
