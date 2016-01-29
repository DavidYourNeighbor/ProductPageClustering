//	@author:
//					David Naber
//
//	@date:
//					8/7/2014, 2:01PM
//
//	@classes:
//					ProductPageCluster.java
//					Triple.java
//					ProductPage.java
//					Inty.java
//
//	@description ProductPage:
//					This program clusters together HTML product pages based their extracted
//					text, using an Locality-Sensitive Hashing Algoritm, described here:
//
//						1. Pick a value of k and construct from each document the set of k-shingles.
//							Optionally, hash the k-shingles to shorter bucket numbers.
//						2. [NOT NEEDED HERE] Sort the document-shingle pairs to order them by shingle.
//						3. Pick a length n for the minhash signatures. Feed the sorted list to the
//							algorithm of Section 3.3.5 to compute the minhash signatures for all the
//							documents.
//						4. Choose a threshold t that defines how similar documents have to be in
//							order for them to be regarded as a desired “similar pair.” Pick a number
//							of bands b and a number of rows r such that b*r = n, and the threshold
//							t is approximately (1/b)^(1/r). If avoidance of false negatives is important,
//							you may wish to select b and r to produce a threshold lower than t; if
//							speed is important and you wish to limit false positives, select b and r to
//							produce a higher threshold.
//						5. Construct candidate pairs by applying the LSH technique of Section 3.4.1.
//						6. Examine each candidate pair’s signatures and determine whether the frac-
//							tion of components in which they agree is at least t.
//						7. Optionally, if the signatures are sufficiently similar, go to the documents
//							themselves and check that they are truly similar, rather than documents
//							that, by luck, had similar signatures.
//


import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

public class ProductPageCluster
{
	private static int lengthOfMinhashSignature = 2000; //2000
	private static int b = 500; //500
	private static int r = 4;
	private static double threshold = 0.2; // threshold is approximately (1/b)^(1/r),
										   //where b * r = lengthOfMinhashSignature
	
	public static void main(String args[])
	{	
		Vector<ProductPage> collectionOfDocuments = initializeAllDocuments();
		MutableInt highestHashValue = hashDocuments(collectionOfDocuments);
		calculateMinhashSignatures(collectionOfDocuments, highestHashValue);
		
		Vector<Vector<int[]>> bands = sortMinhashesIntoBands(collectionOfDocuments);
		Vector<Vector<MutableInt>> hashedBands = hashBands(bands, b*collectionOfDocuments.size(), collectionOfDocuments);
		Vector<Vector<ProductPage>> collectionOfBuckets = compareHashedBands(hashedBands, collectionOfDocuments);
		Vector<ProductPage> leftOvers = pruneBuckets(collectionOfBuckets);
		
		printBuckets(collectionOfBuckets);
		System.out.println("Size of leftovers is "+leftOvers.size());		
	}
	
	//Outputs the contents of the buckets (the final product)
	private static void printBuckets(Vector<Vector<ProductPage>> collectionOfBuckets)
	{
		for(int i = 0; i < 50; i++)
		{
			for(int j = 0; j < collectionOfBuckets.elementAt(i).size(); j++)
			{
				System.out.println("Bucket "+(i+1)+", Document "+(j+1)+": "+collectionOfBuckets.elementAt(i).elementAt(j).getDocLocation());
			}			
		}
	}
	
	//Prunes the buckets to finalize their contents
	private static Vector<ProductPage> pruneBuckets(Vector<Vector<ProductPage>> collectionOfBuckets)
	{
		Vector<ProductPage> random = new Vector<ProductPage>(); 
		
		for(int i = 0; i < collectionOfBuckets.size(); i++)
		{
			for(int j = 0; j < collectionOfBuckets.elementAt(i).size(); j++)
			{
				if(findJaccardSimilarity(collectionOfBuckets.elementAt(i).elementAt(0), collectionOfBuckets.elementAt(i).elementAt(j)) < threshold)
				{
					random.add(collectionOfBuckets.elementAt(i).remove(j));
				}
			}
		}
		
		return random;
	}

	//Compares the hashed bands
	private static Vector<Vector<ProductPage>> compareHashedBands(Vector<Vector<MutableInt>> hashedBands,	Vector<ProductPage> collectionOfDocuments)
	{
		Vector <Vector<ProductPage>> collectionOfBuckets = new Vector<Vector<ProductPage>>();
		Vector<ProductPage> tempVector = new Vector<ProductPage>();
		Vector<MutableInt> similar = new Vector<MutableInt>();
		MutableInt currDocInty = new MutableInt(0);
		int currDoc = 0;

		while(!hashedBands.elementAt(0).isEmpty())
		{
			similar.add(currDocInty);
			for(int iteratorBand = 0; iteratorBand < hashedBands.size(); iteratorBand++) //3 its
			{
				for(int iteratorDocument = currDoc+1; iteratorDocument < hashedBands.elementAt(0).size(); iteratorDocument++) //6 its
				{
					if(hashedBands.elementAt(iteratorBand).elementAt(currDoc).getValue() == hashedBands.elementAt(iteratorBand).elementAt(iteratorDocument).getValue())
					{
						MutableInt temp = new MutableInt(iteratorDocument);
						if(!contains(similar, temp))
						{
							similar.add(temp);
						}
					}
				}
			}

			similar.sort(null);
			while(!similar.isEmpty())
			{
				int tempint = similar.firstElement().getValue();
				tempVector.addElement(collectionOfDocuments.remove(tempint));
				for(int w = 0; w < hashedBands.size(); w++)
				{
					hashedBands.elementAt(w).removeElementAt(tempint);
				}
				similar.remove(0);
				for(int q = 0; q < similar.size(); q++)
				{
					similar.elementAt(q).decrementValue();	
				}
			}

			Vector<ProductPage> tempVector2 = new Vector<ProductPage>(tempVector); 
			collectionOfBuckets.add(tempVector2);
			tempVector.clear();
			
			if(!hashedBands.elementAt(0).isEmpty() && hashedBands.elementAt(0).size() == 1)
			{
				tempVector.add(collectionOfDocuments.remove(currDoc));
				for(int w = 0; w < hashedBands.size(); w++)
				{
					hashedBands.elementAt(w).removeElementAt(0);
				}
				collectionOfBuckets.add(tempVector);
				tempVector2 = new Vector<ProductPage>(tempVector); 
				collectionOfBuckets.add(tempVector2);
				tempVector.clear();
			}
		}

		return collectionOfBuckets;
	}

	//Determines whether there is a match
	private static boolean contains(Vector<MutableInt> similar, MutableInt temp)
	{
		for(int i = 0; i < similar.size(); i++)
		{
			if(similar.elementAt(i).getValue() == temp.getValue())
			{
				return true;
			}
		}
		return false;
	}

	//Hashes the bands of the collection of documents
	private static Vector<Vector<MutableInt>> hashBands(Vector<Vector<int[]>> bands, int highestHash, Vector<ProductPage> collectionOfDocuments)
	{
		Vector<Vector<MutableInt>> hashedBands = new Vector<Vector<MutableInt>>();		
		Vector<Triple> bandHashFunctions = findArrayHashFunctions(highestHash);
		
		for(int j = 0; j < b; j++)
		{
			Vector<MutableInt> hashedRows = new Vector<MutableInt>();
			for(int i = 0; i < collectionOfDocuments.size(); i++)
			{
				MutableInt tempHash = hashArray(bands.elementAt(i).elementAt(j), bandHashFunctions.elementAt(j));
				hashedRows.add(tempHash);
			}
			hashedBands.add(hashedRows);
		}
		
		return hashedBands;
	}

	//Hashes the array of elements
	private static MutableInt hashArray(int[] elements, Triple hashFunction)
	{
		int x = 0;
		for(int i : elements)
		{
			x+=i;
		}
		int a = hashFunction.getA();
		int b = hashFunction.getB();
		int c = hashFunction.getC();
		
		return new MutableInt(((a*x)+b)%c);
	}

	//Finds the necessary hash functions
	private static Vector<Triple> findArrayHashFunctions(int highestHashValue)
	{
		Vector<Triple> randomHashFunctionValues = new Vector<Triple>();		
		int allC = returnNextHighestPrimeNumber(highestHashValue);
		int maxRandomNumber = allC - 1;
		Random randomGenerator = new Random();
		
		for(int i = 0; i < b; i++)
		{
			int a = returnRandomNumber(1, maxRandomNumber, randomGenerator); //random number from 1 to c - 1
			int B = returnRandomNumber(1, maxRandomNumber, randomGenerator); // random number from 1 to c - 1
			Triple temp = new Triple(a, B, allC);	
			randomHashFunctionValues.add(temp);
		}
		
		return randomHashFunctionValues;
	}

	//Sorts minhash signatures into bands
	private static Vector<Vector<int[]>> sortMinhashesIntoBands(Vector<ProductPage> collectionOfDocuments)
	{
		Vector<Vector<int[]>> hashedBands = new Vector<Vector<int[]>>();

		for(int i = 0; i < collectionOfDocuments.size(); i++)
		{
			Vector<MutableInt> tempMinhashSignature = collectionOfDocuments.elementAt(i).getMinhashSignature();
			Vector<int[]> bandRow = new Vector<int[]>();
			for(int j = 0; j < lengthOfMinhashSignature; j+=r)
			{
				int[] tempArr = new int[r];
				for(int k = j; k < (j + r); k++)
				{
					tempArr[k-j] = tempMinhashSignature.elementAt(k).getValue();
				}
				bandRow.add(tempArr);
			}
			hashedBands.add(bandRow);
		}
		
		return hashedBands;
	}

	//Finds the similarity between two product pages
	private static double findJaccardSimilarity(ProductPage p1, ProductPage p2)
	{		
		double JS = 0.0;
		String[] p1Shingles = p1.getShinglesArray();
		String[] p2Shingles = p2.getShinglesArray();
		
		Set<String> p1Set = new HashSet<String>();
		Collections.addAll(p1Set, p1Shingles);
		
		Set<String> p2Set = new HashSet<String>();
		Collections.addAll(p2Set, p2Shingles);
		
		Set<String> union = new HashSet<String>();
		Collections.addAll(union, p1Shingles);
		union.addAll(p2Set);
		
		Set<String> intersection = new HashSet<String>();
		Collections.addAll(intersection, p1Shingles);
		intersection.retainAll(p2Set);
		
		double intersectionSize = (double) intersection.size();
		double unionSize = (double) union.size();
		
		JS = intersectionSize / unionSize;
		return JS;		
	}

	//Hashes each document in the collection
	private static MutableInt hashDocuments(Vector<ProductPage> collectionOfDocuments)
	{
		MutableInt highestAssignedHash = new MutableInt(0);
		MutableInt temp;
		int highestHash = getTotalShingles(collectionOfDocuments);
		for(ProductPage i : collectionOfDocuments)
		{
			temp = i.hashShingles(highestHash);
			if(temp.getValue() > highestAssignedHash.getValue())
			{
				highestAssignedHash.changeValue(temp.getValue());
			}
		}
		
		return highestAssignedHash;
	}
	
	//Returns the total number of shingles in the collection of documents
	private static int getTotalShingles(Vector<ProductPage> collectionOfDocuments)
	{
		int total = 0;
		
		for(ProductPage i : collectionOfDocuments)
		{
			total+=i.getSizeOfShingles();
		}
		
		return total;
	}

	//Calculates the minhash signatures
	private static void calculateMinhashSignatures(Vector<ProductPage> collectionOfDocuments, MutableInt highestHashValue)
	{
		Vector<Triple> randomHashFuntionValues = generateABandC(highestHashValue);
		for(ProductPage i : collectionOfDocuments)
		{
			i.findMinhashSignature(randomHashFuntionValues, lengthOfMinhashSignature);
		}
	}

	//Generates the pieces of the random hash functions
	private static Vector<Triple> generateABandC(MutableInt highestHashValue)
	{
		Vector<Triple> randomHashFunctionValues = new Vector<Triple>();		
		int allC = returnNextHighestPrimeNumber(highestHashValue.getValue());
		int maxRandomNumber = allC - 1;
		Random randomGenerator = new Random();
		
		for(int i = 0; i < lengthOfMinhashSignature; i++)
		{
			int a = returnRandomNumber(1, maxRandomNumber, randomGenerator); //random number from 1 to c - 1
			int B = returnRandomNumber(1, maxRandomNumber, randomGenerator); // random number from 1 to c - 1
			Triple temp = new Triple(a, B, allC);	
			randomHashFunctionValues.add(temp);
		}
		
		return randomHashFunctionValues;
	}
	
	//Returns a random number
	private static int returnRandomNumber(int start, int end, Random random)
	{
		long range = (long)end - (long)start + 1;
		long fraction = (long)(range * random.nextDouble());
		int randomNumber = ((int) fraction) + start; 
		return randomNumber;
	}

	//Returns the next highest prime number from a given number -- TODO: find more efficient method
	public static int returnNextHighestPrimeNumber(int starter)
	{	
		int temp = starter+1;
		while(!isPrime(temp))
		{
			temp++;
		}
		return temp;
	}

	//determines whether a number is prime
	public static boolean isPrime(int num)
	{		
		if(num == 2)
		{
			return(true);
		}

		for(int i = 2; i <= (int) Math.sqrt(num) + 1; i++)
		{
			if(num % i == 0)
			{
				return(false);
			}
		}
		return(true); //if all cases don't divide num, it is prime.
	}

	//Initializes the documents (small set) - testing
	public static Vector<ProductPage> initializeTestDocuments()
	{
		Vector<ProductPage> collectionOfDocuments = new Vector<ProductPage>();
		ProductPage testProductPage0 = new ProductPage("C:/Users/David Naber/Documents/IREP Project/product_pages/data/CollectedData/4Gamers PS4 Camera TV Clip Gaming Sensor Holder Gaming Accessory Price Comparison0");
		ProductPage testProductPage1 = new ProductPage("C:/Users/David Naber/Documents/IREP Project/product_pages/data/CollectedData/4Gamers PS4 Camera TV Clip Gaming Sensor Holder Gaming Accessory Price Comparison1");
		ProductPage testProductPage2 = new ProductPage("C:/Users/David Naber/Documents/IREP Project/product_pages/data/CollectedData/4Gamers PS4 Camera TV Clip Gaming Sensor Holder Gaming Accessory Price Comparison2");
		ProductPage testProductPage3 = new ProductPage("C:/Users/David Naber/Documents/IREP Project/product_pages/data/CollectedData/4Gamers PS4 Camera TV Clip Gaming Sensor Holder Gaming Accessory Price Comparison3");
		ProductPage testProductPage4 = new ProductPage("C:/Users/David Naber/Documents/IREP Project/product_pages/data/CollectedData/4Gamers PS4 Camera TV Clip Gaming Sensor Holder Gaming Accessory Price Comparison4");
		ProductPage testProductPage5 = new ProductPage("C:/Users/David Naber/Documents/IREP Project/product_pages/data/CollectedData/4Gamers PS4 Camera TV Clip Gaming Sensor Holder Gaming Accessory Price Comparison5");
		ProductPage testProductPage6 = new ProductPage("C:/Users/David Naber/Documents/IREP Project/product_pages/data/CollectedData/4Gamers PS4 Camera TV Clip Gaming Sensor Holder Gaming Accessory Price Comparison6");
		ProductPage testProductPage7 = new ProductPage("C:/Users/David Naber/Documents/IREP Project/product_pages/data/CollectedData/4Gamers PS4 Camera TV Clip Gaming Sensor Holder Gaming Accessory Price Comparison7");
		ProductPage testProductPage8 = new ProductPage("C:/Users/David Naber/Documents/IREP Project/product_pages/data/CollectedData/4Gamers PS4 Camera TV Clip Gaming Sensor Holder Gaming Accessory Price Comparison8");
		ProductPage testProductPage9 = new ProductPage("C:/Users/David Naber/Documents/IREP Project/product_pages/data/CollectedData/4Gamers PS4 Camera TV Clip Gaming Sensor Holder Gaming Accessory Price Comparison9");
		/*ProductPage testProductPage10 = new ProductPage("C:/Users/David Naber/Documents/IREP Project/product_pages/data/CollectedData/Acer Aspire E1-570 Notebook Laptop Price Comparison0");
		ProductPage testProductPage11 = new ProductPage("C:/Users/David Naber/Documents/IREP Project/product_pages/data/CollectedData/Acer Aspire E1-570 Notebook Laptop Price Comparison1");
		ProductPage testProductPage12 = new ProductPage("C:/Users/David Naber/Documents/IREP Project/product_pages/data/CollectedData/Acer Aspire E1-570 Notebook Laptop Price Comparison2");
		ProductPage testProductPage13 = new ProductPage("C:/Users/David Naber/Documents/IREP Project/product_pages/data/CollectedData/Acer Aspire E1-570 Notebook Laptop Price Comparison3");
		ProductPage testProductPage14 = new ProductPage("C:/Users/David Naber/Documents/IREP Project/product_pages/data/CollectedData/Acer Aspire E1-570 Notebook Laptop Price Comparison4");
		ProductPage testProductPage15 = new ProductPage("C:/Users/David Naber/Documents/IREP Project/product_pages/data/CollectedData/Acer Aspire E1-570 Notebook Laptop Price Comparison5");
		ProductPage testProductPage16 = new ProductPage("C:/Users/David Naber/Documents/IREP Project/product_pages/data/CollectedData/Acer Aspire E1-570 Notebook Laptop Price Comparison6");
		ProductPage testProductPage17 = new ProductPage("C:/Users/David Naber/Documents/IREP Project/product_pages/data/CollectedData/Acer Aspire E1-570 Notebook Laptop Price Comparison7");
		ProductPage testProductPage18 = new ProductPage("C:/Users/David Naber/Documents/IREP Project/product_pages/data/CollectedData/Acer Aspire E1-570 Notebook Laptop Price Comparison8");
		ProductPage testProductPage19 = new ProductPage("C:/Users/David Naber/Documents/IREP Project/product_pages/data/CollectedData/Acer Aspire E1-570 Notebook Laptop Price Comparison9");*/
																				
		collectionOfDocuments.add(testProductPage0);
		collectionOfDocuments.add(testProductPage1);
		collectionOfDocuments.add(testProductPage2);
		collectionOfDocuments.add(testProductPage3);
		collectionOfDocuments.add(testProductPage4);
		collectionOfDocuments.add(testProductPage5);
		collectionOfDocuments.add(testProductPage6);
		collectionOfDocuments.add(testProductPage7);
		collectionOfDocuments.add(testProductPage8);
		collectionOfDocuments.add(testProductPage9);
		/*collectionOfDocuments.add(testProductPage10);
		collectionOfDocuments.add(testProductPage11);
		collectionOfDocuments.add(testProductPage12);
		collectionOfDocuments.add(testProductPage13);
		collectionOfDocuments.add(testProductPage14);
		collectionOfDocuments.add(testProductPage15);
		collectionOfDocuments.add(testProductPage16);
		collectionOfDocuments.add(testProductPage17);
		collectionOfDocuments.add(testProductPage18);
		collectionOfDocuments.add(testProductPage19);*/
		
		System.out.println("Done initializing documents.");
		return collectionOfDocuments;

	}	
	
	//Initializes the documents (entire set)
	public static Vector<ProductPage> initializeAllDocuments()
	{
		Vector<ProductPage> collectionOfDocuments = new Vector<ProductPage>();
		ProductPage tempProductPage;
		String tempProductPageTitle = "";
		
		File[] files = new File("C:/Users/David Naber/Documents/IREP Project/product_pages/data/CollectedData/").listFiles();
		
		
		for (int i = 0; i < files.length; i++) 
	    {
	    	File file = files[i];
	    	tempProductPageTitle = "C:/Users/David Naber/Documents/IREP Project/product_pages/data/CollectedData/" + file.getName();
	    	tempProductPage = new ProductPage(tempProductPageTitle);
	    	collectionOfDocuments.add(tempProductPage);
	    	tempProductPageTitle = "";
	    }
		
	    System.out.println("Done initializing documents.");
		return collectionOfDocuments;
	}
	
}
