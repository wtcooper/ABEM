package us.fl.state.fwc.abem.test;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Polygon;

import javolution.util.FastTable;
import us.fl.state.fwc.util.Int3D;

import com.vividsolutions.jts.geom.Coordinate;

public class PolygonDraw extends Frame {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected FastTable<Coordinate> testSites; 
	protected FastTable<Coordinate> polyCoords; 
	private int scaleFactor=2; 

	private int gridWidth, gridHeight; 

	public PolygonDraw(int gridWidth, int gridHeight, FastTable<Coordinate> polyCoords, FastTable<Coordinate> testSites){
		this.polyCoords = polyCoords; 
		this.testSites = testSites; 
		this.gridWidth = gridWidth;
		this.gridHeight=gridHeight; 

		System.out.println("poly size: " + this.polyCoords.size()); 
		
		setSize(gridWidth, gridHeight);
		
	}

	
/*	public void drawPolygons(int gridWidth, int gridHeight, FastTable<Coordinate> polyCoords, FastTable<Coordinate> testSites){

		JFrame frame = new JFrame();
		frame.setTitle("DrawPoly");
		frame.setSize(gridWidth, gridHeight);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		Container contentPane = frame.getContentPane();
		contentPane.add(new PolygonDraw(gridWidth, gridHeight, polyCoords, testSites));

		frame.setVisible(true); 

	}
*/


	public void paint(Graphics g) {

		for (int i = 0; i < testSites.size(); i++) {
			g.setColor(Color.BLACK);
			g.fillRect((int)testSites.get(i).x, (int)testSites.get(i).y, 4, 4);
		}

		g.setColor(Color.RED);
		g.fillOval(500,500,4,4); 
		//g.fillOval(500*scaleFactor-3, 500*scaleFactor-3, scaleFactor*2, scaleFactor*2);



	
		Polygon p = new Polygon();

		for (int i = 0; i < polyCoords.size(); i++) {
			Int3D point = new Int3D ((int) polyCoords.get(i).x, (int) polyCoords.get(i).y); 
			int x = point.getX();
			int y = point.getY(); 
			p.addPoint(x, y);
		}
		g.drawPolygon(p);


	}


}

