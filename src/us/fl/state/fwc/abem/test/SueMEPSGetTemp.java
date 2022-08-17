package us.fl.state.fwc.abem.test;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.HashMap;

import us.fl.state.fwc.util.DataTable;
import us.fl.state.fwc.util.TextFileIO;

public class SueMEPSGetTemp {

	
	public void run(){

		HashMap<String, String[]> physData = new HashMap<String, String[]>();
		
		//TextFileIO outFile = new TextFileIO("data/SueMEPSTempFormatted.txt");
		//PrintWriter out = outFile.getWriter();
		
		
		
		//Read in temp data and store temperature with a time stamp
		DataTable allData = new DataTable("data/SueMEPSAll.txt", true, "\t");
		String[][] aData = allData.getData();
		
		DataTable tempData = new DataTable("data/SuesMEPSTemp.txt", true, "\t");
		String[][] tData = tempData.getData();
		
		System.out.println("initialize complete");
		
		//loop through 
		for (int i=0; i<tData.length; i++){
			String key = tData[i][tempData.getIndex("Date")] + "_" + tData[i][tempData.getIndex("Event")];
			int tempIndex = tempData.getIndex("MidDepthTemp");
			int salIndex = tempData.getIndex("MidDepthSalinity");
			String[] data = {tData[i][tempIndex], tData[i][salIndex] };
			
			physData.put(key, data);
		}
		
		for (int i=0; i<aData.length; i++){
			String key = "0" + aData[i][allData.getIndex("meps trial_Date")] + "_" + aData[i][allData.getIndex("Event")];
			String[] data = physData.get(key);
			//out.println(data[0] + "\t" + data[1]);
			System.out.println(data[0] + "\t" + data[1]);
		}
		
		//out.close();
	}
	

	public static void main(String[] args) {
		SueMEPSGetTemp temp = new SueMEPSGetTemp();
		temp.run();
	}

}
