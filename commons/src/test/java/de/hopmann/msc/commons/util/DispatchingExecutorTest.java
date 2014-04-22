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
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

import org.junit.Test;

import de.hopmann.msc.commons.util.DispatchingExecutor.SimpleDispatchingExecutor;

public class DispatchingExecutorTest {

	/**
	 * Ensures that submitting multiple tasks with the same input value does
	 * trigger the loading sequence only once for each unique input value
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void testSingleLoading() throws InterruptedException {

		final AtomicInteger loadCounter = new AtomicInteger(0);

		SimpleDispatchingExecutor<String, String> dispatchingExecutor = new DispatchingExecutor.SimpleDispatchingExecutor<String, String>(
				Executors.newCachedThreadPool()) {

			@Override
			protected String load(String value) throws Exception {
				loadCounter.incrementAndGet();
				Thread.sleep(1000);
				return new String();
			}

		};

		dispatchingExecutor.submit("Test");
		dispatchingExecutor.submit("Test");
		dispatchingExecutor.submit("Test");
		dispatchingExecutor.submit("Test2");
		dispatchingExecutor.submit("Test2");
		dispatchingExecutor.submit("Test2");
		Thread.sleep(500);
		assertEquals(2, loadCounter.get());
	}

	/**
	 * Ensures that canceling a returned Future will only cancel the actual
	 * execution if no other submit calls are waiting for the corresponding
	 * result
	 * 
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	@Test
	public void testCancelation() throws InterruptedException,
			ExecutionException {

		SimpleDispatchingExecutor<String, String> dispatchingExecutor = new DispatchingExecutor.SimpleDispatchingExecutor<String, String>(
				Executors.newCachedThreadPool()) {

			@Override
			protected String load(String value) throws Exception {
				Thread.sleep(1000);
				return value;
			}

		};

		Future<String> submit = dispatchingExecutor.submit("Test");
		Future<String> submit2 = dispatchingExecutor.submit("Test");
		Thread.sleep(500);

		submit.cancel(true);
		assertEquals("Test", submit2.get());
	}
}
