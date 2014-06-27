package ru.dkiselev.osm.o5mreader;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Map.Entry;

import ru.dkiselev.osm.o5mreader.datasets.NWRDataSet;
import ru.dkiselev.osm.o5mreader.datasets.Node;
import ru.dkiselev.osm.o5mreader.datasets.Relation;
import ru.dkiselev.osm.o5mreader.datasets.Relation.RelationReference;
import ru.dkiselev.osm.o5mreader.datasets.Way;

public class Main extends O5MHandler {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			
			O5MReader reader = new O5MReader(new FileInputStream(args[0]));
			reader.read(new Main());
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void handleNode(Node ds) {
		StringBuilder sb = new StringBuilder();
		sb.append("<node ")
			.append("lat=\"").append(ds.getLat()).append("\" ")
			.append("lon=\"").append(ds.getLon()).append("\" ");
		
		fillUserInfo(sb, ds);

		if(ds.getTags() == null || ds.getTags().isEmpty()) {
			sb.append("/>");
		}
		else {
			sb.append(">\n");
			
			appendTags(ds, sb);
			
			sb.append("</node>");
		}
		
		System.out.println(sb);
		
	}
	
	@Override
	public void handleWay(Way ds) {
		StringBuilder sb = new StringBuilder();
		sb.append("<way ");
		
		fillUserInfo(sb, ds);

		if(ds.getTags() == null || ds.getTags().isEmpty()) {
			sb.append("/>");
		}
		else {
			sb.append(">\n");
			
			appendNodeRefs(ds, sb);
			appendTags(ds, sb);
			
			sb.append("</way>");
		}
		
		System.out.println(sb);
	}
	
	@Override
	public void handleRelation(Relation ds) {
		StringBuilder sb = new StringBuilder();
		sb.append("<relation ");
		
		fillUserInfo(sb, ds);

		if(ds.getTags() == null || ds.getTags().isEmpty()) {
			sb.append("/>");
		}
		else {
			sb.append(">\n");
			
			appendRelMembers(ds, sb);
			appendTags(ds, sb);
			
			sb.append("</relation>");
		}
		
		System.out.println(sb);
	}

	private void appendRelMembers(Relation ds, StringBuilder sb) {
		for(RelationReference ref : ds.getReferences()) {
			sb.append("\t<member type=\"")
			.append(ref.getRefObjectType() == null ? null : ref.getRefObjectType().name().toLowerCase())
			.append("\" ref=\"")
			.append(ref.getRefObjectId()).append("\" role=\"")
			.append(ref.getRole() == null ? "" : ref.getRole()).append("\">\n");
		}
	}

	private void appendNodeRefs(Way ds, StringBuilder sb) {
		for(long id : ds.getNodes()) {
			sb.append("\t<nd ref=\"").append(id).append("\"/>\n");
		} 
	}

	private void appendTags(NWRDataSet ds, StringBuilder sb) {
		for(Entry<String, String> tag : ds.getTags().entrySet()) {
			sb.append("\t<tag k=\"").append(tag.getKey()).append("\" v=\"").append(tag.getValue()).append("\"/>\n");
		}
	}

	private void fillUserInfo(StringBuilder sb, NWRDataSet ds) {
		sb.append("id=\"").append(ds.getId()).append("\" ");
		if(ds.getUser() != null) {
			sb.append("user=\"").append(ds.getUser().getValue()).append("\" ");
			sb.append("uid=\"").append(ds.getUser().getKey()).append("\" ");
		}
		sb.append("visible=\"true\" ");
		
		sb.append("version=\"").append(ds.getVersion()).append("\" ")
		.append("changeset=\"").append(ds.getChangeset()).append("\" ")
		.append("timestamp=\"").append(new Timestamp(ds.getTimestamp())).append("\" ");
	}

}
