package de.l3s.oscar.Evaluation;

/**
 * <h1>Class to hold results from the offline evaluation run</h1>
 *
 * At the step of running the offline evaluation holds the results of the run
 * Class to save success and log of the run
 *
 * @since Jan 2017
 * @author Damianos Melidis
 */
public class RunEvaluateOfflineResult {
    private final boolean success;
    private final String log;

    /**
     *
     * <h1> Constructor RunEvaluateOfflineResult</h1>
     *
     * @param log           log of running offline evaluation
     * @param success       flag to show success (true) or failure (false) running the offline evaluation
     */
    public RunEvaluateOfflineResult(String log, boolean success)
    {
        this.log = log;
        this.success = success;
    }

    public String getLog()
    {
        return this.log;
    }

    public boolean getSuccess()
    {
        return this.success;
    }

}
