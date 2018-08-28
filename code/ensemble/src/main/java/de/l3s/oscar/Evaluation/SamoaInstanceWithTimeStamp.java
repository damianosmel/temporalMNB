package de.l3s.oscar.Evaluation;
import com.yahoo.labs.samoa.instances.Instance;

/**
 * Created by damian on 13.02.17.
 */

public class SamoaInstanceWithTimeStamp {

    private Instance samoaInstance;
    private String timeStamp;

    public SamoaInstanceWithTimeStamp(Instance samoaInstance, String timeStamp){
        this.samoaInstance = samoaInstance;
        this.timeStamp = timeStamp;
    }

    public Instance getSamoaInstance(){
        return this.samoaInstance;
    }

    public String getTimeStamp(){
        return this.timeStamp;
    }
}
