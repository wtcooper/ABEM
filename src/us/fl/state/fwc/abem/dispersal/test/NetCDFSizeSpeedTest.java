package us.fl.state.fwc.abem.dispersal.test;

import java.io.IOException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import us.fl.state.fwc.util.geo.NetCDFFile;

public class NetCDFSizeSpeedTest {

	private static DateFormat datef = new SimpleDateFormat("yyyy-MM-dd");


	private NetCDFFile ncFile; //use this for EFDC

	private final double[] NODATA = { Double.NaN, Double.NaN };
	private String latName = "lat";
	private String lonName = "lon";
	private String kName = "depth";
	private String tName = "time";
	private String uName = "water_u";
	private String vName= "water_v";

	String EFDCDirectory = "c:\\work\\workspace\\EFDC\\TampaToSarasota_WGS\\";

	String EFDCFileName = EFDCDirectory + "TB2SB_WGS_070110-071010.nc" ; //"TB2SB_WGS_070110-092610.nc";
	String NCOMFileName =  "C:\\work\\data\\NCOM_AS\\ncom_relo_amseas_2010052500.nc"; 

	NumberFormat nf = NumberFormat.getInstance(); 

	long EFDCTimeOffset = 410227200000l;
	long NCOMTimeOffset = 946684800000l; 


	boolean useEFDC = true; 


	public static void main(String[] args) {
		NetCDFSizeSpeedTest test = new NetCDFSizeSpeedTest();
		test.step();
	}

	public void step(){
		initialize();
		
		int startYear = 2010;
		int startMonth = 7;
		int startDay = 2;
		Calendar startRelease = new GregorianCalendar(startYear, startMonth-1, startDay); 
		startRelease.setTimeZone(TimeZone.getTimeZone("GMT")); 
		
		long time = startRelease.getTimeInMillis(); 
		double z = 0.5;
		double lat = 27.7643	; 
		double lon = -82.8498; 
		
		if (useEFDC){
			lat = 27.6446;
			lon = -82.6255; 
		}
		
		for (int i=0; i<100; i++){
			getUV(time,z,lon,lat);
		}
	}


	public void initialize(){

		try {
			if (useEFDC) {
				ncFile = new NetCDFFile(EFDCFileName);
				ncFile.setInterpolationAxes(latName, lonName);
				ncFile.setVariables(tName, kName, latName, lonName, uName, vName); 
			}
			else {
				ncFile = new NetCDFFile(NCOMFileName);
				ncFile.setInterpolationAxes(latName, lonName);
				ncFile.setVariables(tName, kName, latName, lonName, uName, vName); 
			}
		} catch (IOException e) {}


	}



	public double convertToNCOMTime(long time){
		//NCOM is hours since 1/1/2000
		return (double) (time-NCOMTimeOffset) / (1000*60*60); 
	}

	public double convertToEFDCTime(long time){
		//EFDC is hours since 1/1/1983 (tidal epoch)
		return (double) (time-EFDCTimeOffset) / (1000*60*60); 
	}

	public synchronized double[] getUV(long time, double z, double lon, double lat) {

		long startTime = System.currentTimeMillis();



		double timeConv=0;

		if (useEFDC)  timeConv = convertToEFDCTime(time);
		else timeConv = convertToNCOMTime(time);

		double[] uv = new double[2];


		Double u = (Double) ncFile.getValue(uName, new double[] {timeConv, z, lat, lon}, new boolean[] {false, false, false, false},  true);
		Double v = (Double) ncFile.getValue(vName, new double[] {timeConv, z, lat, lon}, new boolean[] {false, false, false, false}, true);

		if (u == null || v == null ) return null;
		else if (u.isNaN() || v.isNaN() ){
			return NODATA; 
		}

		//else, then set the values
		uv[0] = u.doubleValue();
		uv[1] = v.doubleValue();

		System.out.println(datef.format(time) + "\tgetUV() runtime: " + ((int) (System.currentTimeMillis()-startTime)) + "ms. \tFor data: 	" + Arrays.toString(uv)); 
		//get the UV
		return uv; 

	}


}

