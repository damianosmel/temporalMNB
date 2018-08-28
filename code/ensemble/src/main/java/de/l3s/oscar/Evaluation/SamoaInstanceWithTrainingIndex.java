package de.l3s.oscar.Evaluation;

import com.yahoo.labs.samoa.instances.Instance;


/**
 * Class to store a samoa instance with its index in the training order
 */
public class SamoaInstanceWithTrainingIndex {

    protected Instance trainingInstance = null;
    protected long trainingIndex = 0;
    protected int dateAttributeIndex = 0;

    public SamoaInstanceWithTrainingIndex(Instance currentInstance, long currentTrainingIndex, int dateAttributeIndex){
        this.trainingInstance = currentInstance;
        this.trainingIndex = currentTrainingIndex;
        this.dateAttributeIndex = dateAttributeIndex;
    }

    public Instance getTrainingInstance(){
        return this.trainingInstance;
    }

    public long getTrainingIndex(){
        return this.trainingIndex;
    }

    public double getDateOfInstance() {
        return this.trainingInstance.valueSparse(this.dateAttributeIndex);
    }
}
