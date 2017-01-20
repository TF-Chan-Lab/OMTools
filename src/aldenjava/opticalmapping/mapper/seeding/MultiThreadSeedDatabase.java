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


package aldenjava.opticalmapping.mapper.seeding;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import joptsimple.OptionSet;
import aldenjava.opticalmapping.mapper.AlignmentOptions;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;

public class MultiThreadSeedDatabase {
	private int nrOfProcessors;
	private SeedDatabaseWrapper[] sdws;
	private CompletionService<List<Kmer>> ecs;
	private ExecutorService es;
	private List<Future<List<Kmer>>> futureList;
	private SeedDatabase targetSeedDatabase;
	
	public MultiThreadSeedDatabase(SeedDatabase seedDatabase) {
		targetSeedDatabase = seedDatabase;
	}
	
	public void setParameters(OptionSet options) {
		this.setParameters((int) options.valueOf("meas"), (double) options.valueOf("ear"), (int) options.valueOf("thread"));
	}
	public void setParameters(int meas, double ear, int thread) {
		if (thread < 1)
			throw new IllegalArgumentException("Number of thread must be positive.");
		this.nrOfProcessors = thread;
		es = Executors.newFixedThreadPool(nrOfProcessors);
		ecs = new ExecutorCompletionService<List<Kmer>>(es);
		sdws = new SeedDatabaseWrapper[nrOfProcessors];
		futureList = new ArrayList<Future<List<Kmer>>>();
		for (int i = 0; i < nrOfProcessors; i++)
			futureList.add(null);

		for (int i = 0; i < nrOfProcessors; i++) {
			sdws[i] = new SeedDatabaseWrapper(targetSeedDatabase.copy());
			sdws[i].setParameters(meas, ear);
		}
	}

	public boolean startNext(Kmer kmer) {
		for (int i = 0; i < nrOfProcessors; i++)
			if (futureList.get(i) == null) {
				sdws[i].setKmer(kmer);
				futureList.set(i, ecs.submit(sdws[i]));
				return true;
			}
		return false;
	}

	public SeedingResultNode getNextResult() throws IllegalStateException, InterruptedException, ExecutionException {
		if (getStatus() == -1) // all tasks finish, nothing to wait
			throw new IllegalStateException("All results are taken");

//		Future<List<Kmer>> future = ecs.poll(5, TimeUnit.SECONDS);
		Future<List<Kmer>> future = ecs.take();
//		if (future == null)
//		for (int x = 0; x < nrOfProcessors; x++)
//			if (futureList.get(x) != null) {
//				System.out.println("Culprit: " + sdws[x].getKmer().source);
//			}
		int i = this.futureList.indexOf(future);
		SeedingResultNode seednode = new SeedingResultNode(sdws[i].getKmer(), futureList.get(i).get());
		futureList.set(i, null);
		return seednode;
	}

	public int getStatus() {
		boolean nothingRun = true;
		for (int i = 0; i < nrOfProcessors; i++)
			if (futureList.get(i) != null) {
				nothingRun = false;
				if (futureList.get(i).isDone())
					return 1;
			}
		if (nothingRun)
			return -1;
		else
			return 0;
	}
	public void close() {
		es.shutdown();
	}

	public static void assignOptions(ExtendOptionParser parser, int level) {
		parser.addHeader("Multithread Seeding Options", level);
		AlignmentOptions.assignErrorToleranceOptions(parser);
		parser.accepts("thread", "Number of threads").withRequiredArg().ofType(Integer.class).defaultsTo(1);
	}
}
