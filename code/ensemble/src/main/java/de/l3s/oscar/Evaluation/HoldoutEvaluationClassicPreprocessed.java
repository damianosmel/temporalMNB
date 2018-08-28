package de.l3s.oscar.Evaluation;

import de.l3s.oscar.Preprocess.Preprocessor;
import de.l3s.oscar.Preprocess.ExtendedTweet;
import de.l3s.oscar.DB.RandomDatabaseConnectionPreprocessed;
import de.l3s.oscar.LearningAlgorithm.NaiveBayesMultinomialVanilla;
import jxl.Workbook;
import jxl.write.Number;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import moa.core.Example;
import moa.evaluation.WindowClassificationPerformanceEvaluator;
import com.github.javacliparser.IntOption;

import moa.streams.InstanceStream;
import weka.filters.unsupervised.attribute.StringToWordVector;

import java.util.ArrayList;

public class HoldoutEvaluationClassicPreprocessed {
	
	/*
	 * Holdout = Split dataset into two parts: 70% training and 30% test
	 */
    private final static int OUTPUT_FREQUENCY = 1000; //After how many instances is a result written into evaluator?
    //private final static int WIDTH = 5000;
	private static RandomDatabaseConnectionPreprocessed connection;

	/**
	 * Preprocessor to clean the Tweets
	 */

	private static Preprocessor preprocessor;

	// CLASSIFIER
	/**
	 * Naive Bayes Multinomial Model
	 */
	private static NaiveBayesMultinomialVanilla mnb = new NaiveBayesMultinomialVanilla();
	/**
	 * This is the Java code for a prequential evaluation
	 * Connects to Controller.java and fetches Tweets from database
	 */
	/**
	 * The trainingStream that is generated from the database
	 */
	static InstanceStream trainingStream;
	static ArrayList<ExtendedTweet> training;

	/**
	 * Filter that converts the instances into tf*idf Vector representation
	 */
	static StringToWordVector vector;

	// Variables for the time measurements
	double start1;
	
	public static void main(String[] args) throws Exception {
		preprocessor = new Preprocessor();
        WritableWorkbook workbook = Workbook.createWorkbook(new java.io.File("output-holdout-preprocessed.xls"));
        WritableSheet sheet = workbook.createSheet("First Sheet", 0);
        preprocessor = new Preprocessor();
        WindowClassificationPerformanceEvaluator evaluator = new WindowClassificationPerformanceEvaluator();
        WindowClassificationPerformanceEvaluator evaluator2 = new WindowClassificationPerformanceEvaluator();
        WindowClassificationPerformanceEvaluator evaluator3 = new WindowClassificationPerformanceEvaluator();
        evaluator2.widthOption = new IntOption("Width", 'w', "Window width", 100);
        evaluator3.widthOption = new IntOption("Width", 'w', "Window width", 5000);
        //evaluator.widthOption = new IntOption("Width", 'w', "Window width", WIDTH);
//		int numInstances = 10000;
//		Classifier learner = new NaiveBayesMultinomial();
		//Classifier learner = new HoeffdingTree();
		//RandomRBFGenerator stream = new RandomRBFGenerator();
//		stream.prepareForUse();
//		learner.setModelContext(stream.getHeader());
//		learner.prepareForUse();
		int instanceCount = 1600000;
		int firstPart = (int) (0.7 * instanceCount);
		//int secondPart = (int) 0.3 * 3000;
        String dataset = "tweets140"; //Sebastian: "ts_preprocessed";
        String usr = "root";
        String pass = "root"; //Sebastian: ""
		connection = new RandomDatabaseConnectionPreprocessed(dataset, usr, pass);
        int windowCount = OUTPUT_FREQUENCY;
		training = connection.getTrainingSet(); //.trainingSet;
		// transformation of the set into a stream for training
		trainingStream = new ExtendedBayesianStream(training);

		vector = ((ExtendedBayesianStream) trainingStream).getVector();
        System.out.println("After getting vector.");

		//Prepare classifier for learning
		mnb.prepareForUse();
		mnb.setModelContext(trainingStream.getHeader());
		//Count number of correctly classified tweets
		int numberSamplesCorrect = 0;
		int numberSamples = 0;
		int numInstances = 1000;
		int falsePositives = 0;
		int falseNegatives = 0;
		int truePositives = 0;
		int trueNegatives = 0;
        /*
        Last written row of excel file
        */
        int lastRow = 0;
		//Training
		while (trainingStream.hasMoreInstances() && numberSamples < firstPart) {
			Example<com.yahoo.labs.samoa.instances.Instance> trainInst = trainingStream.nextInstance();
			mnb.trainOnInstance(trainInst);
			numberSamples++;
		}
		System.out.println("Number Training samples: " + numberSamples);
		//Testing
		while(trainingStream.hasMoreInstances() && numberSamples < instanceCount )
		{
            windowCount--;
			Example<com.yahoo.labs.samoa.instances.Instance> trainInst = trainingStream.nextInstance(); //.nextInstance();
			numberSamples++;
			double[] votes = mnb.getVotesForInstance(trainInst);
            evaluator.addResult(trainInst, votes);
            evaluator2.addResult(trainInst, votes);
            evaluator3.addResult(trainInst,votes);
            if(windowCount == 0)
            {

                double firstAccuracy = evaluator.getFractionCorrectlyClassified();
                double firstKappa = evaluator.getKappaStatistic();
                double secondAccuracy = evaluator2.getFractionCorrectlyClassified();
                double secondKappa = evaluator2.getKappaStatistic();
                double thirdAccuracy = evaluator3.getFractionCorrectlyClassified();
                double thirdKappa = evaluator3.getKappaStatistic();
                Number firstKappaCell = new Number(0, lastRow, firstKappa);
                Number firstAccuracyCell = new Number(1, lastRow, firstAccuracy);
                Number secondKappaCell = new Number(2, lastRow, secondKappa);
                Number secondAccuracyCell = new Number(3, lastRow, secondAccuracy);
                Number thirdKappaCell = new Number(4, lastRow, thirdKappa);
                Number thirdAccuracyCell = new Number(5, lastRow, thirdAccuracy);
                sheet.addCell(firstKappaCell);
                sheet.addCell(firstAccuracyCell);
                sheet.addCell(secondAccuracyCell);
                sheet.addCell(secondKappaCell);
                sheet.addCell(thirdAccuracyCell);
                sheet.addCell(thirdKappaCell);
                windowCount = OUTPUT_FREQUENCY;
                lastRow++;
            }
			if(mnb.correctlyClassifies((com.yahoo.labs.samoa.instances.Instance) trainInst)){
				if(votes[0] >= votes[1])
					truePositives++;
				else	
					trueNegatives++;
				numberSamplesCorrect++;
			}
			else
			{
				if(votes[0] >= votes[1])
					falsePositives++;
				else	
					falseNegatives++;
			}
	}//close testing while-loop

        double accuracy = 100.0*(double) numberSamplesCorrect / (double) (numberSamples - firstPart);
        System.out.println((numberSamples-firstPart)+" instances processed with "+accuracy+"% accuracy");
		System.out.println("False positives: " + falsePositives);
		System.out.println("False negatives: " + falseNegatives);
		System.out.println("True positives: " + truePositives);
		System.out.println("True negatives: " + trueNegatives);
        double recall = 100.0 * (double)truePositives/((double)truePositives + (double)falseNegatives);
        System.out.println("Sensitivity/True Positive Rate: " + 100.0 * (double)truePositives/((double)truePositives + (double)falseNegatives));
        System.out.println("Specificity/True Negative Rate: " + 100.0 * (double)trueNegatives/((double)trueNegatives + (double)falsePositives));
        double precision = (100.0 * (double)truePositives) / ((double)truePositives + (double)falsePositives);
        double f1score = (200.0 * (double)truePositives) / (2.0* (double)truePositives + (double)falsePositives + (double)falseNegatives);
        double f1score2 = 200.0 * (precision * recall) / (precision + recall);
        System.out.println("Precision/Positive Predictive Value: " + precision);
        System.out.println("F1 Score: " + f1score);
        //System.out.println("F2 Score different calculation : " + f1score2);
        workbook.write();
        workbook.close();

	}

}
