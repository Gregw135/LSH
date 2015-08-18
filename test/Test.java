package lsh.test;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import lsh.LSH;
import lsh.LSHable;
import lsh.impl.LSHImpl;
import lsh.strategies.LSHStrategy;

/**
 * A simple implementation of Locality Sensitive Hashing. 
 * This demo shows how LSH can be used to find similar devices. 
 * In this demo we generate a large number of simulated devices and hash them using LSH.
 * Then we modify each device, rehash it, and retrieve devices similar to the modified device.
 * If our algorithm is successful, then the original device should be among the devices retrieved.
 * 
 * 
 * LSH can also be used to find similar pictures, documents, people, etc.
 * @author Greg
 *
 */
public class Test {

	public static void main(String[] args) {
		
		/*
		 * Device properties
		 */
		int numDevices = 50000;
		//number of attributes per device.
		int numOfAtts = 100;
		/*
		 * Number of attributes to change
		 */
		int numToChange = 15;
		
		/*
		 * Algorithm input
		 */
		//Ignore devices whose signatures are less than this % similar to our input.
		int searchThreshold = 70;
		//Number of hashes to store per device.
		int numOfBands = 20;
		//Number of minhashes to join together per band. More minhashes yields greater precision but less recall.
		int sizeOfBands = 6;
		
		//best: 90/13. Yields 99% accuracy, 350 search time
		
		int successes = 0;
		int failures = 0;
		long insertionTime = 0;
		long searchTime = 0;
		long memoryRequired = 0;	
		
		System.gc();
		long initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		
		DeviceGenerator gen = new DeviceGenerator(numOfAtts);
		
		Device[] devices = new Device[numDevices];
		for (int i = 0; i < numDevices; i++) {
			devices[i] = gen.getRandomDevice();
		}
		
		//LSHImpl lsh = new LSHImpl(200, 10, LSHStrategy.STRATEGIES.gregsStrategy(numOfBands*sizeOfBands, sizeOfBands));
		LSHImpl lsh = new LSHImpl(200, 10, LSHStrategy.STRATEGIES.randomHashStrategy(numOfBands*sizeOfBands, sizeOfBands));
		
		long t = System.currentTimeMillis();
		for(Device d : devices){
			lsh.add(d);
		}
		insertionTime = System.currentTimeMillis() - t;
		
		for(Device d : devices){
			d.changeProps(numToChange, gen);
		}
		
		t = System.currentTimeMillis();
		for(Device d : devices){
			Collection<LSHable> results = lsh.search(d, searchThreshold);
			if(results != null && results.contains(d)){
				successes++;
			}else{
				failures++;
			}
		}
		searchTime = System.currentTimeMillis() - t;
		
		devices = null;
		System.gc();
		long currentMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		memoryRequired =  currentMemory - initialMemory;
		
		NumberFormat formatter = new DecimalFormat("#0.0"); 
		
		System.out.println(
		"\nSuccesses: " + successes
		+"\nFailures: " + failures
		+"\nAccuracy: " + formatter.format(100*successes/((double)failures + successes)) +"%"
		+"\nMemory: " + (memoryRequired/1000000) +"MB"
		+"\nInsertion Time: " + insertionTime
		+"\nSearch Time: " + searchTime
		+"\nTotal Buckets: " + lsh.getTotalBuckets()
		+"\nCollision Rate: " + formatter.format(100*(1 - lsh.getTotalBuckets()/(double)(numDevices*(numOfBands)))) + "%");

	}

	public static class DeviceGenerator {

		private final int numOfProperties;
		/*
		 * Variables to change the distributions of generated values.
		 * Data that has many common values may not hash as uniformly.
		 */
		private final int numOfOptionsPerAttribute = 10;
		private final int valueCeiling = 10000;
		private final int percentForWhichCapApplies = 100;
		protected final int[][] props;

		
		public DeviceGenerator(int numOfProperties) {
			this.numOfProperties = numOfProperties;
			props = new int[numOfProperties][numOfOptionsPerAttribute];

			Random r = new Random();
			for (int i = 0; i < numOfProperties; i++) {
				for (int j = 0; j < numOfOptionsPerAttribute; j++) {
					if(r.nextInt(100) < percentForWhichCapApplies){
						props[i][j] = r.nextInt(valueCeiling);
					}else{
						props[i][j] = r.nextInt();
					}
				}
			}			
		}

		public Device getRandomDevice() {

			Random r = new Random();
			Integer[] randomProps = new Integer[numOfProperties];
			for(int i = 0; i < numOfProperties; i++){
				randomProps[i] = props[i][r.nextInt(numOfOptionsPerAttribute)];
			}
			Device toReturn = new Device(randomProps);
			return toReturn;
		}
	}

	public static class Device implements LSHable {

		Integer[] props;

		public Device(Integer[] props) {
			this.props = props;
		}

		public void changeProps(int numToChange, DeviceGenerator gen) {
			int[][] dataSet = gen.props;
			Random r = new Random();
			for(int i = 0; i < numToChange; i++){
				int propToChange =  r.nextInt(props.length);
				props[propToChange] = dataSet[propToChange][r.nextInt(gen.numOfOptionsPerAttribute)];
			}
		}

		@Override
		public Iterable<Integer> getInput() {
			return Arrays.asList(props);
		}
	}
}
