package ru.dkiselev.osm.o5mreader;

public class Pair<K, V> {
    
	private K key; //first member of pair
    private V value; //second member of pair

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

	public K getKey() {
		return key;
	}

	public void setKey(K key) {
		this.key = key;
	}

	public V getValue() {
		return value;
	}

	public void setValue(V value) {
		this.value = value;
	}
    
}