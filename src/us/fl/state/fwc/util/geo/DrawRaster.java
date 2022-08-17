package us.fl.state.fwc.util.geo;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JFrame;


public class DrawRaster extends JFrame {

	private static final long serialVersionUID = 1L;
	public int gridXDim, gridYDim, scaleFactor; 
	public int data[][];
	public int dataScaled[][]; 
	ColorKeys colors = new ColorKeys(); 


	// constructor, sets the gridWidth and the gridHeight to the main simulation
	public DrawRaster (int[][] data, int scale, Color[] colorArray) {
		if (colorArray != null) colors.setColors(colorArray);  

		this.gridXDim = data[0].length; 
		this.gridYDim = data.length;
		this.scaleFactor = scale;
		this.data = data; 
		dataScaled = new int[gridYDim*scaleFactor][gridXDim*scaleFactor];

		for (int i = 0; i < gridYDim; i++) {
			for (int j = 0; j < gridXDim; j++) {
				dataScaled[i*scaleFactor][j*scaleFactor] = data[i][j];
			}
		}

		setSize(gridXDim*scaleFactor, gridYDim*scaleFactor);

		this.setVisible(true); 
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}


	public void paint(Graphics g) {
		for (int i = 0; i < gridYDim*scaleFactor; i+=scaleFactor) {
			for (int j = 0; j < gridXDim*scaleFactor; j+=scaleFactor) {
				
				g.setColor(colors.getKeyColor(dataScaled[i][j])); 
				g.fillRect(i, j, i+(scaleFactor-1), j+(scaleFactor-1)); 
				
/*				if (dataScaled[i][j] == 0) {
					g.setColor(Color.BLACK);
					g.fillRect(i, j, i+(scaleFactor-1), j+(scaleFactor-1)); 
				}
				else if (dataScaled[i][j] == 1) {
					g.setColor(Color.GRAY);
					g.fillRect(i, j, i+(scaleFactor-1), j+(scaleFactor-1)); 
				}
				else if (dataScaled[i][j] == 2) {
					g.setColor(Color.WHITE);
					g.fillRect(i, j, i+(scaleFactor-1), j+(scaleFactor-1)); 
				}
				else if (dataScaled[i][j] == 3) {
					g.setColor(Color.BLUE);
					g.fillRect(i, j, i+(scaleFactor-1), j+(scaleFactor-1)); 
				}
				else if (dataScaled[i][j] == 4) {
					g.setColor(Color.GREEN);
					g.fillRect(i, j, i+(scaleFactor-1), j+(scaleFactor-1)); 
				}
				else if (dataScaled[i][j] == 5) {
					g.setColor(Color.CYAN);
					g.fillRect(i, j, i+(scaleFactor-1), j+(scaleFactor-1)); 
				}
				else if (dataScaled[i][j] == 6) {
					g.setColor(Color.ORANGE);
					g.fillRect(i, j, i+(scaleFactor-1), j+(scaleFactor-1)); 
				}
				else if (dataScaled[i][j] == 7) {
					g.setColor(Color.YELLOW);
					g.fillRect(i, j, i+(scaleFactor-1), j+(scaleFactor-1)); 
				}
*/
			}
		}
	}



}
