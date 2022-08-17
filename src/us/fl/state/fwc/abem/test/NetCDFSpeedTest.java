package us.fl.state.fwc.abem.test;

import java.io.IOException;

import ucar.ma2.ArrayFloat;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;

public class NetCDFSpeedTest {

	private static final int seed = (int) System.currentTimeMillis();
	private static MersenneTwister m= new MersenneTwister(seed); 
	private static Uniform uniform = new Uniform(0,1,m); 
	
	//public int x, y; // the x and y position in the Bathymetry array
	public NetcdfFile dataFile = null;
	public int xDim = 5272, yDim = 7941; 
	public float[][] bath = new float[yDim][xDim]; 
	
	public long arrayTotalTime=0, ncTotalTime=0; 
	
	
	public static void main(String[] args) {

		NetCDFSpeedTest nc = new NetCDFSpeedTest(); 
		nc.step();  

	}

	public void step(){

		initialize("data/TBBath10x10.nc");		
		System.out.println("done initializing"); 
		setArray(); 
		System.out.println("done setting array"); 
		
		arrayTotalTime += getArrayRunTime();
		ncTotalTime += getNcRunTime(); 
		arrayTotalTime += getArrayRunTime();
		ncTotalTime += getNcRunTime(); 
		arrayTotalTime += getArrayRunTime();
		ncTotalTime += getNcRunTime(); 
		arrayTotalTime += getArrayRunTime();
		ncTotalTime += getNcRunTime(); 
		arrayTotalTime += getArrayRunTime();
		ncTotalTime += getNcRunTime(); 
		arrayTotalTime += getArrayRunTime();
		ncTotalTime += getNcRunTime(); 

		System.out.println("arrayTotalTime: " + arrayTotalTime); 
		System.out.println("ncTotalTime: " + ncTotalTime); 
		
	}

	
	public long getArrayRunTime(){

		long startTime = System.currentTimeMillis(); 

		for (int i=0; i<1000000; i++){
			int x = uniform.nextIntFromTo(0, xDim-1); 
			int y = uniform.nextIntFromTo(0, yDim-1); 
			float value = bath[y][x]; 
		}

		long endTime = System.currentTimeMillis()-startTime; 
		return endTime; 
	}
	
	
	
	public long getNcRunTime(){
		long startTime = System.currentTimeMillis(); 

		for (int i=0; i<1000000; i++){
			int x = uniform.nextIntFromTo(0, xDim-1); 
			int y = uniform.nextIntFromTo(0, yDim-1); 
			float value = getValue(x, y);  
		}

		long endTime = System.currentTimeMillis()-startTime; 
		return endTime; 
	}

	
	
	
	public void initialize(String filename){


		if (!filename.equals("noData")){
			try {
				dataFile = NetcdfFile.open(filename, null);
			} catch (java.io.IOException e) {
				e.printStackTrace();
			} 
		}
	}

	
	
	public void setArray(){

		Variable dataVar= dataFile.findVariable("depth");

		try {
			ArrayFloat.D2 dataArray;
			int counter = 0; 
			for (int i=0; i<yDim; i++){
				for (int j=0; j<xDim; j++){
					dataArray = (ArrayFloat.D2)dataVar.read(i+":"+i+":1,"+j+":"+j+":1");
					bath[i][j] = dataArray.get(0, 0);
					counter++; 
					if ( (counter%100000 == 0) ) System.out.println("gathing data....num lines read is: " + counter); 

				}
			}

		} catch (InvalidRangeException e) {
			e.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}        

	}


	public float getValue(int x, int y)	{

		float value = 0; 
		Variable dataVar= dataFile.findVariable("depth");

		try {
			ArrayFloat.D2 dataArray;
			dataArray = (ArrayFloat.D2)dataVar.read(y+":"+y+":1,"+x+":"+x+":1");
			value = dataArray.get(0, 0); 

		} catch (InvalidRangeException e) {
			e.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}        

		return value; 
	}



}
