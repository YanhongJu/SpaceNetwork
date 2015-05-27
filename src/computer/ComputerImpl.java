package computer;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import result.Result;
import result.TaskResult;
import space.Space;
import task.Task;
import config.Config;

/**
 * Implementation of Computer, generating Task Proxies to execute the tasks in
 * its Ready Task Queue and put the results into Result Queue. The Computer
 * assigns each task with an task ID.
 *
 */
public class ComputerImpl extends UnicastRemoteObject implements Computer {
	private final static long serialVersionUID = -3289346323918715850L;
	/**
	 * Computer ID.
	 */
	private int ID;

	/**
	 * Task ID.
	 */
	private final static AtomicInteger TaskID = new AtomicInteger();

	/**
	 * Ready Task Queue.
	 */
	private final BlockingQueue<Task<?>> readyTaskQueue;

	/**
	 * Result Queue.
	 */
	private final BlockingQueue<Result> resultQueue;

	/**
	 * Number of Workers
	 */
	private final int workerNum;

	/**
	 * Task Proxy Threads.
	 */
	private final Worker[] workers;

	/**
	 * Space
	 */
	private static Space space;

	/**
	 * Constructor of Computer Implementation. Generate and stat Task Proxies.
	 * 
	 * @throws RemoteException
	 *             Failed to connect to computer.
	 */
	public ComputerImpl() throws RemoteException {
		resultQueue = new LinkedBlockingQueue<>();
		if (Config.AmeliorationFlag) {
			readyTaskQueue = new LinkedBlockingQueue<>(32);
		} else {
			readyTaskQueue = new LinkedBlockingQueue<>(1);
		}
		if (Config.ComputerMultithreadFlag) {
			// Get available processors in JVM
			workerNum = Runtime.getRuntime().availableProcessors();
		} else {
			workerNum = 1;
		}
		workers = new Worker[workerNum];
		for (int i = 0; i < workerNum; i++) {
			workers[i] = new Worker();
			workers[i].start();
		}
		Logger.getLogger(ComputerImpl.class.getName()).log(Level.INFO,
				"Computer: started with " + workerNum + " workers.");
	}

	public static void main(String[] args) throws Exception {
		System.setSecurityManager(new SecurityManager());
		final String domainName = args.length == 0 ? "localhost" : args[0];
		final String url = "rmi://" + domainName + ":" + Space.PORT + "/"
				+ Space.SERVICE_NAME;
		space = (Space) Naming.lookup(url);
		ComputerImpl computer = new ComputerImpl();
		space.register(computer);
		// Main thread waiting for Key Enter to terminate.
		try {
			System.in.read();
		} catch (Throwable ignored) {

		}
		Logger.getLogger(ComputerImpl.class.getName()).log(Level.INFO,
				"Computer: " + computer.ID + " exited.");
		System.exit(-1);
	}

	/**
	 * Set an ID to the computer. Call from Space.
	 * 
	 * @param computerID
	 *            Computer ID.
	 * @throws RemoteException
	 *             Failed to connect computer.
	 */
	@Override
	public void setID(int computerID) throws RemoteException {
		this.ID = computerID;
	}

	/**
	 * Get the number of Workers running in the Computer. Call from Space.
	 * 
	 * @return Number of Workers.
	 * @throws RemoteException
	 *             Failed to connect to computer.
	 */
	@Override
	public int getWorkerNum() throws RemoteException {
		return workerNum;
	}

	/**
	 * Add a task to Ready Task Queue. Call from Computer Proxy in Space.
	 * 
	 * @param task
	 *            The Task to be added.
	 * @throws RemoteException
	 *             Failed to connect to Computer.
	 */
	@Override
	public void addTask(Task<?> task) throws RemoteException {
		try {
			readyTaskQueue.put(task);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get a Result from Result Queue. Call from Computer Proxy in Space.
	 * 
	 * @return The execution Result.
	 * @throws RemoteException
	 *             Failed to connect to computer.
	 */
	@Override
	public Result getResult() throws RemoteException {
		return resultQueue.poll();
	}

	/**
	 * Get a Task from the Ready Task Queue.
	 * 
	 * @return Task
	 */
	private Task<?> getReadyTask() {
		try {
			return readyTaskQueue.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;
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
	 * Cache the subtasks in Task Result into Ready Task Queue.
	 * 
	 * @param result
	 *            Task Result
	 */
	private <T> void cacheTasks(TaskResult<T> result) {
		List<Task<T>> runningtasks = result.getRunningTasks();
		for (int i = 0; i < runningtasks.size(); i++) {
			try {
				readyTaskQueue.put(runningtasks.get(i));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
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
	 * 
	 * A Worker is a thread to get tasks from Computer Ready Queue and execute
	 * them, put the results into Computer Result Task Queue.
	 *
	 */
	private class Worker extends Thread {
		@Override
		public void run() {
			while (true) {
				Task<?> task = getReadyTask();
				if (!task.getID().contains(":W")) {
					task.setID(task.getID() + ":W" + makeTaskID());
				}
				if (Config.STATUSOUTPUT) {
					System.out.println("Worker: Task " + task.getID()
							+ " is taken!");
				}
				Result result = execute(task);
				addResult(result);
				if (Config.STATUSOUTPUT) {
					System.out.println("Worker: Result " + result.getID()
							+ " is added!");
				}
				if (result.isCoarse()) {
					continue;
				}
				// Bug here!
				if (Config.AmeliorationFlag) {
					if (result.getType() == Result.TASKRESULT) {
						((TaskResult<?>) result)
								.setRunningTasks(Config.CacheTaskNum);
						cacheTasks((TaskResult<?>) result);
					}
				}
			}
		}

		/*
		 * Execute the task and generate the result. Assign every subtask with
		 * an task ID.
		 */
		private <T> Result execute(Task<T> task) {
			// Call the task's execution method.
			final Result result = task.execute();
			// If the result is Value Result, return the result directly.
			if (result.getType() == Result.VALUERESULT) {
				return result;
			} else {
				// If the result is Task Result, assign subtasks with Task ID.
				@SuppressWarnings("unchecked")
				List<Task<T>> subtasks = (List<Task<T>>) ((TaskResult<T>) result)
						.getSubTasks();
				// Assign Successor Task with an Task ID
				Task<?> successor = subtasks.get(0);
				String taskid[] = task.getID().split(":"); 
				successor.setID(task.getID().replace(":" + taskid[9], ":W" + makeTaskID()));
				if (Config.STATUSOUTPUT) {
					System.out.println("Worker: Successor " + successor.getID());
				}
				// Assign other Ready Task with Task IDs
				for (int i = 1; i < subtasks.size(); i++) {
					Task<?> subtask = subtasks.get(i);
					subtask.setID(task.getID().replace(":" + taskid[9], ":W" + makeTaskID()));
					if (Config.STATUSOUTPUT) {
						System.out.println("Worker: Subtask " + subtask.getID() +  " -- " + subtask.getArg().get(0));
					}
					subtask.setTargetID(successor.getID());
				}
			}
			return result;
		}
	}

	/**
	 * Exit
	 */
	@Override
	public void exit() throws RemoteException {
		Logger.getLogger(this.getClass().getName()).log(Level.INFO,
				"Computer: exiting.");
		System.exit(0);
	}

}
