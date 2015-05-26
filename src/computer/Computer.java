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
	 * Get the number of Workers running in the Computer. Call from Space.
	 * 
	 * @return Number of Workers.
	 * @throws RemoteException
	 *             Failed to connect to Computer.
	 */
	public int getWorkerNum() throws RemoteException;

	/**
	 * Add a task to Ready Task Queue. Call from Computer Proxy in Space.
	 * 
	 * @param task
	 *            The Task to be added.
	 * @throws RemoteException
	 *             Failed to connect to Computer.
	 */
	public void addTask(Task task) throws RemoteException;

	/**
	 * Get a Result from Result Queue. Call from Computer Proxy in Space.
	 * 
	 * @return The execution Result.
	 * @throws RemoteException
	 *             Failed to connect to computer.
	 */
	public Result getResult() throws RemoteException;

	/**
	 * Execute the Task.
	 * 
	 * @param task
	 *            The Task to be executed.
	 * @return Result of the execution.
	 * @throws RemoteException
	 *             Failed to connect to Computer.
	 * 
	 */
	public Result execute(Task task) throws RemoteException;

	/**
	 * Exit
	 * 
	 * @throws RemoteException
	 *             Failed to connect to Computer.
	 */
	public void exit() throws RemoteException;

}