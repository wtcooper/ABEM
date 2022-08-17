package us.fl.state.fwc.abem.hydro.efdc;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import ucar.ma2.Array;
import ucar.ma2.ArrayFloat;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import us.fl.state.fwc.util.Int3D;
import us.fl.state.fwc.util.geo.Shapefile;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Output's a dxdy.inp file, using netCDF data on bathymetry and 
 * @author wade.cooper
 *
 */
public class DepthAndRoughnessFormatter {

	String directory = "C:\\work\\workspace\\EFDC\\TampaToSarasota_WGS\\";

	double meanTide = 0.354; 
	double minDepth = 1; //set equal to 1 meter, for those boundary cells where gets shallow -- if goes below this, then will throw floating point error if not using wet/dry cells
	//double dx=500d, dy = 500d; 


	//private static final int seed = (int) System.currentTimeMillis();
	//private static MersenneTwister m= new MersenneTwister(seed); 

	//DecimalFormat twoDForm = new DecimalFormat("#.##");

	private HashMap <Int3D, EFDCCell> gridCells; //  = new FastMap <PointLoc, EFDCCell>(); 
	EFDCGrid grid; 

	int xOffset = 313295;
	int yOffset	= 3113157;
	double cellSize=10; 

	private  PrintWriter outFile = null; 


	public void initialize(){
		grid = new EFDCGrid();
		grid.initialize(directory + "corners.inp"); // set up the grid
		grid.setCellSizeAndDepth(directory + "dxdy.inp", 8);
		gridCells =grid.getGridCells(); 
	}

	public void step(){

		//new File(directory + "dxdy.inp").delete(); 
		
		try { outFile = new PrintWriter(new FileWriter(directory + "dxdyTemp.inp", true));
		} catch (IOException e) {e.printStackTrace();}

		outFile.println("C DXDY.INP FILE, IN FREE FORMAT ACROSS COLUMNS for  xxxx Active Cells"); 
		outFile.println("C xxxxx  xxxxx AM"); 
		outFile.println("C                                           BOTTOM                 Veg");
		outFile.println("C  I    J        DX        DY      DEPTH     ELEV     ZROUGH      TYPE");

		Shapefile shp = new Shapefile("data/2006UTM_SG_Clip.shp", false);
		shp.openShapefile(); 
		String bathData = "data/USGSBathUTMClip.nc"; //TBBath10x10.nc"; 
		NetcdfFile bathDataFile = null;


		try {
			bathDataFile = NetcdfFile.open(bathData, null);
			Variable depthVar= bathDataFile.findVariable("depth");
			ArrayFloat.D2 depthArray = (ArrayFloat.D2)depthVar.read();
			Variable xBath = bathDataFile.findVariable("x");
			Variable yBath = bathDataFile.findVariable("y");

			Set<Int3D> set = gridCells.keySet();

			Iterator<Int3D> it = set.iterator();

			int counter2=0; 
			while (it.hasNext()) {

				int xBathIndex, yBathIndex; 
				Int3D index = it.next();

				//Geometry cellGeom = gridCells.get(index).getGeom(); 
				//Envelope bb = cellGeom.getEnvelopeInternal(); 

				List<Coordinate> randomPoints = grid.getRandomPointsWithinCell(500, index); 

				double bathSum = 0; 
				double savSum = 0; 
				int bathCounter = 0; 
				int savCounter = 0; 

				for (int i =0; i<randomPoints.size(); i++){
					Coordinate point = randomPoints.get(i); 
					xBathIndex = locate(xBath,point.x	);
					yBathIndex = locate(yBath, point.y);

					double depth = depthArray.get(yBathIndex, xBathIndex); 
					if ( (depth > 0)){ // if not on land
						//check = true; 
						bathSum  += depth; //depthArray.get(i,j); 
						bathCounter++; 

						Integer hab = (Integer) shp.getFeatureAttribute(point, "FLUCCSCODE");
						if (!(hab == null)){
							int habitat = hab.intValue();  
							if (habitat == 9113) savSum += 0.5; // for patchy seagrass, consider as 50% seagrass cover
							else if (habitat == 9116) savSum+= 0.875; //for continous seagrass, consider as 87.5% cover
						}
						savCounter++;  
					}
				}


				double averageBath = -(bathSum /(double) bathCounter);// -meanTide);  //make negative
				Double avgBath = new Double(averageBath); 

				double savCover = savSum / (double) savCounter; //cover value between 0-1
				Double savCov = new Double(savCover); 



				if (averageBath>-minDepth || avgBath.isNaN() )averageBath= -minDepth; 
				if (savCov.isNaN()) savCover = 0; 

				double depth = -averageBath ; //substract the mean tide depth so that is reflective of mean tide levels and not the MLLH

				double roughness = 0.005 + (0.03-0.005)*savCover; // here, this will calculate a roughness value between 0.005 (fine sand, based on Shi et al. 2003) and 0.03 (100% seagrass cover; this is low value based on literature where z0 measured for seasgrass) 

				double dx = gridCells.get(index).getDx();
				double dy = gridCells.get(index).getDy();

				//outFile.println(index.x + "\t" + index.y + "\t" + dx + "\t" + dy + "\t" + Float.valueOf(twoDForm.format(depth)) + "\t" + Float.valueOf(twoDForm.format(averageBath)) + "\t" + Float.valueOf(twoDForm.format(roughness))); //roughness value from Shi et al. 2003
				outFile.println(index.x + "\t" + index.y + "\t" + dx + "\t" + dy + "\t" + depth + "\t" + averageBath + "\t" + roughness); //roughness value from Shi et al. 2003

				counter2++; 
				System.out.println("total cells processed: " + counter2); 
			} // end of while(it.hasNext()) loop


		} catch (java.io.IOException e) {
			System.out.println(" fail = "+e);
			e.printStackTrace();
		} finally {
			if (bathDataFile != null)
				try {
					bathDataFile.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
		}
		outFile.close();

	}


	private int locate(Variable var, double val) throws IOException {

		Array arr;
		double[] ja;
		int idx;

		/*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		 * TODO Consider creating a map of Variables to arrays.  If
		 * the mapping exists, pull the array rather than reading and
		 * copying.
		 !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/

		// Read the Variable into an Array

		arr = var.read();

		// Convert into a java array
		boolean isBackwards = false; 
		int aSize = 0; 
		// NOTE: CHANGE from Jkool's so can deal with backwards-sorted arrays
		if (arr.getElementType() == Float.TYPE) {
			float[] fa = (float[]) arr.copyTo1DJavaArray();
			int counter = fa.length-1; 
			aSize = counter; 
			ja = new double[fa.length];
			for (int i = 0; i < ja.length; i++) {
				if (fa[0] < fa[1]) ja[i] = fa[i]; // if is alrady sorted, just set to same  
				else {
					ja[counter--] = fa[i]; // if is sorted backwards (e..g, when ArcGIS exports netCDF files, then set backwards
					isBackwards = true; 
				}
			}
		} else {
			double[] fa = (double[]) arr.copyTo1DJavaArray();
			int counter = fa.length-1; 
			aSize = counter; 
			ja = new double[fa.length];
			for (int i = 0; i < ja.length; i++) {
				if (fa[0] < fa[1]) ja[i] = fa[i]; // if is alrady sorted, just set to same  
				else {
					ja[counter--] = fa[i]; // if is sorted backwards (e..g, when ArcGIS exports netCDF files), then set backwards
					isBackwards = true; 
				}
			}
		}

		// Use binary search to look for the value.

		idx = Arrays.binarySearch(ja, val);

		if (idx < 0) {

			// Error check

			if (idx == -1) {
				//throw new IllegalArgumentException(var.getName() + " value "
				//+ val + " does not fall in the range " + ja[0] + " : "
				//+ ja[ja.length - 1] + ".");
				return -1;
			}

			// If not an exact match - determine which value we're closer to

			// Temporary measure to prevent an ArrayOutOfBoundsException. Tidy
			// this up...

			if (-(idx + 2) >= ja.length) {
				return 0;
			}

			double spval = (ja[-(idx + 2)] + ja[-(idx + 1)]) / 2d;

			if (val < spval) {
				if (isBackwards) return aSize - (-(idx + 2));  
				else return -(idx + 2);

			} else {
				if (isBackwards) return aSize- (-(idx+1)); 
				else return -(idx + 1);
			}
		}

		// Otherwise it's an exact match.
		if (isBackwards) return idx; 
		else return aSize- idx;
	}



	public static void main(String[] args) {

		DepthAndRoughnessFormatter frc = new DepthAndRoughnessFormatter();
		frc.initialize();
		frc.step(); 
	}
}
