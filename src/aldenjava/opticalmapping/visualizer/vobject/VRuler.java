/**************************************************************************
**  OMTools
**  A software package for processing and analyzing optical mapping data
**  
**  Version 1.2 -- January 1, 2017
**  
**  Copyright (C) 2017 by Alden Leung, Ting-Fung Chan, All rights reserved.
**  Contact:  alden.leung@gmail.com, tf.chan@cuhk.edu.hk
**  Organization:  School of Life Sciences, The Chinese University of Hong Kong,
**                 Shatin, NT, Hong Kong SAR
**  
**  This file is part of OMTools.
**  
**  OMTools is free software; you can redistribute it and/or 
**  modify it under the terms of the GNU General Public License 
**  as published by the Free Software Foundation; either version 
**  3 of the License, or (at your option) any later version.
**  
**  OMTools is distributed in the hope that it will be useful,
**  but WITHOUT ANY WARRANTY; without even the implied warranty of
**  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
**  GNU General Public License for more details.
**  
**  You should have received a copy of the GNU General Public 
**  License along with OMTools; if not, see 
**  <http://www.gnu.org/licenses/>.
**************************************************************************/


package aldenjava.opticalmapping.visualizer.vobject;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import aldenjava.common.SimpleLongLocation;

public class VRuler extends VObject{

	private SimpleLongLocation startEndPoint;
	public static long smallMark = 10000;
	public static long largeMark = 100000;
	public static boolean display = true;
	private boolean reverse;
	private double scale;
	public VRuler() 
	{
		this.setSize(0, 0);
		this.reverse = false;
		this.scale = 1.000000000;
	}
	public void setStartEndPoint(SimpleLongLocation startEndPoint) 
	{
		this.startEndPoint = startEndPoint;
		this.autoSetSize();
	}
	public void setReverse(boolean reverse)
	{
		this.reverse = reverse;
	}
	public void setScale(double scale)
	{
		this.scale = scale;
	}
	
	@Override
	public void autoSetSize()
	{
		if (startEndPoint == null)
			this.setSize(0, 0);
		else
			this.setSize((int) ((startEndPoint.length()) / dnaRatio * ratio * scale), (int) (30));
	}
	@Override
	public void reorganize() {
	}

	@Override
	public void paintComponent(Graphics graphics)
	{
		if (startEndPoint == null)
			return;
		if (!VRuler.display)
			return;
		
		Graphics2D g = (Graphics2D) graphics;
		int vx = 0;
		int vy = 10;
		
    	long mark;
    	// Small marks
    	g.setColor(new Color(127, 127, 127));
    	g.setStroke(new BasicStroke(3));
    	if (!reverse)
    	{
    		mark = startEndPoint.min / smallMark * smallMark; 
	    	while (mark < startEndPoint.max)
	    	{
	    		if (mark >= startEndPoint.min)
	    			g.drawLine((int) (vx + (mark - startEndPoint.min) / dnaRatio * ratio * scale), vy - 5, (int) (vx + (mark - startEndPoint.min) / dnaRatio * ratio * scale), vy);
	    		mark += smallMark;
	    	}
    	}
    	else
    	{
    		mark = (startEndPoint.max / smallMark + 1) * smallMark; // is there bug?
	    	while (mark > startEndPoint.min)
	    	{
	    		if (mark <= startEndPoint.max)
	    			g.drawLine((int) (vx + (startEndPoint.max - mark) / dnaRatio * ratio * scale), vy - 5, (int) (vx + (startEndPoint.max - mark) / dnaRatio * ratio * scale), vy);
	    		mark -= smallMark;
	    		
	    	}
    	}
    	
    	// Large marks
		g.setColor(Color.BLACK);
    	g.setStroke(new BasicStroke(5));
    	if (!reverse)
    	{
    		mark = startEndPoint.min / largeMark * largeMark;
    		while (mark < startEndPoint.max)
        	{
        		if (mark >= startEndPoint.min)
        		{
    				g.drawLine((int) (vx + (mark - startEndPoint.min) / dnaRatio * ratio * scale), vy - 5, (int) (vx + (mark - startEndPoint.min) / dnaRatio * ratio * scale), vy);
    				g.drawString(String.format("%d", mark), (int) (vx + (mark - startEndPoint.min) / dnaRatio * ratio * scale), vy + 20);
        		}
    	    	mark += largeMark;
        	}
    	}
    	else
    	{
    		mark = startEndPoint.max / largeMark * largeMark;
	    	while (mark > startEndPoint.min)
        	{
	    		if (mark <= startEndPoint.max)
        		{
    				g.drawLine((int) (vx + (startEndPoint.max - mark) / dnaRatio * ratio * scale), vy - 5, (int) (vx + (startEndPoint.max - mark) / dnaRatio * ratio * scale), vy);
    				g.drawString(String.format("%d", mark), (int) (vx + (startEndPoint.max - mark) / dnaRatio * ratio * scale), vy + 20);
        		}
    	    	mark -= largeMark;
        	}
    	}
    	
    	// Horizontal Bar
		g.setColor(Color.BLACK);
    	g.setStroke(new BasicStroke(5));
		g.drawLine(vx, vy, (int) (vx + (startEndPoint.length()) / dnaRatio * ratio * scale), vy);

	}
	@Override
	public long getDNALength() {
		if (startEndPoint == null)
			return 0;
		else
			return startEndPoint.length();
	}
}