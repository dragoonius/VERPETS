/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package verpets4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

/**
 *
 * @author JHelsing
 */
public class RoadNetworkGraph {
    
    private ArrayList<ArrayList<Integer>> adjacencyList;
    private HashMap<String,JunctionNode> junctionNodeIDtoJunctionNode;
    private HashMap<String,RoadNode> roadNodeIDtoRoadNode;
    
    //We both hashes contain the same data but the edgeIDs are hashed for a quick existence lookup
    private HashMap<Integer,String> IDToBERPEdgeID;
    private HashMap<String,Integer> BERPEdgeIDtoID;
    
    //Used for Tarjan's Algorithm
    private int numberOfEdges;
    private int time;
    private int[] discovered;
    private int[] lowValue;
    private boolean[] stackmember;
    private Stack<Integer> nodesList;
    private ArrayList<ArrayList<String>> isolatedEdges;
    
    
    
    public RoadNetworkGraph( )
    {
        adjacencyList = new ArrayList();
        IDToBERPEdgeID = new HashMap();
        BERPEdgeIDtoID = new HashMap();
        junctionNodeIDtoJunctionNode = new HashMap();
        roadNodeIDtoRoadNode = new HashMap();
        numberOfEdges = 0;
        time = 0;
    }
    
    public void addEdge(String edgeID, String fromNodeID, String toNodeID)
    {
        //If we have seen this node, just add the edge, otherwise make an entry for it and then add it
        if(junctionNodeIDtoJunctionNode.containsKey(fromNodeID))
        {
            junctionNodeIDtoJunctionNode.get(fromNodeID).addOutEdge(edgeID);           
        }
        else
        {
            junctionNodeIDtoJunctionNode.put(fromNodeID, new JunctionNode(fromNodeID));
            junctionNodeIDtoJunctionNode.get(fromNodeID).addOutEdge(edgeID);
        }
        
        if(junctionNodeIDtoJunctionNode.containsKey(toNodeID))
        {
            junctionNodeIDtoJunctionNode.get(toNodeID).addInEdge(edgeID);
        }
        else
        {
            junctionNodeIDtoJunctionNode.put(toNodeID, new JunctionNode(toNodeID));
            junctionNodeIDtoJunctionNode.get(toNodeID).addInEdge(edgeID);
        }
    }
    
    public void createEdgeDual()
    {        
        ArrayList<String> inRoads;
        ArrayList<String> outRoads;
        
        //First we iterate through all the junction nodes
        for(String currentKey : junctionNodeIDtoJunctionNode.keySet() )
        {
            
            inRoads = junctionNodeIDtoJunctionNode.get(currentKey).getInEdges();
            outRoads = junctionNodeIDtoJunctionNode.get(currentKey).getOutEdges();
            
            //For each junction node, we want to first get all of the incoming road edges added
            for(int in = 0; in < inRoads.size(); in++)
            {
                //Check if the incoming road is in the roadNodeIDtoID hash
                if(!(roadNodeIDtoRoadNode.containsKey(inRoads.get(in))))
                {
                    roadNodeIDtoRoadNode.put(inRoads.get(in), new RoadNode(inRoads.get(in)));
                    numberOfEdges++;
                }
                
                roadNodeIDtoRoadNode.get(inRoads.get(in)).addOutJunction(currentKey);
                
                //Then we can connect it to the outroads and create the links
                for(int out = 0; out < outRoads.size(); out++)
                {
                    //Check if the outgoing road is in the roadNodeIDtoID hash
                    if(!(roadNodeIDtoRoadNode.containsKey(outRoads.get(out))))
                    {
                        roadNodeIDtoRoadNode.put(outRoads.get(out), new RoadNode(outRoads.get(out)));
                        numberOfEdges++;
                    }
                    
                    roadNodeIDtoRoadNode.get(outRoads.get(out)).addInJunction(currentKey);
                }
            }            
        }
    }
    
    //Courtesy of http://www.geeksforgeeks.org/tarjan-algorithm-find-strongly-connected-components/
    public ArrayList<ArrayList<String>> tarjans()
    {
        System.out.println("Starting Tarjan's");
        this.createAdjacencyList();
        
        discovered = new int[numberOfEdges];
        lowValue = new int[numberOfEdges];
        stackmember = new boolean[numberOfEdges];
        nodesList = new Stack();
        isolatedEdges = new ArrayList();
        
        for(int i = 0; i < numberOfEdges; i++)
        {
            discovered[i] = -1;
            lowValue[i] = -1;
            stackmember[i] = false;            
        }
        
        for(int i = 0; i < numberOfEdges; i++)
        {
            if(discovered[i] == -1)
            {
                DFS(i);
            }
        }
        System.out.println("Finished Tarjan's");
        
        return isolatedEdges;
    }
    
    private void DFS(int u)
    {
        discovered[u] = ++time;
        lowValue[u] = time;
        nodesList.push(u);
        stackmember[u] = true;
        
        for(int i = 0; i < adjacencyList.get(u).size(); i++)
        {
            int temp = adjacencyList.get(u).get(i);
            
            if(discovered[temp] == -1)
            {
                DFS(temp);                
                
                lowValue[u] = Math.min(lowValue[u], lowValue[temp]);
            }
            else if(stackmember[temp])
            {
                lowValue[u] = Math.min(lowValue[u], discovered[temp]);
            }            
        }
        
        int temp = 0;
        if(lowValue[u] == discovered[u])
        {
            ArrayList<String> tempList = new ArrayList();
            
            while(nodesList.peek() != u)
            {
                temp = nodesList.pop();
                tempList.add(IDToBERPEdgeID.get(temp));
                //System.out.print(IDToBERPEdgeID.get(temp) + " ");
                stackmember[temp] = false;
                
            }
            temp = nodesList.pop();
            
            tempList.add(IDToBERPEdgeID.get(temp));
            //System.out.println(IDToBERPEdgeID.get(temp));
            stackmember[temp] = false;
            
            isolatedEdges.add(tempList);
        }
    }
    
    private void createAdjacencyList()
    {
        ArrayList<String> inRoads;
        ArrayList<String> outRoads;
        
        //Initialize the Adjacency List
        for(int i = 0; i < numberOfEdges; i++)
        {
            adjacencyList.add(new ArrayList());
        }
        
        int uniqueID = 0;
        
        //First we iterate through all the junction nodes
        for(String currentKey : junctionNodeIDtoJunctionNode.keySet() )
        {
            
            inRoads = junctionNodeIDtoJunctionNode.get(currentKey).getInEdges();
            outRoads = junctionNodeIDtoJunctionNode.get(currentKey).getOutEdges();
            
            for(int in = 0; in < inRoads.size(); in++)
            {
                //Check if the incoming road is in the roadNodeIDtoID hash
                if(!(BERPEdgeIDtoID.containsKey(inRoads.get(in))))
                {
                    BERPEdgeIDtoID.put(inRoads.get(in), uniqueID);
                    IDToBERPEdgeID.put(uniqueID, inRoads.get(in));
                    uniqueID++;
                }
                
                //Then we can connect it to the outroads and create the links
                for(int out = 0; out < outRoads.size(); out++)
                {
                    //Check if the outgoing road is in the roadNodeIDtoID hash
                    if(!(BERPEdgeIDtoID.containsKey(outRoads.get(out))))
                    {
                        BERPEdgeIDtoID.put(outRoads.get(out), uniqueID);
                        IDToBERPEdgeID.put(uniqueID, outRoads.get(out));
                        uniqueID++;
                    }
                    
                    //System.out.println(BERPEdgeIDtoID.get(inRoads.get(in)));
                    
                    adjacencyList.get(BERPEdgeIDtoID.get(inRoads.get(in))).add(BERPEdgeIDtoID.get(outRoads.get(out)));
                }
                
            }              
        }
    }
    
    public void printGraphs()
    {
        System.out.println("Junctions as nodes");
        for(String currentKey : junctionNodeIDtoJunctionNode.keySet() )
        {
            System.out.print("Node: " + currentKey);
            System.out.print(" Incoming roads: ");
            
            for(int i = 0; i < junctionNodeIDtoJunctionNode.get(currentKey).getInEdges().size(); i++)
            {
                System.out.print(junctionNodeIDtoJunctionNode.get(currentKey).getInEdges().get(i) + " ");
            }
            
            System.out.print(" Outgoing roads: ");
            
            for(int i = 0; i < junctionNodeIDtoJunctionNode.get(currentKey).getOutEdges().size(); i++)
            {
                System.out.print(junctionNodeIDtoJunctionNode.get(currentKey).getOutEdges().get(i) + " ");
            }
            
            System.out.println();
        }
        
        System.out.println("Roads as nodes");
        for(String currentKey : roadNodeIDtoRoadNode.keySet() )
        {
            System.out.print("Node: " + currentKey);
            System.out.print(" Incoming junctions: ");
            
            for(int i = 0; i < roadNodeIDtoRoadNode.get(currentKey).getInJunctions().size(); i++)
            {
                System.out.print(roadNodeIDtoRoadNode.get(currentKey).getInJunctions().get(i) + " ");
            }
            
            System.out.print(" Outgoing junctions: ");
            
            for(int i = 0; i < roadNodeIDtoRoadNode.get(currentKey).getOutJunctions().size(); i++)
            {
                System.out.print(roadNodeIDtoRoadNode.get(currentKey).getOutJunctions().get(i) + " ");
            }
            
            System.out.println();
        }
        
        System.out.println("Adjacency List");
        int biggestList = 0;
        for(int i = 0; i < adjacencyList.size(); i++)
        {
            System.out.print("Edge " + IDToBERPEdgeID.get(i) + " : " + i + " ");
            
            if(adjacencyList.get(i).size() > biggestList)
                biggestList = adjacencyList.get(i).size();
        }
        System.out.println();
        for(int i = 0; i < biggestList; i++)
        {
            for(int j = 0; j < adjacencyList.size(); j++)
            {
                if(i < adjacencyList.get(j).size())
                {
                    System.out.print(" " + IDToBERPEdgeID.get(adjacencyList.get(j).get(i)) + " ");
                }
                else
                {
                    System.out.print(" N ");
                }
            }
            
            System.out.println();
        }
        
    }
    
    private class JunctionNode 
    {
        private ArrayList<String> inEdges;
        private ArrayList<String> outEdges;
        private String nodeID;

        public JunctionNode(String nodeID)
        {
            this.nodeID = nodeID;
            inEdges = new ArrayList();
            outEdges = new ArrayList();
        }
            
        public void addInEdge(String inEdge)
        {
            inEdges.add(inEdge);
        }
            
        public void addOutEdge(String outEdge)
        {
            outEdges.add(outEdge);
        }

        /**
         * @return the inEdges
         */
        public ArrayList<String> getInEdges() {
            return inEdges;
        }

        /**
         * @return the outEdges
         */
        public ArrayList<String> getOutEdges() {
            return outEdges;
        }

        /**
         * @return the nodeID
         */
        public String getNodeID() {
            return nodeID;
        }            
    }   

    private class RoadNode 
    {
        private ArrayList<String> inJunctions;
        private ArrayList<String> outJunctions;
        private String nodeID;

        public RoadNode(String nodeID)
        {
            this.nodeID = nodeID;
            inJunctions = new ArrayList();
            outJunctions = new ArrayList();
        }
            
        public void addInJunction(String inJunction)
        {
            inJunctions.add(inJunction);
        }
            
        public void addOutJunction(String outJunction)
        {
            outJunctions.add(outJunction);
        }

        /**
         * @return the inJunctions
         */
        public ArrayList<String> getInJunctions() {
            return inJunctions;
        }

        /**
         * @return the outJunctions
         */
        public ArrayList<String> getOutJunctions() {
            return outJunctions;
        }

        /**
         * @return the nodeID
         */
        public String getNodeID() {
            return nodeID;
        }
        
        
    }
}

