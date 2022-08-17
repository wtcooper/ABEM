package us.fl.state.fwc.abem.test;

import java.util.Calendar;
import java.util.GregorianCalendar;

import javolution.util.FastTable;

import org.jgap.InvalidConfigurationException;

import us.fl.state.fwc.abem.Scheduler;
import us.fl.state.fwc.abem.params.impl.SeatroutParams;
import cern.jet.random.Normal;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister64;

public class SpawnModelNonSpatial {

	public boolean haveVariableSeasonality = true;
	private static int loopCounter = 1; 
	static long seed = System.currentTimeMillis(); 
	static MersenneTwister64 m; // = new MersenneTwister64((int) seed); 
	static Uniform uniform; // = new Uniform(m); 
	static Normal normal; // = new Normal(0,1,m); 

	static Scheduler sched = new Scheduler(); 

	static  int numFish = 100000; 
	static  FastTable<SpawnOptiTrout> fishList = new FastTable<SpawnOptiTrout>(); 

	static  double[][] numFishPerAge = new double[9][365]; 

	// size frequency for ages 0,1,2,3,4,5,6,7,8
	public double[][] sizeFreq = { {0.02967033,	0.226373626,	0.323076923,	0.22967033,	0.110989011,	0.064835165,	0.00989011,	0.003296703,	0.002197802},
			{0.038565022,	0.234080717,	0.277130045,	0.219730942,	0.137219731,	0.069955157,	0.015246637,	0.007174888,	0.000896861} };

	private double[] sizeFreqArray = {0.311878399f, 0.236195382f, 0.152864581f, 0.100675485f, 
			0.061927761f, 0.050682953f, 0.028968558f, 0.020507908f, 0.013788165f, 0.009270252f, 
			0.006232706f, 0.00419046f, 0.002817389f} ;  

	
	public double[][] avgSizeAtAge = {	{24.52222222,	28.19757282,	31.18843537,	34.0215311,	36.36534653,	40.07627119,	44.5,	50.56666667,	48.75},
			{24.66046512,	30.31609195,	35.68640777,	40.76530612,	45.69542484,	50.16923077,	51.22941176,	54.675,	55.8}};

	static public double[][]stdSizeAtAge = 
	{	{2.404536311, 3.107417394,	2.465904482,	2.586608551,	3.772411315,	
		4.012495095,	5.281571736,	2.122105872,	3.889087297},
			{2.383089185,	4.031115714,	3.327591712,	4.08389234,	4.571555935,	
			6.782891153,	4.944790782,	7.201140783,	0.}};



	Calendar startDate  = new GregorianCalendar(2010, 0, 1) ; 
	Calendar currentDate = (Calendar) startDate.clone(); 

	public static void main(String[] args) throws InvalidConfigurationException {
		SpawnModelNonSpatial st = new SpawnModelNonSpatial();
		st.step(); 
		
	}


	public void step() {

		SeatroutParams params = new SeatroutParams(); 

		m = sched.getM(); 
		uniform = sched.getUniform();
		normal = sched.getNormal(); 
		
		int numFemales = numFish; //(int) (numFish*propFemales);

		double[][] freq = new double[2][9]; 
		freq[1][0] = sizeFreqArray[0]; 
		freq[1][1] = freq[1][0] + sizeFreqArray[1]; 
		freq[1][2] = freq[1][1] + sizeFreqArray[2]; 
		freq[1][3] = freq[1][2] + sizeFreqArray[3]; 
		freq[1][4] = freq[1][3] + sizeFreqArray[4]; 
		freq[1][5] = freq[1][4] + sizeFreqArray[5]; 
		freq[1][6] = freq[1][5] + sizeFreqArray[6]; 
		freq[1][7] = freq[1][6] + sizeFreqArray[7]; 
		freq[1][8] = freq[1][7] + sizeFreqArray[8]; 
		
		
		for (int i =0; i<numFemales; i++){
			double prob = uniform.nextDoubleFromTo(0, 1); 
			int age=0; 
			if (prob < freq[1][0]) age=0;
			else if ( (prob >= freq[1][0] ) && (prob < freq[1][1])) age =1; 
			else if ( (prob >= freq[1][1] ) && (prob < freq[1][2])) age =2; 
			else if ( (prob >= freq[1][2] ) && (prob < freq[1][3])) age =3; 
			else if ( (prob >= freq[1][3] ) && (prob < freq[1][4])) age =4; 
			else if ( (prob >= freq[1][4] ) && (prob < freq[1][5])) age =5; 
			else if ( (prob >= freq[1][5] ) && (prob < freq[1][6])) age =6; 
			else if ( (prob >= freq[1][6] ) && (prob < freq[1][7])) age =7; 
			else  if (prob >=freq[1][7]) age = 8; 

			SpawnOptiTrout trout = new SpawnOptiTrout(sched, params); 

			//##########################################################
			//set birthday, based on calendar spawning peaks, so can get accurate estimate
			//of their size on Jan 1st
			//loop through seasonal peak sizes
			double sum = 0;
			int peak = 0;
			for (int j=0; j< trout.params.getSeasonPeakSizes(age).size(); j++){
				sum += trout.params.getSeasonPeakSizes(age).get(j);
			}
			prob = uniform.nextDoubleFromTo(0, 1); 
			double probSum = 0; 
			for (int j=0; j< trout.params.getSeasonPeakSizes(age).size(); j++){
				probSum += trout.params.getSeasonPeakSizes(age).get(j)/sum; 
				if (prob < probSum) {
					peak = j; 
					continue; 
				}
			}

			int dayOfBirth = 
				(int) Math.round(trout.params.getSeasonNormals(age).get(peak).nextDouble()); 
			int yearOfBirth = 
				currentDate.get(Calendar.YEAR) - (age+1); 
			Calendar tempDate = new GregorianCalendar(yearOfBirth, 0, 1);
			tempDate.add(Calendar.DAY_OF_YEAR, dayOfBirth); 
			double ageInDays = 
				(currentDate.getTimeInMillis()-tempDate.getTimeInMillis())/(1000*60*60*24);

			
			double size = normal.nextDouble(trout.params.getLengthAtAge(ageInDays/365.25, 0), 2);
			if (size < 0) size = 1; 
			double sizeMaturity = normal.nextDouble(trout.params.getSizeAtMaturityAvg(), trout.params.getSizeAtMaturitySD());
			trout.setAge(ageInDays/365.25);
			trout.setSizeMaturity(sizeMaturity);
			trout.setSex(true); 
			trout.setSize(size);
			trout.setBiomass(trout.params.getMassAtLength(size, 0)); 
			trout.setNominalBiomass(trout.params.getMassAtLength(size, 0)); 
			fishList.add(trout); 
			//numFishPerAge[age]++; 
		}


		int  peaks[] = new int[2]; 
		double peaksSD[] = new double[2]; 
		double peakSize[] = new double[2]; // relative 

		double[][] numSpawners = new double[9][365];  //% spawning per day of year out of total females

		peaks[0] = 138;
		peaks[1] = 227; 
		peaksSD[0] = 18; //34.5; 
		peaksSD[1] = 18; //32.12; 
		peakSize[0] = 0.8;
		peakSize[1] = 1;
		
		int peak1Offset = 2; 
		int peak2Offset = 1; 
		
		
		//#########################################################
		//set parameters for each fish
		for(int j = 0; j<fishList.size(); j++){ 
			SpawnOptiTrout trout = fishList.get(j); 

			int[] newPeaks = new int[2];
			double[] newPeaksSD = new double[2];
			
			if (haveVariableSeasonality) {
			if ((int) trout.getAge() == 0) {
				peaksSD[0] = 11; 
				peaksSD[1] = 11;
				peaks[0] = 138+(12-(int) trout.getAge())*(peak1Offset+2);
				peaks[1] = 227-(12-(int) trout.getAge())*peak2Offset;

				newPeaks[0] = peaks[0];
				newPeaks[1] = peaks[1];
				newPeaksSD[0] = peaksSD[0];
				newPeaksSD[1] = peaksSD[1];
			}
			if ((int) trout.getAge() == 1) {
				peaksSD[0] = 13; 
				peaksSD[1] = 13;
				peaks[0] = (int) (138+(12-(int) trout.getAge())*(peak1Offset+1.5) );
				peaks[1] = (int) (227-(12-(int) trout.getAge())*peak2Offset );

				newPeaks[0] = peaks[0];
				newPeaks[1] = peaks[1];
				newPeaksSD[0] = peaksSD[0];
				newPeaksSD[1] = peaksSD[1];
			}
			if ((int) trout.getAge() == 2) {
				peaksSD[0] = 15; 
				peaksSD[1] = 15;
				peaks[0] = 138+(12-(int) trout.getAge())*(peak1Offset+1);
				peaks[1] = 227-(12-(int) trout.getAge())*peak2Offset;

				newPeaks[0] = peaks[0];
				newPeaks[1] = peaks[1];
				newPeaksSD[0] = peaksSD[0];
				newPeaksSD[1] = peaksSD[1];
			}
			if ((int) trout.getAge() == 3) {
				peaksSD[0] = 16; 
				peaksSD[1] = 16;
				peaks[0] = 138+(12-(int) trout.getAge())*peak1Offset;
				peaks[1] = 227-(12-(int) trout.getAge())*peak2Offset;

				newPeaks[0] = peaks[0];
				newPeaks[1] = peaks[1];
				newPeaksSD[0] = peaksSD[0];
				newPeaksSD[1] = peaksSD[1];
			}
			if ((int) trout.getAge() == 4) {
				peaksSD[0] = 18; 
				peaksSD[1] = 18;
				peaks[0] = 138+(12-(int) trout.getAge())*peak1Offset;
				peaks[1] = 227-(12-(int) trout.getAge())*peak2Offset;

				newPeaks[0] = peaks[0];
				newPeaks[1] = peaks[1];
				newPeaksSD[0] = peaksSD[0];
				newPeaksSD[1] = peaksSD[1];
			}
			if ((int) trout.getAge() == 5) {
				peaksSD[0] = 20; 
				peaksSD[1] = 20;
				peaks[0] = 138+(12-(int) trout.getAge())*peak1Offset;
				peaks[1] = 227-(12-(int) trout.getAge())*peak2Offset;

				newPeaks[0] = peaks[0];
				newPeaks[1] = peaks[1];
				newPeaksSD[0] = peaksSD[0];
				newPeaksSD[1] = peaksSD[1];
			}
			if ((int) trout.getAge() == 6) {
				peaksSD[0] = 21; 
				peaksSD[0] = 21; 
				peaks[0] = 138+(12-(int) trout.getAge())*peak1Offset;
				peaks[1] = 227-(12-(int) trout.getAge())*peak2Offset;

				newPeaks[0] = peaks[0];
				newPeaks[1] = peaks[1];
				newPeaksSD[0] = peaksSD[0];
				newPeaksSD[1] = peaksSD[1];
			}
			if ((int) trout.getAge() == 7) {
				peaksSD[0] = 22; 
				peaksSD[1] = 22;
				peaks[0] = 138+(12-(int) trout.getAge())*peak1Offset;
				peaks[1] = 227-(12-(int) trout.getAge())*peak2Offset;

				newPeaks[0] = peaks[0];
				newPeaks[1] = peaks[1];
				newPeaksSD[0] = peaksSD[0];
				newPeaksSD[1] = peaksSD[1];
			}
			if ((int) trout.getAge() == 8) {
				peaksSD[0] = 23; 
				peaksSD[1] = 23;
				peaks[0] = 138+(12-(int) trout.getAge())*peak1Offset;
				peaks[1] = 227-(12-(int) trout.getAge())*peak2Offset;

				newPeaks[0] = peaks[0];
				newPeaks[1] = peaks[1];
				newPeaksSD[0] = peaksSD[0];
				newPeaksSD[1] = peaksSD[1];
			}
			}
			else {
				//set equal to age 3
				newPeaksSD[0] = 16; 
				newPeaksSD[1] = 16;
				newPeaks[0] = 138+(12-3)*peak1Offset;
				newPeaks[1] = 227-(12-3)*peak2Offset;
			}
			
			trout.setParameters(newPeaks, newPeaksSD, peakSize) ;
		}

		
		//step through each day
		for (int i = 0; i<365; i ++) {
			for(int j = 0; j<fishList.size(); j++){ 
				
				SpawnOptiTrout trout = fishList.get(j); 
				
				//increment here so that is accurate proportion belwo
				int ageTemp = (int) trout.getAge();
				if (ageTemp >= 9) ageTemp = 8; 
				numFishPerAge[ageTemp][i]++; 
				
				if (trout.timeToSpawn(currentDate)) {
					//System.out.println("trout"+j+"\t"+(currentDate.get(Calendar.MONTH)+1) + "\t" + currentDate.get(Calendar.DAY_OF_MONTH));
					if ( (int) trout.getAge() >= numSpawners.length) 
						numSpawners[numSpawners.length-1][i]++;  
					else 
						numSpawners[(int) trout.getAge()][i]++; 
				}
				//add a day to trouts age
				trout.setAge(trout.getAge() + 1.0/365.25);
				//grow trout
				double avgGrowthForAge = 
					trout.params.getLengthAtAge(trout.getAge(), 0)
				- trout.params.getLengthAtAge(trout.getAge() - 1.0/365.25, 0); 
				trout.setSize(trout.getSize() + avgGrowthForAge);
				
				
			}
			currentDate.add(Calendar.DAY_OF_YEAR, 1); 
		}

		//TODO -- use the numSpawners[ageClass][dayOfYear] to graph 4 plots of prop spawning per day
		double[][][] propSpawnPerDay = new double[9][2][365]; 

		// sum total number spawning per month for each age group 
		for (int i=0; i<365; i++){
			for (int j=0; j<9; j++){
				propSpawnPerDay[j][0][i] = i; //set this to be the "x" values for plotting, i.e., the day 
				if (numFishPerAge[j][i] == 0) propSpawnPerDay[j][1][i] = 0;  
				else propSpawnPerDay[j][1][i] = numSpawners[j][i]/numFishPerAge[j][i]; 
			}
		}

		for (int i=0; i<365; i++){
			System.out.println(i+"\t"+propSpawnPerDay[0][1][i]+"\t"+propSpawnPerDay[1][1][i]+"\t"+propSpawnPerDay[2][1][i]
	           +"\t"+propSpawnPerDay[3][1][i] +"\t"+propSpawnPerDay[4][1][i] +"\t"+propSpawnPerDay[5][1][i] +"\t"+propSpawnPerDay[6][1][i] 
               +"\t"+propSpawnPerDay[7][1][i] +"\t"+propSpawnPerDay[8][1][i] );
		}

		/*
		ArrayList<Integer> seriesToPlot = new ArrayList<Integer>(); 
		seriesToPlot.add(0);
		seriesToPlot.add(1); 
		seriesToPlot.add(2); 
		seriesToPlot.add(3); 
		String[] titles = {"age0", "age1", "age2", "age3"}; 
		XYPlot plot = new XYPlot("Spawn Timing"); 
		plot.setChartHeight(400);  
		plot.setChartWidth(1000);  
		plot.setBackgroundColor(Color.white); 
		plot.makeLinePlot(titles, seriesToPlot, "DayOfYear", "Proportion Spawning", propSpawnPerDay, true, false); 
		*/
	

	} // end evaluate()






}
