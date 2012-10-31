package net.blerf.ftl.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="shipChassis")
@XmlAccessorType(XmlAccessType.FIELD)
public class ShipChassis {
	@XmlElement(name="img")
	private ChassisImageBounds imageBounds;
	private WeaponMountList weaponMountList;

	@XmlRootElement(name="img")
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class ChassisImageBounds {
		@XmlAttribute
		public int x, y, w, h;
	}

	@XmlRootElement(name="weaponMounts")
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class WeaponMountList {
		private List<WeaponMount> mount;

		@XmlRootElement(name="mount")
		@XmlAccessorType(XmlAccessType.FIELD)
		public static class WeaponMount {
			public int x, y, gib;
			public boolean rotate, mirror;
			public String slide;
		}
	}

	@XmlRootElement(name="explosion")
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class Explosion {
		private List<Gib> gib;

		@XmlRootElement(name="gib")
		@XmlAccessorType(XmlAccessType.FIELD)
		public static class Gib {
			private FloatRange velocity;
			private FloatRange direction;
			private FloatRange angular;
			private int x, y;

			@XmlAccessorType(XmlAccessType.FIELD)
			public static class FloatRange {
				private float min, max;
			}
		}
	}

	public void setImageBounds( ChassisImageBounds imageBounds ) {
		this.imageBounds = imageBounds;
	}

	public ChassisImageBounds getImageBounds() {
		return imageBounds;
	}

	public void setWeaponMountList( WeaponMountList weaponMountList ) {
		this.weaponMountList = weaponMountList;
	}

	public WeaponMountList getWeaponMountList() {
		return weaponMountList;
	}
}
