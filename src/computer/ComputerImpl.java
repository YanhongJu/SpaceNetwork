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

import api.Result;
import api.Task;
import result.TaskResult;
import space.Space;
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
	private final static AtomicInteger taskID = new AtomicInteger();

	/**
	 * Ready Task Queue.
	 */
	private final BlockingQueue<Task> readyTaskQueue;

	/**
	 * Result Queue.
	 */
	private final BlockingQueue<Result> resultQueue;

	/**
	 * Number of processes, same as number of task proxies.
	 */
	private final int ProcessNum;

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
			ProcessNum = Runtime.getRuntime().availableProcessors();
		} else {
			ProcessNum = 1;
		}
		workers = new Worker[ProcessNum];
		for (int i = 0; i < ProcessNum; i++) {
			workers[i] = new Worker();
			workers[i].start();
		}
		Logger.getLogger(ComputerImpl.class.getName()).log(Level.INFO,
				"Computer: started with " + ProcessNum + " threads.");
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
	 * Get Computer ID. Call from Space.
	 * 
	 * @return Computer ID.
	 * @throws RemoteException
	 *             Failed to connect to computer.
	 */
	@Override
	public int getID() throws RemoteException {
		return this.ID;
	}

	/**
	 * Get the number of processes running in the Computer. Call from Space.
	 * 
	 * @return Number of processes.
	 * @throws RemoteException
	 *             Failed to connect to computer.
	 */
	@Override
	public int getProcessNum() throws RemoteException {
		return ProcessNum;
	}

	/**
	 * Add a task to local ready task queue. Call from Space.
	 * 
	 * @param task
	 *            Task to be added.
	 * @throws RemoteException
	 *             Failed to connect to computer.
	 */

	@Override
	public void addTask(Task task) throws RemoteException {
		try {
			readyTaskQueue.put(task);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Take result from local result queue. Call from Space.
	 * 
	 * @return Reult of the execution.
	 * @throws RemoteException
	 *             Failed to connect computer.
	 */
	@Override
	public Result takeResult() throws RemoteException {
		Result result = null;
		result = resultQueue.poll();
		return result;
	}

	/**
	 * Execute the task and generate the result. Assign every subtask with an
	 * task ID.
	 */
	@Override
	public Result execute(Task task) {
		// Call the task's execution method.
		final Result result = task.execute();
		// If the result is Value Result, return the result directly.
		if (result.getResultType() == Result.VALUERESULT) {
			return result;
		} else {
			// If the result is Task Result, assgin subtasks with Task ID.
			List<Task> subtasks = ((TaskResult) result).getSubTasks();
			// Assign Successor Task with an Task ID
			Task successor = subtasks.get(0);
			String successorTaskID = ID + ":"
					+ Thread.currentThread().getId() + ":"
					+ taskID.getAndIncrement();
			successor.setTaskID(successorTaskID);
			// Assign other Ready Task with Task IDs
			for (int i = 1; i < subtasks.size(); i++) {
				String taskid = ID + ":"
						+ Thread.currentThread().getId() + ":"
						+ taskID.getAndIncrement();
				Task subtask = subtasks.get(i);
				subtask.setTaskID(taskid);
				subtask.setTargetTaskID(successorTaskID);
			}
			// Assign running tasks with Task IDs and put them into Computer
			// Ready Task Queue
			List<Task> runningtasks = ((TaskResult) result).getRunningTask();
			for (int i = 0; i < runningtasks.size(); i++) {
				String taskid = ID + ":"
						+ Thread.currentThread().getId() + ":"
						+ taskID.getAndIncrement();
				Task runningtask = runningtasks.get(i);
				runningtask.setTaskID(taskid);
				runningtask.setTargetTaskID(successorTaskID);
			}
		}
		return result;
	}

	/**
	 * Cache the subtasks in Task Result into Ready Task Queue.
	 * 
	 * @param result
	 *            Task Result
	 */
	private void cacheTasks(TaskResult result) {
		List<Task> runningtasks = result.getRunningTask();
		for (int i = 0; i < runningtasks.size(); i++) {
			try {
				readyTaskQueue.put(runningtasks.get(i));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 
	 * A Worker is a thread to get tasks from Computer Ready Queue and
	 * execute them, put the results into Computer Result Task Queue.
	 *
	 */
	private class Worker extends Thread {
		@Override
		public void run() {
			while (true) {
				Task task = null;
				try {
					task = readyTaskQueue.take();
					Result result = execute(task);
					resultQueue.put(result);
					if (Config.AmeliorationFlag) {
						if (result.getResultType() == Result.TASKRESULT) {
							cacheTasks((TaskResult) result);
						}
					}
					if (Config.STATUSOUTPUT) {
						System.out.println(result.getResultId());
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
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
