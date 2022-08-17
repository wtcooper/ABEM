package us.fl.state.fwc.abem.hydro.efdc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import us.fl.state.fwc.util.TimeUtils;

public class TideCharts {


	String[] constituentFileNames = {"data/EgmontEstTidalConstituents.txt", "data/StPeteClearwaterAvgTideConstituents.txt"};
	String obsDataFileName = "data/Egmont2004-2006Buoy_Tab.txt"; 
	String[] labelTitles = {"EgmontEst", "StPeteClearAvg"};
	Calendar startDate = new GregorianCalendar(2004,7,1); 
	Calendar endDate = new GregorianCalendar(2004,7,30); 
	boolean addObsData = true; 


	public static void main(String[] args) {
		TideCharts tc = new TideCharts();
		tc.step(); 
	}

	public void step(){


		startDate.setTimeZone(TimeZone.getTimeZone("GMT")); 
		endDate.setTimeZone(TimeZone.getTimeZone("GMT")); 
		
		
		System.out.println(constituentFileNames.length + "\t" + labelTitles.length); 
		List<XYSeries> series = new ArrayList<XYSeries>(); 

		for (int i=0; i<labelTitles.length; i++){
			Calendar comparisonDate = (Calendar) startDate.clone(); 
			comparisonDate.add(Calendar.SECOND, (int) -(TimeUtils.SECS_PER_DAY)); // remove a day so that .before comparison below will work properly

			XYSeries serie = new XYSeries(labelTitles[i]);

			double day = startDate.get(Calendar.DAY_OF_YEAR); 

			while (comparisonDate.before(endDate)){
				serie.add(day, TimeUtils.getTidalElevation(comparisonDate, constituentFileNames[i])); 
				day += 1d/24d; //add hour to day
				comparisonDate.add(Calendar.HOUR_OF_DAY, 1); 
			}

			series.add(serie); 
		}

		XYSeriesCollection dataset = new XYSeriesCollection();
		if (addObsData){
			series.add(getObservedTides()); 
			for (int i=0; i<labelTitles.length+1; i++){
				dataset.addSeries(series.get(i));
			}		
		}
		else {
			for (int i=0; i<labelTitles.length; i++){
				dataset.addSeries(series.get(i));
			}		
		}

		// create the chart...
		JFreeChart chart = ChartFactory.createXYLineChart("Tidal Elevations", // chart title
				"day", // domain axis label
				"elevation(m)", // range axis label
				(XYDataset) dataset, // data
				PlotOrientation.VERTICAL,
				true, // include legend
				true,
				true
		);

		// create and display a frame...
		ChartFrame frame = new ChartFrame("Tidal Elevations", chart);
		frame.pack();
		frame.setVisible(true);
	}



	public XYSeries getObservedTides(){
		double meanTide = 0.354; 
		BufferedReader reader = null; 

		XYSeries series = new XYSeries("ObservedData"); 

		try {
			reader = new BufferedReader(new FileReader(obsDataFileName));
			/* First line of the data file is the header */
			reader.readLine();

			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				String tokens[] = line.split("\t"); // this is a "greedy qualifier regular expression in java -- don't understand but works

				//#YY  MM DD hh mm WDIR WSPD GST  WVHT   DPD   APD MWD   PRES  ATMP  WTMP  DEWP  VIS PTDY  TIDE
				int year = Integer.parseInt(tokens[0]);
				int month = Integer.parseInt(tokens[1]) -1;
				int day = Integer.parseInt(tokens[2]);
				int hour = Integer.parseInt(tokens[3]);

				Calendar date = new GregorianCalendar(year, month, day);
				if (startDate.before(date) && date.before(endDate) ) {

					double dayOfYear =  date.get(Calendar.DAY_OF_YEAR) + (double) hour  / 24d; 


					//tide
					if (!tokens[16].equals("MM") && !(Double.parseDouble(tokens[16]) == 99.)) {
						double tide = Double.parseDouble(tokens[16])*0.3048 - meanTide; // convert to meters, then adjust so is at meanTide versus MLLH 

						series.add(dayOfYear, tide); 

					}
				}
			}
			//counter++;

		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return series;  
	}

}
