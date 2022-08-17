package us.fl.state.fwc.abem.hydro.ncom;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.NumberFormat;

import us.fl.state.fwc.util.geo.NetCDFFile;

public class NCOM_ASCI_Cells {

	String filename= "c:\\work\\data\\NCOM_AS\\ncom_relo_amseas_2010052800.nc";


	public void step(){

		NumberFormat nf = NumberFormat.getInstance(); 

		try {
			PrintWriter outFile = new PrintWriter(new FileWriter("output/NCOMGridCells.txt", true));


			NetCDFFile ncFile = null;  
			try {
				ncFile = new NetCDFFile(filename);
			} catch (IOException e) {
				e.printStackTrace();
			}

			ncFile.setVariables("lat", "lon", "surf_el"); 

			nf.setMinimumIntegerDigits(3); //set the number format to 2 integers (e.g., 01, 02, ...10, 11)
			
			outFile.print("     ");
			for (int j=0; j<ncFile.getSingleDimension("lon"); j++){
				String num = nf.format(j);
				outFile.print(num.charAt(0));
			}
			outFile.println();
			outFile.print("     ");
			for (int j=0; j<ncFile.getSingleDimension("lon"); j++){
				String num = nf.format(j);
				outFile.print(num.charAt(1));
			}
			outFile.println();
			outFile.print("     ");
			for (int j=0; j<ncFile.getSingleDimension("lon"); j++){
				String num = nf.format(j);
				outFile.print(num.charAt(2));
			}
			outFile.println();
			outFile.println();

			
			int out = 0; 
			for (int i=ncFile.getSingleDimension("lat")-1; i>=0; i--){
				outFile.print(nf.format(i) + "  ");
				for (int j=0; j<ncFile.getSingleDimension("lon"); j++){
					float value = ncFile.getValue("surf_el", new int[] {0, i, j}).floatValue();
					//System.out.println(value);
					if (value > -25)out=1;
					else out=0;
					outFile.print(out);
				}
				outFile.println();
			}


			outFile.close();

		} catch (IOException e1) {
			e1.printStackTrace();
		} 


	}


	public static void main(String[] args) {
		NCOM_ASCI_Cells main = new NCOM_ASCI_Cells ();
		main.step();
	}


}
