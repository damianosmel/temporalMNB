package de.l3s.oscar;

import de.l3s.oscar.Evaluation.EvaluateOffline;
import de.l3s.oscar.Evaluation.PreprocessAndSaveDB;
import org.apache.commons.cli.ParseException;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVSaver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import static java.lang.System.in;

public class Main {
    public static void main(String[] args) throws ParseException, IOException {

        // write your code here

        UserCommandLine userCommandLine = new UserCommandLine(args);

        UserCommandLine.ParsedCommandLine parsedCommandLine = userCommandLine.parse();

        if (!parsedCommandLine.isParsingSuccessful()){
            parsedCommandLine.getFullParsedCommandLine();
            System.exit(1);
        }

        boolean runIsSuccessful = true;
        String runLog = "";



        //based on running mode create respective evaluation class
        if (parsedCommandLine.getRunMode().equalsIgnoreCase("PreprocessSaveDB"))
        {
            System.out.println("Mode: Preprocess data and save them on DB");
            PreprocessAndSaveDB preprocessAndSaveDB = new PreprocessAndSaveDB(parsedCommandLine);

//            preprocessAndSaveDB.readData();

            /*
             * Get statistics to understand data set
             *  a) Class counts for different time periods
             */
            preprocessAndSaveDB.collectAndWriteDataSetStats_run();
        }
        else if (parsedCommandLine.getRunMode().equalsIgnoreCase("EvaluateOffline"))
        {
            System.out.println("Mode: Evaluation offline");
            EvaluateOffline evaluateOffline = new EvaluateOffline(parsedCommandLine);
            evaluateOffline.runOfflineEvaluation();
        }
        else if (parsedCommandLine.getRunMode().equalsIgnoreCase("EvaluateOnline"))
        {
            System.out.println("Mode: Evaluation online");
        }

        if (runIsSuccessful)
        {

        }
        else
        {

        }

    }
}