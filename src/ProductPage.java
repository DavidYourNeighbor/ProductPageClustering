//	@author:	
//			David Naber
//			
//	@date:
//			8/7/2014
//			
//	@description ProductPage:
//				This class was created to allow for easier management of resources in the
//				ProductPageCluster class.


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Vector;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.extractors.ArticleExtractor;


public class ProductPage
{
	private static int wordsPerShingle = 1;
	
	private String docLocation;
	private String docText;
	private Vector<String> shingles;
	private Vector<MutableInt> hashedShingles;
	private Vector<MutableInt> minhashSignature;
	private String[] shinglesArray;
	
	public ProductPage(String location)
	{
		docLocation = location;
		shingles = new Vector<String>();
		hashedShingles = new Vector<MutableInt>();
		minhashSignature = new Vector<MutableInt>();
		
		File tempFile = new File(docLocation);
		try {
			FileReader fr = new FileReader(tempFile);
			try {
				docText = ArticleExtractor.INSTANCE.getText(fr);
			} catch (BoilerpipeProcessingException e) {
				System.out.println("Unfortunately, the boilerplate couldn't be extracted from the FileReader.");
			}
		} catch (FileNotFoundException e) {
			System.out.println("File could not be parsed into a FileReader");
		}		

		this.createShingles();
		shinglesArray = new String[shingles.size()];
		for(int i = 0; i < shingles.size(); i++)
		{
			shinglesArray[i] = shingles.elementAt(i);
		}
	}
	
	// returns the location of a document
	public String getDocLocation()
	{
		return docLocation;
	}
	
	// finds minhash signnature for a single product page
	public void findMinhashSignature(Vector<Triple> hashFunctions, int lengthOfMinhashSignature)
	{
		for(int i = 0; i < lengthOfMinhashSignature; i++)
		{
			MutableInt temp = this.findSingleMinhash(hashFunctions.elementAt(i));
			minhashSignature.add(temp);
		}
	}
	
	// returns minhash signature for a page
	public Vector<MutableInt> getMinhashSignature()
	{
		return this.minhashSignature;
	}
	
	// finds the single minhash value -- iterated several times to find the minhash signature for each page
	private MutableInt findSingleMinhash(Triple hashFunction)
	{
		MutableInt minhash = new MutableInt(10000000);
		
		for(MutableInt x : hashedShingles)
		{
			int a = hashFunction.getA();
			int b = hashFunction.getB();
			int c = hashFunction.getC();
			int hashVal = ((a*x.getValue())+b)%c;
			
			if(hashVal < minhash.getValue())
			{
				minhash.changeValue(hashVal);
			}		
		}
		return minhash;
	}

	// decomposes string containing HTML page's text into shingles (in our case, most likely unigrams)
	public void createShingles()
	{
		int shingleCounter = wordsPerShingle-1;
		String temp = "";
		
		for(int i = 0; i < docText.length(); i++)
		{
			if(docText.charAt(i) == '!' || docText.charAt(i) == '?' || docText.charAt(i) == ','|| docText.charAt(i) == '.')
			{
				
			}
			else if(docText.charAt(i) == ' ' && temp == "")
			{
				
			}
			else if(docText.charAt(i) == ' ' && shingleCounter == 0)
			{
				shingles.add(temp);
				temp = "";
				shingleCounter = wordsPerShingle-1;
			}
			else if(docText.charAt(i) == ' ')
			{
				temp+=docText.charAt(i);
				shingleCounter--;
			}
			else
			{
				temp+=docText.charAt(i);
			}
		}
	}
	
	// prints shingles array
	public void printShingles()
	{
		for(int i = 0; i < shingles.size(); i++)
		{
			System.out.println(shingles.elementAt(i));
		}
	}
	
	// prints minhash signature for a page
	public void printMinhashSignature()
	{
		for(int i = 0; i < minhashSignature.size(); i++)
		{
			System.out.println("Minhash signature element "+i+": "+minhashSignature.elementAt(i).getValue());
		}
	}
	
	// returns vector of shingles
	public Vector<String> getShingles()
	{
		return this.shingles;
	}
	
	// hashes the shingles array
	public MutableInt hashShingles(int highestHash)
	{
		MutableInt temphash;
		MutableInt highestAssignedHash = new MutableInt(0);
		for(String i : shingles)
		{
			temphash = new MutableInt(StringHasher(i, highestHash));
			hashedShingles.add(temphash);
			if(temphash.getValue() > highestAssignedHash.getValue())
			{
				highestAssignedHash.changeValue(temphash.getValue());
			}
		}
		
		return highestAssignedHash;
	}
	
	// prints hashed shingles
	public void printHashedShingles()
	{
		for(MutableInt i : hashedShingles)
		{
			System.out.println(i.getValue());
		}
	}

	// hashes strings
	public static int StringHasher(String s, int M)
	{
		//Hashes a string, resulting in values from 0 to M - 1.
	     int intLength = s.length() / 4;
	     int sum = 0;
	     
	     for (int j = 0; j < intLength; j++)
	     {
	    	 char c[] = s.substring(j * 4, (j * 4) + 4).toCharArray();
	    	 int mult = 1;
	    	 
	    	 for (int k = 0; k < c.length; k++)
	    	 {
	    		 sum += c[k] * mult;
	    		 mult *= 256;
	    	 }
	     }

	     char c[] = s.substring(intLength * 4).toCharArray();
	     int mult = 1;
	     
	     for (int k = 0; k < c.length; k++)
	     {
	    	sum += c[k] * mult;
	       	mult *= 256;
	     }

	     return(Math.abs(sum) % M);
	}
	
	// returns size of shingles array
	public int getSizeOfShingles()
	{
		return shingles.size();
	}
	
	// returns array of shingles
	public String[] getShinglesArray()
	{
		return this.shinglesArray;
	}
}
