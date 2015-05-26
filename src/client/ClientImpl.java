package client;

import java.awt.BorderLayout;
import java.awt.Container;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import config.Config;
import api.Result;
import api.Task;
import server.Server;

public class ClientImpl<T> extends JFrame implements Client, Serializable {
	private static final long serialVersionUID = -4472984886617837870L;
	protected T taskReturnValue;
	private long clientStartTime;

	/**
	 * Client ID.
	 */
	private int ID;

	/**
	 * Ready Task Queue. Containing tasks ready to run.
	 */
	private final BlockingQueue<Task> readyTaskQueue;

	/**
	 * Result Queue. Containing the final result of the submitted task.
	 */
	private final BlockingQueue<Result> resultQueue;

	/**
	 * Task ID Queue. Containing the status of task submitted.
	 */
	private final BlockingQueue<String> taskIDQueue;

	/**
	 * Constructor of Client Implementation. Register to the Server.
	 * 
	 * @param title
	 *            Frame title.
	 * @param ServerDomainName
	 *            Server Domain Name
	 * @throws NotBoundException
	 *             Wrong Server Domain Name
	 * @throws MalformedURLException
	 *             Wrong Server Domain Name
	 * @throws RemoteException
	 *             Cannot connect to Server
	 */
	public ClientImpl(final String title, final String ServerDomainName)
			throws NotBoundException, MalformedURLException, RemoteException {
//		setTitle(title);
	//	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		readyTaskQueue = new LinkedBlockingQueue<>();
		resultQueue = new LinkedBlockingQueue<>();
		taskIDQueue = new LinkedBlockingQueue<>();


	}

	/**
	 * Set an ID to the Client. Call from Client Proxy in Server.
	 * 
	 * @param clientID
	 *            Client Id.
	 * @throws RemoteException
	 *             Failed to connect to Client.
	 */
	@Override
	public void setID(int clientID) throws RemoteException {
		this.ID = clientID;
	}


	/**
	 * Add a Task to Ready Task Queue.
	 * 
	 * @param task
	 *            Task to be added.
	 */
	public void addTask(Task task) {
		try {
			readyTaskQueue.put(task);
			if (Config.DEBUG) {
				System.out.println("Client: Task is put.");
				System.out.println("Client: " + "Ready Task Queue Size is " + readyTaskQueue.size());
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get a Task from Ready Task Queue. Call from Client Proxy in Server.
	 * 
	 * @return Task
	 * @throws RemoteException
	 *             Failed to connect to Client.
	 */
	@Override
	public Task getTask() throws RemoteException {
		try {
			if (Config.DEBUG) {
				System.out.println("Client: " + "Ready Task Queue Size is " + readyTaskQueue.size());
				Task task = readyTaskQueue.take();
				System.out.println("Client: Task is taken");
				return task;
			}
			return readyTaskQueue.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Add a Result to the Result Queue. Call from Client Proxy in Server.
	 * 
	 * @throws RemoteException
	 *             Failed to connect to Client.
	 */
	@Override
	public void addResult(Result result) throws RemoteException {
		try {
			resultQueue.put(result);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get a Result from the Result Queue. Call from Client.
	 * 
	 * @return Result
	 */
	public Result getResult() {
		try {
			return resultQueue.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Add Task ID to Task ID Queue. Call from Client Proxy in Server. For the
	 * Client to keep track of submitted Task.
	 * 
	 * @param TaskID
	 *            Task ID of the submitted task.
	 * @throws RemoteException
	 *             Cannot connect with Client.
	 */
	@Override
	public void addTaskID(String TaskID) throws RemoteException {
		try {
			taskIDQueue.put(TaskID);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get a Task ID from Task ID Queue. Call from Client.
	 */
	public String getTaskID() {
		try {
			return taskIDQueue.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Start counting time.
	 */
	public void begin() {
		clientStartTime = System.nanoTime();
	}

	/**
	 * Stop counting time
	 */
	public void end() {
		Logger.getLogger(ClientImpl.class.getCanonicalName()).log(Level.INFO,
				"Client time: {0} ms.",
				(System.nanoTime() - clientStartTime) / 1000000);
	}

	/**
	 * Add Graph
	 * 
	 * @param jLabel
	 *            label
	 */
/*	public void add(final JLabel jLabel) {
		final Container container = getContentPane();
		container.setLayout(new BorderLayout());
		container.add(new JScrollPane(jLabel), BorderLayout.CENTER);
		pack();
		setVisible(true);
	}*/

	@Override
	public int getID() throws RemoteException {
		return this.ID;
	}

}
