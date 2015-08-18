package lsh.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import lsh.HashingResult;
import lsh.LSH;
import lsh.LSHable;
import lsh.strategies.LSHStrategy;

public class LSHImpl implements LSH {

	private final int numOfMinHashes;
	private final int sizeOfBands;
	private final LSHStrategy strategy;

	private final ConcurrentHashMap<Integer, long[]> buckets;
	private final ConcurrentHashMap<Long, Collection<LSHable>> signatureMap;

	/**
	 * 
	 * @param numOfMinHashes
	 *            : More hashes are slower and take more memory but are more
	 *            accurate. Min 64.
	 * @param sizeOfBands
	 *            : Wider bands are faster but less accurate.
	 * @param strategy
	 *            : The strategy used to generate minhashes and signatures.
	 */
	public LSHImpl(int numOfMinHashes, int sizeOfBands, LSHStrategy strategy) {
		if (numOfMinHashes < 64)
			throw new RuntimeException("At least 64 minhashes are required");
		
		// This limit could probably be adjusted.
		this.numOfMinHashes = numOfMinHashes;
		this.sizeOfBands = sizeOfBands;
		this.strategy = strategy;

		this.buckets = new ConcurrentHashMap<Integer, long[]>();
		this.signatureMap = new ConcurrentHashMap<Long, Collection<LSHable>>();
	}

	@Override
	public Collection<LSHable> search(LSHable input, int matchThreshold) {

		HashingResult result = strategy.hash(input);
		int[] bucketIndices = result.getBuckets();
		long baseSignature = result.getSignature();

		Collection<LSHable> toReturn = new HashSet<LSHable>();
		int maxNumOfDifferences = 64 - 64 *matchThreshold / 100;

		for (int index : bucketIndices) {
			long[] bucket = buckets.get(index);
			if (bucket == null)
				continue;
			for (long signature : bucket) {
				// filter by doing a bitwise-comparison of longs.
				// find # of 1's. We have a match if the % of
				// 1's is greater than the match threshold.
				// also don't use 64 as your base if you have less
				// than 64 minhash functions.

				// num of differences
				if (Long.bitCount(baseSignature ^ signature) < maxNumOfDifferences) {
					// add to return
					Collection<LSHable> toAdd = signatureMap.get(signature);
					if (toAdd != null)
						toReturn.addAll(toAdd);
				}
			}
		}
		return toReturn;
	}

	@Override
	public void add(LSHable input) {

		HashingResult result = strategy.hash(input);
		int[] bucketIndices = result.getBuckets();
		long baseSignature = result.getSignature();

		for (int index : bucketIndices) {
			long[] bucket = buckets.get(index);
			if (bucket == null){
				bucket = new long[1];
				buckets.put(index, bucket);
			}				

			boolean foundSignature = false;
			for (int i = 0; i < bucket.length; i++) {
				if (bucket[i] == baseSignature) {
					foundSignature = true;
					break;
				}
			}
			if (foundSignature == false) {
				// add
				int pos = findNextEmptySpot(bucket);
				if (pos == -1) {
					bucket = Arrays.copyOf(bucket, bucket.length + 1);
					buckets.put(index, bucket);
					pos = bucket.length - 1;
				}
				bucket[pos] = baseSignature;
			}

		}
		Collection<LSHable> vals = signatureMap.get(baseSignature);
		if(vals == null){
			vals = new ArrayList<LSHable>();
			signatureMap.put(baseSignature, vals);
		}
		vals.add(input);

	}

	/**
	 * Returns -1 if there is no empty position available.
	 * 
	 * @param array
	 * @return
	 */
	private int findNextEmptySpot(long[] array) {
		for (int i = 0; i < array.length; i++) {
			//overrwrites any signature with value of 0. This is probably ok.
			if (array[i] == 0) {
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * For testing/debugging
	 * @return
	 */
	public long getTotalBuckets(){
		return buckets.keySet().size();
	}
}
