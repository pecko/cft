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
package org.eclipse.cft.server.ui.internal.editor;

import org.eclipse.cft.server.core.internal.CloudFoundryBrandingExtensionPoint;
import org.eclipse.cft.server.core.internal.CloudFoundryConstants;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudServerEvent;
import org.eclipse.cft.server.core.internal.CloudServerListener;
import org.eclipse.cft.server.core.internal.ServerEventHandler;
import org.eclipse.cft.server.core.internal.client.CloudFoundryClientFactory;
import org.eclipse.cft.server.ui.internal.CloudFoundryServerUiPlugin;
import org.eclipse.cft.server.ui.internal.CloudFoundryURLNavigation;
import org.eclipse.cft.server.ui.internal.CloudUiUtil;
import org.eclipse.cft.server.ui.internal.Messages;
import org.eclipse.cft.server.ui.internal.actions.CloudFoundryServerCommand;
import org.eclipse.cft.server.ui.internal.actions.UpdatePasswordOperation;
import org.eclipse.cft.server.ui.internal.wizards.OrgsAndSpacesWizard;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.PageBook;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;

/**
 * @author Andy Clement
 * @author Christian Dupuis
 * @author Leo Dos Santos
 * @author Steffen Pingel
 * @author Terry Denney
 */
public class CloudFoundryAccountSection extends ServerEditorSection implements CloudServerListener {

	private CloudFoundryServer cfServer;

	private Text emailText;

	private Text passwordText;

	private String sectionTitle;

	private Text urlText;

	private Text orgText;

	private Text spaceText;

	private Text validateLabel;

	private Text passcodeText;

	private Button sso;

	private Label emailLabel;

	private Label passwordLabel;

	private Label passcodeLabel;

	private Hyperlink prompt;

	private Button validateButton;

	private Button changePasswordButton;

	private Control emailPasswordPage;

	private Control passcodePage;

	private PageBook pageBook;
	
	protected boolean updating;

	// private CloudUrlWidget urlWidget;

	// private Combo urlCombo;

	public CloudFoundryAccountSection() {
	}

	public void update() {
		if (cfServer.getUsername() != null && emailText != null && !cfServer.getUsername().equals(emailText.getText())) {
			emailText.setText(cfServer.getUsername());
		}
		if (sso != null && !(sso.getSelection() == cfServer.isSso()) ) {
			cfServer.setSso(sso.getSelection());
		}
		if (cfServer.getPassword() != null && passwordText != null
				&& !cfServer.getPassword().equals(passwordText.getText())) {
			passwordText.setText(cfServer.getPassword());
		}
		if (cfServer.getUrl() != null
				&& urlText != null
				&& !CloudUiUtil.getDisplayTextFromUrl(cfServer.getUrl(), cfServer.getServer().getServerType().getId())
						.equals(urlText.getText())) {
			urlText.setText(CloudUiUtil.getDisplayTextFromUrl(cfServer.getUrl(), cfServer.getServer().getServerType()
					.getId()));
		}
		if (cfServer.hasCloudSpace()) {
			if (cfServer.getCloudFoundrySpace() != null && cfServer.getCloudFoundrySpace().getOrgName() != null
					&& orgText != null && !cfServer.getCloudFoundrySpace().getOrgName().equals(orgText.getText())) {
				orgText.setText(cfServer.getCloudFoundrySpace().getOrgName());
			}
			if (cfServer.getCloudFoundrySpace() != null && cfServer.getCloudFoundrySpace().getSpaceName() != null
					&& spaceText != null && !cfServer.getCloudFoundrySpace().getSpaceName().equals(spaceText.getText())) {
				spaceText.setText(cfServer.getCloudFoundrySpace().getSpaceName());
			}
		}

	}

	@Override
	public void createSection(Composite parent) {
		super.createSection(parent);

		FormToolkit toolkit = getFormToolkit(parent.getDisplay());

		Section section = toolkit.createSection(parent, ExpandableComposite.TWISTIE | ExpandableComposite.TITLE_BAR);
		section.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		section.setText(sectionTitle);

		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);

		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite topComposite = new Composite(composite, SWT.NONE);
		topComposite.setLayout(new GridLayout(2, false));
		topComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		sso = new Button(topComposite, SWT.CHECK);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 2;
		sso.setLayoutData(gd);
		sso.setText("SSO server?");
		sso.setSelection(cfServer.isSso());
		
		pageBook = new PageBook(topComposite, SWT.NONE);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 2;
		pageBook.setLayoutData(gd);
				
		emailPasswordPage = createEmailPasswordControl(toolkit, pageBook);

		passcodePage = createPasscodeComposite(toolkit, pageBook);
		
		Label label = toolkit.createLabel(topComposite, Messages.CloudFoundryAccountSection_LABEL_URL);
		label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		urlText = toolkit.createText(topComposite, "", SWT.NONE); //$NON-NLS-1$
		urlText.setEditable(false);
		urlText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		urlText.setData(FormToolkit.KEY_DRAW_BORDER, Boolean.FALSE);
		if (cfServer.getUrl() != null) {
			urlText.setText(CloudUiUtil.getDisplayTextFromUrl(cfServer.getUrl(), cfServer.getServer().getServerType()
					.getId()));
		}
		
		Label orgLabel = toolkit.createLabel(topComposite, Messages.CloudFoundryAccountSection_LABEL_ORG, SWT.NONE);
		orgLabel.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
		orgLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		orgText = toolkit.createText(topComposite, "", SWT.NONE); //$NON-NLS-1$
		orgText.setEditable(false);
		orgText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		orgText.setData(FormToolkit.KEY_DRAW_BORDER, Boolean.FALSE);
		if (cfServer.getCloudFoundrySpace() != null && cfServer.getCloudFoundrySpace().getOrgName() != null) {
			orgText.setText(cfServer.getCloudFoundrySpace().getOrgName());
		}

		Label spaceLabel = toolkit.createLabel(topComposite, Messages.CloudFoundryAccountSection_LABEL_SPACE, SWT.NONE);
		spaceLabel.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
		spaceLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		spaceText = toolkit.createText(topComposite, "", SWT.NONE); //$NON-NLS-1$
		spaceText.setEditable(false);
		spaceText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		spaceText.setData(FormToolkit.KEY_DRAW_BORDER, Boolean.FALSE);
		if (cfServer.getCloudFoundrySpace() != null && cfServer.getCloudFoundrySpace().getSpaceName() != null) {
			spaceText.setText(cfServer.getCloudFoundrySpace().getSpaceName());
		}

		final Composite buttonComposite = toolkit.createComposite(composite);

		buttonComposite.setLayout(new GridLayout(4, false));
		GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).grab(true, false).applyTo(buttonComposite);

		final Composite validateComposite = toolkit.createComposite(composite);
		validateComposite.setLayout(new GridLayout(1, false));
		validateComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		// Temporary switch for the border style to "no border", so this is drawn properly
		int borderStyleBackup = toolkit.getBorderStyle();
		try {
			toolkit.setBorderStyle(SWT.NULL);
			validateLabel = toolkit.createText(validateComposite, "", SWT.READ_ONLY); //$NON-NLS-1$
		} finally {
			// Make sure under every circumstance, the previous border is restored
			toolkit.setBorderStyle(borderStyleBackup);
		}
		
		validateLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		// This is to avoid the cursor stopping at an empty label when navigating using keyboard
		validateLabel.setVisible(false);

		createCloneServerArea(buttonComposite, toolkit);

		changePasswordButton = toolkit.createButton(buttonComposite,
				Messages.CloudFoundryAccountSection_BUTTON_CHANGE_PW, SWT.PUSH);

		changePasswordButton.setEnabled(true);

		changePasswordButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		changePasswordButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (server.isDirty()) {
					boolean confirm = MessageDialog.openQuestion(getShell(),
							Messages.CloudFoundryAccountSection_DIALOG_UNSAVE_TITLE,
							Messages.CloudFoundryAccountSection_DIALOG_UNSAVE_BODY);
					if (!confirm) {
						return;
					}
				}

				Job job = new Job(Messages.UpdatePasswordCommand_TEXT_PW_UPDATE) {
					protected IStatus run(IProgressMonitor monitor) {

						try {
							new UpdatePasswordOperation(cfServer).run(monitor);
						}
						catch (CoreException e) {
							CloudFoundryPlugin.logError(e);
						}

						return Status.OK_STATUS;
					}
				};
				job.schedule();

			}
		});

		validateButton = toolkit.createButton(buttonComposite,
				Messages.CloudFoundryAccountSection_BUTTON_VALIDATE_ACCOUNT, SWT.PUSH);
		validateButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		validateButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				final String url = cfServer.getUrl();
				final String userName = emailText.getText();
				final String password = passwordText.getText();
				final String org = orgText.getText();
				final String space = spaceText.getText();
				try {
					CloudUiUtil.validateCredentials(userName, password, url, false,
							cfServer.getSelfSignedCertificate(), null);

					if (org != null && space != null) {
						validateLabel.setForeground(validateLabel.getDisplay().getSystemColor(SWT.COLOR_BLUE));
						validateLabel.setText(Messages.VALID_ACCOUNT);
					}
					else {
						String errorMsg = null;
						if (org == null) {
							errorMsg = Messages.ERROR_INVALID_ORG;
						}
						else if (space == null) {
							errorMsg = Messages.ERROR_INVALID_SPACE;
						}

						if (errorMsg != null) {
							validateLabel.setForeground(validateLabel.getDisplay().getSystemColor(SWT.COLOR_RED));
							validateLabel.setText(errorMsg);
						}
						else {
							validateLabel.setForeground(validateLabel.getDisplay().getSystemColor(SWT.COLOR_BLACK));
							validateLabel.setText("");
						}
					}

				}
				catch (CoreException e) {
					validateLabel.setForeground(validateLabel.getDisplay().getSystemColor(SWT.COLOR_RED));
					validateLabel.setText(e.getMessage());
				}
				
				// If validate label is not empty, then make it visible so cursor can visit the control (and be read)
				validateLabel.setVisible(validateLabel.getText().length() > 0);
				
				buttonComposite.layout(new Control[] { validateButton });
				validateComposite.layout(new Control[] { validateLabel });
			}
		});

		// Create signup button only if the server is not local or micro
		if (CloudFoundryURLNavigation.canEnableCloudFoundryNavigation(cfServer)) {
			Button cfSignup = toolkit.createButton(buttonComposite,
					CloudFoundryConstants.PUBLIC_CF_SERVER_SIGNUP_LABEL, SWT.PUSH);
			cfSignup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			cfSignup.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent event) {
					IServer iServer = cfServer.getServer();
					if (iServer != null) {
						String signupURL = CloudFoundryBrandingExtensionPoint.getSignupURL(cfServer.getServerId(),
								cfServer.getUrl());
						if (signupURL != null) {
							CloudFoundryURLNavigation nav = new CloudFoundryURLNavigation(signupURL);
							nav.navigate();
						}
					}
				}
			});
		}

		sso.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (updating)
					return;
				updating = true;
				CloudFoundryServerCommand command = new CloudFoundryServerCommand(cfServer, "Setting sso") {

					@Override
					public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
						server.setSso(sso.getSelection());
						return super.execute(monitor, info);
					}
					
				};
				execute(command);
				updating = false;
				enableSso(sso.getSelection());
			}
			
		});

		enableSso(sso.getSelection());
		toolkit.paintBordersFor(topComposite);
		section.setExpanded(true);

		ServerEventHandler.getDefault().addServerListener(this);
	}

	private Control createPasscodeComposite(FormToolkit toolkit, Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		GridData gd;
		prompt = toolkit.createHyperlink(composite, "", SWT.LEFT | SWT.WRAP);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 2;
		prompt.setLayoutData(gd);
		String ssoUrl = "";
		String href = "";
		if (cfServer.getUrl() != null && !cfServer.getUrl().isEmpty()) {
			try {
				href = CloudFoundryClientFactory.getSsoUrl(cfServer.getUrl());
				ssoUrl = "Passcode: Get one at " + href;
			}
			catch (Exception e1) {
				CloudFoundryServerUiPlugin.logWarning(e1);
			}
		}
		prompt.setText(ssoUrl);
		prompt.setHref(href);
		prompt.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				CloudUiUtil.openUrl(prompt.getHref().toString());
			}
		});
		passcodeLabel = toolkit.createLabel(composite, Messages.CloudFoundryAccountSection_LABEL_PASSCODE, SWT.NONE);
		passcodeLabel.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
		passcodeLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		passcodeText = toolkit.createText(composite, "", SWT.BORDER|SWT.PASSWORD); //$NON-NLS-1$
		passcodeText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		passcodeText.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		passcodeText.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				if (updating)
					return;
				updating = true;
				CloudFoundryServerCommand command = new CloudFoundryServerCommand(cfServer, "Setting sso") {

					@Override
					public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
						server.setPasscode(passcodeText.getText());
						return super.execute(monitor, info);
					}
					
				};
				execute(command);
				updating = false;
			}
		});
		return composite;
	}

	private Control createEmailPasswordControl(FormToolkit toolkit, Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		emailLabel = toolkit.createLabel(composite, Messages.CloudFoundryAccountSection_LABEL_EMAIL, SWT.NONE);
		emailLabel.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
		emailLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		emailText = toolkit.createText(composite, "", SWT.BORDER); //$NON-NLS-1$

		emailText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		emailText.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		if (cfServer.getUsername() != null) {
			emailText.setText(cfServer.getUsername());
		}

		// Changing username is not currently supported through the editor.
		emailText.setEditable(false);
		// emailText.addModifyListener(new DataChangeListener(DataType.EMAIL));

		passwordLabel = toolkit.createLabel(composite, Messages.CloudFoundryAccountSection_LABEL_PASSWORD, SWT.NONE);
		passwordLabel.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
		passwordLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		passwordText = toolkit.createText(composite, "", SWT.PASSWORD|SWT.BORDER); //$NON-NLS-1$
		passwordText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		passwordText.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		if (cfServer.getPassword() != null) {
			passwordText.setText(cfServer.getPassword());
		}

		// Setting password through text control is disabled. Passwords are
		// instead set through a separate update password dialogue
		passwordText.setEditable(false);
		// passwordText.addModifyListener(new
		// DataChangeListener(DataType.PASSWORD));
		
		return composite;
	}

	protected void createCloneServerArea(Composite parent, FormToolkit toolkit) {
		final Button changeSpaceButton = toolkit.createButton(parent,
				Messages.CloudFoundryAccountSection_BUTTON_CLONE_SERVER, SWT.PUSH);

		changeSpaceButton.setEnabled(true);
		passwordText.setEditable(false);

		changeSpaceButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		changeSpaceButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				OrgsAndSpacesWizard wizard = new OrgsAndSpacesWizard(cfServer);

				WizardDialog dialog = new WizardDialog(getShell(), wizard);
				dialog.open();

			}
		});
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) {
		super.init(site, input);
		// String serviceName = null;
		if (server != null) {
			cfServer = (CloudFoundryServer) server.loadAdapter(CloudFoundryServer.class, null);
			update();
			// serviceName =
			// CloudFoundryBrandingExtensionPoint.getServiceName(server.getServerType().getId());
		}
		// if (serviceName == null) {
		sectionTitle = Messages.COMMONTXT_ACCOUNT_INFO;

		// }
		// else {
		// sectionTitle = serviceName + " Account";
		// }
	}

	// NOTE:The data change listener is execute as a WTP command
	// which sets the editor in a dirty state. Editor saves are required if
	// using the Data change listener,
	// otherwise a warning is shown to the user indicating that the underlying
	// server file has changed.
	protected class DataChangeListener implements ModifyListener {

		// private String newValue;
		//
		// private String oldValue;

		private final DataType type;

		private DataChangeListener(DataType type) {
			this.type = type;
		}

		protected void update(String value) {
			switch (type) {
			case EMAIL:
				updateTextField(value, emailText);
				break;
			case PASSWORD:
				updateTextField(value, passwordText);
				break;
			}
		}

		private void updateTextField(String input, Text text) {
			if (text != null && !text.isDisposed() && !text.getText().equals(input)) {
				text.setText(input == null ? "" : input); //$NON-NLS-1$
			}
		}

		public void modifyText(ModifyEvent e) {
			// switch (type) {
			// case EMAIL:
			// oldValue = cfServer.getUsername();
			// newValue = emailText.getText();
			// break;
			// case PASSWORD:
			// oldValue = cfServer.getPassword();passcodeText.setEditable(false);

			// newValue = passwordText.getText();
			// break;
			//
			// }

			// Commenting out as it executing this command via WTP sets the
			// editor
			// to dirty state.
			// Setting password automatically saves the server, so its not
			// necessary to set the editor to dirty, unless
			// passwords are set directly in the text controls (not currently
			// supported as of CF 1.8.1
			//			execute(new AbstractOperation("CloudFoundryServerUpdate") { //$NON-NLS-1$
			//
			// @Override
			// public IStatus execute(IProgressMonitor monitor, IAdaptable info)
			// throws ExecutionException {
			// update(newValue);
			// return Status.OK_STATUS;
			// }
			//
			// @Override
			// public IStatus redo(IProgressMonitor monitor, IAdaptable info)
			// throws ExecutionException {
			// update(newValue);
			// return Status.OK_STATUS;
			// }
			//
			// @Override
			// public IStatus undo(IProgressMonitor monitor, IAdaptable info)
			// throws ExecutionException {
			// update(oldValue);
			// return Status.OK_STATUS;
			// }
			// });
		}
	}

	private enum DataType {
		EMAIL, PASSWORD
	}

	@Override
	public void serverChanged(CloudServerEvent event) {
		if (event.getType() == CloudServerEvent.EVENT_UPDATE_PASSWORD && event.getServer() != null && cfServer != null
				&& cfServer.getServerId().equals(event.getServer().getServerId())) {
			final CloudServerEvent serverEvent = event;
			Display.getDefault().asyncExec(new Runnable() {

				public void run() {
					final CloudFoundryServer eventServer = serverEvent.getServer();

					if (passwordText == null || passwordText.isDisposed() || validateLabel == null
							|| validateLabel.isDisposed()) {
						return;
					}
					String password = eventServer.getPassword();
					if (password != null && !passwordText.getText().equals(password)) {
						passwordText.setText(password);
					}
					String errorMessage = serverEvent.getStatus() != null
							&& serverEvent.getStatus().getSeverity() == IStatus.ERROR ? serverEvent.getStatus()
							.getMessage() : null;

					if (errorMessage != null) {
						validateLabel.setForeground(validateLabel.getDisplay().getSystemColor(SWT.COLOR_RED));
						validateLabel.setText(errorMessage);
					}
					else {
						validateLabel.setForeground(validateLabel.getDisplay().getSystemColor(SWT.COLOR_BLUE));
						validateLabel.setText(Messages.CloudFoundryAccountSection_LABEL_PW_CHANGED);
					}
					validateLabel.getParent().layout(new Control[] { validateLabel });
				}
			});
		}
	}

	@Override
	public void dispose() {
		ServerEventHandler.getDefault().removeServerListener(this);
	}

	private void enableSso(boolean isSso) {
		if (isSso) {
			pageBook.showPage(passcodePage);
		} else {
			pageBook.showPage(emailPasswordPage);
			cfServer.setPasscode(null);
			passcodeText.setText("");
		}
		pageBook.getParent().layout(true, true);
		validateButton.setEnabled(!isSso);
		validateLabel.setVisible(!isSso );
		changePasswordButton.setEnabled(!isSso && validateLabel.getText().length() > 0);
		try {
			cfServer.getBehaviour().disconnect(new NullProgressMonitor());
		}
		catch (CoreException e) {
			CloudFoundryServerUiPlugin.logError(e);
		}
	}
}
