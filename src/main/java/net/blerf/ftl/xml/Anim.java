package net.blerf.ftl.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import net.blerf.ftl.xml.AnimSpec;


@XmlRootElement(name="anim")
@XmlAccessorType(XmlAccessType.FIELD)
public class Anim {

	@XmlAttribute(name="name")
	private String id;

	@XmlElement(name="sheet")
	private String sheetId;

	@XmlElement(name="desc")
	private AnimSpec spec;

	private float time;

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

	public void setAnimSpec( AnimSpec spec ) {
		this.spec = spec;
	}

	public AnimSpec getSpec() {
		return spec;
	}

	public void setTime( float time ) {
		this.time = time;
	}

	public float getTime() {
		return time;
	}

	@Override
	public String toString() {
		return ""+id;
	}
}
