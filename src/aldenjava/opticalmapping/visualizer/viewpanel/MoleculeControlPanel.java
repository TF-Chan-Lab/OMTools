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


package aldenjava.opticalmapping.visualizer.viewpanel;

import javax.swing.JOptionPane;

import aldenjava.opticalmapping.visualizer.OMView;

public class MoleculeControlPanel extends ControlPanel {

	public MoleculeControlPanel(OMView mainView) {
		super(mainView);
	}

	public MoleculeControlPanel(OMView mainView, MoleculeView... mViews) {
		super(mainView);
		createViewPanel(mViews);
		reorganizePanel();
	}

	@Override
	public void initializeTitle() {
		this.title = "Molecule view";
	};
	
	@Override
	public void createViewPanel() {
		this.createViewPanel(new MoleculeView(mainView));
	}

	@Override
	public void createViewPanel(ViewPanel... viewPanels) {
		super.createViewPanel(viewPanels);
	}

	@Override
	public void createAnnotationPanel() {
		JOptionPane.showMessageDialog(mainView, "Molecule view panel does not support annotation mode.");
	}

}
