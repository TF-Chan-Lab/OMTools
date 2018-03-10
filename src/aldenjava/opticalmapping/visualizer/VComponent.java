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


package aldenjava.opticalmapping.visualizer;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JComponent;

public abstract class VComponent extends JComponent implements MouseListener{
	protected double dnaRatio = ViewSetting.defaultDNARatio;
	protected double ratio = ViewSetting.defaultZoom;
	public VComponent()
	{
	}
	public void setDNARatio(double dnaRatio)
	{
		this.dnaRatio = dnaRatio;
		autoSetSize();
		reorganize();
	}
	public void setRatio(double ratio)
	{
		this.ratio = ratio;
		autoSetSize();
		reorganize();
	}
	public double limitRatio(double ratio) {
		assert ViewSetting.maxZoom >= ViewSetting.minZoom;
		double newRatio = ratio;
		if (ratio > ViewSetting.maxZoom)
			newRatio = ViewSetting.maxZoom;
		if (ratio < ViewSetting.minZoom)
			newRatio = ViewSetting.minZoom;
		return newRatio;
	}
	public abstract void autoSetSize();
	public abstract void reorganize();

	@Override
	public void mouseClicked(MouseEvent e) {
	}
	@Override
	public void mouseEntered(MouseEvent e) {
	}
	@Override
	public void mouseExited(MouseEvent e) {
	}
	@Override
	public void mousePressed(MouseEvent e) {
	}
	@Override
	public void mouseReleased(MouseEvent e) {
	}

}
