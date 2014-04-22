/**
 * Copyright (C) 2014 Holger Hopmann (h.hopmann@uni-muenster.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.hopmann.msc.commons.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Threading facility which ensures reusability of scheduled or currently
 * processed items. Threads triggering executions associated to the same input
 * value will receive a shared result and await the same output.
 * 
 * @param <Input>
 * @param <LoadResult>
 * @param <DispatchResult>
 */
public abstract class DispatchingExecutor<Input, LoadResult, DispatchResult> {

	public abstract static class SimpleDispatchingExecutor<Input, Result>
			extends DispatchingExecutor<Input, Result, Result> {
		public SimpleDispatchingExecutor(ExecutorService executor) {
			super(executor);
		}

		@Override
		protected Result dispatch(Result value) throws Exception {
			return value;
		}

	}

	private class Task {

		private final Input inputValue;
		private List<Future<DispatchResult>> dispatchFutureList = new ArrayList<>();
		private Future<LoadResult> loadFuture;
		private final Callable<LoadResult> loadCallable;

		public Task(Input value) {
			this.inputValue = value;
			loadCallable = new Callable<LoadResult>() {
				@Override
				public LoadResult call() throws Exception {
					return load(inputValue);
				}
			};
		}

		private void removeDispatchFuture(Future<DispatchResult> taskHolder) {
			synchronized (dispatchFutureList) {
				dispatchFutureList.remove(taskHolder);
				if (dispatchFutureList.isEmpty()) {
					// No more holders waiting for result -> canceling execution
					if (!loadFuture.isDone()) {
						loadFuture.cancel(true);
					}
					removeTask(this);
				}
			}
		}

		private void addDispatchFuture(Future<DispatchResult> taskHolder) {
			synchronized (dispatchFutureList) {
				dispatchFutureList.add(taskHolder);
				if (dispatchFutureList.size() == 1) {
					loadFuture = executor.submit(loadCallable);
				}
			}
		}

		public Future<DispatchResult> createTaskDispatchFuture() {
			Future<DispatchResult> taskDispatchFuture = new Future<DispatchResult>() {

				@Override
				public boolean cancel(boolean mayInterruptIfRunning) {
					removeDispatchFuture(this);
					return true;
				}

				@Override
				public DispatchResult get() throws InterruptedException,
						ExecutionException {
					try {
						return dispatch(loadFuture.get());
					} catch (Exception e) {
						throw new ExecutionException(e);
					} finally {
						removeDispatchFuture(this);
					}
				}

				@Override
				public DispatchResult get(long timeout, TimeUnit unit)
						throws InterruptedException, ExecutionException,
						TimeoutException {
					try {
						return dispatch(loadFuture.get(timeout, unit));
					} catch (Exception e) {
						throw new ExecutionException(e);
					} finally {
						removeDispatchFuture(this);
					}
				}

				@Override
				public boolean isCancelled() {
					return loadFuture.isCancelled();
				}

				@Override
				public boolean isDone() {
					return loadFuture.isDone();
				}
			};

			addDispatchFuture(taskDispatchFuture);
			return taskDispatchFuture;
		}

	}

	private Map<Input, Task> taskMap = new HashMap<Input, Task>();
	private final ExecutorService executor;

	public DispatchingExecutor(ExecutorService executor) {
		this.executor = executor;
	}

	public Future<DispatchResult> submit(Input value) {

		Task task = taskMap.get(value);
		if (task == null) {
			task = new Task(value);
			taskMap.put(value, task);
		}

		return task.createTaskDispatchFuture();
	}

	private void removeTask(Task task) {
		synchronized (taskMap) {
			taskMap.remove(task.inputValue);
		}
	}

	protected abstract LoadResult load(Input value) throws Exception;

	protected abstract DispatchResult dispatch(LoadResult value)
			throws Exception;

}
