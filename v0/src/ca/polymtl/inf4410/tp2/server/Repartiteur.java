package ca.polymtl.inf4410.tp2.server;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

import ca.polymtl.inf4410.tp2.client.CalculServer;
import ca.polymtl.inf4410.tp2.shared.CalculServerInterface;
import ca.polymtl.inf4410.tp2.shared.OverloadedServerException;

public class Repartiteur implements Runnable {

	/**
	 * IP of the remove server
	 */
	private static final String REMOTE_SERVER_IP = "127.0.0.1";

	/**
	 * Error exit IO code
	 */
	private static final int ERROR_IO = -10;

	/**
	 * Error exit RMI code
	 */
	private static final int ERROR_RMI = -20;

	/**
	 * Error exit not bound code
	 */
	private static final int ERROR_NOT_BOUND = -30;

	/**
	 * Error exit access code
	 */
	private static final int ERROR_ACCESS = -40;

	/**
	 * Minimum number of unit operation to do by a server (q value)
	 */
	private static final int MINIMUM_NUMBER_OF_OPERATIONS = 3;
	
	/**
	 * ArrayList to store the calculations to do
	 */
	private ArrayList<String> calculations;

	/**
	 * Semaphore to provide access to the calculations structure
	 */
	private Semaphore calculationsSemaphore;

	/**
	 * ArrayList to store the calculations to verify
	 */
	private ArrayList<String> toVerifyCalculations;
	
	/**
	 * Boolean to know if the repartitor is in safe mode or not
	 */
	private boolean safeMode;
	
	/**
	 * Hashmap to match the servers with its informations
	 */
	private HashMap<CalculServerInterface, CalculServerInfos> servers;
	
	/**
	 * The distant servers used for our project
	 */
	private CalculServerInterface distantServerStub = null;
	private CalculServerInterface distantServerStub2 = null;
	private CalculServerInterface distantServerStub3 = null;
	private int port1 = 5010;
	private int port2 = 5020;
	private int port3 = 5030;

	/**
	 * Public constructor to create a Repartiteur instance.
	 * 
	 * @param distantServerHostname
	 *            The IP used to connect to the remote server
	 */
	public Repartiteur(String distantServerHostname) {
		super();

		calculationsSemaphore = new Semaphore(1);
		calculations = new ArrayList<String>();
		toVerifyCalculations = new ArrayList<>();

		
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		if (distantServerHostname != null) {
			distantServerStub = loadServerStub(distantServerHostname, port1);
			distantServerStub2 = loadServerStub(distantServerHostname, port2);
			distantServerStub3 = loadServerStub(distantServerHostname, port3);
		}
	}

	public static void main(String[] args) {

		// Creation of the repartitor instance
		Repartiteur repartiteur = new Repartiteur(REMOTE_SERVER_IP);

		System.out.println("Lancement du repartiteur ...");

		// Check is safemode is enable or not
		if (args[1] == "-S") {
			repartiteur.setSafeMode(true);
			System.out.println("Safe mode d�tect�.");
		}

		// Start repartitor's job
		repartiteur.runRepartitor();

	}

	/**
	 * Private method to load the server
	 * 
	 * @param hostname
	 * @return
	 */
	private CalculServerInterface loadServerStub(String hostname, int port) {
		CalculServerInterface stub = null;

		try {
			Registry registry = LocateRegistry.getRegistry(hostname);
			stub = (CalculServerInterface) registry.lookup("server" + port);
			try {
				System.out.println(InetAddress.getLocalHost().getHostName());
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (NotBoundException e) {
			System.err.println("Erreur: Le nom  " + e.getMessage() + "  n est pas defini dans le registre.");
			System.exit(ERROR_NOT_BOUND);
		} catch (AccessException e) {
			System.err.println("Erreur: " + e.getMessage());
			System.exit(ERROR_ACCESS);
		} catch (RemoteException e) {
			System.err.println("Erreur: " + e.getMessage());
			System.exit(ERROR_RMI);
		}

		return stub;
	}

	private void runRepartitor() {

		String commande = null;
		String split[] = null;

		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		
		System.out.println("Attente des commandes ...");

		try {
			while ((commande = reader.readLine()) != null) {
				split = commande.split(" ");
				String command = split[0];
				String arg1 = split[1];

				if (command.equals("loadFile")) {
					// TODO : Start to call the calculous servers
					try {
						storeCalculations(arg1);
					} catch (IOException e) {
						System.err.println("Probleme d'acces au fichier : \"" + arg1 + "\".");
					}
				} else if (command.equals("compute")) { 
					// TODO : Do the job
					execute(arg1);
				}
			}
		} catch (IOException e) {
			System.err.println("Erreur dans la lecture du flux d'entree sortie.");
			e.printStackTrace();
			System.exit(ERROR_IO);
		}
	}

	private void execute(String filename) throws IOException {

		Semaphore semaphore = new Semaphore(1);

		String line = null;
		int compteur = 0;
		String message[] = new String[3];
		int result = 0;

		FileInputStream fis = new FileInputStream(filename);
		InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
		BufferedReader br = new BufferedReader(isr);

		while ((line = br.readLine()) != null) {

			// Buffer pour envoie des calculs a effectuer au serveur
			message[compteur] = line;

			if (compteur == 2) {
				// Essai d'envoie du message
				try {
					result += calculate(distantServerStub, message);
				} catch (RemoteException e) {
					// Erreur RMI
					System.err.println("Erreur RMI");
					e.printStackTrace();
					System.exit(ERROR_RMI);
				} catch (OverloadedServerException e) {
					// Erreur surcharge serveur
					System.out.println("Server surcharge !");
					System.out.println("Renvoie du message vers un autre client !");
					// TODO : implem
				}

				// Reset du compteur
				compteur = 0;
				// Appplication du modulo
				result = result % 4000;
			} else {
				// On a pas atteint le nombre de message a send, on continue a
				// lire le fichier
				compteur++;
			}
		}

		// Cas des fins de fichier et ou il reste des calculs a effectuer
		// mais que ce n'est pas un multiple valable pour la capacite du serveur
		if (compteur != 0) {
			String finalMessage[] = new String[compteur];

			// Remplissage du message final
			for (int i = 0; i < compteur; i++) {
				finalMessage[i] = message[i];
			}

			// Envoie du message
			try {
				// TODO fix server dispatch
				result += calculate(distantServerStub, finalMessage);
			} catch (OverloadedServerException e) {
				// Erreur surcharge serveur
				System.out.println("Server surcharge !");
				System.out.println("Renvoie du message vers un autre client !");
				// TODO : implem
			}

			// Final modulo
			result = result % 4000;
		}

		System.out.println("Resultat des calculs : " + result);
		br.close();
	}

	/**
	 * Private method to store the initial calculations to do
	 * 
	 * @param filename
	 * @throws IOException
	 */
	private void storeCalculations(String filename) throws IOException {
		// Some declarations ...
		String line = null;
		FileInputStream fis = new FileInputStream(filename);
		InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
		BufferedReader br = new BufferedReader(isr);

		// Add each line of the file to the datastructure
		while ((line = br.readLine()) != null) {
			calculations.add(line);
		}
		
		// Close the buffer 
		br.close();
	}

	private int calculate(CalculServerInterface server, String operations[]) throws RemoteException, OverloadedServerException {
		return server.calculate(operations);
	}

	/**
	 * Get some calculous from the calculous datastructure
	 * @return a minimum number of calculous to do
	 */
	private String[] getSomeCalculous() {

		// Protect the datastructure by using semaphore
		try {
			calculationsSemaphore.acquire(1);
		} catch (InterruptedException e) {
			System.err.println("Probleme de semaphore.");
		}

		String[] calculous = new String[3];
		int compteur = 0;
		
		// Store the calculous information into an array
		Iterator<String> i = calculations.iterator();
		while (i.hasNext()) {
			
			// Get the calculous from the calculations datastructure
			calculous[compteur] = i.next();
			// Remove it from the calculations datastructure
			i.remove();
			
			// Set the apropriate number of minimum operations to do
			if (compteur != MINIMUM_NUMBER_OF_OPERATIONS) {
				compteur++;
			} else {
				break;
			}
		}
		
		// Release the token from the semaphore
		calculationsSemaphore.release(1);
		
		return calculous;
	}

	@Override
	public void run() {
		// TODO : implements thread logic
		int result = -1;
		
		// Tant qu'il y a des calculs a faire
		while(!calculations.isEmpty()) {
			// On r�cup�re la liste des calculs
			String[] calculous = getSomeCalculous();
			// On cr�� une statistique associ�e
			CalculServerInfos css = new CalculServerInfos(calculous);
			
			
			try {
				result = calculate(distantServerStub, calculous);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (OverloadedServerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}

	/**
	 * SafeMode setter
	 * 
	 * @param true
	 *            if the mode is enable, false otherwise
	 */
	public void setSafeMode(boolean b) {
		this.safeMode = b;
	}

}
