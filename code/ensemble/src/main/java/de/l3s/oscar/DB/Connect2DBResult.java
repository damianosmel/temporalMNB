package de.l3s.oscar.DB;

/**
 * Created by damian on 20.01.17.
 *
 * <h1>Class to hold results from connecting to DB to read data sequentially</h1>
 *
 * At the step of connect2DB this class holds the result of the operation
 * Class saves a DBconnection, the log of connection and success of connection
 * @since Jan 2017
 * @author Damianos Melidis
 */
public class Connect2DBResult {
    private final boolean success;
    private final String log;
    private final DatabaseConnectionPreprocessed DBconnection;

    /**
     * <h1>Constructor Connect2DBResult</h1>
     * @param DBconnection          connection to DB for sequentially reading
     * @param log                   log of connection with database
     * @param success               flag to show success(true) or failure(false) to connect to database connection
     *
     * @since Jan 2017
     * @author Damianos Melidis
     */
    public Connect2DBResult(DatabaseConnectionPreprocessed DBconnection, String log, boolean success)
    {
        this.DBconnection = DBconnection;
        this.log = log;
        this.success = success;
    }

    public DatabaseConnectionPreprocessed getDBconnection()
    {
        return this.DBconnection;
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
