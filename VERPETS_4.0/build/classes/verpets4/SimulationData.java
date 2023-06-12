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
public class SimulationData {
    
    private final int simulationID;
    private final String alteredEdge;
    private final double completionTime;
    private final String completionClass;
    
    public SimulationData()
    {
        simulationID = -1;
        alteredEdge = null;
        completionTime = -1;
        completionClass = "";
    }
    
    public SimulationData(int simulationID, String alteredEdge, double completionTime, double timeLimit, double nearlyExtension)
    {
        this.simulationID = simulationID;
        this.alteredEdge = alteredEdge;
        this.completionTime = completionTime;
        
        if(completionTime <= timeLimit)
        {
            this.completionClass = "F";
        }
        else if(completionTime > timeLimit && completionTime <= (completionTime + nearlyExtension))
        {
            this.completionClass = "N";
        }
        else
        {
            this.completionClass = "I";
        }    
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
     * @return the completionTime
     */
    public double getCompletionTime() {
        return completionTime;
    }

    /**
     * @return the completionClass
     */
    public String getCompletionClass() {
        return completionClass;
    } 

}
