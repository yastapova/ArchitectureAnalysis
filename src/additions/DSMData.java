package additions;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Arrays;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.jblas.DoubleMatrix;
import org.xml.sax.SAXException;

import edu.carleton.tim.jdsm.DesignStructureMatrix;
import edu.carleton.tim.jdsm.dependency.Dependency;
import edu.carleton.tim.jdsm.dependency.DependencyDSM;
import edu.carleton.tim.jdsm.dependency.analysis.SVGOutput;
import edu.carleton.tim.jdsm.dependency.provider.DependencyFinderDSMProvider;




/**
 * This class can be used to create a DSM and calculate some other information
 * related to it, such as the visibility matrix, VFI, VFO, propagation cost,
 * and Core size.<br>
 * <br>
 * A DSM can be created in two ways:<br>
 * 	1. You can provide a path to the dependencies file created by Dependency Finder
 * and a DSM will be created and all of the information will be calculated automatically.<br>
 * 	2. You can create a blank DSM, and fill it in yourself by calling each method
 * separately.<br>
 * <br>
 * The correct sequence of methods is:
 * <ol>
 * 	<li><code>getDSMFromDep(String depFilePath)</code></li>
 * 	<li><code>recalcDepMatrix(DesignStructureMatrix<Dependency>)</code></li>
 * 	<li><code>calcVisibility()</code></li>
 * 	<li><code>calcFanInOut()</code></li>
 * 	<li><code>calcPropCost()</code></li>
 * 	<li><code>calcPropCostSelf()</code> (Optional)</li>
 * 	<li><code>findCoreSize()</code></li>
 * </ol>
 * <br><br>
 * Another method, <code>getDSMFromXML(String dsmFilePath)</code>, has also been provided as an
 * alternative to step #1, if you have the DSM saved in XML format as given by jDSM.
 * However, this does not appear to work properly with jDSM's given loading method.<br>
 * <br>
 * Also, this class includes the static method
 * <code>DSMFromDepToXML(String depFilePath, String outputFolder)</code>, which can be used to
 * create DSMs from a given dependency file from Dependency Finder and save it through
 * jDSM into XML format, as well as create an image of the DSM in SVG format (viewable in
 * a web browser).<br>
 * <br>
 * Using the <code>findCoreSize()</code> method, you can calculate the Core size
 * of the given DSM. However, this class does not at the present time actually assign the
 * components to cyclic groups. It only calculates the maximum size that would be possible
 * for the largest cyclic group, or the Core.
 * 
 * @author YuliyaA
 *
 */
public class DSMData
{
	/** Logger to print messages to user. */
	private static Logger logger = Logger.getLogger(DSMData.class);
	/** The dependency matrix with which this data is associated. */
	private DoubleMatrix dep;
	/** The visibility matrix for the given DSM. */
	private DoubleMatrix vis;
	/** The number of files in the DSM. */
	private int nrFiles;
	/** Fan in visibility for each element. */
	private DoubleMatrix vfi;
	/** Fan out visibility for each element. */
	private DoubleMatrix vfo;
	/** The propagation cost. */
	private double propCost;
	/** The propagation cost with self dependencies. */
	private double propCost2;
	/** Array of objects that contain data about each component of the DSM. */
	private CycleComponent[] components;
	/** The Core size of the DSM. (The maximum size of the largest cyclic group.) */
	private int coreSize;
	/** Determines whether or not to calculate propCost2. */
	private boolean calcPropCost2;
	
	/**
	 * Default blank constructor.
	 */
	public DSMData()
	{
		dep = null;
		vis = null;
		nrFiles = 0;
		vfi = null;
		vfo = null;
		propCost = 0;
		components = null;
		coreSize = 0;
		calcPropCost2 = false;
	}
	
	/**
	 * Constructor for a DSM from an XML file provided as output
	 * from DependencyFinder.
	 * 
	 * @param calcPropCost2  true to also calculate propagation cost with self-dependencies
	 * @param depFilePath  the path to the XML dependencies file
	 * @param filter  the filter to use when analyzing the DSM
	 */
	public DSMData(boolean calcPropCost2, String depFilePath, String filter)
	{
		this.calcPropCost2 = calcPropCost2;
		dep = null;
		vis = null;
		recalcDepMatrix(getDSMFromDep(depFilePath, filter));
		calcVisibility();
		calcFanInOut();
		calcPropCost();
		calcPropCostSelf();
		components = null;
		coreSize = 0;
		findCoreSize();
	}

	/**
	 * Main method used for testing and debugging.
	 * @param args
	 */
	public static void main(String[] args)
	{
		String path = "../results/pattern/dependencies.xml";
		String filter = "/^com.aptima.netstorm.algorithms.aptima.*/,/^influent.idl.*/";
		boolean calcPropCostSelf = true;
		
		DSMData data1 = new DSMData(calcPropCostSelf, path, filter);
	}
	
	/**
	 * Static method to take an XML dependencies file from DependencyFinder,
	 * create a DSM from it, and save the DSM to an XML file and an SVG file.
	 * 
	 * @param depFilePath  the path to the XML dependencies file
	 * @param outputFolder  the path to the folder that will contain the output files
	 */
	public static void DSMFromDepToXML(String depFilePath, String outputFolder)
	{
		try
		{
			String filter = "[.*]";
			DesignStructureMatrix<Dependency> dsm = DependencyFinderDSMProvider
				.loadDesignStructureMatrix(depFilePath, filter);
			
			SVGOutput.printDsm(dsm, new FileOutputStream(outputFolder + "/dsm_original.svg"));
			dsm.saveToXml(new FileOutputStream(outputFolder + "/dsm_original.xml"));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Load a DSM from an XML file of dependencies provided by DependencyFinder.
	 * 
	 * You have to use this rather than load it from an XML file of the DSM that can
	 * be created by jDSM because apparently when you load it from an XML file from jDSM,
	 * it's not the same as the DSM before saving it. ???
	 * 
	 * @param depFilePath  path to the dependencies XML file
	 * @param filter  the filter to use when analyzing the DSM
	 * @returns  the DSM
	 */
	public DesignStructureMatrix<Dependency> getDSMFromDep(String depFilePath, String filter)
	{
		try
		{
			return DependencyFinderDSMProvider
				.loadDesignStructureMatrix(depFilePath, filter);
		}
		catch (FileNotFoundException | JAXBException | SAXException | ParserConfigurationException e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Recalculates the DependencyDSM into a DoubleMatrix form
	 * with Dependency.YES = 1 and Dependency.NO = 0, and saves it
	 * into the dep field.
	 * 
	 * @param d  the DSM
	 */
	public void recalcDepMatrix(DesignStructureMatrix<Dependency> d)
	{
		Dependency[][] data = d.getMap();
		nrFiles = data.length;
		this.dep = DoubleMatrix.zeros(nrFiles, nrFiles);
		for(int i = 0; i < data.length; i++)
		{
			for(int j = 0; j < data[i].length; j++)
			{
				if(data[i][j].equals(Dependency.YES))
				{
					this.dep.put(i, j, 1.0);
				}
			}
		}
	}
	
	/**
	 * Method to calculate the Visibility Matrix of the given DSM.
	 * This must be done after creating the DSM and before calculating
	 * any data, such as Propagation Cost, VFI, or VFO.
	 * <br><br>
	 * The DSM must be properly initialized prior to calling this method.
	 */
	public void calcVisibility()
	{
		if(dep == null)
		{
			logger.info("Dependency Matrix has not been properly initialized.");
		}
		else
		{
			//DoubleMatrix zeros = DoubleMatrix.zeros(nrFiles, nrFiles);

			DoubleMatrix temp = DoubleMatrix.eye(nrFiles);
			DoubleMatrix visibility;
			
			visibility = DoubleMatrix.zeros(nrFiles, nrFiles);
			
			logger.info("Beginning Visibility Matrix calculation.");
			
			int count = 0;
			while(count < nrFiles && temp.truth().sum() != 0)	// && !temp.equals(zeros)
			{
				count++;
				if (count % 50 == 0) {
					logger.info("Processed " + count + " of " + nrFiles);
				}
				temp = temp.mmul(this.dep);
				visibility = visibility.add(temp);
			}
			
			vis = visibility;
			logger.info("Finished Visibility Matrix calculation.");
		}
	}
	
	/**
	 * Calculate the fan-in and fan-out visibilities for each element.
	 * <br><br>
	 * The visibility matrix must be properly initialized prior to calling this method.
	 */
	public void calcFanInOut()
	{
		logger.info("Calculating VFI and VFO.");
		if(vis == null)
		{
			logger.info("Error: The visibility matrix has not been properly initialized.");
		}
		else
		{
			DoubleMatrix temp = vis.truth();
			this.vfi = temp.columnSums();
			this.vfo = temp.rowSums();
			double isum = this.vfi.sum();
			double osum = this.vfo.sum();
			if(isum == osum)
			{
				logger.info("VFI sum: " + isum + "\tVFO sum: " + osum);
			}
			else
			{
				logger.info("The VFI and VFO are not correct. Their sums should be equal. \nVFI sum: "
						+ isum + "\tVFO sum: " + osum);
			}
		}
	}
	
	/**
	 * Calculate the Propagation Cost by dividing the sum of
	 * the VFI sum or VFO sum by the total size of the DSM (nrFiles * nrFiles).
	 * <br>
	 * Calculates the propagation cost both with and without self-dependencies.
	 * <br><br>
	 * The VFI and VFO must be properly calculated prior to calling this method.
	 */
	public void calcPropCost()
	{
		logger.info("Calculating Propagation Cost.");
		if(vfi == null || vfo == null)
		{
			logger.info("Error: VFI and VFO are not properly initialized yet.");
		}
		else
		{
			double cost1 = this.vfi.sum()/(this.nrFiles * this.nrFiles);
			double cost2 = this.vfo.sum()/(this.nrFiles * this.nrFiles);
			if(cost1 == cost2)
			{
				this.propCost = cost1;
				logger.info("Propagation Cost: " + cost1);
			}
			else
			{
				this.propCost = (cost1 + cost2)/2;
				logger.info("VFI and VFO calculated incorrectly. Propagation cost has been set to the mean of both costs."
						+ "\nPropagation Cost: " + this.propCost);
			}
		}
	}
	
	/**
	 * Calculates the propagation cost with self-dependencies. This is the original
	 * formula used by jDSM, but just much faster. This method only executes if the
	 * <code>calcPropCost2</code> instance variable is set to true.
	 * <br><br>
	 * The visibility matrix must be properly calculated prior to calling this method.
	 */
	public void calcPropCostSelf()
	{
		if(vis == null)
		{
			logger.info("Error: The visibility matrix has not been properly initialized.");
		}
		else
		{
			if(calcPropCost2)
			{
				logger.info("Calculating propagation cost with self-dependencies.");
				
				DoubleMatrix temp = DoubleMatrix.eye(nrFiles).add(vis);
				temp = temp.truth();
				propCost2 = temp.sum() / (nrFiles * nrFiles);
				
				logger.info("Propagation Cost with self-dependencies: " + propCost2);
			}
		}
	}
	
	
	/**
	 * Initializes the array of cyclic components (<code>CycleComponent</code>)
	 * that will be used in finding cyclic groups. Also sorts the array as per the
	 * order defined in the <code>compareTo()</code> method in <code>CycleComponent</code>,
	 * which is descending by a component's VFI, then ascending by VFO.
	 * <br><br>
	 * The VFI and VFO must be properly calculated prior to calling this method.
	 */
	private void initializeCyclicComponents()
	{
		logger.info("Creating list of cyclic group components.");
		if(vfi == null || vfo == null)
		{
			logger.info("Error: VFI and VFO are not properly initialized yet.");
		}
		else
		{
			components = new CycleComponent[nrFiles];
			for(int i = 0; i < nrFiles; i++)
			{
				components[i] = new CycleComponent(i, vfi.get(i), vfo.get(i));
			}
			Arrays.sort(components);
			logger.info("Finished list of cyclic group components.");
		}
	}
	
	/**
	 * Wrapper method that groups and calls all Core-related methods together.
	 */
	public void findCoreSize()
	{
		initializeCyclicComponents();
		findCounts();
		findMaxCycleSize();
	}
	
	/**
	 * Determines the count for each group of components. The count for a
	 * particular component is calculated as follows:<br>
	 * 	1. If the VFI and VFO for two successive components are the same,
	 * then they may belong to the same cyclic group.
	 * <br>
	 * For an element i:<br>
	 * 	2. if VFI_i = 1 or VFO_i = 1 or VFI_i != VFI_(i-1) or VFO_i != VFO_(i-1),
	 * then the countM_i should be set to 1.
	 * <br>
	 * 	3. if VFI_i > 1 and VFO_i > 1 and VFI_i = VFI_(i-1) and VFO_i = VFO_(i-1),
	 * then the countM_i should be set to m_(i-1) + 1.
	 */
	private void findCounts()
	{
		logger.info("Calculating counts for cyclic components.");
		if(components == null)
		{
			logger.info("Error: Components array is not properly initialized yet.");
		}
		else if(components.length == 0)
		{
			logger.info("Error: Components array is empty.");
		}
		else
		{
			int currentCount = 1;
			components[0].setCount(currentCount);
			for(int i = 1; i < components.length; i++)
			{
				if(components[i].getVFI() == components[i-1].getVFI()
						&& components[i].getVFO() == components[i-1].getVFO()
						&& components[i].getVFO() > 1
						&& components[i].getVFI() > 1)
				{
					currentCount++;
				}
				else
				{
					currentCount = 1;
				}
				components[i].setCount(currentCount);
			}
			logger.info("Finished calculating counts.");
		}
	}
	
	/**
	 * Finds the maximum cyclic group sizes for each component, and
	 * records the total maximum cyclic group size (also known as the Core
	 * size) in the coreSize field of this object.<br>
	 * <br>
	 * The maximum cyclic group sizes are found by going through all of the
	 * components until one is found with countM = 1. At this point, the
	 * method looks at the previous slice of components and updates all of
	 * their maxCycleSize fields.<br>
	 * <br>
	 * The proper value to update them with is found by finding the minimum
	 * between the VFI, VFO, and countM of the last component in that slice
	 * (therefore, the component with the accumulation of all of the countMs).
	 * <br><br>
	 * During this process, the when a cyclic group size is found that as
	 * greater than the current coreSize, the coreSize is updated with the
	 * greater value.
	 */
	private void findMaxCycleSize()
	{
		logger.info("Updating maximum cycle sizes.");
		int start = 0;
		int newCount = 0;
		for(int i = 1; i < components.length+1; i++)
		{
			if(i == components.length || components[i].getCount() == 1)
			{
				CycleComponent temp = components[i-1];
				newCount = min(temp.getCount(), temp.getVFI(), temp.getVFO());
				if(newCount < 1)
				{
					newCount = 1;
				}
				if(newCount > coreSize)
				{
					coreSize = newCount;
				}
				for(int j = start; j < i; j++)
				{
					components[j].setMaxCycleSize(newCount);
				}
				start = i;
			}
		}
		logger.info("Finished maximum cycle sizes.");
		logger.info("Largest cycle (Core) size: " + coreSize);
	}
	
	/**
	 * A quick method to find the minimum for finding the maximum
	 * cyclic group size. All numbers are assumed to actually be
	 * integers, even though they are declared doubles, based on the
	 * fact that the VFI and VFO must be whole numbers.
	 * @param a  the count
	 * @param b  the VFI
	 * @param c  the VFO
	 * @return  the minimum of the parameters
	 */
	private int min(int a, double b, double c)
	{
		int temp1;
		if(a < b)
		{
			temp1 = a;
		}
		else
		{
			temp1 = (int) b;
		}
		if(temp1 < c)
		{
			return temp1;
		}
		else
		{
			return (int) c;
		}
	}
	

	/**
	 * Prints each of the components' VFI, VFO, and countM. Used for debugging.
	 */
	private void printCounts()
	{
		for(int i = 0; i < components.length; i++)
		{
			if(components[i].getCount() > 1)
			{
				System.out.println("VFI: " + components[i].getVFI()
								+ "\tVFO: " + components[i].getVFO()
								+ "\tCount: " + components[i].getCount());
			}
		}
	}
	
	/**
	 * Create a DSM from an XML file created with jDSM (as with the DSMToFromXML method
	 * in this class) and assign it to this DSMData object.
	 * 
	 * Apparently doesn't work. ???
	 * 
	 * @param dsmFilePath  path to the DSM XML file
	 * @returns  the DSM
	 */
	public DesignStructureMatrix<Dependency> getDSMFromXML(String dsmFilePath)
	{
		try
		{
			DesignStructureMatrix<Dependency> d = new DependencyDSM();
			d.loadFromXml(new FileInputStream(dsmFilePath));
			
			this.nrFiles = d.getMap().length;
			logger.info("DSM contains " + nrFiles + " files.");
			return d;
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
}
