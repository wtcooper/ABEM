package us.fl.state.fwc.util;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import javolution.util.FastTable;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;


public class DrawFeatures {

	private GeometryFactory gf = new GeometryFactory();
	double minX=1000000, minY=1000000, maxX=0, maxY=0; // min and max of 
	int xShift=0, yShift=0; // amount to substract from actual coordinate values to standardize the screen to 0,0
	int xBuffer=0, yBuffer=0; // amount of edge around frame

	public void drawFeatures(Coordinate[][] coordArrays) {

		FastTable<Geometry> geomList = makeGeometries(coordArrays); 
		
		Toolkit toolkit = Toolkit.getDefaultToolkit(); // Get the default toolkit
		Dimension scrnsize = toolkit.getScreenSize(); // Get the current screen size
		
		
		JFrame frame = new JFrame();
		frame.setTitle("DrawPoly");

		if ( (((int)(maxX-minX+xBuffer)) >= scrnsize.getWidth()) ||  (((int)(maxY-minY+yBuffer)) >= scrnsize.getHeight()) )   frame.setSize((int) scrnsize.getWidth(), (int) scrnsize.getHeight());
		else  frame.setSize(((int)(maxX-minX+xBuffer)), ((int)(maxY-minY+yBuffer))); 
		
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		Container contentPane = frame.getContentPane();
		contentPane.add(new DrawFeatureGraphics(geomList, xShift, yShift));

		frame.setVisible(true); 
	}
	
	
	public FastTable<Geometry>makeGeometries(Coordinate[][] coordArrays){
		
		FastTable<Geometry> geomList = FastTable.newInstance(); 

		for (int i=0; i<coordArrays.length; i++){
			Geometry newGeom = null; 
			if (coordArrays[i].length == 1 ) newGeom = (Geometry) gf.createPoint(coordArrays[i][0]); 
			else if (coordArrays[i].length ==2 ) newGeom = (Geometry) gf.createLineString(coordArrays[i]); 
			else if (coordArrays[i].length >2)  newGeom = (Geometry) gf.createPolygon(new LinearRing(new CoordinateArraySequence(coordArrays[i]), gf), null); 	

			geomList.add(newGeom); 
			Envelope env = newGeom.getEnvelopeInternal(); 
			if (env.getMinX() < minX	) minX=env.getMinX();
			if (env.getMaxX() > maxX	) maxX=env.getMaxX();
			if (env.getMinY() < minY) minY=env.getMinY();
			if (env.getMaxY() > maxY	) maxY=env.getMaxY();
		}
		
		xShift = (int) minX-xBuffer; 
		yShift = (int) minY-yBuffer; 

		return geomList; 
	}
	
	
}
