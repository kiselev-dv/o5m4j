package ru.dkiselev.osm.o5mreader.datasets;

public class FileTimestamp extends DataSet {

	private long timestamp;
	
	public FileTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public long getTimestamp() {
		return timestamp;
	}

}
