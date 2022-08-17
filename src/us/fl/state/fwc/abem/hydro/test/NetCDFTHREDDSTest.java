package us.fl.state.fwc.abem.hydro.test;

import java.io.IOException;
import java.util.List;

import javolution.util.FastMap;
import javolution.util.FastTable;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

public class NetCDFTHREDDSTest {


	String NOMADS_URL = "http://nomads.ncdc.noaa.gov/thredds/dodsC/oceanwindsfilesys/SI/uv/6hrly/netcdf/2000s/uv20100801rt.nc";
	String RSMAS_WERA_URL = "http://hfrnet-dp.rsmas.miami.edu/cgi-bin/nph-dods/wera/um_wera_00_latest.nc";
	String NOGAPS_URL = "http://www.usgodae.org:80/dods/GDS/nogaps/NOGAPS_0100_000100-000000wnd_ucmp"; 
	String filenameNCOM_AS = "http://edac-dap.northerngulfinstitute.org/thredds/dodsC/ncom/amseas/ncom_relo_amseas_2010060100/ncom_relo_amseas_2010060100_t27.nc"; //"c:\\work\\temp\\ncom_relo_amseas_2010072500_t084.nc" ; //"dataTest/v3d_2010051500.nc"; 
	String filenameIASNFS  = "http://edac-dap.northerngulfinstitute.org/thredds/fileServer/iasnfs/2010/may/20100513/v3d_2010051500.nc";
	String filenameSAGBOM = "http://omglnx1.meas.ncsu.edu:8080/thredds/dodsC/fmrc/sabgom/runs/SABGOM_Forecast_Model_Run_Collection_RUN_2010-05-25T00:00:00Z"; 
	String varNameIASNFS = "V_Velocity"; 
	String varNameSAGBOM = "v"; 
	String varNameNCOM_AS = "water_v"; 
	String latVarNameSAGBOM = "lat_v";
	String lonVarNameSAGBOM = "lon_v";
	String latVarNameIASNFS = "Latitude";
	String lonVarNameIASNFS = "Longitude";
	
	String latVarNameNCOM_AS = "lat";
	String lonVarNameNCOM_AS = "lon";
	
	int time = 0;
	int depth = 10; 
	int y = 250;
	int x=100; 
	float scaleFactor = 0.001f; 




	FastMap<Integer, FastTable<Integer>> dimensions; 


	public void IO(){

//		try { outFile = new PrintWriter(new FileWriter("output/NCOM_ASLatLonUpdate.dat", true));

//		} catch (IOException e) {e.printStackTrace();}

		NetcdfFile uDataFile = null;

		try {
			uDataFile = NetcdfDataset.openFile(NOMADS_URL, null);    

			List<Variable> varList = uDataFile.getVariables();

			for (int i=0; i<varList.size(); i++){
				Variable var = varList.get(i);
				System.out.println("Variable name: " + var.getName());
				List<Dimension> dims = var.getDimensions();
				for (int j=0; j<dims.size(); j++){
					Dimension dim = dims.get(j); 
					System.out.println("Variable (" + var.getName() + ") dimension (" + j + "): " + dim.getLength());
				}
				
				List<Attribute> list = var.getAttributes();
				for (int j=0; j<list.size(); j++){
					Attribute att = list.get(j); 
					if (att.getDataType().toString().equals("String")) System.out.println("Variable (" + var.getName() + ") attribute (" + att.getName() + "): " + att.getStringValue());
					else System.out.println("Variable (" + var.getName() + ") attribute (" + att.getName() + "): " + att.getNumericValue());
				}

				System.out.println();
			}
			
/*			Variable uVar = uDataFile.findVariable(varNameNCOM_AS ); 
			ArrayFloat.D4 uArray;
			List<Attribute> list = uVar.getAttributes();
			for (int i=0; i<list.size(); i++){
				Attribute att = list.get(i);
				float scale = 0f;
				if (att.getName().equals("scale_factor")){
					scale = (Float)att.getValue(i);
				}
				System.out.println(att.getName() + "\t" + Float.toString(scale)); //att.getStringValue()	);
			}
			
		       uArray = (ArrayFloat.D4) uVar.read(time+":"+time+":1,"+  depth+":"+depth+":1,"+  y+":"+y+":1,"+x+":"+x+":1"); //read("0:0:1, 0:29:1, 0:480:1, 0:600:1 ");
				float value = uArray.get(0,0,0,0); //*scaleFactor; 
				System.out.println("value at (" +time+","+depth+","+x+","+y+"):" + "\t"+ value); 

				Variable latVar = uDataFile.findVariable(latVarNameNCOM_AS ); 
				Variable lonVar = uDataFile.findVariable(lonVarNameNCOM_AS ); 
				ArrayDouble.D1 latArray = (ArrayDouble.D1) latVar.read(); 
				ArrayDouble.D1 lonArray = (ArrayDouble.D1) lonVar.read(); 
				int[] latVarShape = latVar.getShape();
				int[] lonVarShape = lonVar.getShape();

				outFile.println("Lat" + "\t" + "Lon");  
				
				double lat=0, lon=0;
				for (int i = 0; i < latVarShape[0]; i++){
					for (int j = 0; j < lonVarShape[0]; j++){
						//outFile.println(latArray.get(i,j) + "\t" + lonArray.get(i,j)); 
						lat = latArray.get(i);
						lon = lonArray.get(j) - 360.0; 
						if ( (lat > 23.5) && (lat < 30.7) && (lon>-87.6) && (lon < -78.5)){
							outFile.println(lat + "\t" + lon);
						}
					}
					
				}
*/					
				
		} catch (java.io.IOException e) {
			System.out.println(" fail = "+e);
			e.printStackTrace();
		} /*catch (InvalidRangeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} */finally {
			if (uDataFile != null )
				try {
					uDataFile.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
		}
		//outFile.close();

	}

	public static void main(String[] args) {

		NetCDFTHREDDSTest frc = new NetCDFTHREDDSTest();
		frc.IO(); 


	}
}
