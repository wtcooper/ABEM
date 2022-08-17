package us.fl.state.fwc.util.geo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.ma2.ArrayShort;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import us.fl.state.fwc.util.BiCubicSpline;
import us.fl.state.fwc.util.Int3D;
import us.fl.state.fwc.util.TestingUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;


/**NetCDF utility file designed to connect to the physical oceanography data with time, depth, lat and lon data array structures.  
 * 
 * @author Wade.Cooper
 *
 */
public class NetCDFFile {

	protected SpatialIndex spatialIndex;

	private NetcdfFile dataFile = null;
	public HashMap<String, Variable> variables = new HashMap<String, Variable>(); 
	HashMap<String, Double> varScaleFactors; // = new HashMap<String, Double>(); 
	HashMap<String, Double> varAddOffsets; // = new HashMap<String, Double>(); 
	HashMap<String, Number> varMissingValue; // = new HashMap<String, Double>(); 
	HashMap<Variable, double[]> dimArrays = new HashMap<Variable, double[]>(); 

	//double scaleFactor;
	boolean useScaleFactor = false;
	boolean useAddOffset = false; 

	//parameters for getUV() method
	//private double u, v;
	private String uVarName, vVarName, latVarName, lonVarName; 
	//private Array uArr, vArr;
	private float cutoff = Float.MAX_VALUE; //set this to max value in case have scaled values 
	private boolean nearNoData = false;
	//private final double[] NODATA = { Double.NaN, Double.NaN };

	//interpolation parameters
	private int interpRadius = 1; //this is an interpolation radius, excluding the lat/lon cell
	private int interpDiameter = 2 * interpRadius + 1; //this is the interpolation diameter, wit the +1 referring to the lat/lon cell
	private int missingCellsCutoff = 8; //this is the tolerance for missing data to interpolate; if interpRadius=1 and missingCellsCutoff=8, then will return a value even if only the focal cell has a value
	
	private String interpY, interpX;
	private BiCubicSpline bcs; // = new BiCubicSpline(new double[interpDiameter],new double[interpDiameter], new float[interpDiameter][interpDiameter]);

	private double minLon = Double.MAX_VALUE;
	private double maxLon= Double.MAX_VALUE;
	private double minLat= Double.MAX_VALUE;
	private double maxLat= Double.MAX_VALUE; 
	private double domainWidth = Double.MAX_VALUE;
	private double domainHeight = Double.MAX_VALUE; 

	private boolean isUniformGrid = true;

	public NetCDFFile(String filename /*, boolean isUniform */) throws IOException{
		dataFile = NetcdfDataset.openFile(filename, null);    
		//isUniformGrid = isUniform;
	}



	/**Returns a value for the given variable at the known index location 
	 * Note: user must know the Variable's shape dimensions and order -- usually (time, depth, lat, lon)
	 * 
	 * @param varName
	 * @param index
	 * @param time
	 * @return Number (need to call appropriate typeValue() method) 
	 */
	public synchronized Number getValue(String varName, int[] index) {
		if (variables == null){
			System.out.println("NEED to call setVariables() first!");
			System.exit(1); 
		}

		try {
			Variable dataVar =variables.get(varName);

			int[] shape = new int[index.length];
			for (int i=0; i<shape.length; i++){
				shape[i] = 1; //set all equal to 1 since will return a single value
			}

			if (dataVar.getDataType().toString().equals("float")){
				ArrayFloat floatArray = (ArrayFloat) dataVar.read(index, shape).reduce();
				if ( (varScaleFactors == null || !varScaleFactors.containsKey(varName) ) && ( varAddOffsets == null  || !varAddOffsets.containsKey(varName)) ) return floatArray.getFloat(0); //return value if not using offsets or scaled data
				else {
					float offset = 0; 
					if ( varAddOffsets != null  && varAddOffsets.containsKey(varName)) offset = (float) varAddOffsets.get(varName).doubleValue(); //set the offset if used  
					if ( varScaleFactors != null  && varScaleFactors.containsKey(varName) ) return floatArray.getFloat(0)* (float) varScaleFactors.get(varName).doubleValue() + offset; //return offset (or 0) if use scale factor 	
					else return floatArray.getFloat(0) + offset; //else if don't use scale factor, just return with the offfset 
				}


				//				if (!useScaleFactor || !varScaleFactors.containsKey(varName) ) return floatArray.getFloat(0);
				//				else if (return floatArray.getFloat(0)* (float) varScaleFactors.get(varName).doubleValue(); 
			}

			else if (dataVar.getDataType().toString().equals("double")) {
				ArrayDouble doubleArray = (ArrayDouble) dataVar.read(index, shape).reduce();
				if ( (varScaleFactors == null || !varScaleFactors.containsKey(varName) ) && ( varAddOffsets == null  || !varAddOffsets.containsKey(varName)) ) return doubleArray.getDouble(0); //return value if not using offsets or scaled data
				else {
					double offset = 0; 
					if ( varAddOffsets != null  && varAddOffsets.containsKey(varName)) offset = varAddOffsets.get(varName).doubleValue(); //set the offset if used  
					if ( varScaleFactors != null  && varScaleFactors.containsKey(varName) ) return doubleArray.getDouble(0)* varScaleFactors.get(varName).doubleValue() + offset; //return offset (or 0) if use scale factor 	
					else return doubleArray.getDouble(0) + offset; //else if don't use scale factor, just return with the offfset
				}
				//				if (!useScaleFactor || !varScaleFactors.containsKey(varName)|| !useAddOffset || !varAddOffsets.containsKey(varName)) return doubleArray.getDouble(0);
				//				else return doubleArray.getDouble(0)*varScaleFactors.get(varName); 
			}

			else if (dataVar.getDataType().toString().equals("short")) {
				ArrayShort shortArray = (ArrayShort) dataVar.read(index, shape).reduce();
				if ( (varScaleFactors == null || !varScaleFactors.containsKey(varName) ) && ( varAddOffsets == null  || !varAddOffsets.containsKey(varName)) ) return shortArray.getShort(0); //return value if not using offsets or scaled data
				else {
					short offset = 0; 
					if ( varAddOffsets != null  && varAddOffsets.containsKey(varName)) offset = (short) varAddOffsets.get(varName).doubleValue(); //set the offset if used  
					if ( varScaleFactors != null  && varScaleFactors.containsKey(varName) ) return shortArray.getShort(0)* (short) varScaleFactors.get(varName).doubleValue() + offset; //return offset (or 0) if use scale factor 	
					else return shortArray.getShort(0) + offset; //else if don't use scale factor, just return with the offfset
				}
				//				if (!useScaleFactor || !varScaleFactors.containsKey(varName)|| !useAddOffset || !varAddOffsets.containsKey(varName)) return shortArray.getShort(0);
				//				else return shortArray.getShort(0)*varScaleFactors.get(varName); 
			}

			else if (dataVar.getDataType().toString().equals("int")) {
				ArrayInt intArray = (ArrayInt) dataVar.read(index, shape).reduce();
				if ( (varScaleFactors == null || !varScaleFactors.containsKey(varName) ) && ( varAddOffsets == null  || !varAddOffsets.containsKey(varName)) ) return intArray.getInt(0); //return value if not using offsets or scaled data
				else {
					int offset = 0; 
					if ( varAddOffsets != null  && varAddOffsets.containsKey(varName)) offset = (int) varAddOffsets.get(varName).doubleValue(); //set the offset if used  
					if ( varScaleFactors != null  && varScaleFactors.containsKey(varName) ) return intArray.getInt(0)* (short) varScaleFactors.get(varName).doubleValue() + offset; //return offset (or 0) if use scale factor 	
					else return intArray.getInt(0) + offset; //else if don't use scale factor, just return with the offfset
				}
				//				if (!useScaleFactor || !varScaleFactors.containsKey(varName) || !useAddOffset || !varAddOffsets.containsKey(varName)) return intArray.getInt(0);
				//				else return intArray.getInt(0)*varScaleFactors.get(varName); 
			}

		} catch (InvalidRangeException e) {
			e.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} 

		return null;

	}



	/**Returns a value for the given variable at the known index location 
	 * Note: user must know the Variable's shape dimensions and order -- usually (time, depth, lat, lon)
	 * 
	 * @param varName
	 * @param index
	 * @param time
	 * @return Number (need to call appropriate typeValue() method) 
	 */
	public synchronized Number getScalarValue(String varName) {
		if (variables == null){
			System.out.println("NEED to call setVariables() first!");
			System.exit(1); 
		}

		try {
			Variable dataVar =variables.get(varName);
			

			if (dataVar.getDataType().toString().equals("float")){
				Float floatVal = (Float) dataVar.readScalarFloat();
				if ( (varScaleFactors == null || !varScaleFactors.containsKey(varName) ) && ( varAddOffsets == null  || !varAddOffsets.containsKey(varName)) ) return floatVal; //return value if not using offsets or scaled data
				else {
					float offset = 0; 
					if ( varAddOffsets != null  && varAddOffsets.containsKey(varName)) offset = (float) varAddOffsets.get(varName).doubleValue(); //set the offset if used  
					if ( varScaleFactors != null  && varScaleFactors.containsKey(varName) ) return floatVal* (float) varScaleFactors.get(varName).doubleValue() + offset; //return offset (or 0) if use scale factor 	
					else return floatVal + offset; //else if don't use scale factor, just return with the offfset 
				}


				//				if (!useScaleFactor || !varScaleFactors.containsKey(varName) ) return floatArray.getFloat(0);
				//				else if (return floatArray.getFloat(0)* (float) varScaleFactors.get(varName).doubleValue(); 
			}

			else if (dataVar.getDataType().toString().equals("double")) {
				Double doubleVal = (Double) dataVar.readScalarDouble();
				if ( (varScaleFactors == null || !varScaleFactors.containsKey(varName) ) && ( varAddOffsets == null  || !varAddOffsets.containsKey(varName)) ) return doubleVal; //return value if not using offsets or scaled data
				else {
					double offset = 0; 
					if ( varAddOffsets != null  && varAddOffsets.containsKey(varName)) offset = varAddOffsets.get(varName).doubleValue(); //set the offset if used  
					if ( varScaleFactors != null  && varScaleFactors.containsKey(varName) ) return doubleVal* varScaleFactors.get(varName).doubleValue() + offset; //return offset (or 0) if use scale factor 	
					else return doubleVal + offset; //else if don't use scale factor, just return with the offfset
				}
				//				if (!useScaleFactor || !varScaleFactors.containsKey(varName)|| !useAddOffset || !varAddOffsets.containsKey(varName)) return doubleArray.getDouble(0);
				//				else return doubleArray.getDouble(0)*varScaleFactors.get(varName); 
			}

			else if (dataVar.getDataType().toString().equals("short")) {
				Short shortVal = (Short) dataVar.readScalarShort();
				if ( (varScaleFactors == null || !varScaleFactors.containsKey(varName) ) && ( varAddOffsets == null  || !varAddOffsets.containsKey(varName)) ) return shortVal; //return value if not using offsets or scaled data
				else {
					short offset = 0; 
					if ( varAddOffsets != null  && varAddOffsets.containsKey(varName)) offset = (short) varAddOffsets.get(varName).doubleValue(); //set the offset if used  
					if ( varScaleFactors != null  && varScaleFactors.containsKey(varName) ) return shortVal* (short) varScaleFactors.get(varName).doubleValue() + offset; //return offset (or 0) if use scale factor 	
					else return shortVal + offset; //else if don't use scale factor, just return with the offfset
				}
				//				if (!useScaleFactor || !varScaleFactors.containsKey(varName)|| !useAddOffset || !varAddOffsets.containsKey(varName)) return shortArray.getShort(0);
				//				else return shortArray.getShort(0)*varScaleFactors.get(varName); 
			}

			else if (dataVar.getDataType().toString().equals("int")) {
				Integer intVal = (Integer) dataVar.readScalarInt();
				if ( (varScaleFactors == null || !varScaleFactors.containsKey(varName) ) && ( varAddOffsets == null  || !varAddOffsets.containsKey(varName)) ) return intVal; //return value if not using offsets or scaled data
				else {
					int offset = 0; 
					if ( varAddOffsets != null  && varAddOffsets.containsKey(varName)) offset = (int) varAddOffsets.get(varName).doubleValue(); //set the offset if used  
					if ( varScaleFactors != null  && varScaleFactors.containsKey(varName) ) return intVal * (short) varScaleFactors.get(varName).doubleValue() + offset; //return offset (or 0) if use scale factor 	
					else return intVal + offset; //else if don't use scale factor, just return with the offfset
				}
				//				if (!useScaleFactor || !varScaleFactors.containsKey(varName) || !useAddOffset || !varAddOffsets.containsKey(varName)) return intArray.getInt(0);
				//				else return intArray.getInt(0)*varScaleFactors.get(varName); 
			}

		} catch (IOException ioe) {
			ioe.printStackTrace();
		} 

		return null;

	}


	/**	Returns a value for the given dimension values (continuous space), 
	 * with the possibility to interpolate the returned value in 2D space with BiCubicSpline.  
	 * 
	 * Note: if interpolating, user MUST call set2DInterpolationVariables()!
	 * 
	 * @param varName
	 * @param time
	 * @param depth
	 * @param lat
	 * @param lon
	 * @return
	 */
	public synchronized Number getValue(String varName, double[] dimValues, boolean[] useIndexValue, boolean interpolate)	{

		if (variables == null){
			System.out.println("NEED to call setVariables() first!");
			System.exit(1); 
		}

		try {
			Variable dataVar =variables.get(varName);  
			if (dimValues.length != dataVar.getShape().length){
				System.out.println("Passed the wrong size index array for the variable name!");
				System.exit(1); 
			}

			//instantiate the BiCubicSpline
			if (interpolate){
				bcs = new BiCubicSpline(new double[interpDiameter],new double[interpDiameter], new float[interpDiameter][interpDiameter]);
			}


			int[] origin = new int[dimValues.length];
			int[] shape = new int[origin.length]; //{1,1,1,1};
			int interpYIndex = 0, interpXIndex = 0;
			double interpYValue = 0, interpXValue = 0;

			ArrayList<Dimension> dims = (ArrayList<Dimension>) getVariable(varName).getDimensions();

			for (int i=0; i<dims.size(); i++) { 

				//IF using the double value as the index value instead of locating it, then set origin and shape as such
				if (useIndexValue[i] == true){
					origin[i] = (int) Math.round(dimValues[i]);
					shape[i] = 1; 
				}
				//ELSE, locate the index value based on the dimension values, and if the variable is an interpolation axis,
				// then set as such
				else {
					Dimension dim = dims.get(i);
					String dimName = dim.getName();

					if (interpolate){
						if (dimName.equals(interpY)){
							interpYIndex = locate(variables.get(dim.getName()), dimValues[i]) ;
							origin[i] = interpYIndex - interpRadius;
							interpYValue = dimValues[i];
							shape[i] = 2 * interpRadius + 1;
						}
						else if (dimName.equals(interpX)){
							interpXIndex = locate(variables.get(dim.getName()), dimValues[i]) ;
							origin[i] = interpXIndex - interpRadius;
							interpXValue = dimValues[i];
							shape[i] = 2 * interpRadius + 1;
						}
						else{
							origin[i] = locate(variables.get(dim.getName()), dimValues[i]);
							shape[i] = 1; 
						}
					}
					else{
						origin[i] = locate(variables.get(dim.getName()), dimValues[i]);
						shape[i] = 1; 
					}

				}
			}


			//return the closest index value
			//			int lonIndex = locate(lonVar, lon);
			//			int latIndex = locate(latVar, lat);// Getting the cell
			//			int depthIndex = locate(depthVar, depth);
			//			int timeIndex = locate(timeVar, time);

			if (!interpolate){

				/*				if (dataVar.getDataType().toString().equals("float")){
					if (!useScaleFactor || !varScaleFactors.containsKey(varName)) return dataVar.read(origin, shape).reduce().getFloat(0);
					else return dataVar.read(origin, shape).reduce().getFloat(0)*varScaleFactors.get(varName);
				}

				else if (dataVar.getDataType().toString().equals("double")) {
					if (!useScaleFactor || !varScaleFactors.containsKey(varName)) return dataVar.read(origin, shape).reduce().getDouble(0);
					else return dataVar.read(origin, shape).reduce().getDouble(0)*varScaleFactors.get(varName);
				}

				else if (dataVar.getDataType().toString().equals("short")) {
					if (!useScaleFactor || !varScaleFactors.containsKey(varName)) return dataVar.read(origin, shape).reduce().getShort(0);
					else return dataVar.read(origin, shape).reduce().getShort(0)*varScaleFactors.get(varName);
				}

				else if (dataVar.getDataType().toString().equals("int")) {
					if (!useScaleFactor || !varScaleFactors.containsKey(varName)) return dataVar.read(origin, shape).reduce().getInt(0);
					else return dataVar.read(origin, shape).reduce().getInt(0)*varScaleFactors.get(varName);
				}
				 */
				if (dataVar.getDataType().toString().equals("float")){
					ArrayFloat floatArray = (ArrayFloat) dataVar.read(origin, shape).reduce();
					if ( (varScaleFactors == null || !varScaleFactors.containsKey(varName) ) && ( varAddOffsets == null  || !varAddOffsets.containsKey(varName)) ) return floatArray.getFloat(0); //return value if not using offsets or scaled data
					else {
						float offset = 0; 
						if ( varAddOffsets != null  && varAddOffsets.containsKey(varName)) offset = (float) varAddOffsets.get(varName).doubleValue(); //set the offset if used  
						if ( varScaleFactors != null  && varScaleFactors.containsKey(varName) ) return floatArray.getFloat(0)* (float) varScaleFactors.get(varName).doubleValue() + offset; //return offset (or 0) if use scale factor 	
						else return floatArray.getFloat(0) + offset; //else if don't use scale factor, just return with the offfset 
					}
				}

				else if (dataVar.getDataType().toString().equals("double")) {
					ArrayDouble doubleArray = (ArrayDouble) dataVar.read(origin, shape).reduce();
					if ( (varScaleFactors == null || !varScaleFactors.containsKey(varName) ) && ( varAddOffsets == null  || !varAddOffsets.containsKey(varName)) ) return doubleArray.getDouble(0); //return value if not using offsets or scaled data
					else {
						double offset = 0; 
						if ( varAddOffsets != null  && varAddOffsets.containsKey(varName)) offset = varAddOffsets.get(varName).doubleValue(); //set the offset if used  
						if ( varScaleFactors != null  && varScaleFactors.containsKey(varName) ) return doubleArray.getDouble(0)* varScaleFactors.get(varName).doubleValue() + offset; //return offset (or 0) if use scale factor 	
						else return doubleArray.getDouble(0) + offset; //else if don't use scale factor, just return with the offfset
					}
				}

				else if (dataVar.getDataType().toString().equals("short")) {
					ArrayShort shortArray = (ArrayShort) dataVar.read(origin, shape).reduce();
					if ( (varScaleFactors == null || !varScaleFactors.containsKey(varName) ) && ( varAddOffsets == null  || !varAddOffsets.containsKey(varName)) ) return shortArray.getShort(0); //return value if not using offsets or scaled data
					else {
						short offset = 0; 
						if ( varAddOffsets != null  && varAddOffsets.containsKey(varName)) offset = (short) varAddOffsets.get(varName).doubleValue(); //set the offset if used  
						if ( varScaleFactors != null  && varScaleFactors.containsKey(varName) ) return shortArray.getShort(0)* (short) varScaleFactors.get(varName).doubleValue() + offset; //return offset (or 0) if use scale factor 	
						else return shortArray.getShort(0) + offset; //else if don't use scale factor, just return with the offfset
					}
				}

				else if (dataVar.getDataType().toString().equals("int")) {
					ArrayInt intArray = (ArrayInt) dataVar.read(origin, shape).reduce();
					if ( (varScaleFactors == null || !varScaleFactors.containsKey(varName) ) && ( varAddOffsets == null  || !varAddOffsets.containsKey(varName)) ) return intArray.getInt(0); //return value if not using offsets or scaled data
					else {
						int offset = 0; 
						if ( varAddOffsets != null  && varAddOffsets.containsKey(varName)) offset = (int) varAddOffsets.get(varName).doubleValue(); //set the offset if used  
						if ( varScaleFactors != null  && varScaleFactors.containsKey(varName) ) return intArray.getInt(0)* (short) varScaleFactors.get(varName).doubleValue() + offset; //return offset (or 0) if use scale factor 	
						else return intArray.getInt(0) + offset; //else if don't use scale factor, just return with the offfset
					}
				}
			}

			//else, if need to interpolate, then is a whole different ballgame....
			else {
				// Is the given latitude and longitude within the dimensions of the
				// data set?

				if (interpYIndex < interpRadius || interpYIndex >= (variables.get(interpY).getSize() - interpRadius)) {
					this.notifyAll();
					return null;

				} else if (interpXIndex < interpRadius || interpXIndex >= (variables.get(interpX).getSize() - interpRadius)) {
					this.notifyAll();
					return null;
				}


				//int[] origin = new int[] { timeIndex, depthIndex, latIndex - interpolRadius, lonIndex - interpolRadius };
				//int[] shape = new int[] { 1, 1, 2 * interpolRadius + 1, 2 * interpolRadius + 1 };


				Array dataArr = dataVar.read(origin, shape).reduce();

				Array latArr = variables.get(interpY).read(new int[] { interpYIndex - interpRadius }, new int[] { interpDiameter });
				Array lonArr = variables.get(interpX).read(new int[] { interpXIndex - interpRadius }, new int[] { interpDiameter });



				// do different for the different data types

				//===================================================
				//=======================float=========================
				//===================================================
				if (dataVar.getDataType().toString().equals("float")){
					float[][] dataTemp = (float[][]) dataArr.copyToNDJavaArray();

					// Replace NODATA values with average values...
					int missingCount = 0;
					float dataSum = 0;

					float missing_value = varMissingValue.get(varName).floatValue(); 

					for (int i = 0; i < interpDiameter; i++) {
						for (int j = 0; j < interpDiameter; j++) {

							if (dataTemp[i][j] == missing_value || dataTemp[i][j] > cutoff || Float.isNaN(dataTemp[i][j])) {
								missingCount++;
							} else {
								dataSum += dataTemp[i][j];
							}
						}
					}

					nearNoData = false;

					// If there are null values...
					if (missingCount > 0) {

						nearNoData = true; 

						// If there are more than (io-1)^2, return null
						//if (missingCount-1 > ((interpDiameter - 1) * (interpDiameter - 1))) {
						if (missingCount > this.missingCellsCutoff ) {
							return Float.NaN;
						}

						// Otherwise mitigate by replacing using the average value.
						float ave = dataSum / (float) (Math.pow(interpDiameter, 2) - missingCount);

						for (int i = 0; i < interpDiameter; i++) {
							for (int j = 0; j < interpDiameter; j++) {
								if (dataTemp[i][j] == missing_value || dataTemp[i][j] > cutoff || Float.isNaN(dataTemp[i][j])) {
									dataTemp[i][j] = ave;
								}
							}
						}
					}

					double[] latja; 
					double[] lonja; 

					if (latArr.getElementType() == Double.TYPE) {
						latja = (double[]) latArr.copyTo1DJavaArray();
						lonja = (double[]) lonArr.copyTo1DJavaArray();
					}
					else if (latArr.getElementType() == Float.TYPE){
						float[] latTemp = (float[]) latArr.copyTo1DJavaArray();
						float[] lonTemp = (float[]) lonArr.copyTo1DJavaArray();
						latja = new double[latTemp.length];
						lonja = new double[lonTemp.length];
						for (int i=0; i<latTemp.length; i++){
							latja[i] = (double) latTemp[i];
						}
						for (int i=0; i<lonTemp.length; i++){
							lonja[i] = (double) lonTemp[i];
						}
					}
					else if (latArr.getElementType() == Integer.TYPE){
						int[] latTemp = (int[]) latArr.copyTo1DJavaArray();
						int[] lonTemp = (int[]) lonArr.copyTo1DJavaArray();
						latja = new double[latTemp.length];
						lonja = new double[lonTemp.length];
						for (int i=0; i<latTemp.length; i++){
							latja[i] = (double) latTemp[i];
						}
						for (int i=0; i<lonTemp.length; i++){
							lonja[i] = (double) lonTemp[i];
						}
					}
					else if (latArr.getElementType() == Short.TYPE){
						short[] latTemp = (short[]) latArr.copyTo1DJavaArray();
						short[] lonTemp = (short[]) lonArr.copyTo1DJavaArray();
						latja = new double[latTemp.length];
						lonja = new double[lonTemp.length];
						for (int i=0; i<latTemp.length; i++){
							latja[i] = (double) latTemp[i];
						}
						for (int i=0; i<lonTemp.length; i++){
							lonja[i] = (double) lonTemp[i];
						}
					}
					else {
						long[] latTemp = (long[]) latArr.copyTo1DJavaArray();
						long[] lonTemp = (long[]) lonArr.copyTo1DJavaArray();
						latja = new double[latTemp.length];
						lonja = new double[lonTemp.length];
						for (int i=0; i<latTemp.length; i++){
							latja[i] = (double) latTemp[i];
						}
						for (int i=0; i<lonTemp.length; i++){
							lonja[i] = (double) lonTemp[i];
						}
					}


					// Obtain the interpolated values
					bcs.resetData(lonja, latja, dataTemp);
					//					if (!useScaleFactor || !varScaleFactors.containsKey(varName)) return (float) bcs.interpolate(interpXValue, interpYValue);
					//					else return (float) bcs.interpolate(interpXValue, interpYValue)*varScaleFactors.get(varName);
					if ( (varScaleFactors == null || !varScaleFactors.containsKey(varName) ) && ( varAddOffsets == null  || !varAddOffsets.containsKey(varName)) ) return (float) bcs.interpolate(interpXValue, interpYValue);
					else {
						float offset = 0; 
						if ( varAddOffsets != null  && varAddOffsets.containsKey(varName)) offset = (float) varAddOffsets.get(varName).doubleValue(); //set the offset if used  
						if ( varScaleFactors != null  && varScaleFactors.containsKey(varName) ) return (float) bcs.interpolate(interpXValue, interpYValue)*varScaleFactors.get(varName) + offset;  	
						else return (float) bcs.interpolate(interpXValue, interpYValue);
					}
				}

				//===================================================
				//=======================double=========================
				//===================================================
				else if (dataVar.getDataType().toString().equals("double")) {
					double[][] dataTemp = (double[][]) dataArr.copyToNDJavaArray();

					// Replace NODATA values with average values...
					int missingCount = 0;
					double dataSum = 0;

					double missing_value = varMissingValue.get(varName).doubleValue(); 

					for (int i = 0; i < interpDiameter; i++) {
						for (int j = 0; j < interpDiameter; j++) {

							if (dataTemp[i][j] == missing_value || dataTemp[i][j] > cutoff || Double.isNaN(dataTemp[i][j])) {
								missingCount++;
							} else {
								dataSum += dataTemp[i][j];
							}
						}
					}

					nearNoData = false;

					// If there are null values...
					if (missingCount > 0) {
						nearNoData = true;

						// If there are more than (io-1)^2, return null
						if (missingCount>missingCellsCutoff) { //-1 > ((interpDiameter - 1) * (interpDiameter - 1))) {
							return Double.NaN;
						}

						// Otherwise mitigate by replacing using the average value.
						double ave = dataSum / (double) (Math.pow(interpDiameter, 2) - missingCount);

						for (int i = 0; i < interpDiameter; i++) {
							for (int j = 0; j < interpDiameter; j++) {
								if (dataTemp[i][j] == missing_value || dataTemp[i][j] > cutoff || Double.isNaN(dataTemp[i][j])) {
									dataTemp[i][j] = ave;
								}
							}
						}
					}

					double[] latja; 
					double[] lonja; 

					if (latArr.getElementType() == Double.TYPE) {
						latja = (double[]) latArr.copyTo1DJavaArray();
						lonja = (double[]) lonArr.copyTo1DJavaArray();
					}
					else if (latArr.getElementType() == Float.TYPE){
						float[] latTemp = (float[]) latArr.copyTo1DJavaArray();
						float[] lonTemp = (float[]) lonArr.copyTo1DJavaArray();
						latja = new double[latTemp.length];
						lonja = new double[lonTemp.length];
						for (int i=0; i<latTemp.length; i++){
							latja[i] = (double) latTemp[i];
						}
						for (int i=0; i<lonTemp.length; i++){
							lonja[i] = (double) lonTemp[i];
						}
					}
					else if (latArr.getElementType() == Integer.TYPE){
						int[] latTemp = (int[]) latArr.copyTo1DJavaArray();
						int[] lonTemp = (int[]) lonArr.copyTo1DJavaArray();
						latja = new double[latTemp.length];
						lonja = new double[lonTemp.length];
						for (int i=0; i<latTemp.length; i++){
							latja[i] = (double) latTemp[i];
						}
						for (int i=0; i<lonTemp.length; i++){
							lonja[i] = (double) lonTemp[i];
						}
					}
					else if (latArr.getElementType() == Short.TYPE){
						short[] latTemp = (short[]) latArr.copyTo1DJavaArray();
						short[] lonTemp = (short[]) lonArr.copyTo1DJavaArray();
						latja = new double[latTemp.length];
						lonja = new double[lonTemp.length];
						for (int i=0; i<latTemp.length; i++){
							latja[i] = (double) latTemp[i];
						}
						for (int i=0; i<lonTemp.length; i++){
							lonja[i] = (double) lonTemp[i];
						}
					}
					else {
						long[] latTemp = (long[]) latArr.copyTo1DJavaArray();
						long[] lonTemp = (long[]) lonArr.copyTo1DJavaArray();
						latja = new double[latTemp.length];
						lonja = new double[lonTemp.length];
						for (int i=0; i<latTemp.length; i++){
							latja[i] = (double) latTemp[i];
						}
						for (int i=0; i<lonTemp.length; i++){
							lonja[i] = (double) lonTemp[i];
						}
					}

					// Obtain the interpolated values
					bcs.resetData(lonja, latja, dataTemp);
					//					if (!useScaleFactor || !varScaleFactors.containsKey(varName)) return bcs.interpolate(interpXValue, interpYValue);
					//					else return bcs.interpolate(interpXValue, interpYValue)*varScaleFactors.get(varName);
					if ( (varScaleFactors == null || !varScaleFactors.containsKey(varName) ) && ( varAddOffsets == null  || !varAddOffsets.containsKey(varName)) ) return bcs.interpolate(interpXValue, interpYValue);
					else {
						double offset = 0; 
						if ( varAddOffsets != null  && varAddOffsets.containsKey(varName)) offset = varAddOffsets.get(varName).doubleValue(); 
						if ( varScaleFactors != null  && varScaleFactors.containsKey(varName) ) return bcs.interpolate(interpXValue, interpYValue)*varScaleFactors.get(varName) + offset; 
						else return bcs.interpolate(interpXValue, interpYValue) + offset; 
					}
				}

				//===================================================
				//=======================short=========================
				//===================================================
				else if (dataVar.getDataType().toString().equals("short")) {
					short[][] dataTempShort = (short[][]) dataArr.copyToNDJavaArray();
					float[][] dataTemp = new float[dataTempShort.length][dataTempShort[0].length];

					for (int i = 0; i < interpDiameter; i++) {
						for (int j = 0; j < interpDiameter; j++) {
							dataTemp[i][j] = (float) dataTempShort[i][j];
						}
					}

					short missing_value = varMissingValue.get(varName).shortValue();

					// Replace NODATA values with average values...
					int missingCount = 0;
					float dataSum = 0;

					for (int i = 0; i < interpDiameter; i++) {
						for (int j = 0; j < interpDiameter; j++) {

							if (dataTempShort[i][j] == missing_value || dataTemp[i][j] > cutoff || Float.isNaN(dataTemp[i][j])) {
								missingCount++;
							} else {
								dataSum += dataTemp[i][j];
							}
						}
					}
					nearNoData = false;

					// If there are null values...
					if (missingCount > 0) {
						nearNoData = true;

						// If there are more than (io-1)^2, return null
						if (missingCount > missingCellsCutoff) { //-1 > ((interpDiameter - 1) * (interpDiameter - 1))) {
							return Double.NaN;
						}

						// Otherwise mitigate by replacing using the average value.
						float ave = dataSum / (float) (Math.pow(interpDiameter, 2) - missingCount);

						for (int i = 0; i < interpDiameter; i++) {
							for (int j = 0; j < interpDiameter; j++) {
								if (dataTempShort[i][j] == missing_value || dataTemp[i][j] > cutoff || Float.isNaN(dataTemp[i][j])) {
									dataTemp[i][j] = ave;
								}
							}
						}
					}

					double[] latja; 
					double[] lonja; 

					if (latArr.getElementType() == Double.TYPE) {
						latja = (double[]) latArr.copyTo1DJavaArray();
						lonja = (double[]) lonArr.copyTo1DJavaArray();
					}
					else if (latArr.getElementType() == Float.TYPE){
						float[] latTemp = (float[]) latArr.copyTo1DJavaArray();
						float[] lonTemp = (float[]) lonArr.copyTo1DJavaArray();
						latja = new double[latTemp.length];
						lonja = new double[lonTemp.length];
						for (int i=0; i<latTemp.length; i++){
							latja[i] = (double) latTemp[i];
						}
						for (int i=0; i<lonTemp.length; i++){
							lonja[i] = (double) lonTemp[i];
						}
					}
					else if (latArr.getElementType() == Integer.TYPE){
						int[] latTemp = (int[]) latArr.copyTo1DJavaArray();
						int[] lonTemp = (int[]) lonArr.copyTo1DJavaArray();
						latja = new double[latTemp.length];
						lonja = new double[lonTemp.length];
						for (int i=0; i<latTemp.length; i++){
							latja[i] = (double) latTemp[i];
						}
						for (int i=0; i<lonTemp.length; i++){
							lonja[i] = (double) lonTemp[i];
						}
					}
					else if (latArr.getElementType() == Short.TYPE){
						short[] latTemp = (short[]) latArr.copyTo1DJavaArray();
						short[] lonTemp = (short[]) lonArr.copyTo1DJavaArray();
						latja = new double[latTemp.length];
						lonja = new double[lonTemp.length];
						for (int i=0; i<latTemp.length; i++){
							latja[i] = (double) latTemp[i];
						}
						for (int i=0; i<lonTemp.length; i++){
							lonja[i] = (double) lonTemp[i];
						}
					}
					else {
						long[] latTemp = (long[]) latArr.copyTo1DJavaArray();
						long[] lonTemp = (long[]) lonArr.copyTo1DJavaArray();
						latja = new double[latTemp.length];
						lonja = new double[lonTemp.length];
						for (int i=0; i<latTemp.length; i++){
							latja[i] = (double) latTemp[i];
						}
						for (int i=0; i<lonTemp.length; i++){
							lonja[i] = (double) lonTemp[i];
						}
					}

					// Obtain the interpolated values
					bcs.resetData(lonja, latja, dataTemp);
					double value = bcs.interpolate(interpXValue, interpYValue);
					//					if (!useScaleFactor || !varScaleFactors.containsKey(varName)) return (short) Math.round(value);
					//					else return value*varScaleFactors.get(varName); // return as double if use scaleFactor
					if ( (varScaleFactors == null || !varScaleFactors.containsKey(varName) ) && ( varAddOffsets == null  || !varAddOffsets.containsKey(varName)) ) return (short) Math.round(value);
					else {
						short offset = 0; 
						if ( varAddOffsets != null  && varAddOffsets.containsKey(varName)) offset = (short) varAddOffsets.get(varName).doubleValue(); //set the offset if used  
						if ( varScaleFactors != null  && varScaleFactors.containsKey(varName) ) return value*varScaleFactors.get(varName) + offset; //return offset (or 0) if use scale factor 	
						else return (short) Math.round(value) + offset; 
					}
				}

				//===================================================
				//=======================int=========================
				//===================================================
				else if (dataVar.getDataType().toString().equals("int")) {
					int[][] dataTempInt = (int[][]) dataArr.copyToNDJavaArray();
					float[][] dataTemp = new float[dataTempInt.length][dataTempInt[0].length];

					for (int i = 0; i < interpDiameter; i++) {
						for (int j = 0; j < interpDiameter; j++) {
							dataTemp[i][j] = (float) dataTempInt[i][j];
						}
					}

					// Replace NODATA values with average values...
					int missingCount = 0;
					float dataSum = 0;

					int missing_value = varMissingValue.get(varName).intValue();

					for (int i = 0; i < interpDiameter; i++) {
						for (int j = 0; j < interpDiameter; j++) {

							if (dataTempInt[i][j] == missing_value || dataTemp[i][j] > cutoff || Float.isNaN(dataTemp[i][j])) {
								missingCount++;
							} else {
								dataSum += dataTemp[i][j];
							}
						}
					}
					nearNoData = false;

					// If there are null values...
					if (missingCount > 0) {
						nearNoData = true;

						// If there are more than (io-1)^2, return null
						if (missingCount > missingCellsCutoff) { //-1 > ((interpDiameter - 1) * (interpDiameter - 1))) {
							return Double.NaN;
						}

						// Otherwise mitigate by replacing using the average value.
						float ave = dataSum / (float) (Math.pow(interpDiameter, 2) - missingCount);

						for (int i = 0; i < interpDiameter; i++) {
							for (int j = 0; j < interpDiameter; j++) {
								if (dataTempInt[i][j] == missing_value || dataTemp[i][j] > cutoff || Float.isNaN(dataTemp[i][j])) {
									dataTemp[i][j] = ave;
								}
							}
						}
					}

					double[] latja; 
					double[] lonja; 

					if (latArr.getElementType() == Double.TYPE) {
						latja = (double[]) latArr.copyTo1DJavaArray();
						lonja = (double[]) lonArr.copyTo1DJavaArray();
					}
					else if (latArr.getElementType() == Float.TYPE){
						float[] latTemp = (float[]) latArr.copyTo1DJavaArray();
						float[] lonTemp = (float[]) lonArr.copyTo1DJavaArray();
						latja = new double[latTemp.length];
						lonja = new double[lonTemp.length];
						for (int i=0; i<latTemp.length; i++){
							latja[i] = (double) latTemp[i];
						}
						for (int i=0; i<lonTemp.length; i++){
							lonja[i] = (double) lonTemp[i];
						}
					}
					else if (latArr.getElementType() == Integer.TYPE){
						int[] latTemp = (int[]) latArr.copyTo1DJavaArray();
						int[] lonTemp = (int[]) lonArr.copyTo1DJavaArray();
						latja = new double[latTemp.length];
						lonja = new double[lonTemp.length];
						for (int i=0; i<latTemp.length; i++){
							latja[i] = (double) latTemp[i];
						}
						for (int i=0; i<lonTemp.length; i++){
							lonja[i] = (double) lonTemp[i];
						}
					}
					else if (latArr.getElementType() == Short.TYPE){
						short[] latTemp = (short[]) latArr.copyTo1DJavaArray();
						short[] lonTemp = (short[]) lonArr.copyTo1DJavaArray();
						latja = new double[latTemp.length];
						lonja = new double[lonTemp.length];
						for (int i=0; i<latTemp.length; i++){
							latja[i] = (double) latTemp[i];
						}
						for (int i=0; i<lonTemp.length; i++){
							lonja[i] = (double) lonTemp[i];
						}
					}
					else {
						long[] latTemp = (long[]) latArr.copyTo1DJavaArray();
						long[] lonTemp = (long[]) lonArr.copyTo1DJavaArray();
						latja = new double[latTemp.length];
						lonja = new double[lonTemp.length];
						for (int i=0; i<latTemp.length; i++){
							latja[i] = (double) latTemp[i];
						}
						for (int i=0; i<lonTemp.length; i++){
							lonja[i] = (double) lonTemp[i];
						}
					}

					// Obtain the interpolated values
					bcs.resetData(lonja, latja, dataTemp);
					double value = bcs.interpolate(interpXValue, interpYValue);
					//					if (!useScaleFactor || !varScaleFactors.containsKey(varName)) return (int) Math.round(value);
					//					else return value*varScaleFactors.get(varName);
					if ( (varScaleFactors == null || !varScaleFactors.containsKey(varName) ) && ( varAddOffsets == null  || !varAddOffsets.containsKey(varName)) ) return (int) Math.round(value);
					else {
						int offset = 0; 
						if ( varAddOffsets != null  && varAddOffsets.containsKey(varName)) offset = (int) varAddOffsets.get(varName).doubleValue();   
						if ( varScaleFactors != null  && varScaleFactors.containsKey(varName) ) return value*varScaleFactors.get(varName) + offset;  	
						else return (int) Math.round(value) + offset; 
					}
				}
			}

		} catch (IOException e) {
			System.out	.println("WARNING:  Error reading from velocity files.\n\n");
			e.printStackTrace();
		}catch (InvalidRangeException e) {
			e.printStackTrace();
			System.out.println("catching error " + e);
			TestingUtils.dropBreadCrumb(); 
		}
		return null;


	}




	/**	Returns a value for the given dimension values (continuous space), 
	 * with the possibility to interpolate the returned value in 2D space with BiCubicSpline. 
	 * If likely that are sparesly filled and won't be values to interpolate in vicinity, then can 
	 * also get the closest value relative to the interpolation axes (currently, just lat/lon axes) 
	 * 
	 * Note: if interpolating, user MUST call set2DInterpolationVariables()!
	 * 
	 * @param varName
	 * @param time
	 * @param depth
	 * @param lat
	 * @param lon
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public synchronized Number getClosestValue(String varName, double[] dimValues, boolean[] useIndexValue)	{



		if (variables == null){
			System.out.println("NEED to call setVariables() first!");
			System.exit(1); 
		}



		try {
			Variable dataVar =variables.get(varName);  
			if (dimValues.length != dataVar.getShape().length){
				System.out.println("Passed the wrong size index array for the variable name!");
				System.exit(1); 
			}




			int[] origin = new int[dimValues.length];
			int[] shape = new int[origin.length]; //{1,1,1,1};

			ArrayList<Dimension> dims = (ArrayList<Dimension>) getVariable(varName).getDimensions();

			for (int i=0; i<dims.size(); i++) { 

				//IF using the double value as the index value instead of locating it, then set origin and shape as such
				if (useIndexValue[i] == true){
					origin[i] = (int) Math.round(dimValues[i]);
					shape[i] = 1; 
				}
				//ELSE, locate the index value based on the dimension values, and if the variable is an interpolation axis,
				// then set as such
				else {
					Dimension dim = dims.get(i);
					origin[i] = locate(variables.get(dim.getName()), dimValues[i]);
					shape[i] = 1; 

				}
			}

			Array array = dataVar.read(origin, shape).reduce();

			//if data is missing, then get closest value
			if (array.getDouble(0) == getMissingValue(varName).doubleValue()){

				//first check to make sure it's not because the depth is wrong
				int[] originTemp = origin.clone();
				originTemp[1] = 0; 
				Array array2 = dataVar.read(originTemp, shape).reduce();
				if (array2.getDouble(0) != getMissingValue(varName).doubleValue()){
					originTemp[1] = origin[1]-1; 
					array2 = dataVar.read(originTemp, shape).reduce();
					while (array2.getDouble(0) == getMissingValue(varName).doubleValue()){
						originTemp[1] --;  
						array2 = dataVar.read(originTemp, shape).reduce();
					}
					origin[1] = originTemp[1]; 
				}
				
				
				else {
					if (spatialIndex == null) buildSpatialIndex(varName, origin, shape); 

					double minDist =Double.MAX_VALUE;
					int lonIndex=0, latIndex=0;
					Coordinate midPoint = new Coordinate(dimValues[dimValues.length-1], dimValues[dimValues.length-2], 0); 

					double searchRadius = 0.1; 
					Envelope env = new Envelope(midPoint.x-searchRadius, midPoint.x+searchRadius, midPoint.y-searchRadius, midPoint.y+searchRadius);
					List<Int3D> hits = spatialIndex.query(env);
					while (hits.isEmpty()){
						searchRadius += 0.1; 
						env = new Envelope(midPoint.x-searchRadius, midPoint.x+searchRadius, midPoint.y-searchRadius, midPoint.y+searchRadius);
						hits = spatialIndex.query(env);
					}
					Iterator<Int3D> it = hits.iterator();
					while (it.hasNext()){
						Int3D index = it.next(); 
						Coordinate tempCoord = new Coordinate(dimArrays.get(variables.get("lon"))[index.x], dimArrays.get(variables.get("lat"))[index.y], 0);
						double dist = Math.abs(CoordinateUtils.getDistance(midPoint, tempCoord));
						if (dist<minDist) {
							minDist = dist;
							latIndex = index.y;
							lonIndex = index.x;
						}

					}

					/*				Alternative way where searches through all lat/lon -- takes long long tim
				Coordinate coord2 = new Coordinate(dimValues[dimValues.length-1], dimValues[dimValues.length-2], 0);

				double minDist =Double.MAX_VALUE;
				int lonIndex=0, latIndex=0;
				double[] latArray = dimArrays.get(variables.get("lat"));
				double[] lonArray = dimArrays.get(variables.get("lon"));

				for (int i=0; i<latArray.length; i++){
					for (int j=0; j<lonArray.length; j++){
						int[] originTemp = {origin[0], origin[1], i, j}; 
						array = dataVar.read(originTemp, shape).reduce();

						//if data is missing, then get closest value
						if (array.getDouble(0) != getMissingValue(varName).doubleValue()){

							Coordinate coord1 = new Coordinate(lonArray[j], latArray[i], 0);
							double dist = Math.abs(CoordinateUtils.getDistance(coord1, coord2));
							if (dist<minDist) {
								minDist = dist;
								latIndex = i;
								lonIndex = j;
							}
						}
					}
				}
					 */
					//reset the origin for the lat and lon to the closest ones that are not missing data
					origin[dimValues.length-2] = latIndex;
					origin[dimValues.length-1] = lonIndex; 
				}
			}

			//return the appropriate data type
			if (dataVar.getDataType().toString().equals("float")){
				ArrayFloat floatArray = (ArrayFloat) dataVar.read(origin, shape).reduce();

				if ( (varScaleFactors == null || !varScaleFactors.containsKey(varName) ) && ( varAddOffsets == null  || !varAddOffsets.containsKey(varName)) ) return floatArray.getFloat(0); //return value if not using offsets or scaled data
				else {
					float offset = 0; 
					if ( varAddOffsets != null  && varAddOffsets.containsKey(varName)) offset = (float) varAddOffsets.get(varName).doubleValue(); //set the offset if used  
					if ( varScaleFactors != null  && varScaleFactors.containsKey(varName) ) return (float) floatArray.getFloat(0)* varScaleFactors.get(varName).doubleValue() + offset; //return offset (or 0) if use scale factor 	
					else return floatArray.getFloat(0) + offset; //else if don't use scale factor, just return with the offfset 
				}
			}

			else if (dataVar.getDataType().toString().equals("double")) {
				ArrayDouble doubleArray = (ArrayDouble) dataVar.read(origin, shape).reduce();
				if ( (varScaleFactors == null || !varScaleFactors.containsKey(varName) ) && ( varAddOffsets == null  || !varAddOffsets.containsKey(varName)) ) return doubleArray.getDouble(0); //return value if not using offsets or scaled data
				else {
					double offset = 0; 
					if ( varAddOffsets != null  && varAddOffsets.containsKey(varName)) offset = varAddOffsets.get(varName).doubleValue(); //set the offset if used  
					if ( varScaleFactors != null  && varScaleFactors.containsKey(varName) ) return doubleArray.getDouble(0)* varScaleFactors.get(varName).doubleValue() + offset; //return offset (or 0) if use scale factor 	
					else return doubleArray.getDouble(0) + offset; //else if don't use scale factor, just return with the offfset
				}
			}

			else if (dataVar.getDataType().toString().equals("short")) {
				ArrayShort shortArray = (ArrayShort) dataVar.read(origin, shape).reduce();
				if ( (varScaleFactors == null || !varScaleFactors.containsKey(varName) ) && ( varAddOffsets == null  || !varAddOffsets.containsKey(varName)) ) return shortArray.getShort(0); //return value if not using offsets or scaled data
				else {
					short offset = 0; 
					if ( varAddOffsets != null  && varAddOffsets.containsKey(varName)) offset = (short) varAddOffsets.get(varName).doubleValue(); //set the offset if used  
					if ( varScaleFactors != null  && varScaleFactors.containsKey(varName) ) return (short) shortArray.getShort(0)* varScaleFactors.get(varName).doubleValue() + offset; //return offset (or 0) if use scale factor 	
					else return shortArray.getShort(0) + offset; //else if don't use scale factor, just return with the offfset
				}
			}

			else if (dataVar.getDataType().toString().equals("int")) {
				ArrayInt intArray = (ArrayInt) dataVar.read(origin, shape).reduce();
				if ( (varScaleFactors == null || !varScaleFactors.containsKey(varName) ) && ( varAddOffsets == null  || !varAddOffsets.containsKey(varName)) ) return intArray.getInt(0); //return value if not using offsets or scaled data
				else {
					int offset = 0; 
					if ( varAddOffsets != null  && varAddOffsets.containsKey(varName)) offset = (int) varAddOffsets.get(varName).doubleValue(); //set the offset if used  
					if ( varScaleFactors != null  && varScaleFactors.containsKey(varName) ) return (short) intArray.getInt(0)* varScaleFactors.get(varName).doubleValue() + offset; //return offset (or 0) if use scale factor 	
					else return intArray.getInt(0) + offset; //else if don't use scale factor, just return with the offfset
				}
			}
		} catch (IOException e) {
			System.out.println("catching error " + e);
			TestingUtils.dropBreadCrumb(); 
			e.printStackTrace();
		} catch (InvalidRangeException e) {
			System.out.println("catching error " + e);
			TestingUtils.dropBreadCrumb(); 
			e.printStackTrace();
		}


		return null;

	}



	/**	Builds the spatial index 
	 * @throws InvalidRangeException 
	 * @throws IOException 
	 * 
	 */
	private  void buildSpatialIndex(String varName, int[] origin, int[] shape) throws IOException, InvalidRangeException{

		GeometryFactory gf = new GeometryFactory();
		Variable dataVar =variables.get(varName);  
		spatialIndex = new STRtree(); 
		int[] originTemp = origin.clone(); 

		double[] latArray = dimArrays.get(variables.get("lat"));
		double[] lonArray = dimArrays.get(variables.get("lon"));
		Array array; 

		for (int i=0; i<latArray.length; i++){
			for (int j=0; j<lonArray.length; j++){

				originTemp[origin.length-2] = i; //set the lat 
				originTemp[origin.length-1] = j; //set the lon

				array = dataVar.read(originTemp, shape).reduce();

				//if it's an active cell, then add it to the spatialIndex
				if (array.getDouble(0) != getMissingValue(varName).doubleValue()){

					Coordinate coord = new Coordinate(lonArray[j], latArray[i], 0);
					Point location = gf.createPoint(coord);
					Int3D index = new Int3D(j, i, 0); 
					spatialIndex.insert(location.getEnvelopeInternal(), index); 
				}		
			}
		}

	}



	/**	Returns a Number array of the u and v coordinates, where array[0] = u, array[1] = v
	 * 
	 * @param time
	 * @param depth
	 * @param lat
	 * @param lon
	 * @param interpolate
	 * @return Normal[] array
	 */
	public synchronized Number[] getUV(double time, double depth, double lat, double lon, boolean interpolate)	{
		Number[] uv = new Number[2];
		if (interpolate) {
			if (variables == null){
				System.out.println("NEED to call setVariables() first!");
				System.exit(1); 
			}
			setInterpolationAxes(latVarName, lonVarName);
		}
		uv[0] = getValue(uVarName, new double[] {time, depth, lat, lon}, new boolean[] {false, false, false, false},  interpolate);
		uv[1] = getValue(vVarName, new double[] {time, depth, lat, lon}, new boolean[] {false, false, false, false}, interpolate);
		return uv; 
	}



	/**Returns a netCDF Array -- must know the appropriate array dimension and type of return to cast it.
	 * e.g., variable dimensions are usually (time, depth, lat, lon)  
	 * Can convert to Java array via array.copyToNDJavaArray(), just need to cast appropriately	  
	 * @param varName
	 * @param index
	 * @param time
	 * @return
	 */
	public synchronized Array getArray(String varName)	{

		try {
			return variables.get(varName).read(); 

		} catch (IOException ioe) {
			ioe.printStackTrace();
			System.out.println("catching error " + ioe);
			TestingUtils.dropBreadCrumb(); 

		}
		return null; 
	}


	/**Returns a netCDF Array -- must know the appropriate array dimension and type of return to cast it.
	 * e.g., variable dimensions are usually (time, depth, lat, lon)  
	 * Can convert to Java array via array.copyToNDJavaArray(), just need to cast appropriately	  
	 * @param varName
	 * @param index
	 * @param time
	 * @return
	 */
	public synchronized Array getArray(String varName, int[] origin, int[] shape)	{

		try {
			return variables.get(varName).read(origin, shape).reduce(); 

		} catch (InvalidRangeException e) {
			e.printStackTrace();
			System.out.println("catching error " + e);
			TestingUtils.dropBreadCrumb(); 

		} catch (IOException ioe) {
			ioe.printStackTrace();
			System.out.println("catching error " + ioe);
			TestingUtils.dropBreadCrumb(); 
		}
		return null; 
	}


	/**Returns a netCDF Array -- must know the appropriate array dimension and type of return to cast it.
	 * e.g., variable dimensions are usually (time, depth, lat, lon)  
	 * Can convert to Java array via array.copyToNDJavaArray(), just need to cast appropriately	  
	 * @param varName
	 * @param index
	 * @param time
	 * @return
	 */
	public synchronized Array getArray(String varName, int[] origin, int[] shape, boolean reduce)	{

		try {

			if (reduce) return variables.get(varName).read(origin, shape).reduce();
			else return variables.get(varName).read(origin, shape);

		} catch (InvalidRangeException e) {
			e.printStackTrace();
			System.out.println("catching error " + e);
			TestingUtils.dropBreadCrumb(); 
		} catch (IOException ioe) {
			ioe.printStackTrace();
			System.out.println("catching error " + ioe);
			TestingUtils.dropBreadCrumb(); 
		}
		return null; 
	}



	/**
	 * Returns the index value (or nearest value) for a given search value. This
	 * method assumes that the parameter being accessed is in sorted order (as
	 * should be the case for dimension-type values).
	 * 
	 * NOTE: the value must be within the bounds of the array
	 * values, else will through an ArrayIndexOutOfBoundsException
	 * 
	 * @param ncf
	 * @param varname
	 * @param val
	 * @return
	 */

	public int locate(Variable var, double val) throws IOException {
		double[] ja; 
		int idx;

		//if array has already been store in memory so don't have to re-copy over each time
		if (dimArrays.containsKey(var)){
			ja = dimArrays.get(var);
		}

		//else store the dimension array in memory
		else {
			Array arr;
			// Read the Variable into an Array
			arr = var.read();

			// Convert into a java array
			if (arr.getElementType() == Float.TYPE) {
				float[] temp = (float[]) arr.copyTo1DJavaArray();
				ja = new double[temp.length];
				for (int i=0; i<temp.length; i++){
					ja[i] = (double) temp[i];
				}
				dimArrays.put(var, ja); 
			} 

			else if (arr.getElementType() == Double.TYPE) {
				double[] temp = (double[]) arr.copyTo1DJavaArray();
				ja = new double[temp.length];
				for (int i=0; i<temp.length; i++){
					ja[i] = (double) temp[i];
				}
				dimArrays.put(var, ja); 
			}

			else if (arr.getElementType() == Integer.TYPE) {
				int[] temp = (int[]) arr.copyTo1DJavaArray();
				ja = new double[temp.length];
				for (int i=0; i<temp.length; i++){
					ja[i] = (double) temp[i];
				}
				dimArrays.put(var, ja); 
			}

			else if (arr.getElementType() == Short.TYPE) {
				short[] temp = (short[]) arr.copyTo1DJavaArray();
				ja = new double[temp.length];
				for (int i=0; i<temp.length; i++){
					ja[i] = (double) temp[i];
				}
				dimArrays.put(var, ja); 
			}

			else  {
				long[] temp = (long[]) arr.copyTo1DJavaArray();
				ja = new double[temp.length];
				for (int i=0; i<temp.length; i++){
					ja[i] = (double) temp[i];
				}
				dimArrays.put(var, ja); 
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
				return 0;
			}


			// If not an exact match - determine which value we're closer to
			if (-(idx + 1) >= ja.length) {
				return ja.length-1;
			}

			double spval = (ja[-(idx + 2)] + ja[-(idx + 1)]) / 2d;
			if (val < spval) {
				return -(idx + 2);
			} else {
				return -(idx + 1);
			}
		}

		// Otherwise it's an exact match.
		return idx;
	}

	/**Close the file when done!
	 * 
	 */
	public void closeFile(){
		if (dataFile != null)
			try {
				dataFile.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
	}




	/**Returns the data type of the given variable name string
	 * 
	 * @param varName
	 * @return
	 */
	public String getDataType(String varName){
		Variable dataVar =variables.get(varName);  
		return dataVar.getDataType().toString();
	}

	/**Returns the Netcdf Variable
	 * 
	 * @param varName
	 * @return
	 */
	public Variable getVariable(String varName){
		return variables.get(varName);
	}

	/**Returns the dimension size for a given dimension variable.
	 * Note: if pass a variable name with more than a single dimension,
	 * will return the first dimension.
	 * 
	 * @param dimName
	 * @return
	 */
	public int getSingleDimension(String dimName){
		return variables.get(dimName).getShape(0);
	}

	/**Returns an int[] of dimensions for a given variable.
	 * 
	 * @param dimName
	 * @return
	 */
	public int[] getDimensions(String dimName){
		List<Dimension> dims = variables.get(dimName).getDimensions();
		int[] intDims = new int[dims.size()];
		int counter = 0;
		for (Dimension dim : dims){
			intDims[counter++] = dim.getLength();
		}
		return intDims;
	}



	/**	Sets the Variables given the String variable names if using the general getValue(origin, shape, interpolate) method
	 * 
	 * @param timeVarName
	 * @param depthVarName
	 * @param latVarName
	 * @param lonVarName
	 * @param uVarName
	 * @param vVarName
	 */
	public void setVariables(String...varNames) {
		//for any additional Variables
		if (varNames != null) {
			for (String varName : varNames){
				Variable var = dataFile.findVariable(varName);
				variables.put(varName, var);

				//check if there's a scale factor, and if so, add to the varScaleFactors hashmap
				List<Attribute> list = var.getAttributes();
				for (int i=0; i<list.size(); i++){
					Attribute att = list.get(i);
					if (att.getName().equals("scale_factor")){
						useScaleFactor = true; 
						if (varScaleFactors == null) varScaleFactors = new HashMap<String, Double>();
						varScaleFactors.put(varName, att.getNumericValue().doubleValue()); 
					}
					if (att.getName().equals("add_offset")){
						useAddOffset = true; 
						if (varAddOffsets == null) varAddOffsets = new HashMap<String, Double>();
						varAddOffsets.put(varName, att.getNumericValue().doubleValue()); 
					}

					if (att.getName().equals("missing_value")){
						if (varMissingValue== null) varMissingValue= new HashMap<String, Number>();
						varMissingValue.put(varName, att.getNumericValue()); 
					}
				}
			}
		}
	}

	

	/**Sets the interpolation radius; default is 1 (i.e., does 3x3 array around and including array element)
	 * 
	 * @param interpolRadius
	 */
	public void setInterpolationRadius(int interpRadius) {
		this.interpRadius = interpRadius;
		interpDiameter = 2 * interpRadius + 1; 
	}



	/**Sets the variable names that will be interpolated across in 2D space
	 * 
	 * @param Y_Lat
	 * @param X_Lon
	 */
	public void setInterpolationAxes(String Y_Lat, String X_Lon) {
		this.interpY = Y_Lat;
		this.interpX = X_Lon;
	}


	/**Turns off using the scale_factor if it included as a variable attribute, 
	 * so can access the original data.
	 * 
	 * Note: this MUST be called after the setVariables(String... varNames) method!
	 */
	public void turnOffScaleFactor(){
		this.useScaleFactor = false;
	}



	public boolean isNearNoData() {
		return nearNoData;
	}


	/**Returns the missing value for the given variable name
	 * 
	 * @param varName
	 * @return
	 */
	public Number getMissingValue(String varName){
		return varMissingValue.get(varName);
	}



	/**Returns the minimum longitude
	 * 
	 * @return
	 */
	public double getMinLon(String lonVarName) {
		//if hasn't been set previously, then set here
		if (minLon == Double.MAX_VALUE){
			double cellSize = getValue(lonVarName, new int[] {1}).doubleValue() - getValue(lonVarName, new int[] {0}).doubleValue();
			minLon = getValue(lonVarName, new int[] {0}).doubleValue() - cellSize/2;
		}
		return minLon;
	}


	/**Returns the maximum longitude
	 * 
	 * @return
	 */
	public double getMaxLon(String lonVarName) {
		if (maxLon == Double.MAX_VALUE){
			int lonDim = getSingleDimension(lonVarName); 
			double cellSize = getValue(lonVarName, new int[] {lonDim-1}).doubleValue() - getValue(lonVarName, new int[] {lonDim-2}).doubleValue();
			maxLon = getValue(lonVarName, new int[] {lonDim-1}).doubleValue() + cellSize/2;
		}
		return maxLon;
	}



	/**Returns the minimum latitude
	 * 
	 * @return
	 */
	public double getMinLat(String latVarName) {
		if (minLat == Double.MAX_VALUE){
			double cellSize = getValue(latVarName, new int[] {1}).doubleValue() - getValue(latVarName, new int[] {0}).doubleValue();
			minLat = getValue(latVarName, new int[] {0}).doubleValue() - cellSize/2;
		}
		return minLat;
	}



	/**Returns the maximal latitude
	 * 
	 * @return
	 */
	public double getMaxLat(String latVarName) {
		if (maxLat == Double.MAX_VALUE){
			int latDim = getSingleDimension(latVarName); 
			double cellSize = getValue(latVarName, new int[] {latDim-1}).doubleValue() - getValue(latVarName, new int[] {latDim-2}).doubleValue();
			maxLat = getValue(latVarName, new int[] {latDim-1}).doubleValue() + cellSize/2;
		}
		return maxLat;
	}


	/**Returns the width of the netCDF domain (i.e., max longitude - min longitude)
	 * 
	 * @return
	 */
	public double getDomainWidth(String lonVarName){
		if (domainWidth == Double.MAX_VALUE){
			domainWidth = getMaxLon(lonVarName)-getMinLon(lonVarName); 
		}
		return domainWidth; 
	}


	/**Returns the height of the netCDF domain (i.e., max latitude - min latitude)
	 * 
	 * @return
	 */
	public double getDomainHeight(String latVarName) {
		if (domainHeight == Double.MAX_VALUE){
			domainHeight = getMaxLat(latVarName)-getMinLat(latVarName);
		}
		return domainHeight; 
	}

}



