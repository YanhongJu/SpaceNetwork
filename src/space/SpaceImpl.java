package space;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import computer.Computer;
import config.Config;
import result.Result;
import result.ValueResult;
import task.SuccessorTask;
import task.Task;
import universe.Universe;

/**
 * 
 * Implementation of Space, managing task queues and monitoring Computers.
 *
 */
public class SpaceImpl extends UnicastRemoteObject implements Space {
	private static final long serialVersionUID = 9201498495659202339L;

	/**
	 * Space itslef. One Space in this JVM.
	 */
	private static SpaceImpl space;

	/**
	 * Space ID.
	 */
	private static int ID;

	/**
	 * Computer Id.
	 */
	private static final AtomicInteger ComputerID = new AtomicInteger();

	/**
	 * Ready Task Queue. Containing tasks ready to run.
	 */
	private final BlockingQueue<Task<?>> readyTaskQueue;

	/**
	 * Successor Task Map. Containing successor tasks waiting for arguments.
	 */
	private final Map<String, Task<?>> successorTaskMap;

	/**
	 * Result Queue. Containing the final result of the coarse task.
	 */
	private final BlockingQueue<Result> resultQueue;

	/**
	 * Computer Proxies Map.
	 */
	private final Map<Integer, ComputerProxy> computerProxies;

	/**
	 * Constructor of Space Implementation.
	 * 
	 * @throws RemoteException
	 *             Cannot connect to Space.
	 * @throws NotBoundException
	 *             Bad Universe Domain Name
	 * @throws MalformedURLException
	 *             Bad Universe Domain Name
	 */
	public SpaceImpl(String universeDomainName) throws RemoteException,
			MalformedURLException, NotBoundException {
		readyTaskQueue = new LinkedBlockingQueue<>();
		successorTaskMap = Collections.synchronizedMap(new HashMap<>());
		resultQueue = new LinkedBlockingQueue<>();
		computerProxies = Collections.synchronizedMap(new HashMap<>());
		Logger.getLogger(this.getClass().getName()).log(Level.INFO,
				"Space started.");
		String url = "rmi://" + universeDomainName + ":" + Universe.PORT + "/"
				+ Universe.SERVICE_NAME;
		Universe universe;
		universe = (Universe) Naming.lookup(url);
		universe.register(this);
	}

	public static void main(final String[] args) {
		String universeDomainName = args.length == 0 ? "localhost" : args[0];
		System.setSecurityManager(new SecurityManager());

		try {
			space = new SpaceImpl(universeDomainName);
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
			LocateRegistry.createRegistry(Space.PORT).rebind(
					Space.SERVICE_NAME, space);
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
		System.out.println("Space stopped.\n");
		System.exit(-1);
	}

	/**
	 * Set an ID to the Space. Call from Universe.
	 * 
	 * @param spaceID
	 *            Space Id.
	 * @throws RemoteException
	 *             Failed to connect to Space.
	 */
	@Override
	public void setID(int spaceID) throws RemoteException {
		ID = spaceID;
	}

	/**
	 * Add a Task to the Ready Task Queue. Call from Space Proxy in Universe.
	 * 
	 * @param task
	 *            The Ready/Successor Task to be submitted.
	 * @throws RemoteException
	 *             Cannot connect with Space.
	 */
	@Override
	public void addTask(final Task<?> task) throws RemoteException {
		try {
			readyTaskQueue.put(task);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Add a Task to Ready Task Queue. Call from Result.
	 * 
	 * @param task
	 *            Task to be added.
	 */
	public void addReadyTask(Task<?> task) {
		try {
			readyTaskQueue.put(task);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get a Task from Ready Task Queue.
	 * 
	 * @return Task.
	 */
	public Task<?> getReadyTask() {
		try {
			return readyTaskQueue.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Add a Successor Task to Successor Task Map.
	 * 
	 * @param task
	 *            Task to be added.
	 */
	public void addSuccessorTask(Task<?> task) {
		successorTaskMap.put(task.getID(), task);
	}

	/**
	 * Get a task from the Successor Task Map with Task Id.
	 * 
	 * @param TaskId
	 *            Task Id.
	 * @return A Successor Task.
	 */
	public Task<?> getSuccessorTask(String TaskId) {
		return successorTaskMap.get(TaskId);
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
	public void successorToReady(Task<?> successortask) {
		successorTaskMap.remove(successortask.getID());
		try {
			readyTaskQueue.put(successortask);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Add the final result into the Result Queue.
	 * 
	 * @param result
	 *            The final result.
	 */
	public void addResult(Result result) {
		try {
			resultQueue.put(result);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get Result from Result Queue. Call from Space Proxy in Universe.
	 * 
	 * @return Result
	 * @throws RemoteException
	 *             Cannot connect with Space.
	 */
	@Override
	public Result getResult() throws RemoteException {
		Result result = null;
		try {
			result = resultQueue.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Register a Computer and associate it with a Computer Proxy into Space.
	 * Start the Computer Proxy.
	 */
	@Override
	public void register(final Computer computer) throws RemoteException {
		final ComputerProxy computerproxy = new ComputerProxy(computer,
				ComputerID.getAndIncrement());
		computer.setID(computerproxy.ID);
		computerProxies.put(computerproxy.ID, computerproxy);
		computerproxy.start();
		Logger.getLogger(this.getClass().getName()).log(Level.INFO,
				"Computer {0} started with {1} workers!",
				new Object[] { computerproxy.ID, computer.getWorkerNum() });
	}

	/**
	 * Unregister a Computer and remove its associated Computer Proxy.
	 * Processing all unfinished Value Results. Save all the Computer's
	 * unfinished running tasks into Space Ready Task Queue.
	 * 
	 * @param computer
	 */
	private void unregister(ComputerProxy computerProxy) {
		computerProxies.remove(computerProxy.ID);
		Result result = null;
		while ((result = computerProxy.intermediateResultQueue.poll()) != null) {
			result.process(space, computerProxy.runningTaskMap,
					computerProxy.intermediateResultQueue);
			computerProxy.runningTaskMap.remove(result.getID());
			if (Config.STATUSOUTPUT) {
				System.out.println("Unregister Result: " + result.getID() + "!"
						+ ((ValueResult<?>) result).getTargetTaskID());
			}
		}
		if (!computerProxy.runningTaskMap.isEmpty()) {
			for (String taskId : computerProxy.runningTaskMap.keySet()) {
				try {
					readyTaskQueue
							.put(computerProxy.runningTaskMap.get(taskId));
					if (Config.STATUSOUTPUT) {
						System.out.println("Unregister@Task:" + taskId);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		Logger.getLogger(this.getClass().getName()).log(Level.WARNING,
				"Computer {0} failed.", computerProxy.ID);
	}

	/**
	 * Space directly executes the task.
	 * 
	 * @param <T>
	 *            Argument Type of the Task.
	 * @param successortask
	 *            The successor task to be executed.
	 * @param TempResultQueue
	 *            The Temporary Result Queue in Computer Proxy to contain the
	 *            execution result.
	 */
	@SuppressWarnings("unchecked")
	public <T> void spaceExecuteTask(SuccessorTask<T> successortask,
			BlockingQueue<Result> intermediateResultQueue) {
		successorTaskMap.remove(successortask.getID());
		ValueResult<T> result = (ValueResult<T>) successortask.execute();
		try {
			intermediateResultQueue.put(result);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (Config.STATUSOUTPUT) {
			System.out.println(result.getID());
		}
	}

	/**
	 * 
	 * A Computer Proxy is to manage its associated Computer's working status.
	 * It gives the Computer tasks and gets results, meanwhile keeps track of
	 * the tasks the Computer is running, which is responsible for fault
	 * tolerance.
	 *
	 */
	private class ComputerProxy {
		/**
		 * Computer associated with this Computer Proxy.
		 */
		private final Computer computer;

		/**
		 * Computer Id.
		 */
		private final int ID;

		/**
		 * Task ID.
		 */
		private final AtomicInteger TaskID = new AtomicInteger();

		/**
		 * Running Task Map. The tasks that Computer is running.
		 */
		private final Map<String, Task<?>> runningTaskMap;

		/**
		 * Intermediate Result Queue. Store Results of Space Direct Execution
		 */
		private final BlockingQueue<Result> intermediateResultQueue;

		/**
		 * Receive Service thread.
		 */
		private final ReceiveService receiveService;

		/**
		 * Send Service thread.
		 */
		private final SendService sendService;

		/**
		 * Constructor of Computer Proxy.
		 * 
		 * @param computer
		 *            The Computer associated with this Computer Proxy.
		 * @param comptuerid
		 *            Computer ID
		 */
		ComputerProxy(Computer computer, int computerid) {
			this.computer = computer;
			this.ID = computerid;
			this.runningTaskMap = Collections.synchronizedMap(new HashMap<>());
			this.intermediateResultQueue = new LinkedBlockingQueue<>();
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

		/**
		 * Generate a Task ID.
		 * 
		 * @return Task ID.
		 */
		private int makeTaskID() {
			return TaskID.incrementAndGet();
		}

		/**
		 * Receive Service is a thread for non-blocking polling results from the
		 * Computer's Result Queue as well as the Temporary Result Queue, and
		 * process the result. If the result is processed successfully, remove
		 * its associated task from Computer Proxy Running Task Map.
		 */
		private class ReceiveService extends Thread {
			@Override
			public void run() {
				while (true) {
					try {
						// Get result from Computer Result Queue.
						Result result = computer.getResult();
						if (result != null) {
							if (Config.DEBUG) {
								System.out.println("Computer Proxy: Result "
										+ result.getID() + " is taken!");
							}
							synchronized (runningTaskMap) {
								if (result.isCoarse()) {
									space.addResult(result);
									if (Config.DEBUG) {
										System.out.println("Computer Proxy: Result "
												+ result.getID() + " is added!");
									}
									runningTaskMap.remove(result.getID());
								} else {
									if(!result.process(space, runningTaskMap,
											intermediateResultQueue)) {
										space.addResult(result);
									}
									runningTaskMap.remove(result.getID());
								}
							}
						}
						// Get the result from Intermediate Result Queue.
						result = intermediateResultQueue.poll();
						if (result != null) {
							String resultID[] = result.getID().split(":");
							StringBuffer resultid = new StringBuffer();
							for (int i = 0; i <= 8; i++) {
								resultid.append(resultID[i]);
								resultid.append(":");
							}
							resultid.deleteCharAt(resultid.length() - 1);
							String taskID = resultid.toString();
							synchronized (runningTaskMap) {
								if (result.isCoarse()) {
									space.addResult(result);
									if (Config.DEBUG) {
										System.out.println("Computer Proxy: Result "
												+ result.getID() + " is added!");
									}
									runningTaskMap.remove(result.getID());
								} else {
									if(!result.process(space, runningTaskMap,
											intermediateResultQueue)) {
										space.addResult(result);
									}
									runningTaskMap.remove(result.getID());
								}
							}
						}
					} catch (RemoteException ex) {
						// If the Computer is down, unregister the Computer and
						// save current working status.
						System.out.println("Receive Service: Computer " + ID
								+ " is down!");
						unregister(ComputerProxy.this);
						return;
					}
				}
			}
		}

		/**
		 * Send Service is a thread for putting task from Space Ready Task Queue
		 * to Computer's Ready Task Queue for Computer to execute it and
		 * Computer Proxy Running Task Map to keep track the task.
		 */
		private class SendService extends Thread {
			@Override
			public void run() {
				while (true) {
					Task<?> task = space.getReadyTask();
					if (!task.getID().contains(":C")) {
						task.setID(task.getID() + ":" + ID + ":C" + makeTaskID());
					}
					try {
						synchronized (runningTaskMap) {
							computer.addTask(task);
							runningTaskMap.put(task.getID(), task);
							if (Config.DEBUG) {
								System.out.println("Computer Proxy: Task "
										+ task.getID() + " is added!");
							}
						}
					} catch (RemoteException e) {
						System.out.println("Send Service: Computer " + ID
								+ " is down!");
						space.addReadyTask(task);
						return;
					}
				}
			}
		}

	}

}
