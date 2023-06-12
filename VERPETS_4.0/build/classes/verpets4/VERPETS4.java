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
public class VERPETS4 {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println("Running VERPETS Version: 4.0");
        
        MasterController mc = new MasterController(); 
        
        //First we validate the baseline to see if the plan works.
        mc.validatePlan();

        //Next test the sensitivity of the network to congestion.
        //mc.sensitivityAnalysis();
        
        //Finally, clean up everything.
        //mc.cleanUpFiles();
        
        System.out.println("Complete");
    }
    
}
