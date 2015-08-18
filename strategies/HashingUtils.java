package lsh.strategies;

public class HashingUtils {

	/**
	 * Combine minhashes into a signature. Combines the first digit of every minhash
	 * into a long.
	 * @param minHashes
	 * @return
	 */
	public static long getSignature(int[] minHashes){
		long toReturn = 0;
		for(int i = 0; i < minHashes.length && i < 64; i++){
			int val = minHashes[i];
			val = val&1; //bit-masking everything but the last digit
			toReturn = (toReturn << 1) | val; //shift by 1 and "concatenate" val's digit
		}
		return toReturn;
	}
	
	/**
	 * Helper method for deterministically combining integers into a hash.
	 * The goal is create a hash so that our data is spread uniformly across buckets.
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static int combineIntegers(int a, int b){
			//circular bitshift. Shuffles bits to produce a more random hash.
		/*	a =  (a >>> a) | (a << (Integer.SIZE - a)); 
			a ^=b; //xor to combine hashes.
			return a;*/
			 		
		return MurmurHash.combineIntegers(a, b);
	}
	
	/**
	 * Band minhashes into integers that index a hashmap.
	 * @param minHashes
	 * @param sizeOfBands
	 * @return
	 */
	public static int[] getBuckets(int[] minHashes, int sizeOfBands){
		int[] buckets = new int[minHashes.length/sizeOfBands];
		int counter = 0;
		int counter2 = 0;
		int bucket = 0;
		for(int i : minHashes){
			bucket = combineIntegers(bucket, i);
			if(++counter2 == sizeOfBands){
				buckets[counter++] = bucket;
				counter2 = 0;
				bucket = 0;
			}
		}
		return buckets;
	}
}