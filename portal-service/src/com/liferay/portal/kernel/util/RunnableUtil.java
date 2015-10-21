/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.kernel.util;

import com.liferay.portal.kernel.io.unsync.UnsyncFilterOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * @author Brian Wing Shun Chan
 */
public class RunnableUtil {

	public static void runWithSwappedSystemOut(
		Runnable runnable, OutputStream outputStream) {

		SwappedOutputStream swappedOutputStream = null;

		synchronized (RunnableUtil.class) {
			swappedOutputStream = new SwappedOutputStream(
				outputStream, System.out, Thread.currentThread());

			_swappedPrintStreams.push(swappedOutputStream);

			System.setOut(new PrintStream(swappedOutputStream));
		}

		try {
			runnable.run();
		}
		finally {
			synchronized (RunnableUtil.class) {
				swappedOutputStream._enabled = false;

				if (_swappedPrintStreams.peek() == swappedOutputStream) {
					_swappedPrintStreams.pop();

					System.setOut(swappedOutputStream._fallbackOutputStream);

					Iterator<SwappedOutputStream> iterator =
						_swappedPrintStreams.iterator();

					while (iterator.hasNext()) {
						swappedOutputStream = iterator.next();

						if (swappedOutputStream._enabled) {
							break;
						}

						iterator.remove();

						System.setOut(
							swappedOutputStream._fallbackOutputStream);
					}
				}
			}
		}
	}

	private static final LinkedList<SwappedOutputStream> _swappedPrintStreams =
		new LinkedList<>();

	private static class SwappedOutputStream extends UnsyncFilterOutputStream {

		@Override
		public void write(byte[] bytes, int offset, int length)
			throws IOException {

			Thread thread = Thread.currentThread();

			if ((thread == _thread) && _enabled) {
				super.write(bytes, offset, length);
			}
			else {
				_fallbackOutputStream.write(bytes, offset, length);
			}
		}

		@Override
		public void write(int b) throws IOException {
			Thread thread = Thread.currentThread();

			if ((thread == _thread) && _enabled) {
				super.write(b);
			}
			else {
				_fallbackOutputStream.write(b);
			}
		}

		private SwappedOutputStream(
			OutputStream outputStream, PrintStream fallbackOutputStream,
			Thread thread) {

			super(outputStream);

			_fallbackOutputStream = fallbackOutputStream;
			_thread = thread;
		}

		private volatile boolean _enabled = true;
		private final PrintStream _fallbackOutputStream;
		private final Thread _thread;

	}

}