package us.fl.state.fwc.abem.test;

import javolution.util.FastMap;
import javolution.util.FastTable;
import us.fl.state.fwc.util.geo.CoordinateUtils;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;

import com.vividsolutions.jts.geom.Coordinate;

public class HabSearchTest {

	long seed = System.currentTimeMillis(); 
	MersenneTwister m = new MersenneTwister((int) seed); 
	Uniform uniform = new Uniform(m); 

	protected FastTable<Coordinate> testSites = new FastTable<Coordinate>(); 
	protected FastTable<Coordinate> polyCoords = new FastTable<Coordinate>(); 
	protected int numTestSites = 36; 
	protected int numPointsPerSpiral = 6; 
	protected FastMap<Integer,Double> suitableHabitats = new FastMap<Integer,Double>(); 
	protected FastTable<Integer> suitableHabitatsTest = new FastTable<Integer>(); 
	protected int numSuitableHabitats; 

	protected double cV, cU, cW, wV, wU, wW, depth, newx, newy, newz, oldx, oldy, oldz;
	protected int habitat; 
	protected double habitatQuality; 
	Coordinate coords = new Coordinate (500, 500, 0); 

	protected int maxDepth, minDepth, preferredDepth; 
	double theta; 
	double wv; 

	int gridHeight = 1000;
	int gridWidth = 1000; 
	
	public int[][] landscape = new int[gridHeight][gridWidth]; 

	protected int comfortScaler; 



	public void step(){
		setSuitableHabitats(); 
		setDepthPreference(); 
		comfortScaler = 100;  // will need to set this at beginning of agent's step method based on hunder and fear

		buildSpatialLandscape(); 

		directedMove(60*60); 
		
		polyCoords.add(new Coordinate(250, 250)); 
		polyCoords.add(new Coordinate(750, 250)); 
		polyCoords.add(new Coordinate(750, 750)); 
		polyCoords.add(new Coordinate(250, 750)); 
		polyCoords.add(new Coordinate(250, 250)); 
		
	PolygonDraw poly= new PolygonDraw(gridWidth, gridHeight, polyCoords, testSites);	
	
	poly.setVisible(true); 

	}







	public void directedMove(int runTime){

		
		testSites = FastTable.newInstance(); 

		//if (consoleOutput ) System.out.println("\t" + this.getClassName(this.getClass())+ " directed moving...."  );  
		//** need to do some form of test to see if currently following a waypoint track, and if so, will need to continue following it until reach goal. If reach goal
		// within a single time step, will then need to re-search (or choose) next waypoint and adjust direction.

		// (1) get current and wind forcing for their area
		setPhysicalProperties(); 

		double rand =uniform.nextDoubleFromTo(0, Math.PI/2); 
		//double yRand = 1; //uniform.nextDoubleFromTo(0, 1); 

		for (int i = 0; i<numTestSites; i++){
			double xRand = uniform.nextDoubleFromTo(-1, 1); 
			double yRand = uniform.nextDoubleFromTo(-1, 1); 
			Coordinate testCoord = new Coordinate(); 
			testCoord.x = this.coords.x + cV*cW*runTime + wV*wW*runTime + Math.cos(rand+i*(Math.PI/(numPointsPerSpiral*0.5))+(i/numPointsPerSpiral)*(Math.PI/numPointsPerSpiral))*getSdir(i, runTime); 
			testCoord.y = this.coords.y + cU*cU*runTime + wU*wU*runTime + Math.sin(rand+i*(Math.PI/(numPointsPerSpiral*0.5))+(i/numPointsPerSpiral)*(Math.PI/numPointsPerSpiral))*getSdir(i, runTime);
			testCoord.z = 0; 
			//testCoord.x = this.coords.x + cV*cW*runTime + wV*wW*runTime + xRand*getSdir(i, runTime); 
			//testCoord.y = this.coords.y + cU*cU*runTime + wU*wU*runTime + yRand*getSdir(i, runTime);

			testSites.add(testCoord); 
			
		
			//System.out.println("x displacement for "+i+" with i/4= " + i/4 + ": " + Math.cos(xRand+i*(Math.PI/2)+(i/4)*(Math.PI/4))); 
			//System.out.println("y displacement for "+i+" with i/4= " + i/4 + ": " + Math.sin(xRand+i*(Math.PI/2)+(i/4)*(Math.PI/4)));
			//System.out.println("sdir: " + getSdir(i, runTime)); 
			//System.out.println(testCoord.x+"\t"+testCoord.y + "\t" + getSdir(i, runTime));
			//if ((i+1)%4 == 0) System.out.println(); 

			System.out.println("distance: " + CoordinateUtils.getDistance(coords, testCoord)); 
		}

/*
		Coordinate bestCoord = new Coordinate(); 
		double bestValue = -999; 
		for (int i = 0; i<testSites.size(); i++){
			// check suitability of each testCoord, set highest value to bestCoord
			if (getHabSuitability(testSites.get(i)) > bestValue) bestCoord = testSites.get(i); 
		}

		System.out.println("bestCoord location ("+bestCoord.x+","+bestCoord.y+")"); 

		// (2) need to assess habitat suitability in spiral around current location

		// (3) after choose appropriate habitat to move towards

		// need to add up individual components from cU*cW, cV*cW, cZ*cW (current directions, U,V,Z, * the weight, cW), wU*wW, wV*wW, wZ*wW (wind), and directed movement 
		//newx = oldx + cV*cW*time + wV*wW*time + Math.cos(angle)*speed*time; 

		//FastTable.recycle(testSites); 
*/
	}






	public double getSdir(int index, int runTime){
		double sdir = 0; 
		double st = 5; // here speed = 5m/s
		theta =1; 
		wv =5; 
		sdir = (1+Math.pow((index/numPointsPerSpiral),theta))*((-1+Math.sqrt(1+4*(Math.pow(wv,2))*st*runTime))/(2*Math.pow(wv,2)));
		//System.out.println("sdir: " + sdir); 
		return sdir; 
	}





	public double getHabSuitability(Coordinate coord) {

		habitat = getHabitat(coord); //schedule.getHabitat().getValue(currentTime, this.coords.x, this.coords.y); 
		if (habitat < 0) return -999;  
		System.out.println("habitat type: " + habitat); 
		habitatQuality = comfortScaler*((Math.min(0, (maxDepth-depth)/2)) + (Math.min(0, (depth - minDepth)/2)) + Math.abs((preferredDepth-depth)/2) + 2*maxDepth*suitableHabitats.get(habitat)); 
		System.out.println("habitat quality: " + habitatQuality); 

		return habitatQuality; 
	}


	public int getHabitat(Coordinate coord){
		if ((coord.x<0) || (coord.x>(gridWidth-1)) ||  (coord.y<0) || (coord.y>(gridWidth-1))) return -999; 
		
		int habitat = landscape[(int)coord.x][(int)coord.y];   
		
		return habitat; 
	}

	
	
	public void setPhysicalProperties(){
		depth = 0; 

		cV = 0; cU = 0; cW = 1; 
		wV = 0; wU = 0; wW = 1; 
	}





	public void setSuitableHabitats() {
		suitableHabitats.put(9116, 2.0); //continuous seagrass
		suitableHabitats.put(9113, 1.5); // discountinuous seagrass
		suitableHabitats.put(9121, 1.); // algal beds
		suitableHabitats.put(5700, 1.); // tidal flats
		suitableHabitats.put(0, 0.); 
		//suitableHabitats.add(2); // open water, i.e., everything else out there that isn't 


		suitableHabitatsTest.add(9116); //continuous seagrass
		suitableHabitatsTest.add(9113); // discountinuous seagrass
		suitableHabitatsTest.add(9121); // algal beds
		suitableHabitatsTest.add(5700); // tidal flats
		suitableHabitatsTest.add(0);

	}


	public void setDepthPreference(){
		preferredDepth = 5;
		maxDepth = 200; 
		minDepth = 0; 
	}


	public void buildSpatialLandscape(){

		double prob1 = 0, prob2=0, prob3=0, prob4=0, prob5=0;  
		double sumProb = 0; 
		
		int habType = suitableHabitatsTest.get(uniform.nextIntFromTo(0, 4));

		for (int i = 0; i<gridWidth; i++){
			for (int j=0; j<gridHeight; j++){

				double autoCorr = .75; 

				double baseProb1 = .1;
				double baseProb2 = .1;
				double baseProb3 = .05;
				double baseProb4 = .2; 
				double baseProb5 = .55;  // unsuitable

				if (habType ==9116){
					prob1 = autoCorr;
					prob2 = (1-autoCorr)*(baseProb2/(baseProb1+baseProb2+baseProb3+baseProb4+baseProb5)); 
					prob3 = (1-autoCorr)*(baseProb3/(baseProb1+baseProb2+baseProb3+baseProb4+baseProb5)); 
					prob4 = (1-autoCorr)*(baseProb4/(baseProb1+baseProb2+baseProb3+baseProb4+baseProb5)); 
					prob5 = (1-autoCorr)*(baseProb5/(baseProb1+baseProb2+baseProb3+baseProb4+baseProb5)); 
					sumProb = prob1+prob2+prob3+prob4+prob5; 
				}
				else if (habType ==9113){
					prob1 = (1-autoCorr)*(baseProb1/(baseProb1+baseProb2+baseProb3+baseProb4+baseProb5)); 
					prob2 = autoCorr;
					prob3 = (1-autoCorr)*(baseProb3/(baseProb1+baseProb2+baseProb3+baseProb4+baseProb5)); 
					prob4 = (1-autoCorr)*(baseProb4/(baseProb1+baseProb2+baseProb3+baseProb4+baseProb5)); 
					prob5 = (1-autoCorr)*(baseProb5/(baseProb1+baseProb2+baseProb3+baseProb4+baseProb5)); 
					sumProb = prob1+prob2+prob3+prob4+prob5; 
				}
				else if (habType ==9121){
					prob1 = (1-autoCorr)*(baseProb1/(baseProb1+baseProb2+baseProb3+baseProb4+baseProb5)); 
					prob2 = (1-autoCorr)*(baseProb2/(baseProb1+baseProb2+baseProb3+baseProb4+baseProb5)); 
					prob3 = autoCorr;
					prob4 = (1-autoCorr)*(baseProb4/(baseProb1+baseProb2+baseProb3+baseProb4+baseProb5)); 
					prob5 = (1-autoCorr)*(baseProb5/(baseProb1+baseProb2+baseProb3+baseProb4+baseProb5)); 
					sumProb = prob1+prob2+prob3+prob4+prob5; 
				}
				else if (habType ==5700){
					prob1 = (1-autoCorr)*(baseProb1/(baseProb1+baseProb2+baseProb3+baseProb4+baseProb5)); 
					prob2 = (1-autoCorr)*(baseProb2/(baseProb1+baseProb2+baseProb3+baseProb4+baseProb5)); 
					prob3 = (1-autoCorr)*(baseProb3/(baseProb1+baseProb2+baseProb3+baseProb4+baseProb5)); 
					prob4 = autoCorr; 
					prob5 = (1-autoCorr)*(baseProb5/(baseProb1+baseProb2+baseProb3+baseProb4+baseProb5)); 
					sumProb = prob1+prob2+prob3+prob4+prob5; 
				}
				else {
					prob1 = (1-autoCorr)*(baseProb1/(baseProb1+baseProb2+baseProb3+baseProb4+baseProb5)); 
					prob2 = (1-autoCorr)*(baseProb2/(baseProb1+baseProb2+baseProb3+baseProb4+baseProb5)); 
					prob3 = (1-autoCorr)*(baseProb3/(baseProb1+baseProb2+baseProb3+baseProb4+baseProb5)); 
					prob4 = (1-autoCorr)*(baseProb4/(baseProb1+baseProb2+baseProb3+baseProb4+baseProb5)); 
					prob5 = autoCorr; 
					sumProb = prob1+prob2+prob3+prob4+prob5; 
				}

				

				// Explore potential ways to make rugosity via probability draws
				double habProb = uniform.nextDoubleFromTo(0, sumProb);

				if (habProb < prob1) habType = 9116; // seagrass
				else if (habProb < (prob1+prob2)) habType= 9113; // discontinuous seagrass
				else if (habProb < (prob1+prob2+prob3)) habType= 9121; // algal bds
				else if (habProb < (prob1+prob2+prob3+prob4)) habType= 5700; // mudflats
				else habType= 0; // other

			
				landscape[i][j] = habType; 
			}
		}
	}


	public static void main(String[] args) {
		HabSearchTest hst = new HabSearchTest(); 
		hst.step(); 
	}

}
