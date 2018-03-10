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


package aldenjava.opticalmapping.data.data;

import java.util.ArrayList;
import java.util.List;

//For future development
public class VirtualSignal {
	// We can recover which signal is not retained (FN)
	// We can recover which signal is extra (FP)
	// We can recover measuring error

	// We can recover scaling error
	
	
	
	public final List<VirtualSignal> sources; // For resolution error, they can come from multiple sources of reference signal
	public final ReferenceSignal refSig; 
	public long refp;
	public final static String FP = "FP";
	public VirtualSignal(List<VirtualSignal> sources) {
		this(sources, null);
	}
	public VirtualSignal(ReferenceSignal refSig) {
		this(null, refSig);
	}
	public VirtualSignal(List<VirtualSignal> sources, ReferenceSignal refSig) {
		this.sources = sources;
		this.refSig = refSig;
	}
	public VirtualSignal(String vs) {
		if (vs.equals(VirtualSignal.FP)) {
			this.sources = null;
			this.refSig = null;
		}
		else {
			if (vs.contains(",")) {
				List<VirtualSignal> sources = new ArrayList<>();
				for (String sourcestring : vs.split(","))
					sources.add(new VirtualSignal(sourcestring));
				this.sources = sources;
				this.refSig = null;
			}
			else {
				this.refSig = new ReferenceSignal(vs);
				this.sources = null;
			}
		}
	}

	/**
	 * Test if this virtual signal does not come from any reference signal, i.e. a false positive signal
	 * @return true if the virtual signal is a false positive signal
	 */
	public boolean isFPSignal() {
		if (sources != null) {
			for (VirtualSignal vs : sources)
				if (!vs.isFPSignal())
					return false;
			return true;
		}
		else
			return refSig == null;
		
	}

	/**
	 * Test if this virtual signal should be matched to the target reference signal
	 * @param targetSig
	 * @return true if this signal comes from the target reference signal
	 */
	public boolean matchTarget(ReferenceSignal targetSig) {
		if (sources != null) {
			for (VirtualSignal vs : sources)
				if (vs.matchTarget(targetSig))
					return true;
			return false;
		}
		else
			if (refSig != null)
				return this.refSig.equals(targetSig);
			else
				return targetSig == null;
	}
	@Override
	public String toString() {
		if (sources == null)
			if (refSig == null)
				return VirtualSignal.FP;
			else
				return refSig.toString();
		else {
			StringBuilder b = new StringBuilder();
			for (int j = 0; j < sources.size(); j++) {
				if (j > 0)
					b.append(",");
				b.append(sources.get(j).toString());
			}
			return b.toString();
		}

	}
}