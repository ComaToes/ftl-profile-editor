package net.blerf.ftl.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


/**
 * A simple (length,x,y) object for animations.
 *
 * @see net.blerf.ftl.xml.Anim
 * @see net.blerf.ftl.xml.WeaponAnim
 */
@XmlRootElement(name="desc")
@XmlAccessorType(XmlAccessType.FIELD)
public class AnimSpec {

	@XmlAttribute(name="length")
	public int frameCount;

	@XmlAttribute
	public int x;

	@XmlAttribute
	public int y;

	@Override
	public String toString() {
		return String.format("frames:%s, x:%d, y:%d", frameCount, x, y);
	}
}
