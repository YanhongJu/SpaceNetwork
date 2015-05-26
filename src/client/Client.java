package client;

import java.rmi.Remote;
import java.rmi.RemoteException;

import api.Result;
import api.Task;

public interface Client extends Remote {
	/**
	 * Set an ID to the Client. Call from Client Proxy in Server.
	 * 
	 * @param clientID
	 *            Client Id.
	 * @throws RemoteException
	 *             Failed to connect to Client.
	 */
	public void setID(final int clientID) throws RemoteException;
	
	/**
	 * Set an ID to the Client. Call from Client Proxy in Server.
	 * 
	 * @param clientID
	 *            Client Id.
	 * @throws RemoteException
	 *             Failed to connect to Client.
	 */
	public int getID() throws RemoteException;

	/**
	 * Get a Task from Ready Task Queue. Call from Client Proxy in Server.
	 * 
	 * @return Task
	 * @throws RemoteException
	 *             Failed to connect to Client.
	 */
	public Task getTask() throws RemoteException;

	/**
	 * Add a Result to the Result Queue. Call from Client Proxy in Server.
	 * 
	 * @throws RemoteException
	 *             Failed to connect to Client.
	 */
	public void addResult(final Result result) throws RemoteException;

	/**
	 * Add Task ID to Task ID Queue. Call from Client Proxy in Server. For the
	 * Client to keep track of submitted Task.
	 * 
	 * @param TaskID
	 *            Task ID of the submitted task.
	 * @throws RemoteException
	 *             Cannot connect with Client.
	 */
	public void addTaskID(final String TaskID) throws RemoteException;
}
