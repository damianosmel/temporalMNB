package de.l3s.oscar.TimeSeries;

import org.joda.time.DateTime;

/**
 * For a given shift of a day represents start and end of observed statistics
 *
 * Created by damian on 28.11.17.
 */
public class StatsStartEndTime {
    int numberOfClasses;
    int numberOfRecordedTimes;
    DateTime [] startTimePerClass;
    DateTime [] endTimePerClass;
    static DateTime defaultTime = new DateTime();

    public StatsStartEndTime(int numberOfClasses){
        this.numberOfClasses = numberOfClasses;

        this.startTimePerClass = new DateTime[numberOfClasses];
        this.endTimePerClass = new DateTime[numberOfClasses];

        for (int classIndex=0; classIndex<this.numberOfClasses; classIndex++){
//            this.startTimePerClass[classIndex] = new DateTime();
            this.startTimePerClass[classIndex] = defaultTime;
            this.endTimePerClass[classIndex] = defaultTime;
        }
    }

    public DateTime getStartTimeOfClass(int classIndex){
        assert classIndex < numberOfClasses : "Error: cannot get start time of class with index bigger than the maximum.";
        return this.startTimePerClass[classIndex];
    }

    public DateTime getEndTimeOfClass(int classIndex){
        assert classIndex < numberOfClasses : "Error: cannot get stop time of class with index bigger than the maximum.";
        return this.endTimePerClass[classIndex];
    }

    public void setStartTimeToDefault(int classIndex){
        this.startTimePerClass[classIndex] = defaultTime;
    }

    public void setEndTimeToDefault(int classIndex){
        this.endTimePerClass[classIndex] = defaultTime;
    }
    public void setStartTimeOfClass(int classIndex, DateTime startTimeOfClass){
        assert classIndex < numberOfClasses : "Error: cannot set start time of class with index bigger than the maximum.";
        this.startTimePerClass[classIndex] = startTimeOfClass;
    }

    public void setEndTimePerClass(int classIndex, DateTime endTimeOfClass){
        assert classIndex < numberOfClasses : "Error: cannot set end time of class with index bigger than the maximum.";
        this.endTimePerClass[classIndex] = endTimeOfClass;
    }

    public boolean startTimeEqualsDefault(int classIndex){
        if(this.startTimePerClass[classIndex].isEqual(defaultTime)){
            return true;
        }
        else{
            return false;
        }
    }

    public boolean endTimeEqualsDefault(int classIndex){
        if(this.endTimePerClass[classIndex].isEqual(defaultTime)){
            return true;
        }
        else{
            return false;
        }
    }
}
