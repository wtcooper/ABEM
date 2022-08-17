package us.fl.state.fwc.util.fractal;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.swing.JFrame;

import us.fl.state.fwc.util.geo.DrawRaster;



public class HabitatBuilder /*extends SimState*/ {

	public int gridHeight=512;	//for Diamond-Square topo method, needs to be a grid size of (2^n)+1; 
	public int gridWidth=512;
	
				//	lod		gridwidth/height
				//	6			64
				//	7			128
				//	8			256
				//	9			512
				//	10			1024
	
	public int lod = 9;  		// here,  gridHeight/gridWidth = (2^lod)
	public double roughness = 1.3; // this is NOT value of H; here, is what Merlin recommends where can be from 0-infinity.  As increase, gets more random (i.e., opposite of H) 
	public float upProportion = 0.65f, bottomProportion = 0.10f, vertProportion = 0.25f; // this is proportion of each Orientation to use

	public int scaleFactor = 1; // this is for the quick and easy AWT display; the scaleFactor is the magnification factor so can see the pixels


    public double topo[][] = new double[gridWidth][gridHeight]; 
    public double topoAdjust[][] = new double[gridWidth][gridHeight]; 
    public int topoInt[][] = new int[gridWidth][gridHeight]; //integer array which will be where processing takes place
    public int orientation[][] = new int[gridWidth][gridHeight]; // here, 0 = bottom; 1 = vert; 2 = up facing orientions
    
	private PrintWriter outFile=null;

	public HabitatBuilder() {}
	
	
	
    // ***********************
    // This method normalizes the topography array and classifies the topographies into top, bottom, and vertical based on a proportion
    //  ***********************

    public void buildOrientation() {
    	
    	int counterArray[] = new int[500];  //an array which will count the total number of each interger height measurement in the landscape
    	double minimum = 100000;
    	double maximum = -100;
    	double heightScale;
    	double sum = 0;  // tracks the sum of the height values on the entire grid; i.e., counts the number of pixels at a particular height from 0-499
    	int upCutOff = 0, bottomCutOff = 0, vertCutOff = counterArray.length;  // stores the cutoff height value based on the actual heights on the landscape and the proportion of each orientation
    	
    	
    	// call to the terrain building classes to return an array of doubles with the topography numbers
		FractalBuilder ft = new FractalBuilder(gridWidth, gridHeight, roughness, System.currentTimeMillis());
		topo = ft.getTerrain(); 

//    	GuassianTerrain gt = new GuassianTerrain(); 
//    	topo = gt.computeTerrain(); 
 
    	
 
    	
    	// sets the counter array to zero
    	for (int i = 0; i<counterArray.length; i++) {
    		counterArray[i] = 0;
    	}
    	
    	// finds the minimum value in the height measurements
        for (int i = 0; i < gridWidth; i++) {
        	for (int j = 0; j < gridHeight; j++) {
        		if (topo[i][j] < minimum) { minimum = topo[i][j]; }; 
//        		System.out.println(i + "\t" + j + "\t" + topo[i][j]);

        	}
        }

        // shifts the range of the topo array to the mininum value being zero
        for (int i = 0; i < gridWidth; i++) {
        	for (int j = 0; j < gridHeight; j++) {
        		topoAdjust[i][j] = (topo[i][j]-minimum); 
//        		System.out.println(i + "\t" + j + "\t" + topoInt[i][j]);
        	}
        }

        // finds the maximum value in the height measurements
        for (int i = 0; i < gridWidth; i++) {
        	for (int j = 0; j < gridHeight; j++) {
        		if (topoAdjust[i][j] > maximum) { maximum = topoAdjust[i][j]; }; 
        	}
        }

        // re-scales the range of the array between 0 and 499
        for (int i = 0; i < gridWidth; i++) {
        	for (int j = 0; j < gridHeight; j++) {
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
    		sum = (float) sum + counterArray[i];
//    		System.out.println("Sum Value" + "\t" + i + "\t" + sum);
    		if (sum/((float)(gridWidth*gridHeight)) <= bottomProportion) {bottomCutOff = i;}
    		if (sum/((float)(gridWidth*gridHeight)) <= vertProportion+bottomProportion) {vertCutOff = i;}
    		if (sum/((float)(gridWidth*gridHeight)) <= upProportion+vertProportion+bottomProportion) {upCutOff = i;}
    	}

    	
    	
        // sets the orientation array to either 0, 1, or 2 (bottom, vert, or up facing orientation, respectively)
        for (int i = 0; i < gridWidth; i++) {
        	for (int j = 0; j < gridHeight; j++) {
        		if (topoInt[i][j] <= bottomCutOff) { orientation[i][j] = 0; }
        		else if (topoInt[i][j] <= vertCutOff) { orientation[i][j] = 1; }
        		else if (topoInt[i][j] <= upCutOff) { orientation[i][j] = 2; }
        	}
        }
 
    } // end of method classifyOrientation
    
    
    // this method constructs a DrawHabitat object to make a new 
    public void drawHabitat() {
    
    	DrawRaster drawer = new DrawRaster(orientation, scaleFactor, null);
    	drawer.setVisible(true);
    	drawer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
    
	
	// create new class called "write2DArray", set up so pass it the array and the name of the file
	// Everytime need to write a new Array, simply send it the new data
	public void writeTopo() {

        // open the output file so that it appends if it already exists; will need to make so can do multiple files with this
		// ****NEED to create a string function which concatenates a descriptor number (describing landcape "type" to end of data file
		// then, when actually make landscapes, can build a lot of them and in main method, make a loop to go through the different methods
		// many times for different landscape types
		
		try {
        	outFile = new PrintWriter(new FileWriter("Topography.dat", true));
        } catch (IOException e) {
        	// TODO Auto-generated catch block
        	e.printStackTrace();
        }

        for (int i = 0; i < gridWidth; i++) {
        	for (int j = 0; j < gridHeight; j++) {
        		outFile.println(i + "\t" + j + "\t" + topo[i][j]); 		// stops writing to the file for some reason near end
        		}
        	} 
        outFile.close();
	}	

	public void writeTopoInt() {

        // open the output file so that it appends if it already exists; will need to make so can do multiple files with this
		// ****NEED to create a string function which concatenates a descriptor number (describing landcape "type" to end of data file
		// then, when actually make landscapes, can build a lot of them and in main method, make a loop to go through the different methods
		// many times for different landscape types
		
		try {
        	outFile = new PrintWriter(new FileWriter("Topography.dat", true));
        } catch (IOException e) {
        	// TODO Auto-generated catch block
        	e.printStackTrace();
        }

        for (int i = 0; i < gridWidth; i++) {
        	for (int j = 0; j < gridHeight; j++) {
        		outFile.println(i + "\t" + j + "\t" + topoInt[i][j]); 		
        		}
        	} 
        outFile.close();
	}	

	
	public void writeOrientation() {

		try {
        	outFile = new PrintWriter(new FileWriter("Orientation.dat", true));
        } catch (IOException e) {
        	// TODO Auto-generated catch block
        	e.printStackTrace();
        }

        for (int i = 0; i < gridWidth; i++) {
        	for (int j = 0; j < gridHeight; j++) {
        		outFile.println(i + "\t" + j + "\t" + orientation[i][j]);
        		}
        	} 
        outFile.close();
	}	

	
    public static void main(String[] args)
    {

    	// could make a loop to make a new builder object for each "type" of landscape I want

   	
    	HabitatBuilder builder = new HabitatBuilder();

    	
    	builder.buildOrientation();  // this method call builds the fractal terrain and gets the orientation from it
    	
//    	builder.writeTopo();
//    	builder.writeTopoInt();
//    	builder.writeOrientation();
    	builder.drawHabitat(); 
    	
    	
    //doLoop(HabitatBuilder.class, args);
    }    
}


