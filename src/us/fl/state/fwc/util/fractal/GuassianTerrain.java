package us.fl.state.fwc.util.fractal;

import java.util.Random;

public class GuassianTerrain {

	public int gridHeight;	//for Diamond-Square topo method, needs to be a grid size of (2^n)+1; 
	public int gridWidth;

	//landscape function variables
    public float sigma; 
    public float vol;

 
//  public float sigmaArray[] = {1, 1, 1, 5, 5, 5, 75, 75, 75};  ** original formulation
    public float sigmaArray[] = {1, 1, 1, 5, 5, 5, 75, 75, 75};
    public float volArray[] = {3, 1, 0.4f, 3, 1, 0.4f, 3, 1, 0.4f};
    public float gaussNums[] = {0,0, 50, 0, 0, 0, 0, 0, 0};     	// Array Order: SharpTall, SharpIntermediate, SharpLow, InterTall, InterInter, InterLow, WideTall, WideInter, WideLow

    public double topo[][]; 

    public GuassianTerrain (int lod, double[][] topo) {
		  
		    this.topo = topo;
			this.gridWidth = 1 << lod;
			this.gridHeight = 1 << lod;

    }
   
	public double[][] computeTerrain() {
	    	
	    	// if want to replicate a run, will need to export the "seed" value so can start the same random string
	    	long seed = System.currentTimeMillis();
	    	Random rand = new Random(seed);
	    	
	    	
	    	/*// this is commented out so these guassian functions will add onto fractal landscape; 
	    	 * end of for loop to set topo array to zero
	        for (int i = 0; i < gridWidth; i++) {
	        	for (int j = 0; j < gridHeight; j++) {
	        		topo[i][j] = 0; 
	        		}
	        	} 
				*/
	    	
	        for (int i = 0; i < gaussNums.length; i++) {
	        	for (int j = 0; j < gaussNums[i]; j++) {
	        		if (sigmaArray[i] == 1) {sigma = sigmaArray[i] + rand.nextInt(2) ; }
	        		else if (sigmaArray[i] == 5) {sigma = sigmaArray[i]  + rand.nextInt(10) ; }
	        		else if (sigmaArray[i] == 75) {sigma = sigmaArray[i]  + rand.nextInt(75) ; }
	 
	        		vol = volArray[i]*sigma;
	 	
	        		int x = rand.nextInt(gridWidth);
	        		int y = rand.nextInt(gridHeight);

	        		
	        		// sets a dimension around the (x,y) location for which to put the gaussian values 
	        		int dimension = (int)(0.5+3*sigma);
	        
	        		// sets the limit of the dimension
	        		if (dimension > gridWidth) {
	        			dimension = gridWidth; 
	        		}

	        		// loop to set value of topo grid for a particular dimension (k,l) around the random x and y location
	        		for (int k = (0-dimension); k < dimension+1; k++) {
	        			for (int l = (0-dimension); l < dimension+1; l++) {
	        				int xloc = x+k;
	        				int yloc = y+l;
	        				double d = Math.sqrt((k*k)+(l*l));

	        				if (xloc < 0) { xloc = xloc + (gridWidth); }
	        				if (yloc < 0) { yloc = yloc + (gridHeight); }
	        				if (xloc > (gridWidth-1)) { xloc = xloc - (gridWidth); }
	        				if (yloc > (gridHeight-1)) { yloc = yloc - (gridHeight); }

	        			
	        				topo[xloc][yloc] = topo[xloc][yloc] + (double)((0.5+100*(vol/sigma)*Math.exp(-0.5*((d/sigma)*(d/sigma)))));
	        			
	        			} 
	        		} // end of loop  to set value of topo grid

	        	}
	        }// end of loop to go through all the gaussian function types and for each number of functions needed for each type

    
	    	return topo; 
	
	}// end of buildLandscape
	


	
}
