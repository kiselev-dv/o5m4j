package ru.dkiselev.osm.o5mreader.datasets;

public class Jump extends DataSet {

	private long next;
	private long prev;
	
	public Jump (long next, long prev) {
		this.next = next;
		this.prev = prev;
	}

	public long getNext() {
		return next;
	}

	public long getPrev() {
		return prev;
	}
	
}
