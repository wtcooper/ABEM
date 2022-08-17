package us.fl.state.fwc.abem.spawn;

public class Kupschus2004Test {


	public void run(){
		
		Kupschus2004Model model = new Kupschus2004Model();
		model.initialize();


		
		
		//############################################
		// Param sweep to get optimal value
		//############################################
		/*
		double maxProb = .42;
		double bestIntercept = 0;
		double diff = Double.MAX_VALUE;

		for (double i=-3.5; i<-3.3; i+=.00001){
			model.setIntercept(i);

			double maxProbCompute = Double.MIN_VALUE;
			double TL = 398; //max value
			double condition = .892;
			double lunar =0;
			for (double temp=29; temp<31; temp+=.1) {
				for (double relTime=-2; relTime<0; relTime+=.1){
						double prob = model.getProbOfSpawn(temp, TL, condition, relTime, lunar);
						if (prob > maxProbCompute) maxProbCompute = prob;
				}
			}
			
			if (Math.abs(maxProbCompute-maxProb) < diff) {
				diff = Math.abs(maxProbCompute-maxProb) ;
				bestIntercept = i;
			}

		}

		System.out.println("best intercept: " + bestIntercept);

		
*/		
		//############################################
		// Test to make sure functions returning proper value
		//############################################
		/*
		
		for (int i= 10; i<30; i=i+5){
			double val = model.getFunctionValue(i, "temp");
			System.out.println("temp: " + i + "\tvalue: " + val);
		}

		System.out.println();
		for (int i= -10; i<10; i=i+5){
			double val = model.getFunctionValue(i, "relTime");
			System.out.println("relTime: " + i + "\tvalue: " + val);
		}

		System.out.println();
		for (int i= 200; i<700; i=i+100){
			double val = model.getFunctionValue(i, "TL");
			System.out.println("TL: " + i + "\tvalue: " + val);
		}
	
		System.out.println();
		for (double i= .06; i<.12; i=i+.01){
			double val = model.getFunctionValue(i, "condition");
			System.out.println("condition: " + i + "\tvalue: " + val);
		}

		System.out.println();
		for (double i= 0; i<30; i=i+5){
			double val = model.getFunctionValue(i, "lunar");
			System.out.println("lunar: " + i + "\tvalue: " + val);
		}
*/
		//############################################
		// Simle look at probability ~ optimal values
		//############################################
		
		//Check max values:
		for (double temp=20; temp<35; temp+=1) {

		double prob = model.getProbOfSpawn(temp,400, .892, -1.6, 0);
		System.out.println("temp: " + temp + "\tprob: " + prob);
		}

			
	}

	
	
	public static void main(String[] args) {
		Kupschus2004Test test = new Kupschus2004Test();
		test.run();
	}

}
