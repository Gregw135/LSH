package lsh;

/**
 * A tuple for storing a signature and result in a single return object.
 * @author Administrator
 *
 */
public interface HashingResult {
	
	long getSignature();
	int[] getBuckets();

}
