package us.fl.state.fwc.abem.test;

import cern.jet.random.Empirical;
import cern.jet.random.EmpiricalWalker;
import cern.jet.random.engine.MersenneTwister64;

public class GetSSTAvgAge {

	private double[] sizeFreqArray = {0.311878399f, 0.236195382f, 0.152864581f, 0.100675485f, 0.061927761f, 0.050682953f, 0.028968558f, 0.020507908f, 0.013788165f, 0.009270252f, 0.006232706f, 0.00419046f, 0.002817389f} ;  
	private double[] sizeFreqArrayTruncated = {0.5074f, 0.2286f, 0.1401f, 0.0658f, 0.0338f, 0.0147f, 0.0059f, 0.0037f,  0f, 0f, 0f, 0f, 0f} ;  
	private EmpiricalWalker ewSiteSelect ;
	MersenneTwister64 m= new MersenneTwister64(); ; 
	
	int num = 1000000; 
	double sumAges = 0;
	int totalCount = 0;
	
	public void run() {
		ewSiteSelect = new EmpiricalWalker(new double[] {1.0, 0.0},
				Empirical.NO_INTERPOLATION, m);

		//get 2004 distribution:
		ewSiteSelect.setState2(sizeFreqArray); 
		for (int i=0; i<num; i++) {
			int age = ewSiteSelect.nextInt();
			if (age != 0) totalCount++;
			sumAges += (double) age;
		}

		System.out.println("avg age 2004: " + (sumAges/(double) totalCount));
		
		//get 2004 distribution:
		sumAges = 0;
		totalCount = 0;
		ewSiteSelect.setState2(sizeFreqArrayTruncated); 
		for (int i=0; i<num; i++) {
			int age = ewSiteSelect.nextInt();
			if (age != 0) totalCount++;
			sumAges += (double) age;
		}

		System.out.println("avg age 1982: " + (sumAges/(double) totalCount));
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		GetSSTAvgAge a = new GetSSTAvgAge();
		a.run();
	}

}
