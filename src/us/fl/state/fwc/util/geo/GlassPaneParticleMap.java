package us.fl.state.fwc.util.geo;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JComponent;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.MapContext;
import org.geotools.map.MapViewport;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.JMapPane;

import us.fl.state.fwc.abem.dispersal.bolts.Particle;

import com.vividsolutions.jts.geom.Coordinate;

/**A GlassPane that maps the particles for a Geotools MapContext.
 * 
 * Note: to use, must set the GlassPane to the JMapFrame => frame.setGlassPane(ParticleMap) 
 * 
 * @author wade.cooper
 *
 */
public class GlassPaneParticleMap extends JComponent {

	private static final long serialVersionUID = 1L;
	String shape;
	int radius; 
	Color color; 
	private HashMap<Long, Particle> particleMap; 
	JMapFrame frame; 
	boolean printPreviousPoint = false; 
	
	public GlassPaneParticleMap(HashMap<Long, Particle> particleMap, JMapFrame frame){
		this.particleMap = particleMap; 
		this.frame = frame; 
	}
	
	

	public void setStyle(String commonShape, Color color, int radius){
		this.shape = commonShape;
		this.color = color; 
		this.radius = radius;
	}

	public synchronized void update(Particle p) {

		//1: scroll through all particles in particleMap, and if their old bounds intersect, then repaintImmediatly(rectangle r) for the bounds of ONLY those particles that intersect
		
		
		//2: scroll through all particles, and paint their graphics
		
		Graphics2D gr2D = (Graphics2D) getGraphics();
		gr2D.setColor(color); 

	
	}
	
	protected void paintComponent(Graphics g) {

		if (particleMap.isEmpty()) return; 
		
		g.setColor(color);
		
		if (shape.equals("circle")){

			Set<Long> set = particleMap.keySet();
			Iterator<Long> it = set.iterator();
			while (it.hasNext()) {
				Particle part = particleMap.get(it.next());
				Coordinate coord = new Coordinate(part.getX(), part.getY(), 0); 
				if (coord.x > 180) coord.x = -(360d - coord.x);
				Point point = getPixelLocation(coord);
				g.fillOval((int) Math.round(point.x)-  radius, (int) Math.round(point.y) - radius, radius*2, radius*2);
				
				if (printPreviousPoint){
					//g.setColor(Color.black);
					
					Coordinate coord2 = new Coordinate(part.getPX(), part.getPY(), 0); 
					if (coord2.x > 180) coord2.x = -(360d - coord2.x);
					Point point2 = getPixelLocation(coord2);
					g.fillRect((int) Math.round(point2.x)-  radius/2, (int) Math.round(point2.y) - radius/2, radius, radius);
					g.drawLine(point.x, point.y, point2.x, point2.y);
				}

			}
		}

		else if (shape.equals("square")){

			Set<Long> set = particleMap.keySet();
			Iterator<Long> it = set.iterator();
			while (it.hasNext()) {
				Particle part = particleMap.get(it.hasNext());
				Coordinate coord = new Coordinate(part.getX(), part.getY(), 0); 
				if (coord.x > 180) coord.x = -(360d - coord.x);
				Point point = getPixelLocation(coord);
				g.fillRect((int) Math.round(point.x)-  radius, (int) Math.round(point.y) - radius, radius*2, radius*2);
				
				if (printPreviousPoint){
					g.setColor(Color.black);
					Coordinate coord2 = new Coordinate(part.getPX(), part.getPY(), 0); 
					if (coord2.x > 180) coord2.x = -(360d - coord2.x);
					Point point2 = getPixelLocation(coord2);
					g.fillRect((int) Math.round(point2.x)-  radius/2, (int) Math.round(point2.y) - radius/2, radius, radius);
					g.drawLine(point.x, point.y, point2.x, point2.y);
				}

			}
		}
		//default = circle
		else  {
			Set<Long> set = particleMap.keySet();
			Iterator<Long> it = set.iterator();
			while (it.hasNext()) {
				Particle part = particleMap.get(it.hasNext());
				Coordinate coord = new Coordinate(part.getX(), part.getY(), 0); 
				if (coord.x > 180) coord.x = -(360d - coord.x);
				Point point = getPixelLocation(coord);
				g.fillOval((int) Math.round(point.x)-  radius, (int) Math.round(point.y) - radius, radius*2, radius*2);
				
				if (printPreviousPoint){
					g.setColor(Color.black);
					Coordinate coord2 = new Coordinate(part.getPX(), part.getPY(), 0); 
					if (coord2.x > 180) coord2.x = -(360d - coord2.x);
					Point point2 = getPixelLocation(coord2);
					g.fillRect((int) Math.round(point2.x)-  radius/2, (int) Math.round(point2.y) - radius/2, radius, radius);
					g.drawLine(point.x, point.y, point2.x, point2.y);
				}
				
			}
		}

	}
	
	
	
	public Point getPixelLocation(Coordinate coord){
		Point point = new Point(); 
		
		Rectangle rec = frame.getBounds(); 

		//JMapPane
		//Container content = frame.getContentPane();
		JMapPane mapPane = frame.getMapPane();
		double pW = rec.getWidth()-8; //mapPane.getWidth(); //for whatever reason, the mapPane.getWidth() doesn't work for small windows sizes, but height seems to work fine
		double pH = mapPane.getHeight(); //mapPane.getHeight(); 
		Point paneOffset = mapPane.getLocation(); 
		
		
		MapContext map = frame.getMapContext(); 
		MapViewport view = map.getViewport(); 
		ReferencedEnvelope viewBounds = view.getBounds();
		double minX = viewBounds.getMinX();
		double maxX = viewBounds.getMaxX();
		double minY = viewBounds.getMinY();
		double maxY = viewBounds.getMaxY();
		
		double propX = (coord.x - minX) / (maxX - minX);
		double propY = 1 - ((coord.y - minY) / (maxY - minY));
		point.x = (int) Math.round(pW*propX)+ paneOffset.x; 
		point.y = (int) Math.round(pH*propY)+ paneOffset.y; 
		
		return point; 
	}
	
	
	public void setPrintPreviousPoint(boolean bool){
		this.printPreviousPoint = bool; 
	}
	
}
