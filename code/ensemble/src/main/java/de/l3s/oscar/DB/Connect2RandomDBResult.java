package de.l3s.oscar.DB;

/**
 * Created by damian on 20.01.17.
 *
 * <h1>Class to hold results from connecting to DB to read data randomly</h1>
 *
 * At the step of connect2DB this class holds the result of the operation
 * Class saves a randomDBconnection, the log of connection and success of connection
 * @since Jan 2017
 * @author Damianos Melidis

 */
public class Connect2RandomDBResult {
    private final boolean success;
    private final String log;
    private final RandomDatabaseConnectionPreprocessed randomDBconnection;

    /**
     * <h1>Constructor Connect2RandomDBResult</h1>
     * @param randomDBconnection            connection to DB for randomly reading
     * @param log                           log of connection with database
     * @param success                       flag to show success(true) or failure(false) to connect to database connection
     *
     * @since Jan 2017
     * @author Damianos Melidis
     */
    public Connect2RandomDBResult(RandomDatabaseConnectionPreprocessed randomDBconnection, String log, boolean success)
    {
        this.randomDBconnection = randomDBconnection;
        this.log = log;
        this.success = success;
    }

    public RandomDatabaseConnectionPreprocessed getRandomDBconnection()
    {
        return this.randomDBconnection;
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
