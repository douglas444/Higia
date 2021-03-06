package main.java.higia;
/*
 *    kNN.java
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *    
 */

import java.io.IOException;
import java.util.*;

import java.util.stream.Collectors;

import br.com.douglas444.datastreamenv.common.ConceptCategory;
import br.com.douglas444.datastreamenv.common.ConceptClassificationContext;
import br.com.douglas444.mltk.datastructure.ClusterSummary;
import br.com.douglas444.mltk.datastructure.PseudoPoint;
import br.com.douglas444.mltk.datastructure.Sample;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.MultiClassClassifier;
import moa.classifiers.lazy.neighboursearch.KDTree;
import moa.classifiers.lazy.neighboursearch.LinearNNSearch;
import moa.classifiers.lazy.neighboursearch.NearestNeighbourSearch;
import moa.core.Measurement;
import moa.gui.visualization.DataPoint;
import main.java.higia.utils.ConDis;
import main.java.higia.utils.DriftEvolution;
import main.java.higia.utils.InstanceKernel;
import main.java.higia.utils.MicroCluster;
import main.java.higia.utils.NearestNeighbours;
import main.java.higia.Clusters.ClusteringBla;
import main.java.higia.Clusters.SummClusters;

import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.MultiChoiceOption;

/**
 * k Nearest Neighbor.
 * <p>
 *
 * Valid options are:
 * <p>
 *
 * -k number of neighbours <br>
 * -m max instances <br>
 *
 * @author Jesse Read (jesse@tsc.uc3m.es)
 * @version 03.2012
 */
public class kNN_kem extends AbstractClassifier implements MultiClassClassifier {
	private static final long serialVersionUID = 1L;

	private boolean initialized;
	private boolean newInitialized;

	public static HigiaInterceptor interceptor = new HigiaInterceptor();

	public IntOption kOption = new IntOption("k", 'k', "The number of neighbors", 10, 1, Integer.MAX_VALUE);

	public IntOption limitOption = new IntOption("limit", 'w', "The maximum number of instances to store", 1000, 1,
			Integer.MAX_VALUE);

	public int limit_Option = 1000;

	protected double prob;

	public MultiChoiceOption nearestNeighbourSearchOption = new MultiChoiceOption("nearestNeighbourSearch", 'n',
			"Nearest Neighbour Search to use", new String[] { "LinearNN", "KDTree" },
			new String[] { "Brute force search algorithm for nearest neighbour search. ",
					"KDTree search algorithm for nearest neighbour search" },
			0);

	ArrayList<String> classes;

	int C = 0;

	@Override
	public String getPurposeString() {
		return "kNN: special.";
	}

	protected Instances window;
	long timestamp = -1;

	double threshold = 1.1;
	double kernel = 1;
	double maxClusters = 0;
	double learningRate = 0.5;
	
	String out1 = "";

	// this is just a test
	// double kernel

	ArrayList<MicroCluster> windowKernel;
	ArrayList<MicroCluster> neWindow;
	ArrayList<ConDis> conSeDist;
	ArrayList<DriftEvolution> driftList;

	SummClusters clusters;
	ClusteringBla clusteringBla;

	@Override
	public void setModelContext(InstancesHeader context) {
		try {
			this.classes = new ArrayList<>();
			this.windowKernel = new ArrayList<>();
			this.neWindow = new ArrayList<>();
			this.conSeDist = new ArrayList<>();
			this.driftList = new ArrayList<>();
			this.window = new Instances(context, 0);
			this.window.setClassIndex(context.classIndex());

			clusteringBla = new ClusteringBla();

		} catch (Exception e) {
			System.err.println("Error: no Model Context available.");
			e.printStackTrace();
			System.exit(1);
		}
	}

	@Override
	public void resetLearningImpl() {
		this.windowKernel = null;
		this.neWindow = null;
		this.initialized = false;
		this.newInitialized = false;
		this.classes = null;
		clusters = new SummClusters();
	}

	@Override
	public void trainOnInstanceImpl(Instance inst) {

		if (inst.classValue() > C)
			C = (int) inst.classValue();

		if (this.window == null) {
			this.window = new Instances(inst.dataset());
		}
		// remove instances
		if (this.limitOption.getValue() <= this.window.size()) {
			this.window.delete(0);
		}
		this.window.add(inst);
	}

	public void setLimit_Option(int size) {
		this.limit_Option = size;
	}

	public void setLearningRate(double rate) {
		this.learningRate = rate;
	}

	public void trainOnInstance(MicroCluster inst, int minClusters, DataPoint dp) throws Exception {
		timestamp++;

		int lineSize = inst.getCenter().length - 1;
		double[] data = new double[lineSize];

		for (int j = 0; j < lineSize; j++) {
			data[j] = inst.getCenter()[j];
		}

		// window is empty -> initial training phase
		if (!initialized) {

			// amount of classes before test phase
			if (classes.indexOf(inst.getLabel()) == -1) {
				classes.add(inst.getLabel());
			}

			// first elements of the window
			if (this.windowKernel.size() < this.limit_Option) {
				this.windowKernel.add(inst);

			} else {

				initialized = true;

				// reduce prototypes
				ArrayList<MicroCluster> micros = clusteringBla.CluStream(this.windowKernel, minClusters, classes,
						timestamp, dp);
				this.windowKernel.clear();
				this.windowKernel = micros;
				this.maxClusters = this.windowKernel.size();
				System.out.println(maxClusters);
//				this.maxClusters = this.maxClusters * 1.2;
//				System.out.println(minClusters);
			}
		}

	}

	public boolean testEstupido(Instance inst) {

		int lineSize = inst.toDoubleArray().length - 1;
		double[] data = new double[lineSize];

		for (int j = 0; j < lineSize; j++) {
			data[j] = inst.toDoubleArray()[j];
		}

		Instance inst2 = new DenseInstance(1, data);

		InstanceKernel inKe = new InstanceKernel(inst2, inst2.numAttributes(), timestamp);

//		// get kOption neighbours 
		ArrayList<NearestNeighbours> neighbours = kClosestNeighbor(inKe, kOption.getValue());
		double[] votes = new double[classes.size()];
		double minDist = Double.MAX_VALUE;
		int index = 1;

//		System.out.println("$$$$$$$$");
		for (int i = 0; i < neighbours.size(); i++) {

			double foo = Double.parseDouble(neighbours.get(i).getKernel().getLabel());
//			// if the dist is less or equal to the radius of the closest cluster
			votes[(int) foo]++;
			index = neighbours.get(i).getIndex();

			// minimal dist to the closets cluster
//			double dist = distance(data, neighbours.get(i).getKernel().getCenter());
//			System.out.println(Arrays.toString(data));
//			System.out.println("distancia " + dist);
//			System.out.println("i " + i);
//			System.out.println("label " + neighbours.get(i).getKernel().getLabel());
//			System.out.println("center nei " + Arrays.toString(neighbours.get(i).getKernel().getCenter()));
//			System.out.println("center nei index " + neighbours.get(i).getIndex());
//			if (dist < minDist) {
//				minDist = dist;
//				index = neighbours.get(i).getIndex();
//			}

		}

//		System.out.println("votes " + (double)max(votes));
//		System.out.println("votes " + Arrays.toString(votes));
//		System.out.println("closest predicted " + windowKernel.get(index).getLabel());		
//		System.out.println("center real " + Arrays.toString( windowKernel.get(index).getCenter()));
//		System.out.println("center index " + index);
//		System.out.println("real " + inst.toDoubleArray()[lineSize]);

		if ((double) max(votes) == inst.classValue()) {

			return true;
		}

		int o = 0;
		o++;
		return false;
	}

	public void modelUpdate() throws IOException {

		// 3.2 Merge closest two kernels
		int closestA = 0;
		int closestB = 0;
		double minDistance = Double.MAX_VALUE;

		double radiusB = 0;
		double radiusA = 0;
		for (int i = 0; i < this.windowKernel.size(); i++) {
			double[] centerA = this.windowKernel.get(i).getCenter();
			radiusA = this.windowKernel.get(i).getRadius();
			for (int j = i + 1; j < this.windowKernel.size(); j++) {
				double dist = distance(centerA, this.windowKernel.get(j).getCenter());
				radiusB = this.windowKernel.get(j).getRadius();
				if (dist < minDistance) {
					minDistance = dist;
					closestA = i;
					closestB = j;
				}
			}

			assert (closestA != closestB);
			// heuristica
			if (minDistance <= (radiusA + radiusB)) {
//				System.out.println("acontece");
				this.windowKernel.get(closestA).add(this.windowKernel.get(closestB));
//				this.windowKernel.remove(closestB);
			}

		}
//		System.out.println(" " + this.windowKernel.size());

	}

	public void removeClusters() {

		for (int i = 0; i < this.windowKernel.size(); i++) {
			if (windowKernel.get(i).getTime() < (timestamp - 1000) && this.windowKernel.size() >500) {
				windowKernel.remove(i);
				i--;
			}
		}

	}

	public int classify(InstanceKernel inst, ArrayList<String> claNormal, long time) throws Exception {
//		an unknown instance arrive from the stream
		timestamp = time;

		int lineSize = inst.getCenter().length - 1;
		double[] data = new double[lineSize];

		for (int j = 0; j < lineSize; j++) {
			data[j] = inst.getCenter()[j];
		}

		Instance inst2 = new DenseInstance(1, data);

		InstanceKernel inKe = new InstanceKernel(inst2, inst2.numAttributes(), timestamp);

//		// get kOption neighbours 
		ArrayList<NearestNeighbours> neighbours = kClosestNeighbor(inKe, kOption.getValue());

		String[] info = testInstance(inKe, neighbours, inst.getCenter()[lineSize]);

/*		System.out.println(inst.getCenter()[lineSize]);
		System.out.println(", " + info[0]);
		System.out.println(", " + info[1]);
		System.out.println();*/

		if (timestamp % 1000 == 0) {

			for (int i = 0; i < this.windowKernel.size(); i++) {
/*				System.out.println((int) timestamp);
				System.out.println(", ");
				for (int j = 0; j < lineSize; j++) {
					System.out.println(this.windowKernel.get(i).getCenter()[j]);
					System.out.println(", ");
				}
				System.out.println(this.windowKernel.get(i).getRadius());
				System.out.println(", ");
				System.out.println(this.windowKernel.get(i).getLabel());
				System.out.println();*/
			}
		}

		if (info[0].equals(Double.toString(inst.getCenter()[lineSize]))) {
			return 1;
		}

		// if unknown SUSPEITO
		if (info[0].equals("")) {
			return 2;
		}

		return 0;
	}

	// ver se eh novidade ou extensao
	public void detectingThings(InstanceKernel in) throws Exception {

		int minClusters = 100;
		int minNovelty = 200;
		int sim = 0;

//		System.out.println("neWindow " + this.neWindow.size());
//		System.out.println("windowkernel " + this.windowKernel.size());

		// window is empty -> initial training phase

		if (this.neWindow.size() < minNovelty) {
			MicroCluster micro = new MicroCluster(in, "", "anormal", timestamp, ConceptCategory.NOVELTY);
			this.neWindow.add(micro);

		} else {
			if (!newInitialized) {

				ArrayList<MicroCluster> micros2 = clusteringBla.CluStreamOnline(this.neWindow, minClusters, timestamp);

				this.neWindow.clear();
				this.neWindow = micros2;
				newInitialized = true;

			} else {
//				setThreshold(-0.1);

				Instance den = new DenseInstance(1, in.getCenter());

				double minDistance = Double.MAX_VALUE;
				InstanceKernel closestKernel = null;

				for (int i = 0; i < this.neWindow.size(); i++) {

					double dis = distance(this.neWindow.get(i).getCenter(), in.getCenter());

					if (dis <= minDistance) {

						closestKernel = this.neWindow.get(i);
						minDistance = dis;
					}
				}

				// 2. Check whether instance fits into closestKernel
				double radius = 0.0;
				if (closestKernel.getWeight() == 1) {
					// Special case: estimate radius by determining the distance to the
					// next closest cluster
					radius = Double.MAX_VALUE;
					double[] center = closestKernel.getCenter();
					for (int i = 0; i < this.neWindow.size(); i++) {
						if (this.neWindow.get(i) == closestKernel) {
							continue;
						}

						double distance = distance(this.neWindow.get(i).getCenter(), center);
						radius = Math.min(distance, radius);
					}
				} else {
					radius = closestKernel.getRadius();
				}

				if (minDistance < radius) {
					// Date fits, put into kernel and be happy
					closestKernel.insert(den, timestamp);
					sim = 1;
//					return;
				}
				if (sim == 0) {

					// 3. Date does not fit, we need to free
					// some space to insert a new kernel
					long threshold = timestamp - 1000; // Kernels before this can be forgotten

					// 3.1 Try to forget old kernels
					for (int i = 0; i < this.neWindow.size(); i++) {
						if (this.neWindow.get(i).getRelevanceStamp() < threshold) {
							MicroCluster element = new MicroCluster(in, "", "anormal", timestamp, ConceptCategory.NOVELTY);
							this.neWindow.set(i, element);
							sim = 1;
//							 return;
						}
					}
				}

				if (sim == 0) {
					// eh pior assim
//					// 3.2 Merge closest two kernels
//					int closestA = 0;
//					int closestB = 0;
//					minDistance = Double.MAX_VALUE;
//					for (int i = 0; i < this.neWindow.size(); i++) {
//						double[] centerA = this.neWindow.get(i).getCenter();
//						for (int j = i + 1; j < this.neWindow.size(); j++) {
//							double dist = distance(centerA, this.neWindow.get(j).getCenter());
//							if (dist < minDistance) {
//								minDistance = dist;
//								closestA = i;
//								closestB = j;
//							}
//						}
//					}
//					assert (closestA != closestB);
//
//					this.neWindow.get(closestA).add(this.neWindow.get(closestB));
//					MicroCluster element = new MicroCluster(in, "", "anormal", timestamp);
//					this.neWindow.set(closestB, element);
				}

				for (int i = 0; i < this.neWindow.size(); i++) {
					double newClass = classes.size();

					if (this.neWindow.get(i).getN() >= 10) {

						minDistance = Double.MAX_VALUE;
						int indiceCluster = 0;
						for (int j = 0; j < this.windowKernel.size(); j++) {
							double dist = distance(this.windowKernel.get(j).getCenter(),
									this.neWindow.get(i).getCenter());
							if (dist < minDistance) {
								minDistance = dist;
								indiceCluster = j;
							}
						}
						if (minDistance <= (this.windowKernel.get(indiceCluster).getRadius() * threshold)) {
							newClass = Double.parseDouble(this.windowKernel.get(indiceCluster).getLabel());
							this.neWindow.get(i).setType("extension");
						} else {
							classes.add(Double.toString(newClass));
							this.neWindow.get(i).setType("novelty");
						}

						this.neWindow.get(i).setLabel(Double.toString(newClass));
						

						this.windowKernel.add(this.neWindow.get(i));


						final ClusterSummary targetClusterSummary = new PseudoPoint(
								new Sample(this.neWindow.get(i).getCenter()), this.neWindow.get(i).getRadius()/2);

						final List<Sample> targetSamples = this.neWindow.get(i).getInst()
								.stream()
								.map(Instance::toDoubleArray)
								.map(array -> {
									final double[] x = Arrays.copyOfRange(array, 0, array.length - 1);
									final Integer y = (int) array[array.length - 1];
									return new Sample(x, y);
								})
								.collect(Collectors.toList());

						final List<ClusterSummary> knownClustersSummaries = this.windowKernel
								.stream()
								.filter(microCluster -> microCluster.getType().equals("normal"))
								.map(microCluster -> new PseudoPoint(new Sample(microCluster.getCenter()), microCluster.getRadius()/2))
								.collect(Collectors.toList());

						final Set<Integer> knownLabels = this.windowKernel
								.stream()
								.filter(microCluster -> microCluster.getType().equals("normal"))
								.map(MicroCluster::getLabel)
								.map(Double::valueOf)
								.map(Double::intValue)
								.collect(Collectors.toSet());

						final ConceptCategory conceptCategory;
						if (this.neWindow.get(i).getType().equals("novelty")) {
							conceptCategory = ConceptCategory.NOVELTY;
						} else {
							conceptCategory = this.windowKernel.get(indiceCluster).getConceptCategory();
						}

						final ConceptClassificationContext context = new ConceptClassificationContext()
								.setTargetClusterSummary(targetClusterSummary)
								.setTargetSamples(targetSamples)
								.setKnownClusterSummaries(knownClustersSummaries)
								.setKnownLabels(knownLabels)
								.setDecision(conceptCategory);

						interceptor.NOVELTY_DETECTION_AL_FRAMEWORK.with(context).executeOrDefault(() -> {});

//						removeClusters();

						this.neWindow.remove(i);
					}

				}

			}

			if (this.neWindow.size() > 1000) {
				this.neWindow.remove(0);
			}
		}

	}

	public void setOutFile(String string) {
		this.out1 = string;
	}
	
	// get distance from k closest windowKernel
	public ArrayList<NearestNeighbours> kClosestNeighbor(InstanceKernel inst, int kOption) {

		MicroCluster closestKernel = null;

		NearestNeighbours nN = null;
		NearestNeighbours[] votes = new NearestNeighbours[kOption];

		double minDistance = Double.MAX_VALUE;
		Random rand = new Random();
		// first koption distances
//		System.out.println("0000000");
//		for(int i = 0; i < windowKernel.size(); i ++)
//			System.out.println(Arrays.toString(windowKernel.get(i).getCenter()));
//		System.out.println("0000000");
		for (int i = 0; i < votes.length; i++) {
			int n = rand.nextInt(this.windowKernel.size());
//			int n = i;
//			System.out.println(Arrays.toString(windowKernel.get(n).getCenter()));
			double dis = distance(inst.getCenter(), this.windowKernel.get(n).getCenter());
//			System.out.println("1 - " + dis);
			votes[i] = new NearestNeighbours(n, dis, this.windowKernel.get(n));
		}
		for (int i = 0; i < this.windowKernel.size(); i++) {
			double distance = distance(inst.getCenter(), this.windowKernel.get(i).getCenter());
//			System.out.println("indice " + i + " 2 - " + distance);
			int first = -1;
			double maxDistance = Double.MIN_VALUE;
			// biggest distance
			for (int j = 0; j < votes.length; j++) {
				if (votes[j].getDistance() > maxDistance) {
					maxDistance = votes[j].getDistance();
					first = j;
				}
			}

//			System.out.println("i from loop " + i);
			// replace with the biggest from the array votes
			if (distance < votes[first].getDistance()) {
//				System.out.println("i from if " + i);
				closestKernel = this.windowKernel.get(i);
				nN = new NearestNeighbours(i, distance, closestKernel);
				votes[first] = nN;
			}

		}

		ArrayList<NearestNeighbours> votesList;
		votesList = new ArrayList<>(Arrays.asList(votes));
//		System.out.println("$$$$$$$$$$$$$$$$$$$");
//		for(int i = 0; i < votesList.size(); i ++) {
//			System.out.println(Arrays.toString(votesList.get(i).getKernel().getCenter()));
//			System.out.println("list " + votesList.get(i).getIndex());
//			System.out.println("list " + votesList.get(i).getKernel().getLabel());
//		}

		return votesList;
	}

	// classification phase
	public String[] testInstance(InstanceKernel inst, ArrayList<NearestNeighbours> neighbours, double realLabel)
			throws Exception {

		// por voto, obtem-se a quantidade para cada label de acordo com a distancia.
		double[] votes = new double[classes.size()];
		String[] info = new String[2];
		info[0] = "";
		info[1] = "";
		double minDist = Double.MAX_VALUE;

		int index = 0;
		
//		System.out.println("window size " + windowKernel.size());

		// real classification
		for (int i = 0; i < neighbours.size(); i++) {

			double foo = Double.parseDouble(neighbours.get(i).getKernel().getLabel());
//			// if the dist is less or equal to the radius of the closest cluster
			votes[(int) foo]++;

			// minimal dist to the closets cluster
			double dist = distance(inst.getCenter(), neighbours.get(i).getKernel().getCenter());

			if (dist < minDist) {
				minDist = dist;
				index = neighbours.get(i).getIndex();
			}

		}

		// majority vote
		int valor = (kOption.getValue() / 2) + 1;
//		System.out.println(Arrays.toString(votes) + "---" + realLabel);
		threshold = this.windowKernel.get(index).getThreshold();
		double thre = this.windowKernel.get(index).getThreshold();
//		System.out.println(threshold);
		if (votes[max(votes)] >= valor) {
			// update closets cluster
			if (minDist <= (this.windowKernel.get(index).getRadius())) {
//				System.out.println("minDist " + minDist);
				
				// Double.toString(val); higher score
				info[0] = Double.toString(max(votes));
				info[1] = this.windowKernel.get(index).getType();

//			double result[] = new double[inst.getCenter().length];
//			Arrays.setAll(result, k -> (inst.getCenter()[k]));
				this.windowKernel.get(index).insert(inst.getCenter(), (int) timestamp);
//				System.out.println(Arrays.toString(votes) + "---" + realLabel);
				
			
			this.windowKernel.get(index).setThreshold(thre+0.00001);

				DriftEvolution element = new DriftEvolution(inst, index);
				this.driftList.add(element);
//			this.windowKernel.get(index).sum(inst.getCenter(), learningRate);
				return info;

//			this.threshold += 0.00001;
			} 
			else {
			
//			System.out.println(votes[max(votes)]);
			if(votes[max(votes)] >=  valor & minDist < this.windowKernel.get(index).getRadius()*thre ) {

				info[0] = Double.toString(max(votes));
				info[1] = this.windowKernel.get(index).getType();
				long threshold = timestamp - 1000; // Kernels before this can be forgotten
				
//				if(this.windowKernel.get(index).getRadius() == 0) {
//					double radius = minDist;
//					System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
//				}

////				 3.1 Try to forget old kernels
				if(this.windowKernel.size() >= 500) {
					for (int i = 0; i < this.windowKernel.size(); i++) {
						if (this.windowKernel.get(i).getRelevanceStamp() < threshold) {
//							this.windowKernel.remove(i);

//							System.out.println(radius);
							MicroCluster element = new MicroCluster(inst, info[0],
									"extension", timestamp, this.windowKernel.get(index).getConceptCategory());
							this.windowKernel.set(i, element);
//							this.windowKernel.get(i).setRadius(this.windowKernel.get(index).getRadius());
//							System.out.println("radius " + this.windowKernel.get(i).getRadius());
							return info;

						}
					}

				} else {
					double radius = minDist;
					MicroCluster element = new MicroCluster(inst, info[0], "extension", timestamp,
							this.windowKernel.get(index).getConceptCategory());
					this.windowKernel.add(element);
//					this.windowKernel.get(this.windowKernel.size()-1).setRadius(this.windowKernel.get(index).getRadius());
					return info;
				}
				
				System.out.println("acontece");

//				MicroCluster element = new MicroCluster(inst, info[0], "normal", timestamp);
//				this.windowKernel.add(element);
//				return info;
			}

				// System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
			}
		}

//		System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
		int count = 0;
//		System.out.println("before " + driftList.size());
		if (driftList.size() >= 100) {
			for (int i = 0; i < driftList.size(); i++) {
				for (int j = i + 1; j < this.driftList.size(); j++) {
//					System.out.println("i " +  this.driftList.get(i).getIdCluster() + " j " +  this.driftList.get(j).getIdCluster() );
					if (this.driftList.get(i).getIdCluster() == this.driftList.get(j).getIdCluster()) {
						Instance in = new DenseInstance(1, this.driftList.get(j).getInstaceKernel().getCenter());
						this.driftList.get(i).getInstaceKernel().insert(in, timestamp);
						this.driftList.remove(j);
//						j--;
					}
				}
			}

			for (int i = 0; i < driftList.size(); i++) {

				int ind = this.driftList.get(i).getIdCluster();
//				this.windowKernel.get(ind).sum(this.driftList.get(i).getInstaceKernel().LS, learningRate, this.driftList.get(i).getInstaceKernel().getN());
//				double thre = this.windowKernel.get(ind).getThreshold();
//				System.out.println("entrou " + thre);
//				this.windowKernel.get(ind).setThreshold(thre+0.00001);
			}
//				if(this.driftList.get(i) != null) {
//					System.out.println("id " + driftList.get(i).getIdCluster());
////					System.out.println("** " + Arrays.toString(this.driftList.get(i).getCenter()));
////					System.out.println("** " + this.driftList.get(i).getOldN());
//					System.out.println("** " + Arrays.toString(driftList.get(i).getInstaceKernel().getCenter()));
//					System.out.println("** " + driftList.get(i).getInstaceKernel().getN());
//					System.out.println("-- " + this.windowKernel.get(driftList.get(i).getIdCluster()).getN());
//					System.out.println("-- " + Arrays.toString(this.windowKernel.get(driftList.get(i).getIdCluster()).getCenter()));
////					System.out.println(Arrays.toString(this.driftList.get(i).getInstaceKernel().getCenter()));
////					System.out.println(this.driftList.get(i).getIdCluster());
//					count++;
//				}

//			System.out.println(count);
//			System.out.println("after " + this.driftList.size());
			this.driftList.clear();
		}

//		// if majority
//		if (votes[max(votes)] >= valor) {
//			
//		}

		if (info[0] == "") { // when is false.
//			// buffer
			detectingThings(inst);
//			System.out.println("window size " + windowKernel.size());
//			System.out.println("new size " + neWindow.size());
			this.windowKernel.get(index).setThreshold(this.windowKernel.get(index).getThreshold()- 0.00001);
			this.threshold -= 0.00001;
			info[0] = "-100";
			info[1] = "unknown";
		}

//		// test baseline
//		info[0] = Double.toString(max(votes));
//		info[1] = "normal";
//		System.out.println("threshold " + threshold);
		
//		System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
		return info;

	}

	public void setThreshold(double valor) {
		this.threshold = threshold + valor;
//		System.out.println("threshold " + threshold);
	}

	public double[] getVotesForInstance(InstanceKernel inst) {

		Instance in = new DenseInstance((Instance) inst);

		double v[] = new double[C + 1];
		try {
			NearestNeighbourSearch search;
			if (this.nearestNeighbourSearchOption.getChosenIndex() == 0) {
				search = new LinearNNSearch(this.window);
			} else {
				search = new KDTree();
				search.setInstances(this.window);
			}
			if (this.window.numInstances() > 0) {
				Instances neighbours = search.kNearestNeighbours(in,
						Math.min(kOption.getValue(), this.window.numInstances()));
				for (int i = 0; i < neighbours.numInstances(); i++) {
					v[(int) neighbours.instance(i).classValue()]++;
				}
			}
		} catch (Exception e) {
			return new double[classes.size()];
		}
		return v;
	}

	@Override
	public double[] getVotesForInstance(Instance inst) {

		double v[] = new double[C + 1];
		try {
			NearestNeighbourSearch search;
			if (this.nearestNeighbourSearchOption.getChosenIndex() == 0) {
				search = new LinearNNSearch(this.window);
			} else {
				search = new KDTree();
				search.setInstances(this.window);
			}
			if (this.window.numInstances() > 0) {
				Instances neighbours = search.kNearestNeighbours(inst,
						Math.min(kOption.getValue(), this.window.numInstances()));
				for (int i = 0; i < neighbours.numInstances(); i++) {
					v[(int) neighbours.instance(i).classValue()]++;
				}
			}
		} catch (Exception e) {
			return new double[inst.numClasses()];
		}
		return v;
	}

	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		return null;
	}

	@Override
	public void getModelDescription(StringBuilder out, int indent) {
	}

	public boolean isRandomizable() {
		return false;
	}

	// max value
	public int max(double[] L) {
		double max = L[0];
		int index = 0;
		for (int i = 0; i < L.length; i++) {
			if (L[i] > max) {
				max = L[i];
				index = i;
			}
		}
		return index;
	}

	// min value
	public double min(double[] L) {
		double min = L[0];
		for (int i = 0; i < L.length; i++)
			if (L[i] < min)
				min = L[i];
		return min;
	}

	private static double distance(double[] pointA, double[] pointB) {
		double distance = 0.0;
		for (int i = 0; i < pointA.length; i++) {
			double d = pointA[i] - pointB[i];
			distance += d * d;
		}
		return Math.sqrt(distance);
	}
}