package us.fl.state.fwc.abem.optimize;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.TreeMap;

import javolution.util.FastTable;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math.stat.regression.SimpleRegression;
import org.jgap.Chromosome;
import org.jgap.Configuration;
import org.jgap.FitnessFunction;
import org.jgap.Gene;
import org.jgap.Genotype;
import org.jgap.IChromosome;
import org.jgap.InvalidConfigurationException;
import org.jgap.Population;
import org.jgap.impl.DefaultConfiguration;
import org.jgap.impl.DoubleGene;
import org.jgap.impl.IntegerGene;

import us.fl.state.fwc.abem.Scheduler;
import us.fl.state.fwc.abem.environ.impl.TemperatureSpline;
import us.fl.state.fwc.abem.params.impl.SeatroutParams;
import us.fl.state.fwc.util.DataTable;
import us.fl.state.fwc.util.TextFileIO;
import us.fl.state.fwc.util.TimeUtils;
import cern.jet.random.Empirical;
import cern.jet.random.EmpiricalWalker;
import cern.jet.random.Normal;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister64;

public class SSTBuilderSizeVarianceTester {

	static private EmpiricalWalker ewSizeSelect;



	private  int loopCounter = 1; 
	long seed = System.currentTimeMillis(); 
	public MersenneTwister64 m = new MersenneTwister64((int) seed); 
	public Uniform uniform= new Uniform(m); 
	public Normal normal= new Normal(0,1,m); 

	DataTable suesData = new DataTable("data/SueMEPSWeightLength", true, "\t");
	double outlierCutoff = 30000;

	double LM = 0;
	int numFish = 5000;

	SimpleRegression regress; 

	double[] sizeFreqs; 
	int[] sizeClasses;


	public static void main(String[] args) {
		SSTBuilderSizeVarianceTester test = new SSTBuilderSizeVarianceTester();
		test.step();
		//test.evaluate();
	}




	public void step() {


		TreeMap<Integer, Integer> sizeFreqTemp = new TreeMap<Integer, Integer>();

		//Initialize the model
		//Need to calculate the LM of the actual data
		double[] logTL = ArrayUtils.toPrimitive((Double[]) suesData.getColumn("log(TL)") );
		double[] logSW = ArrayUtils.toPrimitive((Double[]) suesData.getColumn("log(SW)") );
		double[] SW = ArrayUtils.toPrimitive((Double[]) suesData.getColumn("SomaticWt") );
		double[] TL = ArrayUtils.toPrimitive((Double[]) suesData.getColumn("TL") );

		int n = logTL.length;

		regress = new SimpleRegression();

		for (int i=0; i<n;i++){
			regress.addData(logTL[i], logSW[i]);

			//add to size frequency probabilities by keeping track of total number per cm size class
			//get size class in cm
			int TLTemp = (int) Math.round(Math.pow(10,logTL[i])/10);
			Integer counter = sizeFreqTemp.get(TLTemp);
			if (counter == null) counter = 0;
			counter++;
			sizeFreqTemp.put(TLTemp, counter);

		}

		double sum = 0;
		//compute size freq size class
		for (Integer cmSizeClass: sizeFreqTemp.keySet()){
			sum += sizeFreqTemp.get(cmSizeClass);
		}

		sizeFreqs = new double[sizeFreqTemp.size()];
		sizeClasses = new int[sizeFreqTemp.size()];

		int counter = 0;
		for (Integer cmSizeClass: sizeFreqTemp.keySet()){
			double freq = ((double) sizeFreqTemp.get(cmSizeClass))/sum;
			sizeFreqs[counter] = freq; 
			sizeClasses[counter] = cmSizeClass;
			counter++;
		}

		double intcept = regress.getIntercept();
		double slope = regress.getSlope();

		System.out.println("intcept: " + intcept);
		System.out.println("slope : " + slope);

		
		double[] sqResids = new double[n];
		double mse = regress.getMeanSquareError();
		
		System.out.println("mse: " + mse);
		System.out.println("sum squares: " + regress.getSumSquaredErrors());
		System.out.println("total sum squares: " + regress.getTotalSumSquares());
		System.out.println("x sum squares: " + regress.getXSumSquares());
		System.out.println("regress sum squares: " + regress.getRegressionSumSquares());
		System.out.println("n: " + n);
		
		for (int i=0; i<n;i++){
			sqResids[i] = Math.pow(SW[i] - Math.pow(10, slope*logTL[i] + intcept), 2);
		}

		SimpleRegression residReg = new SimpleRegression();
		for (int i=0; i<n;i++){
			if (sqResids[i] < outlierCutoff) residReg.addData(TL[i], sqResids[i]);
		}

		LM = n*residReg.getRSquare();




	}






	/**Method for performing fitness evaluation.
	 */
	public void evaluate( ) {


		//double b = .0028052746860517726; 
		double b =0.01; //09880469613409772; 

		ewSizeSelect = 
				new EmpiricalWalker(sizeFreqs, Empirical.NO_INTERPOLATION, m);

		double intcept = regress.getIntercept();
		double slope = regress.getSlope();


		double[] fishLengths = new double[numFish];
		double[] fishWeights = new double[numFish];

		TextFileIO out = new TextFileIO("dataTest/fishLW.txt");
		//PrintWriter out = file.getWriter();

		//Assign fish weights/lengths
		for (int i=0; i<numFish; i++){
			double TL = (10.0*sizeClasses[ewSizeSelect.nextInt()]);

			double logTL = Math.log10(TL);

			double avgSW = Math.pow(10, slope*logTL + intcept);

			//calculate stDev based on b
			double stDev = Math.exp(TL*b); 
			double realSW = normal.nextDouble(avgSW, stDev);
			while (realSW < 0) {
				realSW = normal.nextDouble(avgSW, stDev);
			}

			fishLengths[i] = TL;
			fishWeights[i] = realSW;

			out.println(TL + "\t" + realSW);
		}



		//now, using estimated data, compute LW as per the real data and compare
		int n = numFish;

		SimpleRegression regress = new SimpleRegression();

		for (int i=0; i<n;i++){
			regress.addData(Math.log10(fishLengths[i]), Math.log10(fishWeights[i]));
		}

		//get new intercept and slope
		intcept = regress.getIntercept();
		slope = regress.getSlope();
		double[] sqResids = new double[n];

		System.out.println("slope: " + slope + "\tintercept: " +  intcept);
		
		for (int i=0; i<n;i++){
			sqResids[i] = Math.pow(fishWeights[i] - Math.pow(10, slope*Math.log10(fishLengths[i]) + intcept), 2);
		}

		SimpleRegression residReg = new SimpleRegression();
		for (int i=0; i<n;i++){
			if (sqResids[i] < outlierCutoff) residReg.addData(fishLengths[i], sqResids[i]);
		}

		double newIntcept = residReg.getIntercept();
		double newSlope = residReg.getSlope();
		double newLM = n*residReg.getRSquare();

		if (!new Double(newLM).isNaN()) { 

			double fitness = 10000 - (Math.abs(LM-newLM));

			System.out.println("fitness: " + fitness); 
		}

		out.close();

	} // end evaluate()







}
