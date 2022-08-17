package us.fl.state.fwc.abem.spawn;

import java.util.ArrayList;

import cern.jet.random.Normal;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister64;

public class SpawnIntervalTest {

	long seed = System.currentTimeMillis(); 
	public MersenneTwister64 m = new MersenneTwister64((int) seed); 
	public Uniform uniform= new Uniform(m); 
	public Normal normal= new Normal(0,1,m); 



	/*This method shows that the time that a fish has a marker/spawning indicator present 
	 * will definitely affects its percieved probability, irrespective of its actual probability.  
	 * 
	 * Therefore, need to account for this when calculating a probability for a specific time step.
	 * 
	 */
	
	public void run() {

		int numFish = 10000;
		int numTimeSteps = 365; //2 months
		
		double probOfSpawning = .1; 
		double timeOfMarker = 1; //2 days
		
		
		for (int k = 1; k<=10; k++){
			timeOfMarker = k;


			ArrayList<Spawner> list = new ArrayList<Spawner>();

			//initialize if already spawned
			for (int j=0; j<numFish; j++){
				Spawner fish = new Spawner();
				if (uniform.nextDoubleFromTo(0, 1) < probOfSpawning){
					fish.lastSpawnTime = 0;
					fish.markerOn = true;
				}
				list.add(fish);
			}


			int[] numFishWithMarker = new int[numFish];

			//loop through each day
			for (int i=1; i<numTimeSteps; i++){
				for (int j=0; j<numFish; j++){
					Spawner fish = list.get(j);

					if (uniform.nextDoubleFromTo(0, 1) < probOfSpawning){
						fish.lastSpawnTime = i;
						fish.markerOn = true;
					}
					else {
						//if marker is already on, check to see if it needs reset to false
						if (fish.markerOn){
							if (i-fish.lastSpawnTime>= timeOfMarker) 
								fish.markerOn = false;
						}
					}

					if (fish.markerOn) numFishWithMarker[i]++;
				}
			}


			double average = 0;
			for (int i=1; i<numTimeSteps; i++){
				average += numFishWithMarker[i]/(double) numFish;
			}
			average /= (double) numTimeSteps;

			System.out.println("timeOfMarker:\t" + k + "\tproportion spawning:\t" + average);

		}


	}

	
	
	/*
	 * This method shows that as you can run for any time step and simply divide the calculated
	 * probability by the time unit, and still get the same probability in the end
	 */
	public void run2() {

		
		int numFish = 100000;
		double numTimeSteps = 1000; //2 months
		
		double initialProbOfSpawning = .1; 
		double probOfSpawning = .1; 
		double timeStep = 1; //2 days
		double timeUnits = 5;
		
		for (int k = 1; k<=10; k++){
			timeStep = (double) k;
			probOfSpawning = initialProbOfSpawning*(timeStep/timeUnits);
			double numSpawners = 0; 

			
			//loop through each day
			double counter = 0;
			for (int i=1; i<numTimeSteps; i+=timeStep){
				counter++;
				for (int j=0; j<numFish; j++){
					
					if (uniform.nextDoubleFromTo(0, 1) < probOfSpawning){
						numSpawners++;
					}

				}
			}


			double average = numSpawners/((numTimeSteps/timeUnits)*numFish);

			System.out.println("time step:\t" + k + "\tproportion spawning:\t" + average);

		}


	}


	public static void main(String[] args) {
		SpawnIntervalTest test = new SpawnIntervalTest();
		test.run2();
	}

}


class Spawner{

	public double lastSpawnTime;
	public boolean markerOn = false;
}