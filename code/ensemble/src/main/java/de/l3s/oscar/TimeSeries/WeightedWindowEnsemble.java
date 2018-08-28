package de.l3s.oscar.TimeSeries;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by damian on 02.02.18.
 */
public class WeightedWindowEnsemble extends EnsembleOfPredictions {
    double c = 0.0D;

    String lossName = new String();
    ArrayList<ArrayList<Double>> lossValueExpPerClass;
    public WeightedWindowEnsemble(int expertsNum, int numberOfClasses, double maxB, double c, String lossName){
        super(expertsNum, numberOfClasses, maxB);
        this.c = c;
        this.lossName = lossName;
        initializeLoss();
    }
    private void initializeLoss(){
        this.lossValueExpPerClass = new ArrayList<ArrayList<Double>>();

        for(int classIndex = 0; classIndex < this.numberOfClasses; classIndex++){
            ArrayList<Double> lossPerClass = new ArrayList<Double>();
            for(int expertIndex = 0; expertIndex < this.expertsNum; expertIndex++){
                lossPerClass.add(0.0D);
            }
            lossValueExpPerClass.add(lossPerClass);
        }
    }

    //https://en.wikipedia.org/wiki/Loss_function#Quadratic_loss_function
    protected void computeSquareLoss(){
//        System.out.println("=== Square Loss ===");
        double lossExpPerClass = 0.0D;
        for(int classIndex = 0; classIndex < this.numberOfClasses; classIndex++){
//            System.out.println("Class " + classIndex);
            for(int expertIndex = 0; expertIndex < this.expertsNum; expertIndex++){
                lossExpPerClass = Math.pow(this.trueOutcome.get(classIndex) - this.predictionsExp.get(classIndex).get(expertIndex), 2.0);
                this.lossValueExpPerClass.get(classIndex).set(expertIndex, lossExpPerClass);
//                System.out.println("expert " + expertIndex + " -> " + this.lossValueExpPerClass.get(classIndex).get(expertIndex));
            }
        }
//        System.out.println("=== ===");
    }
    protected void computeLoss(){
//        System.out.println("computeLoss");
        if(this.lossName.equals("square")){
            computeSquareLoss();
        }
    }

    protected void correctZeroWeights(int classIndex, double sumWeightsAllExpClass){
        for(int expertIndex = 0; expertIndex < this.expertsNum; expertIndex++){
            if(this.weights.get(classIndex).get(expertIndex) < 0.001D) {
                this.weights.get(classIndex).set(expertIndex, 0.001D);
            }
        }

        /*if(sumWeightsAllExpClass == 0.0D){
//            System.out.println("all weights for class are 0 -> 0.001 to all experts");
            for(int expertIndex = 0; expertIndex < this.expertsNum; expertIndex++){
                this.weights.get(classIndex).set(expertIndex,0.001D);
            }
        }*/
    }
    /**
     * Compute weights for experts of ensemble using the weighted average algorithm as in fig. 1 of
     * "Kivinen J, Warmuth MK. Averaging expert predictions.
     * In European Conference on Computational Learning Theory 1999 Mar 29 (pp. 153-167). Springer, Berlin, Heidelberg."
     * */
    protected void computeWeights(){
//        System.out.println("=== computeWeights ===");
        double weightExpPerClass = 0.0D;
        double sumWeightsAllExpClass = 0.0D;
        double numeratorWeight = 0.0D;
        //iterate and compute the numerator of the weights then normalize them
        for(int classIndex = 0; classIndex < this.numberOfClasses; classIndex++){
//            System.out.println("Class " + classIndex);
            sumWeightsAllExpClass = 0.0D;

            //compute numerator of weights
            for(int expertIndex = 0; expertIndex < this.expertsNum; expertIndex++){
//                System.out.println("loss: " + this.lossValueExpPerClass.get(classIndex).get(expertIndex));
//                weightExpPerClass = Math.exp(-1.0 * this.c * this.lossValueExpPerClass.get(classIndex).get(expertIndex)); //OLD to be removed
                weightExpPerClass = this.weights.get(classIndex).get(expertIndex) * Math.exp(-1.0 * this.lossValueExpPerClass.get(classIndex).get(expertIndex) / this.c);
                /*if(weightExpPerClass == 0.0D){
                    System.out.println("w_i_class == 0 -> 1");
                    weightExpPerClass = 0.001D;
                }*/
//                System.out.println("computed weight: " + weightExpPerClass);
                this.weights.get(classIndex).set(expertIndex, weightExpPerClass);
//                System.out.println("expert " + expertIndex + " -> " + this.weights.get(classIndex).get(expertIndex));
                sumWeightsAllExpClass += weightExpPerClass;
            }
//            correctZeroWeights(classIndex, sumWeightsAllExpClass);
            if(sumWeightsAllExpClass == 0.0D){
                sumWeightsAllExpClass = 1.0;
            }
//            System.out.println("total weight of class: " + sumWeightsAllExpClass);

//            System.out.println("normalize: ");
            //compute denominator of weights (normalize)
            for(int expertIndex = 0; expertIndex < this.expertsNum; expertIndex++){
                numeratorWeight = this.weights.get(classIndex).get(expertIndex);
                this.weights.get(classIndex).set(expertIndex, numeratorWeight / sumWeightsAllExpClass);
//                System.out.println("expert " + expertIndex + " -> " + this.weights.get(classIndex).get(expertIndex));
            }
        }
//        System.out.println("=== ===");
    }

    protected void updateWeights(){
        computeLoss();
        computeWeights();
    }


}
