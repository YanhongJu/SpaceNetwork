package computer;

import java.rmi.Remote;
import java.rmi.RemoteException;

import api.Result;
import api.Task;

/**
 * Computer interface is exposed to Computer and Space.
 *
 */
public interface Computer extends Remote {
	/**
	 * Set an ID to the computer. Call from Space.
	 * 
	 * @param computerId
	 *            Computer Id.
	 * @throws RemoteException
	 *             Failed to connect to computer.
	 */
	public void setID(int computerId) throws RemoteException;

	/**
	 * Get Computer ID. Call from Space.
	 * 
	 * @return Computer ID
	 * @throws RemoteException
	 *             Failed to connect to computer.
	 */
	public int getID() throws RemoteException;

	/**
	 * Get the number of processes running in the Computer. Call from Space.
	 * 
	 * @return Number of processes.
	 * @throws RemoteException
	 *             Failed to connect to computer.
	 */
	public int getProcessNum() throws RemoteException;

	/**
	 * Add a task to local ready task queue. Call from Space.
	 * 
	 * @param task
	 *            The task to be added.
	 * @throws RemoteException
	 *             Failed to connect to computer.
	 */
	public void addTask(Task task) throws RemoteException;

	/**
	 * Take result from local result queue. Call from Space.
	 * 
	 * @return The execution result.
	 * @throws RemoteException
	 *             Failed to connect to computer.
	 */
	public Result takeResult() throws RemoteException;

	/**
	 * Execute the task.
	 * 
	 * @param task
	 *            The task to be executed.
	 * @return Result of the execution.
	 * @throws RemoteException
	 *             Failed to connect to computer.
	 * 
	 */
	public Result execute(Task task) throws RemoteException;

	/**
	 * Exit
	 * 
	 * @throws RemoteException
	 *             Failed to connect to computer.
	 */
	public void exit() throws RemoteException;

}