package lsh.strategies;

import lsh.HashingResult;
import lsh.LSHable;

/**
 * 
 * Defines a strategy for a locality sensitive hashing function.
 * Implementing classes must design hash functions that group similar things to the same bucket. 
 * Signatures must also be implemented that generate 64-bit long where each bit is independent of the others.
 * See definition of LSH for more detail.
 * @author Administrator
 * 
 *
 */

public interface LSHStrategy {
	
	/**
	 * Hash an input to a set of buckets and a signature which will be used for quick comparisons.
	 * @param input
	 * @return
	 */
	HashingResult hash(LSHable input);
	
	public static class STRATEGIES{
		
		public static RandomHashStrategy randomHashStrategy(int numOfMinhashes, int sizeOfBands){
			return new RandomHashStrategy(numOfMinhashes, sizeOfBands);
		}
		
		/*public static GregsStrategy gregsStrategy(int numOfMinhashes, int sizeOfBands){
			return new GregsStrategy(numOfMinhashes, sizeOfBands, numOfMinhashes*5);
		}*/
		
	}

}
