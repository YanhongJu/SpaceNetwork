package universe;

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
import server.Server;
import space.Space;
import api.Result;
import api.Task;

public class UniverseImpl extends UnicastRemoteObject implements Universe {
	private static final long serialVersionUID = -5110211125190845128L;
	private static UniverseImpl universe;
	private static String recoveryFileName = "recovery.bk";

	/**
	 * Space Id.
	 */
	private static final AtomicInteger SpaceID = new AtomicInteger();

	/**
	 * Server Id.
	 */
	private static final AtomicInteger ServerID = new AtomicInteger();

	/**
	 * Ready Task Queue. Containing tasks ready to run.
	 */
	private final BlockingQueue<Task> readyTaskQueue;

	/**
	 * Successor Task Map. Containing successor tasks waiting for arguments.
	 */
	private final Map<String, Task> successorTaskMap;

	/**
	 * Server Proxies Map. Containing all registered Server Proxy with
	 * associated Server.
	 */
	private final Map<Integer, ServerProxy> serverProxies;

	/**
	 * Space Proxies Map. Containing all registered Space Proxy with associated
	 * Space.
	 */
	private final Map<Integer, SpaceProxy> spaceProxies;

	/**
	 * Normal Mode Constructor.
	 * 
	 * @throws RemoteException
	 */
	public UniverseImpl() throws RemoteException {
		readyTaskQueue = new LinkedBlockingQueue<>();
		successorTaskMap = Collections.synchronizedMap(new HashMap<>());
		serverProxies = Collections.synchronizedMap(new HashMap<>());
		spaceProxies = Collections.synchronizedMap(new HashMap<>());
		Logger.getLogger(this.getClass().getName()).log(Level.INFO,
				"Universe started.");
	}

	/**
	 * Recovery Mode Constructor
	 * 
	 * @param recoveryFileName
	 *            Recovery File name
	 * @throws RemoteException
	 */
	public UniverseImpl(String recoveryFileName) throws RemoteException {
		readyTaskQueue = new LinkedBlockingQueue<>();
		successorTaskMap = Collections.synchronizedMap(new HashMap<>());
		serverProxies = Collections.synchronizedMap(new HashMap<>());
		spaceProxies = Collections.synchronizedMap(new HashMap<>());
		Logger.getLogger(this.getClass().getName()).log(Level.INFO,
				"Universe started.");
	}

	public static void main(final String[] args) throws Exception {
		System.setSecurityManager(new SecurityManager());
		universe = args.length == 0 ? new UniverseImpl() : new UniverseImpl(
				recoveryFileName);
		LocateRegistry.createRegistry(Universe.PORT).rebind(
				Universe.SERVICE_NAME, universe);

		// Main thread waiting for Key Enter to terminate.
		try {
			System.in.read();
		} catch (Throwable ignored) {

		}
		System.out.println("Universe stopped.\n");
		System.exit(-1);
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
	 * Get a Task from the Ready Task Queue.
	 * 
	 * @return Task
	 */
	private Task getTask() {
		try {
			return readyTaskQueue.take();
		} catch (InterruptedException e) {
			if (Config.STATUSOUTPUT) {
				e.printStackTrace();
				System.out.println("Send Service is interrupted!");
			}
		}
		return null;
	}

	/**
	 * 
	 * Remove a successor task from Successor Task Map and put it into Ready
	 * Task Queue, when this successor task has all needed arguments and ready
	 * to run.
	 * 
	 * @param successortask
	 *            The ready-to-run successor task.
	 */
	private void successorToReady(Task successortask) {
		successorTaskMap.remove(successortask.getTaskID());
		try {
			readyTaskQueue.put(successortask);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Dispatch the Result to corresponding Server Proxy. If the Server is down,
	 * discard the result.
	 * 
	 * @param result
	 *            Result to be dispatched.
	 */
	private void dispatchResult(final Result result) {
		String resultID[] = result.getResultId().split(":");
		int serverID = Integer.parseInt(resultID[0]);
		synchronized (serverProxies) {
			if (serverProxies.containsKey(serverID)) {
				serverProxies.get(serverID).addResult(result);
			}
		}
	}

	/**
	 * Register a Server in Universe. Call from Server.
	 * 
	 * @param server
	 *            Server to be registered.
	 * @throws RemoteException
	 *             Cannot connect with Universe.
	 */
	@Override
	public void register(Server server) throws RemoteException {
		final ServerProxy serverProxy = new ServerProxy(server,
				ServerID.getAndIncrement());
		server.setID(serverProxy.ID);
		serverProxies.put(serverProxy.ID, serverProxy);
		serverProxy.start();
		Logger.getLogger(this.getClass().getName()).log(Level.INFO,
				"Server {0} starts!", serverProxy.ID);
	}

	/**
	 * Unregister a Server. Remove submitted Tasks in the Ready Task Queue.
	 * 
	 * @param serverProxy
	 *            Server associated ServerProxy to be unregistered.
	 */
	private void unregister(ServerProxy serverProxy) {
		serverProxies.remove(serverProxy.ID);
		synchronized (readyTaskQueue) {
			for (Task task : readyTaskQueue) {
				String taskID[] = task.getTaskID().split(":");
				if (taskID[0].equals(serverProxy.ID)) {
					readyTaskQueue.remove(task);
				}
			}
		}
		Logger.getLogger(this.getClass().getName()).log(Level.WARNING,
				"Server {0} is down.", serverProxy.ID);
	}

	/**
	 * Register a Space in Universe. Call from Space.
	 * 
	 * @param Space
	 *            Space to be registered.
	 * @throws RemoteException
	 *             Cannot connect with Universe.
	 */
	@Override
	public void register(Space space) throws RemoteException {
		final SpaceProxy spaceProxy = new SpaceProxy(space,
				SpaceID.getAndIncrement());
		space.setID(spaceProxy.ID);
		spaceProxies.put(spaceProxy.ID, spaceProxy);
		spaceProxy.start();
		Logger.getLogger(this.getClass().getName()).log(Level.INFO,
				"Space {0} started!", spaceProxy.ID);
	}

	/**
	 * Unregister a Space and remove its associated Space Proxy. Processing all
	 * unfinished Value Results. Save all the Space's unfinished running tasks
	 * into Universe Ready Task Queue.
	 * 
	 * @param spaceProxy
	 *            Space associated Space Proxy
	 */
	private void unregister(SpaceProxy spaceProxy) {
		spaceProxies.remove(spaceProxy.ID);
		if (!spaceProxy.runningTaskMap.isEmpty()) {
			for (String taskID : spaceProxy.runningTaskMap.keySet()) {
				try {
					readyTaskQueue.put(spaceProxy.runningTaskMap.get(taskID));
					if (Config.STATUSOUTPUT) {
						System.out.println("Save Space Task:" + taskID);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		Logger.getLogger(this.getClass().getName()).log(Level.WARNING,
				"Space {0} i down.", spaceProxy.ID);
	}

	private class ServerProxy {
		/**
		 * Associated Server
		 */
		private final Server server;

		/**
		 * Server ID
		 */
		private final int ID;

		/**
		 * Result Queue.
		 */
		private final BlockingQueue<Result> resultQueue;

		/**
		 * Send Service
		 */
		private final SendService sendService;

		/**
		 * Receive Service
		 */
		private final ReceiveService receiveService;

		public ServerProxy(Server server, int id) {
			this.server = server;
			this.ID = id;
			this.resultQueue = new LinkedBlockingQueue<>();
			receiveService = new ReceiveService();
			sendService = new SendService();
		}

		/**
		 * Start Receive Service thread and Send Service thread
		 */
		private void start() {
			receiveService.start();
			sendService.start();
		}

		/**
		 * Add a Result to Result Queue.
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
		 * Get a Result from Result Queue.
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

		private class ReceiveService extends Thread {
			@Override
			public void run() {
				while (true) {
					Result result = getResult();
					try {
						server.dispatchResult(result);
					} catch (RemoteException e) {
						e.printStackTrace();
						System.out.println("ReceiveService: Server " + ID
								+ " is Down!");
						return;
						// If the Server is down abnormally, should Clean the
						// Server. Maybe in future. Discard the Result.
					}
				}
			}
		}

		/**
		 * Send Service is a thread for putting tasks from Client to the Server
		 * Ready Task Queue.
		 *
		 */
		private class SendService extends Thread {
			@Override
			public void run() {
				while (true) {
					Task task = null;
					try {
						task = server.getTask();
						UniverseImpl.this.addTask(task);
					} catch (RemoteException e) {
						e.printStackTrace();
						System.out.println("SendService: Server " + ID
								+ " is Down!");
						if (ServerProxy.this.receiveService.isAlive()) {
							ServerProxy.this.receiveService.interrupt();
						}
						unregister(ServerProxy.this);
						return;
						// If the Server is down abnormally, should Clean the
						// Server. Maybe in future. Discard the task.
					}
				}
			}
		}
	}

	private class SpaceProxy {
		/**
		 * Associated Space.
		 */
		private final Space space;

		/**
		 * Space ID.
		 */
		private final int ID;

		/**
		 * Running Task Map. The tasks that Space is running.
		 */
		private final Map<String, Task> runningTaskMap;

		/**
		 * Send Service
		 */
		private final SendService sendService;

		/**
		 * Receive Service
		 */
		private final ReceiveService receiveService;

		public SpaceProxy(Space space, int id) {
			this.space = space;
			this.ID = id;
			this.runningTaskMap = Collections.synchronizedMap(new HashMap<>());
			this.receiveService = new ReceiveService();
			this.sendService = new SendService();
		}

		/**
		 * Start Receive Service thread and Send Service thread
		 */
		private void start() {
			receiveService.start();
			sendService.start();
		}

		private class ReceiveService extends Thread {
			@Override
			public void run() {
				while (true) {
					Result result = null;
					try {
						result = space.getResult();
						synchronized (runningTaskMap) {
							runningTaskMap.remove(result.getResultId());
							UniverseImpl.this.dispatchResult(result);
						}
					} catch (RemoteException e) {
						System.out.println("Receive Servcie Space " + ID
								+ " is Down!");
						if (SpaceProxy.this.sendService.isAlive()) {
							SpaceProxy.this.sendService.interrupt();
						}
						unregister(SpaceProxy.this);
						return;
						// If the Space is down abnormal, Save all tasks.
					}
				}
			}
		}

		/**
		 * Send Service is a thread for putting tasks from Client to the Server
		 * Ready Task Queue.
		 *
		 */
		private class SendService extends Thread {
			@Override
			public void run() {
				while (true) {
					Task task = null;
					try {
						task = UniverseImpl.this.getTask();
						synchronized (runningTaskMap) {
							space.addTask(task);
							runningTaskMap.put(task.getTaskID(), task);
						}
					} catch (RemoteException e) {
						System.out.println("Send Service Space " + ID
								+ " is Down!");
						UniverseImpl.this.addTask(task);
						return;
						// If the Space is down abnormal, Save all tasks.
					}
				}
			}
		}
	}

}
