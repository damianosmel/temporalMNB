package de.l3s.oscar.TimeSeries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Vector;

/**
 * Created by damian on 02.02.18.
 */
public class EnsembleOfPredictions {

    //number of experts
    int expertsNum = 0;
    double maxB; //maximum upper value to predict
    /**
     * weights and predictions -> vectors of size (num of experts x num of class)
     * dotProduct(weightsT,predictions) -> predictionsEns, per class
     * */
    ArrayList<ArrayList<Double>> weights;
    ArrayList<ArrayList <Double>> predictionsExp;

    double [] predictionsEns;

    //trueOutcome per class
    ArrayList<Double> trueOutcome;
    //number of classes
    int numberOfClasses;

    public EnsembleOfPredictions(int expertsNum, int numberOfClasses, double maxB){
        assert expertsNum != 0 : "Ensemble of predictions can be created aggregating 0 base learners.";
        this.expertsNum = expertsNum;

        this.numberOfClasses = numberOfClasses;
        this.predictionsEns = new double [this.numberOfClasses];
        this.maxB = maxB;
        initializeWeights();
        initializePredictions();
        initializeTrueOutcome();
        Arrays.fill(this.predictionsEns, 0.0D);
    }

    private void initializeWeights(){
        this.weights = new ArrayList<ArrayList<Double>>();

        for(int classIndex = 0; classIndex < this.numberOfClasses; classIndex++){
            ArrayList<Double> weightsPerExp = new ArrayList<Double>();
            for(int expertIndex = 0; expertIndex < this.expertsNum; expertIndex++){
                weightsPerExp.add(1.0/this.expertsNum);
            }
            this.weights.add(weightsPerExp);
        }
    }

    private void initializePredictions(){
        this.predictionsExp = new ArrayList<ArrayList<Double>>();

        for(int classIndex = 0; classIndex < this.numberOfClasses; classIndex++){
            ArrayList<Double> predPerExp = new ArrayList<Double>();
            for(int expertIndex = 0; expertIndex < this.expertsNum; expertIndex++){
                predPerExp.add(0.0D);
            }
            this.predictionsExp.add(predPerExp);
        }

    }

    private void initializeTrueOutcome(){
        this.trueOutcome = new ArrayList<Double>();
        for(int classIndex = 0; classIndex < this.numberOfClasses; classIndex++){
            this.trueOutcome.add(0.0D);
        }
    }

    protected void printPredictionsExp(){
        System.out.println("=== Prediction of Experts ===");
        for(int classIndex = 0; classIndex < this.numberOfClasses; classIndex++){
            System.out.println("Class " + classIndex);
            for(int expertIndex = 0; expertIndex < this.expertsNum; expertIndex++){
                System.out.println("Expert " + expertIndex + " -> " + this.predictionsExp.get(classIndex).get(expertIndex));
            }
        }
        System.out.println("=== ===");
    }

    protected void collectPredictionsExp(ArrayList<ArrayList<Double>> expertsPrediction){

        Collections.copy(this.predictionsExp, expertsPrediction);
        /**
         * Loop through each predictions and if the predicted value is above the limit set it to the maximum upper limit
         * */
        for(int classIndex = 0; classIndex < this.numberOfClasses; classIndex++){
            for(int expertIndex = 0; expertIndex < this.expertsNum; expertIndex++){
                if (expertsPrediction.get(classIndex).get(expertIndex) > this.maxB){
                    this.predictionsExp.get(classIndex).set(expertIndex, this.maxB);
                }
                else{
                    this.predictionsExp.get(classIndex).set(expertIndex, expertsPrediction.get(classIndex).get(expertIndex));
                }
            }
        }
    }

    protected void printTrueOutcome(){
//        System.out.println("=== True Outcome ===");
        for(int classIndex = 0; classIndex < this.numberOfClasses; classIndex++){
            System.out.println("Class " + classIndex + " ->> " + this.trueOutcome.get(classIndex));
        }
        System.out.println("=== ===");
    }

    protected double [] getTrueOutcome(){
        double [] trueOutcome = new double[this.numberOfClasses];
        for(int classIndex = 0; classIndex < this.numberOfClasses; classIndex++){
            trueOutcome[classIndex] = this.trueOutcome.get(classIndex);
        }
        return trueOutcome;
    }
    protected void receiveTrueOutcome(ArrayList<Double> trueOutcome){
        Collections.copy(this.trueOutcome, trueOutcome);
    }

    protected void printPredictionsEns(){
        System.out.println("=== Prediction of Ensemble ===");
        for(int classIndex = 0; classIndex < this.numberOfClasses; classIndex++){

            System.out.println("Class " + classIndex + " Ensemble -> " + this.predictionsEns[classIndex]);
        }
        System.out.println("=== ===");
    }

    protected double [] predict(){

        double weightedAverageClass = 0.0D;
        for(int classIndex = 0; classIndex < this.numberOfClasses; classIndex++){
            weightedAverageClass = 0.0D;
            for(int expertIndex = 0; expertIndex < this.expertsNum; expertIndex++){
                weightedAverageClass += this.weights.get(classIndex).get(expertIndex) * this.predictionsExp.get(classIndex).get(expertIndex);
            }

            this.predictionsEns[classIndex] = weightedAverageClass;
        }
        return this.predictionsEns;
    }


    protected void printWeights(){
        System.out.println("=== Weights of Experts ===");
        for(int classIndex = 0; classIndex < this.numberOfClasses; classIndex++){
            System.out.println("Class " + classIndex);
            for(int expertIndex = 0; expertIndex < this.expertsNum; expertIndex++){
                System.out.println("weight of exp " + expertIndex + " -> " + this.weights.get(classIndex).get(expertIndex));
            }
        }
        System.out.println("=== ===");
    }
}
