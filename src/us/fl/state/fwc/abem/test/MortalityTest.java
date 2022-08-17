package us.fl.state.fwc.abem.test;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import us.fl.state.fwc.abem.Scheduler;
import us.fl.state.fwc.abem.params.impl.SeatroutParams;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister64;

/**
 * See functions mortality() and mortality2() for differences in how mortality are represented.  For the model, mortality2() is used.  The difference b/w the two
 * is in how the instananeous rate is parsed out for a variable run time.  For instance, if population has an overall mortality rate (A) of 0.2, corrsponding to an annual instantaneous
 * rate (Z) of ~0.22, then how does this instantanous rate get sub-divided to represent the liklihood of moratlity in a short time step, say one day?  mortality() approaches
 * the problem by calculating Z for each subdivision as log(-1/(A/subdivisions - 1)), then parsing out the instantaneous rates of M and F (natural and fishing) as the proportion of
 * Z due to M and F.  However, this approach under-calculates overall mortality.  mortality2() solves the problem by simply dividing annual instantaneous rates M and F by 
 * the subdivisions, which gives the appropriate annual rate.  Note: Lowerre-Barbierri et al. IBM paper uses the same approach as mortality2(), where they calculate a montly
 * M rate by dividing annual rate by 12.  
 * 
 * @author Wade.Cooper
 *
 */
public class MortalityTest {

	long seed = System.currentTimeMillis(); 
	MersenneTwister64 m; // = new MersenneTwister64((int) seed); 
	Uniform uniform; // = new Uniform(m); 

	SeatroutParams params = new SeatroutParams();


	private  PrintWriter outFile = null; 


	public static void main(String[] args) {

		MortalityTest mt = new MortalityTest();
		mt.mortality2(); 
	}


	public void mortality(){
		Scheduler sched = new Scheduler();
		params.initialize(sched);
		m = sched.getM();
		uniform = sched.getUniform();
		
		double numYearIntervals =365; 

		int totalFish = 100000;
		int numDie = 0;
		int numDieNat = 0;
		int numDieComm = 0;
		int numDieRec = 0; 

		int totalCaught = 0; 
		double A = 0.00199663514145298; //0.5; // 20% die in a year
		double Zyear = Math.log(-1d/(A-1)); //
		double Mprop = 1; //0.49; // half of those killed die from natural mortality
		double  Fprop_comm = 0; //0.02;	//quarter die from commercial fishery
		double Fprop_rec = 0; //0.49; //quarter die from recreational harvest

		double Myear = 	Zyear * Mprop; // 
		double Fyear_comm = Zyear*Fprop_comm; 
		double Fyear_rec = Zyear*Fprop_rec; 
		double propRecCatch = 0.3; // here, 30% of those that are caught end up dying either through direct harvest or release mortality


		double Zstep =Math.log(-1/((A/numYearIntervals)-1)); 
		double Mstep = Zstep*Mprop;
		double Fstep_comm = Zstep*Fprop_comm; 
		double Fstep_rec = Zstep*Fprop_rec; 
		double totRecCatch = Fstep_rec/propRecCatch; //(1d/propRecCatch)*(Zstep*Fprop_rec); 

		boolean alive = false; 
		double probOfNatMort=0, probOfFishCommMort = 0, probOfFishRecMort = 0, probOfRecCatch = 0;  

		//System.out.println("prob of natural mort: " + (1-Math.pow((Math.exp(-(M))), 1)) + "\t prob of fishing mort: " + (1-Math.pow((Math.exp(-(F))), 1))); 

		/**
		 * Note: this formulation gives correct rates if run for shorter time periods, and know the proportion of individuals harvested in each category (natural, recreational, and commercial).  
		 * For some reason, if loop through all the time periods for a single fish through it's life, this formulation will give different results versus looping through all fish for a single time period, then moving 
		 */
		for (int i = 0; i<totalFish; i++){

			for (int j=0; j<numYearIntervals; j++) {
				probOfNatMort =   1-Math.exp(- Mstep ); 

				if (uniform.nextDoubleFromTo(0, 1) < probOfNatMort) {
					alive=false;
					numDie++;
					numDieNat++;
				}
				else alive=true; 

				if (alive)	{
					probOfRecCatch = 1-Math.exp(-totRecCatch); 
					if (uniform.nextDoubleFromTo(0, 1) < probOfRecCatch) {
						probOfFishRecMort = propRecCatch; //(1-Math.exp(-Fstep_rec) ) + ( 1-probOfRecCatch); 
						if (uniform.nextDoubleFromTo(0, 1) < probOfFishRecMort) {
							alive = false; 
							numDie++;
							numDieRec++; 
						}
					}
				}

				if (alive){
					probOfFishCommMort = 1-Math.exp(-Fstep_comm); 
					if (uniform.nextDoubleFromTo(0, 1) < probOfFishCommMort) {
						alive = false; 
						numDie++;
						numDieComm++; 
					}

				}

			}

		}
		double totDie = (double) numDie / (double) totalFish;
		double totDieNat = (double) numDieNat / (double) numDie;
		double totDieRec = (double) numDieRec / (double) numDie;
		double totDieComm = (double) numDieComm  / (double) numDie;
		System.out.println("total die: " + totDie + "\tnatdie:\t" + totDieNat + "\trec die:\t" + totDieRec + "\tcomm die:\t" + totDieComm	); 

	}


	public void mortality2(){

		double numYearIntervals =15; 

		int totalFish = 100000;
		int numDie = 0;
		int numDieNat = 0;
		int numDieComm = 0;
		int numDieRec = 0; 

		int totalCaught = 0; 
		//0.00054742

		double A = 0.5; //0.00054742; //0.5; // 20% die in a year
		double Zyear = Math.log(-1d/(A-1)); //
		double Mprop = 1; //0.49; // half of those killed die from natural mortality
		double  Fprop_comm = 0; //0.02;	//quarter die from commercial fishery
		double Fprop_rec = 0; //0.49; //quarter die from recreational harvest

		double Myear = 	Zyear * Mprop; // 
		double Fyear_comm = Zyear*Fprop_comm; 
		double Fyear_rec = Zyear*Fprop_rec; 
		double propRecCatch = 0.3; // here, 30% of those that are caught end up dying either through direct harvest or release mortality


		double Zstep =Math.log(-1/((A/numYearIntervals)-1)); 
		double Mstep = Myear/(double) numYearIntervals; //Zstep*Mprop;
		double Fstep_comm = Fyear_comm/(double)numYearIntervals; //Zstep*Fprop_comm; 
		double Fstep_rec = Fyear_rec/(double)numYearIntervals; //Zstep*Fprop_rec; 
		double totRecCatch = Fstep_rec/propRecCatch; //(1d/propRecCatch)*(Zstep*Fprop_rec); 

		boolean alive = false; 
		double probOfNatMort=0, probOfFishCommMort = 0, probOfFishRecMort = 0, probOfRecCatch = 0;  

		//System.out.println("prob of natural mort: " + (1-Math.pow((Math.exp(-(M))), 1)) + "\t prob of fishing mort: " + (1-Math.pow((Math.exp(-(F))), 1))); 

		/**
		 * Note: this formulation gives correct rates if run for shorter time periods, and know the proportion of individuals harvested in each category (natural, recreational, and commercial).  
		 * For some reason, if loop through all the time periods for a single fish through it's life, this formulation will give different results versus looping through all fish for a single time period, then moving 
		 */
		for (int i = 0; i<totalFish; i++){
			alive = true;
			int counter = 0; 

			while (alive && (counter<numYearIntervals)){

				//for (int j=0; j<numYearIntervals; j++) {

				if (alive)	{
					probOfRecCatch = 1-Math.exp(-totRecCatch); 
					if (uniform.nextDoubleFromTo(0, 1) < probOfRecCatch) {
						probOfFishRecMort = propRecCatch; //(1-Math.exp(-Fstep_rec) ) + ( 1-probOfRecCatch); 
						if (uniform.nextDoubleFromTo(0, 1) < probOfFishRecMort) {
							alive = false; 
							numDie++;
							numDieRec++; 
						}
					}
				}

				if (alive){
					probOfFishCommMort = 1-Math.exp(-Fstep_comm); 
					if (uniform.nextDoubleFromTo(0, 1) < probOfFishCommMort) {
						alive = false; 
						numDie++;
						numDieComm++; 
					}
				}


				if (alive){
					probOfNatMort =   1-Math.exp(- Myear ); 
					if (uniform.nextDoubleFromTo(0, 1) < probOfNatMort) {
						alive=false;
						numDie++;
						numDieNat++;
					}
				}




				counter++; 

			}
		}
		double totDie = (double) numDie / (double) totalFish;
		double totDieNat = (double) numDieNat / (double) numDie;
		double totDieRec = (double) numDieRec / (double) numDie;
		double totDieComm = (double) numDieComm  / (double) numDie;
		System.out.println("total die: " + totDie + "\tnatdie:\t" + totDieNat + "\trec die:\t" + totDieRec + "\tcomm die:\t" + totDieComm	); 

	}




	public void mortalityOut(){
		try { outFile = new PrintWriter(new FileWriter("output/MortRatesForStats.txt", true));
		} catch (IOException e) {e.printStackTrace();}

		outFile.println("A\t" + "Z\t" + "M\t" + "F\t" + "DeltaA\t"+ "Mprop\t" + "Fprop\t" +  "divisor\t" +"DeltaM\t" + "DealtaF\t"	);

		double M50, Z50, F50; 
		double divisor = 0; 
		Z50 = Math.log(-1d/(0.5-1));
		M50 = Z50/2; 
		F50=Z50/2; 

		double A, Mprop, Fprop, M, Z, F; 

		for (int i=0; i<2000; i++){
			A = uniform.nextDoubleFromTo(0, 1); 
			divisor = uniform.nextDoubleFromTo(1, 365); 
			Mprop = 0.5; //uniform.nextDoubleFromTo(0.1, 0.9); 
			Fprop = 1-Mprop; 
			Z=Math.log(-1d/((A/divisor)-1)); 
			M = Z*Mprop; 
			F=Z-M; 

			outFile.println(A + "\t" + Z + "\t" + M + "\t" + F + "\t" + (0.5-A) + "\t" + Mprop + "\t" + Fprop + "\t" + divisor + "\t" + (M50-M) + "\t" + (F50-F)); 

		}
		outFile.close(); 
	}


	public void mortalityListOfRates(){

		/*		gen:	1	best fitness so far: 	1000.999	avg fitness: 	1000.4380573148142	Z: 0.10674261590588302
		gen:	2	best fitness so far: 	1000.9996666666667	avg fitness: 	1000.6680494444452	Z: 0.10519404356581989
		gen:	3	best fitness so far: 	1000.9996666666667	avg fitness: 	1000.7854100925919	Z: 0.10546472709759028
		gen:	4	best fitness so far: 	1000.9996666666667	avg fitness: 	1000.8578187037039	Z: 0.10546472709759028
		gen:	5	best fitness so far: 	1000.9996666666667	avg fitness: 	1000.9057531481494	Z: 0.10546472709759028
		gen:	6	best fitness so far: 	1001.0	avg fitness: 	1000.9345642592581	Z: 0.10433476024395916
		gen:	7	best fitness so far: 	1001.0	avg fitness: 	1000.9550556481483	Z: 0.1072201241744845
		gen:	8	best fitness so far: 	1001.0	avg fitness: 	1000.9700775	Z: 0.1072201241744845
		gen:	9	best fitness so far: 	1001.0	avg fitness: 	1000.9789607407396	Z: 0.11067900309265992
		gen:	10	best fitness so far: 	1001.0	avg fitness: 	1000.9852682407408	Z: 0.10546472709759028
		 */		

		int totalFish = 100000;
		int numDie = 0; 
		double A = 1.2; 
		double Z = 0.001; 
		boolean isInst = false; 

		double scaler = 0.001;
		for (int x = 0; x< 1000; x++){
			int numFishCaught = 0; 

			for (int i=0; i< totalFish; i++){
				for (int j=0; j<12; j++){

					double probOfCatch = 1- Math.exp(-Z);
					if (uniform.nextDoubleFromTo(0, 1) < probOfCatch){
						numFishCaught++; 
					}

				}

			}

			double propCaught = (double) numFishCaught / (double) totalFish; 
			System.out.println("Z:\t" + Z + "\tA:\t" + propCaught); 
			Z+=0.001; 
		}
	}	




	public void mortalityTest(){

		/*		gen:	1	best fitness so far: 	1000.999	avg fitness: 	1000.4380573148142	Z: 0.10674261590588302
		gen:	2	best fitness so far: 	1000.9996666666667	avg fitness: 	1000.6680494444452	Z: 0.10519404356581989
		gen:	3	best fitness so far: 	1000.9996666666667	avg fitness: 	1000.7854100925919	Z: 0.10546472709759028
		gen:	4	best fitness so far: 	1000.9996666666667	avg fitness: 	1000.8578187037039	Z: 0.10546472709759028
		gen:	5	best fitness so far: 	1000.9996666666667	avg fitness: 	1000.9057531481494	Z: 0.10546472709759028
		gen:	6	best fitness so far: 	1001.0	avg fitness: 	1000.9345642592581	Z: 0.10433476024395916
		gen:	7	best fitness so far: 	1001.0	avg fitness: 	1000.9550556481483	Z: 0.1072201241744845
		gen:	8	best fitness so far: 	1001.0	avg fitness: 	1000.9700775	Z: 0.1072201241744845
		gen:	9	best fitness so far: 	1001.0	avg fitness: 	1000.9789607407396	Z: 0.11067900309265992
		gen:	10	best fitness so far: 	1001.0	avg fitness: 	1000.9852682407408	Z: 0.10546472709759028
		 */		

		int totalFish = 10000;
		double A = 0.2; 
		double Z= 0.223144; //yearly rate for A=0.2
		//double Z = 0.10503928902172623; // monthly for A=1.2
		double runTime = 1; // per day basis 


		int numFishCaught = 0; 


		for (int i=0; i< totalFish; i++){
			for (int j=0; j<365; j++){

				//					double probOfCatch = 1- Math.exp(-Z);
				double probOfCatch = 1- Math.pow(Math.exp(-Z), runTime/365.25);
				if (uniform.nextDoubleFromTo(0, 1) < probOfCatch){
					numFishCaught++; 
				}

			}

		}

		double propCaught = (double) numFishCaught / (double) totalFish; 
		System.out.println("\tA:\t" + propCaught);


	}

	/*
	public void mortalityOldOld(){
		double age =11.99;
		double ageMax = 12; 
		int groupSize =1; 
		double proportionToDie = 0; 
		double mortalityRate = 0; 
		Double  currentMortality = new Double(0); 
		double baseMortality = 0.3; 
		double maturityAge =1; 
		double amod = 0; 
		int runTime = 31536000;
		int Syr = 31536000; 


		amod = ((ageMax + maturityAge)/2d) - maturityAge; 
		//amod = ((params.getAgeMax() + params.getAgeMaturity())/2) - params.getAgeMaturity(); 

		System.out.println("amod: "+ amod); 

		currentMortality  = Math.min(   1,   Math.max(    0,   (((  1-Math.sqrt((1-baseMortality)*(1-baseMortality)*(1-(((age-amod)*(age-amod))/(((ageMax+maturityAge)/2d)*((ageMax+maturityAge)/2d))))  )  ) * (double) runTime)/(double)Syr))  )  ; 
		//currentMortality  = Math.min(   1,   Math.max(0,   (((  1-Math.sqrt((1-params.getBaseMortality())*(1-params.getBaseMortality())*(1-(((groupAge-amod)*(groupAge-amod))/(((params.getAgeMax()+params.getAgeMaturity())/2)*((params.getAgeMax()+params.getAgeMaturity())/2))))  )  ) * (double) runTime)/(double)TimeUtils.secPerYear))  )  ; 

		System.out.println("Km: "+ currentMortality ); 

		if ( (uniform.nextDoubleFromTo(0, 1) < currentMortality  || currentMortality.isNaN())) {
			if (age < ageMax) proportionToDie = 0.001*(uniform.nextDoubleFromTo(0, 1)); 
			else proportionToDie = 1;

			System.out.println("j: "+ proportionToDie); 


			mortalityRate = Math.max(proportionToDie*groupSize, 1); 
			System.out.println("mortality rate: " + mortalityRate ); 
		}

		else System.out.println("no mortality this tick");
	}


	public void mortalityOld(){

		double groupAge = 0; 
		int groupAbundance = 1; 
		double proportionToDie = 0; 
		Double  currentMortality = new Double(0); 
		double amod = 0; 
		int stage = 0; 
		long runTime = TimeUtils.SECS_PER_YEAR; 

		amod = ((params.getAgeMax() + params.getAgeAtRecruitment())/2d) - params.getAgeAtRecruitment(); 
		System.out.println("amod: " + amod); 

		for (int j=0; j<14; j++){
			for (int i=0; i<12; i++){
				currentMortality  = Math.min(   1,   Math.max(0,   ( (  1-Math.sqrt(   (1-params.getBaseMortality(stage))  *  (1-params.getBaseMortality(stage))  *  (1-(   ((groupAge-amod)*(groupAge-amod))  /  (  ((params.getAgeMax()+params.getAgeAtRecruitment())/2d) * ((params.getAgeMax()+params.getAgeAtRecruitment())/2d)  ) ) )  ) )  * (( (double) runTime)/((double)TimeUtils.SECS_PER_YEAR))  ) ) )  ;

				System.out.println("age (in years): " + groupAge + "\tcurrentMortality: " + currentMortality); 

				if ( (uniform.nextDoubleFromTo(0, 1) < currentMortality  || currentMortality.isNaN())) {
					if (groupAge < params.getAgeMax()) proportionToDie = 0.001*(uniform.nextDoubleFromTo(0, 1)); 
					else proportionToDie = 1;

					// here, mortaltiy rate
					double mortalityRate = Math.max(proportionToDie*groupAbundance, 1); 


					//NOTE: if groupSize==0, will remove this agent in the processRates() method and then return out of method; since processRates() is last method, will return from step
				}
				groupAge = groupAge + (1d/12d);

			}
			//runTime += TimeUtils.secPerDay; 
		}
	}



	 * This approach assumes that M, instantaneous rate, can be used as surrogate for proportional rate.  NOT RIGHT!

	public void mortality(){


		int age = 4; 
		long runTime = TimeUtils.SECS_PER_DAY; 

		for (int i =1; i<366; i++){

			double probOfDeath = 1- Math.pow((1-params.getBaseMortality(age)), (1d/( (double) TimeUtils.SECS_PER_YEAR/ (double) runTime))); 

			System.out.println("runTime (in days): " + i + "\tcurrent mortality: " + probOfDeath ); 

			runTime = runTime + TimeUtils.SECS_PER_DAY; 
		}

	}
	 */


}
