package us.fl.state.fwc.abem.hydro.ncom;

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

/**Copies NCOM-American Seas nowcast/forecast data from the edac-dap.northerngulfinstitute.org THREDDS server and stores in a local directory,
 * for the given dates and times.  
 * 
 * Note: because this is a nowcast/forecast model, only copies the 0-24hr data for each day. If data is missing, then can copy 
 * 
 * @author wade.cooper
 *
 */
public class FTP2LocalNetCDF {

	int startYear = 2010;
	int startMonth = 9;
	int startDay = 13;
	int endYear = 2010;
	int endMonth = 9;
	int endDay = 22;

	int startHour = 0;
	int endHour = 24;

	String filename= "C:\\work\\data\\NCOM_AS\\TEMP\\ncom_relo_amseas_2010091300_t000.nc" ; // "http://edac-dap.northerngulfinstitute.org/thredds/dodsC/ncom/amseas/ncom_relo_amseas_2010072900/ncom_relo_amseas_2010072900_t096.nc"; //"c:\\work\\temp\\ncom_relo_amseas_2010072500_t084.nc" ; //"dataTest/v3d_2010051500.nc"; 
	String baseURL = "c:\\work\\data\\NCOM_AS\\TEMP\\"; //"http://edac-dap.northerngulfinstitute.org/thredds/dodsC/ncom/amseas/";
	String nameURL = "ncom_relo_amseas_";
	String baseOutfile = "c:\\work\\data\\NCOM_AS\\"; 



	boolean output2console = false; 

	List<String> varNames = new ArrayList<String>();  //holds the variable names: usually, will be lat, lon, time, depth, u, v, salinity, and temp
	HashMap<Variable, List<Attribute>> varMap = new HashMap<Variable, List<Attribute>>(); 
	double lonMin, lonMax, latMin, latMax; 

	NumberFormat nf = NumberFormat.getInstance(); 



	//NOTE: with Java's Gregorian calendar, the month is in array index (i.e., Jan = 0, Dec=11)
	Calendar startDate = new GregorianCalendar(startYear, startMonth-1, startDay); // 2010-6-1:  this is JULY 1st!
	Calendar endDate = new GregorianCalendar(endYear, endMonth-1, endDay); 

	String timeVarName = "time", depthVarName = "depth", 
	latVarName = "lat", lonVarName = "lon", 
	elevVarName = "surf_el", 
	uVarName = "water_u", vVarName = "water_v", 
	salVarName = "salinity", tempVarName = "water_temp";  

	int timeDim = 8, latDim = 266, lonDim = 319, depthDim = 40; 

	public FTP2LocalNetCDF(	){

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

	public static void main(String[] args) {

		FTP2LocalNetCDF frc = new FTP2LocalNetCDF();
		frc.IO(); 

	}



	public void FTPDownload(){
		/* See Apache Commons FTPClient
		 * http://commons.apache.org/net/api/org/apache/commons/net/ftp/FTPClient.html
		 * 
		 */
	}


	public void unzip(){
		/* See java.util.zip
		 * 
		 * - can do GZIP 
		 * 
		 * Need to use the com.ice.tar package for tar files however
		 * 
		 * \
		 * import java.io.*;
import com.ice.tar.*;
import javax.activation.*;
import java.util.zip.GZIPInputStream;

public class Extract_TAR_GZ_FILE {

	public static InputStream getInputStream(String tarFileName) throws Exception{

      if(tarFileName.substring(tarFileName.lastIndexOf(".") + 1, tarFileName.lastIndexOf(".") + 3).equalsIgnoreCase("gz")){
         System.out.println("Creating an GZIPInputStream for the file");
         return new GZIPInputStream(new FileInputStream(new File(tarFileName)));

      }else{
         System.out.println("Creating an InputStream for the file");
         return new FileInputStream(new File(tarFileName));
      }
   }

	private static void untar(InputStream in, String untarDir) throws IOException {

	  System.out.println("Reading TarInputStream... ");
      TarInputStream tin = new TarInputStream(in);
      TarEntry tarEntry = tin.getNextEntry();
      if(new File(untarDir).exists()){
	      while (tarEntry != null){
	         File destPath = new File(untarDir + File.separatorChar + tarEntry.getName());
	         System.out.println("Processing " + destPath.getAbsoluteFile());
	         if(!tarEntry.isDirectory()){
	            FileOutputStream fout = new FileOutputStream(destPath);
	            tin.copyEntryContents(fout);
	            fout.close();
	         }else{
	            destPath.mkdir();
	         }
	         tarEntry = tin.getNextEntry();
	      }
	      tin.close();
      }else{
         System.out.println("That destination directory doesn't exist! " + untarDir);
      }

	}

	private void run(){

		try {			
			String strSourceFile = "G:/source/BROKERH_20080303_A2008_S0039.TAR.GZ";
			String strDest = "G:/source/Extracted Files";
			InputStream in = getInputStream(strSourceFile);

			untar(in, strDest);		

		}catch(Exception e) {

			e.printStackTrace();		
			System.out.println(e.getMessage());
		}
	}	

	public static void main(String[] strArgs) throws IOException{
		new Extract_TAR_GZ_FILE().run();
	}
}
		 * 
		 */
	}


	public void IO(){
		System.out.println("starting up.");
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




			String inDateURL, outDateURL;

			//|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| loop over all days to record |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| 
			while (startDate.before(endDate)) {
				int month = startDate.get(Calendar.MONTH) + 1;  
				int day = startDate.get(Calendar.DATE);  
				int year = startDate.get(Calendar.YEAR);

				nf.setMinimumIntegerDigits(2); //set the number format to 2 integers (e.g., 01, 02, ...10, 11)


				//set the output date URL to reflect future days is using 24hr + forecasts
				if (startHour == 24) outDateURL = year+nf.format(month)+nf.format(day+1)+"00";
				else if (startHour == 48) outDateURL = year+nf.format(month)+nf.format(day+2)+"00";
				else outDateURL = year+nf.format(month)+nf.format(day)+"00"; 

				//set the input date URL to reflect the start date
				inDateURL = year+nf.format(month)+nf.format(day)+"00";

				//===================== Create local output file for each day  ===================== 

				String filename = baseOutfile + nameURL + outDateURL + ".nc"; 
				NetcdfFileWriteable ncFile =NetcdfFileWriteable.createNew(filename);
				ncFile.setFill(true);	// this will fill in with blank values, as below
				//ncFile.setLargeFile(true); //NOTE: for whatever reason, when I set this = true, I cant open file in ncBrowse!!!
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
					url = baseURL+nameURL+inDateURL+timeURL; 

					int counter2 = 1; 

					boolean fileExists = false;

					//loop until find a file that exists since are missing some single days here and there
					//if a file is missing, then use the previous day's forecast of time+24hrs
					while (!fileExists){
						try {
							inputFile = NetcdfDataset.openFile(url, null);
							fileExists = true; 
						}
						// catch for instances where are missing a day's file, and add 
						catch ( IOException e){
							nf.setMinimumIntegerDigits(3); 
							timeURL = "_t" + nf.format(t+24*counter2) + ".nc";  
							nf.setMinimumIntegerDigits(2); 
							inDateURL = year+nf.format(month)+nf.format(day-counter2)+"00";
							url = baseURL+nameURL+inDateURL+timeURL; 
							counter2++; 
						}
						if (counter2>5){
							System.out.println("Too many missing files!  Exiting system");
							System.exit(1); 
						}
					}

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
/*					List<Runnable> list = new ArrayList<Runnable>(); 

					THREDDSReadTask2 elevTask = new THREDDSReadTask2(service, elevVar, 0+":"+0+":1,"+  latOrigin+":"+(latOrigin+latDim-1)+":1,"+  lonOrigin+":"+(lonOrigin+lonDim-1)+":1" );
					THREDDSReadTask2 uTask =new THREDDSReadTask2(service, uVar, 0+":"+0+":1,"+  0+":"+(depthDim-1)+":1,"+  latOrigin+":"+(latOrigin+latDim-1)+":1,"+  lonOrigin+":"+(lonOrigin+lonDim-1)+":1"); 
					THREDDSReadTask2 vTask =new THREDDSReadTask2(service, vVar, 0+":"+0+":1,"+  0+":"+(depthDim-1)+":1,"+  latOrigin+":"+(latOrigin+latDim-1)+":1,"+  lonOrigin+":"+(lonOrigin+lonDim-1)+":1"); 
					THREDDSReadTask2 salTask =new THREDDSReadTask2(service, salVar,  0+":"+0+":1,"+  0+":"+(depthDim-1)+":1,"+  latOrigin+":"+(latOrigin+latDim-1)+":1,"+  lonOrigin+":"+(lonOrigin+lonDim-1)+":1" ); 
					THREDDSReadTask2 tempTask =new THREDDSReadTask2(service, tempVar,  0+":"+0+":1,"+  0+":"+(depthDim-1)+":1,"+  latOrigin+":"+(latOrigin+latDim-1)+":1,"+  lonOrigin+":"+(lonOrigin+lonDim-1)+":1" ); 

					list.add(elevTask);
					list.add(uTask);
					list.add(vTask);
					list.add(salTask);
					list.add(tempTask);

					service.setLatch(list.size());

					Iterator<Runnable> it = list.iterator(); 
					while (it.hasNext()){
						service.addTask(it.next());  
					}
					service.await(); 

					
*/
					//========================  surf_el ========================  
					ArrayShort.D3 elevData = (ArrayShort.D3) /*elevTask.getArray(); //*/ elevVar.read(0+":"+0+":1,"+  latOrigin+":"+(latOrigin+latDim-1)+":1,"+  lonOrigin+":"+(lonOrigin+lonDim-1)+":1");
					int[] elevOrigin = new int[]{timeCounter, 0, 0};
					ncFile.write(elevVarName, elevOrigin , elevData);
					if (output2console) System.out.println("finished write elev @ t (hr)=" + t); 

					//========================  u ========================  
					ArrayShort.D4 uData = (ArrayShort.D4) /*uTask.getArray(); //*/ uVar.read(0+":"+0+":1,"+  0+":"+(depthDim-1)+":1,"+  latOrigin+":"+(latOrigin+latDim-1)+":1,"+  lonOrigin+":"+(lonOrigin+lonDim-1)+":1");
					int[] uOrigin = new int[]{timeCounter, 0, 0, 0};
					ncFile.write(uVarName, uOrigin, uData);
					if (output2console) System.out.println("finished write u velocity @ t (hr)=" + t); 

					//========================  v ========================  
					ArrayShort.D4 vData = (ArrayShort.D4) /*vTask.getArray(); //*/ vVar.read(0+":"+0+":1,"+  0+":"+(depthDim-1)+":1,"+  latOrigin+":"+(latOrigin+latDim-1)+":1,"+  lonOrigin+":"+(lonOrigin+lonDim-1)+":1");
					int[] vOrigin = new int[]{timeCounter, 0, 0, 0};
					ncFile.write(vVarName, vOrigin, vData);
					if (output2console) System.out.println("finished write v velocity @ t (hr)=" + t); 

					//========================  salinity  ========================  
					ArrayShort.D4 salData = (ArrayShort.D4) /*salTask.getArray(); //*/ salVar.read(0+":"+0+":1,"+  0+":"+(depthDim-1)+":1,"+  latOrigin+":"+(latOrigin+latDim-1)+":1,"+  lonOrigin+":"+(lonOrigin+lonDim-1)+":1");
					int[] salOrigin = new int[]{timeCounter, 0, 0, 0};
					ncFile.write(salVarName, salOrigin, salData);
					if (output2console) System.out.println("finished write salinity @ t (hr)=" + t); 

					//========================  temperature  ========================  
					ArrayShort.D4 tempData = (ArrayShort.D4) /*tempTask.getArray(); //*/ tempVar.read(0+":"+0+":1,"+  0+":"+(depthDim-1)+":1,"+  latOrigin+":"+(latOrigin+latDim-1)+":1,"+  lonOrigin+":"+(lonOrigin+lonDim-1)+":1");
					int[] tempOrigin = new int[]{timeCounter, 0, 0, 0};
					ncFile.write(tempVarName, tempOrigin, tempData);
					if (output2console) System.out.println("finished write temp @ t (hr)=" + t); 

					inputFile.close(); //close after done reading
					
					timeCounter++; 
					System.out.println("finished: day " + inDateURL + ", time " + timeURL + " done."); 
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





}

class THREDDSReadTask2 implements Runnable{

	ThreadService service; 
	Variable var;
	String readAccess;
	Array array; 

	public THREDDSReadTask2(ThreadService service, Variable var, String readAccess){
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
