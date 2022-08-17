package us.fl.state.fwc.abem.test;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;

import javolution.util.FastTable;

import com.vividsolutions.jts.geom.Coordinate;

public class HabSearchDraw extends Frame {

	FastTable<Coordinate> coordList = new FastTable<Coordinate>(); 
	int gridWidth = 200, gridHeight=200, scaleFactor = 3; 
	public int[][] landscape ; 
	public int[][] landscapeScaled = new int[gridWidth*scaleFactor][gridHeight*scaleFactor] ; 

	
	
	
	
	public HabSearchDraw(int[][] landscape, FastTable<Coordinate> coordList){
		this.landscape = landscape; 
		this.coordList = coordList; 
		
		for (int i = 0; i < gridWidth; i++) {
	    	for (int j = 0; j < gridHeight; j++) {
	    		landscapeScaled[i*scaleFactor][j*scaleFactor] = landscape[i][j];
	    	}
		}

		
		
		setSize(gridWidth*scaleFactor, gridHeight*scaleFactor);
		
	}
	
	
	public void paint(Graphics g) {

		for (int i = 0; i < gridWidth*scaleFactor; i+=scaleFactor) {
	    	for (int j = 0; j < gridHeight*scaleFactor; j+=scaleFactor) {

	    		if (landscapeScaled[i][j] == 9116) {
	    			g.setColor(Color.GREEN);
	    			g.fillRect(i, j, i+(scaleFactor-1), j+(scaleFactor-1)); 
	    		}
	    		else if (landscapeScaled[i][j] == 9113) {			// bare
	    			g.setColor(Color.GREEN);
	    			g.fillRect(i, j, i+(scaleFactor-1), j+(scaleFactor-1)); 
	    		}
	    		else if (landscapeScaled[i][j] == 9121) {			// ccaBad
	    			g.setColor(Color.LIGHT_GRAY);
	    			g.fillRect(i, j, i+(scaleFactor-1), j+(scaleFactor-1)); 
	    		}
	    		else if (landscapeScaled[i][j] == 5700){
	    		g.setColor(Color.ORANGE);
    			g.fillRect(i, j, i+(scaleFactor-1), j+(scaleFactor-1)); 
	    		}
	    		else {
		    		g.setColor(Color.WHITE);
	    			g.fillRect(i, j, i+(scaleFactor-1), j+(scaleFactor-1)); 
	    			
	    		}
	    	}
		}
		

		//g.fillRect(100*scaleFactor, 100*scaleFactor, scaleFactor, scaleFactor); 

		for (int i = 0; i < coordList.size(); i++) {

			
	    			g.setColor(Color.BLACK);
	    			g.fillRect((int)coordList.get(i).x*scaleFactor, (int)coordList.get(i).y*scaleFactor, scaleFactor, scaleFactor);
		}
		g.setColor(Color.RED);
		g.fillOval(100*scaleFactor-3, 100*scaleFactor-3, scaleFactor*2, scaleFactor*2);

	}
	
}
