package ru.dkiselev.osm.o5mreader.datasets;

public class SyncDS extends DataSet {
	
	private long skipped;
	
	public SyncDS(long skipped) {
		this.skipped = skipped;
	}

	public long getSkipped() {
		return skipped;
	}
	
}
