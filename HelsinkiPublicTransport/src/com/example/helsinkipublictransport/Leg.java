package com.example.helsinkipublictransport;

import java.util.ArrayList;
import java.util.List;

public class Leg {
	private String type, code, departureTime, arrivalTime, length;
	private int duration;
	private List<TransportLocation> locations;

	public Leg() {
		locations = new ArrayList<TransportLocation>();
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public List<TransportLocation> getTransportLocations() {
		return locations;
	}

	public void setTransportLocations(List<TransportLocation> locations) {
		this.locations = locations;
	}
	
	public void addTransportLocation(TransportLocation location) {
		locations.add(location);
	}

	public String getDepartureTime() {
		return departureTime;
	}

	public void setDepartureTime(String departureTime) {
		this.departureTime = departureTime;
	}

	public String getArrivalTime() {
		return arrivalTime;
	}

	public void setArrivalTime(String arrivalTime) {
		this.arrivalTime = arrivalTime;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public String getLength() {
		return length;
	}

	public void setLength(String length) {
		this.length = length;
	}
}
