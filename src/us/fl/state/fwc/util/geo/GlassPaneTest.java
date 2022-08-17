package us.fl.state.fwc.util.geo;

import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Point;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JComponent;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.MapContext;
import org.geotools.map.MapViewport;
import org.geotools.swing.JMapFrame;

import com.vividsolutions.jts.geom.Coordinate;

/**A GlassPane that maps the particles for a Geotools MapContext.
 * 
 * Note: to use, must set the GlassPane to the JMapFrame => frame.setGlassPane(ParticleMap) 
 * 
 * @author wade.cooper
 *
 */
public class GlassPaneTest extends JComponent {

	private static final long serialVersionUID = 1L;
	String shape;
	int radius; 
	Color color; 
	HashMap<Integer, Coordinate> coords = new HashMap<Integer, Coordinate>(); 
	JMapFrame frame; 
	
	public GlassPaneTest(JMapFrame frame){
		this.frame = frame; 
	}

	public void setStyle(String commonShape, Color color, int radius){
		this.shape = commonShape;
		this.color = color; 
		this.radius = radius;
	}


	protected void paintComponent(Graphics g) {

		
		g.setColor(color);
		if (shape.equals("circle")){

			Set<Integer> set = coords.keySet();
			Iterator<Integer> it = set.iterator();
			while (it.hasNext()) {
				Coordinate coord = coords.get(it.hasNext()); 
				Point point = getPixelLocation(coord);
				g.fillOval((int) Math.round(point.x)-  radius, (int) Math.round(point.y) - radius, radius*2, radius*2);
				
			}
		}
		else  {
			Set<Integer> set = coords.keySet();
			Iterator<Integer> it = set.iterator();
			while (it.hasNext()) {
				Coordinate coord = coords.get(it.next()); 
				Point point = getPixelLocation(coord);
				g.fillOval((int) Math.round(point.x)-  radius, (int) Math.round(point.y) - radius, radius*2, radius*2);
				
			}
		}

	}
	
	public Point getPixelLocation(Coordinate coord){
		Point point = new Point(); 
		
		//Rectangle rec = frame.getBounds(); 

		//JMapPane
		Container content = frame.getContentPane();
		//JMapPane mapPane = frame.getMapPane();
		double pW = content.getWidth(); //rec.getWidth()-8; //mapPane.getWidth(); //for whatever reason, the mapPane.getWidth() doesn't work for small windows sizes, but height seems to work fine
		double pH = content.getHeight(); //mapPane.getHeight(); 
		//Point paneOffset = mapPane.getLocation(); 
		
		
		MapContext map = frame.getMapContext(); 
		MapViewport view = map.getViewport(); 
		ReferencedEnvelope viewBounds = view.getBounds();
		double minX = viewBounds.getMinX();
		double maxX = viewBounds.getMaxX();
		double minY = viewBounds.getMinY();
		double maxY = viewBounds.getMaxY();
		
		double propX = (coord.x - minX) / (maxX - minX);
		double propY = 1 - ((coord.y - minY) / (maxY - minY));
		point.x = (int) Math.round(pW*propX); // + paneOffset.x; 
		point.y = (int) Math.round(pH*propY); // + paneOffset.y; 
		
		return point; 
	}
	
	
	
	public void updateCoord(Integer id, Coordinate newCoord){
		Coordinate oldCoord = coords.get(id);
		oldCoord.x = newCoord.x;
		oldCoord.y = newCoord.y;
		oldCoord.z = newCoord.z;
	}


	public void removeCoord(Integer id){
		coords.remove(id); 
	}
	
	public void addCoord(Integer id, Coordinate coord){
		coords.put(id, coord); 
	}
}
