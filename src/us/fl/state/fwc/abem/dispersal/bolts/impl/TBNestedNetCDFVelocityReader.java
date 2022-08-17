package us.fl.state.fwc.abem.dispersal.bolts.impl;

import java.io.IOException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import us.fl.state.fwc.abem.dispersal.bolts.VelocityReader;
import us.fl.state.fwc.util.TestingUtils;
import us.fl.state.fwc.util.geo.NetCDFFile;

public class TBNestedNetCDFVelocityReader implements VelocityReader, Cloneable {

	//private static DecimalFormat df = new DecimalFormat("#.##"); 
	private static DateFormat datef = new SimpleDateFormat("yyyy-MM-dd");

	//private double u, v;
	private String freqUnits = "Days";
	//private final int io = 1;
	//private final int windim = 2 * io + 1;
	//private float cutoff = 1E3f;

	private NetCDFFile EFDCFile; //use this for EFDC
	private NetCDFFile EFDCBathFile; //use this for EFDC
	private NetCDFFile EFDCLandFile; //land mask file
	private NetCDFFile NCOMFile;
	private double oldNCOMTime = Double.MIN_VALUE; 

	//private boolean ncFileOpened = false; 
	//private Variable uVar, vVar;
	//private Array uArr, vArr;
	//private ShapefileHabitat sh = new ShapefileHabitat();
	private final double[] NODATA = { Double.NaN, Double.NaN };
	private String latName = "lat";
	private String lonName = "lon";
	private String kName = "depth";
	private String tName = "time";
	private String bathName = "water_depth";
	private String landName = "landMask"; 
	private boolean negOceanCoord = true;
	private boolean negPolyCoord = true;
	private boolean nearNoData = false;
	private String uName = "water_u";
	private String vName= "water_v";

	String EFDCDirectory = "c:\\work\\workspace\\EFDC\\TampaToSarasota_WGS\\";
	String cornersInFile = EFDCDirectory + "corners.inp"; 
	String dxdyInFile = EFDCDirectory + "dxdy.inp";
	String boundCellsFilename = EFDCDirectory + "EFDCBoundCells.txt";

	String EFDCFileName = EFDCDirectory + "TB2SB_WGS_070110-092610.nc";
	String EFDCBathFileName = EFDCDirectory + "TB2SB_WGS_Bathy.nc";
	String EFDCLandFileName = EFDCDirectory + "TB2SB_WGS_LandMaskForShapefileBarrier.nc";

	long EFDCTimeOffset = 410227200000l;
	long NCOMTimeOffset = 946684800000l; 

	//EFDCGrid grid;

	public boolean useEFDC = true; 
	//NCOMSettings
	String dateURL;
	String NCOMbaseFile = "c:\\work\\data\\NCOM_AS\\";
	String NCOMnameURL = "ncom_relo_amseas_";
	String EFDCbaseFile = "C:\\work\\workspace\\EFDC\\TampaToSarasota_WGS\\ncFiles\\";
	String EFDCnameURL = "TB2SB_WGS_"; 
	
	NumberFormat nf = NumberFormat.getInstance(); 



	public void initialize(){

		try {
			EFDCBathFile = new NetCDFFile(EFDCBathFileName); 
			EFDCLandFile = new NetCDFFile(EFDCLandFileName); 
		} catch (IOException e) {
			System.out.println("catching error " + e);
			TestingUtils.dropBreadCrumb(); 
		}

		EFDCBathFile.setInterpolationAxes(latName, lonName);
		EFDCBathFile.setVariables(latName, lonName, bathName); 
		EFDCLandFile.setVariables(latName, lonName, landName);
	}



	/*
	 * Returns the proper netCDF file to use, either NCOM or EFDC based on position
	 */
	public NetCDFFile getCorrectDataFile(long time, double lon, double lat){
		//****** time is in Java milliseconds (i.e., sinc 1/1/1970)


		//||||||||||||||||||||||||||||| NOTE ||||||||||||||||||||||||||||||
		//	need to set this for specific EFDC model that are using

		//check if the particle position is to the left of the diagonal line of the EFDC model
		//first, get the slope of a line: y = -1x - 55.2932
		//tempLat is the point on the diagonal at the correct lon value
		double tempLat = -1*lon -55.2932;
		
		//get the right file based on lat, lon, and time; not, EFDC will have all times, but NCOM is broken down by day
		if ( 	(lon < -82.8272 + 0.002) 						||   //checks to see if west of EFDC western boundary, plus 3 rows to avoid boundary layer 
				(lat  < tempLat) 										||	//checks the diagonal section to SW
				(lat < 27.282)  										|| 	//checks the south
				( (lon < -82.7472) && (lat > 27.8956) ) 	||	//checks the N & NW sections
				 (lat < 27.2739)   ) 									{ 	//check the S sections

			double lastNCOMTime = 0;
			if (NCOMFile != null) NCOMFile.getValue(tName, new int[] {NCOMFile.getSingleDimension(tName)-1}).doubleValue(); 
			double NCOMTime = convertToNCOMTime(time); 

			//check to see if need to open a new NCOM NetCDFFile  
			if ( NCOMTime > lastNCOMTime+1.5 ) {
				oldNCOMTime = NCOMTime; 

				GregorianCalendar date = new GregorianCalendar(TimeZone.getTimeZone("GMT")); 
				date.setTimeInMillis(time); 

				int month = date.get(Calendar.MONTH) + 1;  
				int day = date.get(Calendar.DATE);  
				int year = date.get(Calendar.YEAR);

				nf.setMinimumIntegerDigits(2); //set the number format to 2 integers (e.g., 01, 02, ...10, 11)
				dateURL = year+nf.format(month)+nf.format(day)+"00"; 

				try {
					if (NCOMFile != null) NCOMFile.closeFile();
					NCOMFile = new NetCDFFile(NCOMbaseFile + NCOMnameURL + dateURL + ".nc");
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("can't open " + NCOMbaseFile + NCOMnameURL + dateURL + ".nc");
					
				}

				NCOMFile.setInterpolationAxes(latName, lonName);
				NCOMFile.setVariables(tName, kName, latName, lonName, uName, vName); 
			}
			useEFDC = false;
			return NCOMFile; 
		} //check if use NCOM
		
		
		//ELSE, use EFDC
		else {
			
			double lastEFDCTime = 0;
			if (EFDCFile != null) lastEFDCTime = EFDCFile.getValue(tName, new int[] {EFDCFile.getSingleDimension(tName)-1}).doubleValue(); 
			double EFDCTime = convertToEFDCTime(time); 

			//check to see if need to open a new NCOM NetCDFFile  
			if ( EFDCTime > lastEFDCTime+0.5 ) {

				GregorianCalendar date = new GregorianCalendar(TimeZone.getTimeZone("GMT")); 
				date.setTimeInMillis(time); 

				int month = date.get(Calendar.MONTH) + 1;  
				int day = date.get(Calendar.DATE);  
				int year = date.get(Calendar.YEAR);

				nf.setMinimumIntegerDigits(2); //set the number format to 2 integers (e.g., 01, 02, ...10, 11)
				dateURL = year+nf.format(month)+nf.format(day)+"00"; 

				try {
					if (EFDCFile != null) EFDCFile.closeFile();
					EFDCFile = new NetCDFFile(EFDCbaseFile + EFDCnameURL + dateURL + ".nc");
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("can't open " + EFDCbaseFile + EFDCnameURL + dateURL + ".nc");
				}

				EFDCFile.setInterpolationAxes(latName, lonName);
				EFDCFile.setVariables(tName, kName, latName, lonName, uName, vName); 
			
			}
			useEFDC = true; 
			return EFDCFile; 
		}
	}


	public double convertToNCOMTime(long time){
		//NCOM is hours since 1/1/2000
		return (double) (time-NCOMTimeOffset) / (1000*60*60); 
	}

	public double convertToEFDCTime(long time){
		//EFDC is hours since 1/1/1983 (tidal epoch)
		return (double) (time-EFDCTimeOffset) / (1000*60*60); 
	}

	public double getEFDCDepth(double depth){


		return depth; 
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see lagrange.VelocityReader#getUV(long, float, float, float)
	 */

	public synchronized double[] getUV(long time, double z, double lon, double lat) {
		
		long startTime = System.currentTimeMillis();

		
		
		nearNoData = false; 
		if(negOceanCoord){
			if(lon>180){lon = -(360-lon);}
		}

		//gets the proper ncFile to use for this method call
		NetCDFFile ncFile = getCorrectDataFile(time, lon, lat);

		//if for whatever reason the ncFile comes back as null, then return null and have 
		//particle set to lost
		if (ncFile == null) return null; 
		
		//will set the depth as a standardized value
		double timeConv=0;

		if (useEFDC) {
			float waterDepth = Math.abs(EFDCBathFile.getValue(bathName, new double[]{lat, lon}, new boolean[]{false, false}, true).floatValue()); 
			if (z > waterDepth) z= waterDepth; //catch this in case it gets set to deeper than depth
			z = z/waterDepth; //1- z/waterDepth; //standardize z to 0-1 since sigma vertical grid
			timeConv = convertToEFDCTime(time);
		}
		else timeConv = convertToNCOMTime(time);

		double[] uv = new double[2];


		if (useEFDC) {
			Double u = (Double) ncFile.getValue(uName, new double[] {timeConv, z, lat, lon}, new boolean[] {false, false, false, false},  true);
			Double v = (Double) ncFile.getValue(vName, new double[] {timeConv, z, lat, lon}, new boolean[] {false, false, false, false}, true);

			if (u == null || v == null ) return null;
			else if (u.isNaN() || v.isNaN() ){
				nearNoData = true;
				return NODATA; 
			}

			//else, then set the values
			uv[0] = u.doubleValue();
			uv[1] = v.doubleValue();
			
/*			try {
				int latIndex = ncFile.locate(ncFile.variables.get(latName), lat);
				int lonIndex = ncFile.locate(ncFile.variables.get(lonName), lon);

				//check if it's near land
			for (int i=latIndex-io+1; i<=latIndex+io+1; i++ ){
				for (int j=lonIndex-io+1; j<=lonIndex+io+1; j++ ){
					//break if it's out of bounds
					if (i<0 || j<0 || i>ncFile.getSingleDimension(latName)-1 || j>ncFile.getSingleDimension(lonName)-1) break; 
					short landMask = EFDCLandFile.getValue(landName, new int[] {i,j}).shortValue(); 
					if (landMask == 1) nearNoData = true; 
					
				}
			}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
*/
		}
		else {
			Double u = (Double ) ncFile.getValue(uName, new double[] {timeConv, z, lat, lon}, new boolean[] {false, false, false, false},  true);
			Double v = (Double ) ncFile.getValue(vName, new double[] {timeConv, z, lat, lon}, new boolean[] {false, false, false, false}, true);

			if (u == null || v == null ) return null;
			else if (u.isNaN() || v.isNaN() ){
				nearNoData = true;
				return NODATA; 
			}

			//else, then set the values
			uv[0] = u.doubleValue();
			uv[1] = v.doubleValue();

		}

		//NOTE: set nearNoData to true all the time to check, since are small grids and likely
		//can pass over multiple grids in any given time step
		//|||||||||||||||||||  TEST!!!  ||||||||||||||||||
		//TODO -- may be faster to do a smaller time step then to check the land bounce every time
		if (ncFile.isNearNoData()) this.nearNoData = true; 

		//if (useEFDC) System.out.println("EFDC time: " + timeConv + "\tu: " + df.format(uv[0]) + "\tv: " + df.format(uv[1]));
		//else System.out.println("NCOM time: " + timeConv + "\tu: " + df.format(uv[0]) + "\tv: " + df.format(uv[1]));

		//System.out.println(datef.format(time) + "\tgetUV() runtime: " + ((int) (System.currentTimeMillis()-startTime)) + "ms. \tFor data: 	" + Arrays.toString(uv)); 
		//get the UV
		return uv; 

	}


	public boolean getIncomingTide(long time){
		//sunshine skyway monitor: x=-82.6539 y=27.6233
		double u = (Double )  EFDCFile.getValue(uName, new double[] {convertToEFDCTime(time), 0, 27.6233, -82.6539}, new boolean[] {false, false, false, false},  true);
		if (u>0) return true; 
		return false; 
	}
	
	
	/**
	 * Given a time and location, this subroutine returns the velocity
	 * components and error estimates. Valid velocities are returned by setting
	 * the flag variable to 1, invalid values set flag to -1. The velocity
	 * components are estimated by polynomial interpolation of chosen order.
	 * USES THE FOLL GLOBAL VARIABLES via globals.mod
	 * alon,alat,auvel,avvel,iorder,minLat,minLon,maxLat,maxLon,SPVAL
	 */

	public double[] getUVmean(double z, double lon, double lat) {
		return new double[]{0,0}; 
	}




/*	public void setLandFile(String filename) {
		try {
			sh.setDataSource(filename);
			sh.setNegLon(negPolyCoord);

		} catch (IOException e) {
			System.out.println("Land mask file (" + filename
					+ ") could not be read, or does not exist.\n");
			e.printStackTrace();
		}
	}
*/
	public double[] getNODATA() {
		return NODATA;
	}

	public String getLatName() {
		return latName;
	}

	public void setLatName(String latName) {
		this.latName = latName;
	}

	public String getLonName() {
		return lonName;
	}

	public void setLonName(String lonName) {
		this.lonName = lonName;
	}

	public String getKName() {
		return kName;
	}

	public void setKName(String name) {
		kName = name;
	}

	public String getTName() {
		return tName;
	}

	public void setTName(String name) {
		tName = name;
	}

	public boolean isNegOceanCoord() {
		return negOceanCoord;
	}

	public void setNegOceanCoord(boolean negOceanCoord) {
		this.negOceanCoord = negOceanCoord;
	}

	public boolean isNegPolyCoord() {
		return negPolyCoord;
	}

	public void setNegPolyCoord(boolean negPolyCoord) {
		this.negPolyCoord = negPolyCoord;
	}

	public boolean isNearNoData() {
		return nearNoData;
	}

	//TODO -- need to fix up clone so that will re-clone the netCDF file readers
	public TBNestedNetCDFVelocityReader clone(){
		TBNestedNetCDFVelocityReader ncv = new TBNestedNetCDFVelocityReader();

		ncv.freqUnits=freqUnits;
		ncv.negOceanCoord=negOceanCoord;
		ncv.negPolyCoord=negPolyCoord;
		ncv.kName=kName;
		ncv.latName=latName;
		ncv.lonName=lonName;
		ncv.tName=tName;
		ncv.uName=uName;
		ncv.vName=vName;

		//ncv.setLandFile(this.sh.fileName); 
		//this will set the filename, etc
		ncv.initialize(); 

		return ncv;
	}



	@Override
	public int[][] getShape() {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public String getUnits() {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public void setUnits(String units) {
		// TODO Auto-generated method stub

	}



	@Override
	public void closeConnections() {
		if (EFDCFile != null) EFDCFile.closeFile(); //use this for EFDC
		if (EFDCBathFile != null) EFDCBathFile.closeFile(); //use this for EFDC
		if (EFDCLandFile != null) EFDCLandFile.closeFile(); //land mask file
		if (NCOMFile != null) NCOMFile.closeFile();
		
	}
}

