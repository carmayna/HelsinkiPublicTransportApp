package com.example.helsinkipublictransport;

import java.util.ArrayList;
import java.util.List;

public class Route {
	private String origin, destination, departureTime, arrivalTime;
	private int duration; // in minutes
	private List<Leg> legs;
	
	public Route() {
		legs = new ArrayList<Leg>();
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public List<Leg> getLegs() {
		return legs;
	}

	public void setLegs(List<Leg> legs) {
		this.legs = legs;
	}
	
	public void addLeg(Leg leg) {
		legs.add(leg);
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
}
