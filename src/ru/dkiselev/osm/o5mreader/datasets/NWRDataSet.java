package ru.dkiselev.osm.o5mreader.datasets;

import java.util.Map;

import ru.dkiselev.osm.o5mreader.Pair;

public class NWRDataSet extends DataSet {
	
	public static enum BaseObjectTypes {
		NODE, WAY, RELATION
	}
	
	private long id;
	private long version;
	private long timestamp;
	private long changeset;
	private Pair<Integer, String> user;
	private Map<String, String> tags;

	public NWRDataSet(long id){
		this.id = id;
	}
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public long getChangeset() {
		return changeset;
	}

	public void setChangeset(long changeset) {
		this.changeset = changeset;
	}

	public Pair<Integer, String> getUser() {
		return user;
	}

	public void setUser(Pair<Integer, String> user) {
		this.user = user;
	}

	public Map<String, String> getTags() {
		return tags;
	}

	public void setTags(Map<String, String> tags) {
		this.tags = tags;
	}
}