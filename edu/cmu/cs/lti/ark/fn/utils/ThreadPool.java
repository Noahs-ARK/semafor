/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * ThreadPool.java is part of SEMAFOR 2.0.
 * 
 * SEMAFOR 2.0 is free software: you can redistribute it and/or modify  it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * SEMAFOR 2.0 is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. 
 * 
 * You should have received a copy of the GNU General Public License along
 * with SEMAFOR 2.0.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package edu.cmu.cs.lti.ark.fn.utils;

import java.util.LinkedList;

public class ThreadPool extends ThreadGroup {

	private boolean isAlive;

	private LinkedList<Runnable> taskQueue;

	private int threadID;

	private static int threadPoolID;

	/**
	 * Creates a new ThreadPool.
	 * 
	 * @param numThreads
	 *            The number of threads in the pool.
	 */
	public ThreadPool(int numThreads) {
		super("ThreadPool-" + (threadPoolID++));
		setDaemon(true);

		isAlive = true;

		taskQueue = new LinkedList<Runnable>();
		for (int i = 0; i < numThreads; i++) {
			new PooledThread().start();
		}
	}

	/**
	 * Requests a new task to run. This method returns immediately, and the task
	 * executes on the next available idle thread in this ThreadPool.
	 * <p>
	 * Tasks start execution in the order they are received.
	 * 
	 * @param task
	 *            The task to run. If null, no action is taken.
	 * @throws IllegalStateException
	 *             if this ThreadPool is already closed.
	 */
	public synchronized void runTask(Runnable task) {
		if (!isAlive) {
			throw new IllegalStateException();
		}
		if (task != null) {
			taskQueue.add(task);
			notify();
		}

	}

	protected synchronized Runnable getTask() throws InterruptedException {
		while (taskQueue.size() == 0) {
			if (!isAlive) {
				return null;
			}
			wait();
		}
		return (Runnable) taskQueue.removeFirst();
	}

	/**
	 * Closes this ThreadPool and returns immediately. All threads are stopped,
	 * and any waiting tasks are not executed. Once a ThreadPool is closed, no
	 * more tasks can be run on this ThreadPool.
	 */
	public synchronized void close() {
		if (isAlive) {
			isAlive = false;
			taskQueue.clear();
			interrupt();
		}
	}

	/**
	 * Closes this ThreadPool and waits for all running threads to finish. Any
	 * waiting tasks are executed.
	 */
	public void join() {
		// notify all waiting threads that this ThreadPool is no
		// longer alive
		synchronized (this) {
			isAlive = false;
			notifyAll();
		}

		// wait for all threads to finish
		Thread[] threads = new Thread[activeCount()];
		int count = enumerate(threads);
		for (int i = 0; i < count; i++) {
			try {
				threads[i].join();
			} catch (InterruptedException ex) {
			}
		}
	}

	/**
	 * A PooledThread is a Thread in a ThreadPool group, designed to run tasks
	 * (Runnables).
	 */
	private class PooledThread extends Thread {

		public PooledThread() {
			super(ThreadPool.this, "PooledThread-" + (threadID++));
		}

		public void run() {
			while (!isInterrupted()) {

				// get a task to run
				Runnable task = null;
				try {
					task = getTask();
				} catch (InterruptedException ex) {
				}

				// if getTask() returned null or was interrupted,
				// close this thread by returning.
				if (task == null) {
					return;
				}

				// run the task, and eat any exceptions it throws
				try {
					task.run();
				} catch (Throwable t) {
					uncaughtException(this, t);
				}
			}
		}
	}
}
