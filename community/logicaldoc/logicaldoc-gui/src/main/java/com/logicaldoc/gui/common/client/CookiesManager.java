package com.logicaldoc.gui.common.client;

import com.google.gwt.user.client.Cookies;
import com.logicaldoc.gui.common.client.beans.GUIInfo;
import com.smartgwt.client.util.Offline;

/**
 * Here we handle the storage of informations in the broeserk
 * 
 * @author Marco Meschieri - LogicalDOC
 * @since 7.7.2
 */
public class CookiesManager {

	public static final String COOKIE_HITSLIST_MODE = "ldoc-hitslist-mode";

	public static final String COOKIE_HITSLIST_PREV_W = "ldoc-hitslist-prev-w";

	public static final String COOKIE_DOCSLIST_MODE = "ldoc-docslist-mode";

	public static final String COOKIE_DOCSLIST_PREV_W = "ldoc-docslist-prev-w";

	public static final String COOKIE_DOCSMENU_W = "ldoc-docsmenu-w";

	public static final String COOKIE_SAVELOGIN = "ldoc-savelogin";

	public static final String COOKIE_DENSITY = "ldoc-density";

	public static final String COOKIE_USER = "ldoc-user";

	public static final String COOKIE_PASSWORD = "ldoc-password";

	public static final String COOKIE_VERSION = "ldoc-version";

	public static final String COOKIE_SID = "ldoc-sid";

	public static final String COOKIE_DEVICE = "ldoc-device";

	public static final String COOKIE_JSESSIONID = "JSESSIONID";

	/**
	 * Removes the cookies that store the session ID
	 */
	public static void removeSid() {
		try {
			Offline.remove(COOKIE_SID);
			Cookies.removeCookie(COOKIE_SID);
			Cookies.removeCookie(COOKIE_JSESSIONID);
		} catch (Throwable t) {

		}
	}

	/**
	 * Removes the cookies used to handle the login
	 */
	public static void removeLogin() {
		try {
			removeSid();
		} catch (Throwable t) {

		}
	}

	/**
	 * Removes all the informations stored in the browser
	 */
	public static void removeAllCookies() {
		try {
			Offline.remove(COOKIE_DENSITY);
		} catch (Throwable t) {

		}

		try {
			Offline.remove(COOKIE_HITSLIST_PREV_W);
		} catch (Throwable t) {

		}

		try {
			Offline.remove(COOKIE_DOCSLIST_PREV_W);
		} catch (Throwable t) {

		}

		try {
			Offline.remove(COOKIE_DOCSMENU_W);
		} catch (Throwable t) {

		}

		try {
			Offline.remove(COOKIE_PASSWORD);
		} catch (Throwable t) {

		}
		try {
			Offline.remove(COOKIE_SAVELOGIN);
		} catch (Throwable t) {

		}
		try {
			Offline.remove(COOKIE_USER);
		} catch (Throwable t) {

		}

		try {
			Offline.remove(COOKIE_DEVICE);
			Cookies.removeCookie(COOKIE_DEVICE);
		} catch (Throwable t) {

		}

		try {
			removeLogin();
		} catch (Throwable t) {

		}
	}

	/**
	 * Retrieves the device ID saved in the browser
	 * 
	 * @return the saved device ID
	 */
	public static String getSavedDevice() {
		return get(COOKIE_DEVICE);
	}

	/**
	 * Gets a cookie
	 * 
	 * @param cookieName name of the cookie
	 * 
	 * @return value of the cookie
	 */
	public static String get(String cookieName) {
		return (String) Offline.get(cookieName);
	}

	/**
	 * Saves the cookie
	 * 
	 * @param cookieName name of the cookie
	 * @param value value of the cookie
	 */
	public static void save(String cookieName, String value) {
		Offline.put(cookieName, value);
	}

	public static void save(String cookieName, int value) {
		Offline.put(cookieName, value);
	}

	/**
	 * Stores the release information in the browser
	 * 
	 * @param info the User Interface informations
	 */
	public static void saveRelease(GUIInfo info) {
		Cookies.setCookie(COOKIE_VERSION, info.getRelease(), null, null, null, false);
	}

	public static void saveDevice(String deviceId) {
		Cookies.setCookie(COOKIE_DEVICE, deviceId, null, null, null, false);
		Offline.put(COOKIE_DEVICE, deviceId);
	}

	public static boolean isSaveLogin() {
		return "true".equals(Offline.get(COOKIE_SAVELOGIN));
	}

	/**
	 * Retrieves the credentials
	 * 
	 * @return username and password
	 */
	public static String[] getSavedCredentials() {
		try {
			return new String[] { Offline.get(COOKIE_USER).toString(), Offline.get(COOKIE_PASSWORD).toString() };
		} catch (Throwable t) {
			return new String[] { null, null };
		}
	}

	/**
	 * Saves the login informations
	 * 
	 * @param saveLoginEnabled if the 'save login' checkbox must be enabled
	 * @param rememberMe if the 'remember me' option must be enabled
	 * @param username the username
	 * @param password the password
	 */
	public static void saveLogin(boolean saveLoginEnabled, boolean rememberMe, String username, String password) {
		if (saveLoginEnabled) {
			save(COOKIE_SAVELOGIN, Boolean.toString(rememberMe));
			save(CookiesManager.COOKIE_USER, rememberMe ? username : "");
			save(CookiesManager.COOKIE_PASSWORD, rememberMe ? password : "");
		} else {
			CookiesManager.save(CookiesManager.COOKIE_SAVELOGIN, "false");
			CookiesManager.save(CookiesManager.COOKIE_USER, "");
			CookiesManager.save(CookiesManager.COOKIE_PASSWORD, "");
		}
	}
}