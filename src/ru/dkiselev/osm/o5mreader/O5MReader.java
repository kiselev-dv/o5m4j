package ru.dkiselev.osm.o5mreader;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.dkiselev.osm.o5mreader.datasets.BBOXds;
import ru.dkiselev.osm.o5mreader.datasets.DataSet;
import ru.dkiselev.osm.o5mreader.datasets.END;
import ru.dkiselev.osm.o5mreader.datasets.FileTimestamp;
import ru.dkiselev.osm.o5mreader.datasets.Header;
import ru.dkiselev.osm.o5mreader.datasets.Jump;
import ru.dkiselev.osm.o5mreader.datasets.NWRDataSet;
import ru.dkiselev.osm.o5mreader.datasets.SkipDS;
import ru.dkiselev.osm.o5mreader.datasets.NWRDataSet.BaseObjectTypes;
import ru.dkiselev.osm.o5mreader.datasets.Node;
import ru.dkiselev.osm.o5mreader.datasets.Relation;
import ru.dkiselev.osm.o5mreader.datasets.Relation.RelationReference;
import ru.dkiselev.osm.o5mreader.datasets.Reset;
import ru.dkiselev.osm.o5mreader.datasets.SyncDS;
import ru.dkiselev.osm.o5mreader.datasets.Way;

public class O5MReader {

	private static final int STRING_TABLE_SIZE = 15000;
	
	private boolean skipTags = false;
	
	private boolean skipNodesParsing = false;
	private boolean skipWaysParsing = false;
	private boolean skipRelationsParsing = false;

	private boolean dropNodes = false;
	private boolean dropWays = false;
	private boolean dropRelations = false;
	

	protected final InputStream is;
	
	protected long fileOffset = 0;
	
	private DataSet lastDS;
	
	//Counters
	private long nodeId = 0;
	private long wayId = 0;
	private long wayNodeId = 0;	
	private long relId = 0;	
	private long relRefId = 0;
	private long lon = 0;
	private long lat = 0;
	private long versionTimestamp = 0;
	private long versionChangeset = 0;
	private int stringTablePointer = 0;

	//Strings table
	private byte[][] strPairTable;

	//Header types
	private static final byte O5MREADER_DS_HEADER = (byte) 0xe0;
	private static final byte O5MREADER_DS_END = (byte) 0xfe;
	private static final byte O5MREADER_DS_NODE = (byte) 0x10;
	private static final byte O5MREADER_DS_WAY = (byte) 0x11;
	private static final byte O5MREADER_DS_REL = (byte) 0x12;
	private static final byte O5MREADER_DS_BBOX = (byte) 0xdb;
	private static final byte O5MREADER_DS_TSTAMP = (byte) 0xdc;
	private static final byte O5MREADER_DS_SYNC = (byte) 0xee;
	private static final byte O5MREADER_DS_JUMP = (byte) 0xef;
	private static final byte O5MREADER_DS_RESET = (byte) 0xff;
	
	private static interface Counter {
		public void handle(int times);
		public int getValue();
	} 
	
	private static class Decrementor implements Counter {

		private int value = 0;
		
		public Decrementor (int value) {
			this.value = value;
		}
		
		@Override
		public void handle(int times) {
			this.value -= times;
		}

		@Override
		public int getValue() {
			return this.value;
		}
		
	}
	
	public O5MReader(InputStream is){
		this.is = is;
	}
	
	public static class UnexpectedEndOfFileException extends RuntimeException {
		private static final long serialVersionUID = -6279074718931570275L;
	}
	
	public static class StringParseError extends RuntimeException {
		public StringParseError(String string) {
			super(string);
		}

		private static final long serialVersionUID = -6279074718931570276L;
	}
	
	protected Header parseHeader(InputStream inputStream) throws IOException {

		BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
		
		return new Header(br.readLine());
	}
	
	public void read(O5MHandler handler) throws IOException {
		DataSet ds = null;
		do {
			 ds = read();
			 if(ds != null) {
				 if(ds instanceof Node) {
					 handler.handleNode((Node)ds);
				 }
				 if(ds instanceof Way) {
					 handler.handleWay((Way)ds);
				 }
				 if(ds instanceof Relation) {
					 handler.handleRelation((Relation)ds);
				 }
			 }
		}
		while (!(ds instanceof END));
	}
	
	public final DataSet read() throws IOException {
		DataSet newDataSet = readDataSet();
		checkDSOrder(this.lastDS, newDataSet);
		this.lastDS = newDataSet;
		return lastDS;
	}
	
	private void checkDSOrder(DataSet lastDS2, DataSet newDataSet) {
		
	}

	protected DataSet readDataSet() throws IOException  {
		int read = is.read();
		
		if(read < 0) {
			throw new UnexpectedEndOfFileException();
		}
		
		switch((byte)read){
		
		//reset of undefined
		case O5MREADER_DS_RESET:  
			resetState(); 
			return new Reset();
		
		// file ednds
		case O5MREADER_DS_END: 
			fileEnd();
			return new END();

		// file header	
		case O5MREADER_DS_HEADER: 
			return parseHeader(readDSAsBytes(readUInt(is, null), is));
			
		// jump (ref to next/prev jump section) 	
		case O5MREADER_DS_JUMP: 
			return parseJump(readDSAsBytes(readUInt(is, null), is), null);
			
		case O5MREADER_DS_TSTAMP:
			return parseTimestamp(readDSAsBytes(readUInt(is, null), is), null);
			
		case O5MREADER_DS_BBOX:
			return parseBBOX(readDSAsBytes(readUInt(is, null), is), null);

		case O5MREADER_DS_NODE: {
			long length = readUInt(is, null);
			if(dropNodes) {
				is.skip(length);
				return null;
			} 
			else if (skipNodesParsing) {
				byte b[] = new byte[(int) length];
				if(length > is.read(b)){
					throw new UnexpectedEndOfFileException();
				}
				return new SkipDS(BaseObjectTypes.NODE, b);
			}
			else {
				return parseNode(readDSAsBytes(length, is), (int) length);
			}
		}

		case O5MREADER_DS_WAY: {
			long length = readUInt(is, null);
			if(dropWays) {
				is.skip(length);
				return null;
			}
			else if (skipWaysParsing) {
				byte b[] = new byte[(int) length];
				if(length > is.read(b)){
					throw new UnexpectedEndOfFileException();
				}
				return new SkipDS(BaseObjectTypes.WAY, b);
			}
			else {
				return parseWay(readDSAsBytes(length, is), (int) length);
			}
		}

		case O5MREADER_DS_REL: {
			long length = readUInt(is, null);
			if(dropRelations) {
				is.skip(length);
				return null;
			}
			else if (skipRelationsParsing) {
				byte b[] = new byte[(int) length];
				if(length > is.read(b)){
					throw new UnexpectedEndOfFileException();
				}
				return new SkipDS(BaseObjectTypes.RELATION, b);
			}
			else {
				return parseRelation(readDSAsBytes(length, is), (int) length);
			}
		}
		
		case O5MREADER_DS_SYNC: {
			long length = readInt(is, null);
			is.skip(length);
			return new SyncDS(length);
		}
		
		default: 
			long length = readInt(is, null);
			is.skip(length);
			return null;
		}
		
	}

	private Relation parseRelation(InputStream inputStream, int length) throws IOException {
		
		Decrementor dec = new Decrementor(length);
		
		this.relId += readInt(inputStream, dec);
		
		Relation result = new Relation(relId);
		fillVersion(result, inputStream, dec);
		
		int refsLength = (int) readUInt(inputStream, dec);
		int refsStartPosition = dec.getValue();
		
		List<RelationReference> references = new ArrayList<RelationReference>();
		
		//refsStartPosition - dec.getValue()  - bytes readed in members section 
		while(refsStartPosition - dec.getValue() < refsLength)
		{
			this.relRefId += readInt(inputStream, dec);
			String typeAndRole = new String(readString(inputStream, dec), "UTF-8");
			
			RelationReference ref = new RelationReference();
			references.add(ref);
			
			ref.setRefObjectId(this.relRefId);
			char type = typeAndRole.charAt(0);
			
			if(type == '0') {
				ref.setRefObjectType(BaseObjectTypes.NODE);
			}
			else if (type == '1') {
				ref.setRefObjectType(BaseObjectTypes.WAY);
			}
			else if (type == '2') {
				ref.setRefObjectType(BaseObjectTypes.RELATION);
			} 
			else {
				throw new RuntimeException("Unknow referenced object type " + new String(new char[]{type}));
				//continue;
			}
			
			if(typeAndRole.length() > 1){
				ref.setRole(typeAndRole.substring(1));
			}
		}
		result.setReferences(references);
		
		fillTags(inputStream, result, dec.getValue());
		
		return result;
	}

	private Way parseWay(InputStream inputStream, int length) throws IOException {
		
		Decrementor dec = new Decrementor(length);
		
		this.wayId += readInt(inputStream, dec);
		
		Way result = new Way(wayId);
		fillVersion(result, inputStream, dec);
		
		int nodeRefsLength = (int) readUInt(inputStream, dec);
		int refsStartPosition = dec.getValue();
		
		List<Long> nodes = new ArrayList<Long>();
		
		//refsStartPosition - dec.getValue()  - bytes readed in nodes section 
		while(refsStartPosition - dec.getValue() < nodeRefsLength)
		{
			this.wayNodeId += readInt(inputStream, dec);
			nodes.add(Long.valueOf(this.wayNodeId)); 
		}
		result.setNodes(nodes);
		
		fillTags(inputStream, result, dec.getValue());
		
		return result;
	}

	@Deprecated
	private InputStream readDSAsBytes(long length, InputStream is2) throws IOException {
		
		byte[] buffer = new byte[(int) length];
		
		if(is2.read(buffer) < length) {
			throw new UnexpectedEndOfFileException();
		}
		
		return new ByteArrayInputStream(buffer);
	}

	private Node parseNode(InputStream inputStream, int length) throws IOException {
		
		Decrementor dec = new Decrementor(length);
		
		this.nodeId += readInt(inputStream, dec);
		
		Node result = new Node(nodeId);
		fillVersion(result, inputStream, dec);

		this.lon += readInt(inputStream, dec);
		this.lat += readInt(inputStream, dec);
		result.setLonLat((double)this.lon / 10000000.0f, (double)this.lat / 10000000.0f);
		
		fillTags(inputStream, result, dec.getValue());
		
		return result;
	}

	private void fillTags(InputStream inputStream, NWRDataSet result, int length)
			throws IOException {
		
		if(!skipTags) {
			
			Map<String, String> tags = new HashMap<String, String>();
			
			Decrementor dec = new Decrementor(length);
			while(dec.getValue() > 0) {
				Pair<String, String> tag = parseStrStrPair(readPair(inputStream, dec));

				if(tag != null) { 
					tags.put(tag.getKey(), tag.getValue());
				}
			}
			
			result.setTags(tags);
		}
	}

	private void fillVersion(NWRDataSet result, InputStream inputStream, Counter counter) throws IOException {
		
		long version = readUInt(inputStream, counter);
		
		if(version > 0){
			result.setVersion(version);
			
			this.versionTimestamp += readInt(inputStream, counter);
			if(this.versionTimestamp != 0) {
				result.setTimestamp(this.versionTimestamp);
				
				this.versionChangeset += readInt(inputStream, counter);
				result.setChangeset(this.versionChangeset);
				
				Pair<Integer, String> user = parseIntStrPair(readPair(inputStream, counter));
				result.setUser(user);
			}
		}

	}

	private DataSet parseBBOX(InputStream inputStream, Counter counter) throws IOException {
		
		long x1 = readInt(inputStream, counter);
		long y1 = readInt(inputStream, counter);

		long x2 = readInt(inputStream, counter);
		long y2 = readInt(inputStream, counter);
		
		return new BBOXds(x1, y1, x2, y2);
	}

	private DataSet parseTimestamp(InputStream inputStream, Counter counter) throws IOException {
		long timestamp = readInt(inputStream, counter);
		return new FileTimestamp(timestamp);
	}

	private DataSet parseJump(InputStream inputStream, Counter counter) throws IOException {
		long next = readUInt(inputStream, counter);
		long prev = readUInt(inputStream, counter);
		return new Jump(next, prev);
	}

	private void fileEnd() {
		
	}

	private void resetState() {
		nodeId = 0;
		wayId = 0;
		wayNodeId = 0;	
		relId = 0;	
		relRefId = 0;
		lon = 0;
		lat = 0; 	
		versionTimestamp = 0;
		versionChangeset = 0;
		stringTablePointer = 0;

		//Strings table
		if(skipTags) {
			strPairTable = null;
		}
		else {
			strPairTable = new byte[STRING_TABLE_SIZE][255];
		}
	}
	
	private long readUInt(InputStream is, Counter counter) throws IOException {
		int b;
		byte i = 0;
		long result = 0;

		int c = 0;
		do  {
			b = is.read();
			c++;
			
			if ( b < 0 ) {
				throw new UnexpectedEndOfFileException();
			}
			
			result = result | (((long)(b & 0x7f)) << (i++ * 7));
			
		} while ( (b & 0x80) != 0);	
		
		if (counter != null) {
			counter.handle(c);
		}
		
		return result;
	}

	private long readInt(InputStream is, Counter counter) throws IOException {
		
		long uResult = readUInt(is, counter);

		uResult = (uResult & 1) != 0 ? -(uResult >> 1) - 1 	: (uResult >> 1);
		
		return uResult;	
	}
	
	private byte[] readPair(InputStream inputStream, Counter counter) throws IOException {	
		
		int ref = (int) readUInt(inputStream, counter);
		byte[] pair;
		
		if ( ref != 0 ) {
			int tableRef = (stringTablePointer + 15000 - ref) % 15000;
			if(tableRef < 0 || tableRef >= 15000){
				return null;
			}
			pair = strPairTable[tableRef ];
		}
		else {
			byte[] k = readNullTerminatedString(inputStream, counter);			
			byte[] v = readNullTerminatedString(inputStream, counter);			
			
			pair = new byte[k.length + v.length + 1];
			System.arraycopy(k, 0, pair, 0, k.length);
			System.arraycopy(v, 0, pair, k.length + 1, v.length);
			pair[k.length] = 0x00;
			
			if ( k.length + v.length <= 255 ) {
				strPairTable[(stringTablePointer + 15000) % 15000] = pair;
				stringTablePointer++;
			}
					
		}
		
		return pair;
	}

	private byte[] readString(InputStream inputStream, Counter counter) throws IOException {	
		
		int ref = (int) readUInt(inputStream, counter);
		byte[] result;
		
		if ( ref != 0 ) {
			result = strPairTable[(stringTablePointer + 15000 - ref) % 15000 ];
		}
		else {
			result = readNullTerminatedString(inputStream, counter);			
			
			if ( result.length <= 255 ) {
				strPairTable[(stringTablePointer + 15000) % 15000] = result;
				stringTablePointer++;
			}
			
		}
		
		return result;
	}

	private byte[] readNullTerminatedString(InputStream inputStream, Counter counter) throws IOException {
		byte[] buffer = new byte[1024];
		int p = 0, b = 0;
		
		do
		{
			b = inputStream.read();
			
			if(b < 0) {
				return new byte[0];
			}
			
			buffer[p] = (byte)b;
		}
		while(p < 1024 && buffer[p++] != 0x00);
		
		if(counter != null) {
			counter.handle(p);
		}
		
		return Arrays.copyOf(buffer, p - 1);
	}

	private Pair<Integer, String> parseIntStrPair(byte[] pair) throws IOException {
		
		if(pair != null){
			int i = indexOf(pair, (byte)0x00);
			if(i > 0){
				int key = (int) readUInt(new ByteArrayInputStream(Arrays.copyOf(pair, i)), null);
				String value = (i < pair.length - 1) ? new String(Arrays.copyOfRange(pair, i + 1, pair.length)) : null;

				return new Pair<Integer, String>(key, value);
			}
		}
		
		return null;
	}

	private Pair<String, String> parseStrStrPair(byte[] pair) throws IOException {
		
		if(pair != null){
			int i = indexOf(pair, (byte)0x00);
			if(i > 0){
				String key = new String(Arrays.copyOf(pair, i));
				
				String value = (i < pair.length - 1) ? new String(Arrays.copyOfRange(pair, i + 1, pair.length)) : null;

				return new Pair<String, String>(key, value);
			}
		}
		
		return null;
	}

	private int indexOf(byte[] pair, byte i) {
		
		for(int r = 0; r < pair.length; r++){
			if(pair[r] == i){
				return r;
			}
		}
		
		return -1;
	}
	
}
