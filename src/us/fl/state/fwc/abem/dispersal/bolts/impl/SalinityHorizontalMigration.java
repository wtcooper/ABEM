package us.fl.state.fwc.abem.dispersal.bolts.impl;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import us.fl.state.fwc.abem.dispersal.bolts.HorizontalMigration;
import us.fl.state.fwc.abem.dispersal.bolts.Particle;
import us.fl.state.fwc.util.TestingUtils;
import us.fl.state.fwc.util.geo.NetCDFFile;

public class SalinityHorizontalMigration implements HorizontalMigration,Cloneable  {

	private String filename; 
	private BOLTSParams params; 


	private NetCDFFile EFDCFile; //use this for EFDC
	private NetCDFFile NCOMFile;
	private NetCDFFile EFDCBathFile; //use this for EFDC
	private NetCDFFile EFDCLandFile; //land mask file

	private double oldNCOMTime = Double.MIN_VALUE; 

	private String latName = "lat";
	private String lonName = "lon";
	private String kName = "depth";
	private String tName = "time";
	String salVarName = "salinity";

	private boolean nearNoData = false;
	private boolean negOceanCoord = true;
	private boolean negPolyCoord = true;

	private final double[] NODATA = { Double.NaN, Double.NaN };

	String EFDCDirectory = "c:\\work\\workspace\\EFDC\\TampaToSarasota_WGS\\";

	long EFDCTimeOffset = 410227200000l;
	long NCOMTimeOffset = 946684800000l; 

	public boolean useEFDC = true; 
	//NCOMSettings
	String dateURL;
	String NCOMbaseFile = "c:\\work\\data\\NCOM_AS\\";
	String NCOMnameURL = "ncom_relo_amseas_";
	String EFDCbaseFile = "C:\\work\\workspace\\EFDC\\TampaToSarasota_WGS\\ncFiles\\";
	String EFDCnameURL = "TB2SB_WGS_"; 

	private String bathName = "water_depth";
	private String landName = "landMask"; 
	String EFDCBathFileName = EFDCDirectory + "TB2SB_WGS_Bathy.nc";
	String EFDCLandFileName = EFDCDirectory + "TB2SB_WGS_LandMaskForShapefileBarrier.nc";

	NumberFormat nf = NumberFormat.getInstance(); 


	public SalinityHorizontalMigration(BOLTSParams params, String filename){
		this.filename = filename;
		this.params = params;
		
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




	@Override
	public void apply(Particle p) {
		long time = p.getT();
		double z = p.getZ();
		double lon = p.getX();
		double lat = p.getY();

		nearNoData = false; 
		if(negOceanCoord){
			if(lon>180){lon = -(360-lon);}
		}

		//gets the proper ncFile to use for this method call
		NetCDFFile ncFile = getCorrectDataFile(time, lon, lat);

		//if for whatever reason the ncFile comes back as null, then return null and have 
		//particle set to lost
		if (ncFile == null) return ; 

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


		double[] origin = new double[4];


		try {
			origin[0] = ncFile.locate(ncFile.getVariable(tName), timeConv);
			origin[1] = ncFile.locate(ncFile.getVariable(kName), z);
			origin[2] = ncFile.locate(ncFile.getVariable(latName), lat);
			origin[3] = ncFile.locate(ncFile.getVariable(lonName), lon);

			Double cellOrigin= (Double) ncFile.getValue(salVarName, origin, 
					new boolean[] {true,true,true,true},  false);

		} catch (IOException e) {
			e.printStackTrace();
		}

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
				NCOMFile.setVariables(tName, kName, latName, lonName, salVarName); 
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
				EFDCFile.setVariables(tName, kName, latName, lonName, salVarName); 

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


	@Override
	public void closeConnections() {

	}

	public SalinityHorizontalMigration clone() {
		SalinityHorizontalMigration shm = new SalinityHorizontalMigration(params, filename);
		return shm; 
	}

}
