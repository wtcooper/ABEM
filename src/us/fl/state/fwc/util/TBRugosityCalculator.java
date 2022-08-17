package us.fl.state.fwc.util;

import java.io.IOException;
import java.util.ArrayList;

import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

public class TBRugosityCalculator {

	final int xdim = 5272;
	final int ydim = 7941;
	float[][] rugosity = new float[ydim][xdim];
    float [][] depth = new float[ydim][xdim];
    double[] xcoord = new double[xdim]; 
    double[] ycoord = new double[ydim]; 
    

    final float FT__M_CONVERSION = 0.3048f; 

    int x, y; 
	double height; 
	double area;

	boolean check; 
    
	NetcdfFile dataFileInput1;



	public void getBathymetry() {
		
        // Open the file and check to make sure it's valid.
        String filename = "data/TBBath10x10.nc";
        NetcdfFile dataFile = null;

        try {
            dataFile = NetcdfFile.open(filename, null);

            Variable depthVar= dataFile.findVariable("depth");
            Variable xVar = dataFile.findVariable("x");
            Variable yVar = dataFile.findVariable("y"); 
            ArrayDouble.D1 xArray = (ArrayDouble.D1) xVar.read(); 
            ArrayDouble.D1 yArray = (ArrayDouble.D1) yVar.read(); 
            
            for (int i=0; i<ydim; i++) {
            	ycoord[i] = yArray.get(i); 
            }
            for (int i=0; i<xdim; i++) {
            	xcoord[i] = xArray.get(i); 
            }
            
            ArrayFloat.D2 depthArray = (ArrayFloat.D2)depthVar.read();

            for (int i=0; i<ydim; i++) {
              for (int j=0; j<xdim; j++) {
            	  
            	  if ( (depthArray.get(i,j) >-0) || (depthArray.get(i,j) < -1000 ) ){
            		  depth[i][j] =-999; 
            	  }
            	  else depth[i][j] = depthArray.get(i,j);
                  }
              }

            System.out.println("data input complete"); 
            
        } catch (java.io.IOException e) {
              System.out.println(" fail = "+e);
              e.printStackTrace();
        } finally {
           if (dataFile != null)
           try {
             dataFile.close();
           } catch (IOException ioe) {
             ioe.printStackTrace();
           }
        }
}


	
	
	public void calculate() {

	    for (int i = 0; i < ydim; i++) {
	    	for (int j = 0; j < xdim; j++) {
	    		rugosity[i][j] = -999;
	    		}
	    	} 
		
		double x1,x2,x3,y1,y2,y3,z1,z2,z3, dist1, dist2, dist3, s; 

		// here, don't do every cell -- only those that aren't on edges, or if there's a missing cell in the 3x3cell range 
		for (int i = 1; i < ydim-1; i++) {
	    	for (int j = 1; j < xdim-1; j++) {

	    		check = true; 
	    		for (int k = 0; k < 3; k++){
	    			for (int l = 0; l < 3; l++){
		    			if ( (depth[i-1+k][j-1+l] > -1000)  && (depth[i-1+k][j-1+l] < -998) ) {check = false;}
	    			}
	    		}
	    		
	    		if (check == true){
	    			area = 0;

	    			// for first triangle section
	    			x1=i-10; y1=j-10; z1 = depth[i-1][j-1];
	    			x2=i-10; y2=j; z2 = depth[i-1][j];
	    			x3=i; y3=j-10; z3 = depth[i][j-1]; 
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			s = (dist1+dist2+dist3)/2; 
	    			area += Math.sqrt(s*(s-dist1)*(s-dist2)*(s-dist3)); 
	    			//area = area + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;

	    			// for 2nd triangle section
	    			x1=i-10; y1=j; z1 = depth[i-1][j];
	    			x2=i; y2=j-10; z2 = depth[i][j-1];
	    			x3=i; y3=j; z3 = depth[i][j]; 
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			s = (dist1+dist2+dist3)/2; 
	    			area += Math.sqrt(s*(s-dist1)*(s-dist2)*(s-dist3)); 
	    			//area = area + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;

	    			// for 3rd triangle section
	    			x1=i-10; y1=j+10; z1 = depth[i-1][j+1];
	    			x2=i-10; y2=j; z2 = depth[i-1][j];
	    			x3=i; y3=j; z3 = depth[i][j]; 
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			s = (dist1+dist2+dist3)/2; 
	    			area += Math.sqrt(s*(s-dist1)*(s-dist2)*(s-dist3)); 
	    			//area = area + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;

	    			// for 4th triangle section
	    			x1=i-10; y1=j+10; z1 = depth[i-1][j+1];
	    			x2=i; y2=j+10; z2 = depth[i][j+1];
	    			x3=i; y3=j; z3 = depth[i][j]; 
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			s = (dist1+dist2+dist3)/2; 
	    			area += Math.sqrt(s*(s-dist1)*(s-dist2)*(s-dist3)); 
	    			//area = area + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;

	    			// for 5th triangle section
	    			x1=i+10; y1=j-10; z1 = depth[i+1][j-1];
	    			x2=i; y2=j-10; z2 = depth[i][j-1];
	    			x3=i; y3=j; z3 = depth[i][j]; 
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			s = (dist1+dist2+dist3)/2; 
	    			area += Math.sqrt(s*(s-dist1)*(s-dist2)*(s-dist3)); 
	    			//area = area + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;

	    			// for 6th triangle section
	    			x1=i+10; y1=j-10; z1 = depth[i+1][j-1];
	    			x2=i+10; y2=j; z2 = depth[i+1][j];
	    			x3=i; y3=j; z3 = depth[i][j]; 
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			s = (dist1+dist2+dist3)/2; 
	    			area += Math.sqrt(s*(s-dist1)*(s-dist2)*(s-dist3)); 
	    			//area = area + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;

	    			// for 7th triangle section
	    			x1=i; y1=j+10; z1 = depth[i][j+1];
	    			x2=i+10; y2=j; z2 = depth[i+1][j];
	    			x3=i; y3=j; z3 = depth[i][j]; 
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			s = (dist1+dist2+dist3)/2; 
	    			area += Math.sqrt(s*(s-dist1)*(s-dist2)*(s-dist3)); 
	    			//area = area + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;

	    			// for 8th triangle section
	    			x1=i; y1=j+10; z1 = depth[i][j+1];
	    			x2=i+10; y2=j; z2 = depth[i+1][j];
	    			x3=i+10; y3=j+10; z3 = depth[i+1][j+1]; 
	    			dist1 = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)+(z2-z1)*(z2-z1));
	    			dist2 = Math.sqrt((x3-x1)*(x3-x1)+(y3-y1)*(y3-y1)+(z3-z1)*(z3-z1));
	    			dist3 = Math.sqrt((x3-x2)*(x3-x2)+(y3-y2)*(y3-y2)+(z3-z2)*(z3-z2));
	    			s = (dist1+dist2+dist3)/2; 
	    			area += Math.sqrt(s*(s-dist1)*(s-dist2)*(s-dist3)); 
	    			//area = area + (Math.sqrt((dist1+dist2+dist3)*(dist1+dist2-dist3)*(dist2+dist3-dist1)*(dist3+dist1-dist2)))/4;
	    			
	    			rugosity[i][j] = (float) area/400;
	    		}
	    	}
            //System.out.println("rugosity calculations row -- " + i + " -- complete"); 

		}
		
        System.out.println("rugosity calculations complete"); 

	}
	


	public void writeOutput(){

		final int xdim = 5272;
		final int ydim = 7941;

		String filename = "data/TBRugosity.nc";
		NetcdfFileWriteable dataFileOutput = null;

		try {
			dataFileOutput = NetcdfFileWriteable.createNew(filename, false);

			Dimension xDim = dataFileOutput.addDimension("x", xdim );
			Dimension yDim = dataFileOutput.addDimension("y", ydim);

			ArrayList<Dimension> dims =  new ArrayList<Dimension>();

			dataFileOutput.addVariable("x", DataType.DOUBLE, new Dimension[] {xDim});
			dataFileOutput.addVariable("y", DataType.DOUBLE, new Dimension[] {yDim});

			dataFileOutput.addVariableAttribute("x", "units", "meters_east");
			dataFileOutput.addVariableAttribute("y", "units", "meters_north");

			dims.add(yDim);
			dims.add(xDim);

			dataFileOutput.addVariable("rugosity", DataType.FLOAT, dims);

			dataFileOutput.addVariableAttribute("rugosity", "units", "m");

			dataFileOutput.create();

			ArrayDouble.D1 dataX = new ArrayDouble.D1(xDim.getLength());
			ArrayDouble.D1 dataY = new ArrayDouble.D1(yDim.getLength());

			for (int i=0; i<xDim.getLength(); i++) {
				if (i>0 && i<xdim-1){
					if ((xcoord[i+1]-xcoord[i] != 10) ) System.out.println("x element -- " + i + " -- not 10 meters apart from i+1"); 
				}
				//dataX.set(i,  xcoord[i] );
				dataX.set(i,  i*10+313300 );
			}
			for (int i=0; i<yDim.getLength(); i++) {
				if (i>0 && i<ydim-1) {
					if ((ycoord[i]-ycoord[i+1] != 10) ) System.out.println("y element -- " + i + " -- not 10 meters apart from i+1"); 
				}
				//dataY.set(i,  ycoord[i] );
				dataY.set(i,  3113157-i*10 );
			}

			dataFileOutput.write("x", dataX);
			dataFileOutput.write("y", dataY);

			ArrayFloat.D2 dataRugosity = new ArrayFloat.D2(yDim.getLength(), xDim.getLength()); 

			for (int i=0; i<ydim; i++) {
				for (int j=0; j<xdim; j++) {
					dataRugosity.set(i, j,  rugosity[i][j]);
				}
			}
			dataFileOutput.write("rugosity", dataRugosity); 


		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidRangeException e) {
			e.printStackTrace();
		} finally {
			if (null != dataFileOutput)
				try {
					dataFileOutput.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
		}
		System.out.println( "*** SUCCESS writing TB rugosity!" );
	}


	public static void main(String[] args) {

		TBRugosityCalculator rc = new TBRugosityCalculator();
		rc.getBathymetry();
		rc.calculate();
		rc.writeOutput(); 
		
	}

}
