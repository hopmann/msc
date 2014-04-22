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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Recurring structures for working with {@link Future}s
 * 
 */
public class FutureHelper {

	private static class ImmediateFuture<V> implements Future<V> {

		private V value;

		public ImmediateFuture(V value) {
			this.value = value;
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return false;
		}

		@Override
		public V get() throws InterruptedException, ExecutionException {
			return value;
		}

		@Override
		public V get(long timeout, TimeUnit unit) throws InterruptedException,
				ExecutionException, TimeoutException {
			return get();
		}

		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public boolean isDone() {
			return true;
		}

	}

	private static class ExceptionFuture<V> implements Future<V> {

		private Exception cause;

		public ExceptionFuture(Exception cause) {
			this.cause = cause;
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return false;
		}

		@Override
		public V get() throws InterruptedException, ExecutionException {
			throw new ExecutionException(cause);
		}

		@Override
		public V get(long timeout, TimeUnit unit) throws InterruptedException,
				ExecutionException, TimeoutException {
			return get();
		}

		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public boolean isDone() {
			return true;
		}

	}

	public static <T> Future<T> createImmediateFuture(T value) {
		return new ImmediateFuture<T>(value);
	}

	public static <T> Future<T> createExceptionFuture(Exception cause) {
		return new ExceptionFuture<T>(cause);
	}
}
