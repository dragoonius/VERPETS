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
public class BERP {
    
    private final HashMap<Integer,BERPPOD> podList;
    private final HashMap<Integer,BERPGSU> gsuList;
    private final HashMap<String,BERPEdge> edgeList;
    private final HashMap<String,String> laneToEdgeList;
    private HashMap<Integer,BERPRoute> routeList;       //Used to store all of the routes, GSUID to route
    private HashMap<String,Integer> baselineTraffic;
    private HashMap<String,Boolean> protectedEdges;     //Used to prevent removal of necessary edges
    private HashMap<String,Boolean> podEdgeList;        //Used to determine when a car has reached it's POD
    private int totalNumberOfCars;
    private HashMap<Integer, Integer> carToPod;
    private HashMap<Integer, Integer> carsPerPod;
    private String planName;
    
    /**
     * The default no argument constructor. Sets all of the variables to be empty;
    */
    public BERP()
    {
        podList = new HashMap();
        gsuList = new HashMap();
        edgeList = new HashMap();
        laneToEdgeList = new HashMap();
        routeList = new HashMap();
        baselineTraffic = new HashMap();
        protectedEdges = new HashMap();
        podEdgeList = new HashMap();
        totalNumberOfCars = 0;
        carToPod = new HashMap();
        carsPerPod = new HashMap();
        planName = "";
    }
    
    /**
     * The four argument constructor.
     * @param podList
     * @param gsuList
     * @param edgeList
     * @param laneToEdgeList
     */
    public BERP(HashMap<Integer,BERPPOD> podList, HashMap<Integer,BERPGSU> gsuList, HashMap<String,BERPEdge> edgeList, HashMap<String,String> laneToEdgeList)
    {
        this.podList = podList;
        this.gsuList = gsuList;
        this.edgeList = edgeList;
        this.laneToEdgeList = laneToEdgeList;
        routeList = new HashMap();
        totalNumberOfCars = 0;
        carToPod = new HashMap();
        carsPerPod = new HashMap();
        protectedEdges = new HashMap();
        podEdgeList = new HashMap();
        
        for(int key : gsuList.keySet())
        {
            totalNumberOfCars += gsuList.get(key).getPopulation();
        }        
    }
   
    /**
     * @return the podList
     */
    public HashMap<Integer,BERPPOD> getPodList() {
        return podList;
    }

    /**
     * @return the gsuList
     */
    public HashMap<Integer,BERPGSU> getGsuList() {
        return gsuList;
    }

    /**
     * @return the edgeList
     */
    public HashMap<String,BERPEdge> getEdgeList() {
        return edgeList;
    }

    /**
     * @return the laneToEdgeList
     */
    public HashMap<String,String> getLaneToEdgeList() {
        return laneToEdgeList;
    }

    /**
     * @return the baselineTraffic
     */
    public HashMap<String,Integer> getBaselineTraffic() {
        return baselineTraffic;
    }

    /**
     * @param baselineTraffic the baselineTraffic to set
     */
    public void setBaselineTraffic(HashMap<String,Integer> baselineTraffic) {
        this.baselineTraffic = baselineTraffic;
    }

    /**
     * @return the totalNumberOfCars
     */
    public int getTotalNumberOfCars() {
        return totalNumberOfCars;
    }
    
    /**
     * @return the carToPod
     */
    public HashMap<Integer, Integer> getCarToPod() {
        return carToPod;
    }
    
    public void addCarToPod(int carID, int podID)
    {
        carToPod.put(carID, podID);
        
        if(getCarsPerPod().containsKey(podID))
        {
            carsPerPod.put(podID, getCarsPerPod().get(podID) + 1);
        }
        else
        {
            carsPerPod.put(podID, 1);
        }
    }

    /**
     * @return the protectedEdges
     */
    public HashMap<String,Boolean> getProtectedEdges() {
        return protectedEdges;
    }
    
    public void addProtectedEdge(String edgeName)
    {
        protectedEdges.put(edgeName, true);
    }
    
    public void addRoute(BERPRoute route)
    {
        routeList.put(route.getGSUID(), route);
    }
    
    public void addPODEdge(String edgeName)
    {
        getPodEdgeList().put(edgeName, true);
    }

    /**
     * @return the routeList
     */
    public HashMap<Integer,BERPRoute> getRouteList() {
        return routeList;
    }

    /**
     * @return the podEdgeList
     */
    public HashMap<String,Boolean> getPodEdgeList() {
        return podEdgeList;
    }

    /**
     * @return the carsPerPod
     */
    public HashMap<Integer, Integer> getCarsPerPod() {
        return carsPerPod;
    }
    
    
    
    
    
}
