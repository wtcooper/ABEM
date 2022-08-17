package us.fl.state.fwc.abem.optimize;

public class FitnessComparator implements Comparable<FitnessComparator> {

	public double fitness; 
	public double value;
	
	public FitnessComparator(double fitness, double value) {
		this.fitness = fitness;
		this.value = value;
	}


	@Override
	public int compareTo(FitnessComparator o) {
		double thisFit = fitness;
		double thatFit = o.fitness;
		
		if (thisFit == thatFit) {  
			// then check the priority, and if they're equal,
				// randomly allocate which one goes first
				if (Math.random() < 0.5) return 1; 
				else return -1;
		}
		// else if next schedule tick is not equal, run the one that comes first
		else if (thisFit < thatFit) return -1;

		else if (thisFit > thatFit) return 1;

		return 0;
	}
	

}
