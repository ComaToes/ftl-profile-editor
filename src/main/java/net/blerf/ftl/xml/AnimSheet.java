package net.blerf.ftl.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;


@XmlRootElement(name = "animSheet")
@XmlAccessorType(XmlAccessType.FIELD)
public class AnimSheet {

	@XmlAttribute(name = "name")
	private String id;

	@XmlAttribute(name = "w")
	private int width;

	@XmlAttribute(name = "h")
	private int height;

	@XmlAttribute(name = "fw")
	private int frameWidth;

	@XmlAttribute(name = "fh")
	private int frameHeight;

	@XmlValue
	private String innerPath;  // Relative to "img/" (the top-level dir is omitted).


	public void setId( String id ) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setWidth( int width ) {
		this.width = width;
	}

	public int getWidth() {
		return width;
	}

	public void setHeight( int height ) {
		this.height = height;
	}

	public int getHeight() {
		return height;
	}

	public void setFrameWidth( int frameWidth ) {
		this.frameWidth = frameWidth;
	}

	public int getFrameWidth() {
		return frameWidth;
	}

	public void setFrameHeight( int frameHeight ) {
		this.frameHeight = frameHeight;
	}

	public int getFrameHeight() {
		return frameHeight;
	}

	public void setInnerPath( String innerPath ) {
		this.innerPath = innerPath;
	}

	public String getInnerPath() {
		return innerPath;
	}

	@Override
	public String toString() {
		return ""+id;
	}
}
