package us.fl.state.fwc.abem.test;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javolution.util.FastMap;
import javolution.util.FastTable;
import us.fl.state.fwc.util.TimeUtils;
import us.fl.state.fwc.util.charts.XYPlot;
import cern.jet.random.Beta;
import cern.jet.random.Normal;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;

public class BimodalProbTester {

	// will hold a collection of individuals at each coordinate
	// make this a larger coordinate (say 10x10m) which could represent a neighborhood -- each agent can then search 
	// the hashmap around it's area to get surrounding individuals to interact with.  Would need to set the minimal size
	// to the mimimal perception range of all agents in the model, then could do multiples of cooridates for individuals with
	// large perceptions -- e.g. a shark could search a percieve a range of 10x10 coordinates (e.g., 100x100m), while a 
	// pinfish school may percieve a range of 1x1 coordiantes (10x10m)
	// NEED a single map which holds all individuals which will interact

	private  final int seed = (int) System.currentTimeMillis();
	private  MersenneTwister m= new MersenneTwister(seed); 




	static private Calendar startDate  = new GregorianCalendar(TimeZone.getTimeZone("EST")) ; 
	static private Calendar currentDate ; 



	//	double lunarSpecificity = 10; 

	public static void main(String[] args) {

		BimodalProbTester mt = new BimodalProbTester();
		//mt.bimodalLunarGood(); 
		mt.bimodalSeasonGood(); 
	}









	public void bimodalSeasonGood(){

		startDate.set(2006, 0, 1, 0, 0, 0);  
		currentDate = (GregorianCalendar) startDate.clone(); 
 

		int numStages = 8; 
		int peaks[] = {129, 221};
		double peaksSD[] = {10, 10}; 
		double peakSize[] = {.8, 1}; // relative 

		
		FastMap<Integer, FastTable<Normal>> seasonNormals = new FastMap<Integer, FastTable<Normal>> (); 
		for (int i=0; i<numStages; i++) {
			FastTable<Normal> seasonNormal = new FastTable<Normal>();  
			for (int j=0; j<peaks.length; j++){
				seasonNormal.add(new Normal(peaks[j], peaksSD[j], m));
			}
			seasonNormals.put(i, seasonNormal); 
		}


		double[][] data = new double[2][366]; 

		/*Multi-peak bimodal approach*/
		for (int day = 0; day<366; day++){
			double prob = 0; 

			/*			for (int i = 0; i<peaks.length; i++){
				seasonProbs[i].setState(peaks[i], peaksSD[i]	);
				double scaleFactor = 1/seasonProbs[i].pdf(peaks[i]); 
				prob += seasonProbs[i].pdf(day)*scaleFactor*peakSize[i];
			}
			 */			
			//FastTable<Double> seasonProbs = FastTable.newInstance();

			for (int i=0; i<peaks.length; i++) {
				double probtmp =((seasonNormals.get(0).get(i).pdf(day)*peakSize[i])/seasonNormals.get(0).get(i).pdf(peaks[i]));  
				if (probtmp>1) probtmp=1; 
				if (i == 0) prob = probtmp;
				else if ( probtmp > prob) prob = probtmp; 
			}


			data[0][day] = day;
			data[1][day] = prob; 

			//System.out.println("day: " + day + "\tdate: " + currentDate.getTime() + "\tpRob: " + prob); 
			System.out.println(day + "\t" + currentDate.getTime()); 
			currentDate.add(Calendar.DAY_OF_YEAR, 1); 
		}


		XYPlot plot1 = new XYPlot("Season Prob");
		plot1.makeLinePlot("bimodal", "day", "prob", data, false, false); 
	}

















	public void bimodalLunarGood(){

		Uniform uniform = new Uniform(m); 

		int lunarPeaks[] = {0, 14};
		double lunarPeaksSD[] = {5.07, 5.07}; 
		double lunarPeakSize[] = {1, 1}; // relative 
		int peakShift = 2; // this is shift of peak off of new/full peaks; i.e., if was 


		FastTable<Normal> lunarNormals = new FastTable<Normal>();  
		for (int i=0; i<lunarPeaks.length; i++){
			lunarNormals.add(new Normal(lunarPeaks[i], lunarPeaksSD[i], m));
		}

		int days = 120; 
		double[][] data = new double[2][days]; 
		double[][][] dataPeaks = new double[3][2][days]; 


		startDate.set(2006, 0, 1, 0, 0, 0);  
		currentDate = (GregorianCalendar) startDate.clone(); 
		currentDate.add(Calendar.DAY_OF_YEAR, 29); 

		//lunar tester bimodal approach
		for (int day = 0; day<days; day++){
			int lunarPhase = TimeUtils.getMoonPhase(currentDate);
			int lunarAdjust = lunarPhase - peakShift;
			if (lunarAdjust < 0) lunarAdjust = 28+lunarAdjust; 
			if (lunarAdjust > 14) lunarAdjust =28-lunarAdjust; 
			
			double lunarProb=0; 

			FastTable<Double> lunarProbs = FastTable.newInstance();

			for (int i=0; i<lunarPeaks.length; i++) {
				lunarProbs.add((lunarNormals.get(i).pdf(lunarAdjust)/lunarNormals.get(i).pdf(lunarPeaks[i]))*lunarPeakSize[i] );
				if (i == 0) lunarProb = lunarProbs.get(i);
				else if ( lunarProbs.get(i) > lunarProb) lunarProb = lunarProbs.get(i); 
			}

			data[0][day] = day;
			data[1][day] = lunarProb; 

			//System.out.println(day + "\t" + lunarPhase ); 

			System.out.println("day: " + day + "\tdate: " + currentDate.getTime() + "\t\tlunarPhase (simple calc): " + lunarPhase + "\tlunarPRob: " + lunarProb ); 

			//	System.out.println(day + "\t" + lunarPhase + "\t" + lunarProb); 
			if ( uniform.nextDoubleFromTo(0, 1)  < lunarProb  ) {
				// **SPAWN**
			}

			currentDate.add(Calendar.DAY_OF_YEAR, 1); 
			FastTable.recycle(lunarProbs); 
		}


		XYPlot plot = new XYPlot("Lunar Prob");
		plot.makeLinePlot("combined", "day", "prob", data, false, false);

	}


















	public void bimodalLunar(){

		Uniform uniform = new Uniform(m); 

		int lunarPeaks[] = {0, 14}; 
		double lunarPeaksSD[] = {2, 2}; //  should be 

		//		double lunarPeakSize[] = {.8, 1}; // if two equal peaks, set both to 1 and stdev <5; if offset peaks, scale first one (smaller one, always) relative to second with stdev<5, and leave second=1; if only 1 peak, set first equal to 0 and second equal to 1; if no peak, set both equal to 1 with stdev for both >10 
		double lunarPeakSize[] = {1, 1}; 
		Normal normalNew = new Normal(lunarPeaks[0], lunarPeaksSD[0], m); 
		Normal normalFull = new Normal(lunarPeaks[1], lunarPeaksSD[1], m);

		int days = 365; 
		double[][] data = new double[2][days]; 
		double[][][] dataPeaks = new double[3][2][days]; 

		startDate.set(2006, 0, 1, 0, 0, 0);  
		currentDate = (GregorianCalendar) startDate.clone(); 
		currentDate.add(Calendar.DAY_OF_YEAR, 24); 

		//lunar tester bimodal approach
		for (int day = 0; day<days; day++){
			int lunarPhase = TimeUtils.getMoonPhase(currentDate);
			double lunarProb1=0, lunarProb2 =0;  


			if (lunarPhase > 14) lunarPhase= 28-lunarPhase; 


			lunarProb1 = (normalNew.pdf(lunarPhase)/normalNew.pdf(lunarPeaks[0]))*lunarPeakSize[0];
			lunarProb2 = (normalFull.pdf(lunarPhase)/normalFull.pdf(lunarPeaks[1]))*lunarPeakSize[1];

			if ( lunarProb1 < lunarProb2 ) lunarProb1 = lunarProb2;

			data[0][day] = day;
			data[1][day] = lunarProb1; 

			System.out.println("day: " + day + "\tdate: " + currentDate.getTime() + "\t\tlunarPhase (simple calc): " + lunarPhase + "\tlunarPRob: " + lunarProb1 ); 

			//	System.out.println(day + "\t" + lunarPhase + "\t" + lunarProb); 
			if ( uniform.nextDoubleFromTo(0, 1)  < lunarProb1  ) {
				// **SPAWN**
			}

			currentDate.add(Calendar.DAY_OF_YEAR, 1); 

		}


		XYPlot plot = new XYPlot("Lunar Prob");
		plot.makeLinePlot("combined", "day", "prob", data, false, false);
		//		String[] titles = {"smallPeak", "bigPeak", "combined"}; 
		//		plot.makeLinePlot(titles, "day", "prob", dataPeaks, false); 
	}



	// this is smoother approach, but can't do different Standard deviations
	public void bimodalLunarOld(){

		Uniform uniform = new Uniform(m); 

		int lunarPeaks[] = {0, 14}; 
		double lunarPeaksSD[] = {5, 5}; //  should be 

		//		double lunarPeakSize[] = {.8, 1}; // if two equal peaks, set both to 1 and stdev <5; if offset peaks, scale first one (smaller one, always) relative to second with stdev<5, and leave second=1; if only 1 peak, set first equal to 0 and second equal to 1; if no peak, set both equal to 1 with stdev for both >10 
		double lunarPeakSize[] = {.2, 0}; 
		Normal normalNew = new Normal(lunarPeaks[0], lunarPeaksSD[0], m); 
		Normal normalFull = new Normal(lunarPeaks[1], lunarPeaksSD[1], m);

		int days = 31; 
		double[][] data = new double[2][days]; 
		double[][][] dataPeaks = new double[3][2][days]; 

		startDate.set(2001, 0, 1, 0, 0, 0);  
		currentDate = (GregorianCalendar) startDate.clone(); 
		currentDate.add(Calendar.DAY_OF_YEAR, 24); 

		//lunar tester bimodal approach
		for (int day = 0; day<days; day++){
			int lunarPhase = TimeUtils.getMoonPhase(currentDate);
			double lunarProb=0; 

			double lunarPeak1=0;
			double lunarPeak2=0; 

			if (lunarPhase > 14) lunarPhase= 28-lunarPhase; 

			if (lunarPeaksSD[0] == lunarPeaksSD[1]){
				double scaleFactor = 1/ ( normalNew.pdf(lunarPeaks[1]) + normalFull.pdf(lunarPeaks[1]));
				lunarProb = ( normalNew.pdf(lunarPhase)*lunarPeakSize[0] + normalFull.pdf(lunarPhase)*lunarPeakSize[1] ) / ( normalNew.pdf(lunarPeaks[1])*lunarPeakSize[0] + normalFull.pdf(lunarPeaks[1])*lunarPeakSize[1] );

				if (lunarPhase == lunarPeaks[0]){ // do this so is slightly smoother transition at new moon peak; note: this is only a problem when stdev is midvalues (~6-20) for both peaks, and there is a difference in peak heights
					int lunarPhasePlus1 = lunarPhase+1; 
					if (lunarPhasePlus1 > 14) lunarPhasePlus1 = 28-lunarPhasePlus1; 
					double lunarProbPlus1Day = ( normalNew.pdf(lunarPhasePlus1)*lunarPeakSize[0] + normalFull.pdf(lunarPhasePlus1)*lunarPeakSize[1] ) / ( normalNew.pdf(lunarPeaks[1])*lunarPeakSize[0] + normalFull.pdf(lunarPeaks[1])*lunarPeakSize[1] );

					if (lunarProb<lunarProbPlus1Day) lunarProb=lunarProbPlus1Day; 
				}
			}


			if (lunarProb>1) lunarProb=1; 

			data[0][day] = day;
			data[1][day] = lunarProb; 

			System.out.println("\tdate: " + currentDate.getTime() + "\t\tlunarPhase (simple calc): " + lunarPhase + "\tlunarPRob: " + lunarProb ); 

			//	System.out.println(day + "\t" + lunarPhase + "\t" + lunarProb); 
			if ( uniform.nextDoubleFromTo(0, 1)  < lunarProb  ) {
				// **SPAWN**
			}

			currentDate.add(Calendar.DAY_OF_YEAR, 1); 

		}

		/* Older code:
		 * 			for (int j=0; j<lunarPeaks.length; j++) {
				normal.setState(14, lunarPeaksSD[j]); 
				lunarPhase = TimeUtils.getMoonPhase(currentDate);
				//lunarPhaseComplex = TimeUtils.getMoonPhase_Complex(currentDate); 

				int adjustedPhase = lunarPhase; 
				w = adjustedPhase ; 

				// if peak day isn't equal to 15, then need to scale the peak day to equal 15
				if (lunarPeaks[j] != 14) {
					int dayDiff = 14 - lunarPeaks[j]; 
					adjustedPhase = lunarPhase+dayDiff; 
					if (adjustedPhase < 0) adjustedPhase = 28+adjustedPhase; 
					if (adjustedPhase> 14) w = (adjustedPhase-(adjustedPhase-14)*2);
					else w = adjustedPhase;  
					if (w < 0) w=-w; 
				}
				double scaleFactor = 1/(normal.pdf(14) + normal.pdf(0)); 
				//System.out.println("normal.pdf(w): " + normal.pdf(15) ); 
				//System.out.println("scalefactor: " + scaleFactor); 
				lunarProb += normal.pdf(w)*scaleFactor*lunarPeakSize[j]; 

				if (j==0) lunarPeak1 = normal.pdf(w)*scaleFactor*lunarPeakSize[j];
				if (j==1) lunarPeak2 = normal.pdf(w)*scaleFactor*lunarPeakSize[j];

			}


			if (lunarProb>1) lunarProb=1;
			data[0][day] = day;
			data[1][day] = lunarProb; 
			dataPeaks[0][0][day] = day;
			dataPeaks[0][1][day] = lunarPeak1; 
			dataPeaks[1][0][day] = day;
			dataPeaks[1][1][day] = lunarPeak2; 
			dataPeaks[2][0][day] = day;
			dataPeaks[2][1][day] = lunarProb; 

			System.out.println("\tdate: " + currentDate.getTime() + "\t\tlunarPhase (simple calc): " + lunarPhase + "\tlunarPRob: " + lunarProb ); 

		 */

		XYPlot plot = new XYPlot("Lunar Prob");
		plot.makeLinePlot("combined", "day", "prob", data, false, false);
		//		String[] titles = {"smallPeak", "bigPeak", "combined"}; 
		//		plot.makeLinePlot(titles, "day", "prob", dataPeaks, false); 
	}






	public void betaFlattenedPeak(){
		double a, b; 
		Beta beta = new Beta(2, 8, m); 

		double startOfBreeding = 59; 
		double endOfBreeding = 272; 
		int peakSpawn = (int) ((endOfBreeding+startOfBreeding)/2); 

		// Beta distribution approach: flattened peak with tails
		double x = 0, y=0, xtemp=0; 
		for (int day = 0; day<360; day++){
			if ( (day<=startOfBreeding) || day>=endOfBreeding ) xtemp=0; 
			else {
				x=(day-startOfBreeding)/(peakSpawn-startOfBreeding);
				if (x>=1) xtemp = 1-(x-1);
				else xtemp = x; 
			}
			y=beta.cdf(xtemp); 
			System.out.println( day + "\t" +  y);
		}
	}




	public void bimodalSeasonOld(){

		int peaks[] = {129, 221};
		double peaksSD[] = {40, 20}; 
		double peakSize[] = {0.8, 1}; // relative 

		Normal normal = new Normal(0,1,m); 
		double[][] data = new double[2][365]; 

		/*Multi-peak bimodal approach*/
		for (int day = 0; day<360; day++){
			double prob = 0; 

			for (int i = 0; i<peaks.length; i++){
				normal.setState(peaks[i], peaksSD[i]	);
				double scaleFactor = 1/normal.pdf(peaks[i]); 
				prob += normal.pdf(day)*scaleFactor*peakSize[i];
			}

			data[0][day] = day;
			data[1][day] = prob; 

			System.out.println( day + "\t" + prob);
		}

		XYPlot plot1 = new XYPlot("Season Prob");
		plot1.makeLinePlot("bimodal", "day", "prob", data, false, false); 
	}







	public void bimodalSeason(){

		int peaks[] = {129, 221};
		double peaksSD[] = {50, 20}; 
		double peakSize[] = {0.8, 1}; // relative 

		Normal normal1 = new Normal(peaks[0], peaksSD[0], m); 
		Normal normal2 = new Normal(peaks[1], peaksSD[1], m);


		double[][] data = new double[2][365]; 

		/*Multi-peak bimodal approach*/
		for (int day = 0; day<360; day++){
			double prob1 = 0, prob2=0; 

			prob1 = (normal1.pdf(day)/normal1.pdf(peaks[0]))*peakSize[0];
			prob2 = (normal2.pdf(day)/normal2.pdf(peaks[1]))*peakSize[1];

			if ( prob1 < prob2 ) prob1 = prob2;


			data[0][day] = day;
			data[1][day] = prob1; 

			System.out.println( day + "\t" + prob1);
		}

		XYPlot plot1 = new XYPlot("Season Prob");
		plot1.makeLinePlot("bimodal", "day", "prob", data, false, false); 
	}









}
