package additions;

/**
 * This class represents a "component" of a DSM used to find cyclic groups.
 * A "component" is probably a file in a piece of software.<br>
 * <br>
 * Each CycleComponent consists of fields containing its VFI and VFO,
 * as well as the count of other possible elements in its cyclic group and
 * the cyclic group number to which it has been assigned.
 * 
 * @author YuliyaA
 *
 */
public class CycleComponent implements Comparable<CycleComponent>
{
	/** The index of the component in the visibility matrix. */
	private int visIndex;	//currently unused
	/** Fan-in visibility. */
	private double vfi;
	/** Fan-out visibility. */
	private double vfo;
	/** Count of components preceding and including this one that
	 *  have the same VFI and VFO. */
	private int countM;
	/** The maximum size of the cycle to which this component can
	 *  be assigned. Must be less than or equal to the minimum of
	 *  the VFO, VFI, and countM for a group of components. */
	private int maxCycleSize;
	/** Cycle number to which this component is assigned. */
	private int cyclicGroup;	//currently unused
	
	/**
	 * Default blank constructor.
	 */
	public CycleComponent()
	{
		visIndex = 0;
		vfi = 0;
		vfo = 0;
		countM = 0;
		maxCycleSize = 0;
		cyclicGroup = 0;
	}
	
	/**
	 * Constructor with specified VFI and VFO values, but default 0 count and cycle.
	 * 
	 * @param xvfi  the specified Fan-In Visibility value of this component
	 * @param xvfo  the specified Fan-Out Visibility value of this component
	 */
	public CycleComponent(int index, double xvfi, double xvfo)
	{
		visIndex = index;
		vfi = xvfi;
		vfo = xvfo;
		countM = 0;
		cyclicGroup = 0;
	}
	
	/**
	 * Constructor with all values specified.
	 * 
	 * @param xvfi  the specified Fan-In Visibility value of this component
	 * @param xvfo  the specified Fan-Out Visibility value of this component
	 * @param count  the specified count of objects before and including this one
	 * that have the same VFI and VFO
	 * @param size  the specified maximum size of the cycle to which this component
	 * can belong
	 * @param cycle  the specified number cycle that this component belongs to
	 */
	public CycleComponent(int index, double xvfi, double xvfo,
						  int count, int size, int cycle)
	{
		visIndex = index;
		vfi = xvfi;
		vfo = xvfo;
		countM = count;
		maxCycleSize = size;
		cyclicGroup = cycle;
	}
	
	/**
	 * Sets the countM field to the given value.
	 * 
	 * @param newCount  the new count to set countM to
	 */
	public void setCount(int newCount)
	{
		countM = newCount;
	}
	/**
	 * Sets the maximum cycle size to the given value.
	 * 
	 * @param newSize  the new maximum size
	 */
	public void setMaxCycleSize(int newSize)
	{
		maxCycleSize = newSize;
	}
	/**
	 * Sets the cycleN field to the given value.
	 * 
	 * @param newCycle  the new cycle number to which this component belongs
	 */
	public void setCycle(int newCycle)
	{
		cyclicGroup = newCycle;
	}
	
	/**
	 * Gets the index of the component in the visibility matrix.
	 * 
	 * @return  the index in the visibility matrix
	 */
	public int getVisIndex()
	{
		return visIndex;
	}
	/**
	 * Gets the Fan-In Visibility of the component.
	 * 
	 * @return  the Fan-In Visibility
	 */
	public double getVFI()
	{
		return vfi;
	}
	/**
	 * Gets the Fan-Out Visibility of the component.
	 * 
	 * @return  the Fan-Out Visibility
	 */
	public double getVFO()
	{
		return vfo;
	}
	
	/**
	 * Gets the number of components preceding and including this one,
	 * according to a properly sorted list or array, that have the same
	 * VFI and VFO.
	 * 
	 * @return  the countM field of the component
	 */
	public int getCount()
	{
		return countM;
	}
	/**
	 * Gets the maximum size of the cycle to which this component can
	 * belong. Must be less than or equal to the minimum of the VFI, VFO,
	 * and countM for a group of components.
	 * 
	 * @return  the maximum cycle size
	 */
	public int getMaxCycleSize()
	{
		return maxCycleSize;
	}
	/**
	 * Gets the cyclic group to which this element belongs.
	 * 
	 * @return  the number of the cyclic group
	 */
	public int getCyclicGroup()
	{
		return cyclicGroup;
	}

	@Override
	/**
	 * Compares two CycleComponent objects to each other.
	 * First sorts them in descending order by VFI, then in ascending
	 * order by VFO.
	 * 
	 * This was intended to be used with the Arrays.sort() method, so
	 * this returns -1 for less than when the component should precede
	 * another component in an array sorted in "ascending" order by the
	 * Arrays.sort() method, and returns 1 when it should succeed another
	 * component. Returns 0 for components that have the same VFI and VFO.
	 */
	public int compareTo(CycleComponent o)
	{
		double compVFI = this.vfi - o.getVFI();
		if(compVFI > 0)
		{
			return -1;
		}
		else if(compVFI < 0)
		{
			return 1;
		}
		else
		{
			double compVFO = this.vfo - o.getVFO();
			if(compVFO > 0)
			{
				return 1;
			}
			else if(compVFO < 0)
			{
				return -1;
			}
			else
			{
				return 0;
			}
		}
	}

	/*
	//currently unused. Also, not sure what constitutes equivalence yet.
	@Override
	public boolean equals(Object o)
	{
		if(o == null)
		{
			return false;
		}
		if(!(this.getClass().equals(o.getClass())))
		{
			return false;
		}
		else
		{
			if(this.vfi == ((CycleComponent) o).getVFI()
					&& this.vfo == ((CycleComponent) o).getVFO())
			{
				return true;
			}
			else
			{
				return false;
			}
		}
	}
	*/
}
