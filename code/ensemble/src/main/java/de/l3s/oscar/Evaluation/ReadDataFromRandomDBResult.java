package de.l3s.oscar.Evaluation;

/**
 *
 * <h1>Class to hold results from reading data randomly from DB</h1>
 *
 * At the step of reading data from DB connection holds the results of the operation
 * Class to save trainingStream, log message and success of step
 *
 * @since Jan 2017
 * @author Damianos Melidis
 */
public class ReadDataFromRandomDBResult {

    private final boolean success;
    private final String log;
    private final ExtendedBayesianStream trainingStream;

    /**
     * <h1> Constructor ReadDataFromRandomDBResult</h1>
     *
     * @param trainingStream            training instances read from the database connection
     * @param log                       log of reading training instances from the database connection
     * @param success                   flag to show success(true) or failure(false) to read from the database connection
     */
    public ReadDataFromRandomDBResult(ExtendedBayesianStream trainingStream, String log, boolean success)
    {
        this.trainingStream = trainingStream;
        this.success = success;
        this.log = log;
    }

    ExtendedBayesianStream getTrainingStream()
    {
        return this.trainingStream;
    }

    boolean getSuccess()
    {
        return this.success;
    }

    String getLog(){return this.log;}
}
