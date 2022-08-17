package us.fl.state.fwc.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javolution.util.FastMap;
import javolution.util.FastTable;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

public class NetCDFTextOutput {


	int xOffset = 313295;
	int yOffset	= 3113157;
	double cellSize=10; 

	private  PrintWriter outFile = null; 



	FastMap<Integer, FastTable<Integer>> dimensions; 


	public void IO(){

		try { outFile = new PrintWriter(new FileWriter("output/bath100m.xyz", true));
		} catch (IOException e) {e.printStackTrace();}

		NetcdfFile depthDataFile = null;

		try {
			String depthFile = "dataTest/TBBath100m.nc"; 
			depthDataFile = NetcdfFile.open(depthFile, null);
			Variable xVar = depthDataFile.findVariable("x"); 
			Variable yVar = depthDataFile.findVariable("y");
			Variable depthVar= depthDataFile.findVariable("depth");
			ArrayDouble.D1 xArray = (ArrayDouble.D1) xVar.read(); 
			ArrayDouble.D1 yArray = (ArrayDouble.D1) yVar.read(); 

/*			outFile.println(xArray.getSize() + "\tNx"); 
			for (int i=0; i<xArray.getSize(); i++){
				outFile.println(xArray.get(i));
			}
			outFile.println(yArray.getSize() + "\tNy"); 
			for (int i=0; i<yArray.getSize(); i++){
				outFile.println(yArray.get(i));
			}

			outFile.println((yArray.getSize()*xArray.getSize()) + "\tNz"); 
*/			
			ArrayFloat.D2 depthArray = (ArrayFloat.D2)depthVar.read();
			for (int i=0; i<xArray.getSize(); i++){
				for (int j=0; j<yArray.getSize(); j++){
					outFile.println(xArray.get(i) + "\t" + yArray.get(j) + "\t" + depthArray.get(j,i) ); // + "\tMass\t" + 5);
				}
			}


		} catch (java.io.IOException e) {
			System.out.println(" fail = "+e);
			e.printStackTrace();
		} finally {
			if (depthDataFile != null )
				try {
					depthDataFile.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
		}
		outFile.close();

	}

	public static void main(String[] args) {

		NetCDFTextOutput frc = new NetCDFTextOutput();
		frc.IO(); 


	}
}
