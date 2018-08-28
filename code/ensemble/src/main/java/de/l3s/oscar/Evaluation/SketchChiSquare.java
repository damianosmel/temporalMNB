package de.l3s.oscar.Evaluation;

import java.util.ArrayList;
import java.util.Map;

public interface SketchChiSquare {

    /**
     * Produced by code of Behzad in June 2018.
     * */
    public int addToken(String token, int freq); //insert()

    public int addToken(String token, int freq, int classIndex);

    public void showNodes();

    public void addDoc(double docSize);

    public double getCount(String token);

    public int getAttIndex(String token);

    public double getFreqWord(String token);

    public void remove(String token);

    public int addToken(String token, int freq, int classIndex, int[] classesFreq);

    public void showChiSquares();

    public void addDoc(double docSize, int classValue);

    public void setNumDocNegPos(int classValue);

    public Map<String, SpaceSavingChiSquare.Node> getMaps();
    public ArrayList<SpaceSavingChiSquare.Node> getNodes();

    public Map<String, SpaceSavingChiSquare.Node> getChiSqaures();

    public void updateAllChiSquares();

}
