package de.l3s.oscar.Evaluation;

import de.l3s.oscar.DB.Connect2DBResult;
import de.l3s.oscar.DB.Connect2RandomDBResult;
import de.l3s.oscar.DB.DatabaseConnectionPreprocessed;
import de.l3s.oscar.DB.RandomDatabaseConnectionPreprocessed;
import de.l3s.oscar.LearningAlgorithm.*;
import de.l3s.oscar.Preprocess.ExtendedTweet;
import de.l3s.oscar.UserCommandLine;


import com.yahoo.labs.samoa.instances.Instance;

import moa.core.Example;
import moa.core.InstanceExample;
import moa.core.TimingUtils;
import moa.core.Utils;

import org.joda.time.DateTime;

import weka.filters.unsupervised.attribute.StringToWordVector;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.io.File;


/**
 * Created by damian on 17.01.17.
 * <p>
 * Starting Class for Evaluation
 */
public class Evaluation {
    /**
     * flag for running the code in the server
     */
    public boolean runningOnServer = false;
//    public boolean runningOnServer = true;


    //evaluator window size
    private final static int PERFORMANCE_SLIDING_WINDOW_WIDTH = 1000; //length of sliding window of evaluation
    private final static int PERFORMANCE_SLIDING_WINDOW_WIDTH_TESTING = 2; //length of sliding window of evaluation for testing
    private final static int ENSEMBLE_DIVERSITY_WINDOW_BATCH = 100; //length of batch size where kappa and average error rate is calculated

    //required arguments
    protected static String mode;
    protected static String collectionLocation;
    protected static String savedDBTitle;
    protected static String learningAlgorithm;
    protected static String evaluationScheme;
    protected static String rootOutputDirectory;
    protected static int numberOfBaseLearners;
    //optional arguments
    protected static boolean verbose;
    protected static boolean shortText;
    protected static boolean collectionLocationIsUrl;

    //basic accuracy values
    protected long falsePositives = 0;
    protected long falseNegatives = 0;
    protected long truePositives = 0;
    protected long trueNegatives = 0;
    protected long totalPositives = 0;
    protected long totalNegatives = 0;


    //debug
    protected static boolean debuggingPrequential = true;
    protected static boolean debuggingHoldout = false;
    protected static boolean debuggingStatisticsCalculation = false;
    protected String debugLog = "=== Evaluation ===" + "\n";


    /**
     * <h1>Evaluation constructor</h1>
     * Add the user input arguments as parameters for the evaluation
     *
     * @param parsedCommandLine
     * @author Damianos Melidis
     * @since Jan 2017
     */
    public Evaluation(UserCommandLine.ParsedCommandLine parsedCommandLine) {
        this.mode = parsedCommandLine.getRunMode();
        this.collectionLocation = parsedCommandLine.getCollectionLocation();
        this.savedDBTitle = parsedCommandLine.getSavedDBTitle();
        this.learningAlgorithm = parsedCommandLine.getLearningAlgorithm();
        this.evaluationScheme = parsedCommandLine.getEvaluationScheme();
        this.verbose = parsedCommandLine.getVerbose();
        this.shortText = parsedCommandLine.getShortText();
        this.collectionLocationIsUrl = parsedCommandLine.getCollectionLocationIsUrl();
        this.rootOutputDirectory = parsedCommandLine.getRootOutputDirectory();
        this.numberOfBaseLearners = parsedCommandLine.getNumberOfBaseLearners();
//        System.out.print("number of base learners: " + Integer.toString(this.numberOfBaseLearners));

//        System.out.println("After initializing evaluation class");
    }

    /**
     * Parse date to double format
     * TODO: add format option!
     *
     * @param date String of input date
     * @return epoch        the double equivalent of the date
     */
    public static double parseDateToDouble(String date) {
        double epoch = 1.0;
        try {
            /**
             * tweets 1.6 Mil
             * */
            epoch = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(date).getTime() / 1000;

            /**
             * Ephemeral entities - 2015
             * */
//			epoch = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US).parse(date).getTime() / 1000;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return epoch;
    }

    public static void setDebuggingPrequential(boolean userDebuggingPrequential) {
        debuggingPrequential = userDebuggingPrequential;
    }

    public static void setDebuggingHoldout(boolean userDebuggingHoldout) {
        debuggingHoldout = userDebuggingHoldout;
    }

    public static void setDebuggingStatisticsCalculation(boolean userDebuggingStatisticsCalculation) {
        debuggingStatisticsCalculation = userDebuggingStatisticsCalculation;
    }

    public String getDebugLog() {
        return debugLog;
    }

    /**
     * DEPRECATED
     * TODO: get better estimates of elapsed time, from https://github.com/sramirez/MOAReduction/blob/master/src/moa/reduction/test/ExperimentTest.java
     * <p>
     * Format elapsed time into days, hours, minutes and seconds
     *
     * @param elapsedTime measured elapsed time for a step
     * @return formatted elapsed time in days, hours, min and sec
     * @author Damianos Melidis
     * @since Jan 2017
     * @deprecated since March 2017
     */
    public String getPrettyTimeDifference(long elapsedTime) {

        return String.format("%d days, %d hours, %d min, %d sec",
                TimeUnit.DAYS.convert(elapsedTime, TimeUnit.NANOSECONDS),
                TimeUnit.HOURS.convert(elapsedTime, TimeUnit.NANOSECONDS),
                TimeUnit.MINUTES.convert(elapsedTime, TimeUnit.NANOSECONDS),
                TimeUnit.SECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS));
    }

    //TODO: Comment!
    public Connect2DBResult connect2DB(String savedDBTitle, String savedDBusername, String savedDBpassword, boolean verbose) {
        String stepLog = "~~~ Connecting to DB~~~\n";
        boolean isStepSuccessful = true;
        DatabaseConnectionPreprocessed DBconnection = null;
        stepLog += "Info: Connecting to " + savedDBTitle + " DB for sequential reading.\n";

        long startTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
        try {
            DBconnection = new DatabaseConnectionPreprocessed(savedDBTitle, savedDBusername, savedDBpassword);
        } catch (SQLException se) {
            stepLog += "Error: " + se.toString() + "\n";
            isStepSuccessful = false;
        } catch (IOException ioe) {
            stepLog += "Error: " + ioe.toString() + "\n";
            isStepSuccessful = false;
        } catch (ClassNotFoundException ce) {
            stepLog += "Error: " + ce.toString() + "\n";
            isStepSuccessful = false;
        }

        double elapsedTime = TimingUtils.nanoTimeToSeconds(TimingUtils.getNanoCPUTimeOfCurrentThread() - startTime);
        stepLog += "Info: Elapsed time= " + Double.toString(elapsedTime) + " seconds." + "\n";

        if (isStepSuccessful) {
            stepLog += "Info: Connecting to DB " + savedDBTitle + " - Done!\n";
        }

        stepLog += "~~~ End of connecting to DB ~~~\n";
        if (verbose) {
            System.out.println(stepLog);
        }
        return new Connect2DBResult(DBconnection, stepLog, isStepSuccessful);

    }

    //TODO:Comment!
    public Connect2RandomDBResult connect2RandomDB(String savedDBTitle, String savedDBusername, String savedDBpassword, boolean verbose) {
        String stepLog = "~~~ Connecting to DB ~~~\n";
        boolean isStepSuccessful = true;
        RandomDatabaseConnectionPreprocessed randomDBconnection = null;

        stepLog += "Info: Connecting to " + savedDBTitle + " DB for random access reading.\n";

        long startTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
        try {
            randomDBconnection = new RandomDatabaseConnectionPreprocessed(savedDBTitle, savedDBusername, savedDBpassword);
        } catch (SQLException se) {
            stepLog += "Error: " + se.toString() + "\n";
            isStepSuccessful = false;
        } catch (IOException ioe) {
            stepLog += "Error: " + ioe.toString() + "\n";
            isStepSuccessful = false;
        } catch (ClassNotFoundException ce) {
            stepLog += "Error: " + ce.toString() + "\n";
            isStepSuccessful = false;
        }

        double elapsedTime = TimingUtils.nanoTimeToSeconds(TimingUtils.getNanoCPUTimeOfCurrentThread() - startTime);
        stepLog += "Info: Elapsed time= " + Double.toString(elapsedTime) + " seconds." + "\n";

        if (isStepSuccessful) {
            stepLog += "Info: Connecting to DB " + savedDBTitle + " - Done!\n";
        }

        stepLog += "~~~ End of connecting to DB~~~\n";
        if (verbose) {
            System.out.println(stepLog);
        }

        return new Connect2RandomDBResult(randomDBconnection, stepLog, isStepSuccessful);
    }

    /**
     * <h1>Method to read data SEQUENTIALLY from saved (SQL) DB</h1>
     * <p>
     * Reading data from connection to data for prequential evaluation
     * Observation: this method can only fail by failing to connect to DB
     *
     * @param mode
     * @param savedDBTitle saved DB title
     * @param verbose      flag to have (true) verbose out of step log or not (false)
     * @return
     * @throws SQLException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public ReadDataFromDBResult readDataFromDB(String mode, String savedDBTitle, boolean verbose) {
        String stepLog = "~~~ Reading data from DB ~~~\n";
        String savedDBusername = "";
        String savedDBpassword = "";

        if (runningOnServer) {
            /**
             * For Deken
             * */
            savedDBusername = "pi";
            savedDBpassword = "3.14";
        } else {
            savedDBusername = "pi";
            savedDBpassword = "3.14";
        }
        String errorMessage;
        boolean isStepSuccessful = true;
        /**
         * The trainingStream that is generated from the database
         */
        BayesianStreamBig trainingStream = null; //BayesianStreamBig
        ArrayList<ExtendedTweet> training;
        /**
         * Filter that converts the instances into tf*idf Vector representation
         */
        StringToWordVector vector;

        stepLog += "Info: Reading data sequentially from DB.\n";
        Connect2DBResult connect2DBResult = connect2DB(savedDBTitle, savedDBusername, savedDBpassword, verbose);

        if (connect2DBResult.getSuccess()) {
            long startTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
            training = connect2DBResult.getDBconnection().getTrainingSet();
            // transformation of the set into a stream for training
            try {
                trainingStream = new BayesianStreamBig(training);
            } catch (Exception e) {
                stepLog += "Error: " + e.toString() + "\n";
                isStepSuccessful = false;
            }

            Double elapsedTime = TimingUtils.nanoTimeToSeconds(TimingUtils.getNanoCPUTimeOfCurrentThread() - startTime);
            stepLog += "Info: Elapsed time= " + Double.toString(elapsedTime) + " seconds." + "\n";
            } else //failing to connect to DB
        {
            errorMessage = connect2DBResult.getLog();
            stepLog += errorMessage;
            isStepSuccessful = false;
        }

        if (isStepSuccessful) {
            stepLog += "Info: Reading data sequentially from DB - Done!\n";
        }

        stepLog += "~~~ End of reading data from DB ~~~\n";

        if (verbose) {
            System.out.println(stepLog);
        }

        String connectAndReadDBStepsLog = connect2DBResult.getLog() + stepLog;
        return new ReadDataFromDBResult(trainingStream, connectAndReadDBStepsLog, isStepSuccessful);
    }

    //TODO:Comment!
    public ReadDataFromRandomDBResult readDataFromRandomDB(String mode, String savedDBTitle, boolean verbose) {

        String stepLog = "~~~ Reading data from DB ~~~\n";

        String savedDBusername = "";
        String savedDBpassword = "";
        if (runningOnServer) {
            /**
             * For Deken
             * */
            savedDBusername = "pi";
            savedDBpassword = "3.14";
        } else {
            savedDBusername = "pi";
            savedDBpassword = "3.14";
        }

        String errorMessage;
        boolean isStepSuccessful = true;

        /**
         * This is the Java code for a prequential evaluation
         * Connects to Controller.java and fetches Tweets from database
         */
        /**
         * The trainingStream that is generated from the database
         */
        ExtendedBayesianStream trainingStream = null; //InstanceStream
        ArrayList<ExtendedTweet> training;

        /**
         * Filter that converts the instances into tf*idf Vector representation
         */
        StringToWordVector vector;

        stepLog += "Info: Reading data randomly from DB.\n";
        Connect2RandomDBResult connect2RandomDBResult = connect2RandomDB(savedDBTitle, savedDBusername, savedDBpassword, verbose);

        if (connect2RandomDBResult.getSuccess()) {
            long startTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
            training = connect2RandomDBResult.getRandomDBconnection().getTrainingSet();
            // transformation of the set into a stream for training
            try {
                trainingStream = new ExtendedBayesianStream(training);
            } catch (Exception e) {
                stepLog += "Error: " + e.toString() + "\n";
                isStepSuccessful = false;
            }

            Double elapsedTime = TimingUtils.nanoTimeToSeconds(TimingUtils.getNanoCPUTimeOfCurrentThread() - startTime);
            stepLog += "Info: Elapsed time= " + Double.toString(elapsedTime) + " seconds." + "\n";
            //TODO: Do I need this vector??
            vector = ((ExtendedBayesianStream) trainingStream).getVector();
        } else {
            errorMessage = connect2RandomDBResult.getLog();
            stepLog += errorMessage;
            isStepSuccessful = false;
        }

        if (isStepSuccessful) {
            stepLog += "Info: Reading data randomly from DB - Done!\n";
        }

        stepLog += "~~~ End of reading data from DB ~~~\n";

        if (verbose) {
            System.out.println(stepLog);
        }

        String connectAndReadDBStepsLog = connect2RandomDBResult.getLog() + stepLog;
        return new ReadDataFromRandomDBResult(trainingStream, connectAndReadDBStepsLog, isStepSuccessful);

    }

    public File createOutputDirectory(String rootOutputDirectory) {
        File absolutePathOutputDirectory = new File(rootOutputDirectory, "tweets140"); //"spamData"//"emailData"//"ensembleWA" //updateEns17 //outputRunTest_arma5_ewma2_class140_word280_Inst //outputRunFading10K //outputRunADWIN10K //outputRunEnsembleAdapt12hr //"outputRunEnsemble" //outputRunFading //outputRunAdaptRate //outputRunDebug

        if (absolutePathOutputDirectory.mkdirs()) {
            return absolutePathOutputDirectory;
        } else {
            return null;
        }
    }

    /**
     * Function to initialize learning algorithms
     *
     * @param learningAlgorithm    String, user input for the learning algorithm to be used
     * @param numberOfBaseLearners String, number of base learners to be used
     * @return selectedClassifiers         ArrayList<SlidingWindowClassifier>, arraylist containing the selected number of classifiers and type of classifiers
     * @author Damianos Melidis
     * @since Feb 2017
     */

    public ArrayList<SlidingWindowClassifier> getSelectedClassifier(String learningAlgorithm, int numberOfBaseLearners) {
        ArrayList<SlidingWindowClassifier> selectedClassifiers = new ArrayList<SlidingWindowClassifier>();

        if (learningAlgorithm.equalsIgnoreCase("mnb")) {
            NaiveBayesMultinomialVanilla naiveBayesMultinomialVanilla = new NaiveBayesMultinomialVanilla();
            naiveBayesMultinomialVanilla.setDebugMNBVanilla(debuggingPrequential);
            selectedClassifiers.add(naiveBayesMultinomialVanilla);
        } else if (learningAlgorithm.equalsIgnoreCase("mnb4Sketch")) { //MNB that uses Sketch to filter out not frequent words over the stream
            NaiveBayesMultinomial4Sketch naiveBayesMultinomialVanilla4Sketch = new NaiveBayesMultinomial4Sketch();
            selectedClassifiers.add(naiveBayesMultinomialVanilla4Sketch);
        } else if (learningAlgorithm.equalsIgnoreCase("mnb4TimeSeries")) { //MNB that uses time series predictions
            NaiveBayesMultinomial4TimeSeries naiveBayesMultinomial4TimeSeries = new NaiveBayesMultinomial4TimeSeries();
            selectedClassifiers.add(naiveBayesMultinomial4TimeSeries);
        } else if (learningAlgorithm.equalsIgnoreCase("mnbFading")) { // MNB that fades feature count by time interval of their appearance
            double decayDegree = 0.1D; //0.1D; -> 10 hours //1.0D; -> 1 hour //2.0D; -> 0.5 hour
            String aggregationGranularity = "sec";//"sec"; //"hour";
            double instNumInPeriod = 100.0D;
            NaiveBayesMultinomialFading naiveBayesMultinomialFading = new NaiveBayesMultinomialFading(decayDegree, aggregationGranularity, instNumInPeriod);
            selectedClassifiers.add(naiveBayesMultinomialFading);
        } else if (learningAlgorithm.equalsIgnoreCase("mnbAggressive")) { //MNB that accumulates the faded counts
            double decayDegree = 0.1D; //0.1D; -> 10 hours //1.0D; -> 1 hour //2.0D; -> 0.5 hour
            String aggregationGranularity = "sec";//"sec"; //"hour";
            double instNumInPeriod = 50.0D;
            NaiveBayesMultinomialAggressive naiveBayesMultinomialAggressive = new NaiveBayesMultinomialAggressive(decayDegree, aggregationGranularity, instNumInPeriod);
            selectedClassifiers.add(naiveBayesMultinomialAggressive);
        } else if (numberOfBaseLearners != -1){ //ensemble
//            System.out.println("Ensemble of MNB");
            for (int i = 0; i < numberOfBaseLearners; i++) {
                /** Debugging prints
                 System.out.println("creating ensemble base learner " + Integer.toString(i));*/
                NaiveBayesMultinomialVanilla naiveBayesMultinomialVanilla = new NaiveBayesMultinomialVanilla();
                naiveBayesMultinomialVanilla.setDebugMNBVanilla(debuggingPrequential);

                selectedClassifiers.add(naiveBayesMultinomialVanilla);
            }
        } else {
            System.out.println("Error: Nor single or ensemble");
            return null;
        }
        return selectedClassifiers;
    }

    //TODO: read data from file or url
    public void readData() {
        System.out.println("read data from local path or url");
    }

    //TODO: save read data to DB (Sebastian preprocessor has some functions)
    public void saveData2DB() {
        System.out.println("save data to DB");
    }


    /**
     * Update buffer writer connected to the log txt with the debug messages from the current instance's run
     *
     * @param debugLogWholeInstanceRun String, debug log for the run of current instance
     * @param bufferedWriter           BufferedWriter, buffer for writing to the log txt
     * @param verbose                  boolean, flag to have (true) verbose out of step log or not (false)
     * @return stepLog                              String, step log of updating buffer with the statistic values of current evaluation window
     * @author Damianos Melidis
     * @since March 2017
     */
    public String updateDebugLogByWholeInstanceRun(String debugLogWholeInstanceRun, BufferedWriter bufferedWriter, boolean verbose) {
        boolean isDebugLogUpdateSuccessful = true;
        String stepLog = "Success";
        String txtAppender = "\n";

//        System.out.println("=== at buffer writer ===");
        try {
            /*System.out.println("---");
            System.out.println(debugLogWholeInstanceRun);
            System.out.println("---");*/
            bufferedWriter.write(txtAppender + debugLogWholeInstanceRun);

        } catch (IOException e) {
            if (verbose) {
                System.out.println("Error: writing debug log for the current instance.\n" + e.toString());
                stepLog = "Error: writing debug log to current instance.\n" + e.toString();
                isDebugLogUpdateSuccessful = false;
            }
        }
        /*System.out.println("===");
        System.out.println(bufferedWriter.toString());
        System.out.println("===");*/
        //now finish the row by adding a separating line
        try {
            bufferedWriter.write("=== ~ ===\n");
//            bufferedWriter.write("\n\n");
            bufferedWriter.flush();
        } catch (IOException e) {
            if (verbose) {
                System.out.println("Error: writing separating line to debug log.\n" + e.toString());
                stepLog = "Error: writing separating line to debug log.\n" + e.toString();
                isDebugLogUpdateSuccessful = false;
            }
        }
//        System.out.println("=== at buffer writer ===");


        return stepLog;
    }


    /**
     * Calculates the accuracy observed through out all the stream evaluation
     * @param totalCorrectSamples           long, number of total correctly classified samples
     * @param totalSamples                  long, number of total samples
     * @return double, accuracy through out all the stream
     *
     * @see <a href="https://en.wikipedia.org/wiki/Precision_and_recall"> Wiki for ML evaluation statistics</a>
     * @since Feb 2017
     * @author Damianos Melidis
     */

    /**
     * Averaging the probabilities of each class for the MNB base learners of the ensemble
     *
     * @param votesPerClassifier double[numberOfClassifiers][numberOfClasses], array with probabilities of each class for each classifier
     * @return averagedVotesPerClass        double[numberOfClasses], array with the average probability for each class
     * @author Damianos Melidis
     * @since Feb 2017
     */
    public double[] averagingVotesPerClass(double[][] votesPerClassifier) {

        int numberOfClassifiers = votesPerClassifier.length;

        int numberOfClasses = votesPerClassifier[0].length;
        double[] averagedVotesPerClass = new double[numberOfClasses];
        double sumOfVotesPerClass;

        for (int j = 0; j < numberOfClasses; j++) {
            sumOfVotesPerClass = 0.0;
            for (int i = 0; i < numberOfClassifiers; i++) {
                sumOfVotesPerClass += votesPerClassifier[i][j];
            }
            averagedVotesPerClass[j] = sumOfVotesPerClass / numberOfClassifiers;
        }

        return averagedVotesPerClass;
    }

    public ArrayList<Double> calculateKappaOfAllPairs(ArrayList<int[][]> contingencyTableOfAllPairs) {
        int numberOfPairs = contingencyTableOfAllPairs.size();
        double currentPair_theta1 = 0.0;
        double currentPair_theta2 = 0.0;
        double currentPair_kappa = 0.0;

        ArrayList<Double> kappaOfAllPairsBatch = null;

        for (int i = 0; i < numberOfPairs; i++) {
            currentPair_theta1 = calculateTheta1Measure(contingencyTableOfAllPairs.get(i));
            currentPair_theta2 = calculateTheta2Measure(contingencyTableOfAllPairs.get(i));
            currentPair_kappa = calculateKappaMeasure(currentPair_theta1, currentPair_theta2);
            kappaOfAllPairsBatch.add(currentPair_kappa);
        }

        return kappaOfAllPairsBatch;
    }

    /**
     * calculate kappa measure
     * shown in section 3.3 of "Pruning Adaptive Boosting" D. Margineantu, T. Dietterich, ICML '97
     */
    public double calculateKappaMeasure(double theta1Measure, double theta2Measure) {
        double kappaMeasure;
        if (theta2Measure == 1.0) {
            kappaMeasure = 0.0;
        } else {
            kappaMeasure = (theta1Measure - theta2Measure) / (1 - theta2Measure);
        }
        return kappaMeasure;
    }

    /**
     * calculate theta 1 value
     * shown in section 3.3 of "Pruning Adaptive Boosting" D. Margineantu, T. Dietterich, ICML '97
     */
    public double calculateTheta1Measure(int[][] contingencyTableOfAPair) {
        int numberOfClasses = contingencyTableOfAPair[0].length;
        int sumOfFirstDiagonal = 0;
        for (int i = 0; i < numberOfClasses; i++) {
            sumOfFirstDiagonal += contingencyTableOfAPair[i][i];
        }
        double theta1Measure = ((double) sumOfFirstDiagonal / ENSEMBLE_DIVERSITY_WINDOW_BATCH);
        assert (theta1Measure >= 0.0 && theta1Measure <= 1.0) : "Error: Theta 1 value is outside [0,1] range.";

        return theta1Measure;
    }

    /**
     * calculate theta 2 value
     * shown in section 3.3 of "Pruning Adaptive Boosting" D. Margineantu, T. Dietterich, ICML '97
     */
    public double calculateTheta2Measure(int[][] contingencyTableOfAPair) {
        int numberOfClasses = contingencyTableOfAPair[0].length;
        int sumOfRow_i = 0;
        int sumOfColumn_i = 0;
        int sumOfClasses = 0;
        for (int i = 0; i < numberOfClasses; i++) {
            sumOfRow_i = 0;
            sumOfColumn_i = 0;
            for (int j = 0; j < numberOfClasses; j++) {
                sumOfRow_i += contingencyTableOfAPair[i][j];
                sumOfColumn_i += contingencyTableOfAPair[j][i];
            }
            sumOfClasses += (sumOfRow_i * sumOfColumn_i);
        }

        double theta2Measure = ((double) sumOfClasses / (ENSEMBLE_DIVERSITY_WINDOW_BATCH * ENSEMBLE_DIVERSITY_WINDOW_BATCH));
        assert (theta2Measure >= 0.0 && theta2Measure <= 1.0) : "Error: Theta 2 value is outside [0,1] range.";

        return theta2Measure;
    }

    /**
     * Create contingency tables for each pair of base learners
     */
    public ArrayList<int[][]> createListOfContingencyTables(int numberOfBaseLearners, int numberOfClasses) {
        ArrayList<int[][]> contingencyTablesOfAllPairs = new ArrayList<int[][]>();
        /**
         * given N number of base learners to create a pair loop through the grid of (i,j) and accept a pair if i < j
         * */
        for (int i = 0; i < numberOfBaseLearners; i++) {
            for (int j = 0; j < numberOfBaseLearners; j++) {
                if (i < j) {
                    contingencyTablesOfAllPairs.add(new int[numberOfClasses][numberOfClasses]);
                }
            }
        }
        return contingencyTablesOfAllPairs;
    }


    /**
     * Function to create the header of the log for the evaluation step.
     *
     * @param stepLog        String, total log of running the evaluation
     * @param evaluationType String, "prequential or holdout"
     * @return updated stepLog
     */
    public String initializeStepLog(String stepLog, String evaluationType) {
        if (this.debuggingPrequential) {
            this.debugLog += "=== " + evaluationType + " Evaluation ===" + "\n";
        }
        stepLog = "~~~ Applying " + evaluationType + " evaluation ~~~\n"; //

        stepLog += "Info: Selected learning algorithm = " + learningAlgorithm + "\n";
        stepLog += "Info: Sliding window size for accuracy and kappa = " + Integer.toString(PERFORMANCE_SLIDING_WINDOW_WIDTH) + "\n";
//        stepLog += "Info: Sliding window size for accuracy and kappa = " + Integer.toString(PERFORMANCE_SLIDING_WINDOW_WIDTH_TESTING) + "\n";
        stepLog += "Info: Ensemble diversity measures are calculated for landmark window with size = " + Integer.toString(ENSEMBLE_DIVERSITY_WINDOW_BATCH) + "\n";
        stepLog += "Info: Starting please wait.. \n";

        return stepLog;
    }

    /**
     * Function to initialize the ensemble learning
     *
     * @param numberOfSelectedClassifiers int, number of base learners
     * @param selectedClassifiers         ArrayList<SlidingWindowClassifier>, list of sliding window MNBs
     * @param trainingStream              BayesianStreamBig, all tweets in a weka dataset
     */
    public void prepareEnsembleForUse(int numberOfSelectedClassifiers, ArrayList<SlidingWindowClassifier> selectedClassifiers, BayesianStreamBig trainingStream) {
        for (int i = 0; i < numberOfSelectedClassifiers; i++) {
            //prepare classifier for learning
            selectedClassifiers.get(i).prepareForUse();
            /** ---- weka instances ----
             selectedClassifiers.get(i).setModelContext(trainingStream.getHeader());
             selectedClassifiers.get(i).setHeaderInfo(trainingStream.trainingSet);
             */
            selectedClassifiers.get(i).setModelContext(trainingStream.getHeader());
            selectedClassifiers.get(i).setHeaderInfo(trainingStream.trainingSetSamoa);
            /** --- Samoa instances ---
             selectedClassifiers.get(i).setModelContext(trainingStream.getHeader());
             selectedClassifiers.get(i).setHeaderInfo(trainingStream.trainingSetSamoa);
             */
        }

    }

    /**
     * Function to initialize single learnng model
     *
     * @param selectedClassifiers ArrayList<SlidingWindowClassifier>, list of only one element (single learning model)
     * @param trainingStream      BayesianStreamBig, all tweets in a weka dataset
     * @param outputDirectory     File, file path of output directory
     */
    public void prepareSingleModelForUse(ArrayList<SlidingWindowClassifier> selectedClassifiers, BayesianStreamBig trainingStream, File outputDirectory) {
        selectedClassifiers.get(0).prepareForUse();
        /** ---- weka instances ----
         selectedClassifiers.get(0).setModelContext(trainingStream.getHeader());
         selectedClassifiers.get(0).setHeaderInfo(trainingStream.trainingSet);
         */
        selectedClassifiers.get(0).setModelContext(trainingStream.getHeader());
        selectedClassifiers.get(0).setHeaderInfo(trainingStream.trainingSetSamoa);

        /**
         * for reporting word predictions
         * */
        selectedClassifiers.get(0).setOutputDirectory(outputDirectory); // outputDirectory = outputDirectory;
    }

    /**
     * Function to initialize the sliding windows.
     * One base learner is assigned to each sliding window.
     *
     * @param numberOfSelectedClassifiers int, number of sliding windows (ensemble)
     * @param ensembleDebug               boolean, to debug the sliding window ensemble (true) or not (false)
     * @return ensembleSlidingWindowOfInstances     SlidingWindowOfInstances, created instance for sliding windows
     */
    public SlidingWindowOfInstances initializeSlidingWindows(int numberOfSelectedClassifiers, SamoaInstanceWithTrainingIndex currentSamoaInstanceWithTrainingIndex, boolean ensembleDebug) {
        int numberOfSlidingWindows = numberOfSelectedClassifiers;
        /**
         * 1.0, 12.0, 3.0
         * hours, hours, days
         * */
        /*ArrayList<Double> slidingWindowTimePeriods = new ArrayList<Double>(Arrays.asList(20.0, 1.0));
        ArrayList<String> slidingWindowTimePeriodsDescription = new ArrayList<String>(Arrays.asList("seconds", "days"));*/

        /**
         * for debugging
         * */
        /*ArrayList<Double> slidingWindowTimePeriods = new ArrayList<Double>(Arrays.asList(20.0, 30.0));
        ArrayList<String> slidingWindowTimePeriodsDescription = new ArrayList<String>(Arrays.asList("seconds", "seconds"));*/
        /*ArrayList<Double> slidingWindowTimePeriods = new ArrayList<Double>(Arrays.asList(2.0, 5.0, 7.0, 9.0));
        ArrayList<String> slidingWindowTimePeriodsDescription = new ArrayList<String>(Arrays.asList("hours", "hours", "hours", "hours"));*/
        /**
         * for actual experiments
         **/
        /*ArrayList<Double> slidingWindowTimePeriods = new ArrayList<Double>(Arrays.asList(1.0, 1.0, 3.0));
        ArrayList<String> slidingWindowTimePeriodsDescription = new ArrayList<String>(Arrays.asList("days","weeks","weeks"));*/

        /**
         * long ensemble with short, medium and long history - ensemble 9 (Jun '17)
         * */
        /*ArrayList<Double> slidingWindowTimePeriods = new ArrayList<Double>(Arrays.asList(3.0, 5.0, 12.0, 1.0, 2.0, 3.0, 5.0,7.0,14.0)); // 12.0,24.0 // 12,1,1,1 //2.0,9.0 // 3.0, 7.0 //12.0 hrs, 2.0 days, 1.0 week, 1.0 month
        ArrayList<String> slidingWindowTimePeriodsDescription = new ArrayList<String>(Arrays.asList("hours", "hours","hours","days","days","days","days","days","days")); //"hours","days","weeks","months"*/
        /*ArrayList<Double> slidingWindowTimePeriods = new ArrayList<Double>(Arrays.asList(2.0, 7.0, 11.0, 13.0, 19.0, 1.0, 3.0, 5.0, 1.0, 2.0));
        ArrayList<String> slidingWindowTimePeriodsDescription = new ArrayList<String>(Arrays.asList("hours", "hours", "hours", "hours", "hours", "days", "days", "days", "weeks", "weeks"));*/

        /**
         * long ensemble with short, medium and long history - ensemble 8 (July '17)
         *
         * */
        /*ArrayList<Double> slidingWindowTimePeriods = new ArrayList<Double>(Arrays.asList(2.0,12.0,24.0,2.0,5.0,7.0,10.0,14.0));
        ArrayList<String> slidingWindowTimePeriodsDescription = new ArrayList<String>(Arrays.asList("hours", "hours", "hours", "days", "days", "days", "days","days"));
        */
        /**
         * ensemble 5
         * */
        /*ArrayList<Double> slidingWindowTimePeriods = new ArrayList<Double>(Arrays.asList(2.0,12.0,24.0,2.0,3.0));
        ArrayList<String> slidingWindowTimePeriodsDescription = new ArrayList<String>(Arrays.asList("hours", "hours", "hours", "days", "days"));*/

        /**
         * ensemble 2
         * */
        ArrayList<Double> slidingWindowTimePeriods = new ArrayList<Double>(Arrays.asList(2.0, 12.0));
        ArrayList<String> slidingWindowTimePeriodsDescription = new ArrayList<String>(Arrays.asList("hours", "hours"));

        /**
         * ensemble 3
         * */
        /*ArrayList<Double> slidingWindowTimePeriods = new ArrayList<Double>(Arrays.asList(2.0,12.0,24.0));
        ArrayList<String> slidingWindowTimePeriodsDescription = new ArrayList<String>(Arrays.asList("hours", "hours", "hours"));
*/
        SlidingWindowOfInstances ensembleSlidingWindowOfInstances = new SlidingWindowOfInstances(numberOfSlidingWindows, slidingWindowTimePeriods, slidingWindowTimePeriodsDescription, currentSamoaInstanceWithTrainingIndex); //

        ensembleSlidingWindowOfInstances.setDebuggingSlidingWindow(ensembleDebug);
        /**
         * debugPrint
         * */
//        ensembleSlidingWindowOfInstances.setDebuggingSlidingWindow(false);
        return ensembleSlidingWindowOfInstances;
    }

    /**
     * Function to add instances to the sliding windows
     *
     * @param currentSamoaInstanceWithTrainingIndex SamoaInstanceWithTrainingIndex, instance to be added to the sliding windows
     * @param ensembleSlidingWindowOfInstances      SlidingWindowOfInstances, sliding windows
     * @param ensembleDebug                         boolean, to debug the sliding window ensemble (true) or not (false)
     */
    public void addInstanceToSlidingWindows(SamoaInstanceWithTrainingIndex currentSamoaInstanceWithTrainingIndex, SlidingWindowOfInstances ensembleSlidingWindowOfInstances, boolean ensembleDebug) {
//        long startTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
        ensembleSlidingWindowOfInstances.add(currentSamoaInstanceWithTrainingIndex); //trainingInstance

        if (ensembleDebug) {
            this.debugLog += "\n" + ensembleSlidingWindowOfInstances.getThenCleanDebugLog() + "\n";
        }
    }

    public ArrayList<String> updateSlidingWindowsCounts(int numberOfSelectedClassifiers, SlidingWindowOfInstances ensembleSlidingWindowOfInstances) {
        ArrayList<String> currentEnsembleSlidingWindowStatistics = new ArrayList<String>();
        int isCurrentWindowSlided = -1;
        for (int i = 0; i < numberOfSelectedClassifiers; i++) {
            currentEnsembleSlidingWindowStatistics.add(Long.toString(ensembleSlidingWindowOfInstances.getSlidingWindowNumberOfInstances(i)));
            isCurrentWindowSlided = (ensembleSlidingWindowOfInstances.getIsWindowSlided(i)) ? 1 : 0;
            currentEnsembleSlidingWindowStatistics.add(Integer.toString(isCurrentWindowSlided));
        }
//        System.out.println("The updated windows are: " + currentEnsembleSlidingWindowStatistics.toString());
        return currentEnsembleSlidingWindowStatistics;
    }

    /**
     * Function to test instance to the used model
     *
     * @param numberOfSelectedClassifiers int, number of classifiers of the used model
     * @param trainingInstance            samoaInstance, the instance to be tested
     * @param selectedClassifiers         ArrayList<SlidingWindowClassifier>, the actual classifiers composing the model
     * @return
     */
    public double[][] testInstance(int numberOfSelectedClassifiers, Instance trainingInstance, ArrayList<SlidingWindowClassifier> selectedClassifiers) { //double[][] votesPerClassifier

        double[][] votesPerClassifier = new double[numberOfSelectedClassifiers][trainingInstance.numClasses()];
//        System.out.println("=== Test === ");
        for (int i = 0; i < numberOfSelectedClassifiers; i++) {
            votesPerClassifier[i] = selectedClassifiers.get(i).getVotesForInstance(trainingInstance);

//            System.out.println("Classifier " + i + " votes : " + Arrays.toString(votesPerClassifier[i]));
            if (debuggingPrequential) {//debuggingPrequential

                this.debugLog += "\n" + "For base learner " + Integer.toString(i) + "\n";
                this.debugLog += selectedClassifiers.get(i).getThenCleanDebugLog() + "\n";
            }
        }
//        System.out.println("=== Test ===");
        return votesPerClassifier;
    }

    /***
     * Function to test instance to the used model using time series prediction
     * @param numberOfSelectedClassifiers       int, number of classifiers of the used model
     * @param trainingInstance                  samoaInstance, the instance to be tested
     * @param selectedClassifiers               ArrayList<SlidingWindowClassifier>, the actual classifiers composing the model
     * @param predictorName                     String, the name of the predictor of the time series
     * @param predictionTimeRangeStart          DateTime, start of time range for the predictions
     * @param predictionTimeRangeStop           Datetime, stop of time range for the predictions
     * @param windowTimeSize                    int, size of the window of prediction
     * @param windowTimeName                    String, name of the window "day" or "week" or ..
     * @return
     */
    public double[][] testInstance(int numberOfSelectedClassifiers, Instance trainingInstance, ArrayList<SlidingWindowClassifier> selectedClassifiers, String predictorName, DateTime predictionTimeRangeStart, DateTime predictionTimeRangeStop, int windowTimeSize, String windowTimeName) {
        double[][] votesPerClassifier = new double[numberOfSelectedClassifiers][trainingInstance.numClasses()];

        for (int i = 0; i < numberOfSelectedClassifiers; i++) {
            votesPerClassifier[i] = selectedClassifiers.get(i).getVotesForInstanceWithWordTimeSeries(trainingInstance, predictorName, predictionTimeRangeStart, predictionTimeRangeStop, windowTimeSize, windowTimeName);

            if (debuggingPrequential) {
                this.debugLog += "\n" + "For base learner " + Integer.toString(i) + "\n";
                this.debugLog += selectedClassifiers.get(i).getThenCleanDebugLog() + "\n";
            }
        }
        return votesPerClassifier;
    }

    /**
     * @param ensembleSlidingWindowOfInstances SlidingWindowOfInstances, sliding windows containing the instances for each base learner
     * @param selectedClassifiers              ArrayList<SlidingWindowClassifier>, the selected base learners
     * @param slidingWindowIndex
     * @return
     */
    public int deleteInstancesFromBaseLearner(SlidingWindowOfInstances ensembleSlidingWindowOfInstances, ArrayList<SlidingWindowClassifier> selectedClassifiers, int slidingWindowIndex) {

        NavigableSet<DateTime> keysHigherOrEqualThanFirstInstance = null;
        DateTime previousSlidingWindowFirstInstanceDateTime = ensembleSlidingWindowOfInstances.getPreviousSlidingWindowFirstLastInstanceDateTimes(slidingWindowIndex).get(0);
        DateTime currentSlidingWindowFirstInstanceDateTime = ensembleSlidingWindowOfInstances.getCurrentSlidingWindowFirstLastInstanceDateTimes(slidingWindowIndex).get(0);

        int countOfDeletingInstanceFromMNB = 0;
        ArrayList<Instance> instances2RemoveCurrentTime = new ArrayList<Instance>();
        Iterator<Instance> instances2RemoveIteratorCurrentTime;
        keysHigherOrEqualThanFirstInstance = ensembleSlidingWindowOfInstances.getKeysHigherOrEqualThanInstance(previousSlidingWindowFirstInstanceDateTime);

        /**
         * Debugging prints
         * System.out.println("=== Deleting ===");
         System.out.println("first instance not to be removed: " + currentSlidingWindowFirstInstanceDateTime.toString());*/
        for (DateTime instanceToDeleteDateTime : keysHigherOrEqualThanFirstInstance) {
            /** Debugging prints
             System.out.println(" time of candidate to remove: " + instanceToDeleteDateTime.toString());
             */
            if (instanceToDeleteDateTime.isBefore(currentSlidingWindowFirstInstanceDateTime) && !instanceToDeleteDateTime.isEqual(currentSlidingWindowFirstInstanceDateTime)) {

                instances2RemoveCurrentTime = ensembleSlidingWindowOfInstances.getSamoaInstancesInTimePoint(instanceToDeleteDateTime);
                instances2RemoveIteratorCurrentTime = instances2RemoveCurrentTime.iterator();
                while (instances2RemoveIteratorCurrentTime.hasNext()) {
                    /** Debugging prints
                     System.out.println("removing candidate..");*/
                    selectedClassifiers.get(slidingWindowIndex).discardInstanceFromModel(instances2RemoveIteratorCurrentTime.next());
                    countOfDeletingInstanceFromMNB++;
                }
            } else {
                break;
            }
        }
        /** Debugging prints
         System.out.println("=== Deleting ===");*/

        /**
         * Uncomment after debugging ensemble
         * */
        if (this.debuggingPrequential) {
            StringBuilder modelDescriptionOut = new StringBuilder();
//            selectedClassifiers.get(slidingWindowIndex).getModelDescription(modelDescriptionOut, '\t');
            System.out.println("=== Slided MNB " + Integer.toString(slidingWindowIndex) + " ===");
//            System.out.println(modelDescriptionOut.toString());
            System.out.println("=== Slided MNB ===");
        }

        return countOfDeletingInstanceFromMNB;
    }

    /**
     * Train base learner ()
     *
     * @param selectedClassifiers ArrayList<SlidingWindowClassifier>, list of sliding window classifiers
     * @param trainingInstance    SamoaInstance, current training instance
     * @param slidingWindowIndex  int, index of the sliding window
     */
    public void trainBaseLearnerOnInstance(ArrayList<SlidingWindowClassifier> selectedClassifiers, Instance trainingInstance, int slidingWindowIndex) {

        selectedClassifiers.get(slidingWindowIndex).trainOnInstance(trainingInstance);

        if (debuggingPrequential) {
            this.debugLog += "\n" + "For base learner " + Integer.toString(slidingWindowIndex) + "\n";
            this.debugLog += "\n" + selectedClassifiers.get(slidingWindowIndex).getThenCleanDebugLog();
        }
    }

    /**
     * Remove instances from ensemble of sliding windows
     *
     * @param ensembleSlidingWindowOfInstances  SlidingWindowOfInstances, the class containing the data for sliding window classifiers
     * @param indexOfLongestTimePeriodOfWindows int, index of window with longest time period
     * @return number of instances removed from the structure storing all instances of the ensemble
     */
    public long removeInstancesFromSlidingWindows(SlidingWindowOfInstances ensembleSlidingWindowOfInstances, int indexOfLongestTimePeriodOfWindows) {
        return ensembleSlidingWindowOfInstances.removeInstancesFromSlidingWindows(indexOfLongestTimePeriodOfWindows);
    }

    /**
     * Sum up the number of all deleted instances from all base learners
     *
     * @param countOfDeletingInstanceFromMNB
     * @return sumCountsOfDeletingInstanceFromMNB           int, the total number of deleted instances for all base learners of the ensemble
     */
    public long sumNumberOfDeletedInstancesFromBaseLearners(long[] countOfDeletingInstanceFromMNB) {
        long sumCountsOfDeletingInstanceFromMNB = 0;
        for (int i = 0; i < countOfDeletingInstanceFromMNB.length; i++) {
            sumCountsOfDeletingInstanceFromMNB += countOfDeletingInstanceFromMNB[i];
        }
        return sumCountsOfDeletingInstanceFromMNB;
    }

    /**
     * Print average time per number of operation of each sub-task of evaluation
     *
     * @param numberOfSelectedClassifiers
     * @param totalNumberTrainingInstances
     * @param sumCountsOfDeletingInstanceFromMNB
     * @param countOfRemovingInstancesFromWindows
     * @param countOfDeletingInstanceFromMNB
     * @param totalElapsedTimes
     * @param sumElapsedTimeForInitializingWindows
     * @param sumElapsedTimeForAddingInstanceToWindow
     * @param sumElapsedTimeForTesting
     * @param sumElapsedTimeForDeletingInstanceFromMNB
     * @param sumElapsedTimeForLearning
     * @param sumElapsedTimeForRemovingInstancesFromWindows
     */
    public void printAverageTimePerOperation(int numberOfSelectedClassifiers, long totalNumberTrainingInstances, long sumCountsOfDeletingInstanceFromMNB, long countOfRemovingInstancesFromWindows, long[] countOfDeletingInstanceFromMNB, double totalElapsedTimes, double sumElapsedTimeForInitializingWindows, double sumElapsedTimeForAddingInstanceToWindow, double sumElapsedTimeForTesting, double sumElapsedTimeForDeletingInstanceFromMNB, double sumElapsedTimeForLearning, double sumElapsedTimeForRemovingInstancesFromWindows) {

        double averageTotalLearningTime = totalElapsedTimes / totalNumberTrainingInstances;

        System.out.println("=== Time ===");
        System.out.println("Total time of testing and learning: " + Double.toString(totalElapsedTimes) + " seconds.");
        System.out.println("Time for sliding windows initialization: " + Double.toString(sumElapsedTimeForInitializingWindows) + " seconds.");
        System.out.println("Average time for adding instance to windows: " + Double.toString(sumElapsedTimeForAddingInstanceToWindow / totalNumberTrainingInstances) + " seconds.");
        System.out.println("Average time for testing instance: " + Double.toString(sumElapsedTimeForTesting / totalNumberTrainingInstances) + " seconds.");
        if (sumCountsOfDeletingInstanceFromMNB >= 1) {
            System.out.println("Average time for deleting instances from MNB: " + Double.toString(sumElapsedTimeForDeletingInstanceFromMNB / sumCountsOfDeletingInstanceFromMNB) + " seconds.");
        } else {
            System.out.println("No operations for deleting instances from MNB ");
        }
        System.out.println("Average time for learning from instance: " + Double.toString(sumElapsedTimeForLearning / totalNumberTrainingInstances) + " seconds.");
        if (countOfRemovingInstancesFromWindows > 0) {
            System.out.println("Average time for removing instances from windows: " + Double.toString(sumElapsedTimeForRemovingInstancesFromWindows / countOfRemovingInstancesFromWindows) + " seconds.");
        } else {
            System.out.println("No operations for removing instances from windows ");
        }
        System.out.println("Total average total learning time= " + Double.toString(averageTotalLearningTime) + " seconds.");
        System.out.println("Total number of deleted instances from MNB models: "); //1st window:" + Long.toString(countOfDeletingInstanceFromMNB[0]) + " 2nd window: " + Integer.toString(countOfDeletingInstanceFromMNB[1])
        for (int i = 0; i < numberOfSelectedClassifiers; i++) {
            System.out.println("For window " + Integer.toString(i) + ": " + Long.toString(countOfDeletingInstanceFromMNB[i]));
        }
        System.out.println("Total number of removed instances from windows: " + Long.toString(countOfRemovingInstancesFromWindows));
        System.out.println("=== Time ===");

    }

    public String updateStepLogWithTime(long totalNumberTrainingInstances, int numberOfSelectedClassifiers, long[] countOfDeletingInstanceFromMNB, long sumCountsOfDeletingInstanceFromMNB, long countOfRemovingInstancesFromWindows, double totalElapsedTimes, double sumElapsedTimeForInitializingWindows, double sumElapsedTimeForAddingInstanceToWindow, double sumElapsedTimeForTesting, double sumElapsedTimeForDeletingInstanceFromMNB, double sumElapsedTimeForLearning, double sumElapsedTimeForRemovingInstancesFromWindows, boolean ensembleDebug) {

        double averageTotalLearningTime = totalElapsedTimes / totalNumberTrainingInstances;

        String stepLog = "=== Time ===" + "\n";
        stepLog += "Info: Total time of testing and learning: " + Double.toString(totalElapsedTimes) + "seconds." + "\n";
        stepLog += "Info: Total average total learning time (per all instances)= " + Double.toString(averageTotalLearningTime) + " seconds." + "\n"; //getPrettyTimeDifference(averageTotalLearningTime) + "\n";


        if (numberOfSelectedClassifiers > 1 || ensembleDebug) {
            /**
             * if ensemble is selected
             * */
            stepLog += "Time for sliding windows initialization: " + Double.toString(sumElapsedTimeForInitializingWindows) + " seconds." + "\n";
            stepLog += "Average time for adding instance to windows: " + Double.toString(sumElapsedTimeForAddingInstanceToWindow / (totalNumberTrainingInstances - 1)) + " seconds." + "\n";

            stepLog += "Average time for testing instance: " + Double.toString(sumElapsedTimeForTesting / totalNumberTrainingInstances) + "seconds." + "\n";

            if (sumCountsOfDeletingInstanceFromMNB >= 1) {
                stepLog += "Average time for deleting instances from MNB: " + Double.toString(sumElapsedTimeForDeletingInstanceFromMNB / sumCountsOfDeletingInstanceFromMNB) + " seconds." + "\n";
            } else {
                stepLog += "No operations for deleting instances from MNB." + "\n";
            }

            stepLog += "Average time for learning from instance: " + Double.toString(sumElapsedTimeForLearning / totalNumberTrainingInstances) + " seconds." + "\n";

            if (countOfRemovingInstancesFromWindows > 0) {
                stepLog += "Average time for removing instances from windows: " + Double.toString(sumElapsedTimeForRemovingInstancesFromWindows / countOfRemovingInstancesFromWindows) + " seconds." + "\n";
            } else {
                stepLog += "No operations for removing instances from windows." + "\n";
            }

            stepLog += "Total number of operations to delete instances from MNB models: " + "\n";
            for (int i = 0; i < numberOfSelectedClassifiers; i++) {
                stepLog += "For window " + Integer.toString(i) + ": " + Long.toString(countOfDeletingInstanceFromMNB[i]) + "\n";
            }
            stepLog += "Total number of operations to remove instances from windows: " + Long.toString(countOfRemovingInstancesFromWindows) + "\n";
        } else {
            /**
             * //if single classifier is selected
             * */
            stepLog += "Average time for testing instance: " + Double.toString(sumElapsedTimeForTesting / totalNumberTrainingInstances) + " seconds." + "\n";
            stepLog += "Average time for learning from instance: " + Double.toString(sumElapsedTimeForLearning / totalNumberTrainingInstances) + " seconds." + "\n";
        }

        stepLog += "=== Time ===" + "\n";

        return stepLog;
    }


    public boolean createAllPerformanceHeaderCSV(EvaluationMultiValuesPerformance evaluationMultiValuesPerformance) {
        String stepLog = " ";
        /**
         * Write evaluation performance header to the csv file
         * */
        String updateWholeStreamStatisticsCSVLog = evaluationMultiValuesPerformance.createHeaderPerformanceCSV(this.verbose);
        boolean isBufferWritten = evaluationMultiValuesPerformance.checkIfBufferIsWritten(updateWholeStreamStatisticsCSVLog);
        boolean isStepSuccessful = true;
        if (isBufferWritten == false) {
            stepLog += "Error: creating header of csv line\n";
            isStepSuccessful = false;
        }

        /**
         * Significance test
         * */
        updateWholeStreamStatisticsCSVLog = evaluationMultiValuesPerformance.createHeaderStats4SignificanceCSV(this.verbose);
        isBufferWritten = evaluationMultiValuesPerformance.checkIfBufferIsWritten(updateWholeStreamStatisticsCSVLog);
        if (isBufferWritten == false){
            stepLog += "Error: creating header of csv line\n";
            isStepSuccessful = false;
        }

        /**
         * Write evaluation performance for base learners header to the csv file
         * */
        if (this.numberOfBaseLearners > 1) {
//            System.out.println("write the buffer for the base learner");
            updateWholeStreamStatisticsCSVLog = evaluationMultiValuesPerformance.createHeaderPerformanceBaseLearnerCSV(this.verbose);
            isBufferWritten = evaluationMultiValuesPerformance.checkIfBufferIsWritten(updateWholeStreamStatisticsCSVLog);
            if (isBufferWritten == false) {
                stepLog += "Error: creating header of csv line\n";
                isStepSuccessful = false;
            }
        }

        /**
         * Write weights of each base learner header to the csv file
         * */
        if (this.numberOfBaseLearners > 1) {
            updateWholeStreamStatisticsCSVLog = evaluationMultiValuesPerformance.createHeaderWeightBaseLearnerCSV(this.verbose);
            isBufferWritten = evaluationMultiValuesPerformance.checkIfBufferIsWritten(updateWholeStreamStatisticsCSVLog);
            if (isBufferWritten == false) {
                stepLog += "Error: creating header of csv line\n";
                isStepSuccessful = false;
            }
        }

        /**
         * Write size and sliding flag of each base learner header to csv file
         * */
        if (this.numberOfBaseLearners > 1) {
            updateWholeStreamStatisticsCSVLog = evaluationMultiValuesPerformance.createHeaderCountsBaseLearnerCSV(this.verbose);
            isBufferWritten = evaluationMultiValuesPerformance.checkIfBufferIsWritten(updateWholeStreamStatisticsCSVLog);
            if (isBufferWritten == false) {
                stepLog += "Error: creating header of csv file\n";
                isStepSuccessful = false;
            }
        }

        return isStepSuccessful;

    }

    public ApplyEvaluationResult applyPrequentialEvaluation(String learningAlgorithm, BayesianStreamBig trainingStream, EvaluationOneValuePerformance evaluationOneValuePerformance, EvaluationMultiValuesPerformance evaluationMultiValuesPerformance, BufferedWriter debugBuffer, boolean verbose, File outputDirectory) {
        //count number of correctly classified instances (tweets, documents, ..)
        long totalNumberCorrectInstances = 0;
        long totalNumberTrainingInstances = 0;

        boolean isFirstInstanceProcessed = false;
        Instance trainingInstance;
        SamoaInstanceWithTimeStamp trainingInstanceWithTimeStamp;
        DateTime trainingInstanceDateTime = null;
        String trainingInstanceTimeStamp;
        SlidingWindowOfInstances ensembleSlidingWindowOfInstances = null;
        ArrayList<Boolean> isWindowSlided = new ArrayList<Boolean>();
        String stepLog = null;
        boolean isStepSuccessful = true;

        String updateWholeStreamStatisticsCSVLog;
        String updateStatisticsEnsembleCountsCSVLog;
        String updateStatisticsEnsembleDiversityLog;
        String updateDebugLogWholeInstanceRun;
        boolean isBufferWritten = true;
        EvaluationResult evaluationResult;
        ApplyEvaluationResult applyPrequentialEvaluationResult;
        boolean isSingleClassifierSelected = false;
        /**
         * for time estimation
         *
         * average time for the model = (Sum_i=1..N (elapsedTimeForTesting_instance_i + elapsedTimeForLearning_instance_i) ) / N
         */
        long startTime;
        double elapsedTimeForAddingInstanceToWindow = 0.0D;
        double elapsedTimeForInitializingWindows = 0.0D;
        double elapsedTimeForTesting = 0.0D;
        double elapsedTimeForDeletingInstancesFromMNB = 0.0D;
        double elapsedTimeForLearning = 0.0D;
        double allWindowsElapsedTimeForLearning = 0.0D;
        double allWindowsElapsedTimeForDeletingInstanceFromMNB = 0.0D;
        double sumOfElapsedTimeForDeletingInstancesFromMNBAndLearning = 0.0D;
        double elapsedTimeForRemovingInstancesFromWindows = 0.0D;

        double totalElapsedTimes = 0.0D;
        double averageTotalLearningTime = 0.0D;

        double sumElapsedTimeForAddingInstanceToWindow = 0.0D;
        double sumElapsedTimeForInitializingWindows = 0.0D;

        double sumElapsedTimeForTesting = 0.0D;

        double sumElapsedTimeForDeletingInstanceFromMNB = 0.0D;

        double sumElapsedTimeForLearning = 0.0D;
        double sumElapsedTimeForRemovingInstancesFromWindows = 0.0D;

        /**
         * Counter for number of
         * deleted (forgotten) instances from base learners
         * removed instances from data structure storing all instances of ensemble
         */
        long totalCountOfDeletingInstanceFromMNB = 0;
        long currentCountOfRemovingInstancesFromWindows = 0;
        long totalCountOfRemovingInstancesFromWindows = 0;
        int indexOfLongestTimePeriodOfWindows = -1;

        /**
         * Initialize the log file for this evaluation step
         * */
        stepLog = initializeStepLog(stepLog, "prequential");

        /**
         * Set up evaluation performance over sliding windows
         * */
        evaluationMultiValuesPerformance.createPerformanceEvaluator(PERFORMANCE_SLIDING_WINDOW_WIDTH);

        evaluationMultiValuesPerformance.createFadingFactorEvaluator(0.9D, 0);
        evaluationMultiValuesPerformance.createFadingFactorEvaluator(0.1D, 1);

        evaluationMultiValuesPerformance.setWindowEstimatorSize(50); //for email and spam data set //data set without time stamp
//        evaluationMultiValuesPerformance.setWindowEstimatorSize(PERFORMANCE_SLIDING_WINDOW_WIDTH); //sts1.6 data set //data set with time stamp

        /**
         * Set up performance counts and evaluators for each base learner
         * */
        System.out.println("number of learners " + this.numberOfBaseLearners);

        if (this.numberOfBaseLearners > 1) {
            evaluationMultiValuesPerformance.initializeBaseLearnersEvaluatorsAndAccuracyCounts(this.numberOfBaseLearners, PERFORMANCE_SLIDING_WINDOW_WIDTH);
        }


        /**
         * create header of csv files with performance over the whole stream
         * */
        isBufferWritten = createAllPerformanceHeaderCSV(evaluationMultiValuesPerformance);
        if (!isBufferWritten) {
            stepLog += "Error: creating header of csv files with performance information";
        }

        /**
         * Initialize
         * ArrayList of counts for ensemble of sliding windows
         * ArrayList of
         * */
        ArrayList<String> ensembleSlidingWindowCounts = new ArrayList();
        ArrayList<String> ensembleDiversity = new ArrayList();
        ArrayList<String> ensembleSlidingWindowStatistics = new ArrayList<String>();


        boolean ensembleDebug = this.debuggingPrequential;
        //select classifier
        ArrayList<SlidingWindowClassifier> selectedClassifiers = new ArrayList<SlidingWindowClassifier>();

        selectedClassifiers = getSelectedClassifier(learningAlgorithm, this.numberOfBaseLearners);

        int numberOfSelectedClassifiers = selectedClassifiers.size();
        long[] countOfDeletingInstanceFromMNB = new long[numberOfSelectedClassifiers];
        Arrays.fill(countOfDeletingInstanceFromMNB, 0);

        AccuracyUpdatedEnsembleUtilities accuracyUpdatedEnsembleUtilities = null;

        /**
         * the equality holds for ONLY TESTING the ensemble
         */
//        if (numberOfSelectedClassifiers > 1 || (numberOfSelectedClassifiers == 1 && ensembleDebug == true)){ //for ensemble learning initialize the class of slidingWindowInstances
        if (numberOfSelectedClassifiers > 1) {
            /**
             * Prepare ensemble for use
             * */
            prepareEnsembleForUse(numberOfSelectedClassifiers, selectedClassifiers, trainingStream);

            /**
             * create accuracy updated ensemble
             * */
            int chunkSize = 500; //TODO: deprecated to be removed
            int maxNumberOfBaseLearnersForVoting = 2;
            int numberOfClasses = trainingStream.getHeader().numClasses();
            int numberOfHoursInChunk = 2;
            accuracyUpdatedEnsembleUtilities = new AccuracyUpdatedEnsembleUtilities(chunkSize, this.numberOfBaseLearners, maxNumberOfBaseLearnersForVoting, numberOfClasses, numberOfHoursInChunk);

        } else { //single classifier is selected, e.g MNB
            isSingleClassifierSelected = true;
            /**
             * Prepare single model for use
             * */
            prepareSingleModelForUse(selectedClassifiers, trainingStream, outputDirectory);
        }
        /**
         * Samoa instances
         * */
        int dateAttributeIndex = trainingStream.getDateAttributeIndex();


        System.out.println("=== Prequential Evaluation ===");

        double[][] votesPerClassifier;
        double[] averagedVotesPerClass;

        SamoaInstanceWithTrainingIndex currentSamoaInstanceWithTrainingIndex = null;
        Example<com.yahoo.labs.samoa.instances.Instance> trainingInstanceExample;
        int isCurrentWindowSlided = -1;
        DateTime previousInstanceDateTime = null;

        /**
         * Samoa instances
         * */
        int currentInstanceNumAttributes = 0;
        double currentInstanceDate = 0.0D;


        boolean simpleAverage4BaseLearners = true;
//        boolean simpleAverage4BaseLearners = false;

/*
        System.out.println("--- Significance Test ---");
        System.out.println("Num,PredictedLabel,TruePredictedMatch");
*/
        while (trainingStream.hasMoreInstances()) {
            /** Debugging prints
             * System.out.println("Please wait: Test then train for new instance " + Long.toString(totalNumberTrainingInstances));
             * */
            if(totalNumberTrainingInstances % 10000 == 0){ //10000
                System.out.println("Please wait: Test then train for first " + Long.toString(totalNumberTrainingInstances));
            }
//            System.out.println("Please wait: Test then train for new instance " + Long.toString(totalNumberTrainingInstances));

            if (debuggingPrequential) {
                System.out.println("evaluating new instance " + Long.toString(totalNumberTrainingInstances));
                this.debugLog += "\n" + "=== Evaluating new instance " + Long.toString(totalNumberTrainingInstances) + " ===" + "\n";
            }

            trainingInstance = trainingStream.nextSamoaInstance();

            currentSamoaInstanceWithTrainingIndex = new SamoaInstanceWithTrainingIndex(trainingInstance, totalNumberTrainingInstances, dateAttributeIndex);

            /**
             * Initialize sliding windows or add instance to windows
             */
//            if ((numberOfSelectedClassifiers > 1 || (numberOfSelectedClassifiers == 1 && ensembleDebug))) {
            if (numberOfSelectedClassifiers > 1) {
//                if (isFirstTimeSlidingWindow) {
                if (isFirstInstanceProcessed == false) {
                    /**
                     * Initialize the sliding windows
                     */
                    startTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
                    ensembleSlidingWindowOfInstances = initializeSlidingWindows(numberOfSelectedClassifiers, currentSamoaInstanceWithTrainingIndex, ensembleDebug);
                    elapsedTimeForInitializingWindows = TimingUtils.nanoTimeToSeconds(TimingUtils.getNanoCPUTimeOfCurrentThread() - startTime);
                } else {
                    /**
                     * Add instance to windows
                     */
                    startTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
                    addInstanceToSlidingWindows(currentSamoaInstanceWithTrainingIndex, ensembleSlidingWindowOfInstances, ensembleDebug);
                    elapsedTimeForAddingInstanceToWindow = TimingUtils.nanoTimeToSeconds(TimingUtils.getNanoCPUTimeOfCurrentThread() - startTime);

                    elapsedTimeForInitializingWindows = 0.0D; //reset time for initializing windows

                    ensembleSlidingWindowCounts = updateSlidingWindowsCounts(numberOfSelectedClassifiers, ensembleSlidingWindowOfInstances);
                    updateStatisticsEnsembleCountsCSVLog = evaluationMultiValuesPerformance.updateCSVbyArrayListLine(ensembleSlidingWindowCounts, "ensembleSlidingWindowCounts", false, this.verbose);
                    isBufferWritten = evaluationMultiValuesPerformance.checkIfBufferIsWritten(updateStatisticsEnsembleCountsCSVLog);
                    if (isBufferWritten == false) {
                        stepLog += "Error: updating csv line\n";
                        isStepSuccessful = false;
                    }
                }
                sumElapsedTimeForInitializingWindows += elapsedTimeForInitializingWindows;
                sumElapsedTimeForAddingInstanceToWindow += elapsedTimeForAddingInstanceToWindow;
            }


            /**
             * Test instance
             */
            trainingInstanceExample = new InstanceExample(trainingInstance);

            /**
             * Samoa instance
             * */
            //for data set with time stamp
            currentInstanceDate = trainingInstance.valueSparse(dateAttributeIndex);
            trainingInstanceDateTime = new DateTime((long) currentInstanceDate * 1000);

/*
            //data set without time stamp
            trainingInstanceDateTime = new DateTime(2018, 5, 20, 12, 0 , 0);
*/

            startTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
            votesPerClassifier = testInstance(numberOfSelectedClassifiers, trainingInstance, selectedClassifiers);

            if ((numberOfSelectedClassifiers > 1) && simpleAverage4BaseLearners == false) {
                accuracyUpdatedEnsembleUtilities.updateEnsembleStatisticsForChunk(trainingInstanceExample, trainingInstanceDateTime, votesPerClassifier);
//                averagedVotesPerClass = accuracyUpdatedEnsembleUtilities.calculateWeightedVotePerClass(votesPerClassifier, isFirstInstanceProcessed);
//                averagedVotesPerClass = averagingVotesPerClass(votesPerClassifier);
                averagedVotesPerClass = accuracyUpdatedEnsembleUtilities.averagingWeigthedVotesPerClass(votesPerClassifier);
            } else {
//                System.out.println("Yeap we do simple average.. ");
                averagedVotesPerClass = averagingVotesPerClass(votesPerClassifier);
            }
            elapsedTimeForTesting = TimingUtils.nanoTimeToSeconds(TimingUtils.getNanoCPUTimeOfCurrentThread() - startTime);
            sumElapsedTimeForTesting += elapsedTimeForTesting;


            /**
             * Update the classifier evaluator for all course of the data stream
             * */
//            evaluationMultiValuesPerformance.updateWholeStreamPerformanceBatch(trainingInstanceExample, averagedVotesPerClass, this.verbose); //data set without time stamp -> batch evaluation
            evaluationMultiValuesPerformance.updateWholeStreamPerformance(trainingInstanceExample, averagedVotesPerClass, votesPerClassifier, ensembleSlidingWindowOfInstances, accuracyUpdatedEnsembleUtilities, trainingInstanceDateTime, this.verbose); //data set with time stamp -> aggregation of sliding windows
            evaluationMultiValuesPerformance.updateStats4Significance(trainingInstance, averagedVotesPerClass, this.verbose);

            //TODO:Add            updateWholeStream_Kappa_Accuracy(trainingInstanceExample);

            /**
             * Update basic counts (TP, FP, ..) for (one-value) performance measures
             * Update total positives and negatives of the stream
             * Increase the number of instances evaluated by one
             */
            evaluationOneValuePerformance.updatePerformanceCounts(averagedVotesPerClass, trainingInstance);


            /**
             * Train classifier with instance
             * */
            if (numberOfSelectedClassifiers > 1) {//if classifier is an ensemble,
                /**
                 * calculate performance measures for each pair of learners:
                 * 1) average error rate
                 * 2) kappa statistics as shown in "Pruning Adaptive Boosting" D. Margineantu, T. Dietterich, ICML '97
                 * */
                /*
                //TODO: Inside the MultiValues

                if (ensembleDiversityBatchSize >= 1){
                    //update the tables
                    updateAverageErrorRatesOfAllPairs(trainingInstance, votesPerClassifier, averageErrorRateOfAllPairs);
                    updateContingencyTablesOfAllPairs(votesPerClassifier, contingencyTablesOfAllPairs);
                    //decrease the size of the batch
                    ensembleDiversityBatchSize--;
                }
                if (ensembleDiversityBatchSize == 0){ //batch is finished
                    //calculate average error rate and kappa
                    averageErrorRateOfAllPairsBatch = calculateAverageErrorRatesOfAllPairs(averageErrorRateOfAllPairs);
                    kappaOfAllPairsBatch = calculateKappaOfAllPairs(contingencyTablesOfAllPairs);
                    averageErrorRateKappaOfAllPairsBatchString = convertEnsembleStatisticsToStringList(averageErrorRateOfAllPairsBatch, kappaOfAllPairsBatch);
                    updateStatisticsEnsembleDiversityLog = updateCSVbyArrayListLine(averageErrorRateKappaOfAllPairsBatchString, bufferedWriterEnsembleDiversity, verbose);
                    //write to excel
                    if (!updateStatisticsEnsembleDiversityLog.equalsIgnoreCase("Success"))
                    {
                        stepLog += "Error: updating csv line.\n";
                        isStepSuccessful = false;
                        break;
                    }
                    //clear the average rates and contingency tables for all pairs
                    clearAverageErrorRateOfAllPairs(averageErrorRateOfAllPairs);
                    clearContingencyTablesOfAllPairs(numberOfBaseLearners, trainingInstance.numClasses(), contingencyTablesOfAllPairs);
                    ensembleDiversityBatchSize = OUTPUT_FREQUENCY_BATCH;
                }
*/

                /*updateAverageErrorRatesOfAllPairs(trainingInstance, votesPerClassifier, averageErrorRateOfAllPairs);
                updateContingencyTablesOfAllPairs(votesPerClassifier, contingencyTablesOfAllPairs);
                //TODO: implement kappa measure,
                //TODO: Write the in excel!!!
                //TODO: Test them before Prometheus!!!
                updateKappaOfAllPairs(contingencyTablesOfAllPairs, kappaOfAllPairs);
                */

                /**
                 * Train ensemble of classifiers with instance
                 */

                allWindowsElapsedTimeForDeletingInstanceFromMNB = 0.0;
                allWindowsElapsedTimeForLearning = 0.0;
                /**
                 * Debugging prints
                 System.out.println("=== Train ===");
                 */
                for (int slidingWindowIndex = 0; slidingWindowIndex < numberOfSelectedClassifiers; slidingWindowIndex++) {

//                    System.out.println("Removing instances from window - START");
                    /**
                     * check if the window was slided
                     * if yes, then remove the respective word counts for each instance from the model
                     * */
                    elapsedTimeForDeletingInstancesFromMNB = 0.0D;
                    if (ensembleSlidingWindowOfInstances.getIsWindowSlided(slidingWindowIndex)) {

                        startTime = TimingUtils.getNanoCPUTimeOfCurrentThread(); //System.nanoTime();
                        countOfDeletingInstanceFromMNB[slidingWindowIndex] += deleteInstancesFromBaseLearner(ensembleSlidingWindowOfInstances, selectedClassifiers, slidingWindowIndex);
                        elapsedTimeForDeletingInstancesFromMNB = TimingUtils.nanoTimeToSeconds(TimingUtils.getNanoCPUTimeOfCurrentThread() - startTime);

                        /**
                         * Remove after debugging ensemble
                         * */
                        if (debuggingPrequential) {
                            this.debugLog += "--- Deleting instances from base learner " + slidingWindowIndex + " ---\n";
                            this.debugLog += "m_probOfClass: \n" + Arrays.toString(selectedClassifiers.get(slidingWindowIndex).getProbOfClass()) + "\n";
                            this.debugLog += "--- ---\n";
//                        this.debugLog += "\n" +  selectedClassifiers.get(0).getThenCleanDebugLog();
                        }
                    }
                    //sum up the total time for deleting instances from the base learners (due to sliding)
                    allWindowsElapsedTimeForDeletingInstanceFromMNB += elapsedTimeForDeletingInstancesFromMNB;

//                    System.out.println("Removing instances from window - FINISH");

                    /**
                     * After removing instances, train each base learner of the ensemble
                     * */
                    startTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
                    trainBaseLearnerOnInstance(selectedClassifiers, trainingInstance, slidingWindowIndex);
                    elapsedTimeForLearning = TimingUtils.nanoTimeToSeconds(TimingUtils.getNanoCPUTimeOfCurrentThread() - startTime);
                    allWindowsElapsedTimeForLearning += elapsedTimeForLearning;


/**                 Debugging prints
 System.out.println("base learner " + slidingWindowIndex );
 System.out.println("m_probOfClass: \n" + Arrays.toString(selectedClassifiers.get(slidingWindowIndex).getProbOfClass()));
 */
                }//close for loop for removing instances from base learners and then training base learners
                /** Debugging prints
                 System.out.println("=== Train ===");
                 */
                //get the sum of elapsed time for removing instances from all windows (because of sliding)
                sumElapsedTimeForDeletingInstanceFromMNB += allWindowsElapsedTimeForDeletingInstanceFromMNB;
                //get the sum of elapsed times for learning on all windows
                sumElapsedTimeForLearning += allWindowsElapsedTimeForLearning;

                /**
                 * if the longest time window has been slided then remove the instances
                 * */
                elapsedTimeForRemovingInstancesFromWindows = 0.0D;

                indexOfLongestTimePeriodOfWindows = ensembleSlidingWindowOfInstances.findLongestTimePeriodOfWindows();
                if (ensembleSlidingWindowOfInstances.isLongestWindowSlided(indexOfLongestTimePeriodOfWindows)) {
//TODO:Add                    removeInstancesFromSlidingWindows();
                    /**
                     * Remove instances from structure saving them for the whole ensemble
                     * */
                    startTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
                    currentCountOfRemovingInstancesFromWindows = removeInstancesFromSlidingWindows(ensembleSlidingWindowOfInstances, indexOfLongestTimePeriodOfWindows);
                    elapsedTimeForRemovingInstancesFromWindows = TimingUtils.nanoTimeToSeconds(TimingUtils.getNanoCPUTimeOfCurrentThread() - startTime);
                    totalCountOfRemovingInstancesFromWindows += currentCountOfRemovingInstancesFromWindows;

                }
                sumElapsedTimeForRemovingInstancesFromWindows += elapsedTimeForRemovingInstancesFromWindows;
            } else {
                /**
                 * case of only one classifier
                 * */
                startTime = TimingUtils.getNanoCPUTimeOfCurrentThread();

                //TODO: equivalent to trainBaseLearnerOnInstance(selectedClassifiers, trainingInstance, 0);
                selectedClassifiers.get(0).trainOnInstance(trainingInstance);

                elapsedTimeForLearning = TimingUtils.nanoTimeToSeconds(TimingUtils.getNanoCPUTimeOfCurrentThread() - startTime);//System.nanoTime() - startTime
                /**
                 * words removed from filter
                 * will be removed from time series
                 * */
                selectedClassifiers.get(0).removeWordsFromTimeSeries(trainingStream.getRemovedWordsFromFilter());
                if (debuggingPrequential) {
                    this.debugLog += "\n" + selectedClassifiers.get(0).getThenCleanDebugLog();
                }

                sumElapsedTimeForLearning += elapsedTimeForLearning;
            }

            //update total elapsed time
            if (numberOfSelectedClassifiers > 1) {
                totalElapsedTimes += elapsedTimeForInitializingWindows + elapsedTimeForAddingInstanceToWindow + elapsedTimeForTesting + allWindowsElapsedTimeForLearning + allWindowsElapsedTimeForDeletingInstanceFromMNB + elapsedTimeForRemovingInstancesFromWindows;
            } else {
                totalElapsedTimes += elapsedTimeForTesting + elapsedTimeForLearning;
            }

            totalNumberTrainingInstances++; //increase the number of processed date

            //uncomment after debugging ensemble
            if (debuggingPrequential) {
                System.out.println("=== Prequential evaluation ===");

                this.debugLog += "=== Evaluation successful! ===" + "\n";

                updateDebugLogWholeInstanceRun = updateDebugLogByWholeInstanceRun(this.debugLog, debugBuffer, verbose); // bufferedWriterDebug,
                this.debugLog = " ";
                if (!updateDebugLogWholeInstanceRun.equalsIgnoreCase("Success")) {
                    stepLog += "Error: updating debug log txt file.\n";
                    isStepSuccessful = false;
                    break;
                }
            }
            isFirstInstanceProcessed = true; //first instance is processed,
            previousInstanceDateTime = trainingInstanceDateTime;


        }//close while-loop evaluate-then-train for all instances
//        System.out.println("--- ---");
        /**
         * update sliding window performance for last instance
         * */
        /**
         * Data set with time stamp
         * */
        evaluationMultiValuesPerformance.updateWholeStreamPerformanceForLastInstance(ensembleSlidingWindowOfInstances, accuracyUpdatedEnsembleUtilities, this.verbose); //use for aggregation of sliding windows

        /**
         * Sum up the number of instances deleted for each MNB model
         * */
        totalCountOfDeletingInstanceFromMNB = sumNumberOfDeletedInstancesFromBaseLearners(countOfDeletingInstanceFromMNB);

        /**
         * Print average time per operation of the model
         * */
        printAverageTimePerOperation(numberOfSelectedClassifiers, totalNumberTrainingInstances, totalCountOfDeletingInstanceFromMNB, totalCountOfRemovingInstancesFromWindows, countOfDeletingInstanceFromMNB, totalElapsedTimes, sumElapsedTimeForInitializingWindows, sumElapsedTimeForAddingInstanceToWindow, sumElapsedTimeForTesting, sumElapsedTimeForDeletingInstanceFromMNB, sumElapsedTimeForLearning, sumElapsedTimeForRemovingInstancesFromWindows);

        /**
         * save sliding window performance measure to the csv file
         * */
        //after finishing looping through input stream
        if (isStepSuccessful) {

            /**
             * time series + number of registered words
             * */
            /*System.out.println("=== Number of registered words ===");
            System.out.println(selectedClassifiers.get(0).getSizeOfInvertedIndexOfWords2Times());
            System.out.println("=== ===");*/
            /**
             * Update step log with measured average time per operation
             * */
            stepLog += updateStepLogWithTime(totalNumberTrainingInstances, numberOfSelectedClassifiers, countOfDeletingInstanceFromMNB, totalCountOfDeletingInstanceFromMNB, totalCountOfRemovingInstancesFromWindows, totalElapsedTimes, sumElapsedTimeForInitializingWindows, sumElapsedTimeForAddingInstanceToWindow, sumElapsedTimeForTesting, sumElapsedTimeForDeletingInstanceFromMNB, sumElapsedTimeForLearning, sumElapsedTimeForRemovingInstancesFromWindows, ensembleDebug);
        } else {
            stepLog += "Error: evaluation finished due to error on update csv line.\n";
        }
        stepLog += "~~~ End of applying prequential evaluation ~~~\n";
        if (debuggingPrequential) {
            this.debugLog += "=== Prequential Evaluation ===" + "\n";
            updateDebugLogByWholeInstanceRun(this.debugLog, debugBuffer, verbose); //bufferedWriterDebug
        }

        System.out.println("=== Prequential Evaluation ===");

        /**
         * reporting time series pred
         * */
        if (numberOfSelectedClassifiers == 1) {
            HashMap<String, TimeSeriesPredPerformance> trackedWordTimeSeriesPerf = selectedClassifiers.get(0).getTrackedWordTimeSeriesPerf();
            for (String wordName : trackedWordTimeSeriesPerf.keySet()) {
                trackedWordTimeSeriesPerf.get(wordName).flushAndCloseBuffer(trackedWordTimeSeriesPerf.get(wordName).timeSeriesPred4WordBuffer);
            }
        }
        /**
         * write whole stream statistics to output txt file
         * */
        String statisticsOverWholeJuneLog = evaluationOneValuePerformance.printEvaluationStatistics(this.evaluationScheme, this.verbose);
        String writingLog = this.saveStringLogToFile(outputDirectory, statisticsOverWholeJuneLog, "statisticsOverWholeStreamEvaluation.txt", verbose);
        evaluationResult = null;
        return new ApplyEvaluationResult(evaluationResult, stepLog, isStepSuccessful);
    }


    /**
     * Saves log to txt file located to output directory
     *
     * @param outputDirectoryPath File, file of the output directory
     * @param runLog              String, holding the log text
     * @param logFileName         String, the name of the txt file
     * @param verbose             boolean, flag to have (true) verbose out of step log or not (false)
     * @return String, success message or error messages
     */
    public String saveStringLogToFile(File outputDirectoryPath, String runLog, String logFileName, boolean verbose) {
        String stepLog = null;
        String runLogFileName = logFileName; //"runLog.txt"
        String runLogFilePath = outputDirectoryPath.getPath() + File.separator + runLogFileName;
        BufferedWriter runLogBufferedWritter = null;

        try {
            File runLogFile = new File(runLogFilePath);
            runLogBufferedWritter = new BufferedWriter(new FileWriter(runLogFile));
            runLogBufferedWritter.write(runLog);
        } catch (Exception e) {
            stepLog += "Error: Cannot write the String log to txt file.\n" + e.toString() + "\n";
            if (verbose) {
                System.out.println("Error: Cannot write the String log to txt file.\n" + e.toString());
            }
        } finally { //close file even if you could not write on it
            try {
                runLogBufferedWritter.close();
            } catch (Exception e) {
                stepLog += "Error: Cannot close txt file containing String log.\n" + e.toString() + "\n";
                if (verbose) {
                    System.out.println("Error: Cannot close txt file containing String log.\n" + e.toString());
                }
            }
        }
        if (verbose && stepLog == null) {
            System.out.println("Info: Writing String log to " + runLogFilePath + ".");
            stepLog = "Info: Writing String log to " + runLogFilePath + ".\n";
        }

        return stepLog;
    }

    public BufferedWriter createDebugBuffer(File outputDirectory) {
        String debugLogFile = outputDirectory.getPath() + File.separator + "debugLogs.txt";
        FileWriter debugLogFileWriter = null;
        BufferedWriter debugLogBufferedWriter = null;
        try {
            debugLogFileWriter = new FileWriter(debugLogFile);
            debugLogBufferedWriter = new BufferedWriter(debugLogFileWriter);
        } catch (IOException e) {
            System.out.println("Error: Cannot create debug log txt file\n" + e.toString());
        }
        return debugLogBufferedWriter;
    }


}

