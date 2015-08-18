package lsh.impl;

import lsh.HashingResult;

public class HashingResultImpl implements HashingResult{

	private final long signature;
	private final int[] buckets;
	public HashingResultImpl(long signature, int[] buckets){
		this.signature = signature;
		this.buckets = buckets;
	}
	
	@Override
	public long getSignature() {
		return signature;
	}

	@Override
	public int[] getBuckets() {
		return buckets;
	}

	
}
