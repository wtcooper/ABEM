package us.fl.state.fwc.util.charts;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

public class SimplePieChart_SVGExport {


	/**
	 * The starting point for the demo.
	 *
	 * @param args ignored.
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

		// create a dataset...
		DefaultPieDataset data = new DefaultPieDataset();
		data.setValue("Category 1", 43.2);
		data.setValue("Category 2", 27.9);
		data.setValue("Category 3", 79.5);
		// create a chart...

		JFreeChart chart = ChartFactory.createPieChart(
				"Sample Pie Chart",
				data,
				true, // legend?
				true, // tooltips?
				false // URLs?
		);

		Rectangle2D bounds = new Rectangle2D.Double(100, 100, 500, 400); 
		
		exportChartAsSVG(chart, bounds, new File("samplePieChart.svg")); 
		
		// create and display a frame...
		//ChartFrame frame = new ChartFrame("First", chart);
		//frame.pack();
		//frame.setVisible(true);

	

	
	}
	
	/**
	 * Exports a JFreeChart to a SVG file.
	 * 
	 * @param chart JFreeChart to export
	 * @param bounds the dimensions of the viewport
	 * @param svgFile the output file.
	 * @throws IOException if writing the svgFile fails.
	 */
	static void exportChartAsSVG(JFreeChart chart, Rectangle2D bounds, File svgFile) throws IOException {
        // Get a DOMImplementation and create an XML document
        DOMImplementation domImpl =
            GenericDOMImplementation.getDOMImplementation();
        Document document = domImpl.createDocument(null, "svg", null);

        // Create an instance of the SVG Generator
        SVGGraphics2D svgGenerator = new SVGGraphics2D(document);

        // draw the chart in the SVG generator
        chart.draw(svgGenerator, bounds);

        // Write svg file
        OutputStream outputStream = new FileOutputStream(svgFile);
        Writer out = new OutputStreamWriter(outputStream, "UTF-8");
        svgGenerator.stream(out, true /* use css */);						
        outputStream.flush();
        outputStream.close();
	}
	
}

