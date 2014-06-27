package ru.dkiselev.osm.o5mreader.datasets;

public class BBOXds extends DataSet {

	private long x1, y1, x2, y2;
	
	public BBOXds(long x1, long y1, long x2, long y2) {
		
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
		
	}

	public long getX1() {
		return x1;
	}

	public long getY1() {
		return y1;
	}

	public long getX2() {
		return x2;
	}

	public long getY2() {
		return y2;
	}

}
