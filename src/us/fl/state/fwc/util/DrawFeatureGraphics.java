package us.fl.state.fwc.util;

import java.awt.Graphics;
import java.awt.Polygon;

import javax.swing.JPanel;

import javolution.util.FastTable;

import com.vividsolutions.jts.geom.Geometry;


public class DrawFeatureGraphics extends JPanel {

	private static final long serialVersionUID = 1L;
	
	FastTable<Geometry> geomList; 
	int xShift, yShift; 
	
	public DrawFeatureGraphics(FastTable<Geometry> g, int x, int y){
		geomList = g; 
		xShift = x;
		yShift = y; 
	}
	
	
	
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		Polygon p = new Polygon();

		int numCirclePoints = 12; 
		int radius = 200; 
		int midx=500;
		int midy = 500; 
		p.addPoint(midx+radius, midy); 


		double angleIncrement = (Math.PI*2)/numCirclePoints; // in radians, this is angle between each polygon point of circle, based on the total number of circle points defined in Sampler
		double angle = angleIncrement; // start at first increment

		for (int i=1; i<numCirclePoints; i++){
			p.addPoint(((int) (midx + (Math.cos(angle)*radius))), ((int) (midy + (Math.sin(angle)*radius))));  
			angle += angleIncrement; 
		}



		g.drawPolygon(p);

	}


}
