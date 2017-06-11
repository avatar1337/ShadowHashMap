import java.util.Collection;
import java.util.Random;

import map.ShadowHashMap;

public class ShadowHashMapTest {

	public static void main(String[] args) {
		final ShadowHashMap<Integer, Integer> numbers = new ShadowHashMap<>();
		Random rnd = new Random();
		final int N = 1000000;
		
		System.out.println("Adding "+N+" elements to the shadow hashmap...");
		for(int i=0; i < N; i++) {
			numbers.put(i, rnd.nextInt(N));		
		}
		
		System.out.println("Testing...");
		//System.out.println(numbers.show()); // Uses UTF-8
		long start = System.nanoTime();
		for(int i=0; i < N; i++) {
			//numbers.getFirstKey(i);
			numbers.getKeys(i);
		}			
		System.out.println("Mean retrieval time of key collection from value: "+(System.nanoTime()-start)/N+" ns");
		
		start = System.nanoTime();
		for(int i=0; i < N; i++) {
			numbers.get(i);
		}
		System.out.println("Mean retrieval time of value from key: "+(System.nanoTime()-start)/N+" ns");
		
		System.out.println("Testing random retrieval of keys and comparing them to the value");
		Collection<Integer> keys = null;
		int value = 0;
		while(keys == null || keys.size() < 2) { // Give me at least 2 keys to check. 
			value = rnd.nextInt(N);
			keys = numbers.getKeys(value);		
		}
		System.out.println("Using value: "+value+", found "+keys.size()+" keys: "+keys);
		final int V = value;
		if(keys.stream().allMatch(key -> {
			int v = numbers.get(key);			
			System.out.println("Checking: ["+key+" = "+v+"]");
			return v == V;
		}))
			System.out.println("All keys brought back the correct value");
		else
			System.out.println("One or more keys brought back the wrong value!");

	}
	
	
}


