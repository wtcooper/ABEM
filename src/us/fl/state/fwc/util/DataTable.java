package us.fl.state.fwc.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.lang3.ArrayUtils;

public class DataTable {

	//Eventually make this a generic array so can take in any type of Number or String
	//HashMap<String, Object[]> dataTable = new HashMap<String,Object[]>();

	private String[][] data; 
	private String[] headers; 
	private int numRows;
	private int numColumns;


	public DataTable(String filename, boolean hasHeader, String delim){
		readData(filename, hasHeader, delim);
	}


	/**
	 * Returns an Object array (String or Double) of the values for a particular variable.
	 * 
	 * @param varName
	 * @return
	 */
	public Object[] getColumn(String varName){

		int index=ArrayUtils.indexOf(headers, varName);

		String[] tempData = new String[numRows];

		for (int i=0; i<numRows; i++){
			tempData[i] = data[i][index];
		}


		try {
			Double[] doubVals = new Double[tempData.length];
			for (int i=0;i<doubVals.length;i++){
				doubVals[i] = Double.parseDouble(tempData[i]);
			}
			return doubVals;
		}
		catch (Exception e){
			return tempData;
		}
	}




	/**
	 * Returns an Object of the value for a particular variable.
	 * 
	 * @param varName
	 * @return
	 */
	public Object getValue(int row, int column){
		try {
			return Double.parseDouble(data[row][column]);
		}
		catch (Exception e){
			return data[row][column];
		}
	}

	

	private void readData(String fileName, boolean hasHeader, String delim){

		try {
			File file = new File(fileName); 
			BufferedReader reader = new BufferedReader(new FileReader(file));
			ArrayList<String[]> tempList = new ArrayList<String[]>();


			int loop=0;
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {


				String[] tempVals =line.split(delim); 

				//calculate the numColumns on the first read
				if (loop == 0){
					numColumns = tempVals.length;
					loop++;
				}

				//only add those rows that have the appropriate number of column entries
				//				if (tempVals.length == numColumns) {
				tempList.add(tempVals);
				//				}
			}

			headers = null;
			if (hasHeader) {

				headers = tempList.remove(0);
			}
			else {
				headers = new String[tempList.get(0).length]; 

				for (int i=0; i<headers.length; i++){
					headers[i] = "var"+(i+1);
				}
			}

			numRows = tempList.size();
			data = new String[numRows][numColumns];

			//loopover rows
			for (int row=0; row<tempList.size(); row++){

				String[] tempRow = tempList.get(row);
				for (int column = 0; column<numColumns; column++){
					if (column < tempRow.length) data[row][column] =tempRow[column];
				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public String[][] getData() {
		return data;
	}


	public int getNumRows() {
		return numRows;
	}


	public int getNumColumns() {
		return numColumns;
	}
	
	/**
	 * Returns the header titles as a String array
	 * 
	 * @return
	 */
	public String[] getHeaders(){
		return headers;
	}

	public int getIndex(String varName){
		return ArrayUtils.indexOf(headers, varName);
	}
	
	
	/**
	 * Converts a String line to an int vector, assumming it's in integer format, else will
	 * throw error
	 * 
	 * @param line
	 * @return
	 */
	public static int[]	toIntVector(String line){
		int[] vals;
		String newline = line.trim();

		System.out.println(newline);
		//this should use comma, tab, or space delimited
		String[] tokens = newline.split(",|\t|\\s+");
		vals = new int[tokens.length];
		
		for (int i=0; i<tokens.length; i++){
			vals[i] = Integer.parseInt(tokens[i]);
		}
		return vals;
	}
	
	
	/**
	 * Converts a String line to a double vector, assumming it's in decimal format, else will
	 * throw error
	 * 
	 * @param line
	 * @return
	 */
	public static double[] toDoubleVector(String line){
		double[] vals;
		String[] tokens = line.split("\\x20+|\t|\\s");
		vals = new double[tokens.length];
		
		for (int i=0; i<tokens.length; i++){
			vals[i] = Double.parseDouble(tokens[i]);
		}
		
		return vals;
	}

}
