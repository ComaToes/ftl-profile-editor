package net.blerf.ftl.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="augBlueprint")
@XmlAccessorType(XmlAccessType.FIELD)
public class AugBlueprint {
	
	private String title;
	private String desc;
	private int cost, bp, rarity;
	private String stackable;
	private float value;

}
