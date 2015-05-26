package server;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import config.Config;
import universe.Universe;
import client.ClientImpl;
import api.Result;
import api.Task;

public class ServerImpl extends UnicastRemoteObject implements Server {
	private static final long serialVersionUID = -7458792337176706359L;

	/**
	 * Server itslef. One Server in this JVM.
	 */
	private static ServerImpl server;

	/**
	 * Server ID. Assigned from Universe
	 */
	private int ID;

	/**
	 * Task ID. Assigned from Server.
	 */
	private static final AtomicInteger TaskID = new AtomicInteger();

	/**
	 * Client ID. Assign to Client.
	 */
	private static final AtomicInteger ClientID = new AtomicInteger();

	/**
	 * Ready Task Queue. Take Tasks from Client and to be taken by Universe
	 */
	private final BlockingQueue<Task> readyTaskQueue;

	/**
	 * Client Proxies Map. All registered Clients.
	 */
	private final Map<Integer, ClientProxy> clientProxies;

	/**
	 * Constructor of Server Implementation. Register to the Universe.
	 * 
	 * @param universeDomainName
	 *            Universe Domain Name
	 * @throws NotBoundException
	 *             Bad Universe Domain Name.
	 * @throws MalformedURLException
	 *             Bad Universe Domain Name.
	 * @throws RemoteException
	 *             Cannot connect to the Universe.
	 */
	public ServerImpl(final String universeDomainName)
			throws NotBoundException, MalformedURLException, RemoteException {
		readyTaskQueue = new LinkedBlockingQueue<>();
		clientProxies = Collections.synchronizedMap(new HashMap<>());
		Logger.getLogger(this.getClass().getName()).log(Level.INFO,
				"Server started.");
		String url = "rmi://" + universeDomainName + ":" + Universe.PORT + "/"
				+ Universe.SERVICE_NAME;
		Universe universe;
		universe = (Universe) Naming.lookup(url);
		universe.register(this);
	}

	public static void main(final String[] args) {
		String universeDomainName = args.length == 0 ? "localhost" : args[0];
		System.setSecurityManager(new SecurityManager());
		// Instantiate the Server.
		try {
			server = new ServerImpl(universeDomainName);
		} catch (RemoteException e) {
			e.printStackTrace();
			System.out.println("Cannot regiseter to the Universe!");
			return;
		} catch (MalformedURLException | NotBoundException e) {
			System.out.println("Bad Universe domain name!");
			e.printStackTrace();
			return;
		}
		try {
			LocateRegistry.createRegistry(Server.PORT).rebind(
					Server.SERVICE_NAME, server);
		} catch (RemoteException e) {
			System.out.println("Fail to bind Server!");
			e.printStackTrace();
			return;
		}

		// Main thread waiting for Key Enter to terminate.
		try {
			System.in.read();
		} catch (Throwable ignored) {

		}
		System.out.println("Server stopped.\n");
		System.exit(-1);
	}

	/**
	 * Set an ID to the Server. Call from Universe.
	 * 
	 * @param serverID
	 *            Server Id.
	 * @throws RemoteException
	 *             Failed to connect to Server.
	 */
	@Override
	public void setID(int serverID) throws RemoteException {
		this.ID = serverID;
	}

	/**
	 * Generate a Task ID.
	 * 
	 * @return Task ID.
	 */
	private int makeTaskID() {
		return TaskID.getAndIncrement();
	}

	/**
	 * Add a Task to Ready Task Queue.
	 * 
	 * @param task
	 *            Task to be added.
	 */
	private void addTask(Task task) {
		try {
			readyTaskQueue.put(task);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get a Task from Ready Task Queue. Call from Server Proxy in Universe.
	 * 
	 * @return Task
	 * @throws RemoteException
	 *             Cannot connect with Server.
	 */
	@Override
	public Task getTask() throws RemoteException {
		try {
			return readyTaskQueue.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Dispatch the Result to corresponding Client Proxy. Call from Server Proxy
	 * in Universe. If the Client is down, discard the result.
	 * 
	 * @param result
	 *            Result to be dispatched.
	 * @throws RemoteExcemption
	 *             Cannot connect with Server.
	 */
	@Override
	public void dispatchResult(final Result result) throws RemoteException {
		String resultID[] = result.getResultId().split(":");
		int clientID = Integer.parseInt(resultID[1]);
		if (clientProxies.containsKey(clientID)) {
			clientProxies.get(clientID).addResult(result);
		}
	}

	/**
	 * Register a Client in Server. Call from Client.
	 * 
	 * @param client
	 *            Client to be registered.
	 * @throws RemoteException
	 *             Cannot connect with Server.
	 */
	@Override
	public void register(final ClientImpl<?> client) throws RemoteException {
		final ClientProxy clientProxy = new ClientProxy(client,
				ClientID.getAndIncrement());
		client.setID(234);
		clientProxies.put(clientProxy.ID, clientProxy);
		clientProxy.start();
		Logger.getLogger(this.getClass().getName()).log(Level.INFO,
				"Client {0} starts!", clientProxy.ID);
	}

	/**
	 * Unregister a Client in Server. Remove submitted Tasks in the Ready Task
	 * Queue.
	 * 
	 * @param clientProxy
	 *            Associated Cient Proxy.
	 */
	private void unregister(ClientProxy clientProxy) {
		clientProxies.remove(clientProxy.ID);
		synchronized (readyTaskQueue) {
			for (Task task : readyTaskQueue) {
				String taskID[] = task.getTaskID().split(":");
				if (taskID[1].equals(clientProxy.ID)) {
					readyTaskQueue.remove(task);
				}
			}
		}
		Logger.getLogger(this.getClass().getName()).log(Level.INFO,
				"Client {0} is down!", clientProxy.ID);
	}

	/**
	 * 
	 * A Client Proxy is to manage its associated Client. It takes Tasks from
	 * the Client and retrieves Results to the Client.
	 *
	 */
	private class ClientProxy {
		/**
		 * Client associated with the Client Proxy
		 */
		private final ClientImpl<?> client;

		/**
		 * Client Proxy ID
		 */
		private final int ID;

		/**
		 * Result Queue
		 */
		private final LinkedBlockingQueue<Result> resultQueue;

		/**
		 * Send Service
		 */
		private final SendService sendService;

		/**
		 * Receive Service
		 */
		private final ReceiveService receiveService;

		/**
		 * Constructor of Client Proxy
		 * 
		 * @param client
		 *            Client to be associated with the Client Proxy.
		 * @param ID
		 *            Client Proxy ID
		 */
		public ClientProxy(ClientImpl<?> client, int ID) {
			this.client = client;
			this.ID = ID;
			resultQueue = new LinkedBlockingQueue<Result>();
			sendService = new SendService();
			receiveService = new ReceiveService();
			if(Config.DEBUG) {
				try {
					System.out.println("Server: client is " + client.getID());
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}

		/**
		 * Start Receive Service thread and Send Service thread
		 */
		private void start() {
			receiveService.start();
			sendService.start();
		}

		/**
		 * Add Result to Result Queue.
		 * 
		 * @param result
		 *            Result to be added.
		 */
		private void addResult(Result result) {
			try {
				resultQueue.put(result);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Get Result from Result Queue.
		 * 
		 * @return Result
		 */
		private Result getResult() {
			try {
				return resultQueue.take();
			} catch (InterruptedException e) {
				if (Config.STATUSOUTPUT) {
					e.printStackTrace();
					System.out.println("Receive Service is interrupted!");
				}
			}
			return null;
		}

		/**
		 * Assign Task ID and Target ID to the task to be added into Server's
		 * Ready Task Queue. TaskID is ServerID:ClientID:TaskID
		 * 
		 * @param task
		 *            Task to be submitted.
		 * @return Task ID
		 */
		private String submitTask(Task task) {
			task.setTaskID(server.ID + ":" + this.ID + ":"
					+ server.makeTaskID());
			task.setTargetTaskID(server.ID + ":" + this.ID + ":"
					+ server.makeTaskID() + ":" + "-1");
			if(Config.DEBUG) {
				System.out.println("Server: Task ID generated:" + task.getTaskID());
			}
			server.addTask(task);
			if(Config.DEBUG) {
				System.out.println("Server: Task is added.");
			}
			return task.getTaskID();
		}

		/**
		 * 
		 * Receive Service is a thread for taking Result from the Server Result
		 * Map.
		 *
		 */
		private class ReceiveService extends Thread {
			@Override
			public void run() {
				while (true) {
					Result result = getResult();
					try {
						client.addResult(result);
						// client.addResult(getResult());  Better?
					} catch (RemoteException e) {
						e.printStackTrace();
						System.out.println("ReceiveService : Client " + ID
								+ " is Down!");
						return;
						// If Client is down abnormally, should clean the
						// Universe.Maybe in future. Discard the result.
					}
				}
			}
		}

		/**
		 * Send Service is a thread for putting Tasks from Client to the
		 * Server's Ready Task Queue.
		 *
		 */
		private class SendService extends Thread {
			@Override
			public void run() {
				while (true) {
					try {
						Task task = client.getTask();
						String submittedtaskID = submitTask(task);
						client.addTaskID(submittedtaskID);
						if(Config.DEBUG) {
							System.out.println(submittedtaskID);
						}
						// client.addTaskID(submitTask(client.getTask()));
					} catch (RemoteException e) {
						e.printStackTrace();
						System.out.println("SendService : Client " + ID
								+ " is Down!");
						if (ClientProxy.this.receiveService.isAlive()) {
							ClientProxy.this.receiveService.interrupt();
						}
						unregister(ClientProxy.this);
						return;
						// If Client is down abnormally, should clean the
						// Universe.Maybe in future
					}
				}
			}
		}
	}

}
