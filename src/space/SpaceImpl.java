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
import api.Result;
import api.Task;
import result.ValueResult;
import task.SuccessorTask;
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
	private final BlockingQueue<Task> readyTaskQueue;

	/**
	 * Successor Task Map. Containing successor tasks waiting for arguments.
	 */
	private final Map<String, Task> successorTaskMap;

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
	 * @throws MalformedURLException
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
	public void addTask(final Task task) throws RemoteException {
		try {
			readyTaskQueue.put(task);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Add a task to Ready Task Queue.
	 * 
	 * @param task
	 *            The task to be added.
	 */
	public void addReadyTask(Task task) {
		try {
			readyTaskQueue.put(task);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Add a task to Sucessor Task Map.
	 * 
	 * @param task
	 *            The task to be added.
	 */
	public void addSuccessorTask(Task task) {
		successorTaskMap.put(task.getTaskID(), task);
	}

	/**
	 * Get a task from the Successor Task Map with Task Id.
	 * 
	 * @param TaskId
	 *            Task Id.
	 * @return A Successor Task.
	 */
	public Task getSuccessorTask(String TaskId) {
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
	public void successorToReady(Task successortask) {
		successorTaskMap.remove(successortask.getTaskID());
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
		final ComputerProxy computerproxy = new ComputerProxy(computer,ComputerID.getAndIncrement());
		computer.setID(computerproxy.ID);
		computerProxies.put(computerproxy.ID, computerproxy);
		computerproxy.start();
		Logger.getLogger(this.getClass().getName()).log(
				Level.INFO,
				"Computer {0} started with {1} threads",
				new Object[] { computerproxy.ID,
						computer.getProcessNum() });
	}

	/**
	 * Unregister a Computer and remove its associated Computer Proxy.
	 * Processing all unfinished Value Results. Save all the Computer's
	 * unfinished running tasks into Space Ready Task Queue.
	 * 
	 * @param computer
	 */
	private void unregister(final Computer computer) {
		ComputerProxy computerProxy = computerProxies.remove(computer);
		Result result = null;
		while ((result = computerProxy.intermediateResultQueue.poll()) != null) {
			if (result.process(space,
					computerProxy.runningTaskMap,
					computerProxy.intermediateResultQueue)) {
				computerProxy.runningTaskMap.remove(result
						.getResultId());
			}
			if (Config.STATUSOUTPUT) {
				System.out.println("Unregister Result: " + result.getResultId()
						+ "!" + ((ValueResult<?>) result).getTargetTaskId());
			}
		}
		if (!computerProxy.runningTaskMap.isEmpty()) {
			for (String taskId : computerProxy.runningTaskMap
					.keySet()) {
				try {
					readyTaskQueue
							.put(computerProxy.runningTaskMap
									.get(taskId));
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
			BlockingQueue<Result> TempResultQueue) {
		successorTaskMap.remove(successortask.getTaskID());
		ValueResult<T> result = (ValueResult<T>) successortask.execute();
		try {
			TempResultQueue.put(result);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (Config.STATUSOUTPUT) {
			System.out.println(result.getResultId());
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
		 * Running Task Map. The tasks that Computer is running.
		 */
		private final Map<String, Task> runningTaskMap;

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
		 */
		private volatile boolean isReceiveException;

		ComputerProxy(Computer computer, int computerid) {
			this.computer = computer;
			this.ID = computerid;

			this.runningTaskMap = Collections
					.synchronizedMap(new HashMap<>());
			this.intermediateResultQueue = new LinkedBlockingQueue<>();
			isReceiveException = false;
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
		 * Receive Service is a thread for non-blocking polling results from the
		 * Computer's Result Queue as well as the Temporary Result Queue, and
		 * process the result. If the result is processed successfully, remove
		 * its associated task from Computer Proxy Running Task Map.
		 */
		private class ReceiveService extends Thread {
			@Override
			public void run() {
				Result result = null;
				while (true) {
					try {
						// Get result from Computer Result Queue.
						result = computer.takeResult();
						if (result != null) {
							synchronized (runningTaskMap) {
								if (result.process(space,
										runningTaskMap,
										intermediateResultQueue)) {
									runningTaskMap.remove(result
											.getResultId());
								}
							}
						}
						// Get the result from Temporary Result Queue.
						result = intermediateResultQueue.poll();
						if (result != null) {
							synchronized (runningTaskMap) {
								if (result.process(space,
										runningTaskMap,
										intermediateResultQueue)) {
									runningTaskMap.remove(result
											.getResultId());
								}
							}
						}
					} catch (RemoteException ex) {
						// If the Computer is down, unregister the Computer and
						// save current working status.
						System.out.println("Receive Exception!");
						isReceiveException = true;
						unregister(computer);
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
					Task task = null;
					try {
						// Get a task from Space Ready Task Queue.
						task = readyTaskQueue.poll();
						if (task != null) {
							synchronized (runningTaskMap) {
								// Put the task to Computer's Ready Task
								// Queue.
								computer.addTask(task);
								// Put the task into Computer Proxy Running
								// Task
								// Map.
								runningTaskMap.put(
										task.getTaskID(), task);
							}
						} else {
							if (isReceiveException) {
								System.out.println("Send knows Exception!");
								return;
							}
						}
					} catch (RemoteException ex) {
						System.out.println("Send Exception!");
						try {
							readyTaskQueue.put(task);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						return;
					}
				}
			}
		}
	}

}
