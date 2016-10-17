package ca.polymtl.inf4410.tp2.client;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import ca.polymtl.inf4410.tp2.shared.File;
import ca.polymtl.inf4410.tp2.shared.Header;
import ca.polymtl.inf4410.tp2.shared.ServerInterface;

/**
 * Client class of the project. Use to run the client side.
 * 
 * @author Jeremy
 * 
 */
public class Client {

	/**
	 * Path of the file use to store the user id.
	 */
	private static final String FILE_PATH = ".user";

	/**
	 * IP of the remove server
	 */
	private static final String REMOTE_SERVER_IP = "127.0.0.1";

	/**
	 * Success id code, the program ended in a good way.
	 */
	private static final int EXIT_SUCCESS = 0;

	/**
	 * Error id code in case of problem during the push process.
	 */
	private static final int ID_PUSH_CANCEL = -10;

	/**
	 * Error id code in case of problem during the create process.
	 */
	private static final int ID_CREATE_CANCEL = -30;

	/**
	 * Error id code in case of problem during the RMI call process.
	 */
	private static final int ID_RMI_ERROR = -50;

	/**
	 * Error IO exit code.
	 */
	private static final int ID_IO_ERROR = -80;

	/**
	 * Error id not bound.
	 */
	private static final int ID_NOT_BOUND_ERROR = -60;

	/**
	 * Error id access.
	 */
	private static final int ID_ACCESS_ERROR = -70;

	/**
	 * Exit id code in case of a cancel operation during the get process.
	 */
	private static final int ID_GET_CANCEL = 10;

	/**
	 * Error id code in case of invalid argument.
	 */
	private static final int ID_INVALID_ARGUMENT = -20;

	/**
	 * The distant server used for our project
	 */
	private ServerInterface distantServerStub = null;

	/**
	 * Main of the program. Takes one or two arguments depending on the command
	 * you want to run.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Client client = new Client(REMOTE_SERVER_IP);

		String action = args[0];
		String filename = "";

		// Catch the command
		switch (action) {
		// Do the appropriate choice and do the job
		case "create":
			filename = checkFirstArgument(args);
			client.createFile(filename);
			break;
		case "list":
			client.displayList();
			break;
		case "syncLocalDir":
			client.synchroLocalDirectory();
			break;
		case "get":
			filename = checkFirstArgument(args);
			client.getFile(filename);
			break;
		case "lock":
			filename = checkFirstArgument(args);
			client.lockFile(filename);
			break;
		case "push":
			filename = checkFirstArgument(args);
			client.pushFile(filename);
			break;
		default:
			System.out.println("Commande introuvable.");
			System.out.println("Liste des commandes disponibles :");
			System.out.println("./client create file");
			System.out.println("./client list");
			System.out.println("./client lock file");
			System.out.println("./client get file");
			System.out.println("./client push file");
			System.out.println("./client syncLocalDir");
			System.exit(ID_INVALID_ARGUMENT);
		}

		// Best practice exit success
		System.exit(EXIT_SUCCESS);
	}

	/**
	 * Public constructor to create a client instance.
	 * 
	 * @param distantServerHostname
	 *            The IP used to connect to the remote server
	 */
	public Client(String distantServerHostname) {
		super();

		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		if (distantServerHostname != null) {
			distantServerStub = loadServerStub(distantServerHostname);
		}
	}

	/**
	 * Private method to load the server
	 * 
	 * @param hostname
	 * @return
	 */
	private ServerInterface loadServerStub(String hostname) {
		ServerInterface stub = null;

		try {
			Registry registry = LocateRegistry.getRegistry(hostname);
			stub = (ServerInterface) registry.lookup("server");
		} catch (NotBoundException e) {
			System.err.println("Erreur: Le nom  " + e.getMessage() + "  n est pas defini dans le registre.");
			System.exit(ID_NOT_BOUND_ERROR);
		} catch (AccessException e) {
			System.err.println("Erreur: " + e.getMessage());
			System.exit(ID_ACCESS_ERROR);
		} catch (RemoteException e) {
			System.err.println("Erreur: " + e.getMessage());
			System.exit(ID_RMI_ERROR);
		}

		return stub;
	}

	/**
	 * Private create method to create a file on the remove server.
	 * 
	 * @param filename
	 *            : the name of the file you want to create
	 */
	private void createFile(String filename) {

		try {
			// Call on the server side
			distantServerStub.create(filename);
		} catch (RemoteException e) {
			System.err.println("Erreur: " + e.getMessage());
			System.exit(ID_RMI_ERROR);
		} catch (FileAlreadyExistsException e) {
			System.err.println(e.getMessage());
			System.exit(ID_CREATE_CANCEL);
		}

		System.out.println("Fichier " + filename + " ajoute.");

	}

	/**
	 * Private method to display the list
	 */
	private void displayList() {
		try {
			ArrayList<Header> list = distantServerStub.list();
			if (list.isEmpty()) {
				System.out.println("Il n'y a pas encore de fichier dans la liste !");
			} else {
				// Display all the element of the list
				for (Header h : list) {
					System.out.println(h);
				}
			}
			System.out.println(list.size() + " fichier(s).");
		} catch (RemoteException e) {
			System.err.println("Erreur : " + e.getMessage());
		}
	}

	/**
	 * Private method to push a local file onto the server.
	 * 
	 * @param filename
	 *            : the name of the file
	 */
	private void pushFile(String filename) {
		// Get the local client id
		int clientId = getUserId();

		// Get the local file
		byte[] content = null;
		try {
			content = getLocalFile(filename);
		} catch (NoSuchFileException e) {
			// Canceled if the file does not exist locally
			System.out.println("Fichier non detecte en local.");
			System.out.println("Annulation de l'operation de televersement du fichier \"" + filename + "\".");
			System.exit(ID_PUSH_CANCEL);
		}

		try {
			// Push the file on the remove server
			distantServerStub.push(filename, content, clientId);
			System.out.println("Le fichier \"" + filename + "\" a bien ete televerse.");
		} catch (RemoteException e) {
			System.err.println("Erreur RMI :" + e.getMessage());
			System.exit(ID_RMI_ERROR);
		} catch (NoSuchFileException er) {
			System.err.println("Erreur NoSuchFileException :" + er.getMessage());
			System.exit(ID_PUSH_CANCEL);
		}
	}

	/**
	 * Lock a file on the remte server
	 * 
	 * @param filename
	 *            : the name of the file to lock
	 */
	private void lockFile(String filename) {
		// Get the local client id
		int clientId = getUserId();

		// Try to get the local file
		byte[] data = null;
		try {
			data = getLocalFile(filename);
		} catch (NoSuchFileException e) {
			// Verification de la presence du fichier local
			System.out.println("Fichier non detecte en local.");
			System.out.println("Telechargement du fichier depuis le server.");
			try {
				data = distantServerStub.get(filename, null).getContent().getContent();
			} catch (RemoteException re) {
				System.err.println("Erreur RMI : " + re.getMessage());
				System.exit(ID_RMI_ERROR);
			}
		}

		// Compute the local checksum
		byte[] checksum = computeChecksum(data);

		// Lock the file
		File result = null;
		try {
			result = distantServerStub.lock(filename, clientId, checksum);
			if (result != null) {
				System.out.println("Fichier local different de la version distante.");
				storeLocalFile(result);
				System.out.println("Le fichier local a ete remplace par la version du serveur.");
			}
		} catch (RemoteException e) {
			System.err.println("Erreur RMI : " + e.getMessage());
			System.exit(ID_RMI_ERROR);
		} catch (IOException e) {
			System.err.println("Erreur IO : " + e.getMessage());
			System.exit(ID_IO_ERROR);
		}

		// Inform the user that the file has been locked
		System.out.println("Fichier \"" + filename + "\" verouille.");

	}

	/**
	 * Private method to get a file from the server.
	 * 
	 * @param filename
	 *            : the name of the file you want to get from the remote server.
	 */
	private void getFile(String filename) {
		byte[] file = null;
		byte[] checksum = null;

		try {
			file = getLocalFile(filename);
			checksum = computeChecksum(file);
		} catch (NoSuchFileException e) {
			System.out.println("Version locale du fichier \"" + filename + "\" inexistante.");
		}

		File result = null;

		try {
			System.out.println("Recuperation de la version du serveur.");
			result = distantServerStub.get(filename, checksum);
			System.out.println("Fichier " + result.getHeader().getName() + " recupere.");
		} catch (RemoteException e) {
			System.err.println("Erreur RMI : " + e.getMessage());
			System.exit(ID_RMI_ERROR);
		} catch (NullPointerException e) {
			System.err.println("Le fichier " + filename + " n'existe pas cote serveur.");
			System.err.println("Executer ./client list pour voir la liste des fichiers disponibles.");
			System.exit(ID_GET_CANCEL);
		}

		try {
			storeLocalFile(result);
		} catch (IOException e) {
			System.err.println("IOE Exception : " + e.getMessage());
			System.exit(ID_IO_ERROR);
		}
	}

	/**
	 * Private method to synchronize your local directory with the remove
	 * server. The server will send you all the files he has and will not check
	 * if your local files are different or not from its version.
	 */
	private void synchroLocalDirectory() {
		ArrayList<File> results = null;

		try {
			System.out.println("Synchronisaion des fichiers avec le serveur.");
			results = distantServerStub.syncLocalDir();
			if (results.isEmpty()) {
				System.out.println("Aucun fichier present sur le serveur.");
			} else {
				System.out.println("Fichier(s) recupere(s).");
			}
		} catch (RemoteException e) {
			System.err.println("Erreur RMI : " + e.getMessage());
			System.exit(ID_RMI_ERROR);
		}

		try {
			for (int i = 0; i < results.size(); i++) {
				// Re use the GET code which is based on the same logic.
				storeLocalFile(results.get(i));
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(ID_IO_ERROR);
		}

	}

	/**
	 * Private method to check if the first argument is appropriated.
	 * 
	 * @param args
	 * @return
	 */
	private static String checkFirstArgument(String[] args) {
		String fileName = args[1];
		try {
			if (fileName == null) {
				throw new IllegalArgumentException();
			}
		} catch (IllegalArgumentException e) {
			System.err.println("Argument(s) invalide(s). Lisez le fichier README.txt.");
			System.exit(ID_INVALID_ARGUMENT);
		}
		return fileName;
	}

	/**
	 * Private method to get the user id from the user.
	 * 
	 * @return the user id
	 */
	private Integer getUserId() {

		Integer id = new Integer(-1);
		try {
			java.io.File f = new java.io.File(FILE_PATH);
			FileReader fr = new FileReader(f);
			BufferedReader br = new BufferedReader(fr);
			String line = br.readLine();
			line = line.substring(line.lastIndexOf("=") + 1).trim();

			br.close();
			fr.close();

			if (!line.isEmpty()) {
				id = Integer.parseInt(line);
				System.out.println("Vous etes l'utilisateur : " + id);
			} else {
				id = new Integer(distantServerStub.generateClientId());
				storeUserId(id);
				System.out.println("Demande d'un nouvel id aupres du serveur ...");
				System.out.println("Vous etes l'utilisateur :" + id);
			}
		} catch (FileNotFoundException exception) {
			try {
				id = new Integer(distantServerStub.generateClientId());
				storeUserId(id);
				System.out.println("Demande d'un nouvel id aupres du serveur ...");
				System.out.println("Vous etes l'utilisateur :" + id);
			} catch (RemoteException e) {
				System.err.println("Remote exception : " + e);
			} catch (IOException e) {
				System.err.println("Erreur acces au fichier : " + e);
			}
		} catch (RemoteException e) {
			System.err.println("Erreur: " + e.getMessage());
		} catch (IOException e) {
			System.err.println("Erreur lors de la lecture : " + e.getMessage());
		} catch (NumberFormatException e) {
			System.err.println("Fichier d'id corrompu : " + e.getMessage());
		}

		return id;
	}

	/**
	 * Store the client id into a file to recognize the client
	 * 
	 * @param id
	 *            the id of the client
	 * @throws IOException
	 */
	private void storeUserId(Integer id) throws IOException {

		String stringToStore = "id=" + id;
		java.io.File userFile = new java.io.File(FILE_PATH);
		if (!userFile.exists()) {
			userFile.createNewFile();
		}

		try (FileWriter userFileWritter = new FileWriter(userFile, false)) {
			userFileWritter.write(stringToStore);
		}
	}

	/**
	 * Private method to get the local file
	 * 
	 * @param filename
	 *            : the name of the file
	 * @return the content of the file
	 * @throws NoSuchFileException
	 */
	private byte[] getLocalFile(String filename) throws NoSuchFileException {
		byte[] file = null;
		try {
			file = Files.readAllBytes(Paths.get(filename));
		} catch (NoSuchFileException e) {
			throw new NoSuchFileException("Fichier inexistant en local.");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(ID_IO_ERROR);
		}
		return file;
	}

	/**
	 * Private method to compute the checksum on the client side
	 * 
	 * @param file
	 *            : the file to compute the checksum
	 * @return the checksum associated to the file
	 */
	private byte[] computeChecksum(byte[] file) {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Erreur Algorithme " + e.getMessage());
		}
		return md.digest(file);
	}

	/**
	 * Private method to store a file locally on the client side.
	 * 
	 * @param file
	 *            the file to store
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void storeLocalFile(File file) throws IOException {
		FileOutputStream stream = new FileOutputStream(file.getHeader().getName());
		stream.write(file.getContent().getContent());
		stream.close();
	}
}
