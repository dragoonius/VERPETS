/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package verpets4;

import java.util.HashMap;

/**
 *
 * @author JHelsing
 */
public class BERPEdge
{
    
    private final String id;
    private final String name;
    private final int numLanes;
    private final double speed;
    private final double length;
    private int numRoutes;
    private int totalCars;
    private HashMap<Integer,Boolean> PODS;
    private double confidence;
    private double simulationTime;
    private boolean taboo;
    
    public BERPEdge()
    {
        id = "";
        name = "";
        numLanes = 0;
        speed = 0.0;
        length = 0.0;
        numRoutes = 0;
        totalCars = 0;
        PODS = null;  
        confidence = 0;
        simulationTime = 0;
        taboo = false;
    }
    
    public BERPEdge(String id, String name, int numLanes, double speed, double length)
    {
        this.id = id;
        this.name = name;
        this.numLanes = numLanes;
        this.speed = speed;
        this.length = length;
        this.numRoutes = 0;
        this.totalCars = 0;
        this.PODS = new HashMap();
        this.taboo = false;
        confidence = 0;
        simulationTime = 0;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the numLanes
     */
    public int getNumLanes() {
        return numLanes;
    }

    /**
     * @return the speed
     */
    public double getSpeed() {
        return speed;
    }

    /**
     * @return the length
     */
    public double getLength() {
        return length;
    }

    /**
     * @return the numRoutes
     */
    public int getNumRoutes() {
        return numRoutes;
    }
    
    /**
     * Adds the specified number of routes to the total.
     */
    public void addRoute()
    {
        numRoutes++;
    }

    /**
     * @return the totalCars
     */
    public int getTotalCars() {
        return totalCars;
    }
    
    /**
     * Adds more cars to the total for this edge.
     * @param newcars 
     */
    public void addCars(int newcars)
    {
        totalCars += newcars;
    }

    /**
     * @return the PODS
     */
    public HashMap<Integer,Boolean> getPODS() {
        return PODS;
    }
    
    
    
    
    public boolean routesToPOD(int pod)
    {
        if(PODS.containsKey(pod))
        {
            PODS.put(pod, true);
        }
        
        return false;
    }
    
    /**
     * Add the specified pod id to the list of PODS 
     * @param pod 
     */
    public void addPOD(int pod)
    {
        if(!(PODS.containsKey(pod)))
        {
            PODS.put(pod, true);
        }  
    }

    /**
     * @return the confidence
     */
    public double getConfidence() {
        return confidence;
    }

    /**
     * @param confidence the confidence to set
     */
    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    /**
     * @return the simulationTime
     */
    public double getSimulationTime() {
        return simulationTime;
    }

    /**
     * @param simulationTime the simulationTime to set
     */
    public void setSimulationTime(double simulationTime) {
        this.simulationTime = simulationTime;
    }

    /**
     * @return the taboo
     */
    public boolean isTaboo() {
        return taboo;
    }

    /**
     * @param taboo the taboo to set
     */
    public void setTaboo(boolean taboo) {
        this.taboo = taboo;
    }
  
    
    
        
}
