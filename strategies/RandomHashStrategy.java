package lsh.strategies;

import java.util.Random;

import lsh.HashingResult;
import lsh.LSHable;
import lsh.impl.HashingResultImpl;

public class RandomHashStrategy implements LSHStrategy{

	
	int[] randomIntegers;
	private final int numOfMinhashes;
	private final int sizeOfBands;
	
	public RandomHashStrategy(int numOfMinhashes, int sizeOfBands){
		int[] rand = new int[numOfMinhashes];
		Random r = new Random();
		for(int i = 0; i< numOfMinhashes; i++){
			rand[i] = r.nextInt();
		}
		this.numOfMinhashes = numOfMinhashes;
		this.sizeOfBands = sizeOfBands;
		this.randomIntegers = rand;
	}
	
	
	@Override
	/**
	 * Strategy: compare each input with a set of randomly generated integers.
	 * The input that matches most closely with each random integer are selected.
	 */
	public HashingResult hash(LSHable input) {
		Iterable<Integer> itr = input.getInput();
		int[] minhashes = new int[numOfMinhashes];
		int counter = 0;
		
		for(int r : randomIntegers){
			int closestDistance = Integer.MAX_VALUE;
			int bestValue = 0;
			int mostDigitsDifferent = 64;		
			
			for(int i : itr){
				/*
				 * a large prime number. Helps scatter clustered values. This especially
				 * helps with groups of small numbers, which would otherwise generate similar xor values,
				 * and therefore have a disproportionately small chance of being selected.
				 * 
				 */
				int val = i*1293843569; 
				int xor = r^val;
				
				/*
				 * Experimentally, bitcount works better than minimum value for choosing minhashes.
				 * In the case of a tie, we revert to answer closest to our random int.
				 */
				int differences = Integer.bitCount(xor);
				if(differences <= mostDigitsDifferent){
					int distance = Math.abs(i - r);
					if(differences == mostDigitsDifferent){						
						if(distance < closestDistance){
							closestDistance = distance;
							bestValue = i;
						}
					}else{ //differences < mostDigitsDifferent
						mostDigitsDifferent = differences;
						bestValue = i;
					}
				}
			}
			minhashes[counter++] = bestValue;
		}
		return new HashingResultImpl(HashingUtils.getSignature(minhashes), HashingUtils.getBuckets(minhashes, sizeOfBands));
	}		
}
