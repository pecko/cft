/*******************************************************************************
 * Copyright (c) 2012, 2015 Pivotal Software, Inc. 
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
package org.eclipse.cft.server.standalone.internal.application;

import org.cloudfoundry.client.lib.archive.ApplicationArchive;
import org.eclipse.cft.server.core.internal.CloudFoundryProjectUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.application.ModuleResourceApplicationDelegate;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.model.IModuleResource;

/**
 * 
 * Determines if a give module is a Java standalone application. Also provides
 * an archiving mechanism that is specific to Java standalone applications.
 * 
 */
public class StandaloneApplicationDelegate extends
		ModuleResourceApplicationDelegate {

	public StandaloneApplicationDelegate() {

	}

	public boolean requiresURL() {
		// URLs are optional for Java standalone applications
		return false;
	}

	@Override
	public boolean shouldSetDefaultUrl(CloudFoundryApplicationModule appModule) {
		return CloudFoundryProjectUtil.isSpringBoot(appModule);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cft.server.core.internal.application.
	 * AbstractApplicationDelegate
	 * #getApplicationArchive(org.eclipse.cft.internal
	 * .server.core.client.CloudFoundryApplicationModule,
	 * org.eclipse.cft.server.core.internal.CloudFoundryServer,
	 * org.eclipse.wst.server.core.model.IModuleResource[])
	 */
	public ApplicationArchive getApplicationArchive(
			CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer, IModuleResource[] moduleResources,
			IProgressMonitor monitor) throws CoreException {
		return new JavaCloudFoundryArchiver(appModule, cloudServer)
				.getApplicationArchive(monitor);
	}

}
