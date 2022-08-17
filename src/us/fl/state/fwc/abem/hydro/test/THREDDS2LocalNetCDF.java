package us.fl.state.fwc.abem.hydro.test;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayShort;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import us.fl.state.fwc.abem.ThreadService;

public class THREDDS2LocalNetCDF {

	boolean output2console = false; 

	List<String> varNames = new ArrayList<String>();  //holds the variable names: usually, will be lat, lon, time, depth, u, v, salinity, and temp
	HashMap<Variable, List<Attribute>> varMap = new HashMap<Variable, List<Attribute>>(); 
	double lonMin, lonMax, latMin, latMax; 

	NumberFormat nf = NumberFormat.getInstance(); 

	String filename= "http://edac-dap.northerngulfinstitute.org/thredds/dodsC/ncom/amseas/ncom_relo_amseas_2010072900/ncom_relo_amseas_2010072900_t096.nc"; //"c:\\work\\temp\\ncom_relo_amseas_2010072500_t084.nc" ; //"dataTest/v3d_2010051500.nc"; 
	String baseURL = "http://edac-dap.northerngulfinstitute.org/thredds/dodsC/ncom/amseas/";
	String nameURL = "ncom_relo_amseas_";

	String baseOutfile = "c:\\work\\data\\NCOM_AS\\"; 
	//String nameOutfile = "nameURL"; 

	//NOTE: with Java's Gregorian calendar, the month is in array index (i.e., Jan = 0, Dec=11)
	Calendar startDate = new GregorianCalendar(2010, 6, 15); // this is JULY 1st!
	Calendar endDate = new GregorianCalendar(2010, 7, 7); 

	String timeVarName = "time", depthVarName = "depth", 
	latVarName = "lat", lonVarName = "lon", 
	elevVarName = "surf_el", 
	uVarName = "water_u", vVarName = "water_v", 
	salVarName = "salinity", tempVarName = "water_temp";  

	int timeDim = 8, latDim = 266, lonDim = 319, depthDim = 40; 

	public THREDDS2LocalNetCDF(){
		varNames.add(timeVarName);
		varNames.add(depthVarName);
		varNames.add(latVarName);
		varNames.add(lonVarName);
		varNames.add(elevVarName);
		varNames.add(uVarName);
		varNames.add(vVarName);
		varNames.add(salVarName);
		varNames.add(tempVarName);

		//(lat > 23.5) && (lat < 30.7) && (lon>-87.6) && (lon < -78.5)
		lonMin  =-87.6;
		lonMax = -78.5;
		latMin = 23.5;
		latMax = 30.7;
	}
 

	public void IO(){
		System.out.println("starting up.");
		@SuppressWarnings("unused")
		long startTime =0; 
		ThreadService service = new ThreadService();

		NetcdfFile inputFile = null;


		try {

			//|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| Get variables and attributes |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||

			inputFile = NetcdfDataset.openFile(filename, null);
			List<Variable> varList = inputFile.getVariables();

			for (int i=0; i<varList.size(); i++){
				Variable var = varList.get(i);
				//only pull the ones that we want
				if (varNames.contains(var.getName())){
					varMap.put(var, var.getAttributes());
				}
				//System.out.println("variable name: " + var.getName() + "\tvariable dataType: " + var.getDataType());
			}




			String dateURL;

			//|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| loop over all days to record |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| 
			while (startDate.before(endDate)) {
				if (output2console) startTime = System.currentTimeMillis();
				int month = startDate.get(Calendar.MONTH) + 1;  
				int day = startDate.get(Calendar.DATE);  
				int year = startDate.get(Calendar.YEAR);

				nf.setMinimumIntegerDigits(2); //set the number format to 2 integers (e.g., 01, 02, ...10, 11)
				dateURL = year+nf.format(month)+nf.format(day)+"00"; 


				//===================== Create local output file for each day  ===================== 
				NetcdfFileWriteable ncFile =NetcdfFileWriteable.createNew(baseOutfile + nameURL + dateURL + ".nc");
				ncFile.setFill(true);	// this will fill in with blank values, as below
				ncFile.setLargeFile(true);
				/*
				long approxSize = (long) timeDim * depthDim * latDim * lonDim * 2 + //water_u 
				timeDim * depthDim * latDim * lonDim * 2 + //water_v
				timeDim * depthDim * latDim * lonDim * 2 + //salinity
				timeDim * depthDim * latDim * lonDim * 2 + // temp
				timeDim * latDim * lonDim * 2 + //surf_el
				timeDim * 4 +
				depthDim * 4 +
				latDim * 4 +
				lonDim * 4 +
				4000;

				ncFile.setLength(approxSize);
*/

				//NEED to list these all out here because will be different than file with respect to dimensions
				Dimension[] dim = new Dimension[4];
				dim[0]  = ncFile.addDimension(timeVarName, timeDim); //time per local netCDF file; will create a new file for each day
				dim[1]  = ncFile.addDimension(depthVarName, depthDim); 
				dim[2] = ncFile.addDimension(latVarName, latDim); //should be correct for the bounding box of (lat > 23.5) && (lat < 30.7) && (lon>-87.6) && (lon < -78.5)
				dim[3] = ncFile.addDimension(lonVarName, lonDim); //dito

				ncFile.addVariable(timeVarName, DataType.DOUBLE, new Dimension[] {dim[0]});
				ncFile.addVariable(depthVarName, DataType.DOUBLE, new Dimension[] {dim[1]});
				ncFile.addVariable(latVarName, DataType.DOUBLE, new Dimension[] {dim[2]});
				ncFile.addVariable(lonVarName, DataType.DOUBLE, new Dimension[] {dim[3]});

				//NOTE: is a scale factor here of 0.001, so write as short's to save mem
				ncFile.addVariable(elevVarName, DataType.SHORT, new Dimension[] {dim[0], dim[2], dim[3]});
				ncFile.addVariable(uVarName, DataType.SHORT, dim);
				ncFile.addVariable(vVarName, DataType.SHORT, dim);
				ncFile.addVariable(tempVarName, DataType.SHORT, dim);
				ncFile.addVariable(salVarName, DataType.SHORT, dim);

				Iterator<Variable> iterator = varMap.keySet().iterator();
				while( iterator. hasNext() ){
					Variable key = (Variable) iterator.next(); 
					List<Attribute> list = varMap.get(key); 

					//add all attributes for each variable
					for (int j=0; j<list.size(); j++){
						if (list.get(j).getDataType().toString().equals("String")) {
							ncFile.addVariableAttribute(key.getName(), list.get(j).getName(), list.get(j).getStringValue());	
						}
						else ncFile.addVariableAttribute(key.getName(), list.get(j).getName(), list.get(j).getNumericValue());

					}
				}

				//only call create after defintions are done (i.e., move from "Define" mode and in write mode; see API)
				ncFile.create();



				// ================== loop over 24hr time steps for each day ==================				
				String timeURL, url;
				nf.setMinimumIntegerDigits(3); //reset the NumberFormat to 3 integer places

				if (output2console) System.out.println("Beginning netCDF writting at time 0"); 
				int timeCounter = 0; 
				for (int t=0; t<24; t+=3){ //don't include 24!!!!
					timeURL = "_t" + nf.format(t) + ".nc";  
					url = baseURL+nameURL+dateURL+"/"+nameURL+dateURL+timeURL; 

					inputFile = NetcdfDataset.openFile(url, null);    

					//for (int vars=0; vars < varMap.size(); vars++ ){
					double lat, lon; 
					int latOrigin=-1, lonOrigin=-1; 

					//get dimension variables
					Variable timeVar = inputFile.findVariable(timeVarName ); 
					Variable depthVar = inputFile.findVariable(depthVarName); 
					Variable latVar = inputFile.findVariable(latVarName); 
					Variable lonVar = inputFile.findVariable(lonVarName); 
					Variable elevVar = inputFile.findVariable(elevVarName);
					Variable uVar = inputFile.findVariable(uVarName);
					Variable vVar = inputFile.findVariable(vVarName);
					Variable salVar = inputFile.findVariable(salVarName);
					Variable tempVar = inputFile.findVariable(tempVarName); 

					ArrayDouble.D1 timeArray = (ArrayDouble.D1) timeVar.read(); 
					ArrayDouble.D1 depthArray = (ArrayDouble.D1) depthVar.read(); 
					ArrayDouble.D1 latArray = (ArrayDouble.D1) latVar.read(); 
					ArrayDouble.D1 lonArray = (ArrayDouble.D1) lonVar.read(); 


					//loop through all Variables that are recording,

					//========================  time ========================  
					ncFile.write(timeVarName, new int[]{timeCounter}, timeArray); //origin in the timeCounter, so increment after set 
					if (output2console) System.out.println("finished write time @ t (hr)=" + t); 

					//========================  depth ======================== 
					ncFile.write(depthVarName, depthArray); 
					if (output2console) System.out.println("finished write depth @ t (hr)=" + t); 

					//========================  lat ========================  
					ArrayDouble.D1 latData = new ArrayDouble.D1(latDim);
					int counter = 0;
					for (int i = 0; i < latVar.getShape(0); i++){
						//outFile.println(latArray.get(i,j) + "\t" + lonArray.get(i,j)); 
						lat = latArray.get(i);
						if ( (lat > 23.5) && (lat < 30.7)){
							if (latOrigin<0) latOrigin = i;
							latData.set(counter++, lat); 
						}
					}
					ncFile.write(latVarName, latData);
					if (output2console) System.out.println("finished write lat @ t (hr)=" + t); 

					//========================  lon ========================  
					ArrayDouble.D1 lonData = new ArrayDouble.D1(lonDim);
					counter = 0;
					for (int i = 0; i < lonVar.getShape(0); i++){
						//outFile.println(latArray.get(i,j) + "\t" + lonArray.get(i,j)); 
						lon = lonArray.get(i) - 360.0; 
						if ( (lon>-87.6) && (lon < -78.5)){
							if (lonOrigin<0) lonOrigin = i;
							lonData.set(counter++, lon); 
						}
					}
					ncFile.write(lonVarName, lonData);
					if (output2console) System.out.println("finished write lon @ t (hr)=" + t); 

					//set up a threadService to do the reading
					List<Runnable> list = new ArrayList<Runnable>(); 

					THREDDSReadTask elevTask = new THREDDSReadTask(service, elevVar, 0+":"+0+":1,"+  latOrigin+":"+(latOrigin+latDim-1)+":1,"+  lonOrigin+":"+(lonOrigin+lonDim-1)+":1" );
					THREDDSReadTask uTask =new THREDDSReadTask(service, uVar, 0+":"+0+":1,"+  0+":"+(depthDim-1)+":1,"+  latOrigin+":"+(latOrigin+latDim-1)+":1,"+  lonOrigin+":"+(lonOrigin+lonDim-1)+":1"); 
					THREDDSReadTask vTask =new THREDDSReadTask(service, vVar, 0+":"+0+":1,"+  0+":"+(depthDim-1)+":1,"+  latOrigin+":"+(latOrigin+latDim-1)+":1,"+  lonOrigin+":"+(lonOrigin+lonDim-1)+":1"); 
					THREDDSReadTask salTask =new THREDDSReadTask(service, salVar,  0+":"+0+":1,"+  0+":"+(depthDim-1)+":1,"+  latOrigin+":"+(latOrigin+latDim-1)+":1,"+  lonOrigin+":"+(lonOrigin+lonDim-1)+":1" ); 
					THREDDSReadTask tempTask =new THREDDSReadTask(service, tempVar,  0+":"+0+":1,"+  0+":"+(depthDim-1)+":1,"+  latOrigin+":"+(latOrigin+latDim-1)+":1,"+  lonOrigin+":"+(lonOrigin+lonDim-1)+":1" ); 

					list.add(elevTask);
					list.add(uTask);
					list.add(vTask);
					list.add(salTask);
					list.add(tempTask);

					int currentSize = list.size(); 
					service.setLatch(currentSize);

					Iterator<Runnable> it = list.iterator(); 
					while (it.hasNext()){
						service.addTask(it.next());  
					}
					service.await(); 

					inputFile.close(); //close after done reading
					
					//========================  surf_el ========================  
					ArrayShort.D3 elevData = (ArrayShort.D3) elevTask.getArray(); //elevVar.read(0+":"+0+":1,"+  latOrigin+":"+(latOrigin+latDim-1)+":1,"+  lonOrigin+":"+(lonOrigin+lonDim-1)+":1");
					int[] elevOrigin = new int[]{timeCounter, 0, 0};
					ncFile.write(elevVarName, elevOrigin , elevData);
					if (output2console) System.out.println("finished write elev @ t (hr)=" + t); 

					//========================  u ========================  
					ArrayShort.D4 uData = (ArrayShort.D4) uTask.getArray(); //uVar.read(0+":"+0+":1,"+  0+":"+(depthDim-1)+":1,"+  latOrigin+":"+(latOrigin+latDim-1)+":1,"+  lonOrigin+":"+(lonOrigin+lonDim-1)+":1");
					int[] uOrigin = new int[]{timeCounter, 0, 0, 0};
					ncFile.write(uVarName, uOrigin, uData);
					if (output2console) System.out.println("finished write u velocity @ t (hr)=" + t); 

					//========================  v ========================  
					ArrayShort.D4 vData = (ArrayShort.D4) vTask.getArray(); //vVar.read(0+":"+0+":1,"+  0+":"+(depthDim-1)+":1,"+  latOrigin+":"+(latOrigin+latDim-1)+":1,"+  lonOrigin+":"+(lonOrigin+lonDim-1)+":1");
					int[] vOrigin = new int[]{timeCounter, 0, 0, 0};
					ncFile.write(vVarName, vOrigin, vData);
					if (output2console) System.out.println("finished write v velocity @ t (hr)=" + t); 

					//========================  salinity  ========================  
					ArrayShort.D4 salData = (ArrayShort.D4) salTask.getArray(); //salVar.read(0+":"+0+":1,"+  0+":"+(depthDim-1)+":1,"+  latOrigin+":"+(latOrigin+latDim-1)+":1,"+  lonOrigin+":"+(lonOrigin+lonDim-1)+":1");
					int[] salOrigin = new int[]{timeCounter, 0, 0, 0};
					ncFile.write(salVarName, salOrigin, salData);
					if (output2console) System.out.println("finished write salinity @ t (hr)=" + t); 

					//========================  temperature  ========================  
					ArrayShort.D4 tempData = (ArrayShort.D4) tempTask.getArray(); //tempVar.read(0+":"+0+":1,"+  0+":"+(depthDim-1)+":1,"+  latOrigin+":"+(latOrigin+latDim-1)+":1,"+  lonOrigin+":"+(lonOrigin+lonDim-1)+":1");
					int[] tempOrigin = new int[]{timeCounter, 0, 0, 0};
					ncFile.write(tempVarName, tempOrigin, tempData);
					if (output2console) System.out.println("finished write temp @ t (hr)=" + t); 


					timeCounter++; 
					System.out.println("finished: day " + dateURL + ", hour " + t + " done."); 
				}//end for loop over time steps (24 hrs)

				//close local file after all time steps in a day (8) are written
				ncFile.close();

				startDate.add(Calendar.DAY_OF_YEAR, 1); // add a day to the date
			} // while loop over start and end dates
//			double runTime = 0; 
//			if (output2console) runTime = ( ( (double) System.currentTimeMillis() - (double) startTime)/1000.0) * 60.0;  
//			if (output2console) System.out.println("time (min) to copy 1 day: " + runTime); 



		} catch (java.io.IOException e) {
			System.out.println(" fail = "+e);
			e.printStackTrace();
		} catch (InvalidRangeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (inputFile != null )
				try {
					inputFile.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
		}

		service.shutdown();
	}



	public static void main(String[] args) {

		THREDDS2LocalNetCDF frc = new THREDDS2LocalNetCDF();
		frc.IO(); 

	}

}

class THREDDSReadTask implements Runnable{

	ThreadService service; 
	Variable var;
	String readAccess;
	Array array; 

	public THREDDSReadTask(ThreadService service, Variable var, String readAccess){
		this.service = service;
		this.var = var;
		this.readAccess=readAccess;
	}

	public Array getArray(){
		return array;
	}

	@Override
	public void run() {
		
		try {
			array = var.read(readAccess);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidRangeException e) {
			e.printStackTrace();
		}

		//service.releaseSemaphore(); 
	}

}
