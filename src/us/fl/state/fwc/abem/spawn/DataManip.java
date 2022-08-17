package us.fl.state.fwc.abem.spawn;

import java.io.File;
import java.io.FilenameFilter;

import us.fl.state.fwc.util.DataTable;
import us.fl.state.fwc.util.TextFileIO;


public class DataManip {

	String batch = "Batch2";
	
	public void step() {

		
		TextFileIO outSSB = new TextFileIO("output/SpawnModel/GoodOutput/" + batch + "/SSB_sums.txt");
		
		//Read in all SSB for batch 1
		
		File dir = new File("output/SpawnModel/GoodOutput/" + batch + "/SSB"); 
		String[] list = dir.list(); 

		double[] ssbSums = null;
		for (String file: list){
			String filePath = dir.getAbsolutePath() + "\\" + file;

			DataTable tab = new DataTable(filePath, false, "\t");
			String[][] data = tab.getData(); 

			ssbSums = new double[data[0].length-1];

			for (int i=0; i<data.length; i++) {
				for (int j=1; j<data[0].length; j++){
					Double val = Double.parseDouble(data[i][j]);
					if (val.isNaN()) val = new Double(0);
					ssbSums[j-1] += val;
				}
			}

			//print out ID string
			String[] idTokens = file.split("_");
			for (int i=2; i<idTokens.length; i++){
				outSSB.print(idTokens[i] + "\t");
			}

			//print out sums
			for (int i=0; i<ssbSums.length; i++){
				outSSB.print(ssbSums[i] + "\t");
			}
			
			outSSB.print("\n");
			
			System.out.println("finished " + file);
		}
		
		
		
		outSSB.close();
		

		
		//TEP
		TextFileIO outTEP = new TextFileIO("output/SpawnModel/GoodOutput/" + batch + "/TEP_sums.txt");
		
		//Read in all SSB for batch 1
		
		dir = new File("output/SpawnModel/GoodOutput/" + batch + "/TEP"); 
		list = dir.list(); 

		double[] tepSums = null;
		for (String file: list){
			
			String filePath = dir.getAbsolutePath() + "\\" +  file;

			DataTable tab = new DataTable(filePath, false, "\t");
			String[][] data = tab.getData(); 

			tepSums = new double[data[0].length-1];

			for (int i=0; i<data.length; i++) {
				for (int j=1; j<data[0].length; j++){
					Double val = Double.parseDouble(data[i][j]);
					if (val.isNaN()) val = new Double(0);
					tepSums[j-1] += val;
				}
			}

			//print out ID string
			String[] idTokens = file.split("_");
			for (int i=2; i<idTokens.length; i++){
				outTEP.print(idTokens[i] + "\t");
			}

			//print out sums
			for (int i=0; i<tepSums.length; i++){
				outTEP.print(tepSums[i] + "\t");
			}
			
			outTEP.print("\n");
			
			System.out.println("finished " + file);

		}

		outTEP.close();
		
	}

	
	
	public static void main(String[] args) {
		DataManip d = new DataManip();
		d.step();
		
	}

}
