package client;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import result.ValueResult;
import server.Server;
import task.ReadyTask;
import tasks.FibonacciReadyTask;

/**
 * ClientFibonacci is the client to submit Fibonacci job to calculate F(N). It
 * takes Space's domain name as its argument.
 */
public class ClientFibonacci extends ClientImpl<Integer> {
	private static final long serialVersionUID = -6190190465235435463L;

	/**
	 * Constructor of Client Fibonacci.
	 * 
	 * @param domainName
	 *            Server domain name
	 * @throws RemoteException
	 *             Cannot connect to the Server
	 * @throws NotBoundException
	 *             Bad Server domain name
	 * @throws MalformedURLException
	 *             Bad Server domain name
	 */
	public ClientFibonacci(String serverDomainName) throws RemoteException,
			NotBoundException, MalformedURLException {
		super("Fibonacci", serverDomainName);
	}

	/**
	 * Prepare a Fibonacci Ready Task
	 * 
	 * @param N
	 *            Fibonacci Number
	 * @return Fibonacci Ready Task
	 */
	private static ReadyTask<Integer> makeReadyTask(int N) {
		ArrayList<Integer> arg = new ArrayList<Integer>();
		arg.add(N);
		return new FibonacciReadyTask(arg);
	}

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		System.setSecurityManager(new SecurityManager());

		String ServerDomainName = args.length == 0 ? "localhost" : args[0];
		// Instantiate a Client Fibonacci.
		ClientImpl client = null;
		try {
			client = new ClientFibonacci(ServerDomainName);
			String url = "rmi://" + ServerDomainName + ":" + Server.PORT + "/"
					+ Server.SERVICE_NAME;
			Server server;
			server = (Server) Naming.lookup(url);
			server.register(client);
		} catch (RemoteException e) {
			e.printStackTrace();
			System.out.println("Cannot regiseter to the Server!");
			return;
		} catch (MalformedURLException | NotBoundException e) {
			System.out.println("Bad Server domain name!");
			e.printStackTrace();
			return;
		}
		client.begin();

		int N = 10;
		// Prepare a Task
		ReadyTask<Integer> fibTask = makeReadyTask(N);
		// Put it into Ready Task Queue.
		client.addTask(fibTask);
		// Get TaskID assigned by the Server.
		String taskID = client.getTaskID();
		Logger.getLogger(ClientImpl.class.getCanonicalName()).log(Level.INFO,
				"Task: F({0}) is submitted. ID is {1}",
				new Object[] { N, taskID });
		// Get the Result of submitted Task.
		ValueResult<Integer> result = (ValueResult<Integer>) client.getResult();
		Logger.getLogger(ClientImpl.class.getCanonicalName()).log(Level.INFO,
				"Result: F({0}) is {1}",
				new Object[] { N, result.getResultValue() });
		client.end();
	}
}
