package ca.polymtl.inf4410.tp2.server;

import ca.polymtl.inf4410.tp2.shared.CalculousServerInterface;
import java.util.ArrayList;
import java.util.Arrays;

public class Task {

	public ArrayList<String> calculousList;
        public ArrayList<String> calculousToCheck;  
        public ArrayList<String> calculousChecked;  
        
	private final int operationNumberToCheck;
        private int operationNumberChecked;    
        
	public CalculousServerInterface firstServer;	
        public CalculousServerInterface secondServer;

	private final int firstResult;
        private int secondResult;
        
        /**
         * 
         * @param server
         * @param calculous
         * @param result
         * @param operationNumber 
         */
	public Task(CalculousServerInterface server, String[] calculous, int result, int operationNumber) {
                // init the lists
                calculousList = new ArrayList<>();                
                calculousList.addAll(Arrays.asList(calculous));                
                calculousToCheck = new ArrayList<>(calculousList);
                calculousChecked = new ArrayList<>();
                
		this.firstServer = server;
                this.secondServer = null;
                
		this.firstResult = result;
                this.secondResult = 0;
                
                this.operationNumberToCheck = operationNumber;                
                this.operationNumberChecked = 0;                                              
	}
	        
        /**
         * 
         * @param result
         * @param list
         * @param numberOfOperations 
         */
        public void addVerificationResult(int result, String[] list, int numberOfOperations){
                secondResult += result;
                secondResult %= 4000;
                calculousChecked.addAll(Arrays.asList(list));
                operationNumberChecked += numberOfOperations;
        }
        
        /**
         * 
         * @return 
         */
        public boolean shouldBeCheckedBy(CalculousServerInterface server){ 
            return firstServer != server && secondServer == null;
        } 
        
        /**
         * 
         * @return 
         */
        public int getSecondResult(){ return secondResult; }
        
        /**
         * 
         * @return 
         */
        public int getInitialOperationNumber(){ return operationNumberToCheck; }
	 
        /**
         * 
         * @return 
         */
        public boolean isTaskCorrect(){ return secondResult == firstResult; }
        
        /**
         * 
         * @return 
         */
        public boolean isTaskVerified(){
                return operationNumberChecked == operationNumberToCheck && calculousToCheck.isEmpty();
        }
        
        /**
         * 
         * @param server 
         */
        public void attributeVerificationToServer(CalculousServerInterface server){
                secondServer = server;
        }

        /**
         * 
         * @param nextCapacity
         * @return 
         */
        public String[] getCalculous(int nextCapacity) {            
                ArrayList<String> temp = new ArrayList<>();              
                for (int i = 0; i < nextCapacity && i < calculousToCheck.size(); i++) {  
                    temp.add(calculousToCheck.remove(i));                     
                }
                String [] ret = new String[temp.size()];
                ret = temp.toArray(ret);
                return ret;
        }
        
        /**
         * 
         * @param calcs 
         */
        public void pushBackCalculousToTask(String [] calcs) {	
            calculousToCheck.addAll(Arrays.asList(calcs));
        }     

        @Override
        public String toString() {
            return "TASK : \r\n"
                    + "operationNumberChecked is " + operationNumberChecked + "\r\n"
                    + "operationNumberToCheck is " + operationNumberToCheck + "\r\n"
                    + "firstResult is " + firstResult + "\r\n"
                    + "secondResult is " + secondResult + "\r\n";
        }
        
        
        
}