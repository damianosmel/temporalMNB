package de.l3s.oscar.Evaluation;

/**
 * Created by damian on 25.01.17.
 *
 * <h1>Class to hold results from prequential evaluation</h1>
 *
 * At the step of applying prequential evaluation the class holds
 * the basic statistics of the performed classification
 * the log of the evaluation
 * and the success of the evaluation
 *
 * @since Jan 2017
 * @author Damianos Melidis
 *
 */
public class ApplyEvaluationResult {

    private final EvaluationResult evaluationResult;
    private final String stepLog;
    private final boolean success;

    /**
     *
     *<h1>Constructor ApplyPrequentialEvaluationResult</h1>
     *
     * @param evaluationResult                      EvaluationResult, basic statistics out of this evaluation run
     * @param stepLog                               String, log message of the evaluation
     * @param success                               boolean, flag to show successful (true) or failed (false) evaluation
     */
    public ApplyEvaluationResult(EvaluationResult evaluationResult, String stepLog, boolean success)
    {
        this.evaluationResult = evaluationResult;
        this.stepLog = stepLog;
        this.success = success;
    }

    public EvaluationResult getEvaluationResult(){
        return this.evaluationResult;}

    public String getStepLog()
    {return this.stepLog;}

    public boolean getSuccess()
    {return this.success;}
}
