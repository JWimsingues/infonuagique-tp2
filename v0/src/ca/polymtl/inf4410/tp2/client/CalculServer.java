/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.polymtl.inf4410.tp2.client;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Random;

import ca.polymtl.inf4410.tp2.shared.CalculServerInterface;
import ca.polymtl.inf4410.tp2.shared.OverloadedServerException;

/**
 * CalculServer implement the server that will procceed calculation
 * 
 * @author robinroyer
 */
public class CalculServer implements CalculServerInterface{

    
    
    /**
     * Maximum capacity of the instance of CalculServer
     */
    private int capacity;
    
    /**
     * Percentage of trusted return message
     */
    private int confidence;
    
    
    /**
    * Main to run the server.
    * 
    * @param args
    */
    public static void main(String[] args) {
        //TODO: add args to constructor
        CalculServer server = new CalculServer(args);
        server.run();
    }
    
    /**
    * Main method to run the server.
    */
    private void run() {
        if (System.getSecurityManager() == null) {
                System.setSecurityManager(new SecurityManager());
        }

        try {
                CalculServerInterface stub = (CalculServerInterface) UnicastRemoteObject.exportObject(this, 0);
                Registry registry = LocateRegistry.getRegistry();
                registry.rebind("server", stub);
                System.out.println("Server ready.");
        } catch (ConnectException e) {
                System.err.println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lance ?");
                System.err.println();
                System.err.println("Erreur: " + e.getMessage());
        } catch (Exception e) {
                System.err.println("Erreur: " + e.getMessage());
        }
    }
    
    
    /**
     * Constructor from Args
     * 
     * @param Args Args where we should fin
     */
    
    public CalculServer(String [] Args){
        
    }
    
    /**
     * Constructor with confidence and capacity
     * @param confidence
     * @param capacity
     */
    private CalculServer( int confidence, int capacity){        
        this.confidence = confidence;
        this.capacity = capacity;
    }  
    
    /**
     * Default Constructor
     */
    public CalculServer(){        
    }    

    @Override
    public int calculate(String[] operations) throws RemoteException, OverloadedServerException {
        // TODO: implement calcul from operations
        return -1;
    }
    
    
    
    /**
     * Test if the server is overloaded, in order to accept or refuse the opertions
     * 
     * @param operationNumber  Number of operations sent to the CalculServer
     * @return True if the operations is not be accepted
     */
    private boolean isOverloaded(int operationNumber){                
        //Algorythm to calculate refusingRate:  T = (U-Q)/(4*Q) * 100                 
        double refusingRate = (operationNumber - capacity) / (4 * capacity) * 100;        
        // Using a random generator for refusing
        Random rand = new Random(System.currentTimeMillis());        
        return 100 * rand.nextDouble() > refusingRate;  
    }
    
}
