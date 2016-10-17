package ca.polymtl.inf4410.tp2.server;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.rmi.AccessException;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import ca.polymtl.inf4410.tp2.shared.CalculServerInterface;
import ca.polymtl.inf4410.tp2.shared.OverloadedServerException;

public class Repartiteur {

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
	 * The distant server used for our project
	 */
	private CalculServerInterface distantServerStub = null;

	/**
	 * Public constructor to create a Repartiteur instance.
	 * 
	 * @param distantServerHostname
	 *            The IP used to connect to the remote server
	 */
	public Repartiteur(String distantServerHostname) {

		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		if (distantServerHostname != null) {
			distantServerStub = loadServerStub(distantServerHostname);
		}
	}

	public static void main(String[] args) {

		Repartiteur repartiteur = new Repartiteur(REMOTE_SERVER_IP);

		System.out.println("Lancement du repartiteur ...");
		repartiteur.run();

	}

	/**
	 * Private method to load the server
	 * 
	 * @param hostname
	 * @return
	 */
	private CalculServerInterface loadServerStub(String hostname) {
		CalculServerInterface stub = null;

		try {
			Registry registry = LocateRegistry.getRegistry(hostname);
			stub = (CalculServerInterface) registry.lookup("server");
		} catch (NotBoundException e) {
			System.err.println("Erreur: Le nom  " + e.getMessage()
					+ "  n est pas defini dans le registre.");
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

	private void run() {

		System.out.println("Attente des commandes ...");

		String commande = null;
		String split[] = null;

		BufferedReader reader = new BufferedReader(new InputStreamReader(
				System.in));

		try {
			while ((commande = reader.readLine()) != null) {

				split = commande.split(" ");

				if (split[0].equals("compute")) {
					// Start to call the calculous servers
					// TODO
					try {
						execute(split[1]);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		} catch (IOException e) {
			System.err
					.println("Erreur dans la lecture du flux d'entree sortie.");
			e.printStackTrace();
			System.exit(ERROR_IO);
		}
	}

	private void execute(String filename) throws IOException {

		String line = null;
		int compteur = 0;
		String message[] = null;

		FileInputStream fis = new FileInputStream(filename);
		InputStreamReader isr = new InputStreamReader(fis,
				Charset.forName("UTF-8"));
		BufferedReader br = new BufferedReader(isr);

		while ((line = br.readLine()) != null) {

			message[compteur] = line;

			if (compteur == 2) {
				// Essai d'envoie du message
				try {
					calculate(message);
				} catch (RemoteException e) {
					// Erreur RMI
					System.err.println("Erreur RMI");
					e.printStackTrace();
					System.exit(ERROR_RMI);
				} catch (OverloadedServerException e) {
					// Erreur surcharge serveur
					System.out.println("Server surcharge !");
					System.out
							.println("Renvoie du message vers un autre client !");
					// TODO : implem
				}

				// Reset du compteur
				compteur = 0;
			} else {
				// On a pas atteint le nombre de message a send, on continue a
				// lire le fichier
				compteur++;
			}

		}
	}

	private void calculate(String message[]) throws RemoteException,
			OverloadedServerException {
		distantServerStub.calculate(message);
	}

}