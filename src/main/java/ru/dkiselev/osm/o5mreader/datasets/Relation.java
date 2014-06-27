package ru.dkiselev.osm.o5mreader.datasets;

import java.util.List;

public class Relation extends NWRDataSet {
	
	public static class RelationReference {
		
		private BaseObjectTypes refObjectType;
		private long refObjectId;
		private String role;

		public BaseObjectTypes getRefObjectType() {
			return refObjectType;
		}
		public void setRefObjectType(BaseObjectTypes refObjectType) {
			this.refObjectType = refObjectType;
		}
		public long getRefObjectId() {
			return refObjectId;
		}
		public void setRefObjectId(long refObjectId) {
			this.refObjectId = refObjectId;
		}
		public String getRole() {
			return role;
		}
		public void setRole(String role) {
			this.role = role;
		}
	}
	
	private List<RelationReference> references;

	public Relation(long id) {
		super(id);
	}

	public List<RelationReference> getReferences() {
		return references;
	}

	public void setReferences(List<RelationReference> references) {
		this.references = references;
	}

}
