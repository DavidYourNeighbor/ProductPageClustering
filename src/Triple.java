//@author:
//			David Naber
//			
//@date:		
//			8/7/2014
//			
//@description TripleInt:
//			This class was created to allow for easy management of randomly generated hash function values.

public class Triple {
	
	private int a;
	private int b;
	private int c;
	
	public Triple(int aa, int bb, int cc)
	{
		a = aa;
		b = bb;
		c = cc;
	}
	
	public int getA()
	{
		return a;
	}
	
	public int getB()
	{
		return b;
	}

	public int getC()
	{
		return c;
	}
}
