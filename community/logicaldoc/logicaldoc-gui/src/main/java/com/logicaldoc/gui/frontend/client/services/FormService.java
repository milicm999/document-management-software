package com.logicaldoc.gui.frontend.client.services;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.logicaldoc.gui.common.client.LDRpcRequestBuilder;
import com.logicaldoc.gui.common.client.ServerException;
import com.logicaldoc.gui.common.client.beans.GUIEmail;
import com.logicaldoc.gui.common.client.beans.GUIForm;

/**
 * The client side stub for the Form Service. This service gives all needed
 * methods to handle the forms.
 */
@RemoteServiceRelativePath("form")
public interface FormService extends RemoteService {
	/**
	 * Retrieves a specific form by its ID
	 * 
	 * @param id identifier of the forms
	 * 
	 * @return the form retrieved by the server application
	 * 
	 * @throws ServerException an error happened in the server application
	 */
	public GUIForm getById(long id) throws ServerException;

	/**
	 * Saves / Creates a form
	 * 
	 * @param form the form to store
	 * 
	 * @return the stored form
	 * 
	 * @throws ServerException an error happened in the server application
	 */
	public GUIForm save(GUIForm form) throws ServerException;

	/**
	 * Processes an uploaded image for being used as form header
	 * 
	 * @return the Bass64 representation of the uploaded image
	 * 
	 * @throws ServerException an error happened in the server application
	 */
	public String processImage() throws ServerException;

	/**
	 * Deletes a form
	 * 
	 * @param id identifier of the form to delete
	 * 
	 * @throws ServerException an error happened in the server application
	 */
	public void delete(long id) throws ServerException;

	/**
	 * Invites by email a set of recipients to fill out the given form
	 * 
	 * @param formId identifier of the form
	 * @param email the email to send
	 * @param locale the current locale
	 * 
	 * @throws ServerException an error happened in the server application
	 */
	void invite(long formId, GUIEmail email, String locale) throws ServerException;

	public static class Instance {
		private static FormServiceAsync instance;

		public static FormServiceAsync get() {
			if (instance == null) {
				instance = GWT.create(FormService.class);
				((ServiceDefTarget) instance).setRpcRequestBuilder(new LDRpcRequestBuilder());
			}
			return instance;
		}
	}
}