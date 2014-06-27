package ru.dkiselev.osm.o5mreader.datasets;

import java.util.List;


public class Way extends NWRDataSet {

	private List<Long> nodes;

	public Way(long id) {
		super(id);
	}

	public List<Long> getNodes() {
		return nodes;
	}

	public void setNodes(List<Long> nodes) {
		this.nodes = nodes;
	}
	
}
