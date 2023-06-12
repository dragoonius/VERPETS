/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package verpets4;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static java.lang.Thread.sleep;
import java.util.Map;


/**
 *
 * @author JHelsing
 */
public class SimulationManager {
    private String addlFileName;
    private String tripFileName;
    private String netFileName;
    private String routeFileName;
    private String sumocfgFileName;
    private BERP currentBERP; // Handle on the current BERP
    private ArrayList<String> podLocation;
    private HashMap<Integer,Integer> tripIDtoPop;
    //private ArrayList<ArrayList<String>> sumoOutput;
    private int currentSimID;
    private boolean oneHourCutoff;

    
    private final int CEIL = 0, FLOOR = 1, CLOSEST = 2, NONE = 3;
    
    public SimulationManager()
    {
        currentSimID = 0;
    }
    
    /**
     * This provides the SimulationManager with a handle on the current BERP being analyzed.
     * @param current This is the current BERP being analyzed
     */
    public void assignBERP(BERP current)
    {
        currentBERP = current;
    }
    
    /**
     * Generates the .net file based on an OSM file.
     * Potentially includes weakly connected edges.
     * @param osmFilename
     * @param filename 
     */
    public void netFileCreator(String osmFilename, String filename)
    {
        System.out.println("Creating .net file from OSM file");
        
        File outputFile = new File(filename);

        //The list of edge types that we want to keep
        String keptEdges = "highway.motorway,highway.trunk,highway.primary,highway.secondary,highway.tertiary,highway.motorway_link,highway.trunk_link,highway.primary_link,highway.secondary_link,highway.tertiary_link";

        //Building the process to call NETCOVERT using flag, argument pairs
        ProcessBuilder builder = new ProcessBuilder("netconvert",
                "--osm", osmFilename,
                "-o", filename,       
                "--keep-edges.by-type", keptEdges,
                "--output.street-names", "true",
                "--no-warnings","true",
                "--remove-edges.isolated","true"
        );
        builder.redirectErrorStream(true);
        
        
        /*
        *To have this run correctly, we need to capture any potential warnings and output.
        *Thus, an IOThreadHandler was created to act as a separate thread and capture the output from running NETCONVERT
        */
        try {
            //Starting the process
            Process proc = builder.start();
            
            IOThreadHandler outputHandler = new IOThreadHandler(proc.getInputStream());
            
            //Starting the handler
            outputHandler.start();
            
            proc.waitFor();
            
            //If the exit value is not 0, then something went wrong and we need to throw an error
            if(proc.exitValue() != 0)
            {
                //TODO Add in something here
            }
            
            System.out.println("OUTPUT: " + outputHandler.getOutput());
        } catch (IOException ex) {
            Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.out.println(".net file created");
        
    }
    
    /**
     * Generates the .net file based on an OSM file.
     * Potentially includes weakly connected edges.
     * @param osmFilename
     * @param filename 
     * @param isolatedEdges 
     * @param selectedEdges 
     */
    public void netFileCreatorSelectedEdges(String osmFilename, String filename, ArrayList<ArrayList<String>> isolatedEdges)
    {
        System.out.println("Creating .net file from OSM file and selected edges");
        
        File outputFile = new File(filename);

        //The list of edge types that we want to keep
        String keptEdges = "highway.motorway,highway.trunk,highway.primary,highway.secondary,highway.tertiary,highway.motorway_link,highway.trunk_link,highway.primary_link,highway.secondary_link,highway.tertiary_link";

       //The list of isolatd edges that we want to remove
        String removedEdges = "";
        int largestArray = -1;
        int largestArrayPos = -1;

        
        //First we find the position of the largest array, which must be the one excluding the isolated edges
        for(int i = 0; i < isolatedEdges.size(); i++)
        {
            if(isolatedEdges.get(i).size() > largestArray)
            {                
                largestArray = isolatedEdges.get(i).size();
                largestArrayPos = i;
            }
        }
        
        //Next we add all of the isolated edges into the removedEdges string
        for(int i = 0; i < isolatedEdges.size(); i++)
        {
            if(i != largestArrayPos)
            {
                for(int j = 0; j < isolatedEdges.get(i).size(); j++)
                {
                    removedEdges = removedEdges.concat(isolatedEdges.get(i).get(j) + ",");
                }
            }
        }

        //Then we remove the final ,
        removedEdges = removedEdges.substring(0, removedEdges.length()-1);
        
        //Building the process to call NETCOVERT using flag, argument pairs
        ProcessBuilder builder = new ProcessBuilder("netconvert",
                "--osm", osmFilename,
                "-o", filename,       
                "--keep-edges.by-type", keptEdges,
                "--output.street-names", "true",
                "--no-warnings","true",
                "--remove-edges.isolated","true",
                "--remove-edges.explicit",removedEdges
        );
        builder.redirectErrorStream(true);
        
        
        /*
        *To have this run correctly, we need to capture any potential warnings and output.
        *Thus, an IOThreadHandler was created to act as a separate thread and capture the output from running NETCONVERT
        */
        try {
            //Starting the process
            Process proc = builder.start();
            
            IOThreadHandler outputHandler = new IOThreadHandler(proc.getInputStream());
            
            //Starting the handler
            outputHandler.start();
            
            proc.waitFor();
            
            //If the exit value is not 0, then something went wrong and we need to throw an error
            if(proc.exitValue() != 0)
            {
                //TODO Add in something here
            }
            
            System.out.println("OUTPUT: " + outputHandler.getOutput());
        } catch (IOException ex) {
            Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.out.println("new .net file created");
        
    }
 
    /**
     * Generates the .net file based on the original OSM file and removes any isolated edges identified by Tarjan's.
     * @param osmFilename
     * @param filename
     * @param isolatedEdges 
     */
    public void netFileCreatorIsolatedEdges(String osmFilename, String filename, ArrayList<ArrayList<String>> isolatedEdges)
    {
        System.out.println("Recreating .net file without isolated edges");
        
        File outputFile = new File(filename);

        //The list of edge types that we want to keep
        String keptEdges = "highway.motorway,highway.trunk,highway.primary,highway.secondary,highway.tertiary,highway.motorway_link,highway.trunk_link,highway.primary_link,highway.secondary_link,highway.tertiary_link";

        //The list of isolatd edges that we want to remove
        String removedEdges = "";
        int largestArray = -1;
        int largestArrayPos = -1;

        
        //First we find the position of the largest array, which must be the one excluding the isolated edges
        for(int i = 0; i < isolatedEdges.size(); i++)
        {
            if(isolatedEdges.get(i).size() > largestArray)
            {                
                largestArray = isolatedEdges.get(i).size();
                largestArrayPos = i;
            }
        }
        
        //Next we add all of the isolated edges into the removedEdges string
        for(int i = 0; i < isolatedEdges.size(); i++)
        {
            if(i != largestArrayPos)
            {
                for(int j = 0; j < isolatedEdges.get(i).size(); j++)
                {
                    removedEdges = removedEdges.concat(isolatedEdges.get(i).get(j) + ",");
                }
            }
        }

        //Then we remove the final ,
        removedEdges = removedEdges.substring(0, removedEdges.length()-1);
        
        //System.out.println("REMOVED EDGES " + removedEdges);
        
        //Building the process to call NETCOVERT using flag, argument pairs
        ProcessBuilder builder = new ProcessBuilder("netconvert",
                "--osm", osmFilename,
                "-o", filename,       
                "--keep-edges.by-type", keptEdges,
                "--output.street-names", "true",
                "--no-warnings","true",
                "--remove-edges.isolated","true",
                "--remove-edges.explicit",removedEdges
        );
        builder.redirectErrorStream(true);
        
        
        /*
        *To have this run correctly, we need to capture any potential warnings and output.
        *Thus, an IOThreadHandler was created to act as a separate thread and capture the output from running NETCONVERT
        */
        try {
            //Starting the process
            Process proc = builder.start();
            
            IOThreadHandler outputHandler = new IOThreadHandler(proc.getInputStream());
            
            //Starting the handler
            outputHandler.start();
            
            proc.waitFor();
            
            //If the exit value is not 0, then something went wrong and we need to throw an error
            if(proc.exitValue() != 0)
            {
                //TODO Add in something here
            }
            
            System.out.println("OUTPUT: " + outputHandler.getOutput());
        } catch (IOException ex) {
            Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.out.println(".net file updated");
    }
    
    /**
     * Creates the trip.xml file that is used to generate routes for the population for the validation analysis.
     * Assumes everyone leaves at timestep 0
     * @param filename 
     * @param roundingStrategy 
     * @param agentScaling 
     * @param tripsArray 
     * @param vehicleReleaseInterval 
     * @return  
     */
    public HashMap<Integer,String> perCatchmentTripFileCreator(String filename, int roundingStrategy, int agentScaling, HashMap<Integer,String> tripsArray, int vehicleReleaseInterval)
    {
        System.out.println("Creating trip files");
        
        HashMap<Integer,String> carIDtoArrivalEdge = new HashMap();
        tripIDtoPop = new HashMap();
                
        DecimalFormat numberFormat = new DecimalFormat("#.00");
        
        HashMap<Integer, BERPGSU> tempGSUList = currentBERP.getGsuList();
        HashMap<Integer, BERPPOD> tempPODList = currentBERP.getPodList();
        HashMap<Integer, Integer> PODtoActualPop = new HashMap();
        HashMap<Integer, Integer> PODtoGSUTotalPop = new HashMap();
        HashMap<Integer, Integer> GSUtoActualPop = new HashMap();
        
        
        //Need to keep this up here because the carID is an index in a hash
        int tripID = 1;
        
                
        File outputFile;
        FileWriter writer;
        
        for(int currentPOD: tempPODList.keySet())
        {        
            //System.out.println("POD " + currentPOD + " POPULATION " + tempPODList.get(currentPOD).getPopulationOfCatchmentArea());
            int currentTimeStep = 0;            
            
            PODtoActualPop.put(currentPOD, 0);
            PODtoGSUTotalPop.put(currentPOD, 0);
            
            try {
                tripsArray.put(currentPOD,filename + "_pod_" + currentPOD + ".trips.xml");
                
                outputFile= new File(tripsArray.get(currentPOD));

                //If the file does not exist, create a new one.
                if(!outputFile.exists()) 
                {
                    try {
                        outputFile.createNewFile();
                    } catch (IOException ex) {
                     Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                writer = new FileWriter(outputFile.getAbsoluteFile());
                BufferedWriter buffwriter = new BufferedWriter(writer);

                buffwriter.write("<?xml version=\"1.0\"?>\n\n\n");

                buffwriter.write("<trips>\n");

                HashMap<Integer,Integer> GSUtoPop = new HashMap();

                for(int currentKey : tempPODList.get(currentPOD).getListOfGSUs())
                {
                    GSUtoPop.put(currentKey, tempGSUList.get(currentKey).getPopulation());
                    PODtoGSUTotalPop.put(currentPOD, PODtoGSUTotalPop.get(currentPOD) + tempGSUList.get(currentKey).getPopulation());
                    GSUtoActualPop.put(currentKey, 0);
                }                

                boolean stillReleasing = true;

                while(stillReleasing)
                {
                    stillReleasing = false;

                    for(int currentKey : GSUtoPop.keySet())
                    {
                        if(GSUtoPop.get(currentKey) > 0)
                        {
                            int toRelease = 0;

                            if(GSUtoPop.get(currentKey) - agentScaling >= 0 )
                            {
                                if(currentTimeStep == 0)
                                {
                                    buffwriter.write("\t<trip id=\"" + tripID +"\" depart=\"0.00\" from=\"" + tempGSUList.get(currentKey).getEdge() + "\" to=\"" + tempGSUList.get(currentKey).getEdge() + "\" via=\"" + tempPODList.get(tempGSUList.get(currentKey).getAssignedPODID()).getEdge() + "\"/>\n");
                                }
                                else
                                {
                                    buffwriter.write("\t<trip id=\"" + tripID +"\" depart=\""+ numberFormat.format(currentTimeStep) +"\" from=\"" + tempGSUList.get(currentKey).getEdge() + "\" to=\"" + tempGSUList.get(currentKey).getEdge() + "\" via=\"" + tempPODList.get(tempGSUList.get(currentKey).getAssignedPODID()).getEdge() + "\"/>\n");
                                }                                
                                carIDtoArrivalEdge.put(tripID,tempPODList.get(tempGSUList.get(currentKey).getAssignedPODID()).getEdge());
                                currentBERP.addCarToPod(tripID, tempGSUList.get(currentKey).getAssignedPODID());
                                tripIDtoPop.put(tripID, agentScaling);
                                tripID++;
                                
                               // System.out.println("GSU ID\t" + currentKey + "\tPOP\t" + GSUtoPop.get(currentKey));
                                GSUtoPop.put(currentKey,GSUtoPop.get(currentKey) - agentScaling);
                                PODtoActualPop.put(currentPOD, PODtoActualPop.get(currentPOD) + agentScaling);
                                GSUtoActualPop.put(currentKey, GSUtoActualPop.get(currentKey) + agentScaling);
                            }
                            else
                            {
                                switch (roundingStrategy) {
                                case CEIL:
                                    if(currentTimeStep == 0)
                                    {
                                        buffwriter.write("\t<trip id=\"" + tripID +"\" depart=\"0.00\" from=\"" + tempGSUList.get(currentKey).getEdge() + "\" to=\"" + tempGSUList.get(currentKey).getEdge() + "\" via=\"" + tempPODList.get(tempGSUList.get(currentKey).getAssignedPODID()).getEdge() + "\"/>\n");
                                    }
                                    else
                                    {
                                        buffwriter.write("\t<trip id=\"" + tripID +"\" depart=\""+ numberFormat.format(currentTimeStep) +"\" from=\"" + tempGSUList.get(currentKey).getEdge() + "\" to=\"" + tempGSUList.get(currentKey).getEdge() + "\" via=\"" + tempPODList.get(tempGSUList.get(currentKey).getAssignedPODID()).getEdge() + "\"/>\n");
                                    } 
                                    carIDtoArrivalEdge.put(tripID,tempPODList.get(tempGSUList.get(currentKey).getAssignedPODID()).getEdge());
                                    currentBERP.addCarToPod(tripID, tempGSUList.get(currentKey).getAssignedPODID());
                                    tripIDtoPop.put(tripID, agentScaling);
                                    PODtoActualPop.put(currentPOD, PODtoActualPop.get(currentPOD) + agentScaling);
                                    GSUtoActualPop.put(currentKey, GSUtoActualPop.get(currentKey) + agentScaling);                                    
                                    tripID++;

                                    break;
                                case FLOOR:

                                    break;
                                case CLOSEST:
                                    if(GSUtoPop.get(currentKey)/(double)agentScaling >= 0.5)
                                    {
                                        if(currentTimeStep == 0)
                                        {
                                            buffwriter.write("\t<trip id=\"" + tripID +"\" depart=\"0.00\" from=\"" + tempGSUList.get(currentKey).getEdge() + "\" to=\"" + tempGSUList.get(currentKey).getEdge() + "\" via=\"" + tempPODList.get(tempGSUList.get(currentKey).getAssignedPODID()).getEdge() + "\"/>\n");
                                        }
                                        else
                                        {
                                            buffwriter.write("\t<trip id=\"" + tripID +"\" depart=\""+ numberFormat.format(currentTimeStep) +"\" from=\"" + tempGSUList.get(currentKey).getEdge() + "\" to=\"" + tempGSUList.get(currentKey).getEdge() + "\" via=\"" + tempPODList.get(tempGSUList.get(currentKey).getAssignedPODID()).getEdge() + "\"/>\n");
                                        } 
                                        carIDtoArrivalEdge.put(tripID,tempPODList.get(tempGSUList.get(currentKey).getAssignedPODID()).getEdge());
                                        currentBERP.addCarToPod(tripID, tempGSUList.get(currentKey).getAssignedPODID());
                                        tripIDtoPop.put(tripID, agentScaling);
                                        PODtoActualPop.put(currentPOD, PODtoActualPop.get(currentPOD) + agentScaling);
                                        GSUtoActualPop.put(currentKey, GSUtoActualPop.get(currentKey) + agentScaling);
                                        tripID++;
                                    }

                                    break;
                                case NONE:
                                    if(currentTimeStep == 0)
                                    {
                                        buffwriter.write("\t<trip id=\"" + tripID +"\" depart=\"0.00\" from=\"" + tempGSUList.get(currentKey).getEdge() + "\" to=\"" + tempGSUList.get(currentKey).getEdge() + "\" via=\"" + tempPODList.get(tempGSUList.get(currentKey).getAssignedPODID()).getEdge() + "\"/>\n");
                                    }
                                    else
                                    {
                                        buffwriter.write("\t<trip id=\"" + tripID +"\" depart=\""+ numberFormat.format(currentTimeStep) +"\" from=\"" + tempGSUList.get(currentKey).getEdge() + "\" to=\"" + tempGSUList.get(currentKey).getEdge() + "\" via=\"" + tempPODList.get(tempGSUList.get(currentKey).getAssignedPODID()).getEdge() + "\"/>\n");
                                    } 
                                    carIDtoArrivalEdge.put(tripID,tempPODList.get(tempGSUList.get(currentKey).getAssignedPODID()).getEdge());
                                    currentBERP.addCarToPod(tripID, tempGSUList.get(currentKey).getAssignedPODID());
                                    tripIDtoPop.put(tripID, agentScaling - GSUtoPop.get(currentKey) );
                                    //System.out.println("GSU ID " + currentKey + " PARTIAL AGENT " + GSUtoPop.get(currentKey) +  " Actual Pop " + tempGSUList.get(currentKey).getPopulation());
                                    PODtoActualPop.put(currentPOD, PODtoActualPop.get(currentPOD) + GSUtoPop.get(currentKey));
                                    GSUtoActualPop.put(currentKey, GSUtoActualPop.get(currentKey) + GSUtoPop.get(currentKey));
                                    tripID++;
                                    
                                    break;
                                }                      
                               // System.out.println("GSU ID\t" + currentKey + "\tPOP\t" + GSUtoPop.get(currentKey));
                                GSUtoPop.put(currentKey,0);
                                //System.out.println("GSU ID\t" + currentKey + "\tPOP\t" + GSUtoPop.get(currentKey));
                            }
                            stillReleasing = true;
                        }
                    }                    
                    currentTimeStep += vehicleReleaseInterval;
                }

                buffwriter.write("</trips>\n");

                buffwriter.flush();

                buffwriter.close();
            } catch (IOException ex) {
                Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        for(int currentKey : PODtoActualPop.keySet())
        {
            System.out.println("POD " + currentKey + " Actual Pop " + tempPODList.get(currentKey).getPopulationOfCatchmentArea() + " Counted Pop " + PODtoGSUTotalPop.get(currentKey) + " Generated Pop " + PODtoActualPop.get(currentKey));
        }  
        /*
        for(int currentKey : tripIDtoPop.keySet())
        {
            System.out.println("TRIP ID " + currentKey + " POP " + tripIDtoPop.get(currentKey));
        }
        
        for(int currentKey : GSUtoActualPop.keySet())
        {
            System.out.println("GSU ID " + currentKey + " Actual Pop " + tempGSUList.get(currentKey).getPopulation() + " Counted Pop " + GSUtoActualPop.get(currentKey));
        }
        */
        System.out.println("Trip files created");
        
        return carIDtoArrivalEdge;
    }
    
    /**
     * Creates the trip.xml file that is used to generate routes for the population for the validation analysis.
     * Assumes everyone leaves at timestep 0
     * @param filename 
     * @param roundingStrategy 
     * @param agentScaling 
     * @param vehicleReleaseInterval 
     * @return  
     */
    public HashMap<Integer,String> globalTripFileCreator(String filename, int roundingStrategy, int agentScaling, int vehicleReleaseInterval)
    {
        System.out.println("Creating trip file");
        
        HashMap<Integer,String> carIDtoArrivalEdge = new HashMap();
        tripIDtoPop = new HashMap();
                
        DecimalFormat numberFormat = new DecimalFormat("#.00");
        
        HashMap<Integer, BERPGSU> tempGSUList = currentBERP.getGsuList();
        HashMap<Integer, BERPPOD> tempPODList = currentBERP.getPodList();
        HashMap<Integer, Integer> GSUtoActualPop = new HashMap();
        
        
        //Need to keep this up here because the carID is an index in a hash
        int tripID = 1;
        int currentTimeStep = 0;
                
        File outputFile = new File(filename + ".trips.xml");
        FileWriter writer;
        
        try 
        {
            if(!outputFile.exists()) 
            {
                try {
                    outputFile.createNewFile();
                } catch (IOException ex) {
                 Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            writer = new FileWriter(outputFile.getAbsoluteFile());
            BufferedWriter buffwriter = new BufferedWriter(writer);

            buffwriter.write("<?xml version=\"1.0\"?>\n\n\n");

            buffwriter.write("<trips>\n");
        
            HashMap<Integer,Integer> GSUtoPop = new HashMap();

            for(int currentKey : tempGSUList.keySet())
            {
                GSUtoPop.put(currentKey, tempGSUList.get(currentKey).getPopulation());                
                GSUtoActualPop.put(currentKey, 0);
            }
            
            boolean stillReleasing = true;

            while(stillReleasing)
            {
                stillReleasing = false;             
                
                for(int currentKey : GSUtoPop.keySet())
                {
                    if(GSUtoPop.get(currentKey) > 0)
                    {
                        if(GSUtoPop.get(currentKey) - agentScaling >= 0 )
                        {
                            if(currentTimeStep == 0)
                            {
                                buffwriter.write("\t<trip id=\"" + tripID +"\" depart=\"0.00\" from=\"" + tempGSUList.get(currentKey).getEdge() + "\" to=\"" + tempGSUList.get(currentKey).getEdge() + "\" via=\"" + tempPODList.get(tempGSUList.get(currentKey).getAssignedPODID()).getEdge() + "\"/>\n");
                            }
                            else
                            {
                                buffwriter.write("\t<trip id=\"" + tripID +"\" depart=\""+ numberFormat.format(currentTimeStep) +"\" from=\"" + tempGSUList.get(currentKey).getEdge() + "\" to=\"" + tempGSUList.get(currentKey).getEdge() + "\" via=\"" + tempPODList.get(tempGSUList.get(currentKey).getAssignedPODID()).getEdge() + "\"/>\n");
                            }
                            
                            carIDtoArrivalEdge.put(tripID,tempPODList.get(tempGSUList.get(currentKey).getAssignedPODID()).getEdge());
                            currentBERP.addCarToPod(tripID, tempGSUList.get(currentKey).getAssignedPODID());
                            tripIDtoPop.put(tripID, agentScaling);
                            tripID++;

                           // System.out.println("GSU ID\t" + currentKey + "\tPOP\t" + GSUtoPop.get(currentKey));
                            GSUtoPop.put(currentKey,GSUtoPop.get(currentKey) - agentScaling);
                            GSUtoActualPop.put(currentKey, GSUtoActualPop.get(currentKey) + agentScaling);
                        }
                        else
                        {
                            switch (roundingStrategy) {
                            case CEIL:                                
                                if(currentTimeStep == 0)
                                {
                                    buffwriter.write("\t<trip id=\"" + tripID +"\" depart=\"0.00\" from=\"" + tempGSUList.get(currentKey).getEdge() + "\" to=\"" + tempGSUList.get(currentKey).getEdge() + "\" via=\"" + tempPODList.get(tempGSUList.get(currentKey).getAssignedPODID()).getEdge() + "\"/>\n");
                                }
                                else
                                {
                                    buffwriter.write("\t<trip id=\"" + tripID +"\" depart=\""+ numberFormat.format(currentTimeStep) +"\" from=\"" + tempGSUList.get(currentKey).getEdge() + "\" to=\"" + tempGSUList.get(currentKey).getEdge() + "\" via=\"" + tempPODList.get(tempGSUList.get(currentKey).getAssignedPODID()).getEdge() + "\"/>\n");
                                }
                                carIDtoArrivalEdge.put(tripID,tempPODList.get(tempGSUList.get(currentKey).getAssignedPODID()).getEdge());
                                currentBERP.addCarToPod(tripID, tempGSUList.get(currentKey).getAssignedPODID());
                                tripIDtoPop.put(tripID, agentScaling);
                                GSUtoActualPop.put(currentKey, GSUtoActualPop.get(currentKey) + agentScaling);                                    
                                tripID++;

                                break;
                            case FLOOR:

                                break;
                            case CLOSEST:
                                if(GSUtoPop.get(currentKey)/(double)agentScaling >= 0.5)
                                {
                                    if(currentTimeStep == 0)
                                    {
                                        buffwriter.write("\t<trip id=\"" + tripID +"\" depart=\"0.00\" from=\"" + tempGSUList.get(currentKey).getEdge() + "\" to=\"" + tempGSUList.get(currentKey).getEdge() + "\" via=\"" + tempPODList.get(tempGSUList.get(currentKey).getAssignedPODID()).getEdge() + "\"/>\n");
                                    }
                                    else
                                    {
                                        buffwriter.write("\t<trip id=\"" + tripID +"\" depart=\""+ numberFormat.format(currentTimeStep) +"\" from=\"" + tempGSUList.get(currentKey).getEdge() + "\" to=\"" + tempGSUList.get(currentKey).getEdge() + "\" via=\"" + tempPODList.get(tempGSUList.get(currentKey).getAssignedPODID()).getEdge() + "\"/>\n");
                                    }
                                    carIDtoArrivalEdge.put(tripID,tempPODList.get(tempGSUList.get(currentKey).getAssignedPODID()).getEdge());
                                    currentBERP.addCarToPod(tripID, tempGSUList.get(currentKey).getAssignedPODID());
                                    tripIDtoPop.put(tripID, agentScaling);
                                    GSUtoActualPop.put(currentKey, GSUtoActualPop.get(currentKey) + agentScaling);
                                    tripID++;
                                }

                                break;
                            case NONE:
                                if(currentTimeStep == 0)
                                {
                                    buffwriter.write("\t<trip id=\"" + tripID +"\" depart=\"0.00\" from=\"" + tempGSUList.get(currentKey).getEdge() + "\" to=\"" + tempGSUList.get(currentKey).getEdge() + "\" via=\"" + tempPODList.get(tempGSUList.get(currentKey).getAssignedPODID()).getEdge() + "\"/>\n");
                                }
                                else
                                {
                                    buffwriter.write("\t<trip id=\"" + tripID +"\" depart=\""+ numberFormat.format(currentTimeStep) +"\" from=\"" + tempGSUList.get(currentKey).getEdge() + "\" to=\"" + tempGSUList.get(currentKey).getEdge() + "\" via=\"" + tempPODList.get(tempGSUList.get(currentKey).getAssignedPODID()).getEdge() + "\"/>\n");
                                }
                                carIDtoArrivalEdge.put(tripID,tempPODList.get(tempGSUList.get(currentKey).getAssignedPODID()).getEdge());
                                currentBERP.addCarToPod(tripID, tempGSUList.get(currentKey).getAssignedPODID());
                                tripIDtoPop.put(tripID, agentScaling - GSUtoPop.get(currentKey) );
                                //System.out.println("GSU ID " + currentKey + " PARTIAL AGENT " + GSUtoPop.get(currentKey) +  " Actual Pop " + tempGSUList.get(currentKey).getPopulation());
                                GSUtoActualPop.put(currentKey, GSUtoActualPop.get(currentKey) + GSUtoPop.get(currentKey));
                                tripID++;

                                break;
                            }                      
                            // System.out.println("GSU ID\t" + currentKey + "\tPOP\t" + GSUtoPop.get(currentKey));
                            GSUtoPop.put(currentKey,0);
                            //System.out.println("GSU ID\t" + currentKey + "\tPOP\t" + GSUtoPop.get(currentKey));
                        }
                        stillReleasing = true;
                    }
                }
                
                currentTimeStep += vehicleReleaseInterval;
            }
            
            buffwriter.write("</trips>\n");

            buffwriter.flush();

            buffwriter.close();
        } catch (IOException ex) {
            Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        /*
        for(int currentKey : tempPODList.keySet())
        {
            System.out.println("POD " + currentKey + " Actual Pop " + tempPODList.get(currentKey).getPopulationOfCatchmentArea());
        }  
        
        for(int currentKey : tripIDtoPop.keySet())
        {
            System.out.println("TRIP ID " + currentKey + " POP " + tripIDtoPop.get(currentKey));
        }
        
        for(int currentKey : GSUtoActualPop.keySet())
        {
            System.out.println("GSU ID " + currentKey + " Actual Pop " + tempGSUList.get(currentKey).getPopulation() + " Counted Pop " + GSUtoActualPop.get(currentKey));
        }
        */
        System.out.println("Trip file created");
        
        return carIDtoArrivalEdge;
    }

    public HashMap<Integer,String> rrCongestTripFileCreator(String filename, int processingTime, int planTimeLimit, HashMap<Integer,String> tripsArray)
    {
        System.out.println("Creating round robin congestion trip files");
        
        HashMap<Integer,String> carIDtoArrivalEdge = new HashMap();
                       
        DecimalFormat numberFormat = new DecimalFormat("#.00");
        
        HashMap<Integer, BERPGSU> tempGSUList = currentBERP.getGsuList();
        HashMap<Integer, BERPPOD> tempPODList = currentBERP.getPodList();
        //Array that holds all possible time slots for a plan
        byte[] schedule;
        
        //
        HashMap<Integer, Integer> GSUtoTimeslot;
        ArrayList<tripCarIDDepartTime> carIDtoTimeslot;
        
        HashMap<Integer, BERPRoute> tempRouteList = currentBERP.getRouteList();
        
        //Need to keep this up here because the carID is an index in a hash
        int tripID;
                
        File outputFile;
        FileWriter writer;
        

        
        for(int currentPOD: tempPODList.keySet())
        {   
            //Reinitilize everything
            carIDtoTimeslot = new ArrayList();
            GSUtoTimeslot = new HashMap();
            tripID = 1;
            schedule = new byte[planTimeLimit*3600];
            
            
            try {
                tripsArray.put(currentPOD,filename + "_pod_" + currentPOD + ".trips.xml");
                
                outputFile= new File(tripsArray.get(currentPOD));

                //If the file does not exist, create a new one.
                if(!outputFile.exists()) 
                {
                    try {
                        outputFile.createNewFile();
                    } catch (IOException ex) {
                     Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                writer = new FileWriter(outputFile.getAbsoluteFile());
                BufferedWriter buffwriter = new BufferedWriter(writer);

                buffwriter.write("<?xml version=\"1.0\"?>\n\n\n");

                buffwriter.write("<trips>\n");

                HashMap<Integer,Integer> GSUtoPop = new HashMap();
                ArrayList<Integer> GSUIDList = new ArrayList();
                
                for(int currentKey : tempPODList.get(currentPOD).getListOfGSUs())
                {
                    GSUtoPop.put(currentKey, tempGSUList.get(currentKey).getPopulation());
                    GSUtoTimeslot.put(currentKey, 0);
                    GSUIDList.add(currentKey);
                }                
    
                //Do we still have cars to release
                boolean stillReleasing = true;
                
                //Helps iterate through the ID list
                int currentID = 0;
                
                System.out.println("POD " + currentPOD + " POPULATION " + tempPODList.get(currentPOD).getPopulationOfCatchmentArea() + " NUM GSUS " + GSUIDList.size() + " NUM BOOTHS " + tempPODList.get(currentPOD).getNumBooths());
                                
                while(stillReleasing)
                {
                    //Reset everything
                    stillReleasing = false;
                    
                    while(GSUIDList.size() > 0)
                    {
                        if(GSUtoPop.get(GSUIDList.get(currentID)) != 0)
                        {
                            
                            //The plan went over time limit and thus failed.
                            if(GSUtoTimeslot.get(GSUIDList.get(currentID)) + tempRouteList.get(GSUIDList.get(currentID)).getTotalTime() + processingTime > planTimeLimit*3600)
                            {
                                System.out.println("GSU TIME " + GSUtoTimeslot.get(GSUIDList.get(currentID)) + " TOTAL TIME " + tempRouteList.get(GSUIDList.get(currentID)).getTotalTime() + " PROC TIME " + processingTime + " LIMIT " + (planTimeLimit*3600));
                                
                                System.out.println("BFAILED AT " + tripID);
                                carIDtoArrivalEdge.put(-1, "FAIL");
                                return carIDtoArrivalEdge;
                            }
                            
                            boolean willNotFit = true;
                            
                            //Test if the current car will be able to be processed  
                            while(willNotFit)
                            {
                                willNotFit = false;
                                
                                //Check each slot in the processing schedule
                                for(int i = GSUtoTimeslot.get(GSUIDList.get(currentID)) + (int)tempRouteList.get(GSUIDList.get(currentID)).getTotalTime(); i < schedule.length; i++)
                                {
                                    if(schedule[i] >= tempPODList.get(currentPOD).getNumBooths())
                                    {
                                        willNotFit = true;
                                                                                
                                        GSUtoTimeslot.put(GSUIDList.get(currentID), GSUtoTimeslot.get(GSUIDList.get(currentID)) + 1);
                                        
                                        break;
                                    }
                                }
                            }
                            
                            
                            
                            //The plan went over time limit and thus failed.
                            if(GSUtoTimeslot.get(GSUIDList.get(currentID)) + tempRouteList.get(GSUIDList.get(currentID)).getTotalTime() + processingTime > planTimeLimit*3600)
                            {
                               System.out.println("GSU TIME " + GSUtoTimeslot.get(GSUIDList.get(currentID)) + " TOTAL TIME " + tempRouteList.get(GSUIDList.get(currentID)).getTotalTime() + " PROC TIME " + processingTime + " LIMIT " + (planTimeLimit*3600));
                                
                                
                                for(int i = 0; i < schedule.length; i++)
                                {
                                    buffwriter.write(i + "\t" + schedule[i] + "\n");
                                }
                                                   
                                buffwriter.flush();
                                
                                System.out.println("AFAILED AT " + tripID);
                                carIDtoArrivalEdge.put(-1, "FAIL");
                                return carIDtoArrivalEdge;
                            }
                            //Now we create a trip object to write later
                            carIDtoTimeslot.add(new tripCarIDDepartTime(tripID,GSUIDList.get(currentID),GSUtoTimeslot.get(GSUIDList.get(currentID))));
                            
                            //Now we can go through the schedule and increment all used slots
                            for(int i = GSUtoTimeslot.get(GSUIDList.get(currentID)) + (int)tempRouteList.get(GSUIDList.get(currentID)).getTotalTime(); i <= GSUtoTimeslot.get(GSUIDList.get(currentID)) + (int)tempRouteList.get(GSUIDList.get(currentID)).getTotalTime() + processingTime; i++)
                            {
                                schedule[i]++;
                            }
                            
                            carIDtoArrivalEdge.put(tripID,tempPODList.get(tempGSUList.get(GSUIDList.get(currentID)).getAssignedPODID()).getEdge());
                                                        
                            //Decrement the pop to go for that GSU by 1
                            GSUtoPop.put(GSUIDList.get(currentID), GSUtoPop.get(GSUIDList.get(currentID))-1);
                            //Increment the pop by 1
                            tripID++;
                            //Can potentially still release cars
                            stillReleasing = true;
                        }
                        else
                        {
                            GSUIDList.remove(currentID);
                            currentID--;
                        }
                            
                        //We wrap around to keep going through the GSUIDList
                        if(currentID < GSUIDList.size() - 1)
                        {
                            currentID++;
                        }
                        else
                        {
                            currentID = 0;
                        }

                    }

                }
                
                //Now we have to sort everything and then write it
                Collections.sort(carIDtoTimeslot, new SortByDepartTime());
                
                for(int i = 0; i < carIDtoTimeslot.size(); i++)
                {                 
                    if(carIDtoTimeslot.get(i).departTime == 0)
                    {
                        buffwriter.write("\t<trip id=\"" + carIDtoTimeslot.get(i).getID() +"\" depart=\"0.00\" from=\"" + tempGSUList.get(carIDtoTimeslot.get(i).getGSUID()).getEdge() + "\" to=\"" + tempGSUList.get(carIDtoTimeslot.get(i).getGSUID()).getEdge() + "\" via=\"" + tempPODList.get(tempGSUList.get(carIDtoTimeslot.get(i).getGSUID()).getAssignedPODID()).getEdge() + "\"/>\n");
                    }
                    else
                    {
                        buffwriter.write("\t<trip id=\"" + carIDtoTimeslot.get(i).getID() +"\" depart=\"" + numberFormat.format(carIDtoTimeslot.get(i).departTime) + "\" from=\"" + tempGSUList.get(carIDtoTimeslot.get(i).getGSUID()).getEdge() + "\" to=\"" + tempGSUList.get(carIDtoTimeslot.get(i).getGSUID()).getEdge() + "\" via=\"" + tempPODList.get(tempGSUList.get(carIDtoTimeslot.get(i).getGSUID()).getAssignedPODID()).getEdge() + "\"/>\n");
                    }
                }

                buffwriter.write("</trips>\n");

                buffwriter.flush();

                buffwriter.close();
            } catch (IOException ex) {
                Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        System.out.println("Congrestion trip files created");
        
        return carIDtoArrivalEdge;
    }  
    
    public void rrCongestionAnalysis(int processingTime, int planTimeLimit, HashMap<Integer,Boolean> routeSuccess, HashMap<Integer,Boolean> scheduleSuccess, HashMap<Integer,Integer> endTime)
    {
        System.out.println("Analyzing schedulability for each catchment area");
        
        HashMap<Integer, BERPGSU> tempGSUList = currentBERP.getGsuList();
        HashMap<Integer, BERPPOD> tempPODList = currentBERP.getPodList();
        
        //Array that holds all possible time slots for a plan
        byte[] schedule;
        
        //
        HashMap<Integer, Integer> GSUtoTimeslot;
        ArrayList<tripCarIDDepartTime> carIDtoTimeslot;
        
        HashMap<Integer, BERPRoute> tempRouteList = currentBERP.getRouteList();
        
        //Need to keep this up here because the carID is an index in a hash
        int tripID;
        
        for(Map.Entry<Integer,Boolean> entry : routeSuccess.entrySet())
        {
            //If we could route it, then we can test for congestion
            if(entry.getValue())
            {
                //Reinitilize everything
                carIDtoTimeslot = new ArrayList();
                GSUtoTimeslot = new HashMap();
                tripID = 1;
                schedule = new byte[planTimeLimit*3600];
                
                HashMap<Integer,Integer> GSUtoPop = new HashMap();
                ArrayList<Integer> GSUIDList = new ArrayList();
                
                for(int currentKey : tempPODList.get(entry.getKey()).getListOfGSUs())
                {
                    GSUtoPop.put(currentKey, tempGSUList.get(currentKey).getPopulation());
                    GSUtoTimeslot.put(currentKey, 0);
                    GSUIDList.add(currentKey);
                }                
    
                //Do we still have cars to release
                boolean stillReleasing = true;
                
                //Helps iterate through the ID list
                int currentID = 0;
                
                System.out.println("POD " + entry.getKey() + " POPULATION " + tempPODList.get(entry.getKey()).getPopulationOfCatchmentArea() + " NUM GSUS " + GSUIDList.size() + " NUM BOOTHS " + tempPODList.get(entry.getKey()).getNumBooths());
                                
                while(stillReleasing)
                {
                    //Reset everything
                    stillReleasing = false;
                    
                    while(GSUIDList.size() > 0)
                    {
                        if(GSUtoPop.get(GSUIDList.get(currentID)) != 0)
                        {                            
                            //The plan went over time limit and thus failed.
                            if(GSUtoTimeslot.get(GSUIDList.get(currentID)) + tempRouteList.get(GSUIDList.get(currentID)).getTotalTime() + processingTime > planTimeLimit*3600)
                            {
                                System.out.println("GSU TIME " + GSUtoTimeslot.get(GSUIDList.get(currentID)) + " TOTAL TIME " + tempRouteList.get(GSUIDList.get(currentID)).getTotalTime() + " PROC TIME " + processingTime + " LIMIT " + (planTimeLimit*3600));
                                
                                System.out.println("BFAILED AT " + tripID);
                                scheduleSuccess.put(entry.getKey(), false);
                                stillReleasing = false;
                                break;
                            }
                            
                            boolean willNotFit = true;
                            
                            //Test if the current car will be able to be processed  
                            while(willNotFit)
                            {
                                willNotFit = false;
                                
                                //Check each slot in the processing schedule
                                for(int i = GSUtoTimeslot.get(GSUIDList.get(currentID)) + (int)tempRouteList.get(GSUIDList.get(currentID)).getTotalTime(); i < schedule.length; i++)
                                {
                                    if(schedule[i] >= tempPODList.get(entry.getKey()).getNumBooths())
                                    {
                                        willNotFit = true;
                                                                                
                                        GSUtoTimeslot.put(GSUIDList.get(currentID), GSUtoTimeslot.get(GSUIDList.get(currentID)) + 1);
                                        
                                        break;
                                    }
                                }
                            }                         
                            
                            //The plan went over time limit and thus failed.
                            if(GSUtoTimeslot.get(GSUIDList.get(currentID)) + tempRouteList.get(GSUIDList.get(currentID)).getTotalTime() + processingTime > planTimeLimit*3600)
                            {   
                                System.out.println("GSU TIME " + GSUtoTimeslot.get(GSUIDList.get(currentID)) + " TOTAL TIME " + tempRouteList.get(GSUIDList.get(currentID)).getTotalTime() + " PROC TIME " + processingTime + " LIMIT " + (planTimeLimit*3600));
             
                                System.out.println("AFAILED AT " + tripID);
                                stillReleasing = false;
                                scheduleSuccess.put(entry.getKey(), false);
                                break;
                            }
                            //Now we create a trip object to write later
                            carIDtoTimeslot.add(new tripCarIDDepartTime(tripID,GSUIDList.get(currentID),GSUtoTimeslot.get(GSUIDList.get(currentID))));
                            
                            //Now we can go through the schedule and increment all used slots
                            for(int i = GSUtoTimeslot.get(GSUIDList.get(currentID)) + (int)tempRouteList.get(GSUIDList.get(currentID)).getTotalTime(); i <= GSUtoTimeslot.get(GSUIDList.get(currentID)) + (int)tempRouteList.get(GSUIDList.get(currentID)).getTotalTime() + processingTime; i++)
                            {
                                schedule[i]++;
                            }                            
                             
                            //Decrement the pop to go for that GSU by 1
                            GSUtoPop.put(GSUIDList.get(currentID), GSUtoPop.get(GSUIDList.get(currentID))-1);
                            //Increment the pop by 1
                            tripID++;
                            //Can potentially still release cars
                            stillReleasing = true;
                        }
                        else
                        {
                            GSUIDList.remove(currentID);
                            currentID--;
                        }
                            
                        //We wrap around to keep going through the GSUIDList
                        if(currentID < GSUIDList.size() - 1)
                        {
                            currentID++;
                        }
                        else
                        {
                            currentID = 0;
                        }
                    }
                }
                
                //If it makes it out of here and has not failed, then it has succeeded
                if(!scheduleSuccess.containsKey(entry.getKey()))
                {
                    scheduleSuccess.put(entry.getKey(), true);
                }
                
                
                
                File schedout = new File("pod_"+ entry.getKey() +"_schedout.txt");
                
                FileWriter sw = null;
                BufferedWriter swb = null;

                if(!schedout.exists()) 
                {
                    try 
                    {
                        schedout.createNewFile();
                    } catch (IOException ex) {
                        Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                try 
                {
                    sw = new FileWriter(schedout.getAbsoluteFile());
                    swb = new BufferedWriter(sw);
                    
                    for(int i = 0; i < schedule.length; i++)
                    {
                        swb.write(i + "\t" + schedule[i] + "\n");
                    }    
                    swb.flush();
                    swb.close();
                } catch (IOException ex) {
                    Logger.getLogger(MasterController.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                
                //And we also need to determine when the last car left.
                int end = 0;
                for(int i = 0; i < schedule.length; i++)
                {
                    if(schedule[i] != 0)
                    {
                        end = i;
                    }
                }
                
                endTime.put(entry.getKey(), end);
                
            }            
        }
    }

    /**
     * Generates the .addl.xml file for running the simulation.
     * 
     * @param filename The name of the file that will be written to. If it does not exist, one will be created.
     * @param addlsArray
     */
    public void perCatchmentAddlFileCreator(String filename, HashMap<Integer,String> addlsArray)
    {
        System.out.println("Creating additional files");
        
        HashMap<Integer, BERPPOD> tempPODList = currentBERP.getPodList();
        
        File outputFile;
        File flowOutputFile;
        
        FileWriter writer;

        for(int currentKey : tempPODList.keySet())
        {
            try 
            {
                addlsArray.put(currentKey,filename + "_pod_" + currentKey + ".addl.xml");

                System.out.println("POD ID " + currentKey + " ARRAY SIZE " + (addlsArray.size()-1));
                
                outputFile= new File(addlsArray.get(currentKey));

                //If the file does not exist, create a new one.
                if(!outputFile.exists()) 
                {
                    try {
                        outputFile.createNewFile();
                    } catch (IOException ex) {
                     Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                writer = new FileWriter(outputFile.getAbsoluteFile());
                BufferedWriter buffwriter = new BufferedWriter(writer);

                buffwriter.write("<additional xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://sumo.dlr.de/xsd/additional_file.xsd\">");
                buffwriter.newLine();
                buffwriter.newLine();

                int numberOfSpaces = (2 * tempPODList.get(currentKey).getNumBooths()) + 10;                
                //numberOfSpaces = tempPODList.get(currentKey).getNumBooths() + 10;

                buffwriter.write("<parkingArea id=\"" + tempPODList.get(currentKey).getId() + "\" lane=\"" + tempPODList.get(currentKey).getLane() + "\" startPos=\"10\" endPos=\"" + numberOfSpaces + "\" roadsideCapacity=\"" + tempPODList.get(currentKey).getNumBooths() +"\" />");
                buffwriter.newLine();


                buffwriter.newLine();
                //Use - to tell the program to write to standard out
                buffwriter.write("<vTypeProbe id=\"1\" type=\"Car\" freq=\"1\" file=\"-\"/>");
                buffwriter.newLine();
                buffwriter.newLine();
                buffwriter.write("</additional>");

                buffwriter.flush();

                buffwriter.close();
            } catch (IOException ex) {
                Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
          
        System.out.println("Additional files created");
    }
    
    /**
     * Generates the .addl.xml file for running the simulation.
     * 
     * @param filename The name of the file that will be written to. If it does not exist, one will be created.
     * @param addlsArray
     */
    public void globalAddlFileCreator(String filename)
    {
        System.out.println("Creating additional file");
        
        HashMap<Integer, BERPPOD> tempPODList = currentBERP.getPodList();
        
        File outputFile = new File(filename + ".addl.xml");
        
        FileWriter writer;

        try 
        {               

            //If the file does not exist, create a new one.
            if(!outputFile.exists()) 
            {
                try {
                    outputFile.createNewFile();
                } catch (IOException ex) {
                 Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            writer = new FileWriter(outputFile.getAbsoluteFile());
            BufferedWriter buffwriter = new BufferedWriter(writer);

            buffwriter.write("<additional xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://sumo.dlr.de/xsd/additional_file.xsd\">");
            buffwriter.newLine();
            buffwriter.newLine();

            for(int currentKey : tempPODList.keySet())
            {          
                int numberOfSpaces = (2 * tempPODList.get(currentKey).getNumBooths()) + 10;                

                buffwriter.write("<parkingArea id=\"" + tempPODList.get(currentKey).getId() + "\" lane=\"" + tempPODList.get(currentKey).getLane() + "\" startPos=\"10\" endPos=\"" + numberOfSpaces + "\" roadsideCapacity=\"" + tempPODList.get(currentKey).getNumBooths() +"\" />");
                buffwriter.newLine();
            }

            buffwriter.newLine();
            //Use - to tell the program to write to standard out
            buffwriter.write("<vTypeProbe id=\"1\" type=\"Car\" freq=\"1\" file=\"-\"/>");
            buffwriter.newLine();
            buffwriter.newLine();
            buffwriter.write("</additional>");

            buffwriter.flush();

            buffwriter.close();
        } catch (IOException ex) {
            Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        System.out.println("Additional file created");    
    }
    
    /**
    * Generates the .addl.xml file for the traffic flow simulation.
    * 
    * @param filename The name of the file that will be written to. If it does not exist, one will be created.
    */
    public void flowaddlFileCreator(String filename)
    {
        System.out.println("Creating additional files");
        
        HashMap<Integer, BERPPOD> tempPODList = currentBERP.getPodList();
        
        File outputFile;
        
        FileWriter writer;

        try 
        {              
            outputFile= new File(filename);

            //If the file does not exist, create a new one.
            if(!outputFile.exists()) 
            {
                try {
                    outputFile.createNewFile();
                } catch (IOException ex) {
                 Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            writer = new FileWriter(outputFile.getAbsoluteFile());
            BufferedWriter buffwriter = new BufferedWriter(writer);

            buffwriter.write("<additional xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://sumo.dlr.de/xsd/additional_file.xsd\">");
            buffwriter.newLine();
            buffwriter.newLine();

            for(int currentKey : tempPODList.keySet())
            {
                
                int numberOfSpaces = (2 * tempPODList.get(currentKey).getNumBooths()) + 10;                
                //numberOfSpaces = tempPODList.get(currentKey).getNumBooths() + 10;

                buffwriter.write("<parkingArea id=\"" + tempPODList.get(currentKey).getId() + "\" lane=\"" + tempPODList.get(currentKey).getLane() + "\" startPos=\"10\" endPos=\"" + numberOfSpaces + "\" roadsideCapacity=\"" + tempPODList.get(currentKey).getNumBooths() +"\" />");
                buffwriter.newLine();
            }
            //Use - to tell the program to write to standard out
            buffwriter.write("<vTypeProbe id=\"1\" type=\"Car\" freq=\"1\" file=\"-\"/>");            
            
            buffwriter.newLine();
            buffwriter.newLine();
            buffwriter.write("</additional>");

            buffwriter.flush();

            buffwriter.close();
        } catch (IOException ex) {
            Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
        }          
    }
    
    public boolean perCatchmentRouteFileCreator(String routeFile, String netFile, HashMap<Integer,String> addlsArray, HashMap<Integer,String> tripsArray, HashMap<Integer,String> routeXMLS, HashMap<Integer,String> refRouteXMLS, int batch, int popToAgent, HashMap<Integer,Integer> PODtoProcTime)
    {
        System.out.println("Calling DUAROUTER to create route files");
        
        int batchSize = batch;
             
        HashMap<Integer,ProcessBuilder> builderList = new HashMap();
        HashMap<Integer,Process> procList = new HashMap();
        HashMap<Integer,RouteReader> readerList = new HashMap();
        
        //Building the process to call NETCOVERT using flag, argument pairs
        //duarouter -n testing.net.xml -d testing.addl.xml -t testing.trips.xml -o testing.rout.xml
        for(int currentKey : addlsArray.keySet())
        {
            routeXMLS.put(currentKey,routeFile + "_pod_" + currentKey + ".route.xml");
            refRouteXMLS.put(currentKey,routeFile + "_pod_" + currentKey + "_refined.route.xml");
            builderList.put(currentKey,new ProcessBuilder("duarouter",
                "-n", netFile,
                "-d", addlsArray.get(currentKey),       
                "-t", tripsArray.get(currentKey),
                "-o", routeXMLS.get(currentKey)  
            ));
            
            builderList.get(currentKey).redirectErrorStream(true);
        }
        
        try {
            //Starting the process in batches
            int startingPOD = 1;
            int endingPOD;
            
            //If we have fewer PODs than the batch size, just run all of the PODs
            if(builderList.size() <= batchSize)
            {
                endingPOD = builderList.size();
            }
            else
            {
                endingPOD = batchSize;
            }            
            
            while(startingPOD <= builderList.size())
            {
                System.out.println("Batch " + startingPOD + " through " + endingPOD);
                
                for(int i = startingPOD; i <= endingPOD; i++)
                {          
                    System.out.println("Routing POD: " + i);

                    procList.put(i,builderList.get(i).start());
                    readerList.put(i,new RouteReader(procList.get(i).getInputStream()));

                    //Starting the handler
                    readerList.get(i).start();
                }        


                //Wait for all the processes to complete
                for(int i = startingPOD; i <= endingPOD; i++)
                {
                    procList.get(i).waitFor();
                    System.out.println("Finished routing POD: " + i);
                }
                
                for(int i = startingPOD; i <= endingPOD; i++)
                {
                    if(readerList.get(i).hasProblem())
                    {
                        return true;
                    }

                    System.out.println("OUTPUT: " + readerList.get(i).getOutput());
                }
                
                startingPOD += batchSize;
                
                if(endingPOD + batchSize <= builderList.size())
                {
                    endingPOD += batchSize;
                }
                else
                {
                    endingPOD = builderList.size();
                }
                
            }
            
            /*
            //Starting the processes
            for(int currentKey : builderList.keySet())
            {
                procList.put(currentKey,builderList.get(currentKey).start());
                readerList.put(currentKey,new RouteReader(procList.get(currentKey).getInputStream()));
                
                //Starting the handler
                readerList.get(currentKey).start();
            }        

            
            //Wait for all the processes to complete
            for(int currentKey : procList.keySet())
            {
                procList.get(currentKey).waitFor();
            }

            //Check if any of the PODs failed to have a route
            for(int currentKey : readerList.keySet())
            {
                if(readerList.get(currentKey).hasProblem())
                {
                    return true;
                }

                System.out.println("OUTPUT: " + readerList.get(currentKey).getOutput());
            }
            */

        } catch (IOException ex) {
            Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        System.out.println("Refining route files");
        
        //Now we need to add in the car vType and set each vehicle to that vtype
        Pattern header = Pattern.compile("<routes.*>");
        Pattern vehicleInfo = Pattern.compile("(<vehicle .* depart=\".*\")");
        Pattern vehicleID = Pattern.compile("<vehicle id=\"(.*)\" depart");
        Pattern vehicleClose = Pattern.compile("</vehicle>");
        Matcher match = null;        
        BufferedReader br = null; 
        BufferedWriter bw = null;
        int tripID = 0;
                
        for(int currentKey : refRouteXMLS.keySet())
        {
            System.out.println(routeXMLS.get(currentKey));

            try
            {
                //First we initialize the buffered reader
                br = new BufferedReader(new FileReader(new File(routeXMLS.get(currentKey))));
                //Then we initialize the buffered writer
                bw = new BufferedWriter(new FileWriter(refRouteXMLS.get(currentKey)));

                String line = "";
                while((line = br.readLine()) != null)
                {
                    if(header.matcher(line).find())
                    {
                        bw.write(line);
                        bw.write("\n");
                        bw.write("\n");
                        bw.write("\t<vType  id=\"Car\" lcStrategic=\"100.0\" lcSpeedGain=\"100.0\" lcKeepRight=\"0.0\"/>");
                        //bw.write("\t<vType  id=\"Car\"/>");
                        //bw.write("\t<vType accel=\"1.0\" decel=\"5.0\" id=\"Car\" length=\"2.0\" maxSpeed=\"9.0\" sigma=\"0.0\" />");
                        bw.write("\n");
                        bw.write("\n");
                    }
                    else if(vehicleInfo.matcher(line).find())
                    {
                        match = vehicleInfo.matcher(line);
                        match.find();

                        bw.write("\t" + match.group(1) + " type=\"Car\">");
                        bw.write("\n");                       
                        
                        match = vehicleID.matcher(line);
                        match.find();
                        
                        tripID = Integer.parseInt(match.group(1));
                        
                        //System.out.println("TESTING MATCHER " + tripID + " " + tripIDtoPop.get(tripID));

                    }
                    else if(vehicleClose.matcher(line).find())
                    {                       
                        //bw.write("\t\t<stop parkingArea=\"" + currentBERP.getPodList().get(currentBERP.getCarToPod().get(currentCar)).getId() + "\" duration=\"" + stopDuration + "\" />");
                        bw.write("\t\t<stop parkingArea=\"" + currentKey + "\" duration=\"" + PODtoProcTime.get(currentBERP.getCarToPod().get(tripID))*(tripIDtoPop.get(tripID)) + "\" />");
                        bw.write("\n");
                        bw.write(line);
                        bw.write("\n");
                    }
                    else
                    {
                        bw.write(line);
                        bw.write("\n");
                    }
                }

                bw.flush();
            }
            catch (IOException ex) {
                Logger.getLogger(DataManager.class.getName()).log(Level.SEVERE, null, ex);
            }
            finally {
                try {
                    br.close();                
                    bw.close();
                } catch (IOException ex) {
                    Logger.getLogger(DataManager.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        }
        
        System.out.println("Route files created");
        
        return false;
    }
    
    public boolean globalRouteFileCreator(String routeFile, String netFile, HashMap<Integer,Integer> PODtoProcTime)
    {
        System.out.println("Calling DUAROUTER to create route file");
        
        //Building the process to call NETCOVERT using flag, argument pairs
        //duarouter -n testing.net.xml -d testing.addl.xml -t testing.trips.xml -o testing.rout.xml
        ProcessBuilder procBuild;
        Process proc;
        RouteReader reader;
                
        procBuild = new ProcessBuilder("duarouter",
                "-n", netFile,
                "-d", routeFile + ".addl.xml",       
                "-t", routeFile + ".trips.xml",
                "-o", routeFile + ".route.xml");
        procBuild.redirectErrorStream(true);
        
        try {
            proc = procBuild.start();
            reader = new RouteReader(proc.getInputStream());
            
            reader.start();
            
            proc.waitFor();
            
            if(reader.hasProblem())
            {
                return true;
            }
            
            System.out.println("OUTPUT: " + reader.getOutput());
            
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
        }        
        
        System.out.println("Refining route file");
        
        //Now we need to add in the car vType and set each vehicle to that vtype
        Pattern header = Pattern.compile("<routes.*>");
        Pattern vehicleInfo = Pattern.compile("(<vehicle .* depart=\".*\")");
        Pattern vehicleID = Pattern.compile("<vehicle id=\"(.*)\" depart");
        Pattern vehicleClose = Pattern.compile("</vehicle>");
        Matcher match = null;        
        BufferedReader br = null; 
        BufferedWriter bw = null;
        int tripID = 0;

        try
        {
            //First we initialize the buffered reader
            br = new BufferedReader(new FileReader(new File(routeFile + ".route.xml")));
            //Then we initialize the buffered writer
            bw = new BufferedWriter(new FileWriter(routeFile + "_refined.route.xml"));

            String line = "";
            while((line = br.readLine()) != null)
            {
                if(header.matcher(line).find())
                {
                    bw.write(line);
                    bw.write("\n");
                    bw.write("\n");
                    bw.write("\t<vType  id=\"Car\" lcStrategic=\"100.0\" lcSpeedGain=\"100.0\" lcKeepRight=\"0.0\"/>");
                    //bw.write("\t<vType  id=\"Car\"/>");
                    bw.write("\n");
                    bw.write("\n");
                }
                else if(vehicleInfo.matcher(line).find())
                {
                    match = vehicleInfo.matcher(line);
                    match.find();

                    bw.write("\t" + match.group(1) + " type=\"Car\">");
                    bw.write("\n");                       

                    match = vehicleID.matcher(line);
                    match.find();

                    tripID = Integer.parseInt(match.group(1));

                    //System.out.println("TESTING MATCHER " + tripID + " " + tripIDtoPop.get(tripID));

                }
                else if(vehicleClose.matcher(line).find())
                {                       
                    bw.write("\t\t<stop parkingArea=\"" + currentBERP.getPodList().get(currentBERP.getCarToPod().get(tripID)).getId() + "\" duration=\"" + PODtoProcTime.get(currentBERP.getCarToPod().get(tripID))*(tripIDtoPop.get(tripID)) + "\" />");
                    bw.write("\n");
                    bw.write(line);
                    bw.write("\n");
                }
                else
                {
                    bw.write(line);
                    bw.write("\n");
                }
            }

            bw.flush();
        }
        catch (IOException ex) {
            Logger.getLogger(DataManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        finally 
        {
            try 
            {
                br.close();                
                bw.close();
            } catch (IOException ex) {
                Logger.getLogger(DataManager.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        
        
        System.out.println("Route file created");
        
        return false;
    }
    
    /**
     * Creates the .sumocfg file for running the simulation.
     * @param filename The name of the file that will be written to. If it does not exist, one will be created.
     * @param netFile The name of the .net.xml file.
     * @param refRouteXMLS
     * @param addlsArray
     * @param routeFile The name of the .rout.xml file.
     * @param sumoCFGXMLS
     * @param addFile The name of the .addl.xml file.
     */
    public void perCatchmentSumoconfigFileCreator(String filename, String netFile, HashMap<Integer,String> refRouteXMLS, HashMap<Integer,String> addlsArray, HashMap<Integer,String> sumoCFGXMLS)
    {
        System.out.println("Creating SUMO Config file");
        File outputFile;
        
        //for(int i = 0; i < addlsArray.size(); i++)
            //System.out.println("ADDL ARRAY " + (i+1) + " FOR POD " + addlsArray.get(i));
        
        for(int currentKey : refRouteXMLS.keySet())
        {
            sumoCFGXMLS.put(currentKey,filename+ "_pod_" + currentKey + ".sumocfg"); 
            outputFile = new File(sumoCFGXMLS.get(currentKey));
            
            //If the file does not exist, create a new one.
            if(!outputFile.exists()) 
            {
                try {
                    outputFile.createNewFile();
                } catch (IOException ex) {
                 Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            FileWriter writer;
            try {
                writer = new FileWriter(outputFile.getAbsoluteFile());
                BufferedWriter buffwriter = new BufferedWriter(writer);

                buffwriter.write("<configuration>");
                buffwriter.newLine();
                buffwriter.write("\t<input>");
                buffwriter.newLine();
                buffwriter.write("\t\t<net-file value=\"" + netFile + "\"/>");
                buffwriter.newLine();
                buffwriter.write("\t\t<route-files value=\"" + refRouteXMLS.get(currentKey) + "\"/>");
                buffwriter.newLine();
                buffwriter.write("\t\t<additional-files value=\"" + addlsArray.get(currentKey) + "\"/>");
                buffwriter.newLine();
                buffwriter.write("\t\t<time-to-teleport value=\"-1\"/>");
                buffwriter.newLine();
                buffwriter.write("\t</input>");
                buffwriter.newLine();
                buffwriter.write("</configuration>");

                buffwriter.flush();

                buffwriter.close();
            } catch (IOException ex) {
                Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        System.out.println("SUMO Config file created");
    }
        
    /**
     * Creates the .sumocfg file for running the simulation.
     * @param filename The name of the file that will be written to. If it does not exist, one will be created.
     * @param netFile The name of the .net.xml file.
     */
    public void globalSumoconfigFileCreator(String filename, String netFile)
    {
        System.out.println("Creating SUMO Config file");
        
        File outputFile = new File(filename + ".sumocfg");
           
        //If the file does not exist, create a new one.
        if(!outputFile.exists()) 
        {
            try {
                outputFile.createNewFile();
            } catch (IOException ex) {
             Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        FileWriter writer;
        try {
            writer = new FileWriter(outputFile.getAbsoluteFile());
            BufferedWriter buffwriter = new BufferedWriter(writer);

            buffwriter.write("<configuration>");
            buffwriter.newLine();
            buffwriter.write("\t<input>");
            buffwriter.newLine();
            buffwriter.write("\t\t<net-file value=\"" + netFile + "\"/>");
            buffwriter.newLine();
            buffwriter.write("\t\t<route-files value=\"" + filename + "_refined.route.xml\"/>");
            buffwriter.newLine();
            buffwriter.write("\t\t<additional-files value=\"" + filename + ".addl.xml\"/>");
            buffwriter.newLine();
            buffwriter.write("\t\t<time-to-teleport value=\"-1\"/>");
            buffwriter.newLine();
            buffwriter.write("\t</input>");
            buffwriter.newLine();
            buffwriter.write("</configuration>");

            buffwriter.flush();

            buffwriter.close();
        } catch (IOException ex) {
            Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        System.out.println("SUMO Config file created");
    }
    
    public SimulationData runPerCatchmentValidationSimulation(HashMap<Integer,String> sumoCFGXMLS, HashMap<Integer,Integer> PODtoProcTime, HashMap<Integer,String> carIDtoArrivalEdge, int batch)
    {
        System.out.println("Running simulation");
        
        double longestPODCompletionTime = 0;
        int longestPODID = 1;
        int batchSize = batch;
                
        //Store the arrival edge to the associated POD for processing
        HashMap<String,BERPPOD> arrivalEdgeToPOD = new HashMap();
        
        HashMap<Integer,ProcessBuilder> builderList = new HashMap();
        HashMap<Integer,Process> procList = new HashMap();
        HashMap<Integer,PerCatchmentSimOutputHandler> outputHandlerList = new HashMap();
        HashMap<Integer,Long> podIDtoTotalTime = new HashMap();
        
        for(int key : currentBERP.getPodList().keySet())
        {
            //Fill the hash with the PODS and then reset all of the cars to be processed
            arrivalEdgeToPOD.put(currentBERP.getPodList().get(key).getEdge(), currentBERP.getPodList().get(key));
            currentBERP.getPodList().get(key).resetCarsNotProcessed();
        }       
        
        //Now build the process
        for(int currentPOD: sumoCFGXMLS.keySet())
        {
            builderList.put(currentPOD,new ProcessBuilder("sumo",
                "-c", sumoCFGXMLS.get(currentPOD)
            ));
            
            builderList.get(currentPOD).redirectErrorStream(true);
        }

        
        try {
            //Starting the process in batches
            int startingPOD = 1;
            int endingPOD;
            
            //If we have fewer PODs than the batch size, just run all of the PODs
            if(builderList.size() <= batchSize)
            {
                endingPOD = builderList.size();
            }
            else
            {
                endingPOD = batchSize;
            }
            
            //Run batches until startingPOD >= the total number of PODs
            while(startingPOD <= builderList.size())
            {
                System.out.println("Batch " + startingPOD + " through " + endingPOD);
                
                for(int i = startingPOD; i <= endingPOD; i++)
                {
                    System.out.println("Starting POD: " + i);               
                    
                    procList.put(i,builderList.get(i).start());
                    outputHandlerList.put(i,new PerCatchmentSimOutputHandler(procList.get(i).getInputStream(), procList.get(i), i, currentBERP.getPodList().get(i).getName(), currentBERP.getPodList().get(i).getLane(), currentBERP.getCarsPerPod().get(i), oneHourCutoff, currentBERP, PODtoProcTime.get(i)));

                    //Starting the handler
                    outputHandlerList.get(i).start();
                }        


                //Wait for all the processes to complete
                for(int i = startingPOD; i <= endingPOD; i++)
                {
                    procList.get(i).waitFor();
                    System.out.println("Finishing POD: " + i);
                }
                
                startingPOD += batchSize;
                
                if(endingPOD + batchSize <= builderList.size())
                {
                    endingPOD += batchSize;
                }
                else
                {
                    endingPOD = builderList.size();
                }
                
            }
            //System.out.println ("Total num cars " + currentBERP.getTotalNumberOfCars() + " total num booths " + totalNumberBooths + " avg proc time " + averageProcTime);
            
            //sumoOutput = outputHandler.getOutputArray();
            
            //outputHandler.printArray();
            
            //Estimate general completion time and find the longest POD
            for(int currentPOD: outputHandlerList.keySet())
            {
               double checkLongest = outputHandlerList.get(currentPOD).getFinalTime();
               System.out.println("POD " + currentPOD + " : " + currentBERP.getPodList().get(currentPOD).getName() + " taking " + (checkLongest/3600) + " hours.");
               
               //double checkLongest = outputHandlerList.get(i).estimateSlowestPODEndTime();
               
               if(checkLongest > longestPODCompletionTime)
               {
                   longestPODCompletionTime = checkLongest;
                   longestPODID = currentPOD;
               }
            }
            
  
            System.out.println("Total Plan Estimated Runtime: " + (longestPODCompletionTime/3600) + " hours.");
            System.out.println("Longest POD " + longestPODID + " : " + currentBERP.getPodList().get(longestPODID).getName() + " taking " + (longestPODCompletionTime/3600) + " hours.");
            
            //System.out.println("OUTPUT: " + outputHandler.getOutput());
        } catch (IOException ex) {
            Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        System.out.println("Finished baseline simulation");
        
        return new SimulationData(currentSimID++, "none", longestPODCompletionTime, 129600, 7200);
    }
        
    public SimulationData runGlobalValidationSimulation(String filename, HashMap<Integer,String> carIDtoArrivalEdge)
    {
        System.out.println("Running simulation");
        
        double longestPODCompletionTime = 0;
        int longestPODID = 1;
                
        HashMap<Integer,Long> podIDtoTotalTime = new HashMap();
        
        ProcessBuilder procBuild;
        Process proc;
        GlobalSimOutputHandler handler = null;
 
        System.out.println("FILENAME " + filename + ".sumocfg");
                
        procBuild = new ProcessBuilder("sumo",
                "-c", filename + ".sumocfg");
        procBuild.redirectErrorStream(true);
   
        try {
            proc = procBuild.start();
            
            handler = new GlobalSimOutputHandler(proc.getInputStream(), proc, currentBERP);
            
            handler.start();
            
            proc.waitFor();
            
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
        }    
        
          
            //System.out.println ("Total num cars " + currentBERP.getTotalNumberOfCars() + " total num booths " + totalNumberBooths + " avg proc time " + averageProcTime);
                      
        //Estimate general completion time and find the longest POD
        for(int currentPOD : handler.getCompletionTimes().keySet())
        {
           double checkLongest = handler.getCompletionTimes().get(currentPOD);
           System.out.println("POD " + currentPOD + " : " + currentBERP.getPodList().get(currentPOD).getName() + " taking " + (checkLongest/3600) + " hours.");

           if(checkLongest > longestPODCompletionTime)
           {
               longestPODCompletionTime = checkLongest;
               longestPODID = currentPOD;
           }
        }

        System.out.println("Total Plan Runtime: " + (longestPODCompletionTime/3600) + " hours.");
        System.out.println("Longest POD " + longestPODID + " : " + currentBERP.getPodList().get(longestPODID).getName() + " taking " + (longestPODCompletionTime/3600) + " hours.");

        //System.out.println("OUTPUT: " + handler.getOutput());
        
        System.out.println("Finished baseline simulation");
        
        return new SimulationData(currentSimID++, "none", longestPODCompletionTime, 129600, 7200);
    }
    
    /**
     * Analyzes the maximum flow along the road network given static routing.
     * @param tripfilename 
     * @param routefilename
     * @param staticFlowfilename
     * @param netFile
     * @param addlFile
     */
    public void singleCarGlobalAnalysis(String tripfilename, String routefilename, String staticFlowfilename, String netFile, String addlFile)
    {
        System.out.println("Analyzing single car trips and routes");
        
        //First we create the trip for the analysis
        
        File outputFile = new File(tripfilename);
        
        //If the file does not exist, create a new one.
        if(!outputFile.exists()) 
        {
            try {
                outputFile.createNewFile();
            } catch (IOException ex) {
             Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        FileWriter writer;
        try {
            writer = new FileWriter(outputFile.getAbsoluteFile());
            BufferedWriter buffwriter = new BufferedWriter(writer);
            
            buffwriter.write("<?xml version=\"1.0\"?>\n\n\n");
            
            buffwriter.write("<trips>\n");
            
            
            HashMap<Integer, BERPGSU> tempGSUList = currentBERP.getGsuList();
            HashMap<Integer, BERPPOD> tempPODList = currentBERP.getPodList();
            
            int tripID = 1;
            
            for(int currentKey : tempGSUList.keySet())
            {
                buffwriter.write("\t<trip id=\"" + currentKey +"\" depart=\"0.00\" from=\"" + tempGSUList.get(currentKey).getEdge() + "\" to=\"" + tempGSUList.get(currentKey).getEdge() + "\" via=\"" + tempPODList.get(tempGSUList.get(currentKey).getAssignedPODID()).getEdge() + "\"/>\n");
                
                tripID++;                 
            }
            
            buffwriter.write("</trips>\n");
            
            buffwriter.flush();
            
            buffwriter.close();
        } catch (IOException ex) {
            Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //Then we create the route for the analysis
        
        ProcessBuilder builder = new ProcessBuilder("duarouter",
                "-n", netFile,
                "-d", addlFile,       
                "-t", tripfilename,
                "-o", routefilename,
                "-s","10.00"
        );
        builder.redirectErrorStream(true);
        
        try {
            //Starting the process
            Process proc = builder.start();
            
            IOThreadHandler outputHandler = new IOThreadHandler(proc.getInputStream());
            
            //Starting the handler
            outputHandler.start();
            
            proc.waitFor();
            
            //If the exit value is not 0, then something went wrong and we need to throw an error
            if(proc.exitValue() != 0)
            {
                //TODO Add in something here
            }
            
            System.out.println("OUTPUT: " + outputHandler.getOutput());
        } catch (IOException ex) {
            Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        HashMap<String,Integer> carTotalsPerEdge = new HashMap();
        HashMap<String,String> PODsPerEdge = new HashMap();
        HashMap<String,String> GSUsPerEdge = new HashMap();
        //Now we can go through each route
        
        //First we initialize every edge to 0
        for(String key : currentBERP.getEdgeList().keySet())
        {
            carTotalsPerEdge.put(key, 0);
            PODsPerEdge.put(key,"");
            GSUsPerEdge.put(key,"");
        }
        
        //System.out.println("TESTING EDGELIST: " + currentBERP.getEdgeList().size() + " CARS " + carTotalsPerEdge.size());
        
        //Now we need to add in the car vType and set each vehicle to that vtype
        Pattern GSUIDPattern = Pattern.compile("<vehicle id=\"(.*?)\".*");
        Pattern routeEdge = Pattern.compile("<route edges=\"(.*?)\"/>");
        Matcher match = null;   
        File staticFlowAnalysis = new File(staticFlowfilename);
        BufferedReader br = null;    
        BufferedWriter bw = null;
        
        //We also need to add the population and route information to each BERPEdge
        
        
        int GSUID = 0;
        String route = "";
      
        try
        {
            //First we initialize the buffered reader
            br = new BufferedReader(new FileReader(new File(routefilename)));
            bw = new BufferedWriter(new FileWriter(staticFlowAnalysis));
            
            
            String line = "";
            while((line = br.readLine()) != null)
            {
                if(GSUIDPattern.matcher(line).find())
                {
                    match = GSUIDPattern.matcher(line);
                    match.find();
                    
                    GSUID = Integer.parseInt(match.group(1));
                }
                else if(routeEdge.matcher(line).find())
                {
                    match = routeEdge.matcher(line);
                    match.find();
                    
                    route = match.group(1);
                    
                    String[] edges = route.split("\\s+");
                    
                    int popForCurrentRoute = currentBERP.getGsuList().get(GSUID).getPopulation();
                    //System.out.print(GSUID + " " + popForCurrentRoute);
                    
                    //Now we are going to create BERProutes for later analysis
                    //This is only for the route leading up to the POD
                    BERPRoute newRoute = new BERPRoute(GSUID);
                    boolean beforePOD = true;
                    
                    
                    //Now we assign cars to each edge
                    for(String edge : edges) {
                       //System.out.print(" " + edge);
                        carTotalsPerEdge.put(edge, carTotalsPerEdge.get(edge) + popForCurrentRoute);
                        PODsPerEdge.put(edge, PODsPerEdge.get(edge) + "\t" + currentBERP.getGsuList().get(GSUID).getAssignedPODID());
                        GSUsPerEdge.put(edge, GSUsPerEdge.get(edge) + "\t" + GSUID);
                        
                        currentBERP.getEdgeList().get(edge).addPOD(currentBERP.getGsuList().get(GSUID).getAssignedPODID());
                        currentBERP.getEdgeList().get(edge).addCars(popForCurrentRoute);
                        currentBERP.getEdgeList().get(edge).addRoute();      
                        
                        if(beforePOD && !(currentBERP.getPodEdgeList().containsKey(edge)))
                        {
                            newRoute.addEdge(currentBERP.getEdgeList().get(edge));
                        }
                        else
                        {
                            //System.out.println("Reached the POD");
                            beforePOD = false;
                        }
                            
                    }                                        

                    currentBERP.addRoute(newRoute);
                    
                    //System.out.print("\n");                    
                }  
            }
            
            bw.write("edge_id\tcount\n");
            
            int tim = 0;
            
            for(String key : carTotalsPerEdge.keySet())
            {
                    bw.write(key + "\t" + carTotalsPerEdge.get(key) + "\t" + PODsPerEdge.get(key) + GSUsPerEdge.get(key) + "\n");
                    tim++;
            }
            
            currentBERP.setBaselineTraffic(carTotalsPerEdge);
            
            bw.flush();
            
        }
        catch (IOException ex) {
            Logger.getLogger(DataManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        finally {
            try {
                br.close();
                bw.close();
            } catch (IOException ex) {
                Logger.getLogger(DataManager.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
        
        System.out.println("Baseline created");
    }
    
    public void singleCarLocalAnalysis(String stubname, HashMap<Integer,String> congestionTripXMLs, HashMap<Integer,String> congestionRouteXMLs, String netFile, HashMap<Integer,String> validationAddlXMLs, HashMap<Integer,Boolean> routeSuccess)
    {
        System.out.println("Analyzing single car trips and routes for each catchment area");
        
        File outputFile;            
        
        for(int currentPODID : currentBERP.getPodList().keySet())
        {
            //First we generate all the trip files
            congestionTripXMLs.put(currentPODID,stubname + "_pod_" + currentPODID + ".trips.xml"); 
            congestionRouteXMLs.put(currentPODID,stubname + "_pod_" + currentPODID + ".route.xml");
            outputFile = new File(congestionTripXMLs.get(currentPODID));
            
            //If the file does not exist, create a new one.
            if(!outputFile.exists()) 
            {
                try {
                    outputFile.createNewFile();
                } catch (IOException ex) {
                 Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            FileWriter writer;
            try 
            {
                writer = new FileWriter(outputFile.getAbsoluteFile());
                BufferedWriter buffwriter = new BufferedWriter(writer);

                buffwriter.write("<?xml version=\"1.0\"?>\n\n\n");

                buffwriter.write("<trips>\n");


                HashMap<Integer, BERPGSU> tempGSUList = currentBERP.getGsuList();
                HashMap<Integer, BERPPOD> tempPODList = currentBERP.getPodList();

                int tripID = 1;

                for(int currentGSUID : tempPODList.get(currentPODID).getListOfGSUs())
                {
                    buffwriter.write("\t<trip id=\"" + currentGSUID +"\" depart=\"0.00\" from=\"" + tempGSUList.get(currentGSUID).getEdge() + "\" to=\"" + tempGSUList.get(currentGSUID).getEdge() + "\" via=\"" + tempPODList.get(currentPODID).getEdge() + "\"/>\n");

                    tripID++;                 
                }

                buffwriter.write("</trips>\n");

                buffwriter.flush();

                buffwriter.close();
            } catch (IOException ex) {
                Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            //Then we attempt to generate all of the route files
            ProcessBuilder builder = new ProcessBuilder("duarouter",
                "-n", netFile,
                "-d", validationAddlXMLs.get(currentPODID),       
                "-t", congestionTripXMLs.get(currentPODID),
                "-o", congestionRouteXMLs.get(currentPODID),
                "-s","10.00"
            );
            builder.redirectErrorStream(true);
                        
            try 
            {
                //Starting the process
                Process proc = builder.start();

                RouteReader outputHandler = new RouteReader(proc.getInputStream());

                //Starting the handler
                outputHandler.start();

                proc.waitFor();

                //If the exit value is not 0, then something went wrong and we need to throw an error
                if(proc.exitValue() != 0)
                {
                    //TODO Add in something here
                }
                
                //Detect if the route could be generated or not
                if(outputHandler.hasProblem())
                {
                    System.out.println("POD " + currentPODID + " HAD A PROBLEM");
                    routeSuccess.put(currentPODID, false);
                    System.out.println("OUTPUT: " + outputHandler.getOutput());
                    continue;
                }
                else
                {
                    System.out.println("POD " + currentPODID + " DID NOT HAVE A PROBLEM");
                    routeSuccess.put(currentPODID, true);
                    System.out.println("OUTPUT: " + outputHandler.getOutput());
                }
                
                
            } catch (IOException ex) 
            {
                Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InterruptedException ex) 
            {
                Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            //Now we can analyze them to get the routes
            HashMap<String,Integer> carTotalsPerEdge = new HashMap();
        
            //Now we can go through each route

            //First we initialize every edge to 0
            for(String key : currentBERP.getEdgeList().keySet())
            {
                carTotalsPerEdge.put(key, 0);
            }

            //System.out.println("TESTING EDGELIST: " + currentBERP.getEdgeList().size() + " CARS " + carTotalsPerEdge.size());

            //Now we need to add in the car vType and set each vehicle to that vtype
            Pattern GSUIDPattern = Pattern.compile("<vehicle id=\"(.*?)\".*");
            Pattern routeEdge = Pattern.compile("<route edges=\"(.*?)\"/>");
            Matcher match = null;   
            BufferedReader br = null;    

            //We also need to add the population and route information to each BERPEdge


            int GSUID = 0;
            String route = "";

            try
            {
                //First we initialize the buffered reader
                br = new BufferedReader(new FileReader(new File(congestionRouteXMLs.get(currentPODID))));

                String line = "";
                while((line = br.readLine()) != null)
                {
                    if(GSUIDPattern.matcher(line).find())
                    {
                        match = GSUIDPattern.matcher(line);
                        match.find();

                        GSUID = Integer.parseInt(match.group(1));
                    }
                    else if(routeEdge.matcher(line).find())
                    {
                        match = routeEdge.matcher(line);
                        match.find();

                        route = match.group(1);

                        String[] edges = route.split("\\s+");

                        int popForCurrentRoute = currentBERP.getGsuList().get(GSUID).getPopulation();
                        //System.out.print(GSUID + " " + popForCurrentRoute);

                        //Now we are going to create BERProutes for later analysis
                        //This is only for the route leading up to the POD
                        BERPRoute newRoute = new BERPRoute(GSUID);
                        boolean beforePOD = true;


                        //Now we assign cars to each edge
                        for(String edge : edges) 
                        {
                           //System.out.print(" " + edge);
                            carTotalsPerEdge.put(edge, carTotalsPerEdge.get(edge) + popForCurrentRoute);

                            currentBERP.getEdgeList().get(edge).addPOD(currentBERP.getGsuList().get(GSUID).getAssignedPODID());
                            currentBERP.getEdgeList().get(edge).addCars(popForCurrentRoute);
                            currentBERP.getEdgeList().get(edge).addRoute();      

                            if(beforePOD && !(currentBERP.getPodEdgeList().containsKey(edge)))
                            {
                                newRoute.addEdge(currentBERP.getEdgeList().get(edge));
                            }
                            else
                            {
                                //System.out.println("Reached the POD");
                                beforePOD = false;
                            }

                        }                                        

                        currentBERP.addRoute(newRoute);

                        //System.out.print("\n");                    
                    }  
                }

                currentBERP.setBaselineTraffic(carTotalsPerEdge);

            }
            catch (IOException ex) 
            {
                Logger.getLogger(DataManager.class.getName()).log(Level.SEVERE, null, ex);
            }
            finally 
            {
                try 
                {
                    br.close();
                } catch (IOException ex) 
                {
                    Logger.getLogger(DataManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
                        
        }
                
    }
    
    public void setCutOff(boolean cutoff)
    {
        oneHourCutoff = cutoff;
    }
    
    private static class IOThreadHandler extends Thread 
    {
        private InputStream inStream;
        private StringBuilder outString = new StringBuilder();
        private boolean readyToRead;
    
        IOThreadHandler (InputStream inStream) 
        {
            this.inStream = inStream;
            readyToRead = false;
        }
        
        
        @Override
        public void run()
        {
            BufferedReader br = null;
            
            try
            {
                br = new BufferedReader(new InputStreamReader(inStream));
                String line = "";

                while((line = br.readLine()) != null)
                {  
                    outString.append(line);
                    outString.append("\n");
                }

                } catch (IOException ex) {                
                Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);                           
            } finally
            {
                try {
                    br.close();
                } catch (IOException ex) {
                    Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            readyToRead = true;
        }
        
        public StringBuilder getOutput()
        {
            while(!readyToRead)
            {
                System.out.println("Not ready to read");
                
                try {
                    sleep(2000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            return outString;
        }
    
    }
    
    private static class RouteReader extends Thread 
    {
        private InputStream inStream;
        private StringBuilder outString = new StringBuilder();        
        private Pattern noConnection = Pattern.compile(".*has no valid route.*");
        private boolean problem = false;
        private boolean readyToRead;
    
        RouteReader (InputStream inStream) 
        {
            this.inStream = inStream;
            readyToRead = false;
        }
        
        
        @Override
        public void run()
        {
            BufferedReader br = null;
            
            try
            {
                br = new BufferedReader(new InputStreamReader(inStream));
                String line = "";

                while((line = br.readLine()) != null)
                {  
                    
                    if(noConnection.matcher(line).find())
                    {
                        problem = true;
                        //System.out.println("NO CONNECTION!");
                    }
                    
                    outString.append(line);
                    outString.append("\n");
                }

                } catch (IOException ex) {                
                Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);                           
            } finally
            {
                try {
                    br.close();
                } catch (IOException ex) {
                    Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            readyToRead = true;
        }
        
        public StringBuilder getOutput()
        {
            while(!readyToRead)
            {
                System.out.println("Not ready to read");
                
                try {
                    sleep(2000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            return outString;
        }
        
        /**
         * Returns true if the route could not be created, false otherwise.
         * @return 
         */
        public boolean hasProblem()
        {
            while(!readyToRead)
            {
                System.out.println("Not ready to read");
                
                try {
                    sleep(2000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            return problem;
        }
    
    }
    
    /**
     * This inner class samples and collects output from the simulator until it has determined steady state has been reached in the network.
     * 
     */
    private static class PerCatchmentSimOutputHandler extends Thread
    {
        //Holds on to the SUMO process thread
        private Process proc;
        //Holds on to the output from SUMO
        private InputStream inStream;
        
        private BERP currentBERP;
        
        private int podID;
        private String podName;
        private ArrayList<Integer> timeStepTime = new ArrayList();
        
        private final int beforePOD = 0, atPOD = 1, afterPOD = 2;
        private String podLane;
        private HashMap<Integer, Integer> vehicleStatus = new HashMap(); // vehicle ID to status
        private HashMap<Integer, Double> timeLeftPOD = new HashMap(); // vehicle ID to time in seconds
        
        
        private final Pattern timestepLine = Pattern.compile("<timestep time=\"(.*?)\".*>");
        private final Pattern msPerStep = Pattern.compile("\\((.*?)ms");
        private final Pattern vehicleInfo = Pattern.compile("<vehicle id=\"(.*?)\" lane=\"(.*?)\" pos.*");

        private Matcher match = null;

        private int finalTimeStep;
        
        private int carsProcessed;
        private int totalCars;
        private int avgProcTime;

        private boolean stillReading;
        private boolean useCutOff;
        
        /**
         * Constructor for the simulation output handler. Sets the 
         * @param inStream The handle for the instream from the simulator
         * @param sumo The handle for the process
         * @param numCars The total number of cars for in the simulation
         * @param window The number of time steps to analyze to determine if steady state has been reached
         */
        PerCatchmentSimOutputHandler (InputStream inStream, Process sumo, int podID, String podName, String podLane, int numCars, boolean useCutOff, BERP currentBERP, int avgProcTime) 
        {
            this.inStream = inStream;
            proc = sumo; 
            this.podID = podID;
            this.podName = podName;
            this.podLane = podLane;
                       
            stillReading = true;
            
            finalTimeStep = 0;
            carsProcessed = 0;
            totalCars = numCars;
            this.useCutOff = useCutOff;
            
            this.currentBERP = currentBERP;
            this.avgProcTime = avgProcTime;
        }        
        
        @Override
        public void run()
        {
            BufferedReader br = null;
            
            try
            {
                br = new BufferedReader(new InputStreamReader(inStream));
                
                String line;
                double currentTimeStep = 0;

                while((line = br.readLine()) != null)
                {  
                     //If the line has a timestep header, then we examine it
                    if(timestepLine.matcher(line).find())
                    {
                        match = timestepLine.matcher(line);
                        match.find();
                    
                        currentTimeStep = Double.parseDouble(match.group(1));   
                        
                        if(currentTimeStep % 10000 == 0 )
                        {
                            System.out.println("POD " + podID + " is at timestep " + currentTimeStep );
                        }
                        
                        if(currentTimeStep == 3600 && useCutOff)
                        {
                            System.out.println("POD " + podID + " is at the one hour mark. Ending simulation.");
                            
                            System.out.println("POD " + podID + " total cars " + totalCars + " processed cars " + carsProcessed + " proc time " + avgProcTime);
                            
                            finalTimeStep += (totalCars-carsProcessed)*((double)avgProcTime/currentBERP.getPodList().get(podID).getNumBooths());
                            
                            proc.destroy();
                            break;
                        }
                        
                        finalTimeStep++;                
                    }
                    else if(msPerStep.matcher(line).find())
                    {
                        match = msPerStep.matcher(line);
                        match.find();
                        
                        timeStepTime.add(Integer.parseInt(match.group(1)));
                   
                    }
                    else if(vehicleInfo.matcher(line).find())
                    {
                        match = vehicleInfo.matcher(line);
                        match.find();
                        
                        int vehicleID = Integer.parseInt(match.group(1));
                        String vehicleLane = match.group(2);
                        
                        if(vehicleStatus.containsKey(vehicleID))
                        {
                            if(vehicleStatus.get(vehicleID) == 0 && vehicleLane.equals(podLane))
                            {
                                vehicleStatus.put(vehicleID, 1);
                                /*
                                if(podID == 3)
                                {
                                    System.out.println("Vehicle " + vehicleID + " state 1 at \t" + currentTimeStep);
                                }
                                */
                            }
                            else if(vehicleStatus.get(vehicleID) == 1 && !vehicleLane.equals(podLane))
                            {
                                vehicleStatus.put(vehicleID, 2);
                                timeLeftPOD.put(vehicleID, currentTimeStep);
                                carsProcessed++;
                                /*
                                if(podID == 3)
                                {
                                    System.out.println("Vehicle " + vehicleID + " state 2 at \t" + currentTimeStep);
                                }
                                */
                            }
                        }
                        else
                        {
                            vehicleStatus.put(vehicleID, 0);
                        }
                        
                        //Checks if all of the cars have been processed;
                        if(carsProcessed == totalCars)
                        {
                            System.out.println("POD " + podID + " completed.");
                            proc.destroy();
                            break;
                        }
                    }

                }                
            
            stillReading = false;

            } catch (IOException ex) 
            {
                Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
            }
            finally 
            {
                try {
                    br.close();                
                } catch (IOException ex) {
                    Logger.getLogger(DataManager.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        }
                
        /**
         * Returns the final end time of the simulation.
         * @return 
         */
        public int getFinalTime()
        {
            while(stillReading)
            {
                System.out.println("Still reading output");
                
                try {
                    sleep(2000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            return finalTimeStep;
        }
        
        public ArrayList<Integer> getTimeStepTimes()
        {
            while(stillReading)
            {
                System.out.println("Still reading output");
                
                try {
                    sleep(2000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            return timeStepTime;
        }
        
        public HashMap<Integer, Double> getCompletionTimes()
        {
            return timeLeftPOD;
        }

    }
    
     /**
     * This inner class samples and collects output from the simulator until it has determined steady state has been reached in the network.
     * 
     */
    private static class GlobalSimOutputHandler extends Thread
    {
        //Holds on to the SUMO process thread
        private Process proc;
        //Holds on to the output from SUMO
        private InputStream inStream;
        
        private BERP currentBERP;
          private String podName;
        private ArrayList<Integer> timeStepTime = new ArrayList();
        
        private final int beforePOD = 0, atPOD = 1, afterPOD = 2;
        private HashMap<Integer, Integer> vehicleStatus = new HashMap(); // vehicle ID to status
        private HashMap<Integer, Double> timeLeftPOD = new HashMap(); // vehicle ID to time in seconds
        private HashMap<Integer, Double> PODCompletionTimes = new HashMap();
        
        HashMap<Integer, Integer> carToPOD;        
        HashMap<Integer, Integer> carsLeftForPOD= new HashMap();
               
        
        private final Pattern timestepLine = Pattern.compile("<timestep time=\"(.*?)\".*>");
        private final Pattern msPerStep = Pattern.compile("\\((.*?)ms");
        private final Pattern vehicleInfo = Pattern.compile("<vehicle id=\"(.*?)\" lane=\"(.*?)\" pos.*");

        private Matcher match = null;

        private int finalTimeStep;
        
        private int carsProcessed;
        private int totalCars;

        private boolean stillReading;
        
        /**
         * Constructor for the simulation output handler. Sets the 
         * @param inStream The handle for the instream from the simulator
         * @param sumo The handle for the process
         * @param numCars The total number of cars for in the simulation
         * @param window The number of time steps to analyze to determine if steady state has been reached
         */
        GlobalSimOutputHandler (InputStream inStream, Process sumo, BERP currentBERP) 
        {
            this.inStream = inStream;
            proc = sumo; 
            this.currentBERP = currentBERP;
            carToPOD = currentBERP.getCarToPod();
            
            totalCars = 0;
            
            //Use this to determine how many cars are left
            for(int key : currentBERP.getCarsPerPod().keySet())
            {
                totalCars += currentBERP.getCarsPerPod().get(key);
                carsLeftForPOD.put(key, currentBERP.getCarsPerPod().get(key));
            }
                       
            stillReading = true;
            
            finalTimeStep = 0;
            carsProcessed = 0;
            
            
        }        
        
        @Override
        public void run()
        {
            BufferedReader br = null;
            
            try
            {
                br = new BufferedReader(new InputStreamReader(inStream));
                
                String line;
                double currentTimeStep = 0;

                while((line = br.readLine()) != null)
                {  
                     //If the line has a timestep header, then we examine it
                    if(timestepLine.matcher(line).find())
                    {
                        match = timestepLine.matcher(line);
                        match.find();
                    
                        currentTimeStep = Double.parseDouble(match.group(1));   
                        
                        if(currentTimeStep % 10000 == 0 )
                        {
                            System.out.println("The simulation is at timestep " + currentTimeStep );
                        }
                        
                        finalTimeStep++;                
                    }
                    else if(msPerStep.matcher(line).find())
                    {
                        match = msPerStep.matcher(line);
                        match.find();
                        
                        timeStepTime.add(Integer.parseInt(match.group(1)));
                   
                    }
                    else if(vehicleInfo.matcher(line).find())
                    {
                        match = vehicleInfo.matcher(line);
                        match.find();
                        
                        int vehicleID = Integer.parseInt(match.group(1));
                        String vehicleLane = match.group(2);
                        String podLane = currentBERP.getPodList().get(carToPOD.get(vehicleID)).getLane();
                        
                        if(vehicleStatus.containsKey(vehicleID))
                        {
                                                        
                            if(vehicleStatus.get(vehicleID) == 0 && vehicleLane.equals(podLane))
                            {
                                vehicleStatus.put(vehicleID, 1);
                                /*
                                if(carToPOD.get(vehicleID) == 3)
                                {
                                    System.out.println("Vehicle " + vehicleID + " state 1 at \t" + currentTimeStep);
                                }
                                */
                            }
                            else if(vehicleStatus.get(vehicleID) == 1 && !vehicleLane.equals(podLane))
                            {
                                vehicleStatus.put(vehicleID, 2);
                                timeLeftPOD.put(vehicleID, currentTimeStep);
                                
                                carsLeftForPOD.put(carToPOD.get(vehicleID), carsLeftForPOD.get(carToPOD.get(vehicleID)) -1 );
                                /*
                                if(carToPOD.get(vehicleID) == 3)
                                {
                                    System.out.println("Vehicle " + vehicleID + " state 2 at \t" + currentTimeStep);
                                }
                                */
                                carsProcessed++;
                            }
                        }
                        else
                        {
                            vehicleStatus.put(vehicleID, 0);
                        }
                        
                        if(carsLeftForPOD.get(carToPOD.get(vehicleID)) == 0 && !PODCompletionTimes.containsKey(carToPOD.get(vehicleID)))
                        {              
                            PODCompletionTimes.put(carToPOD.get(vehicleID), currentTimeStep);
                        }
                        
                        //Checks if all of the cars have been processed;
                        if(carsProcessed == totalCars)
                        {
                            System.out.println("Simulation completed.");
                            proc.destroy();
                            break;
                        }
                    }

                }                
            
            stillReading = false;

            } catch (IOException ex) 
            {
                Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
            }
            finally 
            {
                try {
                    br.close();                
                } catch (IOException ex) {
                    Logger.getLogger(DataManager.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        }
                
        /**
         * Returns the final end time of the simulation.
         * @return 
         */
        public int getFinalTime()
        {
            while(stillReading)
            {
                System.out.println("Still reading output");
                
                try {
                    sleep(2000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            return finalTimeStep;
        }
        
        public ArrayList<Integer> getTimeStepTimes()
        {
            while(stillReading)
            {
                System.out.println("Still reading output");
                
                try {
                    sleep(2000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            return timeStepTime;
        }
        
        public HashMap<Integer, Double> getCompletionTimes()
        {
            return PODCompletionTimes;
        }

    }
    
    
    /**
     * This class stores car ID's and their depart times 
     */
    private class tripCarIDDepartTime
    {
        private int id = 0;
        private int GSUID = 0;
        private int departTime = 0;
        public tripCarIDDepartTime(int id, int GSUID, int departTime)
        {
            this.id = id;
            this.GSUID = GSUID;
            this.departTime = departTime;
        }
        
        int getID()
        {
            return id;
        }
        
        int getGSUID()
        {
            return GSUID;
        }
        
        int getDepartTime()
        {
            return departTime;
        }
    }
    
    /**
     * This class is a comparator used to sort car ID's and their depart times
     */
    private class SortByDepartTime implements Comparator<tripCarIDDepartTime>
{
    // Used for sorting in ascending order of
    // roll number
    @Override
    public int compare(tripCarIDDepartTime a, tripCarIDDepartTime b)
    {
        return a.getDepartTime() - b.getDepartTime();
    }

    }
}
