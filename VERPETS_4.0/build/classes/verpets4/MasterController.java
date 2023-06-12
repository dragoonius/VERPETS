/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package verpets4;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author JHelsing
 */
public class MasterController {
    
    private SimulationManager simManager;
    private DataManager dataManager;
    
    private String VERPETSconfigFile;
    private String originalOSMFile;
    private String processedOSMFile;
    private String OSMPath;
    private String OSMName;

    private String flowTripXML;
    private String flowRouteXML;
    private String flowAddlXML;
    private String statisFlowFile;
    private String chosenSinglePlan;
    private String trainingFile;
    private String testingFile;
    private String rawNetXML;
    private String validationNetXML;
    private String congestionNetXML;
    private String stubValidation;
    private String stubCongestion;
    private HashMap<Integer,String> validationTripXMLs;
    private HashMap<Integer,String> congestionTripXMLs;
    private HashMap<Integer,String> validationAddlXMLs;
    private HashMap<Integer,String> validationRouteXMLs;
    private HashMap<Integer,String> congestionRouteXMLs;
    private HashMap<Integer,String> validationRefinedRouteXMLs;
    private HashMap<Integer,String> congestionRefinedRouteXMLs;
    private HashMap<Integer,String> validationSumoCFGs;
    private HashMap<Integer,String> congestionSumoCFGs;
    ArrayList<ArrayList<String>> isolatedEdges;
    
    private int steadyStateWindow;
    private int planTimeLimitHours;
    //private int averageProcessingTime;
    private int vehicleReleaseInterval;
    private int batchSize;
    private int roundingStrategy;
    private final int CEIL = 0, FLOOR = 1, CLOSEST = 2, NONE = 3;
    private final double carLength = 7.5;
    private int populationPerAgent;
    private boolean runCatchmentsSeparately;
    private boolean oneHourCutoff;
    private boolean homogenousProcTime;
    private double procTimeScaling;
    private HashMap<Integer,Integer> PODtoProcTime;
    
    public MasterController()
    {
        this.setupDatabase();
        this.setupSimulation();        
        
        VERPETSconfigFile = "C:\\Users\\JHelsing\\Documents\\NetBeansProjects\\VERPETS_4.0\\src\\verpets4\\files\\verpets.config";
    }
    
    private void setupDatabase()
    {
        dataManager = new DataManager();
        dataManager.createDBConnection();
        dataManager.generateTables();
    }
    
    private void setupSimulation()
    {     
        simManager = new SimulationManager();
    }
    
    public void validatePlan()
    {
        this.loadConfiguration();
        this.loadOSMFile();
        this.initializeBERP();
        this.initializeSimulation();
        this.staticFlowAnalysis();
        this.runValidationSimulation();
    }
    
    public void sensitivityAnalysis()
    {
        System.out.println("Starting Sensitivity Analysis");
        
        
        
        /*
        
        //First we load the BERP into the LearningAgentManager        
        laManager.loadBERP(dataManager.getBERP());
        
        //Next we mark all of the taboo edges that should never be removed
        dataManager.setProtetectedEdges();
        
        //Then we generate the list of edges for every route in every catchment area
        laManager.generateEdgeLists();
        
        //This has two phases: the random experiment phase, and the learning phase
        //First we create the base congestion free plan to setup departure times
            //Generate the trip files that will be used across all experiment
        //HashMap<Integer,String> carIDtoArrivalEdge = simManager.congestTripFileCreator(stubCongestion, averageProcessingTime, congestionTripXMLs);
        //HashMap<Integer,String> carIDtoArrivalEdge = simManager.naiveCongestTripFileCreator(stubCongestion, averageProcessingTime, congestionTripXMLs);
        //simManager.rrCongestTripFileCreator(stubCongestion, averageProcessingTime, planTimeLimitHours, congestionTripXMLs);

        int numberOfEdgesUsed = 0;
        double currentAvgConfidence = 0;
        int currentNumIterations = 0;
        int numPODS = dataManager.getBERP().getPodList().size();
        HashMap<Integer,String> selectedEdges;
        //Use this to keep track of if we have added in the selected edges yet
        int numRemovedEdges = isolatedEdges.size();
        //Keeps track of which POD's could have a route generated for them
        HashMap<Integer,Boolean> routeSuccess;        
        //Keeps track of which POD's could have a route generated for them
        HashMap<Integer,Boolean> scheduleSuccess;
        //Keeps track of when the last car left each POD
        HashMap<Integer,Integer> endTime;
        
        //TESTING FILE
        File testingfile = new File(OSMPath + "\\testingFile.txt");
        FileWriter testingwriter = null;
        BufferedWriter tbuffwriter = null;
        
        if(!testingfile.exists()) 
        {
            try {
                testingfile.createNewFile();
            } catch (IOException ex) {
             Logger.getLogger(SimulationManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        try {
            testingwriter = new FileWriter(testingfile.getAbsoluteFile());
            tbuffwriter = new BufferedWriter(testingwriter);
        } catch (IOException ex) {
            Logger.getLogger(MasterController.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        while(numberOfEdgesUsed < initialTrainingSetSize)
        {        
            routeSuccess = new HashMap();
            selectedEdges = new HashMap();
            scheduleSuccess = new HashMap();
            endTime = new HashMap();
            
            //First we selected edges
            laManager.selectRandomEdges(selectedEdges);
                        
            if(isolatedEdges.size() > numRemovedEdges)
            {
                isolatedEdges.set(numRemovedEdges, new ArrayList());
                for(Map.Entry<Integer,String> entry : selectedEdges.entrySet())
                {
                    isolatedEdges.get(numRemovedEdges).add(entry.getValue());
                }
                
            }
            else
            {
                isolatedEdges.add(new ArrayList());
                for(Map.Entry<Integer,String> entry : selectedEdges.entrySet())
                {
                    isolatedEdges.get(numRemovedEdges).add(entry.getValue());
                }
            }
            
            //Then we generate the netfile with those removed
            simManager.netFileCreatorSelectedEdges(processedOSMFile,congestionNetXML,isolatedEdges);
            dataManager.generateShapefile(congestionNetXML,0);
            
            //Now we generate single car trips to generate the new routes
            simManager.singleCarLocalAnalysis(stubCongestion, congestionTripXMLs, congestionRouteXMLs, congestionNetXML, validationAddlXMLs, routeSuccess);
                        
            //Finally we attempt to schedule the trips using the route information, but only for routes are possible
            simManager.rrCongestionAnalysis(averageProcessingTime, planTimeLimitHours, routeSuccess, scheduleSuccess, endTime);
            
            //Last we create data instances to train from            
            for(Map.Entry<Integer,String> entry : selectedEdges.entrySet())
            {    
                try
                {
                if(!routeSuccess.get(entry.getKey()))
                {//If it was not routable
                    //tbuffwriter.write("3\tPOD " + entry.getKey() + "\t with edge " + entry.getValue() + " was neither routable nor schedulable");
                    laManager.addTrainingData(entry.getValue(), false, false,-1);
                }
                else if(!scheduleSuccess.get(entry.getKey()))
                {//If it was routable but not schedulable
                    tbuffwriter.write("2\tPOD " + entry.getKey() + "\t with edge " + entry.getValue() + " was routable but not schedulable with end time \t" + endTime.get(entry.getKey()));
                    laManager.addTrainingData(entry.getValue(), false, true, endTime.get(entry.getKey()));
                }
                else
                {//If it was routable and scheduleable
                    tbuffwriter.write("1\tPOD " + entry.getKey() + "\t with edge " + entry.getValue() + " was routable and schedulable with end time \t" + endTime.get(entry.getKey()));
                    laManager.addTrainingData(entry.getValue(), true, true, endTime.get(entry.getKey()));
                }  
                
                tbuffwriter.newLine();
                tbuffwriter.flush();
                } catch (IOException ex) {
                    Logger.getLogger(MasterController.class.getName()).log(Level.SEVERE, null, ex);
                }
            }            
            
            numberOfEdgesUsed += numPODS;
        }
        
        try 
        {
            
            tbuffwriter.close();
        } catch (IOException ex) 
        {
            Logger.getLogger(DataManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        */
        
        System.out.println("Sensitivity Analysis Complete");
    }
    
    private void loadConfiguration()
    {
        BufferedReader br = null; 
        File f = new File(VERPETSconfigFile);
        Pattern osmFile = Pattern.compile("OSM File=(.*$)");
        Pattern planTable = Pattern.compile("Response Plan Table=(.*$)");
        Pattern timeLimit = Pattern.compile("Plan Time Limit=(.*$)");
        Pattern procTime = Pattern.compile("Average Processing Time=(.*$)");
        Pattern vehicRelease = Pattern.compile("Vehicle Release Interval=(.*$)");
        Pattern batchS = Pattern.compile("Batch Size=(.*$)");
        Pattern roundStrat = Pattern.compile("Rounding=(.*$)");
        Pattern popPerAg = Pattern.compile("Households Per Agent=(.*$)");
        Pattern sepCatch = Pattern.compile("Run Catchments Separately=(.*$)");
        Pattern cutoff = Pattern.compile("One hour cutoff=(.*$)");
        Pattern homogProcTime = Pattern.compile("Homogenous Processing Time=(.*$)");
        Matcher match;
        
        PODtoProcTime = new HashMap();
        
        if(!f.exists())
        {
            System.out.println("Failed to load VERPETS configuration file. Bad filename or does not exist."); 
            System.exit(-1);
        }
        
        try {       
            //First we initialize the buffered reader
            br = new BufferedReader(new FileReader(f)); 
            
            String line;
            while((line = br.readLine()) != null)
            {
                if(osmFile.matcher(line).find())
                {                    
                    match = osmFile.matcher(line);
                    match.find();
                    
                    originalOSMFile = match.group(1);
                    
                    System.out.println("Using OSM file: " + originalOSMFile);
                }
                else if(planTable.matcher(line).find())
                {                    
                    match = planTable.matcher(line);
                    match.find();
                    
                    chosenSinglePlan = match.group(1);
                    
                    System.out.println("Using response plan: " + chosenSinglePlan);
                }
                else if(timeLimit.matcher(line).find())
                {
                    match = timeLimit.matcher(line);
                    match.find();
                    
                    planTimeLimitHours = Integer.parseInt(match.group(1));
                    
                    System.out.println("Response plan time limit: " + planTimeLimitHours + " hours");
                }
                else if(homogProcTime.matcher(line).find())
                {
                    match = homogProcTime.matcher(line);
                    match.find();
                    
                    if(match.group(1).toLowerCase().equals("true"))
                    {
                        homogenousProcTime = true;
                        System.out.println("All PODs have the same processing time");
                    }
                    else if(match.group(1).toLowerCase().equals("false"))
                    {
                        homogenousProcTime = false;
                        System.out.println("PODs may have different processing times");
                    }
                    else
                    {
                        System.out.println("Incorrect input for how to homogenous processing time.");
                        System.exit(1);
                    }
                    
                }                
                else if(procTime.matcher(line).find())
                {
                    match = procTime.matcher(line);
                    match.find();
                    
                    if(homogenousProcTime)
                    {
                        PODtoProcTime.put(1,Integer.parseInt(match.group(1)));
                        System.out.println("The average processing time is: " + PODtoProcTime.get(1) + " seconds");
                    }
                    else
                    {
                        System.out.print("The processing times are: ");
                        String times[] = match.group(1).split(",");
                        for (int i = 1; i <= times.length; i++)
                        {
                            PODtoProcTime.put(i, Integer.parseInt(times[i-1]));
                            System.out.print("POD " + i + " " + PODtoProcTime.get(i) + " ");
                        }
                        System.out.print("\n");
                    }                    
                   
                }
                else if(vehicRelease.matcher(line).find())
                {
                    match = vehicRelease.matcher(line);
                    match.find();
                    
                    vehicleReleaseInterval = Integer.parseInt(match.group(1));
                    
                    if(vehicleReleaseInterval > 0)
                    {
                        System.out.println("Vehicles released every " + vehicleReleaseInterval + " seconds");
                    }
                    else
                    {
                        System.out.println("All vehicles released at once");
                    }
                }
                else if(popPerAg.matcher(line).find())
                {
                    match = popPerAg.matcher(line);
                    match.find();
                    
                    populationPerAgent = Integer.parseInt(match.group(1));
                    
                    System.out.println("There will be " + populationPerAgent + " households per agent");
                }  
                else if(sepCatch.matcher(line).find())
                {
                    match = sepCatch.matcher(line);
                    match.find();
                    
                    if(match.group(1).toLowerCase().equals("true"))
                    {
                        runCatchmentsSeparately = true;
                        System.out.println("The catchment areas will be processed separately");
                    }
                    else if(match.group(1).toLowerCase().equals("false"))
                    {
                        runCatchmentsSeparately = false;
                        System.out.println("The catchment areas will be processed together");
                    }
                    else
                    {
                        System.out.println("Incorrect input for how to process catchment areas");
                        System.exit(1);
                    }                                        
                    
                }
                else if(batchS.matcher(line).find())
                {
                    match = batchS.matcher(line);
                    match.find();
                    
                    batchSize = Integer.parseInt(match.group(1));
                    
                    System.out.println("The batch size is " + batchSize + " PODS");
                }
                else if(cutoff.matcher(line).find())
                {
                    match = cutoff.matcher(line);
                    match.find();
                    
                    if(match.group(1).toLowerCase().equals("true"))
                    {
                        oneHourCutoff = true;                        
                        System.out.println("The simulation will be cutoff after 1 hour of simulated time");
                    }
                    else if(match.group(1).toLowerCase().equals("false"))
                    {
                        oneHourCutoff = false;
                        System.out.println("The simulation will be allowed to run to completion");
                    }
                    else
                    {
                        System.out.println("Incorrect input for how to run simulation");
                        System.exit(1);
                    }  
                    
                    simManager.setCutOff(oneHourCutoff);
                    
                }
                else if(roundStrat.matcher(line).find())
                {
                    match = roundStrat.matcher(line);
                    match.find();
                    
                    roundingStrategy = Integer.parseInt(match.group(1));
                                        
                    switch (roundingStrategy) {
                        case CEIL:
                            System.out.println("Vehicle rounding strategy is to take the ceiling");
                            break;
                        case FLOOR:
                            System.out.println("Vehicle rounding strategy is to take the floor");
                            break;
                        case CLOSEST:
                            System.out.println("Vehicle rounding strategy is to take the closest");
                            break;
                        case NONE:
                            System.out.println("Vehicle rounding strategy is to use partial agents");
                            break;
                        default:
                            System.out.println("Vehicle rounding strategy is undefined");
                            System.out.println("Exiting program");
                            System.exit(-1);
                    }
                    
                }
            }
            
        }
        catch (IOException ex) {
            Logger.getLogger(DataManager.class.getName()).log(Level.SEVERE, null, ex);
        }finally {
            try {
                br.close();                
            } catch (IOException ex) {
                Logger.getLogger(DataManager.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
        
        System.out.println("Successfully loaded configuration file.");
    }
    
    /**
     * Loads the raw OSM file into the system and renames the ways to have unique ID's.
     * @param osmFilePath
     * @return Status result of processing for output text area
     */
    private String loadOSMFile()
    {
        File f = new File(originalOSMFile);
        
        if(!f.exists())
        {
            return "Failed to load OSM file: " + originalOSMFile + ". Bad filename or does not exist. \n";             
        }
        
        OSMPath = f.getParent();
        
        String filename = f.getName().substring(0, f.getName().length()-4);
        
        OSMName = filename;
        
        //Now we can set the name of all of the associated files
        processedOSMFile = f.getParent() + "\\" + "edited_" + f.getName();        
        validationNetXML = f.getParent() + "\\" + filename + "_validation.net.xml";
        congestionNetXML = f.getParent() + "\\" + filename + "_congestion.net.xml";
        rawNetXML = f.getParent() + "\\" + filename + "_raw.net.xml";
        stubValidation = f.getParent() + "\\" + filename + "_validation";
        stubCongestion = f.getParent() + "\\" + filename + "_congestion";
        flowTripXML = f.getParent() + "\\" + filename + "_flow.trip.xml";
        flowRouteXML = f.getParent() + "\\"  + filename + "_flow.route.xml";
        flowAddlXML = f.getParent() + "\\"  + filename + "_flow.addl.xml";
        statisFlowFile = f.getParent() + "\\"  + filename + "_staticFlowAnalysis.txt";
        trainingFile = f.getParent() + "\\"  + filename + "_training.arff";
        testingFile = f.getParent() + "\\"  + filename + "_testing.arff";
        
        validationTripXMLs = new HashMap();
        congestionTripXMLs = new HashMap();
        validationAddlXMLs = new HashMap();
        validationRouteXMLs = new HashMap();
        congestionRouteXMLs = new HashMap();
        validationRefinedRouteXMLs = new HashMap();
        congestionRefinedRouteXMLs = new HashMap();
        validationSumoCFGs = new HashMap();
        congestionSumoCFGs = new HashMap();
        
        dataManager.renameOSMWays(originalOSMFile, processedOSMFile); 
          
        return "Successfully loaded and processed OSM file: " + originalOSMFile + "\n";
    }
    
    /**
     * Generates the main BERP to be used by the program.
     * First it generates a .net file from the OSM file and then adds that as a shapefile to the database
     * Then it removes any weakly connected edges.
     * Then it regenerates the .new file from the OSM file excluding any weakly connected edges, and replaces the original shapefile
     * Finally it generates the BERP data structure
     */
    private void initializeBERP()
    {
        //Processing Road Network Data
        simManager.netFileCreator(processedOSMFile, rawNetXML);
        dataManager.collectRoadNetworkEdges(rawNetXML,0);

        //Now we find the edges that are not strongly connected and then recreate the network file without them
        isolatedEdges = dataManager.roadNetworkGraph();
        simManager.netFileCreatorIsolatedEdges(processedOSMFile, rawNetXML, isolatedEdges);
        
        dataManager.netFileSpeedLimitAdjustment(rawNetXML, validationNetXML, populationPerAgent, carLength);

        dataManager.generateShapefile(validationNetXML,0);

        //Processing BERP Data
        dataManager.generateBERP(chosenSinglePlan,PODtoProcTime);
    }
    
    private void initializeSimulation()
    {
        //Pass the simulation manager a handle on the current BERP
        simManager.assignBERP(dataManager.getBERP()); 

        if(runCatchmentsSeparately)
        {
            //Generate the validation trip files
            HashMap<Integer,String> carIDtoArrivalEdge = simManager.perCatchmentTripFileCreator(stubValidation, roundingStrategy, populationPerAgent, validationTripXMLs, vehicleReleaseInterval);

            dataManager.setCarIDtoArrivalEdge(carIDtoArrivalEdge);

            //Generate the validation addl files
            simManager.perCatchmentAddlFileCreator(stubValidation, validationAddlXMLs);

            //Generate the route files
            boolean problem = simManager.perCatchmentRouteFileCreator(stubValidation,validationNetXML,validationAddlXMLs,validationTripXMLs, validationRouteXMLs, validationRefinedRouteXMLs,batchSize,populationPerAgent,PODtoProcTime);

            if(problem)
            {
                System.out.println("Could not generate some route. Halting system.");
                System.exit(1);
            }
            
            //Generate the SUMO config files
            simManager.perCatchmentSumoconfigFileCreator(stubValidation, validationNetXML, validationRefinedRouteXMLs, validationAddlXMLs, validationSumoCFGs);
        }
        else
        {
            //Generate the validation trip file
            HashMap<Integer,String> carIDtoArrivalEdge = simManager.globalTripFileCreator(stubValidation, roundingStrategy, populationPerAgent, vehicleReleaseInterval);

            dataManager.setCarIDtoArrivalEdge(carIDtoArrivalEdge);

            //Generate the validation addl file
            simManager.globalAddlFileCreator(stubValidation);
            
            //Generate the route file
            boolean problem = simManager.globalRouteFileCreator(stubValidation, validationNetXML, PODtoProcTime);
            
            if(problem)
            {
                System.out.println("Could not generate some route. Halting system.");
                System.exit(1);
            }
            
            //Generate the SUMO config file
            simManager.globalSumoconfigFileCreator(stubValidation, validationNetXML);
            
        }
    }
    
    private void runValidationSimulation()
    {
        System.out.println("Running validation simulation");
              
        //Don't save this data
        SimulationData validationData = null;
        
        if(runCatchmentsSeparately)
        {
            validationData = simManager.runPerCatchmentValidationSimulation(validationSumoCFGs, PODtoProcTime, dataManager.getCarIDtoArrivalEdge(), batchSize);
        }
        else
        {
            validationData = simManager.runGlobalValidationSimulation(stubValidation, dataManager.getCarIDtoArrivalEdge());
        }
        
        if((validationData.getCompletionTime()/3600.00) > planTimeLimitHours)
        {
            System.out.println("This plan failed to complete within " + planTimeLimitHours + " hours.");
            System.out.println("It exceeded the time limit by " + (validationData.getCompletionTime()/3600 - planTimeLimitHours) + " hours.");
            System.out.println("No further testing will be performed.");
            System.exit(0);
        }
        else
        {
            System.out.println("This plan successfully completed within " + planTimeLimitHours + " hours.");
            System.out.println("Beginning sensitivity analysis.");
        }
        
    }
    
    private void staticFlowAnalysis()
    {
        System.out.println("Analyzing the static flow");
        
        simManager.flowaddlFileCreator(flowAddlXML);
                
        simManager.singleCarGlobalAnalysis(flowTripXML, flowRouteXML, statisFlowFile, validationNetXML, flowAddlXML);
        
        for(Integer routeID : dataManager.getBERP().getRouteList().keySet())
        {
            System.out.print("Route ID: " + dataManager.getBERP().getRouteList().get(routeID).getGSUID() + " ");
            System.out.print("Num edges: " + dataManager.getBERP().getRouteList().get(routeID).getNumEdges() + " ");
            System.out.print("Length: " + dataManager.getBERP().getRouteList().get(routeID).getTotalLength() + " ");
            System.out.print("Time: " + dataManager.getBERP().getRouteList().get(routeID).getTotalTime() + "\n");
        }            
    }
    
    /**
     * Cleans up all of the extra files generated during runtime.
     */    
    public void cleanUpFiles()
    {
        System.out.println("Cleaning up files.");
        File f;
        
        f = new File(processedOSMFile);
        f.delete();
        
        f = new File(flowTripXML);
        f.delete();
        
        f = new File(flowAddlXML);
        f.delete();
        
        f = new File(flowRouteXML);
        f.delete();
        
        f = new File(validationNetXML);
        f.delete();       
        
        f = new File(stubValidation);
        f.delete();   
        
        for(Map.Entry<Integer,String> entry : validationTripXMLs.entrySet()) 
        {
            f = new File(entry.getValue());
            f.delete();
        }
        
        for(Map.Entry<Integer,String> entry : congestionTripXMLs.entrySet()) 
        {
            f = new File(entry.getValue());
            f.delete();
        }
        
        for(Map.Entry<Integer,String> entry : validationAddlXMLs.entrySet()) 
        {
            f = new File(entry.getValue());
            f.delete();
        }
        
        for(Map.Entry<Integer,String> entry : validationRouteXMLs.entrySet()) 
        {
            f = new File(entry.getValue());
            f.delete();
        }
        
        for(Map.Entry<Integer,String> entry : congestionRouteXMLs.entrySet()) 
        {
            f = new File(entry.getValue());
            f.delete();
        }
        
        for(Map.Entry<Integer,String> entry : validationRefinedRouteXMLs.entrySet()) 
        {
            f = new File(entry.getValue());
            f.delete();
        }
        
        for(Map.Entry<Integer,String> entry : congestionRefinedRouteXMLs.entrySet()) 
        {
            f = new File(entry.getValue());
            f.delete();
        }
        
        for(Map.Entry<Integer,String> entry : validationSumoCFGs.entrySet()) 
        {
            f = new File(entry.getValue());
            f.delete();
        }
        
        for(Map.Entry<Integer,String> entry : congestionSumoCFGs.entrySet()) 
        {
            f = new File(entry.getValue());
            f.delete();
        }
    }
        
}
