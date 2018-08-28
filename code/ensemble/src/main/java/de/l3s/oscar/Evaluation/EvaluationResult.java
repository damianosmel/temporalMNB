package de.l3s.oscar.Evaluation;

/**
 * Created by damian on 03.02.17.
 */
public class EvaluationResult {

    private final long totalNumberTrainingInstances;
    private final long totalNumberCorrectInstances;
    private final long truePositives;
    private final long falsePositives;
    private final long trueNegatives;
    private final long falseNegatives;

    /**
     * <h1>Constructor EvaluationResult</h1>
     *
     * @param totalNumberTrainingInstances          long, total number of training instances of the evaluation
     * @param totalNumberCorrectInstances           long, total number of correctly classified instances of the evaluation
     * @param truePositives                         long, true positives value out of the evaluation
     * @param falsePositives                        long, false positives value out of the evaluation
     * @param trueNegatives                         long, true negatives value out of the evaluation
     * @param falseNegatives                        long, false negatives value out of the evaluation
     */
    public EvaluationResult(long totalNumberTrainingInstances, long totalNumberCorrectInstances, long truePositives, long falsePositives, long trueNegatives, long falseNegatives){

        this.totalNumberTrainingInstances = totalNumberTrainingInstances;
        this.totalNumberCorrectInstances = totalNumberCorrectInstances;
        this.truePositives = truePositives;
        this.falsePositives = falsePositives;
        this.trueNegatives = trueNegatives;
        this.falseNegatives = falseNegatives;
    }


    public long getTotalNumberTrainingInstances()
    {return this.totalNumberTrainingInstances;}

    public long getTotalNumberCorrectInstances()
    {return this.totalNumberCorrectInstances;}

    public long getTruePositives()
    {return this.truePositives;}

    public long getFalsePositives()
    {return this.falsePositives;}

    public long getTrueNegatives()
    {return this.trueNegatives;}

    public long getFalseNegatives()
    {return this.falseNegatives;}
}
