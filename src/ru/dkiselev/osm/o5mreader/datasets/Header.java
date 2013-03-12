package ru.dkiselev.osm.o5mreader.datasets;

public class Header extends DataSet {
	
	public static enum O5MType {
		O5M, O5C
	}

	private O5MType type;
	
	public Header(String type) {
		if(type.equals(".o5m2")){
			this.type = O5MType.O5M;
		}
		else if(type.equals(".o5c2")){
			this.type = O5MType.O5C;
		}
	}
	
	public O5MType getType(){
		return this.type;
	}
}
