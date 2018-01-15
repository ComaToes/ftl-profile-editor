package net.blerf.ftl.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import net.blerf.ftl.xml.Anim;
import net.blerf.ftl.xml.AnimSheet;
import net.blerf.ftl.xml.WeaponAnim;


@XmlRootElement(name = "animations")
@XmlAccessorType(XmlAccessType.FIELD)
public class Animations {

	@XmlElement(name = "animSheet", required = false)
	private List<AnimSheet> sheets;

	@XmlElement(name = "anim", required = false)
	private List<Anim> anims;

	@XmlElement(name = "weaponAnim", required = false)
	private List<WeaponAnim> weaponAnims;


	public void setSheets( List<AnimSheet> sheets ) {
		this.sheets = sheets;
	}

	public List<AnimSheet> getSheets() {
		return sheets;
	}

	public void setAnims( List<Anim> anims ) {
		this.anims = anims;
	}

	public List<Anim> getAnims() {
		return anims;
	}

	public void setWeaponAnims( List<WeaponAnim> weaponAnims ) {
		this.weaponAnims = weaponAnims;
	}

	public List<WeaponAnim> getWeaponAnims() {
		return weaponAnims;
	}

	/**
	 * Returns an AnimSheet with a given id.
	 *
	 * AnimSheets have a separate namespace from Anims and WeaponAnims.
	 */
	public AnimSheet getSheetById( String id ) {
		if ( id == null || sheets == null ) return null;

		AnimSheet result = null;
		for ( AnimSheet tmpSheet : sheets ) {
			if ( id.equals(tmpSheet.getId()) ) result = tmpSheet;
		}

		return result;
	}

	/**
	 * Returns all Anims that appear in a given AnimSheet.
	 */
	public List<Anim> getAnimsBySheetId( String id ) {
		if ( id == null || anims == null ) return null;

		List<Anim> results = new ArrayList<Anim>();
		for ( Anim tmpAnim : anims ) {
			if ( id.equals( tmpAnim.getSheetId() ) ) results.add( tmpAnim );
		}

		return results;
	}

	/**
	 * Returns an Anim with a given id.
	 *
	 * AnimSheets have a separate namespace from Anims and WeaponAnims.
	 */
	public Anim getAnimById( String id ) {
		if ( id == null || anims == null ) return null;

		Anim result = null;
		for ( Anim tmpAnim : anims ) {
			if ( id.equals(tmpAnim.getId()) ) result = tmpAnim;
		}

		return result;
	}

	/**
	 * Returns a WeaponAnim with a given id.
	 *
	 * AnimSheets have a separate namespace from Anims and WeaponAnims.
	 */
	public WeaponAnim getWeaponAnimById( String id ) {
		if ( id == null || weaponAnims == null ) return null;

		WeaponAnim result = null;
		for ( WeaponAnim tmpWeaponAnim : weaponAnims ) {
			if ( id.equals(tmpWeaponAnim.getId()) ) result = tmpWeaponAnim;
		}

		return result;
	}
}
