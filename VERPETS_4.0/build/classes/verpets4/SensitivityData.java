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
public class SensitivityData {
    
    private final int simulationID;
    private final String alteredEdge;
    private final boolean isSchedulable;
    private final boolean isRoutable;
    private final int completionTime;
    
    public SensitivityData()
    {
        simulationID = -1;
        alteredEdge = null;
        isSchedulable = false;
        isRoutable = false;
        completionTime = 0;
    }
    
    public SensitivityData(int simulationID, String alteredEdge, boolean isSchedulable, boolean isRoutable, int completionTime)
    {
        this.simulationID = simulationID;
        this.alteredEdge = alteredEdge;
        this.isSchedulable = isSchedulable; 
        this.isRoutable = isRoutable;
        this.completionTime = completionTime;
    }

    /**
     * @return the simulationID
     */
    public int getSimulationID() {
        return simulationID;
    }

    /**
     * @return the offEdges
     */
    public String getAlteredEdge() {
        return alteredEdge;
    }

    /**
     * @return the isSchedulable
     */
    public boolean isIsSchedulable() {
        return isSchedulable;
    }

    /**
     * @return the isRoutable
     */
    public boolean isIsRoutable() {
        return isRoutable;
    }

    /**
     * @return the completionTime
     */
    public int getCompletionTime() {
        return completionTime;
    }

    

}
