package us.fl.state.fwc.abem.hydro.test;

import java.io.IOException;
import java.util.List;

import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

public class NOGAPSDataConnect {


	//there are multiple u/v file pairs in the dataset -- not sure what is what, but likely the numbering represents the height bin above the surface (e.g., 10km, 20km)
	String uWindFilename = "http://www.usgodae.org:80/dods/GDS/nogaps/NOGAPS_0100_000100-000000wnd_ucmp"; 
	String vWindFilename = "http://www.usgodae.org:80/dods/GDS/nogaps/NOGAPS_0100_000100-000000wnd_vcmp"; 

	public void IO(){


		NetcdfFile uDataFile = null;

		try {
			uDataFile = NetcdfDataset.openFile(uWindFilename, null);    

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
				for (int j=0; j<varList.size(); j++){
					Attribute att = list.get(j); 
					if (att.getDataType().toString().equals("String")) System.out.println("Variable (" + var.getName() + ") attribute (" + att.getName() + "): " + att.getStringValue());
					else System.out.println("Variable (" + var.getName() + ") attribute (" + att.getName() + "): " + att.getNumericValue());
				}

				System.out.println();
			}
							
		} catch (java.io.IOException e) {
			System.out.println(" fail = "+e);
			e.printStackTrace();
		} finally {
			if (uDataFile != null )
				try {
					uDataFile.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
		}

	}

	public static void main(String[] args) {

		NOGAPSDataConnect frc = new NOGAPSDataConnect();
		frc.IO(); 


	}
}
