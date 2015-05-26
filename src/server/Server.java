package server;

import java.rmi.Remote;
import java.rmi.RemoteException;

import api.Result;
import api.Task;
import client.ClientImpl;

public interface Server extends Remote {
	/**
	 * The port used by the RMI registry.
	 */
	public static int PORT = 8001;

	/**
	 * The service name associated with this Remote interface.
	 */
	public static String SERVICE_NAME = "Server";

	/**
	 * Set an ID to the Server. Call from Universe.
	 * 
	 * @param serverID
	 *            Server Id.
	 * @throws RemoteException
	 *             Failed to connect to Server.
	 */
	public void setID(final int serverID) throws RemoteException;

	/**
	 * Get a Task from Ready Task Queue. Call from Server Proxy in Universe.
	 * 
	 * @return Task
	 * @throws RemoteException
	 *             Cannot connect with Server.
	 */
	public Task getTask() throws RemoteException;

	/**
	 * Dispatch the Result to corresponding Client Proxy. Call from Server Proxy
	 * in Universe. If the Client is down, discard the result.
	 * 
	 * @param result
	 *            Result to be dispatched.
	 * @throws RemoteExcemption
	 *             Cannot connect with Server.
	 */
	public void dispatchResult(final Result result) throws RemoteException;

	/**
	 * Register a Client in Server. Call from Client.
	 * 
	 * @param client
	 *            Client to be registered.
	 * @throws RemoteException
	 *             Cannot connect with Server.
	 */
	void register(final ClientImpl<?> client) throws RemoteException;

}
