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


package aldenjava.opticalmapping.mapper.seeding;

import java.util.List;
import java.util.concurrent.Callable;

public class SeedDatabaseWrapper implements Callable<List<Kmer>> {
	private SeedDatabase seedDatabase;
	private double ear;
	private int measure;
	private Kmer kmer;
	public SeedDatabaseWrapper(SeedDatabase seedDatabase) {
		this.seedDatabase = seedDatabase;
	}
	public void setParameters(int measure, double ear) {
		this.measure = measure;
		this.ear = ear;
	}
	public void setKmer(Kmer kmer) {
		this.kmer = kmer;
	}
	public Kmer getKmer() {
		return kmer;
	}
	@Override
	public List<Kmer> call() {
		return seedDatabase.getKmerList(kmer, ear, measure);
	}
}

