package us.fl.state.fwc.util.fractal;

import java.util.Random;

import us.fl.state.fwc.util.geo.DrawRaster;

public class FractalBuilder  {
	private double roughness;
	private int divisions, lod;
	int gridXDim, gridYDim; 
	private double[][] topo; 
	//long seed; // = System.currentTimeMillis();
	Random rng; // = new Random (seed);
	


	/*	lod:		gridwidth/height
	  0:  1
	  1:  2
	  2:  4
	  3:  8
	  4:  16
	  5:  32
	  6:  64
	  7:  128
	  8:  256
	  9:  512
	  10:  1024
	  11:  2048
	  12:  4096
	  13:  8192
	 */

	public FractalBuilder (int gridXDim, int gridYDim, double roughness, long seed) {

		this.roughness = roughness; // this is H, where H is between 0-1; 2^(-H) is the amount to multiply the random number by each time
		this.lod = getLod(gridXDim, gridYDim);
		this.divisions = 1 << lod;  // this is equal to saying 1*2^(lod), so here if lod=7, then divisions (or cells) will be 128
		this.gridXDim = gridXDim;
		this.gridYDim = gridYDim; 
		rng = new Random(seed); 
	}

	   public static void main(String[] args){
		   int xdim=100, ydim=100; 
		   double roughness = 0.5; // low numbers (e.g., .1) -> high spatial aggregation, versus high numbers (~2) -> low spatial aggregation
		   int scaleFactor = 3; // how many pixels to represent for each cell
		   FractalBuilder fb = new FractalBuilder(xdim,ydim,roughness, System.currentTimeMillis()); 
		   //int[][] cats = fb.getCategories(new double[]{.8, .1, .1});
		 new DrawRaster(fb.getCategories(new double[]{.8, .1, .1}), scaleFactor, null );
		   
	    }

	public int getLod(int gridXDim, int gridYDim){
		int dim = gridXDim;
		if (gridYDim > gridXDim) dim = gridYDim;

		if (dim == 1) return 0;
		else if (dim < 3) return 1;
		else if (dim < 6) return 2;
		else if (dim < 12) return 3; 
		else if (dim < 24) return 4; 
		else if (dim < 48) return 5; 
		else if (dim < 96) return 6; 
		else if (dim < 192) return 7; 
		else if (dim < 384) return 8; 
		else if (dim < 384) return 9; 
		else if (dim < 768) return 10; 
		else if (dim < 1536) return 11; 
		else if (dim < 3072) return 12;
		else if (dim < 6144) return 13;
		else return 14; 
	}



	// this is the method that will be called from the main method 
	// will return the terrain array reference

	public double[][] getTerrain() {
		topo = new double[divisions + 1][divisions + 1]; // here, sets the terrain array to appropriate size; lod=7 sets terrain to 129x129;
		double[][] topoAdjust = new double[divisions][divisions]; // this will be final version to pass to main method, which doesn't include the last elements which are same value as first elements for seemless wrapping;

		double cornerHeight; 
		cornerHeight = rnd(); 

		topo[0][0] = cornerHeight; // rnd (); Set this to rnd() if don't want wrappable surface
		topo[0][divisions] = cornerHeight; // rnd (); 
		topo[divisions][divisions] = cornerHeight; // rnd (); 
		topo[divisions][0] = cornerHeight; // rnd (); 


		//double rangeReduce = 1; 
		double range = roughness; 


		// this following code (including square method) is slightly modified from Merlin's code; it is from http://www.java.sys-con.com/read/46231.htm
		// this loops go through the total number of iterations for the entire grid
		for (int i = 0; i < lod; ++ i) {

			//int q = 1 << i;      	// here, q = 1*2^i; for i=0, this will equal 1 (2^0=1)   
			int r = 1 << (lod - i); // here, for the first loop, i=0, so r = divisions or length between points
			int s = r >> 1; 		// here, s = 1/2 of the r, where r>>1 is r/(2^1); so for each loop, r = 1/2 divisions because start with i=0


			// this loops through all of the points in the grid to do a diamond application to each point needed
			for (int j = 0; j < divisions; j += r)  // assume j=x coordinate, so will do 1 point on first loop (x=0, x=divisions-1)
				for (int k = 0; k < divisions; k += r)
					diamond (j, k, r, range);  	// this passes the point coordinate, the distance between the points, and the roughness value


			if (s > 0) //this says that as long as the lod hasn't been finished (i.e., on last loop), keep doing square
				for (int j = 0; j < divisions; j += s) // j is x coordinate; s is half the divisions

					// here k is y coordinate, and starts at 64 ((0+64)/r = 0 with 64 remainder)
					// the next loop will be when k = r, so will do a second point at (0, 128) assumming lod = 7
					for (int k = (j + s) % r; k < divisions; k += r)	// (j+s)%r for fraction is simply the numerator, i.e., for 1st loop is 64
						square (j, k, r, range);


			// These are different options for how to change the roughness constant.  
			// the range *= roughness seems to produce the most "random" landscapes with initial values between 1-2


			//	    rangeReduce = Math.pow(2, -roughness);  // this should be correct interpretation, so after the first loop, the scale will now be
			//	    range = range * rangeReduce; 
			//		  range = range/2.0f;	// this is another option as proposed by "fractal purists" 
			range *= roughness;	// this is option that Merlin and others recommend       

		}



		// This code checks to see if the edges are equal which is a necessity for seamless wrapping
		/*  	for (int j = 0; j <= divisions; j++) {

	    		if (terrain[0][j] == terrain[divisions][j]) {System.out.println("Y-value" + "\t" + j + "\t" + "TRUE");}
	    		else {System.out.println("Y-value" + "\t" + j + "\t" + "FALSE");}
	   	}
		 */  	

		// This code stores the terrain values into a new array, without the equal edges; thus, when passed to HabitatBuilder it is wrappable
		for (int i = 0; i < divisions; i++) {
			for (int j = 0; j < divisions; j++) {
				topoAdjust[i][j] = topo[i][j];
			}
		}
		return topoAdjust; 
	}

	// Diamond step
	private void diamond (int x, int y, int side, double scale) {
		if (side > 1) {
			int half = side / 2;
			double avg = (topo[x][y] + topo[x + side][y] +
					topo[x + side][y + side] + topo[x][y + side]) * 0.25;

			topo[x + half][y + half] = avg + rnd () * scale;

		}
	}


	// Square step
	private void square (int x, int y, int side, double scale) {
		int half = side / 2;
		double avg = 0.0, sum = 0.0;

		// computes middle point in a diamond accounting for seamless wrapping

		// West side
		if (x-half >= 0) 	{ avg += topo[x-half][y]; sum += 1.0; }
		else 				{ avg += topo[x-half + divisions][y]; sum += 1.0; }

		// South corner
		if (y-half >= 0) 	{ avg += topo[x][y-half]; sum += 1.0; }
		else 				{ avg += topo[x][y-half+divisions]; sum += 1.0; }

		// East corner
		if (x + half <= divisions)	{ avg += topo[x + half][y]; sum += 1.0; }
		else						{ avg += topo[x + half - divisions][y]; sum += 1.0; }

		// North corner
		if (y + half <= divisions)	{ avg += topo[x][y + half]; sum += 1.0; }
		else						{ avg += topo[x][y + half - divisions]; sum +=1.0; }


		// this will make the edges the same 
		if (x == 0) 					{ 	topo[x][y] = avg/sum + rnd () * scale;
		topo[divisions][y] = topo[x][y];}

		else if (y == 0) 				{ 	topo[x][y] = avg/sum + rnd () * scale;
		topo[x][divisions] = topo[x][y];}

		else 							{	topo[x][y] = avg/sum + rnd () * scale;}
	}


	private double rnd () {
		return 2. * rng.nextDouble () - 1.0; // returns a random number from -1 to 1 
	}


	public int[][] rescale(int[][] cat){
		int[][] catAdjust = new int[gridXDim][gridYDim]; 
		int indexI, indexJ;
		for (int i = 0; i < gridXDim; i++) {
			for (int j = 0; j < gridYDim; j++) {

				indexI= (int) Math.round( ((double) i / (double) gridXDim) * divisions); 
				indexJ = (int) Math.round( ((double) j / (double) gridYDim) * divisions);

				catAdjust[i][j] = cat[indexI][indexJ]; 
			}
		}

		return catAdjust; 
	}

	
	
	public void resample(){
		//this will resample the terrain to make it a specific gridWidth/gridHeight
		//e.g., via interpolation
	}



	public int[][] getCategories(double[] props){

		int[] propsCutOff = new int[props.length]; 
		//float upProportion = 0.65f, bottomProportion = 0.10f, vertProportion = 0.25f; // this is proportion of each Orientation to use

		//int scaleFactor = 1; // this is for the quick and easy AWT display; the scaleFactor is the magnification factor so can see the pixels

		double topo[][] = getTerrain(); 
		double topoAdjust[][] = new double[divisions][divisions]; 
		int topoInt[][] = new int[divisions][divisions]; //integer array which will be where processing takes place
		int cat[][] = new int[divisions][divisions]; // here, 0 = bottom; 1 = vert; 2 = up facing orientions

		// this will use Wiegand method in HabitatBuilder to return the categories of the topo array
		// have it so pass in a topo array, calculate the

		int counterArray[] = new int[500];  //an array which will count the total number of each interger height measurement in the landscape
		double minimum = 100000;
		double maximum = -100;
		double heightScale;
		double sum = 0;  // tracks the sum of the height values on the entire grid; i.e., counts the number of pixels at a particular height from 0-499
		//int upCutOff = 0, bottomCutOff = 0, vertCutOff = counterArray.length;  // stores the cutoff height value based on the actual heights on the landscape and the proportion of each orientation




		// sets the counter array to zero
		for (int i = 0; i<counterArray.length; i++) {
			counterArray[i] = 0;
		}

		// finds the minimum value in the height measurements
		for (int i = 0; i < divisions; i++) {
			for (int j = 0; j < divisions; j++) {
				if (topo[i][j] < minimum) { minimum = topo[i][j]; }; 
				//	        		System.out.println(i + "\t" + j + "\t" + topo[i][j]);

			}
		}

		// shifts the range of the topo array to the mininum value being zero
		for (int i = 0; i < divisions; i++) {
			for (int j = 0; j < divisions; j++) {
				topoAdjust[i][j] = (topo[i][j]-minimum); 
				//	        		System.out.println(i + "\t" + j + "\t" + topoInt[i][j]);
			}
		}

		// finds the maximum value in the height measurements
		for (int i = 0; i < divisions; i++) {
			for (int j = 0; j < divisions; j++) {
				if (topoAdjust[i][j] > maximum) { maximum = topoAdjust[i][j]; }; 
			}
		}

		// re-scales the range of the array between 0 and 499
		for (int i = 0; i < divisions; i++) {
			for (int j = 0; j < divisions; j++) {
				heightScale = (topoAdjust[i][j])/maximum; 
				topoInt[i][j] = (int) (499*heightScale);
				counterArray[topoInt[i][j]] = counterArray[topoInt[i][j]] + 1;
			}
		}

		// TEST of counter array      
		/*    	for (int i = 0; i<counterArray.length; i++) {
	    		System.out.println("Counter Array Value" + "\t" + i + "\t" + counterArray[i]);
	    	}
		 */

		// sets the cut-off height value for the different orientations
		for (int i = 0; i<counterArray.length; i++) {
			sum  += counterArray[i];
			double summer =0; 
			for (int j=0; j<propsCutOff.length; j++){
				summer += props[j]; 
				if (sum/((float)(divisions*divisions)) <= summer) {propsCutOff[j] = i;}
			}
			//if (sum/((float)(divisions*divisions)) <= bottomProportion) {bottomCutOff = i;}
			//if (sum/((float)(divisions*divisions)) <= vertProportion+bottomProportion) {vertCutOff = i;}
			//if (sum/((float)(divisions*divisions)) <= upProportion+vertProportion+bottomProportion) {upCutOff = i;}
		}



		// sets the orientation array to either 0, 1, or 2 (bottom, vert, or up facing orientation, respectively)
		boolean check = true; 
		for (int i = 0; i < divisions; i++) {
			for (int j = 0; j < divisions; j++) {

				for (int k=0; k<propsCutOff.length; k++){
					if (check && (topoInt[i][j] <= propsCutOff[k]) ) { 
						cat[i][j] = k;
						check = false; 
						}
				}
				check = true; 
				//if (topoInt[i][j] <= propsCutOff[0]) { cat[i][j] = 0; }
				//else if (topoInt[i][j] <= propsCutOff[0]) { cat[i][j] = 1; }
				//else if (topoInt[i][j] <= upCutOff) { cat[i][j] = 2; }
			}
		}

		return rescale(cat); 
	}

	

}
