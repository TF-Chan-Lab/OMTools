/**************************************************************************
**  OMTools
**  A software package for processing and analyzing optical mapping data
**  
**  Version 1.4 -- March 10, 2018
**  
**  Copyright (C) 2018 by Alden Leung, Ting-Fung Chan, All rights reserved.
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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import aldenjava.common.SimpleLongLocation;
import aldenjava.common.UnweightedRange;
import aldenjava.common.WeightedRange;
import aldenjava.opticalmapping.visualizer.ViewSetting;

public class VVariability extends VObject {

	private SimpleLongLocation startEndPoint;
	private List<WeightedRange<Long, Double>> variabilityRanges;
	
	public VVariability() {
		this(new ArrayList<>());
	}
	public VVariability(List<WeightedRange<Long, Double>> variabilityRanges) {
		this.variabilityRanges = variabilityRanges;
	}
	
	public void setVariabilities(List<WeightedRange<Long, Double>> variabilityRanges) {
		this.variabilityRanges = variabilityRanges;
	}
	public void setStartEndPoint(SimpleLongLocation startEndPoint) {
		this.startEndPoint = startEndPoint;
		this.autoSetSize();
	}
	
	@Override
	public long getDNALength() {
		return startEndPoint.length();
	}

	@Override
	public void autoSetSize() {
		this.setSize((int) (getDNALength() / dnaRatio * ratio), (int) (ViewSetting.variabilityHeight * ratio));
	}


	@Override
	public void reorganize() {
	}
	@Override
	public void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		Graphics2D g = (Graphics2D) graphics;
		g.setPaint(ViewSetting.coverageBGColor);
		g.fillRect(0, 0, getWidth(), getHeight());

		
		double max = 0;
		for (WeightedRange<Long, Double> range : variabilityRanges)
			if (max < range.weight - 1)
				max = range.weight - 1;
		for (WeightedRange<Long, Double> range : variabilityRanges) {
			double colorRatio = (range.weight - 1) / max;
//			double colorRatio = range.weight / max;
			
			if (colorRatio > 1)
				colorRatio = 1;
			int red = (int) Math.abs((colorRatio * ViewSetting.variationColor2.getRed()) + ((1 - colorRatio) * ViewSetting.variationColor1.getRed()));
			int green = (int) Math.abs((colorRatio * ViewSetting.variationColor2.getGreen()) + ((1 - colorRatio) * ViewSetting.variationColor1.getGreen()));
			int blue = (int) Math.abs((colorRatio * ViewSetting.variationColor2.getBlue()) + ((1 - colorRatio) * ViewSetting.variationColor1.getBlue()));
			g.setPaint(new Color(red, green, blue));
			Rectangle2D rect = new Rectangle2D.Double(((range.min - startEndPoint.min) / dnaRatio * ratio), 0, (range.length() / dnaRatio * ratio), (ViewSetting.variabilityHeight * ratio));
			g.fill(rect);
		}
		
		
	}

	public double getAverageVariability(UnweightedRange<Long> targetRange) {
		double totalVariability = 0;
		double max = 0;

		for (WeightedRange<Long, Double> range : variabilityRanges)
			if (max < range.weight)
				max = range.weight;
//		max = 68;
//		max 
		for (WeightedRange<Long, Double> range : variabilityRanges)
			if (range.overlap(targetRange))
				totalVariability += range.overlapSize(targetRange) / (double) targetRange.length() * range.weight;
		
		return totalVariability / max;
	}
}
