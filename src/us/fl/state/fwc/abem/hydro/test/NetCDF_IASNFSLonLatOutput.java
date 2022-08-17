package us.fl.state.fwc.abem.hydro.test;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javolution.util.FastMap;
import javolution.util.FastTable;
import ucar.ma2.ArrayFloat;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

public class NetCDF_IASNFSLonLatOutput {



	int xOffset = 313295;
	int yOffset	= 3113157;
	double cellSize=10; 

	private  PrintWriter outFile = null; 



	FastMap<Integer, FastTable<Integer>> dimensions; 


	public void IO(){

		try { outFile = new PrintWriter(new FileWriter("output/THREDDSTest.dat", true));

		} catch (IOException e) {e.printStackTrace();}

		NetcdfFile uDataFile = null;

		try {
			String filename = "dataTest/v3d_2010051500.nc"; 
			//String filename = "http://edac-dap.northerngulfinstitute.org/thredds/fileServer/iasnfs/2010/may/20100513/v3d_2010051500.nc";
			uDataFile = NetcdfDataset.openFile(filename, null);    
			
			Variable uVar = uDataFile.findVariable("V_Velocity"); 
			ArrayFloat.D4 uArray;
	
			int time = 0;
			int depth = 10; 
			int y = 250;
			int x=100; 
		       uArray = (ArrayFloat.D4) uVar.read(time+":"+time+":1,"+  depth+":"+depth+":1,"+  y+":"+y+":1,"+x+":"+x+":1"); //read("0:0:1, 0:29:1, 0:480:1, 0:600:1 ");
				float value = uArray.get(0,0,0,0); 
				System.out.println("value at (" +time+","+depth+","+x+","+y+")" + value); 
	
/*			ArrayFloat.D2 depthArray = (ArrayFloat.D2)depthVar.read();
			for (int i=0; i<xArray.getSize(); i++){
				for (int j=0; j<yArray.getSize(); j++){
					outFile.println(xArray.get(i) + "\t" + yArray.get(j) + "\t" + depthArray.get(j,i) ); // + "\tMass\t" + 5);
				}
			}
*/

		} catch (java.io.IOException e) {
			System.out.println(" fail = "+e);
			e.printStackTrace();
		} catch (InvalidRangeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (uDataFile != null )
				try {
					uDataFile.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
		}
		outFile.close();

	}

	public static void main(String[] args) {

		NetCDF_IASNFSLonLatOutput frc = new NetCDF_IASNFSLonLatOutput();
		frc.IO(); 


	}
}
