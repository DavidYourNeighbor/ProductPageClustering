//@author:
//			David Naber
//
//@date:
//			8/7/2014
//			
//@description MutableInt:
//			This class was created so that integers could be easily managed in Vectors,
//			because ints cannot be stored in Vectors and because Integers cannot be changed
//			after initialization.

import java.util.Comparator;

public class MutableInt implements Comparable<MutableInt>
{
	private int value;
	
	public MutableInt()
	{
		value = 0;
	}
	
	public MutableInt(int newValue)
	{
		value = newValue;
	}

	public int getValue()
	{
		return this.value;
	}
	
	public void changeValue(int newValue)
	{
		this.value = newValue;
	}

	public void decrementValue()
	{
		this.value--;
	}

	public int compareTo(MutableInt arg0) {
		return Comparators.VALUE.compare(this, arg0);
	}
	
	public static class Comparators
	{
		public static Comparator<MutableInt> VALUE = new Comparator<MutableInt>() {
            public int compare(MutableInt o1, MutableInt o2) {
                return o1.value - o2.value;
            }
        };
	}
}
