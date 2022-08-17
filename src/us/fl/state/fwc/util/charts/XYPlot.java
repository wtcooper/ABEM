package us.fl.state.fwc.util.charts;

/* ===========================================================
 * JFreeChart : a free chart library for the Java(tm) platform
 * ===========================================================
 *
 * (C) Copyright 2000-2004, by Object Refinery Limited and Contributors.
 *
 * Project Info:  http://www.jfree.org/jfreechart/index.html
 *
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation;
 * either version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * library; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 *
 * [Java is a trademark or registered trademark of Sun Microsystems, Inc. 
 * in the United States and other countries.]
 *
 */


import java.awt.Paint;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

/**
 * A demo of the fast scatter plot.
 *
 */
public class XYPlot extends ApplicationFrame {

	private int chartWidth = 1000;
	private int chartHeight = 700; 

	private Paint backgroundColor;
	private boolean changeBackgroundColor; 
	private Paint[] seriesColors;
	private boolean changeSeriesColors; 
	private boolean connectScatterPoints;

	private static final long serialVersionUID = 1L;
	private String title; 
	/**
	 * Creates a new fast scatter plot demo.
	 *
	 * @param title  the frame title.
	 */
	//    public XYPlot(String type, String title, String xAxis, String yAxis, float[][] data, boolean forSVG) {

	public XYPlot(String title){
		super(title);
		this.title = title; 
	}

	/**	Creates an XY scatter plot
	 * 
	 * @param seriesTitle
	 * @param xAxis
	 * @param yAxis
	 * @param data: double array[j][k], where [j] = x or y ([0] or [1] respectively); [k] = data
	 * @param forSVG
	 */
	public void makeScatterPlot(String seriesTitle, String xAxis, String yAxis, double[][] data, boolean forSVG, boolean forJPEG){
		XYSeries series = new XYSeries(seriesTitle);
		for (int i=0; i<data[0].length; i++){
			series.add(data[0][i], data[1][i]); 
		}
		XYDataset dataset = new XYSeriesCollection(series);
		JFreeChart  chart = ChartFactory.createScatterPlot(title,xAxis, yAxis,dataset,PlotOrientation.VERTICAL,true,true,false);

		if (connectScatterPoints){
			final org.jfree.chart.plot.XYPlot plot = chart.getXYPlot();
			final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
			renderer.setSeriesLinesVisible(0, true);
			renderer.setSeriesShapesVisible(1, true);
			renderer.setToolTipGenerator(new StandardXYToolTipGenerator());
			plot.setRenderer(renderer);
		}

		NumberAxis domainAxis = (NumberAxis) chart.getXYPlot().getDomainAxis();
		domainAxis.setAutoRangeIncludesZero(false);
		ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(chartWidth	, chartHeight)); 
		setContentPane(chartPanel);
		draw(chart, forSVG, forJPEG);
	}

	/**	Creates an XY line plot with multiple series
	 * 
	 * @param seriesTitle -- array of series titles
	 * @param xAxis
	 * @param yAxis
	 * @param data: triple array[i][j][k], where [i] = series; [j] = x or y ([0] or [1] respectively); [k] = data
	 * @param forSVG
	 * 	 * @param forJPEG
	 */
	public void makeLinePlot(String[] seriesTitle, ArrayList<Integer> seriesToPlot, String xAxis, String yAxis, double[][][] data, boolean forSVG, boolean forJPEG){
		XYSeriesCollection dataset = new XYSeriesCollection();
		for (int j = 0; j<seriesToPlot.size(); j++){
			int elementToPlot =seriesToPlot.get(j);
			XYSeries series = new XYSeries(seriesTitle[j]);
			for (int i=0; i<data[elementToPlot][0].length; i++){
				series.add(data[elementToPlot][0][i], data[elementToPlot][1][i]); 
			}
			dataset.addSeries(series);
		}

		JFreeChart  chart = ChartFactory.createXYLineChart(title,xAxis, yAxis,dataset,PlotOrientation.VERTICAL,true,true,false);
		Plot plot = chart.getPlot(); 
		if (changeBackgroundColor) plot.setBackgroundPaint(backgroundColor); 

		NumberAxis domainAxis = (NumberAxis) chart.getXYPlot().getDomainAxis();
		domainAxis.setAutoRangeIncludesZero(false);
		ChartPanel chartPanel = new ChartPanel(chart);

		chartPanel.setPreferredSize(new java.awt.Dimension(chartWidth	, chartHeight)); 
		setContentPane(chartPanel);
		draw(chart, forSVG, forJPEG);
	}


	/**	Creates an XY line plot with a single series
	 * 
	 * @param seriesTitle 
	 * @param xAxis
	 * @param yAxis
	 * @param data: double array[j][k], where [j] = x or y ([0] or [1] respectively); [k] = data
	 * @param forSVG
	 * @param forJPEG
	 */
	public void makeLinePlot(String seriesTitle, String xAxis, String yAxis, double[][] data, boolean forSVG, boolean forJPEG){
		XYSeries series = new XYSeries(seriesTitle);
		for (int i=0; i<data[0].length; i++){
			series.add(data[0][i], data[1][i]); 
		}
		XYDataset dataset = new XYSeriesCollection(series);
		JFreeChart  chart = ChartFactory.createXYLineChart(title,xAxis, yAxis,dataset,PlotOrientation.VERTICAL,true,true,false);
		NumberAxis domainAxis = (NumberAxis) chart.getXYPlot().getDomainAxis();
		domainAxis.setAutoRangeIncludesZero(false);


		ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(chartWidth	, chartHeight)); 
		setContentPane(chartPanel);
		draw(chart, forSVG, forJPEG);
	}


	/**	Draws the chart either to the screen or outputs as an SVG graphic, based on boolean forSVG
	 * 
	 * @param chart
	 * @param forSVG: true if output as SVG, false otherwise
	 */
	private void draw(JFreeChart chart, boolean forSVG, boolean forJPEG){
		if (forSVG){
			JChartSVGExport exp = new JChartSVGExport();
			try {
				exp.exportChartAsSVG(chart, new Rectangle2D.Double(100, 100, chartWidth, chartHeight) , new File(title+".svg"));
			} catch (IOException e) {
				e.printStackTrace();
			} 
		}
		else if (forJPEG){
			try {
				ChartUtilities.saveChartAsJPEG(new File(title+".jpg"), chart, chartWidth	, chartHeight);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else {
			this.pack(); 
			RefineryUtilities.centerFrameOnScreen(this); 
			this.setVisible(true); 
		}
	}

	public void setChartWidth(int chartWidth) {
		this.chartWidth = chartWidth;
	}

	public void setChartHeight(int chartHeight) {
		this.chartHeight = chartHeight;
	}

	public void setBackgroundColor(Paint backgroundColor) {
		this.backgroundColor = backgroundColor;
		this.changeBackgroundColor = true; 
	}

	public void setSeriesColors(Paint[] seriesColors) {
		this.seriesColors = seriesColors;
		this.changeSeriesColors = true; 
	}

	public void setScatterConnectPoints(){
		this.connectScatterPoints = true; 
	}

}