/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package verpets4;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author JHelsing
 */
public class DataManager {
    
    //Database Connection Variables
    private Connection connection = null;
    private Statement statement = null;
    private String databaseHost = "";
    private String databaseUser = "";
    private String databasePass = "";
    private String networkTableName = "jhelsing.networkShapefile";
    
    //BERP and Simulation information
    private BERP currentBERP;
    private HashMap<String,SimulationData> simulationList;
    private HashMap<String,String> workingLaneToEdgeList;
    private HashMap<Integer,String> carIDtoArrivalEdge;
    private RoadNetworkGraph roadNetwork;
    
    /**
     * Default DatabaseController setup.
     * Assumes the localhost instance
     */
    public DataManager()
    {
        databaseHost = "jdbc:postgresql://localhost:5433/tx_acs15";
        databaseUser = "replan_user";
        databasePass = "replan_rack_server";
        
        simulationList = new HashMap();
        workingLaneToEdgeList = new HashMap();
        roadNetwork = new RoadNetworkGraph();
        carIDtoArrivalEdge = new HashMap();
    }
    
    /**
     * Creates the connection the database.
     * 
     * We assume for the moment that the database is local
     */
    public void createDBConnection()
    {        
        try 
        {
            Class.forName("org.postgresql.Driver");
            
            connection = DriverManager.getConnection(databaseHost,databaseUser, databasePass);
            
            statement = connection.createStatement();
        } 
        catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(DataManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Closes the connection the database.
     * 
     * We assume for the moment that the database is local
     */
    public void closeDBConnection()
    {
        if(statement != null)
        {
            try {
                statement.close();
            } catch (SQLException ex) {
                Logger.getLogger(DataManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        if(connection != null)
        {
            try {
                connection.close();
            } catch (SQLException ex) {
                Logger.getLogger(DataManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    /**
     * Generates the necessary table for the road network shapefile.
     * If a table currently exists it drops that table first before generating a new table.
     */
    public void generateTables()
    {
        int result = 0;
        
        try {
            
            result = statement.executeUpdate("DROP TABLE IF EXISTS " + networkTableName + ";");
            
            //Generate table for the Shapefile of the road network
            result = statement.executeUpdate("CREATE TABLE " + networkTableName + " (" + 
                                                 "uid INTEGER" +
                                                 ", simID INTEGER" +
                                                 ", edge_name TEXT" +
                                                 ", edge_id TEXT" +
                                                 ", lane_id TEXT" +
                                                 ", length FLOAT" +
                                                 ", speed FLOAT" +                                                 
                                                 ", geom_text TEXT" +
                                                 ", the_geom GEOMETRY(LINESTRING,4269)" +
                                                 ");");
            
            //Set a the_geom's SRID to 4326
            //result = statement.executeUpdate("SELECT UpdateGeometrySRID('" + networkTableName + "','the_geom',4326);");

        } catch (SQLException ex) {
            Logger.getLogger(DataManager.class.getName()).log(Level.SEVERE, null, ex);
        }       
    }
    
    /**
     * Takes in a raw OSM file and either appends unique IDs to the ends of the existing names or adds a unique ID as a name
     * @param osmFilename This is the name and location of the original OSM file
     * @param newOSMFilename This is the name and location of the edited OSM file
     */
    public void renameOSMWays(String osmFilename, String newOSMFilename)
    {
        System.out.println("Adding names and unique IDs to OSM file");
        
        File f = new File(newOSMFilename);
        
        if(f.exists())
        {
            System.out.println(newOSMFilename +" already exists. Skipping this step.\n");      
            return;
        }
        
        BufferedReader br = null;        
        BufferedWriter bw = null;
        
        int uniqueNameID = 0;
        
        Pattern openWayTag = Pattern.compile("<way id=");
        Pattern closeWayTag = Pattern.compile("</way>");
        Pattern nameTag = Pattern.compile("<tag k=\"name\"");
        Pattern nameValue = Pattern.compile("(<*.v=\")(.*)(\"/>)");
        Matcher match = null;
        
        try {
            //First we initialize the buffered reader
            br = new BufferedReader(new FileReader(new File(osmFilename)));
            //Then we initialize the buffered writer
            bw = new BufferedWriter(new FileWriter(newOSMFilename));
            
            String line = "";
            while((line = br.readLine()) != null)
            {
                //We are looking for an open way tag here 
                if(openWayTag.matcher(line).find())
                {
                    bw.write(line + "\n");
                    
                    boolean nameFound = false;
                    
                    while((line = br.readLine()) != null)
                    {
                        //If we find a way tag, we now want to determine if the way has a name or not
                        if(nameTag.matcher(line).find())
                        {//if it has a name, just append a unique ID number to the end of it
                            nameFound = true;
                            
                            //<tag k="name" v="Chapel Drive"/>
                            
                            match = nameValue.matcher(line);
                            match.find();
                            
                            bw.write("  <tag k=\"name\" v=\""+ match.group(2) +"_" + uniqueNameID +"\"/>\n");
                            
                            uniqueNameID++;
                        }
                        else if(closeWayTag.matcher(line).find())
                        {//Once you reach the close way tag, we need to either add a name or just write the close tag
                            if(!nameFound)
                            {//If a name has not been found, the unique ID will become the name
                                bw.write("  <tag k=\"name\" v=\"" + uniqueNameID +"\"/>\n");
                                
                                uniqueNameID++;
                                
                                bw.write(line + "\n");
                            }
                            else
                            {//Otherwise, just write the close tag
                                bw.write(line + "\n");
                            }
                            
                            break;
                        }
                        else
                        {//If this is just a normal line, just write it
                            bw.write(line + "\n");
                        }                         
                    }                    
                }
                else
                {//If the line is not part of a way, just write it
                    bw.write(line + "\n");
                }
            }
            
            bw.flush();
            
        } catch (IOException ex) {
            Logger.getLogger(DataManager.class.getName()).log(Level.SEVERE, null, ex);
        }finally {
            try {
                br.close();                
                bw.close();
            } catch (IOException ex) {
                Logger.getLogger(DataManager.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
        
        System.out.println("Names and unique IDs successfully added to OSM file");
            
    }
    
    public void generateShapefile(String netFileName, int simID)
    {
        System.out.println("Generating shapefile from .net file and reading in road network edges");
        
        BufferedReader br = null;        
        
        int uid = 1;
        String netOffsetx = "";
        String netOffsety = "";
        int result = 0;
        
        //Now we have to setup our Regex patterns and our Matcher
        Pattern finalLine = Pattern.compile("</net>");
        Pattern location = Pattern.compile("<location");
        Pattern offset = Pattern.compile("(<*.netOffset=\")(.*?),(.*?)(\".*>)");
        Pattern edgeDetect = Pattern.compile("<edge.*name=");
        Pattern edgeInfo = Pattern.compile("(<*.id=)\"(.*?)\"(.*from=)\"(.*?)\"(.*to=)\"(.*?)\"(.*name=)\"(.*?)\"(.*>)");
        Pattern lane = Pattern.compile("<lane");
        Pattern shapefile = Pattern.compile("(<*.id=)\"(.*?)\"(.*?speed=)\"(.*?)\"(.*?length=)\"(.*?)\"(.*?shape=)\"(.*?)\"(.*>)");
        Pattern locationXY = Pattern.compile("(.*),(.*)");
        Matcher match = null;
        
        //Clean out the old roadNetwork
        roadNetwork = new RoadNetworkGraph();
        workingLaneToEdgeList = new HashMap();
        
        try {       
            //First we initialize the buffered reader
            br = new BufferedReader(new FileReader(new File(netFileName)));         
            
            //Now we start reading lines from the net file
            System.out.println("Reading .net file");
            
            String line = "";
            while((line = br.readLine()) != null)
            {
                //We want to stop reading if we hit the </net> tag
                if(finalLine.matcher(line).find())
                {
                    break;
                }
                
                //Need to find the location offset to correctly create the shapefile
                if(location.matcher(line).find())
                {                    
                    match = offset.matcher(line);
                    match.find();
                    
                    netOffsetx = match.group(2);
                    netOffsety = match.group(3);
                    
                    //System.out.println("netOffset: " + netOffsetx + " " + netOffsety);
                }
                
                //Once we get to the edge portion of the file we want to extract the information
                //and store the edges as well as write the geographic data to a shapefile                
                if(edgeDetect.matcher(line).find())
                {                    
                    match = edgeInfo.matcher(line);
                    match.find();
                    
                    String edgeID = match.group(2);
                    String fromID = match.group(4);
                    String toID = match.group(6);
                    String edgeName = match.group(8);
                    
                    //Now we can add the edge to the RoadNetworkGraph
                    roadNetwork.addEdge(edgeID, fromID, toID);
                    
                    //System.out.println("ID: " + edgeID + " FROM: " + fromID + " TO: " + toID + " Name: " + edgeName);
                    //Now we want to harvest the shapefile from each lane
                    line = br.readLine();           
                    while(lane.matcher(line).find())
                    {
                        match = shapefile.matcher(line);
                        match.find();
                        
                        String laneID = match.group(2);                        
                        double speed = Double.parseDouble(match.group(4));
                        double length = Double.parseDouble(match.group(6));
                        String shapefileInfo = match.group(8);
                        
                        //System.out.println("Lane ID: " + laneID + " shapefile: " + shapefileInfo);
                        
                        //Now we need to modify each X,Y coordinate pairs by the netOffsetx,netOffsety and then write them to the file
                        String[] pieces = shapefileInfo.split(" ");

                        //This holds the linestring contents while we create it
                        String lineString = "";
                        
                        for (int i = 0; i < pieces.length; i++) 
                        {
                            match = locationXY.matcher(pieces[i]);
                            match.find();
                            
                            //System.out.println(match.group(1) + " " + match.group(2));
                            
                            //System.out.println(netOffsetx + " " + netOffsety);
                            
                            double tempx = Double.parseDouble(match.group(1)) - Double.parseDouble(netOffsetx);
                            double tempy = Double.parseDouble(match.group(2)) - Double.parseDouble(netOffsety);
                            
                            
                            lineString = lineString.concat(tempx + " " + tempy);
                            
                            if(i+1 < pieces.length)
                            {
                                lineString = lineString.concat(", ");
                            }
                            
                            //System.out.println(tempx + " " + tempy);
                        }
                        
                        //System.out.println("UID: " + uid + " EDGE NAME: '" + edgeName + "' EDGE ID: '" + edgeID + "' LANE ID: '" + laneID + "' 'LINESTRING ( " + lineString + ")'");
                        
                        //Now we insert the entire line into the database
                        result = statement.executeUpdate("INSERT INTO " + networkTableName + " " +
                                                        "(uid,simID,edge_name,edge_id,lane_id,length,speed,geom_text) VALUES (" +
                                                        uid + "," + simID +  ",'" + edgeName + "','" + edgeID + "','" + laneID + "','" + length + "','" + speed + "', 'LINESTRING (" + lineString + ")'" +
                                                        ");");
                        
                        //And then we populate the workingLaneToEdgeList
                        workingLaneToEdgeList.put(laneID, edgeID);
                        
                        uid++;
                        //This stops when line is </edge> so nothing is wasted
                        line = br.readLine(); 
                    }
                }
            }
        } catch (IOException | SQLException ex) {
            Logger.getLogger(DataManager.class.getName()).log(Level.SEVERE, null, ex);
        }finally {
            try {
                br.close();                
            } catch (IOException ex) {
                Logger.getLogger(DataManager.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
        
        //Now we want to generate the geometry for each of the LINESTRINGs
        try {
            result = statement.executeUpdate("UPDATE " + networkTableName + " " +
                    " SET the_geom = subquery.st_transform " +
                    " FROM (SELECT ST_TRANSFORM(ST_GeomFromText(geom_text,32614),4269),uid FROM " + networkTableName + ") AS subquery" +
                    " WHERE " + networkTableName + ".uid=subquery.uid;");
            
        } catch (SQLException ex) {
            Logger.getLogger(DataManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        System.out.println("Shapefiles successfully added to database");       
    }
    
    public void collectRoadNetworkEdges(String netFileName, int simID)
    {
        System.out.println("Reading in road network edges");
        
        BufferedReader br = null;        
        
        int uid = 1;
        String netOffsetx = "";
        String netOffsety = "";
        int result = 0;
        
        //Now we have to setup our Regex patterns and our Matcher
        Pattern finalLine = Pattern.compile("</net>");
        Pattern location = Pattern.compile("<location");
        Pattern offset = Pattern.compile("(<*.netOffset=\")(.*?),(.*?)(\".*>)");
        Pattern edgeDetect = Pattern.compile("<edge.*name=");
        Pattern edgeInfo = Pattern.compile("(<*.id=)\"(.*?)\"(.*from=)\"(.*?)\"(.*to=)\"(.*?)\"(.*name=)\"(.*?)\"(.*>)");
        Pattern lane = Pattern.compile("<lane");
        Pattern shapefile = Pattern.compile("(<*.id=)\"(.*?)\"(.*?speed=)\"(.*?)\"(.*?length=)\"(.*?)\"(.*?shape=)\"(.*?)\"(.*>)");
        Pattern locationXY = Pattern.compile("(.*),(.*)");
        Matcher match = null;

        //Clean out the old roadNetwork
        roadNetwork = new RoadNetworkGraph();
        workingLaneToEdgeList = new HashMap();
        
        try {       
            //First we initialize the buffered reader
            br = new BufferedReader(new FileReader(new File(netFileName)));         
            
            //Now we start reading lines from the net file
            System.out.println("Reading .net file");
            
            String line = "";
            while((line = br.readLine()) != null)
            {
                //We want to stop reading if we hit the </net> tag
                if(finalLine.matcher(line).find())
                {
                    break;
                }
                
                //Need to find the location offset to correctly create the shapefile
                if(location.matcher(line).find())
                {                    
                    match = offset.matcher(line);
                    match.find();
                    
                    netOffsetx = match.group(2);
                    netOffsety = match.group(3);
                    
                    //System.out.println("netOffset: " + netOffsetx + " " + netOffsety);
                }
                
                //Once we get to the edge portion of the file we want to extract the information
                //and store the edges as well as write the geographic data to a shapefile                
                if(edgeDetect.matcher(line).find())
                {                    
                    match = edgeInfo.matcher(line);
                    match.find();
                    
                    String edgeID = match.group(2);
                    String fromID = match.group(4);
                    String toID = match.group(6);
                    String edgeName = match.group(8);
                    
                    //Now we can add the edge to the RoadNetworkGraph
                    roadNetwork.addEdge(edgeID, fromID, toID);
                    
                    //System.out.println("ID: " + edgeID + " FROM: " + fromID + " TO: " + toID + " Name: " + edgeName);
                    //Now we want to harvest the shapefile from each lane
                    line = br.readLine();           
                    while(lane.matcher(line).find())
                    {
                        match = shapefile.matcher(line);
                        match.find();
                        
                        String laneID = match.group(2);                        
 
                        //System.out.println("UID: " + uid + " EDGE NAME: '" + edgeName + "' EDGE ID: '" + edgeID + "' LANE ID: '" + laneID + "' 'LINESTRING ( " + lineString + ")'");

                        workingLaneToEdgeList.put(laneID, edgeID);

                        //This stops when line is </edge> so nothing is wasted
                        line = br.readLine(); 
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(DataManager.class.getName()).log(Level.SEVERE, null, ex);
        }finally {
            try {
                br.close();                
            } catch (IOException ex) {
                Logger.getLogger(DataManager.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
  
        
        System.out.println("Road network edges read in");
    }
    
    /**
     * Runs Tarjan's strongly connected components algorithm.
     * @return ArrayList<ArrayList<String>> of components
     */
    public ArrayList<ArrayList<String>> roadNetworkGraph()
    {
        roadNetwork.createEdgeDual();
        ArrayList<ArrayList<String>> isolatedEdges = roadNetwork.tarjans();
        
        //roadNetwork.printGraphs();
        
        return isolatedEdges;
    }
    
    /**
     * Generates a Biological Emergency Response Plan based on the list of PODs, GSUs, and road network edges. 
     * This contains all of the data necessary to generate a simulation.
     * @param planName 
     * @param avgProcTime 
     */
    public void generateBERP(String planName, HashMap<Integer,Integer> avgProcTime)
    {
        System.out.println("Generating the BERP");
        
        BERP workingBERP;
                
        System.out.println("Generating POD list");
        HashMap<Integer,BERPPOD> workingPODList = this.generatePODList(planName,avgProcTime);
        System.out.println("Generating EDGE list");
        HashMap<String,BERPEdge> workingEdgeList = this.generateEdgeList();
        System.out.println("Generating GSU list");
        HashMap<Integer,BERPGSU> workingGSUList = this.generateGSUList(planName);         
        
        for(int currentKey : workingGSUList.keySet())
        {
            workingPODList.get(workingGSUList.get(currentKey).getAssignedPODID()).addGSU(currentKey,workingGSUList.get(currentKey).getPopulation());

        }
 
        currentBERP = new BERP(workingPODList, workingGSUList, workingEdgeList, workingLaneToEdgeList);
        
        //for(String edge : workingEdgeList.keySet())
        //{            
           //System.out.println(edge + " " + current.getEdgeList().get(edge).getName() + " " + current.getEdgeList().get(edge).getSpeed());
        //}
         
        System.out.println("BERP generated");
    }
    
    /**
     * Creates a HashMap of the GSU ID to its associated population, and modifies the population by however many people per agent were specified.
     * 
     * @param planName
     * @param popPerAgent
     * @return A HashMap where the key is the GSU ID and the value is a population count
    */
    public HashMap<Integer,Integer> GSUPopulations(String planName)
    {
        System.out.println("Running gsupopulation queries");
        
        HashMap<Integer,Integer> gsu2pop = new HashMap();
        
        String sql =    "SELECT logrecno, acs13_5yr_b25002002 as pop \n" +
                        "FROM jhelsing." + planName + "_population \n";        
        
        try
        {
            ResultSet resultset = statement.executeQuery(sql);
            
            while (resultset.next())
            {
                 gsu2pop.put(resultset.getInt("logrecno"), resultset.getInt("pop"));
                
                //System.out.println("GSU: " + resultset.getInt("logrecno") + " POP: " + resultset.getInt("pop"));                
            }            
        } 
        catch (SQLException ex) {
            Logger.getLogger(DataManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return gsu2pop;
    }
    
    /**
     * Creates a HashMap of the GSU ID to its associated road segment edge.
     * 
     * @return A HashMap where the key is the GSU ID and the value is the road segment edge ID
     */
    public HashMap<Integer,String> GSUEdge(String planName)
    {
        System.out.println("Running gsuedge queries");
        HashMap<Integer,String> gsu2edge = new HashMap();
     
        String sql =    "SELECT DISTINCT jhelsing.networkshapefile.edge_id, sub.logrecno\n" +
                        "FROM (\n" +
                        "    SELECT \n" +
                        "    (   SELECT c.edge_id\n" +
                        "        FROM jhelsing.networkshapefile c\n" +
                        "        ORDER BY l.centroid <-> c.the_geom LIMIT 1) \n" +
                        "    AS edgeid,\n" +
                        "    l.logrecno as logrecno\n" +
                        "    FROM       \n" +
                        "    jhelsing." + planName + "_population l\n" +
                        "    ) sub\n" +
                        "INNER JOIN jhelsing.networkshapefile ON sub.edgeid = jhelsing.networkshapefile.edge_id";
        
        try
        {
            ResultSet resultset = statement.executeQuery(sql);
            
            while (resultset.next())
            {
                gsu2edge.put(resultset.getInt("logrecno"), resultset.getString("edge_id"));
                
                //System.out.println("GSU: " + resultset.getInt("logrecno") + " Edge: " + resultset.getString("edge_id"));                
            }            
        } 
        catch (SQLException ex) {
            Logger.getLogger(DataManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return gsu2edge;
    }
    
    /**
     * Creates a HashMap of the GSU ID to its associated POD.
     * 
     * @return A HashMap where the key is the GSU ID and value is its associated POD ID
     */
    public HashMap<Integer,Integer> GSUPOD(String planName)
    {
        System.out.println("Running gsupod queries");
        HashMap<Integer,Integer> gsu2pod = new HashMap();
        
        String sql = "SELECT * FROM jhelsing." + planName + "_block_to_pods";
        
        try
        {
            ResultSet resultset = statement.executeQuery(sql);
            
            while (resultset.next())
            {
                gsu2pod.put(resultset.getInt("block"), resultset.getInt("pod"));
                
                //System.out.println("GSU: " + resultset.getInt("block") + " POD: " + resultset.getInt("pod"));                
            }            
        } 
        catch (SQLException ex) {
            Logger.getLogger(DataManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return gsu2pod;
    }
    
    /**
     * Combines all of the information about a GSU into a BERPGSU, including its population, POD id, and starting edge.
     * Its ID is the logrecno from the database.
     * @param planName
     * @return 
     */
    public HashMap<Integer,BERPGSU> generateGSUList(String planName)
    {
        System.out.println("Generating GSU list");
        HashMap<Integer,BERPGSU> workingList = new HashMap();
        
        HashMap<Integer,Integer> workingPODList = this.GSUPOD(planName);
        HashMap<Integer,String> workingEdgeList = this.GSUEdge(planName);
        HashMap<Integer,Integer> workingPopList = this.GSUPopulations(planName);
        
        for(int currentKey : workingPopList.keySet())
        {
            workingList.put(currentKey, new BERPGSU(currentKey,workingPopList.get(currentKey),workingEdgeList.get(currentKey),workingPODList.get(currentKey)));
        
            //System.out.println("GSU ID: " + currentKey + " POP: " + workingPopList.get(currentKey) + " EDGE: " + workingEdgeList.get(currentKey) + " POD ID: " + workingPODList.get(currentKey));
        }
        
        return workingList;
    }
    
    /**
     * Creates a HashMap of the POD ID to its associated POD.
     * 
     * @return A HashMap where the key is the GSU ID and value is the road segment edge ID
     */
    public HashMap<Integer,String> PODLane(String planName)
    {
        System.out.println("Running POD queries");
        HashMap<Integer,String> pod2edge = new HashMap();
        
        String sql =    "SELECT DISTINCT jhelsing.networkshapefile.lane_id, jhelsing.networkshapefile.edge_id, sub.id\n" +
                        "FROM (\n" +
                        "	SELECT\n" +
                        "        (   	SELECT c.lane_id\n" +
                        "		FROM jhelsing.networkshapefile c\n" +
                        "               WHERE c.length > 61.0\n" +
                        "                ORDER BY pod.location <-> c.the_geom LIMIT 1) \n" +
                        "                AS laneid,\n" +
                        "                pod.id as id\n" +
                        "                FROM       \n" +
                        "                jhelsing." + planName + "_pods pod\n" +
                        "        ) sub\n" +
                        "INNER JOIN jhelsing.networkshapefile ON sub.laneid = jhelsing.networkshapefile.lane_id;";
        
        try
        {
            ResultSet resultset = statement.executeQuery(sql);
            
            while (resultset.next())
            {
                pod2edge.put(resultset.getInt("id"), resultset.getString("lane_id"));
                
                //System.out.println("POD: " + resultset.getInt("id") + " EDGE ID: " + resultset.getString("lane_id"));                
            }            
        } 
        catch (SQLException ex) {
            Logger.getLogger(DataManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return pod2edge;
    }
    
    /**
     * Queries the database for the list of PODs.
     * @param planName
     * @return 
     */
    public HashMap<Integer,BERPPOD> generatePODList(String planName, HashMap<Integer,Integer> avgProcTime)
    {
        System.out.println("Running POD queries");
        HashMap<Integer,BERPPOD> workingPODList = new HashMap();
        
        HashMap<Integer,String> PODLane = this.PODLane(planName);
        
        String sql = "SELECT * FROM jhelsing." + planName + "_pods;";
        
        boolean singleProcTime = true;
        int procTime = avgProcTime.get(1);
        
        if(avgProcTime.size() > 1)
        {
            singleProcTime = false;
        }
        
        try
        {
            ResultSet resultset = statement.executeQuery(sql);    
            
            while (resultset.next())
            {
                if(singleProcTime)
                {
                    avgProcTime.put(resultset.getInt("id"), procTime);
                    workingPODList.put(resultset.getInt("id"), new BERPPOD(resultset.getInt("id"),resultset.getInt("numbooths"),resultset.getString("name_of_pod"),resultset.getString("street_address"),resultset.getString("city"),resultset.getInt("zipcode"), workingLaneToEdgeList.get(PODLane.get(resultset.getInt("id"))), PODLane.get(resultset.getInt("id")),procTime));
                }                                    
                else
                {
                    //System.out.println("POD: " + resultset.getInt("id") + " proctime " + avgProcTime.get(resultset.getInt("id")));
                    workingPODList.put(resultset.getInt("id"), new BERPPOD(resultset.getInt("id"),resultset.getInt("numbooths"),resultset.getString("name_of_pod"),resultset.getString("street_address"),resultset.getString("city"),resultset.getInt("zipcode"), workingLaneToEdgeList.get(PODLane.get(resultset.getInt("id"))), PODLane.get(resultset.getInt("id")),avgProcTime.get(resultset.getInt("id"))));
                }                
                    
                //System.out.println("POD: " + resultset.getInt("id") + " BOOTHS: " + resultset.getInt("numbooths") + " NAME: " + resultset.getString("name_of_pod") + " ADDRESS: " + resultset.getString("street_address") + " CITY: " + resultset.getString("city") + " ZIP: " + resultset.getInt("zipcode") + " LANE ID: " + PODLane.get(resultset.getInt("id")) + " EDGE ID: " + workingLaneToEdgeList.get(PODLane.get(resultset.getInt("id"))));                
            }            
        } 
        catch (SQLException ex) {
            Logger.getLogger(DataManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return workingPODList;
    }
    
    /**
     * Generates all of the BERPEdge objects by querying the networkshapefile table and selecting distinct edges
     * 
     * @return A HasMap of edge ID's to BERPEdges
     */
    public HashMap<String,BERPEdge> generateEdgeList()
    {
        System.out.println("Running edge queries");
        HashMap<String,BERPEdge> workingEdgeList = new HashMap();
        HashMap<String, Integer> laneCounts = new HashMap();
        
        //We select distinct because the table has multiple entries for the same edge due to each edge potentially have multiple lanes
        String edges =  "SELECT DISTINCT edge_id, edge_name, length, speed\n" +
                        "FROM jhelsing.networkshapefile\n" +
                        "ORDER BY edge_id DESC";
        
        //This let's us count up the number of lanes as it produces one entry for every edge and lane
        String lanes =  "SELECT edge_id, edge_name\n" +
                        "FROM jhelsing.networkshapefile\n" +
                        "ORDER BY edge_id DESC";
        
        try
        {
            //First we are going to get the lane counts
            ResultSet resultset = statement.executeQuery(lanes);
            
             while (resultset.next())
            {
                if(laneCounts.containsKey(resultset.getString("edge_id")))
                {
                    laneCounts.put(resultset.getString("edge_id"), laneCounts.get(resultset.getString("edge_id"))+1);
                }
                else
                {
                    laneCounts.put(resultset.getString("edge_id"), 1);
                }
            }
            
            //Now we can get the edges
            resultset = statement.executeQuery(edges);
             
            String currentEdge;
            
            while (resultset.next())
            {
                currentEdge = resultset.getString("edge_id");
                
                
                workingEdgeList.put(currentEdge, new BERPEdge(resultset.getString("edge_id"),resultset.getString("edge_name"),laneCounts.get(resultset.getString("edge_id")),resultset.getDouble("speed"),resultset.getDouble("length")));

                //System.out.println("QUERY LIST EDGE ID: " + resultset.getString("edge_id") + " EDGE NAME: " + resultset.getString("edge_name") + " SPEED: " + resultset.getDouble("speed"));
                //System.out.println("WORKING LIST EDGE ID: " + workingEdgeList.get(currentEdge).getId() + " EDGE NAME: " + workingEdgeList.get(currentEdge).getName() + " SPEED: " + workingEdgeList.get(currentEdge).getSpeed());                
            }            
        } 
        catch (SQLException ex) {
            Logger.getLogger(DataManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return workingEdgeList;
    }
    
    /**
     * Sets all edges that are entry edges for a POD or GSU to being taboo to use for training or testing data.
     */
    public void setProtetectedEdges()
    {
        //First we get all the GSU edges
        for(Integer gsuID : currentBERP.getGsuList().keySet())
        { 
            currentBERP.addProtectedEdge(currentBERP.getGsuList().get(gsuID).getEdge());
        }
        
        for(Integer podID : currentBERP.getPodList().keySet())
        { 
            currentBERP.addProtectedEdge(currentBERP.getPodList().get(podID).getEdge());
            currentBERP.addPODEdge(currentBERP.getPodList().get(podID).getEdge());
        }
    }
    
    public BERP getBERP()
    {
        return currentBERP;
    }
    
    /**
     * @param carIDtoArrivalEdge the carIDtoArrivalEdge to set
     */
    public void setCarIDtoArrivalEdge(HashMap<Integer,String> carIDtoArrivalEdge) {
        this.carIDtoArrivalEdge = carIDtoArrivalEdge;
    }

    /**
     * @return the carIDtoArrivalEdge
     */
    public HashMap<Integer,String> getCarIDtoArrivalEdge() {
        return carIDtoArrivalEdge;
    }
    
        
    public void netFileSpeedLimitAdjustment(String rawFileName, String updatedFileName, int populationPerAgent, double carLength)
    {
        BufferedReader br = null;        
        BufferedWriter bw = null;
        
        double newSpeed = 0;
        DecimalFormat numberFormat = new DecimalFormat("0.00");
        
        Pattern speedTag = Pattern.compile("(<lane.*speed=\")(.*?)(\" length=\")(.*?)(\".*/>)");
        Pattern openInternalEdge = Pattern.compile("function");
        Pattern closeInternalEdge = Pattern.compile("</edge>");
        
        boolean inFunctionEdge = false;
        
        Matcher match = null;
        
        try 
        {
            //First we initialize the buffered reader
            br = new BufferedReader(new FileReader(new File(rawFileName)));
            //Then we initialize the buffered writer
            bw = new BufferedWriter(new FileWriter(updatedFileName));
            
            String line = "";
            while((line = br.readLine()) != null)
            {
                if(openInternalEdge.matcher(line).find())
                {
                    bw.write(line + "\n");
                    inFunctionEdge = true;
                    //System.out.println("ENTERING INTERNAL EDGE");
                }
                else if(closeInternalEdge.matcher(line).find())
                {
                    bw.write(line + "\n");
                    inFunctionEdge = false;
                    //System.out.println("EXITING INTERNAL EDGE");
                }
                else if(speedTag.matcher(line).find() && !inFunctionEdge)
                {//Once you reach the close way tag, we need to either add a name or just write the close tag
                    match = speedTag.matcher(line);
                    match.find();
                    /*
                    System.out.println(match.group(1));
                    System.out.println(match.group(2));
                    System.out.println(match.group(3));
                    System.out.println(match.group(4));
                    System.out.println(match.group(5));
                    */
                    
                    //If the capacity is greater than the scaling factor, do not adjust the speed
                    if((Double.parseDouble(match.group(4))/carLength) >= populationPerAgent)
                    {
                        bw.write("\t\t" + match.group(1) + match.group(2) + match.group(3) + match.group(4) + match.group(5) + "\n");
                    }
                    else
                    {
                        newSpeed = Double.parseDouble(match.group(2))/(populationPerAgent/(Double.parseDouble(match.group(4))/carLength));
                        bw.write("\t\t" + match.group(1) + numberFormat.format(newSpeed) + match.group(3) + match.group(4) + match.group(5) + "\n");
                    }                   
                    
                }                
                else
                {
                    bw.write(line + "\n");
                }
                
            }
            
        } catch (IOException ex) {
            Logger.getLogger(DataManager.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                br.close();                
                bw.close();
            } catch (IOException ex) {
                Logger.getLogger(DataManager.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
    }
}
