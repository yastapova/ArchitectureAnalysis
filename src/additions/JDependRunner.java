package additions;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.jblas.DoubleMatrix;

import jdepend.framework.JDepend;
import jdepend.framework.JavaPackage;

/**
 * Creates an instance of the JDepend class and, given a path to a folder,
 * performs its analysis on the files. It first looks for packages; then
 * calculates each metric for each package while recording all results; and
 * then prints out the min, max, and mean of each metric (except for the
 * number of cycles, which is just a count). Requires .class or .jar files.
 * 
 * @author YuliyaA
 *
 */
public class JDependRunner
{
	/** Logger to print messages to user. */
	private static Logger logger = Logger.getLogger(JDependRunner.class);
	/** Contains the names of each metric for the purposes of printing. */
	private static String[] headings = {"Afferent Coupling -- ",
										"Efferent Coupling -- ",
										"Abstractness -- ",
										"Instability -- ",
										"Distance -- "};
	/** Contains the metrics for each processed package. */
	private DoubleMatrix data;
	/** The number of packages that are detected. */
	private int size;
	/** The number of cycles that are detected. */
	private int cycleCount;
	
	/**
	 * Default blank constructor.
	 */
	public JDependRunner()
	{
		data = null;
		size = 0;
		cycleCount = 0;
	}
	/**
	 * Creates a JDependRunner object and calls <code>analyze</code>
	 * and then <code>printResults</code>.
	 * @param pathToClasses  the path to the folder that contains the
	 * classes to be analyzed
	 */
	public JDependRunner(String pathToClasses)
	{
		data = null;
		size = 0;
		cycleCount = 0;
		analyze(pathToClasses);
		printResults();
	}
	
	/**
	 * Main method to start running the analysis.
	 * @param args
	 */
	public static void main(String[] args)
	{
		String path = "C:/Users/YuliyaA/Documents/codefacts-compare/wings-portal-4.0-SNAPSHOT.war";
		
		JDependRunner test = new JDependRunner(path);
	}
	
	/**
	 * Finds all of the packages and analyzes them. Creates a DoubleMatrix of the
	 * results and saves it in the <code>data</code> instance variable. Also saves
	 * the number of packages and the number of cycles found in their respective
	 * instance variables.
	 * 
	 * @param pathToClasses  the path to the folder containing the files to be analyzed
	 */
	private void analyze(String pathToClasses)
	{
		JDepend jdepend = new JDepend();
		try
		{
			jdepend.addDirectory(pathToClasses);
		}
		catch (IOException e)
		{
			logger.info("That is not a valid directory.");
			e.printStackTrace();
		}
		Collection packages = jdepend.analyze();	//finds the packages
		size = packages.size();
		logger.info("Found " + size + " packages.");
		int count = 0;
		cycleCount = 0;
		data = DoubleMatrix.zeros(size, 5);		//initialize data matrix to 0
		JavaPackage jPackage;	//the package to be analyzed
		
		Iterator i = packages.iterator();
		while (i.hasNext())
		{
			jPackage = (JavaPackage) i.next();
			//String name = jPackage.getName();					//package name
			data.put(count, 0, jPackage.afferentCoupling());	//afferent coupling
			data.put(count, 1, jPackage.efferentCoupling());	//efferent coupling
			data.put(count, 2, jPackage.abstractness());		//abstractness
			data.put(count, 3, jPackage.instability());			//instability
			data.put(count, 4, jPackage.distance());			//distance
			if(jPackage.containsCycle())
			{
				cycleCount++;		//total cycles
			}
			count++;
			if(count % 50 == 0)
			{
				logger.info("Processed package " + count + " of " + size + ".");
			}
		}
	}
	
	/**
	 * Calculates the min, max, and mean of each metric and prints them.
	 */
	private void printResults()
	{
		DoubleMatrix means = data.columnMeans();
		DoubleMatrix maxes = data.columnMaxs();
		DoubleMatrix mins = data.columnMins();
		
		logger.info("Metrics calculated.");
		for(int j = 0; j < headings.length; j++)
		{
			logger.info(headings[j] + "Min: " + mins.get(j)
						+ ", Max: " + maxes.get(j)
						+ ", Mean: " + means.get(j));
		}
		logger.info("Total cycles counted -- " + cycleCount);
		
		//theoretically, without self-dependencies
		DoubleMatrix propagation = data.getColumn(0);
		double propCost = propagation.columnMeans().sum()/size;
		logger.info("Propagation Cost -- " + propCost);
	}
}
