
package client;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import result.ValueResult;
import server.Server;
import task.Task;
import tasks.FibonacciReadyTask;

/**
 * ClientFibonacci is the client to submit Fibonacci job to calculate F(N). It
 * takes Space's domain name as its argument.
 */
public class ClientFibonacci extends Client<Integer,Integer> {
	private static final long serialVersionUID = -6190190465235435463L;

	/**
	 * Constructor of Client Fibonacci.
	 * 
	 * @param clientName
	 *            Client Name
	 */
	public ClientFibonacci(String clientName) {
		super("Fibonacci", clientName);
	}

	/**
	 * Prepare a Fibonacci Ready Task
	 * 
	 * @param N
	 *            Fibonacci Number
	 * @return Fibonacci Ready Task
	 */
	@Override
	public Task<Integer> makeTask(Integer N) {
		ArrayList<Integer> arg = new ArrayList<Integer>();
		arg.add(N);
		return new FibonacciReadyTask(arg);
	}

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		System.setSecurityManager(new SecurityManager());

		String serverDomainName = args.length == 0 ? "localhost" : args[0];
		ClientFibonacci client = new ClientFibonacci("F");
		client.begin();

		int N = 20;
		Task<Integer> fibTask = client.makeTask(N);
		try {
			Server server = client.findServer(serverDomainName);
			if (!server.register(client.getName(), null)) {
				System.out.println("Failed to register in Server");
			} else {
				String taskID = server.submit(fibTask, client.getName());
				Logger.getLogger(Client.class.getCanonicalName()).log(
						Level.INFO, "Task: F({0}) is submitted. ID is {1}",
						new Object[] { N, taskID });
				ValueResult<Integer> result = (ValueResult<Integer>) server
						.getResult(client.getName());
				Logger.getLogger(Client.class.getCanonicalName()).log(
						Level.INFO, "Result: F({0}) is {1}",
						new Object[] { N, result.getResultValue() });
			}
			server.unregister(client.getName());
		} catch (MalformedURLException | NotBoundException e) {
			System.out.println("Bad Server domain name!");
		} catch (RemoteException e) {
			System.out.println("Cannot regiseter to the Server!");
		}
		client.end();
	}

}
