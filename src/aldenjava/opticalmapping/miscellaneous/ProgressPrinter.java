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


package aldenjava.opticalmapping.miscellaneous;


public class ProgressPrinter {

	private final Integer totalSteps;
	private final Integer leap;
	private final Long millisecond;
	private long prevPrintTime; 
	private int step = 0; 
	public ProgressPrinter(Integer totalSteps, Integer leap) {
		super();
		this.totalSteps = totalSteps;
		this.leap = leap;
		this.millisecond = null;
	}

	public ProgressPrinter(Integer totalSteps, Long millisecond) {
		super();
		this.totalSteps = totalSteps;
		this.leap = null;
		this.millisecond = millisecond;
		this.prevPrintTime = System.currentTimeMillis();
	}

	public void printProgress() {
		if (totalSteps != null)
			VerbosePrinter.println(step + " / " + totalSteps + " completed.");
		else
			VerbosePrinter.println(step + " items completed.");
	}
	public void update() {
		step++;
		if (totalSteps != null && step == totalSteps) {
			printProgress();
			return;
		}
		
		if (millisecond != null) {
			if (System.currentTimeMillis() - prevPrintTime >= millisecond) {
				printProgress();
				prevPrintTime = System.currentTimeMillis();
			}
		}
		if (leap != null) {
			if (step % leap == 0) {
				printProgress();
			}
		}
	}
}
