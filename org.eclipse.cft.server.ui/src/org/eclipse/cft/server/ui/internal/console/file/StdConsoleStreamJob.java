/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution. 
 * 
 * The Eclipse Public License is available at 
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * and the Apache License v2.0 is available at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * You may elect to redistribute this code under either of these licenses.
 *  
 *  Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
package org.eclipse.cft.server.ui.internal.console.file;

import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

class StdConsoleStreamJob extends Job implements IConsoleJob {

	private String toStream;

	private LocalConsoleStream content;

	public StdConsoleStreamJob(LocalConsoleStream content) {
		super(content.getContentType().getId());
		this.content = content;
	}

	protected IStatus run(IProgressMonitor monitor) {

		if (toStream != null) {
			try {
				content.write(toStream);
				toStream = null;
			}
			catch (CoreException e) {
				CloudFoundryPlugin.logError(
						"Failed to write message to Cloud Foundry console due to - " + e.getMessage(), e); //$NON-NLS-1$
			}
		}

		return Status.OK_STATUS;
	}

	public void write(String message) {
		this.toStream = message;
		if (this.toStream != null) {
			schedule();
		}
	}

	public void close() {
		content.close();
	}

	public IContentType getContentType() {
		return content.getContentType();
	}

}