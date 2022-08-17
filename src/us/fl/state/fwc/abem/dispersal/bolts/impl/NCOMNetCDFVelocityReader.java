package us.fl.state.fwc.abem.dispersal.bolts.impl;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import ucar.nc2.Variable;
import us.fl.state.fwc.abem.dispersal.bolts.VelocityReader;
import us.fl.state.fwc.util.geo.NetCDFFile;

public class NCOMNetCDFVelocityReader implements VelocityReader, Cloneable {

	//private double u, v;
	private String freqUnits = "Days";
	//private final int io = 1;

	private NetCDFFile NCOMFile;
	private double oldNCOMTime = Double.MIN_VALUE; 

	//private boolean ncFileOpened = false; 
	@SuppressWarnings("unused")
	private Variable uVar, vVar;
	//private Array uArr, vArr;
	private ShapefileHabitat sh = new ShapefileHabitat();
	private final double[] NODATA = { Double.NaN, Double.NaN };
	private String latName = "lat";
	private String lonName = "lon";
	private String kName = "depth";
	private String tName = "time";
	private boolean negOceanCoord = true;
	private boolean negPolyCoord = true;
	private boolean nearNoData = false;
	private String uName = "water_u";
	private String vName= "water_v";


	long NCOMTimeOffset = 946684800000l; 

	//EFDCGrid grid;

	//NCOMSettings
	String dateURL;
	String baseFile = "c:\\work\\data\\NCOM_AS\\";
	String nameURL = "ncom_relo_amseas_";

	NumberFormat nf = NumberFormat.getInstance(); 


	
	public void initialize(){

	
	}



	/*
	 * Returns the proper netCDF file to use, either NCOM or EFDC based on position
	 */
	public NetCDFFile getCorrectDataFile(long time, double lon, double lat){
		//****** time is in Java milliseconds (i.e., sinc 1/1/1970)

			double NCOMTime = convertToNCOMTime(time); 

			//check to see if need to open a new NCOM NetCDFFile  
			if ( NCOMTime > (oldNCOMTime+8) ) {
				oldNCOMTime = NCOMTime; 

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

					NCOMFile.setInterpolationAxes(latName, lonName);
					NCOMFile.setVariables(tName, kName, latName, lonName, uName, vName); 
					uVar = NCOMFile.getVariable(uName);
					vVar = NCOMFile.getVariable(vName);
			}

			return NCOMFile; 
	}


	public double convertToNCOMTime(long time){
		//NCOM is hours since 1/1/2000
		return (double) (time-NCOMTimeOffset) / (1000*60*60); 
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
		nearNoData = false; 
		if(negOceanCoord){
			if(lon>180){lon = -(360-lon);}
		}
		
		//gets the proper ncFile to use for this method call
		NetCDFFile ncFile = getCorrectDataFile(time, lon, lat);

		double timeConv=0;

		timeConv = convertToNCOMTime(time);

		Float u = (Float) ncFile.getValue(uName, new double[] {timeConv, z, lat, lon}, new boolean[] {false, false, false, false},  true);
		Float v = (Float) ncFile.getValue(vName, new double[] {timeConv, z, lat, lon}, new boolean[] {false, false, false, false}, true);
		
		double[] uv = new double[2];

		if (u == null || v == null ) return null;
		else if (u.isNaN() || v.isNaN() ) return NODATA; 

		//else, then set the values
		uv[0] = u.floatValue();
		uv[1] = v.floatValue();

		if (ncFile.isNearNoData()) this.nearNoData = true; 


		//get the UV
		return uv; 

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




	public void setLandFile(String filename) {
		try {
			sh.setDataSource(filename);
			sh.setNegLon(negPolyCoord);

		} catch (IOException e) {
			System.out.println("Land mask file (" + filename
					+ ") could not be read, or does not exist.\n");
			e.printStackTrace();
		}
	}

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
	public NCOMNetCDFVelocityReader clone(){
		NCOMNetCDFVelocityReader ncv = new NCOMNetCDFVelocityReader();

		ncv.freqUnits=freqUnits;
		ncv.negOceanCoord=negOceanCoord;
		ncv.negPolyCoord=negPolyCoord;
		ncv.kName=kName;
		ncv.latName=latName;
		ncv.lonName=lonName;
		ncv.tName=tName;
		ncv.uName=uName;
		ncv.vName=vName;

		ncv.setLandFile(this.sh.fileName); 
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
	public boolean getIncomingTide(long time) {
		// TODO Auto-generated method stub
		return false;
	}



	@Override
	public void closeConnections() {
		if (NCOMFile != null) NCOMFile.closeFile(); 
	}
}

