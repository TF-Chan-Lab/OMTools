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

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import aldenjava.opticalmapping.data.annotation.AnnotationNode;
import aldenjava.opticalmapping.visualizer.ViewSetting;

public class VAnno extends VObject implements Comparable<VAnno>{
	public AnnotationNode anno;
	public VAnno(AnnotationNode anno)
	{
		if (anno == null)
			throw new NullPointerException("VBED must be initiated with proper BEDNode.");
		this.anno =anno;
		if (anno.getName() != null) {
			this.setToolTipText(anno.getName());
		}
	}

	@Override
	public long getDNALength() 
	{
		return anno.region.length() + ViewSetting.annotationTextLength;
	}

	@Override
	public void autoSetSize() 
	{
		setSize((int) (((anno.region.length() + ViewSetting.annotationTextLength) / dnaRatio) * ratio), (int) (ViewSetting.annotationBlockHeight * ratio));
		if (this.getWidth() == 0)
			this.setSize(1, this.getHeight());
	}

	@Override
	public void reorganize() {
	}

	@Override
	public void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics;
		Font font; 
		int fsize = 1;
		int h = this.getHeight();
		while (true) {
			font = new Font("Arial", Font.PLAIN, fsize + 1);
			int testHeight = getFontMetrics(font).getHeight();
			if (testHeight > h)
				break;
			fsize++;
		}
		
		font = new Font("Arial", Font.PLAIN, fsize);
		int leadingSpace = getFontMetrics(font).getLeading();
		int descentSpace = getFontMetrics(font).getDescent();
		g.setFont(font);
		g.setPaint(anno.getColor());
		
		g.fill(new Rectangle2D.Double(0, 0, (anno.region.length() / dnaRatio * ratio), this.getHeight()));
		
		g.drawString(anno.getName(), (int) (anno.region.length() / dnaRatio * ratio), this.getHeight() - leadingSpace - descentSpace);
	}

	@Override
	public int compareTo(VAnno vanno) {
		return this.anno.region.compareTo(vanno.anno.region);
	}

}
