/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.handler.advice;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.Message;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;

/**
 * Base class for {@link MessageHandler} advice classes. Subclasses should provide
 * an implementation for {@link #doInvoke(ExecutionCallback, Object, Message)}.
 * Used to advise the handleRequestMessage method for {@link AbstractReplyProducingMessageHandler} or
 * {@link MessageHandler#handleMessage(Message)} for other message handlers.
 *
 * @author Gary Russell
 * @since 2.2
 *
 */
public abstract class AbstractRequestHandlerAdvice extends IntegrationObjectSupport
	implements MethodInterceptor {

	protected final Log logger = LogFactory.getLog(this.getClass());

	public final Object invoke(final MethodInvocation invocation) throws Throwable {

		Method method = invocation.getMethod();
		Object[] arguments = invocation.getArguments();
		boolean isMessageMethod = (method.getName().equals("handleRequestMessage") || method.getName().equals("handleMessage"))
				&& (arguments.length == 1 && arguments[0] instanceof Message);

		if (!isMessageMethod) {
			return invocation.proceed();
		}
		else {
			Message<?> message = (Message<?>) arguments[0];
			try {
				return doInvoke(new ExecutionCallback(){

					public Object execute() throws Exception {
						try {
							return invocation.proceed();
						}
						catch (Throwable e) {
							throw new ThrowableHolderException(e);
						}
					}
				}, invocation.getThis(), message);
			}
			catch (Exception e) {
				if (e instanceof ThrowableHolderException) {
					throw e.getCause();
				}
				else {
					throw e;
				}
			}
		}
	}

	/**
	 * Subclasses implement this method to apply behavior to the {@link MessageHandler}.<p/> callback.execute()
	 * invokes the handler method and returns its result, or null.
	 * @param callback Subclasses invoke the execute() method on this interface to invoke the handler method.
	 * @param target The target handler.
	 * @param message The message that will be sent to the handler.
	 * @return the result after invoking the {@link MessageHandler}.
	 * @throws Exception
	 */
	protected abstract Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception;

	protected interface ExecutionCallback {

		Object execute() throws Exception;
	}

	@SuppressWarnings("serial")
	private class ThrowableHolderException extends RuntimeException {

		public ThrowableHolderException(Throwable cause) {
			super(cause);
		}
	}
}