package de.l3s.oscar.TimeSeries;

import java.util.Arrays;

/**
 * Containing values for each class that are aggregated per shift
 * and the number of time points observed in this shift
 * Created by damian on 17.11.17.
 */
public class SumStatsOverShift {
    int numberOfClasses;
    double[] conditionalCountsPerClass; //e.g N(word, class)
    long[] numberOfTimePointsPerClass;
    //TODO: number of documents (idf)

    public SumStatsOverShift(int numberOfClasses){
        this.numberOfClasses = numberOfClasses;

        this.conditionalCountsPerClass = new double[this.numberOfClasses];
        Arrays.fill(this.conditionalCountsPerClass, 0.0);

        this.numberOfTimePointsPerClass = new long[this.numberOfClasses];
        Arrays.fill(this.numberOfTimePointsPerClass, 0L);
    }

    public long getNumberOfTimePoints(int classIndex){
        assert classIndex < this.numberOfClasses : "Error: cannot get number of time points in a class bigger than the max one.";
        return this.numberOfTimePointsPerClass[classIndex];
    }

    public void setNumberOfTimePoints(long numberOfTimePoints, int classIndex){
        assert classIndex < this.numberOfClasses : "Error: cannot set number of time points in a class bigger than the max one.";
        this.numberOfTimePointsPerClass[classIndex] = numberOfTimePoints;
    }

    public void increaseNumberOfTimePoints(double increaseStep, int classIndex){
        assert classIndex < this.numberOfClasses : "Error: cannot increase the conditional count for a class bigger than the max one.";
        this.numberOfTimePointsPerClass[classIndex] += increaseStep;
    }

    public void decreaseNumberOfTimePoints(double decreaseStep, int classIndex){
        assert classIndex < this.numberOfClasses : "Error: cannot decrease the conditional count for a class bigger than the max one.";
        this.numberOfTimePointsPerClass[classIndex] -= decreaseStep;
        if(this.numberOfTimePointsPerClass[classIndex] < 0){
            this.numberOfTimePointsPerClass[classIndex] = 0L;
        }
    }

    public double getConditionalCountsPerClass(int classIndex){
        assert classIndex < this.numberOfClasses : "Error: cannot get conditional count for a class bigger than the max one.";
        return conditionalCountsPerClass[classIndex];
    }

    public void setConditionalCountsPerClass(double conditionalCountsPerClass, int classIndex){
        assert classIndex < this.numberOfClasses : "Error: cannot set conditional count for a class bigger than the max one.";
        this.conditionalCountsPerClass[classIndex] = conditionalCountsPerClass;
    }

    public void increaseConditionalCount(double increaseStep, int classIndex){
        assert classIndex < this.numberOfClasses : "Error: cannot increase the conditional count for a class bigger than the max one.";
        this.conditionalCountsPerClass[classIndex] += increaseStep;

    }

    public void decreaseConditionalCount(double decreaseStep, int classIndex){
        assert classIndex < this.numberOfClasses : "Error: cannot decrease the conditional count for a class bigger than the max one.";
        this.conditionalCountsPerClass[classIndex] -= decreaseStep;
        if (this.conditionalCountsPerClass[classIndex] < 0.0){
            this.conditionalCountsPerClass[classIndex] = 0.0;
        }
    }
}
