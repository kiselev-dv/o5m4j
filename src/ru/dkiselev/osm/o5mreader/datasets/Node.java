package ru.dkiselev.osm.o5mreader.datasets;


public class Node extends NWRDataSet {

	private double lon;
	private double lat;

	public Node(long id){
		super(id);
	}
	
	public double getLon() {
		return lon;
	}
	public void setLon(double lon) {
		this.lon = lon;
	}
	public double getLat() {
		return lat;
	}
	public void setLat(double lat) {
		this.lat = lat;
	}

	public void setLonLat(double lon, double lat) {
		this.lon = lon;
		this.lat = lat;
	}
	
	

}
