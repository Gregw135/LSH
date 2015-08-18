package lsh.strategies;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import lsh.HashingResult;
import lsh.LSHable;
import lsh.impl.HashingResultImpl;

/**
 * A custom strategy I was trying. Doesn't seem to work as well.  
 * 
 * Strategy: Use a predetermined set of random numbers. For each random number
 * combined with each input, generate a score of + or -1 in some independent
 * fashion. Total scores above 0 get a minhash of 1; scores below 0 get a
 * minhash of 0.
 * 
 * The trick is that we generate thrice the desired number of minhashes and
 * bands, and then pare the results by eliminating the results that are most
 * likely to change.
 * 
 * TODO:
 * Your solution of selecting the best minhashes introduces a bias towards
 * minhashes that don't split your population evenly. Eliminate this, either
 * by selecting minhashes initially using training data that split your data evenly,
 * or by dynamically choosing a splitting point for each minhash.
 * 
 * @author Greg
 *
 */
public class GregsStrategy implements LSHStrategy{

	int[][] positiveOrNegative;
	private final int numOfMinhashesToTest;
	private final int numOfMinhashes;
	private final int sizeOfBands;
	
	public GregsStrategy(int numOfMinhashes, int sizeOfBands, int numOfMinhashesToTest, Set<LSHable> trainingData){
		this.numOfMinhashes = numOfMinhashes;
		this.numOfMinhashesToTest = numOfMinhashesToTest;
		this.sizeOfBands = sizeOfBands;
		List<Integer> randomPlusOrMinusOne = new ArrayList<Integer>();
		for(int j = 0; j < 1000; j++){
			if(j < 500){
				randomPlusOrMinusOne.add(1);
			}else{
				randomPlusOrMinusOne.add(-1);
			}
		}
		
		int[][] positiveOrNegative = new int[numOfMinhashesToTest][1000];
		for(int i = 0; i < numOfMinhashesToTest; i++){
			Collections.shuffle(randomPlusOrMinusOne);
			int counter = 0;
			for(Integer j : randomPlusOrMinusOne){
				positiveOrNegative[i][counter++] = j;
			}
		}
		this.positiveOrNegative = positiveOrNegative;
	}
	
	/*private int[][] getMinhashes(int numOfMinhashesToTest, Set<LSHable> trainingData){
		
		
		 * TODO: Priority queue
		 
	}*/
	
	@Override
	public HashingResult hash(LSHable input) {
		
		Iterable<Integer> itr = input.getInput();
		/*
		 * So, we want to sort an array of integers by their absolute value,
		 * so we can throw out scores close to 0. Unfortunately Java is too
		 * stupid to allow a primitive comparator, so I'm using this IntHelper
		 * class as a workaround :( 
		 */
		int[] scores = new int[numOfMinhashesToTest];
		
		int counter = 0;
		
		for(int j = 0; j < numOfMinhashesToTest; j++){
			int score = 0;
			for(int i : itr){
				score += positiveOrNegative[j][Math.abs(i%1000)];
			}
			scores[counter++] = score;
		}		
		
		int[] minhashes = getBuckets(scores, sizeOfBands);
		
		//minhashes[counter++] = (score > 0 ? 1 : 0);
		/*
		 * TODO: combining integers is the problem.
		 */
		return new HashingResultImpl(getSignature(minhashes), minhashes);
	}
	
	/**
	 * Side effect: sorts the input array.
	 * @param scores
	 * @param sizeOfBands
	 * @return
	 */
	private int[] getBuckets(int[] scores, int sizeOfBands){
		
		/*
		 * We're generating excess buckets, and then we'll filter to return the buckets that are most stable.
		 * Entry 1: The bucket
		 * Entry 2: A goodness score for the bucket. 0 is good, higher is worse
		 */
		IntegerPair[] testBuckets = new IntegerPair[numOfMinhashesToTest/sizeOfBands];
		
		int bucketCounter = 0;
		int hashesInCurrentBucket = 0;
		int bucket = 0;
		int numOfBadScores = 0;
		/*
		 * TODO: use probability instead.
		 */
		for(int i = 0 ; i < numOfMinhashesToTest; i++){
			//todo: shift in bits
			bucket = (bucket <<1) | (scores[i] > 0 ? 1 : 0);
			if(Math.abs(scores[i]) < 4)
				numOfBadScores++;
			if(++hashesInCurrentBucket == sizeOfBands){
				testBuckets[bucketCounter++] = new IntegerPair(bucket, numOfBadScores);
				hashesInCurrentBucket = 0;
				numOfBadScores = 0;
				bucket = 0;
			}
		}		
		
		Arrays.sort(testBuckets, new Comparator<IntegerPair>(){

			@Override
			public int compare(IntegerPair a, IntegerPair b) {
				if(a.val2 == b.val2)
					return 0;
				return (a.val2 < b.val2 ? -1 : 1);
			}			
		});
		
		int totalBuckets = numOfMinhashes/sizeOfBands;
		int[] buckets = new int[totalBuckets];
		for(int i =0; i < totalBuckets; i++){
			buckets[i] = testBuckets[i].val1;
		}
		
		return buckets;
	}
	
	private class IntegerPair{
		public final int val1;
		public final int val2;
		
		public IntegerPair(int val1, int val2){
			this.val1 = val1;
			this.val2 = val2;
		}
	}	
	
	private long getSignature(int[] minHashes){
		long signature = 0;
		for(int i = 0; i < minHashes.length && i < 64; i++){
			signature = (signature <<1) | (i&1);
		}
		return signature;
	}
}
