package us.fl.state.fwc.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Scanner;

public class TemperatureDataExtract {

	String[] files = {"egkf1h2006.txt", "egkf1h2007.txt", "egkf1h2008.txt", "sapf1h2005.txt", "sapf1h2006.txt", "sapf1h2007.txt", "sapf1h2008.txt" }; 


	private  PrintWriter outFile = null; 


	public void test(){
		
		try { outFile = new PrintWriter(new FileWriter("tempdata.txt", true));
		} catch (IOException e) {e.printStackTrace();}

		System.out.println("start"); 

		for (int i=0; i< files.length; i++){
			File fFile=new File(files[i]); 
			if (fFile.exists()){
				try {
					//first use a Scanner to get each line
					Scanner scanner = new Scanner(fFile);
					while ( scanner.hasNextLine() ){
						Scanner lineScanner = new Scanner(scanner.nextLine()); 
						if ( lineScanner.hasNext() ){

							//#YY  MM DD hh mm WDIR WSPD GST  WVHT   DPD   APD MWD   PRES  ATMP  WTMP  DEWP  VIS  TIDE

							int year = lineScanner.nextInt();
							int month = lineScanner.nextInt();
							int day = lineScanner.nextInt();
							float wtmp = lineScanner.nextFloat();

							String code = null;
							
							if (wtmp != 999.0){
								if (i<3) code = "egk";
								else code = "sap"; 

								Calendar date = new GregorianCalendar(year, month, day);
								int dayOfYear = date.get(Calendar.DAY_OF_YEAR);
								outFile.println(code + "\t" + year + "\t" + dayOfYear + "\t" + wtmp); 
							}
						}// end lineScanner 
						lineScanner.close();
					} // end file scanner
					scanner.close();
				}
				catch (IOException ex){
					System.out.println(" fail = "+ex);
				}
			}
			outFile.close();
		}

	} // end setDependents() method


	public static void main(String[] args) {
		TemperatureDataExtract df = new TemperatureDataExtract();
		df.test(); 

	}

}
