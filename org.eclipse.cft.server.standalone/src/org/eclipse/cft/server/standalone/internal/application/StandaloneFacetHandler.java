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

import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryProjectUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

/**
 * Helper that determines if a project has the Java standalone facet. See the
 * Eclipse WST project facet framework for more information.
 * 
 * 
 */
public class StandaloneFacetHandler {

	private final IProject project;

	public static final String ID_JAVA_STANDALONE_APP_VERSION = "1.0"; //$NON-NLS-1$
	public static final String ID_MODULE_STANDALONE = "cloudfoundry.standalone.app"; //$NON-NLS-1$

	public StandaloneFacetHandler(IProject project) {
		this.project = project;
	}

	public static final IProjectFacet FACET = ProjectFacetsManager
			.getProjectFacet(ID_MODULE_STANDALONE);

	public boolean hasFacet() {
		try {
			IFacetedProject facetedProject = ProjectFacetsManager
					.create(project);
			return facetedProject != null
					&& facetedProject.hasProjectFacet(FACET);
		} catch (CoreException e) {
			CloudFoundryPlugin.log(e);
			return false;
		}
	}

	public void addFacet(IProgressMonitor monitor) {
		if (canAddFacet()) {
			try {
				IFacetedProject facetedProject = ProjectFacetsManager
						.create(project);
				if (facetedProject == null) {
					facetedProject = ProjectFacetsManager.create(project, true,
							monitor);
				}
				if (facetedProject != null) {
					facetedProject.installProjectFacet(
							FACET.getDefaultVersion(), null, null);
				}
			} catch (CoreException e) {
				CloudFoundryPlugin.log(e);
			}
		}
	}

	public void removeFacet() {
		if (hasFacet()) {
			try {
				IFacetedProject facetedProject = ProjectFacetsManager
						.create(project);
				if (facetedProject != null) {
					facetedProject.uninstallProjectFacet(
							FACET.getDefaultVersion(), null, null);
				}
			} catch (CoreException e) {
				CloudFoundryPlugin.log(e);
			}
		}
	}

	public boolean canAddFacet() {
		if (project == null || !project.isAccessible() || hasFacet()) {
			return false;
		}

		IJavaProject javaProject = CloudFoundryProjectUtil
				.getJavaProject(project);
		return javaProject != null && javaProject.exists();
	}

	public static class CFFacetInstallDelegate implements IDelegate {
		public void execute(IProject project, IProjectFacetVersion fv,
				Object config, IProgressMonitor monitor) throws CoreException {
			if (!new StandaloneFacetHandler(project).canAddFacet()) {
				throw new CoreException(
						CloudFoundryPlugin
								.getErrorStatus("Cloud Foundry Standalone Facet can only be installed on a Java project.")); //$NON-NLS-1$
			}
		}
	}

	public static class CFFacetUninstallDelegate implements IDelegate {
		public void execute(IProject project, IProjectFacetVersion fv,
				Object config, IProgressMonitor monitor) throws CoreException {
			// Nothing. Allow Cloud Foundry facets to always be uninstallable.
		}
	}

}
