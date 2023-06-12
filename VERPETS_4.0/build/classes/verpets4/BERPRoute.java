/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package verpets4;

import java.util.ArrayList;

/**
 *
 * @author JHelsing
 */
public class BERPRoute {
    
    private int GSUID;
    private int numEdges;
    private double totalLength;
    private double totalTime;
    private double toPodTime;
    private ArrayList<String> edgeList;
    
    
    public BERPRoute()
    {
        GSUID = -1;
        numEdges = -1;
        totalLength = -1;
        totalTime = -1;
        toPodTime = -1;
        edgeList = null;
    }
    
    public BERPRoute(int GSUID)
    {
        this.GSUID = GSUID;
        numEdges = 0;
        totalLength = 0;
        totalTime = 0;
        toPodTime = 0;
        edgeList = new ArrayList();
    }
    
    public void addEdge(BERPEdge edge)
    {
        numEdges++;
        totalLength += edge.getLength();
        totalTime += edge.getLength()/edge.getSpeed();
        edgeList.add(edge.getId());
    }
   
    /**
     * @return the GSUID
     */
    public int getGSUID() {
        return GSUID;
    } 

    /**
     * @return the numEdges
     */
    public int getNumEdges() {
        return numEdges;
    }

    /**
     * @return the totalLength
     */
    public double getTotalLength() {
        return totalLength;
    }

    /**
     * @return the totalTime
     */
    public double getTotalTime() {
        return totalTime;
    }

    /**
     * @return the edgeList
     */
    public ArrayList<String> getEdgeList() {
        return edgeList;
    }

    
    
}
