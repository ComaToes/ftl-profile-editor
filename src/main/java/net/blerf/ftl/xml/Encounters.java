package net.blerf.ftl.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import net.blerf.ftl.xml.FTLEvent;
import net.blerf.ftl.xml.FTLEventList;


@XmlRootElement(name="events")
@XmlAccessorType(XmlAccessType.FIELD)
public class Encounters {
	@XmlElement(name="event",required=false)
	private List<FTLEvent> events;

	@XmlElement(name="eventList",required=false)
	private List<FTLEventList> eventLists;

	public void setEvents( List<FTLEvent> events ) {
		this.events = events;
	}

	public List<FTLEvent> getEvents() {
		return events;
	}

	public void setEventLists( List<FTLEventList> eventLists ) {
		this.eventLists = eventLists;
	}

	public List<FTLEventList> getEventLists() {
		return eventLists;
	}

	/**
	 * Returns an Event with a given id.
	 *
	 * Events and EventLists share a namespace,
	 * so an id could belong to either.
	 */
	public FTLEvent getEventById( String id ) {
		if ( id == null || events == null ) return null;

		for ( FTLEvent tmpEvent : events ) {
			if ( id.equals(tmpEvent.getId()) ) return tmpEvent;
		}

		return null;
	}

	/**
	 * Returns an EventList with a given id.
	 *
	 * Events and EventLists share a namespace,
	 * so an id could belong to either.
	 */
	public FTLEventList getEventListById( String id ) {
		if ( id == null || eventLists == null ) return null;

		for ( FTLEventList tmpEventList : eventLists ) {
			if ( id.equals(tmpEventList.getId()) ) return tmpEventList;
		}

		return null;
	}
}
