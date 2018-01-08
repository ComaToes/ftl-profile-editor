package net.blerf.ftl.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import net.blerf.ftl.xml.DefaultDeferredText;


@XmlRootElement( name = "augBlueprint" )
@XmlAccessorType( XmlAccessType.FIELD )
public class AugBlueprint {
	
	@XmlAttribute( name = "name" )
	private String id;

	private DefaultDeferredText title;
	private DefaultDeferredText desc;

	@XmlElement( name = "bp" )
	private int bp;  // TODO: Rename this.

	private int cost;
	private int rarity;
	private boolean stackable;
	private float value;


	public void setId( String id ) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setTitle( DefaultDeferredText title ) {
		this.title = title;
	}

	public DefaultDeferredText getTitle() {
		return title;
	}

	public void setDescription( DefaultDeferredText desc ) {
		this.desc = desc;
	}

	public DefaultDeferredText getDescription() {
		return desc;
	}

	public void setBP( int bp ) {
		this.bp = bp;
	}

	public int getBP() {
		return bp;
	}

	public void setCost( int cost ) {
		this.cost = cost;
	}

	public int getCost() {
		return cost;
	}

	public void setRarity( int rarity ) {
		this.rarity = rarity;
	}

	public int getRarity() {
		return cost;
	}

	public void setStackable( boolean b ) {
		stackable = b;
	}

	public boolean isStackable() {
		return stackable;
	}

	public void setValue( float n ) {
		value = n;
	}

	public float getValue() {
		return value;
	}

	@Override
	public String toString() {
		return ""+title;
	}
}
