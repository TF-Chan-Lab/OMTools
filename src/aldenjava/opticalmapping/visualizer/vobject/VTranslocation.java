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

import aldenjava.opticalmapping.GenomicPosNode;
import aldenjava.opticalmapping.visualizer.ViewSetting;

public class VTranslocation extends VSpace {

	private int direction;
	private GenomicPosNode region;

	public VTranslocation(GenomicPosNode region, int direction) {
		super(ViewSetting.SVObjectSize, 0);
		this.direction = direction;
		this.region = region;
		this.setToolTipText(String.format("%s: %s", this.getType(), region.toString()));
	}

	@Override
	public void autoSetSize() {
		this.setSize((int) (getDNALength() / dnaRatio * ratio), (int) (ViewSetting.bodyHeight * ratio));
	}

	@Override
	public void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		Graphics2D g = (Graphics2D) graphics;
		g.setStroke(new BasicStroke(2));
		g.setPaint(Color.BLACK);
		int lineWidth = (int) (ViewSetting.SVObjectSize * 2 / 5 / dnaRatio * ratio);
		int recWidth = (int) (ViewSetting.SVObjectSize * 3 / 5 / dnaRatio * ratio);
		if (direction == 1) {
			g.drawLine(0, this.getHeight() / 5 * 2, lineWidth, this.getHeight() / 5 * 2);
			g.drawLine(0, this.getHeight() / 5 * 3, lineWidth, this.getHeight() / 5 * 3);
			g.drawRect(lineWidth, this.getHeight() / 2 - recWidth / 2, recWidth, recWidth);
		} else if (direction == -1) {
			g.drawRect(0, this.getHeight() / 2 - recWidth / 2, recWidth, recWidth);
			g.drawLine(recWidth, this.getHeight() / 5 * 2, recWidth + lineWidth, this.getHeight() / 5 * 2);
			g.drawLine(recWidth, this.getHeight() / 5 * 3, recWidth + lineWidth, this.getHeight() / 5 * 3);
		}

	}

	@Override
	public String getType() {
		return "Translocation";
	}

}
