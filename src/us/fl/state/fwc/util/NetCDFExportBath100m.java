package us.fl.state.fwc.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javolution.util.FastMap;
import javolution.util.FastTable;
import ucar.ma2.ArrayFloat;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

public class NetCDFExportBath100m {



	int xOffset = 313295;
	int yOffset	= 3113157;
	double cellSize=10; 

	private  PrintWriter outFile = null; 

	

	FastMap<Integer, FastTable<Integer>> dimensions; 
	

	public void IO(){

		try { outFile = new PrintWriter(new FileWriter("output/depthRugosity_Cell_0_7.txt", true));
		} catch (IOException e) {e.printStackTrace();}

		NetcdfFile rugDataFile = null;
		NetcdfFile depthDataFile = null;
		
		try {
			String rugosityFile = "data/TBRugosity.nc";
			String depthFile = "data/TBBath10x10.nc"; 
			rugDataFile = NetcdfFile.open(rugosityFile, null);
			depthDataFile = NetcdfFile.open(depthFile, null);
			Variable rugVar= rugDataFile.findVariable("rugosity");
			Variable depthVar= depthDataFile.findVariable("depth");
			ArrayFloat.D2 rugArray = (ArrayFloat.D2)rugVar.read();
			ArrayFloat.D2 depthArray = (ArrayFloat.D2)depthVar.read();



				int minXIndex = 23; 
				int maxXIndex =68; 
				int minYIndex = 187; 
				int maxYIndex =237; 

				outFile.println("i" + "\t" + "j" + "\t" + "rugosity" + "\t" + "depth");
				
					for (int i = minYIndex; i <maxYIndex; i++){
						for (int j = minXIndex; j < maxXIndex; j++){

							outFile.println(i + "\t" + j + "\t" + rugArray.get(i,j) + "\t" + depthArray.get(i,j));

						} // end of for loop over X's
					} // end of for loop over Y's


		} catch (java.io.IOException e) {
			System.out.println(" fail = "+e);
			e.printStackTrace();
		} finally {
			if (depthDataFile != null && rugDataFile!=null)
				try {
					depthDataFile.close();
					rugDataFile.close(); 
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
		}
		outFile.close();

	}

	public static void main(String[] args) {

		NetCDFExportBath100m frc = new NetCDFExportBath100m();
		frc.IO(); 


	}
}
