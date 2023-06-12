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
public class BERPPOD {
    
    private final int id;
    private final int numBooths;
    private final String name;
    private final String address;
    private final String city;
    private final int zipcode;
    private final String edge;
    private final String lane; 
    private final double maxThroughputRate;
    private ArrayList<Integer> listOfGSUs;
    private int populationOfCatchmentArea;
    private int carsNotProcessed; // This is used in the simulation to keep track of the pop.
    private int avgProcTime;
    
    public BERPPOD()
    {
        id = -1;
        numBooths = -1;
        name = "";
        address = "";
        city = "";
        zipcode = -1;
        edge = "";
        lane = "";
        maxThroughputRate = 0;
        listOfGSUs = new ArrayList();
        populationOfCatchmentArea = 0;
        carsNotProcessed = populationOfCatchmentArea;
        avgProcTime = -1;
    }
    
    public BERPPOD(int id, int numBooths, String name, String address, String city, int zipcode, String edge, String lane, int avgProcTime)
    {
        this.id = id;
        this.numBooths = numBooths;
        this.name = name;
        this.address = address;
        this.city = city;
        this.zipcode = zipcode;
        this.edge = edge;
        this.lane = lane;
        this.maxThroughputRate = (double)numBooths/(double)avgProcTime;
        this.listOfGSUs = new ArrayList();
        this.populationOfCatchmentArea = 0;
        carsNotProcessed = populationOfCatchmentArea;
        this.avgProcTime = avgProcTime;
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }
    
    /**
     * @return the numBooths
     */
    public int getNumBooths() {
        return numBooths;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the address
     */
    public String getAddress() {
        return address;
    }

    /**
     * @return the city
     */
    public String getCity() {
        return city;
    }

    /**
     * @return the zipcode
     */
    public int getZipcode() {
        return zipcode;
    }

    /**
     * @return the edge
     */
    public String getEdge() {
        return edge;
    }

    /**
     * @return the lane
     */
    public String getLane() {
        return lane;
    }

    /**
     * @return the maxThroughputRate
     */
    public double getMaxThroughputRate() {
        return maxThroughputRate;
    }
    
    public void addGSU(int berpgsu, int GSUpop)
    {
        getListOfGSUs().add(berpgsu);
        populationOfCatchmentArea += GSUpop;
    }

    /**
     * @return the listOfGSUs
     */
    public ArrayList<Integer> getListOfGSUs() {
        return listOfGSUs;
    }

    /**
     * @return the populationOfCatchmentArea
     */
    public int getPopulationOfCatchmentArea() {
        return populationOfCatchmentArea;
    }

    /**
     * @return the carsNotProcessed
     */
    public int getCarsNotProcessed() {
        return carsNotProcessed;
    }
    
    /**
     * Resets the simulation catchment area pop back to the original total.
     */
    public void resetCarsNotProcessed()
    {
        carsNotProcessed = populationOfCatchmentArea;
    }
    
    /**
     * Decreases the amount of cars that have yet to be processed.
     */
    public void carProcessed()
    {
        carsNotProcessed--;
    }

    /**
     * @return the avgProcTime
     */
    public int getAvgProcTime() {
        return avgProcTime;
    }
    
    
}
