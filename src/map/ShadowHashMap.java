package map;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 
 * A shadow hash map differs from a normal hash map in the sense
 * that key retrieval from value and value retrieval from key both
 * have O(1) time complexity. This is done by each entry casting
 * a "shadow" onto another place in the table with a reference on
 * where to find the key holding that value. This will use a little
 * bit more memory, but not twice as much since a shadow shares a 
 * position with an entry.
 * 
 * @author Mikael Murstam
 *
 * @param <K> Key
 * @param <V> Value
 */
public class ShadowHashMap<K,V> extends AbstractMap<K,V> implements Iterable<K> {
	private int size;
	private final double loadFactor;
	Entry<K,V>[] table;
	
	@SuppressWarnings("unchecked")
	public ShadowHashMap() {
		size = 0;		
		loadFactor = 0.75;
		table = (Entry<K,V>[]) new Entry[16];
	}
	
	@SuppressWarnings("unchecked")
	public ShadowHashMap(int capacity) {
		size = 0;		
		loadFactor = 0.75;
		table = (Entry<K,V>[]) new Entry[capacity];
	}
	
	@SuppressWarnings("unchecked")
	public ShadowHashMap(int capacity, double loadFactor) {
		size = 0;		
		this.loadFactor = loadFactor;
		table = (Entry<K,V>[]) new Entry[capacity];
	}

	@SuppressWarnings("unchecked")
	@Override
	public V get(Object key) {
		Entry<K,V> entry = findValue(index((K)key), (K)key);
		return (entry == null)? null : entry.value;
	}
	
	@SuppressWarnings("unchecked")
	public K getFirstKey(Object value) {		
		Entry<K, V> entry = findFirstKey(index((V)value), (V)value);
		return (entry == null)? null : entry.key;
	}	
	
	@SuppressWarnings("unchecked")
	public Collection<K> getKeys(Object value) {		
		return findKeys(index((V)value), (V)value);
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	@Override
	public V put(K key, V value) {	
		int index = index(key);
		Entry<K,V> exist = findValue(index, key);
		if(exist != null) {
			V old = exist.value;
			exist.value = value;
			return old;
		}
		
		Entry<K,V> newEntry = new Entry<K,V>(key, value);
		
		if(table[index] == null) {
			table[index] = newEntry;
		} else {
			Entry<K, V> current = table[index];
			if(current.isEmpty()) {
				newEntry.shadow = current.shadow;
				newEntry.next = current.next;
				table[index] = newEntry;
			} else {
				while(current.next != null && !current.next.isEmpty())
					current = current.next;
				
				if(current.next == null) {
					current.next = newEntry;
				} else {
					newEntry.shadow = current.next.shadow;
					newEntry.next = current.next.next;
					current.next = newEntry;
				}					
			}
		}
		
		castShadow(newEntry);
		size++;
		if(size > loadFactor * table.length) rehash();
		return null;
	}
	
	private void castShadow(Entry<K,V> entry) {
		int shadowIndex = index(entry.value);
		if(table[shadowIndex] == null) {
			table[shadowIndex] = new Entry<K,V>(null,null); // create a shadow
			table[shadowIndex].shadow = entry;
		} else {
			Entry<K, V> current = table[shadowIndex];			
			while(current.shadow != null && current.next != null) current = current.next;
			if(current.shadow == null) {
				current.shadow = entry;
			} else {
				current.next = new Entry<K,V>(null, null);
				current.next.shadow = entry;
			}							
		}
	}
	
	@SuppressWarnings("unchecked")
	private void rehash() {
		Entry<K,V>[] oldTable = table;
		table = (Entry<K,V>[]) new Entry[(int)(table.length*2)];
		size = 0;
		for(int i=0; i < oldTable.length; i++) {
			Entry<K, V> current = oldTable[i];
			while(current != null && !current.isEmpty()) { 
				put(current.key, current.value);
				current = current.next;
			}
		}
	}
	
	@Override
	public V remove(Object k) {
		@SuppressWarnings("unchecked")
		K key = (K) k;
		int index = index(key);
		Entry<K,V> e = findValue(index, key);
		if (e == null) return null;		
		if (table[index].equals(e)){
			table[index] = table[index].next;
			size--;
			return e.value;
		}
		else {
			Entry<K,V> before = table[index];
			while (!before.next.equals(e)) before = before.next;
			before.next = e.next;
			size--;
			return e.value;
		}
	}

	@Override
	public int size() {
		return size;
	}
	
	public int getTableLength() {
		int c=0;
		for(int i=0; i < table.length; i++)
			if(table[i] != null) c++;
		return c;
	}
	
	private int index(Object o) {
		int index = o.hashCode() % table.length;
		if (index < 0 )	index += table.length;
		return index;
	}
	
	private Entry<K,V> findValue(int index, K key) {
		return findValue(table, index, key);
	}
	
	private Entry<K,V> findValue(Entry<K,V>[] table, int index, K key) {
		Entry<K,V> e = table[index];
		while(e != null) {
			if(e.key == null) return null;
			if(e.key.equals(key)) return e;
			e = e.next;
		}
		return e;
	}
	
	private Entry<K,V> findFirstKey(int index, V value) {
		return findFirstKey(table, index, value);
	}
	
	private Entry<K,V> findFirstKey(Entry<K,V>[] table, int index, V value) {
		Entry<K,V> e = table[index];
		while(e != null && e.shadow != null) {
			if(e.shadow.value.equals(value)) return e.shadow;
			e = e.next;
		}
		return (e == null)? null : e.shadow;
	}
	
	private Collection<K> findKeys(int index, V value) {
		return findKeys(table, index, value);
	}
	
	private Collection<K> findKeys(Entry<K,V>[] table, int index, V value) {
		Collection<K> keys = new LinkedList<>();
		
		Entry<K,V> e = table[index];
		while(e != null && e.shadow != null) {
			if(e.shadow.value.equals(value)) keys.add(e.shadow.key);
			e = e.next;
		}
		return keys;
	}
	
	public String show() {
		StringBuilder entries = new StringBuilder();
		entries.append(" ✤ HASHTABLE CONTENT ✤\n");
		entries.append("⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺⎺\n");
		for(int i=0; i < table.length; i++) {
			if(table[i] == null) {
				entries.append(""+i+": 〔〕\n");
				continue;
			}			
			Entry<K,V> next = table[i];
			boolean first = true;
			while(next != null) {
				if(first) {
					entries.append(i);
					entries.append(": 〔");
				} else
					entries.append(" 〔");
				first = false;
				entries.append(next.toString() + ((next.shadow != null)? " @ index: "+index(next.shadow.key):""));
				entries.append("〕");
				entries.append(((next.next != null)? " ➜" : ""));				
				next = next.next;
			}
			entries.append("\n");
		}
		return entries.toString();
	}
	
	@Override
	public String toString() {
		StringBuilder entries = new StringBuilder();
		int counter = 0;
		entries.append("{");
		for(int i=0; i < table.length; i++) {
			Entry<K,V> act = table[i];
			while(act != null) {
				entries.append(act);
				if(counter < size-1) entries.append(", ");
				counter++;
				act = act.next;
			}			
		}
		entries.append("}");
		return entries.toString();
	}
	
	private static class Entry<K,V> implements Map.Entry<K, V> {
		private K key;
		private V value;
		private Entry<K,V> shadow;
		private Entry<K,V> next;
		
		public Entry(K key, V value){
            this.key = key;
            this.value = value;
            this.next = null;
            this.shadow = null;
        }
		
		public boolean isEmpty() {
			return key == null;
		}
		
		@SuppressWarnings("unused")
		public void setShadow(Entry<K,V> s) {
			shadow = s;
		}
		
		@SuppressWarnings("unused")
		public Entry<K,V> getShadow() {
			return shadow;
		}
		
		@Override
		public K getKey() {
			return key;
		}

		@Override
		public V getValue() {
			return value;
		}

		@Override
		public V setValue(V value) {
			V old = this.value;
			this.value = value;
			return old;
		}
		
		@Override
		public String toString() {
			if(key==null) return "SHADOW ➜ "+shadow.key.toString()+" = "+shadow.value.toString();
			return key.toString()+" = "+value.toString() + ((shadow != null)? ", SHADOW ➜ "+shadow.key.toString()+" = "+shadow.value.toString():"");
		}		
	}

	@Override
	public Iterator<K> iterator() {
		return new MapIterator();
	}
	
	// Slow :(
	private class MapIterator implements Iterator<K> {
		private int index;
		private Entry<K,V> next;
				
		public MapIterator() {
			index=0;
			next=null;			
		}

		@Override
		public boolean hasNext() {
			while(next == null || next.isEmpty()) {
				if(index == table.length)
					return false;
				next = table[index++];
			}							
			return true;
		}

		@Override
		public K next() {
			while(next == null || next.isEmpty()) {
				if(index == table.length)
					throw new NoSuchElementException();
				next = table[index++];
			}					
			K key = next.getKey();
			next = next.next;
			return key;
		}
		
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		HashSet<Map.Entry<K,V>> set = new HashSet<>();
		Stream.of(table).forEach(e -> {
			if(e != null && e.key != null) set.add(e);
		});
		return set;
	}
}
