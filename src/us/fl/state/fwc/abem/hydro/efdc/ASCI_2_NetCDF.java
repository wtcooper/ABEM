package us.fl.state.fwc.abem.hydro.efdc;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TimeZone;

import org.apache.commons.math.geometry.Vector3D;

import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import us.fl.state.fwc.util.Int3D;
import us.fl.state.fwc.util.TimeUtils;
import us.fl.state.fwc.util.geo.CoordinateUtils;
import us.fl.state.fwc.util.geo.NetCDFFile;

import com.vividsolutions.jts.geom.Coordinate;


/**
 * Outputs EFDC ASCII data to a netCDF file.
 * Current implementation outputs U, V, and salinity.  Note: also includes 
 * temperature in the netCDF, but this is interpolated from the NCOM data
 * since the EFDC model does not include temperature.  
 * @author wade.cooper
 *
 */
public class ASCI_2_NetCDF {

	//!!!!!!!!!!!!!!!! PARAMETERS TO SET --- MAKE SURE IS CORRECT !!!!!!!!!!!!!!!!
	String directory = "C:\\work\\workspace\\EFDC\\TampaToSarasota_WGS\\";
	String modelName = "ncFiles\\withSalinity\\TB2SB_WGS_"; 
	String timeStamp = "070110-071010_test";
	boolean scaleData = true;
	boolean addOffset = true; 
	boolean outputSalinity = true;
	boolean outputTemp = false; 
	
	float offset = 20.0f;
	//max m/s velocity at 0.0001 scale factor is +/-3.2767 -- shouldn't be above this
	float velScaleFactor = 0.0001f; 
	float otherScaleFactor = 0.001f; //scale factor for salinity and temp
	int maxTime = 11000;
	//this isn't used -- just a dummy variable for the setDimension() method set to unlimited
	int timeSize = 0; 
	int depthSize = 8; 
	int latSize = 196; 
	int lonSize = 136;
	//padding is number of cells that are edge cells.  
	//E.g., L=2 (first cell) is usually j=3, which would be padding=2.  
	//However, sometimes is j=4, which is padding=3
	int latPadding = 3; 
	int lonPadding = 2; 
	double gridXDim = 0.004;
	double gridYDim = 0.004; 

	NetCDFFile NCOMFile;
	String dateURL;
	String baseFile = "c:\\work\\data\\NCOM_AS\\";
	String nameURL = "ncom_relo_amseas_";
	NumberFormat nf = NumberFormat.getInstance(); 
	
	Coordinate originNode = new Coordinate(0, 0, 0);
	String cornersInFile = directory + "corners.inp"; 
	String dxdyInFile = directory + "dxdy.inp"; 
	private HashMap <Int3D, EFDCCell> gridCells; //  = new FastMap <PointLoc, EFDCCell>(); 
	private HashMap<Integer, Int3D> gridIndexes; 
	File uInFile= new File(directory + "UUUDMPF.ASC");  
	File vInFile= new File(directory + "VVVDMPF.ASC");  
	File salInFile = null;// new File(directory + "SALDMPF.ASC");  
	//String outFile = directory + modelName + "_" + timeStamp+ ".nc"; 	
	String uVarName = "water_u"; 
	String vVarName = "water_v"; 
	String timeVarName = "time"	; 
	String lonVarName = "lon"; 
	String latVarName = "lat"; 
	String depthVarName = "depth"; 
	String bathVarName = "water_depth";
	String salVarName = "salinity";
	String tempVarName = "water_temp"; 
	BufferedReader uReader = null; 
	BufferedReader vReader = null; 
	BufferedReader salReader = null; 

	//rotate variables only is a rotated projection of the model with respect the netCDF projection wanting to make
	boolean rotate = false; // |||||||||||||||||||| rotate() method NOT WORKING -- need to debug -- something ailing with Vector3D assignment
	final double rotationAngle = 12.60394;

	ArrayList<Int3D> boundCellList = new ArrayList<Int3D>(); 

	NetcdfFileWriteable ncFile; 
	
	Calendar currentDateAndTime; 	
	long NCOMTimeOffset = 946684800000l; 
	
	boolean start = true; 
	
	public static void main(String[] args) {
		ASCI_2_NetCDF a = new ASCI_2_NetCDF();
		try {
			a.writeFile();
			//a.writeBathymetry();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidRangeException e) {
			e.printStackTrace();
		}
	}
		
		
		
		
		

	
	
	public void writeFile() throws IOException, InvalidRangeException {


		if (outputSalinity) salInFile = new File(directory + "SALDMPF.ASC");

		//Create an array list of all the open boundary condition cells in EFDC model
		// 	- this will be used to get correct u-vel which is 1/2 of correct due to averaging in EFDC
		// - here, EFDC averages as: DMPVAL(L-1,K)=0.5*(U(L,K)+U(L+1,K)), presumably due 
		BufferedReader boundCellReader = new BufferedReader(new FileReader(new File(directory + "EFDCBoundCells.txt")));
		for (String line = boundCellReader.readLine(); line != null; line = boundCellReader.readLine()) {
			String tokens[] = line.split("\t"); //split("[ ]+"); 
			Int3D index = new Int3D();
			index.x = Integer.parseInt(tokens[0]);
			index.y = Integer.parseInt(tokens[1]);
			boundCellList.add(index); 
		}



		EFDCGrid grid = new EFDCGrid();
		grid.initialize(cornersInFile); // set up the grid
		grid.setCellSizeAndDepth(dxdyInFile, depthSize); 
		gridCells =grid.getGridCells(); 
		gridIndexes = grid.getGridIndexMap();  // returns a HashMap of the I,J's indexed on the L value
		if (gridIndexes == null){
			System.out.println("gridIndexes is null");
			System.exit(1); 
		}

		// loop through and get the min lat and lon centroid
		//because UTM is shifted compared to WGS, need to store the minlon and minLat as coordiantes, 
		// then convert, to get a singular originNode 
		Coordinate minLat = new Coordinate(Double.MAX_VALUE, Double.MAX_VALUE, 0);
		Coordinate minLon =new Coordinate(Double.MAX_VALUE, Double.MAX_VALUE, 0);
		Set<Int3D> set = gridCells.keySet();
		Iterator<Int3D> it = set.iterator();
		while (it.hasNext()) {

			Int3D index = it.next();
			//Geometry geom = gridCells.get(index).getGeom();
			Coordinate centroid = gridCells.get(index).getCentroidCoord(); 
			if (centroid.x < minLon.x) minLon = centroid;
			if (centroid.y < minLat.y) minLat = centroid; 
		}

		CoordinateUtils.convertUTMToLatLon(minLon, 17, false);
		CoordinateUtils.convertUTMToLatLon(minLat, 17, false);

		originNode.x = minLon.x; 
		originNode.y = minLat.y; 
		//convert the origin to WGS to write the lat/lon files
		//CoordinateUtils.convertUTMToLatLon(originNode, 17, false); 
		originNode.x -= gridXDim*lonPadding;
		originNode.y -= gridYDim*latPadding; 

		uReader = new BufferedReader(new FileReader(uInFile));
		vReader = new BufferedReader(new FileReader(vInFile));
		if (outputSalinity) salReader = new BufferedReader(new FileReader(salInFile));

		// create the dimension arrays -- these will be stored permanently for writing to each individual ncFile
		ArrayDouble.D1 dataX = new ArrayDouble.D1(lonSize);
		ArrayDouble.D1 dataY = new ArrayDouble.D1(latSize);
		ArrayDouble.D1 dataDepth = new ArrayDouble.D1(depthSize);

		double lonSum = originNode.x;
		for (int i=0; i<lonSize; i++) {
			dataX.set(i,  lonSum); // save in
			lonSum += gridXDim; 
		}
		double latSum = originNode.y; 
		for (int i=0; i<latSize; i++) {
			dataY.set(i, latSum);
			latSum += gridYDim; 
		}

		//Set the depth variable values to the mid-point of each depth layer, where the layers are standardized to 0-1 since is a sigma grid
		//double depthSum = (1.0/(double) depthSize) /2.0;
		//NOTE: didn't set to mid-point because the NCOM series starts at zero, and the locate(val) method will through ArrayIndexOutOfBounds if outside
		// the array values
		double depthSum = 0; 
		for (int i=0; i<depthSize; i++) {
			dataDepth.set(i, depthSum);
			depthSum += 1.0/(double) depthSize; 
		}


		long hrsSinceEpoch = 0; 
		int timeIndex = -1; 
		int cellIndex = 1;  
		int latIndex = 0; 
		int lonIndex = 0; 


		//Scroll through all the lines of the ASCI file, and for each row (a single L, or I,J cell) get an array of the depth bins, where first in layer 1, or deepest layer
		//Note: 'reader' is for U data, 'reader2' is for V data
		for (String uLine = uReader.readLine(); uLine != null; uLine = uReader.readLine()) {
			String vLine = vReader.readLine(); 
			String salLine = "temp string"; 
			if (outputSalinity) salLine = salReader.readLine(); 
			String uTokens[] = uLine.split("[ ]+"); 
			String vTokens[] = vLine.split("[ ]+"); 
			String salTokens[] = salLine.split("[ ]+"); 

			// if there's only 1 token per line, then this is the start of a new time, and this token is the timeStamp in days with decimal for hour
			//NOTE: is a space in front of tokens so get tokens == 2 when in actuality is just the time stamp
			if (uTokens.length == 2){ 

				
				System.out.println("reading time: " + uTokens[1]); 

				timeIndex++; // only do this after first index 
				cellIndex = 1; // reset so is the first L value 

				Double time = new Double(uTokens[1]);
				int daysSinceEpoch = time.intValue(); // turncate

				if (daysSinceEpoch > maxTime) break;

				long hour = Math.round((time.doubleValue() - daysSinceEpoch)*24); // should return the hour of day
				hrsSinceEpoch  = daysSinceEpoch*24 + hour; 

				//if new day, then close the old file and re-open a new one
				if (start || hour == 0) {
					
					if (!start) ncFile.close(); //close the old one
					if (!start) timeIndex = 0; 
					
					start = false; 

					ncFile = this.createNewFile(hrsSinceEpoch); 

					//write the full lat, lon, and depth arrays out
					ncFile.write(lonVarName, dataX);
					ncFile.write(latVarName, dataY);
					ncFile.write(depthVarName, dataDepth);
				}

				//Write the time step values to the timeVarName array
				int[] longShape = new int[]{1}; 
				long[] timeValue = new long[] {hrsSinceEpoch};
				Array longArray = Array.factory(long.class, longShape, timeValue); 
				int[] origin = new int[]{timeIndex}; 
				ncFile.write(timeVarName, origin, longArray); 
			}


			else {
				
				//if scaling data, then do differently
				if (scaleData){
					short[] uData = new short[depthSize]; 
					short[] vData = new short[depthSize]; 
					short[] salData = new short[depthSize]; 
					short[] tempData = new short[depthSize]; 
					

					Int3D index = gridIndexes.get(cellIndex);
					
					//substract 1 because EFDC uses 1 as base value, while Java is 0 for first array index
					lonIndex = index.x -1; 
					latIndex = index.y -1; 

					int counter = 0;
					//!!!!!!!!!!!!!!! NOTE !!!!!!!!!!!!!!!!
					// Need to flip the array here, because EFDC ASCI output starts with layer1 per row,
					// and layer 1 is the BOTTOM LAYER.  The highest numbered layer is the SURFACE LAYER
					
					for (int i=depthSize-1; i>=0; i--){
						//float[] temp = rotate(new Vector3D(Float.parseFloat(tokens[i+1]), Float.parseFloat(tokens2[i+1]))); 

						float uTemp = Float.parseFloat(uTokens[i+1]); //temp[0];
						float vTemp = Float.parseFloat(vTokens[i+1]); //temp[1];
						float salTemp = 0;
						if (outputSalinity) salTemp = Float.parseFloat(salTokens[i+1]); //temp[1];

						if (boundCellList.contains(index))   uTemp *= 2.0; 

						
						//round to Long and truncate to short 
						uData[counter] = (short) Math.round((uTemp* (1/velScaleFactor)));  
						vData[counter] = (short) Math.round((vTemp* (1/velScaleFactor)));
						if (outputSalinity) salData[counter] = (short) Math.round(((salTemp-offset)* (1/otherScaleFactor)));
						counter++; 
					}


					/*TODO - get and set temp data
					 * (1) get the appropriate NCOM time unit
					 * 		- do so by converting the currentDateAndTime to millis, then 
					 * (1) get the appropriate depths for a given cell
					 * (2) for each of the depthSize depth layers, get the NCOM value for a given depth 
					 */
					if (outputTemp) {
					double time = this.convertToNCOMTime(currentDateAndTime.getTimeInMillis()); 
					double maxDepth = grid.getGridCell(index).getDepth();

					Coordinate coord = grid.getGridCell(index).getCentroidCoord(); 
					CoordinateUtils.convertUTMToLatLon(coord, 17, false); 
					
					double depthLayer = 0; 
					for (int i=0; i<depthSize; i++){
						double depth = depthLayer*maxDepth; 
						depthLayer += 1.0/(double) depthSize;
						double temp = NCOMFile.getClosestValue(tempVarName, 
								new double[] {time, depth, coord.y, coord.x}, 
								new boolean[] {false, false, false, false}).doubleValue();
						tempData[i] = (short) Math.round(((temp-offset)* (1/otherScaleFactor)));
						
					}
					}


					int[] shape = new int[]{1, depthSize, 1, 1};
					Array uArray = Array.factory(short.class, shape, uData);
					Array vArray = Array.factory(short.class, shape, vData);
					Array salArray = null; 
					if (outputSalinity) salArray = Array.factory(short.class, shape, salData);
					Array tempArray = null; 
					if (outputTemp) tempArray = Array.factory(short.class, shape, tempData);

					//for (int d = 0; d < depthSize; d++) {
						int[] origin = new int[]{timeIndex, 0, latIndex, lonIndex};
						ncFile.write(uVarName, origin, uArray);
						ncFile.write(vVarName, origin, vArray);
						if (outputSalinity) ncFile.write(salVarName, origin, salArray);
						if (outputTemp) ncFile.write(tempVarName, origin, tempArray);
					//}
						
						//System.out.println("done with " + index.x + "\t" + index.y);
						
				} // end of if statement to check for scaling data
				
				
				else {
					
				float[] uData = new float[depthSize]; 
				float[] vData = new float[depthSize]; 
				float[] salData = new float[depthSize]; 

				//get the I-J point index from the L #, since the L# is the only thing we can get from the ASCI (is output in the L order)
				Int3D index = gridIndexes.get(cellIndex);

				for (int i=depthSize-1; i>=0; i--){
					//float[] temp = rotate(new Vector3D(Float.parseFloat(tokens[i+1]), Float.parseFloat(tokens2[i+1]))); 
					//if this cell is an open boundary cell, then multiple the u-vel by 2X

					uData[i] = Float.parseFloat(uTokens[i+1]); //temp[0]; 
					vData[i] = Float.parseFloat(vTokens[i+1]); //temp[1];  
					salData[i] = Float.parseFloat(vTokens[i+1]); //temp[1];  

					if (boundCellList.contains(index))   uData[i] *= 2.0; 

				}




				// need to get the latIndex (J) and lonIndex (I) from the EFDC Grid, by passing the L index
				//NEED to subtract 1 here, because EFDC stores first index as 1 instead of 0


				//|||||||||||||||||||||||||||||| NOTE ||||||||||||||||||||||||||||||
				/*this needs to be custom set to the model
				 * the lower bound EFDC cells should be index of '3' because pads 2 cells
				 * for whatever reason, the TBExtendedWGS has left-most cell (i.e., min lon index)
				 * of '4', so need to subtract out 2 cells here for the lonIndex
				 *
				 */

				lonIndex = index.x -1; //SEE NOTE ABOVE!!!!
				latIndex = index.y -1; 


				int[] shape = new int[]{1, depthSize, 1, 1};
				Array uArray = Array.factory(float.class, shape, uData);
				Array vArray = Array.factory(float.class, shape, vData);
				Array salArray = Array.factory(float.class, shape, vData);
				//for (int d = 0; d < depthSize; d++) {
					int[] origin = new int[]{timeIndex, 0, latIndex, lonIndex};
					ncFile.write(uVarName, origin, uArray);
					ncFile.write(vVarName, origin, vArray);
					if (outputSalinity) ncFile.write(salVarName, origin, salArray);
			//	}

				}

			} // end of else to pull out the data
			cellIndex++; 
		}	// end for loop over file

		ncFile.close();


	}

	public double convertToNCOMTime(long time){
		//NCOM is hours since 1/1/2000
		return (double) (time-NCOMTimeOffset) / (1000*60*60); 
	}
	
	
	
	
	public NetcdfFileWriteable createNewFile(long hrsSinceEpoch) throws IOException{
		//createNewFile()

		currentDateAndTime = TimeUtils.getDateFromHrsSinceTidalEpoch(hrsSinceEpoch); 
		int year = currentDateAndTime.get(Calendar.YEAR);
		int month = currentDateAndTime.get(Calendar.MONTH) +1;
		int day = currentDateAndTime.get(Calendar.DAY_OF_MONTH); 
		
		setNCOMFile(currentDateAndTime.getTimeInMillis()); 
		
		nf.setMinimumIntegerDigits(2); //set the number format to 2 integers (e.g., 01, 02, ...10, 11)
		String outDateURL = year+nf.format(month)+nf.format(day)+"00"; 
		String filename = directory + modelName + outDateURL + ".nc";
		new File(filename).delete(); 
		
		NetcdfFileWriteable ncFile = NetcdfFileWriteable.createNew(directory + modelName + outDateURL + ".nc");

		ncFile.setFill(true);	// this will fill in with blank values, as below
		//ncFile.setLargeFile(true); //NOTE: for whatever reason, when I set this = true, I cant open file in ncBrowse!!!


		String timeUnits = "hours since tidal epoch 1983-01-01 00:00:0.0 GMT";
		String latUnits = "degrees_north";
		String lonUnits = "degrees_east";
		String depthUnits = "standardized mid-layer depth (0-1) for sigma vertical grid"; 

		Dimension[] dim = new Dimension[4];


		dim[0] = setDimension(ncFile, DataType.DOUBLE, timeVarName, timeUnits, timeSize, true);
		dim[1]  = setDimension(ncFile, DataType.DOUBLE, depthVarName, depthUnits, depthSize, false); 
		dim[2] = setDimension(ncFile, DataType.DOUBLE, latVarName, latUnits, latSize, false);
		dim[3] = setDimension(ncFile, DataType.DOUBLE, lonVarName, lonUnits, lonSize, false);


		if (scaleData) {
			ncFile.addVariable(uVarName, DataType.SHORT, dim);
			ncFile.addVariable(vVarName, DataType.SHORT, dim);
			if (outputTemp) ncFile.addVariable(tempVarName, DataType.SHORT, dim);
			if (outputSalinity) ncFile.addVariable(salVarName, DataType.SHORT, dim);
		}
		else {
			ncFile.addVariable(uVarName, DataType.FLOAT, dim);
			ncFile.addVariable(vVarName, DataType.FLOAT, dim);
			if (outputTemp) ncFile.addVariable(tempVarName, DataType.FLOAT, dim);
			if (outputSalinity) ncFile.addVariable(salVarName, DataType.FLOAT, dim);
		}

		short missingVal = -9999; 
		
		//uVel
		ncFile.addVariableAttribute(uVarName, "units", "meter/sec");
		if (scaleData) {
			ncFile.addVariableAttribute(uVarName, "scale_factor", velScaleFactor);
			ncFile.addVariableAttribute(uVarName, "_FillValue", missingVal);
			ncFile.addVariableAttribute(uVarName, "missing_value", missingVal);
		}
		else {
			ncFile.addVariableAttribute(uVarName, "_FillValue", (float) missingVal);
			ncFile.addVariableAttribute(uVarName, "missing_value", (float) missingVal);
		}

		//vVel
		ncFile.addVariableAttribute(vVarName, "units", "meter/sec");
		if (scaleData) {
			ncFile.addVariableAttribute(vVarName, "scale_factor", velScaleFactor);
			ncFile.addVariableAttribute(vVarName, "_FillValue", missingVal);
			ncFile.addVariableAttribute(vVarName, "missing_value", missingVal);
		}
		else {
			ncFile.addVariableAttribute(vVarName, "_FillValue", (float) missingVal);
			ncFile.addVariableAttribute(vVarName, "missing_value", (float) missingVal);
		}

		
		//temp
		if (outputTemp) {
		ncFile.addVariableAttribute(tempVarName, "units", "degC");
		if (addOffset) ncFile.addVariableAttribute(tempVarName, "add_offset", offset);
		if (scaleData) {
			ncFile.addVariableAttribute(tempVarName, "scale_factor", otherScaleFactor);
			ncFile.addVariableAttribute(tempVarName, "_FillValue", missingVal);
			ncFile.addVariableAttribute(tempVarName, "missing_value", missingVal);
		}
		else {
			ncFile.addVariableAttribute(tempVarName, "_FillValue", (float) missingVal);
			ncFile.addVariableAttribute(tempVarName, "missing_value", (float) missingVal);
		}
		}
		
		//salinity
		if (outputSalinity) {
		ncFile.addVariableAttribute(salVarName, "units", "psu");
		if (addOffset) ncFile.addVariableAttribute(salVarName, "add_offset", offset);
		if (scaleData) {
			ncFile.addVariableAttribute(salVarName, "scale_factor", otherScaleFactor);
			ncFile.addVariableAttribute(salVarName, "_FillValue", missingVal);
			ncFile.addVariableAttribute(salVarName, "missing_value", missingVal);
		}
		else {
			ncFile.addVariableAttribute(salVarName, "_FillValue", (float) missingVal);
			ncFile.addVariableAttribute(salVarName, "missing_value", (float) missingVal);
		}
		}
		
		//set the remaining attributes for the dimensions; 
		//Note: units attributes are set in the setDimension() method
		ncFile.addVariableAttribute(timeVarName, "_FillValue", (double) missingVal);
		ncFile.addVariableAttribute(timeVarName, "missing_value", (double) missingVal);

		ncFile.addVariableAttribute(depthVarName, "_FillValue", (double) missingVal);
		ncFile.addVariableAttribute(depthVarName, "missing_value", (double) missingVal);

		ncFile.addVariableAttribute(latVarName, "_FillValue", (double) missingVal);
		ncFile.addVariableAttribute(latVarName, "missing_value", (double) missingVal);

		ncFile.addVariableAttribute(lonVarName, "_FillValue", (double) missingVal);
		ncFile.addVariableAttribute(lonVarName, "missing_value", (double) missingVal);


		ncFile.create();
		
		return ncFile; 
	}

	
	

	public void setNCOMFile(long time){
		
				GregorianCalendar date = new GregorianCalendar(TimeZone.getTimeZone("GMT")); 
				date.setTimeInMillis(time); 

				int month = date.get(Calendar.MONTH) + 1;  
				int day = date.get(Calendar.DATE);  
				int year = date.get(Calendar.YEAR);

				nf.setMinimumIntegerDigits(2); //set the number format to 2 integers (e.g., 01, 02, ...10, 11)
				dateURL = year+nf.format(month)+nf.format(day)+"00"; 

				try {
					NCOMFile = new NetCDFFile(baseFile + nameURL + dateURL + ".nc");
				} catch (IOException e) {}

				NCOMFile.setInterpolationRadius(2); 
				NCOMFile.setInterpolationAxes(latVarName, lonVarName);
				NCOMFile.setVariables(timeVarName, depthVarName, latVarName, lonVarName, uVarName, vVarName, tempVarName); 
	}




	public void writeBathymetry() throws IOException, InvalidRangeException {

		String outFile = directory + "TB2SB_WGS_bath.nc"; 	

		//delete existing file
		new File(outFile).delete();





		EFDCGrid grid = new EFDCGrid();
		grid.initialize(cornersInFile); // set up the grid
		grid.setCellSizeAndDepth(directory + "dxdy.inp", 8);
		gridCells =grid.getGridCells(); 
		gridIndexes = grid.getGridIndexMap();  // returns a HashMap of the I,J's indexed on the L value
		if (gridIndexes == null){
			System.out.println("gridIndexes is null");
			System.exit(1); 
		}

		// loop through and get the min lat and lon centroid
		//because UTM is shifted compared to WGS, need to store the minlon and minLat as coordiantes, 
		// then convert, to get a singular originNode 
		Coordinate minLat = new Coordinate(Double.MAX_VALUE, Double.MAX_VALUE, 0);
		Coordinate minLon =new Coordinate(Double.MAX_VALUE, Double.MAX_VALUE, 0);
		Set<Int3D> set = gridCells.keySet();
		Iterator<Int3D> it = set.iterator();
		while (it.hasNext()) {

			Int3D index = it.next();
			//Geometry geom = gridCells.get(index).getGeom();
			Coordinate centroid = gridCells.get(index).getCentroidCoord(); 
			if (centroid.x < minLon.x) minLon = centroid;
			if (centroid.y < minLat.y) minLat = centroid; 
		}

		CoordinateUtils.convertUTMToLatLon(minLon, 17, false);
		CoordinateUtils.convertUTMToLatLon(minLat, 17, false);

		originNode.x = minLon.x; 
		originNode.y = minLat.y; 
		//convert the origin to WGS to write the lat/lon files
		//CoordinateUtils.convertUTMToLatLon(originNode, 17, false); 
		originNode.x -= gridXDim*2;// substract out here two grid cells since EFDC pads 2 cells;
		originNode.y -= gridYDim*2; 

		NetcdfFileWriteable ncFile =NetcdfFileWriteable.createNew(outFile);
		//ncFile.setFill(true);	// this will fill in with blank values, as below
		//ncFile.setLargeFile(true); //NOTE: for whatever reason, when I set this = true, I cant open file in ncBrowse!!!


		String latUnits = "degrees_north";
		String lonUnits = "degrees_east";

		Dimension[] dim = new Dimension[2];


		dim[0] = setDimension(ncFile, DataType.DOUBLE, latVarName, latUnits, latSize, false);
		dim[1] = setDimension(ncFile, DataType.DOUBLE, lonVarName, lonUnits, lonSize, false);


		ncFile.addVariable(bathVarName, DataType.FLOAT, dim);

		ncFile.addVariableAttribute(bathVarName, "units", "meters");
		//ncFile.addVariableAttribute(bathVarName, "_FillValue", -9999);
		ncFile.addVariableAttribute(bathVarName, "missing_value", -9999);

		//ncFile.addVariableAttribute(latVarName, "_FillValue", -9999);
		ncFile.addVariableAttribute(latVarName, "missing_value", -9999);

		//ncFile.addVariableAttribute(lonVarName, "_FillValue", -9999);
		ncFile.addVariableAttribute(lonVarName, "missing_value", -9999);


		ncFile.create();


		// write the lat and lon index values out (here, leave as index values since grid is rotated and hence not regular and parallel with UTM lines
		ArrayDouble.D1 dataX = new ArrayDouble.D1(dim[1].getLength());
		ArrayDouble.D1 dataY = new ArrayDouble.D1(dim[0].getLength());
		ArrayFloat.D2 bath = new ArrayFloat.D2(dim[0].getLength(), dim[1].getLength());


		double latSum = originNode.y;
		for (int i=0; i<dim[0].getLength(); i++) {

			double lonSum = originNode.x;
			for (int j=0; j<dim[1].getLength(); j++) {

				Coordinate coord = new Coordinate(lonSum, latSum, 0);
				CoordinateUtils.convertLatLonToUTM(coord, 17);
				System.out.println("writing file at " + coord.x + "\t" + coord.y);

				EFDCCell cell = grid.getGridCell(coord);
				if (cell != null) {
					float depth = (float) cell.getDepth();
					bath.set(i, j, depth); 
				}
				else bath.set(i, j, -9999); 

				dataX.set(j,  lonSum); // save in
				lonSum += gridXDim; 

			}

			dataY.set(i, latSum);
			latSum += gridYDim; 
		}

		ncFile.write(lonVarName, dataX);
		ncFile.write(latVarName, dataY);
		ncFile.write(bathVarName, bath); 

		ncFile.close();


	}



	/*Returns a float[0=x,1=y] of a rotated vector
	 * 
	 */
	public float[] rotate(Vector3D vec){
		float[] values = new float[2]; 
		if (!rotate){
			values[0] = (float) vec.getX();
			values[1] = (float) vec.getY();
			return values;
		}
		else{
			double magnitude = vec.getNorm();
			double angle = vec.getAlpha();
			angle += Math.toRadians(rotationAngle);
			values[0] = (float) (Math.cos(angle)* magnitude);
			values[1] = (float) (Math.sin(angle) * magnitude);

			return values;
		}
	}


	private static Dimension setDimension(NetcdfFileWriteable ncFile,
			DataType type, String name, String units, int length, boolean isUnlimited) {

		Dimension dimension; 
		if (isUnlimited) {
			dimension = ncFile.addUnlimitedDimension(name);
		}
		else{
			dimension = ncFile.addDimension(name, length);
		}
		ncFile.addVariable(name, type, new Dimension[]{dimension});
		ncFile.addVariableAttribute(name, "units", units);


		return dimension;
	}



	}

