package lsh.test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import lsh.LSHable;
import lsh.impl.LSHImpl;
import lsh.strategies.LSHStrategy;

public class Newsgroups {

	public static void main(String[] args) throws Exception{
		
		/*
		 * Algorithm input
		 */
		//Ignore devices whose signatures are less than this % similar to our input.
		int searchThreshold = 70;
		//Number of hashes to store per device.
		int numOfBands = 40;
		//Number of minhashes to join together per band. More minhashes yields greater precision but less recall.
		int sizeOfBands = 3;
		
		//best: 90/13. Yields 99% accuracy, 350 search time		
		int successes = 0;
		int failures = 0;
		long insertionTime = 0;
		long searchTime = 0;
		long memoryRequired = 0;
		
		int numFiles = 0;
		
		//LSHImpl lsh = new LSHImpl(200, 10, LSHStrategy.STRATEGIES.gregsStrategy(numOfBands*sizeOfBands, sizeOfBands));
		LSHImpl lsh = new LSHImpl(200, 10, LSHStrategy.STRATEGIES.randomHashStrategy(numOfBands*sizeOfBands, sizeOfBands));	
		
		List<NewsDoc> docs = new ArrayList<NewsDoc>();
		
		Map<String, List<File>> documents = getDocuments("20news-18828");
		
		for(String key : documents.keySet()){
			List<File> l = documents.get(key);
			for(File doc : l){
				String str = readFile(doc);
				NewsDoc news = new NewsDoc(key, str);
				docs.add(news);
			}
		}
		
		/*
		 * Remove common word pairs
		 */
		
		//todo: learn scala or java8 so you don't have to do this bullshit
		
		Map<String, AtomicInteger> wordCount = new HashMap<String, AtomicInteger>();
		for(NewsDoc doc : docs){
			for(String pair : doc.tuples){
				AtomicInteger i = wordCount.get(pair);
				if(i == null){
					i = new AtomicInteger(0);
					wordCount.put(pair,  i);
				}
				i.incrementAndGet();
			}
		}
		
		Collection<AtomicInteger> coll = wordCount.values();
		
		ArrayList<AtomicInteger> al = new ArrayList<AtomicInteger>(coll);
	
	
		al.sort((AtomicInteger arg0, AtomicInteger arg1) -> {
			if(arg0.get() == arg1.get())
				return 0;
			return arg0.get() < arg1.get() ? -1 : 1;
		});
		
		
		for(NewsDoc doc : docs){
			Object[] array = doc.tuples.toArray();
			for(Object tuple : array){
				String str = (String)tuple;
				if(wordCount.get(str).get() > 2000){
					doc.tuples.remove(str);
				}					
			}
		}
		
		/*
		 * Add to lsh
		 */
		
		wordCount  = null;
		System.gc();
		long initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		
		long t = System.currentTimeMillis();
		for(NewsDoc doc : docs){
			lsh.add(doc);
			numFiles++;
		}
		insertionTime = System.currentTimeMillis() - t;
		t = System.currentTimeMillis();
		
		System.gc();
		long currentMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		memoryRequired =  currentMemory - initialMemory;
		System.out.println("Memory required: " + (memoryRequired/1000000) +"MB");
		
		for(NewsDoc doc : docs){
			Collection<LSHable> similar = lsh.search(doc, searchThreshold);
			for(LSHable l : similar){
				NewsDoc news = (NewsDoc)l;
				if(news == doc)
					continue;
				if(((NewsDoc)l).category.equals(doc.category)){
					successes++;
				}else{
					failures++;
				}
			}
		}
		
		searchTime = System.currentTimeMillis() - t;
		
		NumberFormat formatter = new DecimalFormat("#0.0"); 
		
		System.out.println(
		"\nSuccesses: " + successes
		+"\nFailures: " + failures
		+"\nAccuracy: " + formatter.format(100*successes/((double)failures + successes)) +"%"
		+"\nMemory: " + (memoryRequired/1000000) +"MB"
		+"\nInsertion Time: " + insertionTime
		+"\nSearch Time: " + searchTime
		+"\nTotal Buckets: " + lsh.getTotalBuckets()
		+"\nCollision Rate: " + formatter.format(100*(1 - lsh.getTotalBuckets()/(double)(numFiles*(numOfBands)))) + "%");

	}

	
	/*
	 * Returns a map from Category name to Document
	 */
	private static Map<String, List<File>> getDocuments(String location) {
		File[] files = new File(location).listFiles();
		Map<String, List<File>> toReturn = new HashMap<String, List<File>>();
	    return showFiles(files, toReturn);
	}

	public static Map<String, List<File>> showFiles(File[] files, Map<String, List<File>> toReturn) {
		
	    for (File file : files) {
	        if (file.isDirectory()) {
	            showFiles(file.listFiles(), toReturn); // Calls same method again.
	        } else {
	        	String cat = file.getParentFile().getName();
	        	List<File> l = toReturn.get(cat);
	        	if(l == null){
	        		l = new ArrayList<File>();
	        		toReturn.put(cat, l);
	        	}	        		
	        	l.add(file);
	        }
	    }
	    return toReturn;
	}
	
	public static String readFile(File file) throws IOException
	{
	    String content = null;
	    FileReader reader = null;
	    try {
	        reader = new FileReader(file);
	        char[] chars = new char[(int) file.length()];
	        reader.read(chars);
	        content = new String(chars);
	        reader.close();
	    } catch (IOException e) {
	        e.printStackTrace();
	    } finally {
	        if(reader !=null){reader.close();}
	    }
	    return content;
	}
	
	public static class NewsDoc implements LSHable{
		
		private final String input;
		public final List<String> tuples = new ArrayList<String>();
		final String category;
		
		
		public NewsDoc(String category, String input){
			this.category = category;
			this.input = input;
			/*int index = input.indexOf("writes:");
			if(index == -1)
				throw new RuntimeException("Document doesn't have a writes line");
			String body = input.substring(index + 7);*/
			String[] words = input.split("\\s");
			
			String lastWord = "";
			for(String word : words){
				tuples.add(lastWord + " " + word);
				lastWord = word;
			}			
		}
		
		private List<Integer> getTuplesAsIntegers(){
			List<Integer> toReturn = new ArrayList<Integer>();
			for(String str : tuples){
				toReturn.add(str.hashCode());
			}
			return toReturn;
		}

		@Override
		public Iterable<Integer> getInput() {
			return getTuplesAsIntegers();
		}
	}
	
}
