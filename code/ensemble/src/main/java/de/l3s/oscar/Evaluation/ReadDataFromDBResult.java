package de.l3s.oscar.Evaluation;


/**
 * Created by damian on 20.01.17.
 *
 * <h1>Class to hold results from reading data sequentially from DB</h1>
 *
 * At the step of reading data from DB connection holds the results of the operation
 * Class to save trainingStream, log message and success of step
 *
 * @since Jan 2017
 * @author Damianos Melidis
 */
public class ReadDataFromDBResult {
    private final boolean success;
    private final String log;
    private final BayesianStreamBig trainingStream;

    /**
     * <h1>Constructor ReadDataFromDBResult</h1>
     *
     * @param trainingStream            training instances read from the database connection
     * @param log                       log of reading training instances from the database connection
     * @param success                   flag to show success(true) or failure(false) to read from the database connection
     *
     * @since Jan 2017
     * @since Damianos Melidis
     */
    public ReadDataFromDBResult(BayesianStreamBig trainingStream, String log, boolean success)
    {
        this.trainingStream = trainingStream;
        this.log = log;
        this.success = success;
    }

    public BayesianStreamBig getTrainingStream()
    {
        return this.trainingStream;
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
