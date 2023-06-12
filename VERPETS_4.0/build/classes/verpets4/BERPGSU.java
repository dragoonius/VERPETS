/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package verpets4;

/**
 *
 * @author JHelsing
 */
public class BERPGSU {
    
    private final int id;
    private final int population;
    private final String edge;
    private final int assignedPODID;
    
    public BERPGSU()
    {
        id = -1;
        population = -1;
        edge = "";
        assignedPODID = -1;
    }
    
    public BERPGSU(int id, int population, String edge, int assignedPODID)
    {
        this.id = id;
        this.population = population;
        this.edge = edge;
        this.assignedPODID = assignedPODID;
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @return the population
     */
    public int getPopulation() {
        return population;
    }

    /**
     * @return the edge
     */
    public String getEdge() {
        return edge;
    }

    /**
     * @return the assignedPODID
     */
    public int getAssignedPODID() {
        return assignedPODID;
    }
    
    
    
}
