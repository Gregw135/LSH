package lsh;

import java.util.Collection;

/**
 * Interface for locality sensitive hashing algorithm. Please see chapter 3 of mining of massive data sets.
 * 
 * TODO: 
 * 1. Implement remove
 * 2. Implement an eviction strategy for old data, so your program doesn't run out of memory.
 * @author Administrator
 *
 */
public interface LSH {
	Collection<LSHable> search(LSHable input, int threshold);
	void add(LSHable input);
}


