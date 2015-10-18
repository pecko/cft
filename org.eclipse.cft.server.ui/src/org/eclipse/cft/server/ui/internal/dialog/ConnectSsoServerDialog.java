/*******************************************************************************
 * Copyright (c) 2015 Pivotal Software, Inc. 
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
package org.eclipse.cft.server.ui.internal.dialog;

import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.client.CloudFoundryClientFactory;
import org.eclipse.cft.server.ui.internal.CloudFoundryServerUiPlugin;
import org.eclipse.cft.server.ui.internal.CloudUiUtil;
import org.eclipse.cft.server.ui.internal.Messages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.internal.browser.WebBrowserPreference;
import org.eclipse.wst.server.core.IServerWorkingCopy;

/**
 * A dialog for connecting to a sso server
 */
public class ConnectSsoServerDialog extends Dialog {
	/**
	 * Passcode text widget.
	 */
	private Text passcodeText;

	/**
	 * Error message label widget.
	 */
	private Text errorMessageText;

	/**
	 * Error message string.
	 */
	private String errorMessage;

	private CloudFoundryServer cloudServer;

	public ConnectSsoServerDialog(Shell shell, CloudFoundryServer cloudServer) {
		super(shell);
		this.cloudServer = cloudServer;
	}

	/*
	 * (non-Javadoc) Method declared on Dialog.
	 */
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.OK_ID) {
			getShell().setCursor(Display.getCurrent().getSystemCursor(SWT.CURSOR_WAIT));
			SafeRunner.run(new SafeRunnable() {
				public void run() throws Exception {
					IServerWorkingCopy server = cloudServer.getServerWorkingCopy();
					if (server == null) {
						server = cloudServer.getServer().createWorkingCopy();
						cloudServer.getBehaviour();
					}
					server.setAttribute(CloudFoundryServer.PROP_PASSCODE_ID, passcodeText.getText());
					server.save(true, new NullProgressMonitor());
					cloudServer.getBehaviour().connect(new NullProgressMonitor());
				}

				@Override
				public void handleException(Throwable e) {
					setErrorMessage(e.getMessage());
					CloudFoundryServerUiPlugin.logWarning(e);
				}
			});
			getShell().setCursor(null);
			if (cloudServer.isConnected()) {
				super.buttonPressed(buttonId);
			}
		} else {
			super.buttonPressed(buttonId);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets
	 * .Shell)
	 */
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(Messages.ConnectSsoServerDialog_CONNECT_TITLE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse
	 * .swt.widgets.Composite)
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, Messages.ConnectSsoServerDialog_CONNECT_BUTTON, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		passcodeText.setFocus();
		String value = cloudServer.getPasscode();
		if (value != null) {
			passcodeText.setText(value);
			passcodeText.selectAll();
		}
	}

	/*
	 * (non-Javadoc) Method declared on Dialog.
	 */
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		GridData gd;
		Link prompt = new Link(composite, SWT.LEFT | SWT.WRAP);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 2;
		prompt.setLayoutData(gd);
		String ssoUrl = CloudUiUtil.getPromptText(cloudServer);
		prompt.setText(ssoUrl);
		prompt.addListener(SWT.Selection, new Listener() {
			
			@Override
			public void handleEvent(Event event) {
				if (cloudServer.getUrl() != null && !cloudServer.getUrl().isEmpty()) {
					try {
						String url = CloudFoundryClientFactory.getSsoUrl(cloudServer.getUrl(), cloudServer.getSelfSignedCertificate());
						CloudUiUtil.openUrl(url, WebBrowserPreference.EXTERNAL);
					}
					catch (Exception e) {
						CloudFoundryServerUiPlugin.logWarning(e);
					}
				}
			}
		});
		Label passcodeLabel = new Label(composite, SWT.NONE);
		passcodeLabel.setText(Messages.LABEL_PASSCODE);
		gd = new GridData(SWT.FILL, SWT.CENTER, false, false);
		passcodeLabel.setLayoutData(gd);

		passcodeText = new Text(composite, SWT.BORDER|SWT.PASSWORD);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		passcodeText.setLayoutData(gd);
		passcodeText.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				validateInput();
			}
		});

		errorMessageText = new Text(composite, SWT.READ_ONLY | SWT.WRAP);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 2;
		errorMessageText.setLayoutData(gd);
		errorMessageText.setBackground(errorMessageText.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		setErrorMessage(errorMessage);
		
		applyDialogFont(composite);
		return composite;
	}

	/**
	 * Validates the input.
	 */
	protected void validateInput() {
		String errorMessage = null;
		if (passcodeText.getText().isEmpty()) {
			errorMessage = Messages.ConnectSsoServerDialog_Passcode_is_required;
		}
		setErrorMessage(errorMessage);
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
		if (errorMessageText != null && !errorMessageText.isDisposed()) {
			errorMessageText.setText(errorMessage == null ? " \n " : errorMessage); //$NON-NLS-1$
			boolean hasError = errorMessage != null && (StringConverter.removeWhiteSpaces(errorMessage)).length() > 0;
			errorMessageText.setEnabled(hasError);
			errorMessageText.setVisible(hasError);
			errorMessageText.getParent().update();
			Control button = getButton(IDialogConstants.OK_ID);
			if (button != null) {
				button.setEnabled(errorMessage == null);
			}
		}
	}

}
