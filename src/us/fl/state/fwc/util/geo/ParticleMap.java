package us.fl.state.fwc.util.geo;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.swing.JMapPane;

import us.fl.state.fwc.abem.dispersal.bolts.Particle;

public class ParticleMap extends JMapPane {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Raster spriteBackground;
	private boolean firstDisplay = true;
	private boolean blocked = false; 
	private boolean blocked2 = false; 
	int radius = 4; 
	Color color = Color.red; 
	boolean useImage;
	private HashMap<Long, Particle> particleMap; 
	private HashMap<Long, Raster> backgroundMap; //the spriteBackgrounds mapped to the particle ID 

	private Particle p; 
	Point currentPos;
	Point prevPos; 

	// We override the JMapPane paintComponent method so that when
	// the map needs to be redrawn (e.g. after the frame has been
	// resized) the animation is stopped until rendering is complete.
	@Override
	protected void paintComponent(Graphics g) {
		while(blocked2){}
		getAndSetBlocked(true);  
		//animationTimer.stop();
		super.paintComponent(g);
	}

	public void setParticleMap(HashMap<Long, Particle> particleMap){
		this.particleMap = particleMap; 
		backgroundMap = new HashMap<Long, Raster>(); 
	}


	// We override the JMapPane onRenderingCompleted method to
	// restart the animation after the map has been drawn.
	@Override
	public void onRenderingCompleted() {
		super.onRenderingCompleted();
		//spriteBackground = null;
		//animationTimer.start();

		
		//after re-sizing, need to loop through all particles and re-set the background maps
		Set<Long> parts = particleMap.keySet();
		Iterator<Long> it = parts.iterator();
		while (it.hasNext()){
			Long id = it.next();
			p = particleMap.get(id); 
			Rectangle bounds = getSpritePosition();
			backgroundMap.put(p.getID(), getBaseImage().getData(bounds));
		}

		getAndSetBlocked(false);  
	}

	
	
	// This is the top-level animation method. It erases
	// the sprite (if showing), updates its position and then
	// draws it.
	public synchronized void update(Particle p) {

		this.p = particleMap.get(p.getID()); 

		getAndSetBlocked2(true);
		while(blocked){}

		Graphics2D gr2D = (Graphics2D) getGraphics();
		gr2D.setColor(color); 

		if (firstDisplay) {
			paintSprite(gr2D);
			firstDisplay=false;
		}
		else {
			spriteBackground = backgroundMap.get(p.getID()); 
			eraseSprite(gr2D);
			//moveSprite();
			paintSprite(gr2D);
		}
		getAndSetBlocked2(false);

	}


	// Erase the sprite by replacing the background map section that
	// was cached when the sprite was last drawn.
	private void eraseSprite(Graphics2D gr2D) {
		
//		Rectangle rect = spriteBackground.getBounds();
//		this.repaint(rect.x, rect.y, rect.width, rect.height); 
		if (spriteBackground != null) {
			Rectangle rect = spriteBackground.getBounds();

			BufferedImage image = new BufferedImage(
					rect.width, rect.height, BufferedImage.TYPE_INT_ARGB);

			Raster child = spriteBackground.createChild(
					rect.x, rect.y, rect.width, rect.height, 0, 0, null);

			image.setData(child);

			gr2D.setBackground(getBackground());
			gr2D.clearRect(rect.x, rect.y, rect.width, rect.height);
			gr2D.drawImage(image, rect.x, rect.y, null);
			spriteBackground = null;
		}
}

	// Update the sprite's location. 
/*	private void moveSprite() {
		spriteEnv.translate(p.getX()-p.getPX(), p.getY()-p.getPY());
	}
*/
	// Paint the sprite: before displaying the sprite image we
	// cache that part of the background map image that will be
	// covered by the sprite.
	private void paintSprite(Graphics2D gr2D) {
		Rectangle bounds = getSpritePosition();
		Rectangle screenBounds = getVisibleRect();

		if (bounds.getMinX() < screenBounds.getMinX() || bounds.getMaxX() > screenBounds.getMaxX() || bounds.getMinY() < screenBounds.getMinY() || bounds.getMaxY() > screenBounds.getMaxY()) return;
		spriteBackground = getBaseImage().getData(bounds);
		backgroundMap.put(p.getID(), spriteBackground); 

		gr2D.fillOval((int) Math.round(currentPos.x)-  radius, (int) Math.round(currentPos.y) - radius, radius*2, radius*2);
		gr2D.drawLine(currentPos.x, currentPos.y, prevPos.x, prevPos.y);

	}

	// Set the sprite's intial position, and returns a rectangle of screen positions for the sprite draw space
	private Rectangle getSpritePosition() {

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
		double pX = p.getX();
		double pY = p.getY();
		double pPX = p.getPX();
		double pPY = p.getPY();
		if (pX > 180) pX = -(360d - pX);
		if (pPX > 180) pPX = -(360d - pPX);

		double propX = (pX - minX) / (maxX - minX);
		double propY = 1 - ((pY - minY) / (maxY - minY));
		currentPos.x = (int) Math.round(pW*propX); //+ paneOffset.x; 
		currentPos.y = (int) Math.round(pH*propY); //+ paneOffset.y; 

		prevPos = new Point(); 
		propX = (pPX - minX) / (maxX - minX);
		propY = 1 - ((pPY - minY) / (maxY - minY));
		prevPos.x = (int) Math.round(pW*propX); //+ paneOffset.x; 
		prevPos.y = (int) Math.round(pH*propY); //+ paneOffset.y; 



		int minX2=0, maxX2=0, minY2=0, maxY2=0;
		//set minimum x for bounds
		if (prevPos.x < currentPos.x-radius) minX2 = prevPos.x;
		else minX2 = currentPos.x-radius; 
		//set maxiumum x for bounds
		if (prevPos.x > currentPos.x+radius) maxX2 = prevPos.x;
		else maxX2 = currentPos.x+radius; 
		//set minimum y for bounds
		if (prevPos.y < currentPos.y-radius) minY2 = prevPos.y;
		else minY2 = currentPos.y-radius; 
		//set maxiumum y for bounds
		if (prevPos.y > currentPos.y+radius) maxY2 = prevPos.y;
		else maxY2 = currentPos.y+radius; 

		int w = maxX2-minX2+2;
		int h = maxY2-minY2+2;

		int x = screenBounds.x + minX2-1;
		int y = screenBounds.y + minY2-1; 

		Rectangle r = new Rectangle(x, y, w, h);
	
		return r; 
	}

	// Get the position of the sprite as screen coordinates
	/*	private Rectangle getSpriteScreenPos() {
		AffineTransform tr = getWorldToScreenTransform();

		Point2D lowerCorner = new Point2D.Double(spriteEnv.getMinX(), spriteEnv.getMinY());
		Point2D upperCorner = new Point2D.Double(spriteEnv.getMaxX(), spriteEnv.getMaxY());

		Point2D p0 = tr.transform(lowerCorner, null);
		Point2D p1 = tr.transform(upperCorner, null);

		Rectangle r = new Rectangle();
		r.setFrameFromDiagonal(p0, p1);
		return r;
	}
	 */
	private synchronized void getAndSetBlocked(boolean blocked){
		if (blocked) blocked = true;
		if (!blocked) blocked = false; 
	}

	private synchronized void getAndSetBlocked2(boolean blocked){
		if (blocked) blocked = true;
		if (!blocked) blocked = false; 
	}


}
