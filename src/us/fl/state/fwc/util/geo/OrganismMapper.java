package us.fl.state.fwc.util.geo;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import javax.swing.Timer;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.swing.JMapPane;

import us.fl.state.fwc.abem.organism.Organism;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Simple JMapPane to draw sub class implementations of 'Thing' agents.
 * 
 * @author wade.cooper
 *
 */
public class OrganismMapper extends JMapPane {


	private static final long serialVersionUID = 1L;
	//private ReferencedEnvelope spriteEnv;
	//	private Raster spriteBackground;
	//	private Rectangle backgroundBound; 
	private boolean firstDisplay = true;
	int radius = 4; 
	Color color = Color.red; 
	boolean useImage;
	private HashMap<Long, Organism> organismMap; 
	private HashMap<Long, Raster> backgroundMap; //the spriteBackgrounds mapped to the particle ID 

	ReferencedEnvelope oldViewBounds;

	Point currentPos;
	//Point prevPos; 
	
	boolean update = false; 
	private boolean updateDone = true; 
	
	
    // This animation will be driven by a timer which fires
    // every 200 milliseconds. Each time it fires the drawSprite
    // method is called to update the animation
    private Timer animationTimer = new Timer(50, new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            if (update) {
            	updateDone = false;
            	updateBypass(); //drawSprite();
            	updateDone = true;
            	update = false; 
            }
        }
    });
    
    
	public void setOrganismMap(HashMap<Long, Organism> organismMap){
		this.organismMap = organismMap; 
		backgroundMap = new HashMap<Long, Raster>(); 


	}


	@SuppressWarnings("static-access")
	@Override
	protected void paintComponent(Graphics g) {

        animationTimer.stop();

        //should hopefully halt draw and render until update is done
		while (!updateDone){
	        try { Thread.currentThread().sleep((long) (100)); } catch (InterruptedException e) { e.printStackTrace(); }
		}

		super.paintComponent(g);

	}




	@Override
	public void onRenderingCompleted() {
		super.onRenderingCompleted();

		if (!firstDisplay){
			backgroundMap.clear(); 
			updateBypass(); 
		}
        animationTimer.start();
	}


	
	
	public void updateBypass(){
		Graphics2D gr2D = (Graphics2D) getGraphics();
		gr2D.setColor(color); 

		ReferencedEnvelope viewBounds = getDisplayArea();

		//===========================
		// add the particles in the display bounds to redraw
		ArrayList<Organism> intersectors = new ArrayList<Organism>();

		Set<Long> parts = organismMap.keySet();
		Iterator<Long> it = parts.iterator();
		while (it.hasNext()){
			Long id = it.next();
			Organism agent = organismMap.get(id);
			Coordinate coord = agent.getCoords();
			
			double pX = coord.x;
			double pY = coord.y;
			if (pX > 180) pX = -(360d - pX);
			
			
			//if inside the display area, add it to the list to manipulate
			if (pX > viewBounds.getMinX() && pX < viewBounds.getMaxX() &&
					pY > viewBounds.getMinY() && pY < viewBounds.getMaxY() ) {
				intersectors.add(agent);
			}
		}


		//if they haven't been displayed before, or 
		if (backgroundMap.isEmpty()) {
			for (int i=0; i<intersectors.size(); i++){
				Organism agent = intersectors.get(i); 
				paintSprite(gr2D, agent);
			}
		}


		//if background map isn't empty, then need to erase old particles in bounds first 
		else {

			//first erase all intersecting particles
			for (int i=0; i<intersectors.size(); i++){
				Organism agent = intersectors.get(i); 
				eraseSprite(gr2D, agent);
			}

			//then repaint them all
			for (int i=0; i<intersectors.size(); i++){
				Organism agent = intersectors.get(i); 
				paintSprite(gr2D, agent);
			}

		}
		firstDisplay = false; 
	}
	
	



	public void setUpdate(){
		update = true; 
	}



	// Erase the sprite by replacing the background map section that
	// was cached when the sprite was last drawn.
	private void eraseSprite(Graphics2D gr2D, Organism agent) {

		Raster spriteBackground = backgroundMap.get(agent.getID());
		Rectangle rect = spriteBackground.getBounds();

		BufferedImage image = new BufferedImage(
				rect.width, rect.height, BufferedImage.TYPE_INT_ARGB);

		Raster child = spriteBackground.createChild(
				rect.x, rect.y, rect.width, rect.height, 0, 0, null);

		image.setData(child);

		gr2D.setBackground(getBackground());
		gr2D.clearRect(rect.x, rect.y, rect.width, rect.height);
		gr2D.drawImage(image, rect.x, rect.y, null);

		//remove the old background since will be reset when redrawn
		backgroundMap.remove(agent.getID()); 
	}







	// Paint the sprite: before displaying the sprite image we
	// cache that part of the background map image that will be
	// covered by the sprite.
	private void paintSprite(Graphics2D gr2D, Organism agent) {
		

		//set the new positions and get the new bounds
		Rectangle bounds = getSpritePosition(agent);

		//get a new background image for the new position
		Raster spriteBackground = getBaseImage().getData(bounds);
		backgroundMap.put(agent.getID(), spriteBackground);

		Rectangle screenBounds = getVisibleRect();

		if (bounds.getMinX() < screenBounds.getMinX() || bounds.getMaxX() > screenBounds.getMaxX() || bounds.getMinY() < screenBounds.getMinY() || bounds.getMaxY() > screenBounds.getMaxY()) return;


		gr2D.fillOval((int) Math.round(currentPos.x)-  radius, (int) Math.round(currentPos.y) - radius, radius*2, radius*2);
		//gr2D.drawLine(currentPos.x, currentPos.y, prevPos.x, prevPos.y);

		
	}








	// Set the sprite's intial position, and returns a rectangle of screen positions for the sprite draw space
	private Rectangle getSpritePosition(Organism agent) {
		

		Rectangle screenBounds = getVisibleRect();
		double pW = screenBounds.getWidth();
		double pH = screenBounds.getHeight();  
		//MapViewport view = getMapContext().getViewport(); 
		//ReferencedEnvelope viewBounds = view.getBounds();
		ReferencedEnvelope viewBounds = getDisplayArea();

		double minX = viewBounds.getMinX();
		double maxX = viewBounds.getMaxX();
		double minY = viewBounds.getMinY();
		double maxY = viewBounds.getMaxY();

		currentPos = new Point(); 
		Coordinate coord = agent.getCoords();
		double pX = coord.x;
		double pY = coord.y;
		if (pX > 180) pX = -(360d - pX);

		double propX = (pX - minX) / (maxX - minX);
		double propY = 1 - ((pY - minY) / (maxY - minY));
		currentPos.x = (int) Math.round(pW*propX); //+ paneOffset.x; 
		currentPos.y = (int) Math.round(pH*propY); //+ paneOffset.y; 



		int minX2=0, maxX2=0, minY2=0, maxY2=0;
		//set minimum x for bounds
		minX2 = currentPos.x-radius; 
		//set maxiumum x for bounds
		maxX2 = currentPos.x+radius; 
		//set minimum y for bounds
		minY2 = currentPos.y-radius; 
		//set maxiumum y for bounds
		maxY2 = currentPos.y+radius; 

		int w = maxX2-minX2+2;
		int h = maxY2-minY2+2;

		int x = screenBounds.x + minX2-1;
		int y = screenBounds.y + minY2-1; 

		Rectangle r = new Rectangle(x, y, w, h);

		return r; 
	}


	/**Removes an organism from the map
	 * 
	 * @param id
	 */
	public void remove(Organism agent) {
	       animationTimer.stop();

	        //should hopefully halt draw and render until update is done
			while (!updateDone){
		        try { Thread.currentThread().sleep((long) (100)); } catch (InterruptedException e) { e.printStackTrace(); }
			}

			Graphics2D gr2D = (Graphics2D) getGraphics();
			
			eraseSprite(gr2D, agent);
			
			organismMap.remove(agent.getID());		

			animationTimer.start();
		
	}



}
