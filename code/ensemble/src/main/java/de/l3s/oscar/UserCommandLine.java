package de.l3s.oscar;

import java.io.File;

import java.util.*;
import java.util.logging.Logger;
import java.io.IOException;


import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * <h1>Process Main() input arguments</h1>
 * Read user input arguments
 * Show error messages for incompatible input
 * Show help message
 *<p>
 * Expected command line modes:
 * [-v] --mode Preprocess&SaveDB --CollectionLocation (url or /absolute/path/to/local/machine) --ShortText (yes or no) --rootOutputDirectory /absolute/path/to/local/root/for/output/folder
 * [-v] --mode EvaluatePreprocessedData --SavedDBTitle --ShortText (yes or no) --LearningAlgorithm (mnbVanilla, mnbFading, mnbAggressive) --EvaluationScheme (prequential or holdout) --rootOutputDirectory /absolute/path/to/local/root/for/output/folder
 * [-v] --mode EvaluateRawData --CollectionLocation (url or /absolute/path/to/local/machine) --ShortText (yes or no) --LearningAlgorithm (mnbVanilla, mnbFading, mnbAggressive) --EvaluationScheme (prequential or holdout) --rootOutputDirectory /absolute/path/to/local/root/for/output/folder
 *
 * @see <a href="https://github.com/addthis/hermes/blob/master/src/main/java/com/addthis/hermes/framework/Main.java"> Command line </a>
 * @since Jan 2017
 * @author Damianos Melidis
 */
public class UserCommandLine
{
    /**
     * Inner class to represent parsed input arguments
     *
     */
    public boolean runningOnServer = false;
//    public boolean runningOnServer = true;


    public class ParsedCommandLine
    {
        private boolean verbose;
        private String runMode;
        private String collectionLocation;
        private boolean collectionLocationIsUrl;
        private String savedDBTitle;
        private boolean shortText;
        private String learningAlgorithm;
        private String evaluationScheme;
        private String rootOutputDirectory;
        private int numberOfBaseLearners;
        private boolean isParsingSuccessful = true;
        public  ParsedCommandLine(boolean verbose, String runMode, String collectionLocation, boolean collectionLocationIsUrl, String savedDBTitle, boolean shortText, String learningAlgorithm, String evaluationScheme, String rootOutputDirectory, int numberOfBaseLearners)
        {
            this.verbose = verbose;
            this.runMode = runMode;
            this.collectionLocation = collectionLocation;
            this.collectionLocationIsUrl = collectionLocationIsUrl;
            this.savedDBTitle = savedDBTitle;
            this.shortText = shortText;
            this.learningAlgorithm = learningAlgorithm;
            this.evaluationScheme = evaluationScheme;
            this.rootOutputDirectory = rootOutputDirectory;
            this.numberOfBaseLearners = numberOfBaseLearners;
        }

        public boolean getVerbose()
        {
            return this.verbose;
        }

        public String getRunMode()
        {
            return this.runMode;
        }

        public String getCollectionLocation()
        {
            return this.collectionLocation;
        }

        public boolean getCollectionLocationIsUrl()
        {
            return this.collectionLocationIsUrl;
        }

        public String getSavedDBTitle()
        {
            return this.savedDBTitle;
        }

        public boolean getShortText()
        {
            return this.shortText;
        }

        public String getLearningAlgorithm()
        {
            return this.learningAlgorithm;
        }

        public String getEvaluationScheme()
        {
            return this.evaluationScheme;
        }

        public String getRootOutputDirectory(){return this.rootOutputDirectory;}

        public int getNumberOfBaseLearners(){return this.numberOfBaseLearners;}

        public boolean isParsingSuccessful(){return this.isParsingSuccessful;}
        /**
         * Function to return the full user input arguments
         *
         *  @return fullParsedCommandLine            String, to hold all parsed user input arguments
         */
        public String getFullParsedCommandLine()
        {
            String fullParsedCommandLine = "OSCAR-main \n";

            fullParsedCommandLine += "--verbose " + Boolean.toString(this.verbose) +"\n";
            fullParsedCommandLine += "--run_mode " + this.runMode + "\n";
            fullParsedCommandLine += "--collection_location " + this.collectionLocation + "\n";
            fullParsedCommandLine += "--saved_db_title " + this.savedDBTitle + "\n";
            fullParsedCommandLine += "--short_text " + Boolean.toString(this.shortText) + "\n";
            fullParsedCommandLine += "--learning_algorithm " + this.learningAlgorithm + "\n";
            fullParsedCommandLine += "--evaluation_scheme " + this.evaluationScheme + "\n";
            fullParsedCommandLine += "--root_output_directory " + this.rootOutputDirectory + "\n";
            if (isParsingSuccessful()){
                fullParsedCommandLine += "\n Parsing user arguments succeed\n";
            }
            else{
                fullParsedCommandLine += "\n Parsing user arguments failed\n";
            }

            return fullParsedCommandLine;
        }
    }

    /**
     * Default usage of verbose evaluation
     * Can be overwritten by user choice
     */
    public static final boolean DEFAULT_USE_VERBOSE = true;

    /**
     * Default usage of short texts in the collection
     * Can be overwritten by user choice
     */
    public static final boolean DEFAULT_USE_SHORT_TEXT = true;

    @SuppressWarnings("static-access")
    private static final Option help = Option.builder("h").longOpt("help").desc("Print this help message.").build();
    private static final Option verbose = Option.builder("v").argName("true|false").longOpt("verbose").desc("Verbose out at standard output.").build();
    private static final Option runMode = Option.builder("r").longOpt("run_mode").argName("PreprocessSaveDB|EvaluateOffline|EvaluateOnline").desc("PreprocessSave2DB -> Preprocess data & save them to a SQL DB.\n EvaluateOffline -> Learn and evaluate from preprocessed data.\n EvaluateOnline -> Preprocess, learn and evaluate from raw data.").hasArg().required().type(String.class).build();
    private static final Option collectionLocation = Option.builder("c").longOpt("collection_location").argName("Url or absolute path to local machine").desc("If run_mode -> PreprocessSaveDB or EvaluateOnline, then insert url or absolute path to local machine \n else run_mode -> EvaluateOffline, -1.").hasArg().required().type(String.class).build();
    private static final Option savedDBTitle = Option.builder("d").longOpt("saved_db_title").argName("Name of SQL DB with preprocessed data").desc("If run_mode -> EvaluateOffline, Name of saved SQL database with preprocessed data \n else run_mode -> PreprocessSaveDB or EvaluateOnline, -1.").hasArg().required().type(String.class).build();
    private static final Option shortText = Option.builder("s").longOpt("short_text").argName("true|false").desc("The documents in your collection are of short (true) or medium/large size (false)?").hasArg().required().type(Boolean.class).build();
    private static final Option learningAlgorithm = Option.builder("l").longOpt("learning_algorithm").argName("mnbVanilla|mnbFading|mnbAggressive|-1").desc("Available algorithms: mnbVanilla, mnbFading, mnbAggressive\n if run_mode -> PreprocessSaveDB then learning_algorithm -> -1.").hasArg().required().type(String.class).build();
    private static final Option evaluationScheme = Option.builder("e").longOpt("evaluation_scheme").argName("holdout|prequfal|-1").desc("Available evaluation schemes: holdout, prequential\n if run_mode -> PreprocessSaveDB then learning_algorithm -> -1.").hasArg().required().type(String.class).build();
    private static final Option rootOutputDirectory = Option.builder("o").longOpt("root_output_directory").argName("absolute path of root directory for output").desc("Absolute path of root directory to place output folder.").hasArg().required().type(String.class).build();

    private static final Logger log = Logger.getLogger(UserCommandLine.class.getName());
    private String[] args = null;
    private Options options = new Options();

    /**
     * <h1>UserCommandLine Constructor</h1>
     * Add created options as Main arguments from command line
     * @param args
     * @since Jan 2017
     * @author Damianos Melidis
     */
    public UserCommandLine(String[] args)
    {

        this.args = args;

        options.addOption(help);
        options.addOption(verbose);
        options.addOption(shortText);
        options.addOption(runMode);
        options.addOption(collectionLocation);
        options.addOption(savedDBTitle);
        options.addOption(learningAlgorithm);
        options.addOption(evaluationScheme);
        options.addOption(rootOutputDirectory);
    }

    /**
     * <h1>Parse input arguments</h1>
     *
     * @throws IOException                  if input files cannot be read
     * @throws ParseException               if user did not input required parameters
     * @returns ParsedCommandLine           object from user input arguments
     * @since Jan 2017
     * @author Damianos Melidis
     */
    public ParsedCommandLine parse() throws ParseException//IOException, InterruptedException
    {
        String userRunMode = "";
        String userCollectionLocation = "";
        String userSavedDBTitle = "";
        String userLearningAlgorithm = "";
        String userEvaluationScheme = "";
        String userRootOutputDirectory = "";
        boolean userVerbose;
        boolean userShortText;
        if (runningOnServer) {
            /**
             * For Deken
             * */

            /**
             * --verbose
             false
             --run_mode
             EvaluateOffline
             --collection_location
             /home/damian/Desktop/commandLineOptions.txt
             --saved_db_title
             tweets140
             --short_text
             true
             --learning_algorithm
             ensemble_2
             --evaluation_scheme
             prequential
             --root_output_directory
             /home/damian/Desktop/Implementation/
             */

            userRunMode = "EvaluateOffline";
            /***
             * Deken and Prometheus
             */
            userCollectionLocation = "/home/melidis/commandLineOptions.txt";
            userSavedDBTitle = "oscar_melidis_1";
//            userLearningAlgorithm = "ensemble_3";
//            userLearningAlgorithm = "mnb";
//            userLearningAlgorithm = "mnb4Sketch";
            userLearningAlgorithm = "mnb4TimeSeries";
//            userLearningAlgorithm = "mnbFading";
//            userLearningAlgorithm = "mnbAggressive";

            userEvaluationScheme = "prequential";
            /**
             * Deken
             */
             //userRootOutputDirectory = "/media/fs/data-central/home/melidis/";

            /**
             * Prometheus
             * */
            userRootOutputDirectory = "/home/melidis/";
            userVerbose = false;
            userShortText = true;

            /** For Deken
             */
            /*
             * Options helpOptions = this.options;
             * showHelpMessage(this.args,helpOptions);

             * CommandLineParser parser = new DefaultParser();
             * CommandLine userCommandLine = null;
             */
            System.out.println("Please wait, parsing user arguments..");

            /**
             * for Deken
             */
             /*
             try{
             userCommandLine = parser.parse(this.options,this.args);

             } catch (ParseException ex){
             System.err.println("Parsing failed. Reason: " + ex.getMessage());
             System.exit(1);
             }*/
        }
        else{
            Options helpOptions = this.options;
            showHelpMessage(this.args,helpOptions);

            CommandLineParser parser = new DefaultParser();
            CommandLine userCommandLine = null;

            try{
                userCommandLine = parser.parse(this.options,this.args);

            } catch (ParseException ex){
                System.err.println("Parsing failed. Reason: " + ex.getMessage());
                System.exit(1);
            }


             userRunMode = userCommandLine.getOptionValue("run_mode");
             userCollectionLocation = userCommandLine.getOptionValue("collection_location");
             userSavedDBTitle = userCommandLine.getOptionValue("saved_db_title");
             userLearningAlgorithm = userCommandLine.getOptionValue("learning_algorithm");
             userEvaluationScheme = userCommandLine.getOptionValue("evaluation_scheme");
             userRootOutputDirectory = userCommandLine.getOptionValue("root_output_directory");
             userVerbose = Boolean.parseBoolean(userCommandLine.getOptionValue("verbose", Boolean.toString(DEFAULT_USE_VERBOSE)));
             userShortText = Boolean.parseBoolean(userCommandLine.getOptionValue("short_text", Boolean.toString(DEFAULT_USE_SHORT_TEXT)));
        }

      /** for Deken
      String userRunMode = userCommandLine.getOptionValue("run_mode");
      String userCollectionLocation = userCommandLine.getOptionValue("collection_location");
      String userSavedDBTitle = userCommandLine.getOptionValue("saved_db_title");
      String userLearningAlgorithm = userCommandLine.getOptionValue("learning_algorithm");
      String userEvaluationScheme = userCommandLine.getOptionValue("evaluation_scheme");
      String userRootOutputDirectory = userCommandLine.getOptionValue("root_output_directory");
       */


//      boolean collectionLocationIsSqlFile = false;
      boolean collectionLocationIsUrl = false;

      /*if (userCollectionLocation.endsWith("sql"))
      {
          System.out.println("Collection location is a SQL file");
          collectionLocationIsSqlFile = true;
      }*/
      if (userCollectionLocation.startsWith("www") || userCollectionLocation.startsWith("http"))
      {
          System.out.println("Collection location is a url");
          collectionLocationIsUrl = true;
          if (!userCollectionLocation.startsWith("http"))
          {
              userCollectionLocation = "http://" + userCollectionLocation;
          }
      }
/**
 *    for Deken
 *
 *    boolean userVerbose = Boolean.parseBoolean(userCommandLine.getOptionValue("verbose", Boolean.toString(DEFAULT_USE_VERBOSE)));
 *    boolean userShortText = Boolean.parseBoolean(userCommandLine.getOptionValue("short_text", Boolean.toString(DEFAULT_USE_SHORT_TEXT)));
 */


      /**
       * identify problematic inputs:
       *
       */
      if (identifyProblematicInputs(userRunMode, userEvaluationScheme, userSavedDBTitle, userLearningAlgorithm))
      {
          //System.exit(1);

          int numberOfBaseLearners = 0;
          ParsedCommandLine emptyParsedCommandLine = new ParsedCommandLine(userVerbose, userRunMode, userCollectionLocation, collectionLocationIsUrl, userSavedDBTitle,userShortText, userLearningAlgorithm, userEvaluationScheme, userRootOutputDirectory, numberOfBaseLearners);
          emptyParsedCommandLine.isParsingSuccessful = false;
          return emptyParsedCommandLine;

      }else
      {
          //if collection location is not url or -1 for the case of offline evaluation
          if (collectionLocationIsUrl == false && !userCollectionLocation.equalsIgnoreCase("-1") )
          {
              File collectionLocationFile = new File(userCollectionLocation);
              if (!collectionLocationFile.exists())
              {
                  System.out.println("Error: Input file path does not exist.");
                  System.exit(1);
              }
          }
/***
 *
 * For Deken

          //if root directory for output folder is not existing or is not directory
          File rootOutputDirectory = new File(userRootOutputDirectory);
          if( !rootOutputDirectory.exists() || !rootOutputDirectory.canWrite() || !userRootOutputDirectory.contains("/")) //!rootOutputDirectory.isDirectory() ||
          {
              System.out.println( Boolean.toString(rootOutputDirectory.exists()));
              System.out.println( Boolean.toString( rootOutputDirectory.canWrite() ) );
              System.out.println( Boolean.toString(userRootOutputDirectory.contains("/") ) );
              System.out.println("Error: Please, specify an absolute path to an existing write-accessible root directory for output.");
              System.exit(1);
          }
*/
          /*else
          {
              System.out.println("Info: Output directory " + rootOutputDirectory + " was successfully created.");
          }*/

          int numberOfBaseLearners = parseNumberOfBaseLearners(userLearningAlgorithm);
//          System.out.println("number of base learners " + Integer.toString(numberOfBaseLearners));

          ParsedCommandLine parsedCommandLine = new ParsedCommandLine(userVerbose, userRunMode, userCollectionLocation, collectionLocationIsUrl, userSavedDBTitle,userShortText, userLearningAlgorithm, userEvaluationScheme,userRootOutputDirectory, numberOfBaseLearners);
          parsedCommandLine.isParsingSuccessful = true;
          System.out.println("Parsing user arguments - Done!");
          return parsedCommandLine;
      }
    }

    /**
     * <h1>Print help message</h1>
     *
     * @since Jan 2017
     * @author Damianos Melidis
     */
    private static void showHelpMessage(String[] args, Options options)
    {
        Options helpOptions = new Options();

        helpOptions.addOption(Option.builder("h").longOpt("help").desc("Print this message").build());

        try{
            CommandLine helpLine = new DefaultParser().parse(helpOptions, args, true);
            if (helpLine.hasOption("help") || args.length == 0)
            {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("OSCAR-main", options);
                System.exit(0);
            }
        } catch (ParseException ex){
            System.err.println("Parsing failed. Reason: " + ex.getMessage());
            System.exit(1);
        }
    }

    /**
     *
     * <h1>Identify problematic user inputs</h1>
     *
     * @param runMode           user choice for running mode
     * @param evaluationScheme  user choice for evaluation scheme
     * @param savedDBTitle      user choice for saved DB title
     * @param learningAlgorithm user choice for learning algorithm
     * @since Jan 2017
     * @author Damianos Melidis
     */
    public boolean identifyProblematicInputs(String runMode, String evaluationScheme, String savedDBTitle, String learningAlgorithm)
    {
        boolean inputIsProblematic = false;

        //define the available implemented classifiers
        List<String> availableLearningAlgorithms = new ArrayList<String>(Arrays.asList("-1", "mnb", "mnb4sketch", "mnb4timeseries", "mnbfading", "mnbaggressive", "ensemble"));
        //load the list of available algorithms into a hashSet
        Set<String> availableLearningAlgorithmsSet =  new HashSet<String>(availableLearningAlgorithms);

        if ( !(runMode.equalsIgnoreCase("PreprocessSaveDB") || runMode.equalsIgnoreCase("EvaluateOffline") || runMode.equalsIgnoreCase("EvaluateOnline")) )
        {
            System.err.println("Error: Run mode can be one of the following PreprocessSaveDB, EvaluateOffline or EvaluateOnline.\n Please, run application with only parameter -h or --help.");
            inputIsProblematic = true;
            //return inputIsProblematic;
        }

        if( !(evaluationScheme.equalsIgnoreCase("prequential") || evaluationScheme.equalsIgnoreCase("holdout") || evaluationScheme.equalsIgnoreCase("-1")))
        {
            System.err.println("Error: Evaluation mode can be one of the following prequential or holdout");
            inputIsProblematic = true;
            //return inputIsProblematic;
        }

        if (runMode.equalsIgnoreCase("EvaluateOffline") && savedDBTitle.equalsIgnoreCase("-1")) //&& collectionLocationIsSqlFile == false)
        {
            System.err.println("Error: For offline evaluation, the name of SQL should be specified.");
            inputIsProblematic = true;
            //return inputIsProblematic;
        }

        if( (runMode.equalsIgnoreCase("EvaluateOnline") || runMode.equalsIgnoreCase("EvaluateOffline")) && (learningAlgorithm.equalsIgnoreCase("-1")|| evaluationScheme.equalsIgnoreCase("-1")))
        {
            System.out.println("Error: For on/offline evaluation, both algorithm and evaluation scheme are needed.");
            inputIsProblematic = true;
            //return inputIsProblematic;
        }

        if( runMode.equalsIgnoreCase("PreprocessSaveDB") && (!learningAlgorithm.equalsIgnoreCase("-1") || !evaluationScheme.equalsIgnoreCase("-1")) )
        {
            System.out.println("Warning: Evaluation mode is to preprocess and create DB, so learning algorithm and evaluation scheme are not to be considered.");
            inputIsProblematic = true;
            //return inputIsProblematic;
        }

        String splittedLearningAlgorithm = learningAlgorithm.split("_")[0];
        if (!availableLearningAlgorithmsSet.contains(splittedLearningAlgorithm.toLowerCase()))
        {
            System.out.println("Error: You have selected " + splittedLearningAlgorithm.toLowerCase());
            System.out.println("Error: Please select one of the available learning algorithms.");
            inputIsProblematic = true;
        }

        return inputIsProblematic;
    }


    /**
     * Parse the number of base learners that constitute the ensemble
     * @param learningAlgorithm         String, user input for learning algorithm
     * @return numberOfBaseLearners     int, parsed number of base learners for the ensemble
     */
    public int parseNumberOfBaseLearners(String learningAlgorithm){
        String ensembleName = "ensemble";
        String numberOfBaseLearners;
        String [] splittedlearningAlgorithm = learningAlgorithm.split("_");

        if (splittedlearningAlgorithm[0].equalsIgnoreCase(ensembleName)){

//            System.out.println("num of base learners = " +  splittedlearningAlgorithm[1]);
            //check for "adaptive" as second part of ensemble name
            return Integer.parseInt(splittedlearningAlgorithm[1]);
        }
        else{
            return -1;
        }
    }


}
