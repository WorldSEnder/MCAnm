package com.github.worldsender.mcanm.common.util;

/**
 * http://stackoverflow.com/a/35083181/3102935
 * 
 * @author WorldSEnder
 *
 */
public class CallResolver extends SecurityManager {
	public static final CallResolver INSTANCE = new CallResolver();

	public Class<?>[] getCallingClasses() {
		return getClassContext();
	}

	public Class<?> getCallingClass() {
		return getCallingClasses()[3];
	}
}
