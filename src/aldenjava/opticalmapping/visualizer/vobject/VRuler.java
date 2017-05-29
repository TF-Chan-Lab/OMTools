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
import aldenjava.opticalmapping.visualizer.ViewSetting;

public class VRuler extends VObject{

	private SimpleLongLocation startEndPoint;
	private boolean reverse;
	private double scale;
	private boolean invert = false;
	public VRuler() {
		this.setSize(0, 0);
		this.reverse = false;
		this.scale = 1.000000000;
		this.invert = false;
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
	
	public void setInvert(boolean invert) {
		this.invert = invert;
	}
	@Override
	public void autoSetSize()
	{
		if (startEndPoint == null)
			this.setSize(0, 0);
		else
			this.setSize((int) ((startEndPoint.length()) / dnaRatio * ratio * scale), (int) (ViewSetting.rulerHeight));
	}
	@Override
	public void reorganize() {
	}

	@Override
	public void paintComponent(Graphics graphics)
	{
		if (startEndPoint == null)
			return;
		if (!ViewSetting.displayRuler)
			return;
		
		Graphics2D g = (Graphics2D) graphics;
		int vx = 0;
		int vy = 10;
		int markHeight = 5;
		int textCoordinateY = 30;
		if (invert) {
			vy = 20;
			markHeight = -5;
			textCoordinateY = g.getFontMetrics().getHeight();
		}
			
		
		
    	long mark;
    	// Small marks
    	g.setColor(ViewSetting.rulerSmallMarkColor);
    	g.setStroke(new BasicStroke(3));
    	if (!reverse)
    	{
    		mark = startEndPoint.min / ViewSetting.rulerSmallMark * ViewSetting.rulerSmallMark; 
	    	while (mark < startEndPoint.max)
	    	{
	    		if (mark >= startEndPoint.min)
	    			g.drawLine((int) (vx + (mark - startEndPoint.min) / dnaRatio * ratio * scale), vy - markHeight, (int) (vx + (mark - startEndPoint.min) / dnaRatio * ratio * scale), vy);
	    		mark += ViewSetting.rulerSmallMark;
	    	}
    	}
    	else
    	{
    		mark = (startEndPoint.max / ViewSetting.rulerSmallMark + 1) * ViewSetting.rulerSmallMark; // is there bug?
	    	while (mark > startEndPoint.min)
	    	{
	    		if (mark <= startEndPoint.max)
	    			g.drawLine((int) (vx + (startEndPoint.max - mark) / dnaRatio * ratio * scale), vy - markHeight, (int) (vx + (startEndPoint.max - mark) / dnaRatio * ratio * scale), vy);
	    		mark -= ViewSetting.rulerSmallMark;
	    		
	    	}
    	}
    	
    	// Large marks
		g.setColor(ViewSetting.rulerLargeMarkColor);
    	g.setStroke(new BasicStroke(5));
    	if (!reverse)
    	{
    		mark = startEndPoint.min / ViewSetting.rulerLargeMark * ViewSetting.rulerLargeMark;
    		while (mark < startEndPoint.max)
        	{
        		if (mark >= startEndPoint.min)
        		{
    				g.drawLine((int) (vx + (mark - startEndPoint.min) / dnaRatio * ratio * scale), vy - markHeight, (int) (vx + (mark - startEndPoint.min) / dnaRatio * ratio * scale), vy);
    				g.drawString(String.format("%d", mark), (int) (vx + (mark - startEndPoint.min) / dnaRatio * ratio * scale), textCoordinateY);
        		}
    	    	mark += ViewSetting.rulerLargeMark;
        	}
    	}
    	else
    	{
    		mark = startEndPoint.max / ViewSetting.rulerLargeMark * ViewSetting.rulerLargeMark;
	    	while (mark > startEndPoint.min)
        	{
	    		if (mark <= startEndPoint.max)
        		{
    				g.drawLine((int) (vx + (startEndPoint.max - mark) / dnaRatio * ratio * scale), vy - 5, (int) (vx + (startEndPoint.max - mark) / dnaRatio * ratio * scale), vy);
    				g.drawString(String.format("%d", mark), (int) (vx + (startEndPoint.max - mark) / dnaRatio * ratio * scale), textCoordinateY);
        		}
    	    	mark -= ViewSetting.rulerLargeMark;
        	}
    	}
    	
    	// Horizontal Bar
		g.setColor(ViewSetting.rulerBodyColor);
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