package us.fl.state.fwc.abem.test;

import java.text.DecimalFormat;

import us.fl.state.fwc.abem.params.impl.SeatroutParams;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;

public class GrowthTest {

	protected DecimalFormat df = new DecimalFormat("#.####"); 
	protected DecimalFormat df2 = new DecimalFormat("#"); 

	long seed = System.currentTimeMillis(); 
	MersenneTwister m = new MersenneTwister((int) seed); 
	Uniform uniform = new Uniform(m); 

	double biomass, grayMass, biomass2;
	double nominalBiomass, grayNominalMass; 
	double age; // in year
	double runTime = 60*60*24*1; // 1 year in seconds 
	double length=1; 
	double length2; 

	
	 //Seatrout; modified MM
			double c = 2.0301; // c parameter defines the shape of the weight as function of age relationship: c <=1 is saturating; c>1 is sigmoidal
			double K = 7.1504; // K is constant which defines the growth rate, where increasing K is faster growth rate;  here, K is in per year units, calculated from vonBertalanffy length curve
			double iniMass = 0.001; // will need to get accurate estimate of initial mass -- this should be weight at 
			double maxMass = 4.569382*1000; // maximum mass

	// Reddrum; modified MM
	//		double c = 2.4599; // c parameter defines the shape of the weight as function of age relationship: c <=1 is saturating; c>1 is sigmoidal
	//		double K = 3.7612; // K is constant which defines the growth rate, where increasing K is faster growth rate;  here, K is in per year units, calculated from vonBertalanffy length curve
	//		double iniMass = 0.001; // will need to get accurate estimate of initial mass -- this should be weight at 
	//		double maxMass = 11.77916; // maximum mass

	// Snook; modified MM
	//double c = 2.479; // c parameter defines the shape of the weight as function of age relationship: c <=1 is saturating; c>1 is sigmoidal
	//double K = 7.6941; // K is constant which defines the growth rate, where increasing K is faster growth rate;  here, K is in per year units, calculated from vonBertalanffy length curve
	//double iniMass = 0.001; // will need to get accurate estimate of initial mass -- this should be weight at 
	//double maxMass = 8.045; // maximum mass




	public static void main(String[] args) {

		GrowthTest mt = new GrowthTest();
		mt.step2(); 
	}


	public void step2() {
		SeatroutParams params = new SeatroutParams(); 
		
		biomass = 0; 
		//loop through up to 12 years of age
		for (int i = 1; i < 365*12; i++) {

			double avgGrowthForAge = 
				params.getLengthAtAge(i/365.25, 0)
			-  params.getLengthAtAge((i-1)/365.25, 0);
			
			double avgLength = params.getLengthAtMass(biomass, 0);
			
			avgLength += avgGrowthForAge; 
			
			biomass = params.getMassAtLength(avgLength, 0);
			
			//TODO -- see how to do a nominal biomass statement
			nominalBiomass = biomass; 

			System.out.println( i + "\t" + df.format(avgLength) + "\t" + df.format(biomass));
		}

		
		
	}
	
	
	public void step(){


		biomass = 1; 
		nominalBiomass = 1; 
		grayMass = biomass; 
		grayNominalMass = nominalBiomass; 

		
		
		double counter = 0; 
		int counter2 = 0;

		for (int i = 1; i < 365*12; i++) {
			
			counter += (runTime)/(60*60*24); 
			//setMMGrowth(); 
			this.setVonBertGrowth(i);
			
//			System.out.println("days: " + counter + "\tnew n mass: " + df.format(biomass) + "\tnew n nominal mass: " + df.format(nominalBiomass)); 
			if (i%365 == 0) 
				System.out.println(++counter2 + "\t" + df.format(length) + "\t" + df.format(biomass)+ "\t" + df.format(biomass2));

			

			// Gray et al. approach
			double tm = maxMass - grayMass;
			double tn = maxMass - grayNominalMass;
			double lambdaMass = 0.0774/(365.25*86400); 
			double Lm = Math.exp(-lambdaMass * runTime);

			tm = tm * Lm;
			tn = tn * Lm;

			grayNominalMass = maxMass - tn;
			grayMass = maxMass - (tm+tn)/2.0;

		//	System.out.println("new g mass: " + df.format(grayMass) + "\tnew g nominal mass: " + df.format(grayNominalMass)); 

		}

	}	




	public void  setMMGrowth() {
		double newGrowth, newGrowthNom; 
		double tPrime, tPrimeNom; 
		double runTimePrime = ((double) runTime) / (60*60*24*365.25); 
		double tempMass = 0, tempNomMass = 0; 
		double weight; 
		int massCatchUpRate = 6; 

		if (biomass >= maxMass) tempMass = maxMass-0.0001; // here, set this so that new growth rate will equal growth rate for the maximum mass.  If over this, will return NaN
		else tempMass = biomass; 

		if (nominalBiomass >= maxMass	) tempNomMass = maxMass-0.0001; 
		else tempNomMass = nominalBiomass; 

		tPrime = Math.pow(((iniMass*Math.pow(K,c))/(tempMass-maxMass) - (tempMass*Math.pow(K,c))/(tempMass-maxMass)),(1/c)); 
		tPrimeNom = Math.pow(((iniMass*Math.pow(K,c))/(tempNomMass-maxMass) - (tempNomMass*Math.pow(K,c))/(tempNomMass-maxMass)),(1/c)); 

		newGrowth = ((iniMass*Math.pow(K,c) + maxMass*Math.pow(tPrime+runTimePrime, c))/(Math.pow(K,c) + Math.pow(tPrime+runTimePrime,c))) - ((iniMass*Math.pow(K,c) + maxMass*Math.pow(tPrime, c))/(Math.pow(K,c) + Math.pow(tPrime,c))); 
		newGrowthNom = ((iniMass*Math.pow(K,c) + maxMass*Math.pow(tPrimeNom+runTimePrime, c))/(Math.pow(K,c) + Math.pow(tPrimeNom+runTimePrime,c))) - ((iniMass*Math.pow(K,c) + maxMass*Math.pow(tPrimeNom, c))/(Math.pow(K,c) + Math.pow(tPrimeNom,c))); 

		biomass+=newGrowth;
		nominalBiomass+=newGrowthNom; 

		double secsInYear = (60*60*24*365.25); 
		
		if (runTime > (secsInYear/2)) {
			weight = 1; 
			//System.out.println("runtime greater than 6 months, setting weight to 1");
		}
		else {
			weight = runTime / (secsInYear/massCatchUpRate); 
			if (weight > 1) weight = 1; 
			//System.out.println("weight: " + df.format(weight));
		}
		biomass = (1-weight)*biomass+(weight)*nominalBiomass; 
	}
	
	public void setVonBertGrowth(int i) {
		
		double  k= 0.217538462;  			//K in vonBert age-length relationship
		double  Lmax= 68.91538;  	//L infinity in vonBert age-length relationship, in centimeters
		double  t0 = -0.381666667;  	//t0 in von Bert age-length relationship
		double a = 0.0131;									// 'a' length-weight conversion factor, where lenght = a*(weight^b); HERE, weight is structural weight (no gonad or stomach content)
		double b = 3;								// 'b' length-weight conversion factor
		//double a = 0.0116;									// 'a' length-weight conversion factor, where lenght = a*(weight^b); HERE, weight is structural weight (no gonad or stomach content)
		//double b = 2.9204;								// 'b' length-weight conversion factor

		double age = (double) i/365;
		length2 = Lmax*(1-Math.exp(-k*(age-t0)));

		if (i == 1){
			//set initial length
			//L = Lso[l - exp {-kit- to)}]
			length = Lmax*(1-Math.exp(-k*(age-t0)));
		}
		
		else {
		
			double growth = (Lmax*Math.exp(k*t0))*k*Math.exp(-k*age);
			double growth2 = k*Lmax - k*(age-(1.0/365.0));

			double growth3 = Lmax*(1-Math.exp(-k*((age+(1.0/365.0))-t0))) 
			-Lmax*(1-Math.exp(-k*(age-t0))); 

			length += growth3;

			biomass = a*Math.pow(length, b);
			
		}
		/*			// Von Bertalanffy
		 * 
		 * dL/dt = bk exp {-kt}
		 * 		with b =Linf *  exp {k to}
		 * 
		 * or
		 * 
		 * dLjdt = kLmax — kLt-1
 _i
 
 
		double vbWeight =wtMax*(1-Math.exp((-obsK*(i-t0))));  
		double vbGrowth = wtMax*(1-Math.exp((-obsK*(i-1-t0)))) - wtMax*(1-Math.exp((-obsK*(i-1-t0)))) ; 

		// modified MM (Lopez et al. 2000)
		double mmWeight = (wtAge0*Math.pow(7.6941, 2.479) + wtMax*Math.pow(i, 2.479)) / (Math.pow(7.6941, 2.479) + Math.pow(i, 2.479)); 

		// modified MM growth rate (per year)
		double mmGrowth = (2.479*(Math.pow(i, 2.479-1)))/(Math.pow(7.6941, 2.479) + Math.pow(i, 2.479)); 
		double mmGrowth2 = (wtAge0*Math.pow(7.6941, 2.479) + wtMax*Math.pow(i, 2.479)) / (Math.pow(7.6941, 2.479) + Math.pow(i, 2.479)) - (wtAge0*Math.pow(7.6941, 2.479) + wtMax*Math.pow(i-1, 2.479)) / (Math.pow(7.6941, 2.479) + Math.pow(i-1, 2.479)); 

	//	System.out.println ("Weight at age " + i + ": " + mmWeight + "\tmmGrowth 1: " + mmGrowth + "\tmmGrowth 2: "+mmGrowth2); 
	//	System.out.println (i + "\t" + mmWeight +  "\t" + mmGrowth + "\t" + mmGrowth2 + "\t" + mmGrowth2/mmGrowth); 


	// Gray et al. approach
	double mass = 12;
	double maxMass = 20;
	double lambdaMass = 0.03/(365.25*86400); 
	double nominalMass = 12; 
	double runTime = 604800*52; //604800 = 1 week 

	double tm = maxMass - mass;
	double tn = maxMass - nominalMass;


	double Lm = Math.exp(-lambdaMass * runTime);

	tm = tm * Lm;
	tn = tn * Lm;

	nominalMass = maxMass - tn;
	mass = maxMass - (tm+tn)/2.0;

	System.out.println("new nominal mass: " + nominalMass)	; 
	System.out.println("new mass: " + mass)	; 

 */


	}



}
