package main.java.higia;

import java.util.ArrayList;

import br.com.douglas444.datastreamenv.common.ConceptCategory;
import br.com.douglas444.datastreamenv.common.InterceptionResult;
import com.github.javacliparser.FileOption;
import com.yahoo.labs.samoa.instances.Instance;

import moa.core.TimingUtils;
import moa.gui.visualization.DataPoint;
import moa.streams.clustering.FileStream;
import main.java.higia.utils.InstanceKernel;
import main.java.higia.utils.MicroCluster;

public class Run {

	int minWeight = 0;
	double threshold = 0;
	int minClusters = 0;
	int kNumber = 0;
	String defaultfile = "";
	String outFile = "";
	int numInstan = 0;

	public Run(int minWeight, int kNumber, double threshold, String defaultfile, int numInstan, int minClusters, String outFile) {

		this.minWeight = minWeight; // nao estou a usar

		this.kNumber = kNumber;
		this.threshold = threshold;
		this.minClusters = minClusters;

		this.defaultfile = defaultfile;
		this.outFile = outFile;
		this.numInstan = numInstan;

	}

	public void run( double rate) throws Exception {


		// datasets
		FileStream stream = new FileStream();
		stream.arffFileOption = new FileOption("arffFile", 'f', "ARFF file to load.", defaultfile, "arff", false);
		stream.prepareForUse();

		// classifier
		kNN_kem learner = new kNN_kem();
		// set number of k
		learner.kOption.setValue(kNumber);
		learner.setOutFile(outFile);
		learner.prepareForUse();
		learner.setModelContext(stream.getHeader());

		learner.setLearningRate(rate);

		int numberSamplesCorrect = 0;
		int numberSamples = 0;
		int testSamples = 0;

		long timestamp = -1;

		// values for evaluation
//		boolean preciseCPUTiming = TimingUtils.enablePreciseTiming();
		long evaluateStartTime = TimingUtils.getNanoCPUTimeOfCurrentThread();

		// true classification
//		ArrayList<ClassificationMeasures> measures = new ArrayList<>();

		ArrayList<String> classes = new ArrayList<>();

		// stream
		while (stream.hasMoreInstances()) {
			Instance inst = stream.nextInstance().getData();
			DataPoint dp = new DataPoint(inst, 1);

			String classValue = String.valueOf(inst.classValue());

			// array of classes
			int pos = classes.indexOf(classValue);
			if (pos == -1)
				classes.add(classValue);

			InstanceKernel inKe = new InstanceKernel(inst, inst.numAttributes(), timestamp);

			// train
			if (numberSamples <= numInstan) {

				MicroCluster in = new MicroCluster(inKe, classValue, "normal", timestamp, ConceptCategory.KNOWN);
				learner.setLimit_Option(numInstan);
				learner.trainOnInstance(in, minClusters, dp);

			} else {

				if (timestamp == -1) {
					System.out.println("...Testing the model...");
					
					stream.restart();
					stream.prepareForUse();
					
					numberSamples = 0;
					testSamples = 0;
					while (stream.hasMoreInstances()) {
						timestamp += 1;
						
						inst = stream.nextInstance().getData();
						// numInstances = 10;
						if (numberSamples <= numInstan) {
							testSamples++;
							if(learner.testEstupido(inst)) {
//								System.out.println(inst.classValue());
								numberSamplesCorrect++;
							}
							// time to merge closests clusters
							if (timestamp != 0 & timestamp % 1000 == 0) {
								System.out.println("numberSamplesCorrect " + numberSamplesCorrect);
								System.out.println("testSamples " + testSamples);
								double accuracy = 100.0 * (double) numberSamplesCorrect / (double) testSamples;
								double time = TimingUtils.nanoTimeToSeconds(
										TimingUtils.getNanoCPUTimeOfCurrentThread() - evaluateStartTime);
								System.out.println(numberSamples + " instances processed with " + accuracy
										+ "% accuracy in " + time + " seconds.");
								numberSamplesCorrect = 0;
								testSamples = 0;
							}
							
							numberSamples++;
							
						} else {
							break;
						}
						
					}
					numberSamplesCorrect = 0;
					timestamp=0;
					testSamples = 0;
					numberSamples = numInstan+1;
				}

				timestamp += 1;
				testSamples++;
				if(timestamp == 0) {
					System.out.println("...Real Testing...");
				}
				int answer = learner.classify(inKe, classes, timestamp);

				if (answer == 1) {
					numberSamplesCorrect++;
				}
//				if (answer == 2) {
//					testSamples--;
//				}

				// time to merge closests clusters
				if (timestamp!= 0 & timestamp % 1000 == 0) {
					double accuracy = 100.0 * (double) numberSamplesCorrect / (double) testSamples;
					double time = TimingUtils
							.nanoTimeToSeconds(TimingUtils.getNanoCPUTimeOfCurrentThread() - evaluateStartTime);
					System.out.println(numberSamples + " instances processed with " + accuracy + "% accuracy in " + time
							+ " seconds.");
////					learner.modelUpdate();
////					// remove os com menor peso, mas outro sistema tambem poderia ser utilizado
					numberSamplesCorrect = 0;
					testSamples = 0;
				}

				if(timestamp % 1000 == 0)
					learner.removeClusters();

				

			}

			numberSamples++;
		}

		double accuracy = 100.0 * (double) numberSamplesCorrect / (double) testSamples;
		double time = TimingUtils.nanoTimeToSeconds(TimingUtils.getNanoCPUTimeOfCurrentThread() - evaluateStartTime);
		System.out.println(
				numberSamples + " instances processed with " + accuracy + "% accuracy in " + time + " seconds.");
	}

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub

		int minW = 3;
		int k = 1;
		double threshold = 1.1;
		double rate = 0.0;

		Run run = new Run(
				minW,k,
				threshold,
				args[0],
				Integer.parseInt(args[1]),
				100,
				"/higiaOutput");

		System.out.println("baseline moa ");
		run.run(rate);

	}

}
