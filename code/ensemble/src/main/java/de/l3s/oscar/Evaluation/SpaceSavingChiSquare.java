package de.l3s.oscar.Evaluation;

import com.github.javacliparser.IntOption;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;
import weka.core.ContingencyTables;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SpaceSavingChiSquare extends AbstractOptionHandler implements SketchChiSquare {
    private static final long serialVersionUID = 1L;

    public IntOption capacityOption = new IntOption("capacity", 'c', "Number of attributes to use", 10000);

    protected int _top = 0;

    protected Map<String, Node> _map = null;

    protected ArrayList<Node> _nodes = null;

    protected int numDoc = 0;

    protected double numTerms = 0;

    // TODO: 04.06.18 Behzad

    protected double numDocPositive = 0;
    protected double numDocNegative = 0;

    // TODO: 03.06.18 Behzad add ClassPriors for terms

    protected Map<String, Node> _chiSqaure = null;
    protected double chiSqaureThreshold = 6.63;




    /**
     * Initialise a set of counters for String tokens
     *
     */
    @Override
    protected void prepareForUseImpl(TaskMonitor arg0, ObjectRepository arg1) {
        _top = 0;
        _map = new HashMap<String, Node>();
        _nodes = new ArrayList<Node>();
    }

    public int addToken(String token, int freq, int classIndex) {
        return addToken(token, freq);
    }

    // TODO: 14.04.18 Behzad new method for adding multiple new  ClassesFreq
    // TODO: 14.04.18 Behzad "CHANGE THE return VALUE" Done
    @Override
    public int addToken(String token, int totalfreq, int classIndex, int[] classesFreq) {
        return addToken(token, classesFreq, this.numDocPositive ,this.numDocNegative);
    }

    /**
     * Retrieve the (estimated, upper-bound) count for any token
     *
     * @param token
     * @return count (or 0 if not being counted currently, i.e. not frequent enough)
     */
    public double getCount(String token) {
        Node node = _map.get(token);
        if (node == null) {
            return 0.0;
        } else {
            return node.getCount();
        }
    }

    // TODO: 18.06.18 Behzad
    public double getChisquareValue(String token) {
        Node node = _map.get(token);
        if (node == null) {
            return 0.0;
        } else {
            return node.getChiSquareValue();
        }
    }

    // TODO: 04.06.18  Behzad
    public double[][] getClassPriors(String token) {
        Node node = _map.get(token);
        if (node == null) {
            double[][] a = { {0, 0}, {0,0} };
            return a;

        } else {
            return node.getClassPriors();
        }
    }

    /**
     * Get the attribute index of the token specified.
     *
     * @param token
     * @return
     */
    public int getAttIndex(String token) {
        Node node = _map.get(token);
        if (node != null) {
            return node.attrIndex;
        } else {
            return -1;
        }
    }

    /**
     * Add a token to be counted.
     *
     * @param token
     * @param freq
     * @return attributeIndex which is useful for constructing Instance objects
     */
    public int addToken(String token, int freq) {
        Node node = _map.get(token);
        if (node == null) {
            if (_top < this.capacityOption.getValue()) {
                fill(token, freq);
            } else {
                newToken(token, freq);
            }
        } else {
            addCount(node, freq);
            updatePosition(node);
        }

        return _map.get(token).attrIndex;
    }




    // TODO: 03.06.18 Behzad
    /*
     * ____________________________________
     * |_______|__Positive__|__Negative__|
     * |       |            |            |
     * |  TERM |     0      |      0     |
     * |__yes__|____________|____________|
     * |       |            |            |
     * |  TERM |     0      |      0     |
     * |___no__|____________|____________|
     *
     * */
    public double calculateChiSqaure(double[][] classpriors) {
        //ChiSquareWordStats tokenClassPriors = _chiSqaure.get(token);
        double [][] classPriors = classpriors;
        //sfd

        ContingencyTables chi = new ContingencyTables();
        double chi_value = chi.chiSquared(classpriors, false);
        return chi_value;
    }

    // TODO: 14.04.18 Behzad sync with classes freq
    public int addToken(String token, int[] freq, double numDocsPo, double numDocsNeg ) {
        Node node = _map.get(token);
        Node node2 = _chiSqaure.get(token);

        int c0Freq = freq[0];
        int c1Freq = freq[1];
        int totalFreq = c0Freq + c1Freq;
        //node.updateClassPriors(numDocsPo, numDocsNeg);
        //updateAllChiSquares();
        if (node == null) {
            if (_top < this.capacityOption.getValue()) {
                fill(token, c0Freq, c1Freq);
            } else {
                newToken(token, c0Freq, c1Freq);
            }
        } else {
            addCount(node, c0Freq, c1Freq);
            node.updateClassPriors(numDocsPo, numDocsNeg);
            node.chiSquareValue = calculateChiSqaure(node.classpiors);
            updatePosition(node);
        }


        return _map.get(token).attrIndex;
    }



    protected void fill(String token, int freq) {
        Node node = newNode(token, _top, freq);
        _nodes.add(node);
        _map.put(token, node);
        updatePosition(node);
        _top++;
    }

    // TODO: 14.04.18 Behzad
    protected void fill(String token, int c0Freq, int c1Freq) {
        Node node = newNode(token, _top, c0Freq, c1Freq, this.numDocPositive, this.numDocNegative);
        node.chiSquareValue = calculateChiSqaure(node.classpiors);
        if( node.chiSquareValue > chiSqaureThreshold){
            _chiSqaure.put(token, node);
            System.out.println(node.token + "--*-> Chi-Sqare : " + node.chiSquareValue + "\n\n" );
        }
        _nodes.add(node);
        _map.put(token, node);
        updatePosition(node);
        _top++;
    }

    protected void newToken(String token, int freq) {
        Node node = _nodes.get(0);
        _map.remove(node.token);
        _map.put(token, node);
        node.token = token;
        // TODO: 08.06.18 Behzad
        node.updateClassPriors(this.numDocPositive, this.numDocNegative);

        addCount(node, freq);
        updatePosition(node);
    }

    // TODO: 14.04.18 Behzad
    protected void newToken(String token, int c0Freq, int c1Freq) {
        Node node = _nodes.get(0);
        _chiSqaure.remove(node.token);
        // updating Chi-square value
        node.updateClassPriors(this.numDocPositive, this.numDocNegative);
        node.chiSquareValue = calculateChiSqaure(node.classpiors);
        if( node.chiSquareValue > chiSqaureThreshold){
            _chiSqaure.put(token, node);
            System.out.println(node.token + "--**-> Chi-Sqare : " + node.chiSquareValue + "\n\n" );
        }
        _map.remove(node.token);
        _map.put(token, node);
        node.token = token;
        addCount(node, c0Freq, c1Freq);
        updatePosition(node);
    }

    // TODO: 18.06.18 Behzad : Update Position based on Chi-`Square`
    protected void updatePosition(Node node) {
        boolean isDescending = false;

        int offset = node.index + 1;
        while ((offset < _nodes.size()) && (node.getChiSquareValue() > _nodes.get(offset).getChiSquareValue())) {
            offset++;
        }
        offset--;

        //Descend in the list order if count was reduced
        if (offset == node.index) {
            offset = node.index - 1;
            while ((offset >= 0) && (node.count < _nodes.get(offset).getChiSquareValue())) {
                offset--;
                isDescending = true;
            }
            offset++;
        }

        if (offset != node.index) {
            int oldIndex = node.index;

            if (isDescending) {
                _nodes.add(offset, node);
                //showNodes();System.out.println("===");
                _nodes.remove(oldIndex + 1);
            } else {
                _nodes.add(offset + 1, node);
                _nodes.remove(oldIndex);
            }
        }
//        System.out.println("Updating "+node);
        if (_nodes != null) {
            for (int i = 0; i < _nodes.size(); i++) {
                _nodes.get(i).index = i;
            }
        }
        //showNodes();
    }



    /**
     * Print all nodes to System.out, useful for debugging
     *
     */
    public void showNodes() {
        for (Node node : _nodes) {
            if (node != null) {
                System.out.println(node);
            }
        }
    }


    // TODO: 08.06.18 Behzad
    /**
     * Print all chi-squares to System.out, useful for debugging
     *
     */
    public void showChiSquares(){
        for (Node node : _chiSqaure.values()) {
            if ((node != null) && (node.chiSquareValue > chiSqaureThreshold)) {
                System.out.println(node.getToken() + " ---> chi_Sqare: " + node.getChiSquareValue());
                //System.out.println(node.index+ " ," +node.attrIndex+ " ," + node.count + ", "+ node.token);
                //System.out.println("getClass0Count: " + node.getClass0Count());
            }
//            else{
//                System.out.println("Nothing to show!!");
//            }
        }

        System.out.println("==== end of Printing Chi Squares!! from SpaceSaving.java ===");
    }



    protected boolean addCount(Node node, int freq) {
        node.addCount((double) freq, this.numDoc);
        return false;
    }

    // TODO: 14.04.18 Behzad
    protected boolean addCount(Node node, int c0Freq, int c1Freq) {
        node.addCount((double) c0Freq, (double) c1Freq, this.numDoc);
        return false;
    }

    public void addDoc(double docSize) {
        this.numDoc++;
        this.numTerms += docSize;
    }

    // TODO: 04.06.18 Behzad
    public void addDoc(double docSize,int classValue) {
        this.numDoc++;
//        if(classValue == 1 ){
//            numDocPositive++;
//        }
//        else {
//            numDocNegative++;
//        }

        this.numTerms += docSize;
    }

    // TODO: 04.06.18  Behzad

    public void setNumDocNegPos(int classValue) {
        if(classValue == 1 ){
            this.numDocPositive++;
        }
        else {
            this.numDocNegative++;
        }
        //this.numDocNegative = numDocNegative;
    }

    protected Node newNode(String token, int index, int freq) {
        return new Node(token, index, freq);
    }

    // TODO: 14.04.18 Behzad
    protected Node newNode(String token, int index, int c0Freq, int c1Freq, double numDocPos, double numDocNeg) {
        return new Node(token, index, c0Freq, c1Freq, numDocPos, numDocNeg);
    }

    public void remove(String token) {
    }

    // TODO: 03.06.18 Make Node Public to use in other classes
    // static class Node implements Serializable, Comparable<Node> {
    public static class Node implements Serializable, Comparable<Node> {

        private static final long serialVersionUID = 1L;

        int index;

        int attrIndex;

        double count;

        String token;

        // TODO: 14.04.18 behzad Freq_Count for each class
        double class0Count;
        double class1Count;

        // TODO: 04.06.18 Behzad
        double[][] classpiors = new double [2][2];

        // TODO: 04.06.18 Behzad
        double chiSquareValue = 0;


        Node() {
        }

        Node(String token, int index, int freq) {
            this.index = index;
            this.attrIndex = index;
            this.token = token;
            initCount(freq);
        }

        // TODO: 14.04.18 Behzad
        Node(String token, int index, int c0Freq, int c1Freq, double numDocPos, double numDocNeg) {
            this.index = index;
            this.attrIndex = index;
            this.token = token;
            initCount(c0Freq, c1Freq);
            initCount(c0Freq + c1Freq);
            updateClassPriors(numDocPos, numDocNeg);
            this.chiSquareValue = 0;
        }

        protected boolean addCount(double freq, int doc) {
            // We don't use doc, only on SpaceSavingAdwin
            this.count += freq;
            return false;
        }

        // TODO: 14.04.18 Behzad
        protected boolean addCount(double c0Freq, double c1Fereq, int doc) {
            // We don't use doc, only on SpaceSavingAdwin
            this.count += c0Freq + c1Fereq;
            this.class0Count = c0Freq;
            this.class1Count = c1Fereq;
            //updateClassPriors();
            return false;
        }

        protected double getCount() {
            return this.count;
        }

        // TODO: 04.06.18 behzad
        protected double getChiSquareValue() {
            return this.chiSquareValue;
        }

        // TODO: 04.06.18 Behzad
        protected double[][] getClassPriors() {
            return this.classpiors;
        }

        // TODO: 14.04.18  Behzad
        protected double getClass0Count() {
            return this.class0Count;
        }

        // TODO: 14.04.18 Behzad
        protected double getClass1Count() {
            return this.class1Count;
        }


        protected String getToken() {
            return this.token;
        }

        protected void initCount(int freq) {
            this.count = freq;
        }

        // TODO: 14.04.18 Behzad
        protected void initCount(int c0freq, int c1freq) {
            this.class0Count = c0freq;
            this.class1Count = c1freq;
            this.count = class0Count + class1Count;
        }

        // TODO: 03.06.18 Behzad
        /*
         * ____________________________________
         * |_______|__Positive__|__Negative__|
         * |       |            |            |
         * |  TERM |     0      |      0     |
         * |__yes__|____________|____________|
         * |       |            |            |
         * |  TERM |     0      |      0     |
         * |___no__|____________|____________|
         *
         * */

        // TODO: 04.06.18 Behzad
        protected void updateClassPriors(double numDocsPos, double numDocsNeg){
            this.classpiors[1][0] = numDocsPos - this.class1Count;
            this.classpiors[1][1] = numDocsNeg - this.class0Count;
            this.classpiors[0][0] = class1Count;
            this.classpiors[0][1] = class0Count;
        }

        public int compareTo(Node other) {
            if (getCount() < other.getCount()) {
                return -1;
            }
            if (getCount() > other.getCount()) {
                return 1;
            }
            return 0;
        }

        @Override
        public String toString() {
            return "" + index + "," + attrIndex + "," + count + "," + token;
        }

        // TODO: 14.04.18 Behzad
        public String toStringWithClassFreq() {
            return "" + index + "," + attrIndex + "," + count + "," + token + "," + class0Count + "," + class1Count;
        }
    }


    // TODO: 18.06.18 Behzad : Update all chi squares when a new docs is processed
    // TODO: 18.06.18 Behzad : Only in this section should calculate the Chi-Square values!!!! (Other Methods should be edited)
    public void updateAllChiSquares(){
        Iterator<Node> iter = _nodes.iterator();
        Node node = null;
        while (iter.hasNext()) {
            node = iter.next();
            node.updateClassPriors(numDocPositive, numDocNegative);
            node.chiSquareValue = calculateChiSqaure(node.classpiors);
        }
//        while (iter.hasNext()) {
//            node = iter.next();
//            updatePosition(node);
//        }
    }


    //    @Override
    public void getDescription(StringBuilder arg0, int arg1) {
    }

    public double getFreqWord(String word) {
        return getCount(word) / this.numTerms;
    }


    // TODO: 19.07.18

    public Map<String, Node> getMaps() {
        return _map;
    }

    public ArrayList<Node> getNodes() {
        return _nodes;
    }

    public Map<String, Node> getChiSqaures() {
        return _chiSqaure;
    }


}
