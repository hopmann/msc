/**
 * Copyright (C) 2014 Holger Hopmann (h.hopmann@uni-muenster.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.hopmann.msc.commons.producer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;

import de.hopmann.msc.commons.qualifier.Configuration;

// TODO maybe use Commons Configuration
@ApplicationScoped
public class ConfigurationProducer {

	public static class LocalPreference<T> {

		private static Logger log = Logger.getLogger(LocalPreference.class
				.getName());

		private InjectionPoint injectionPoint;
		private Class<?> returnClass;

		public LocalPreference(InjectionPoint injectionPoint) {
			this.injectionPoint = injectionPoint;
			Field field = (Field) injectionPoint.getMember();
			Type genericType = field.getGenericType();
			if (genericType instanceof ParameterizedType) {
				returnClass = (Class<?>) ((ParameterizedType) genericType)
						.getActualTypeArguments()[0];
			} else {
				log.severe("Could not determine actual type of injected LocalPreference at "
						+ field);
			}
		}

		private Preferences getNode() {
			Class<?> declaringClass = injectionPoint.getMember()
					.getDeclaringClass();
			return Preferences.userNodeForPackage(declaringClass).node(
					declaringClass.getSimpleName());
		}

		private String getKey() {
			return injectionPoint.getMember().getName();
		}

		public void set(T value) {
			Preferences node = getNode();
			String key = getKey();
			if (value instanceof String) {
				node.put(key, (String) value);
			} else if (value instanceof Integer) {
				node.putInt(key, (Integer) value);
			} else if (value instanceof Serializable) {
				try {
					ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
					ObjectOutputStream objectOutputStream = new ObjectOutputStream(
							byteArrayOutputStream);
					objectOutputStream.writeObject(value);
					objectOutputStream.close();

					node.put(key, byteArrayOutputStream.toString());
				} catch (IOException e) {
					log.severe("Could not store local preference for key "
							+ key + ": " + e.getLocalizedMessage());
				}
			} else {
				log.severe("Could not store local preference for key " + key
						+ ": No suitable type");
				return;
			}

			try {
				node.flush();
			} catch (BackingStoreException e) {
				log.log(Level.SEVERE,
						"Error flushing local preference changes", e);
			}
		}

		public T get() {
			Preferences node = getNode();
			String key = getKey();
			if (String.class.isAssignableFrom(returnClass)) {
				return (T) node.get(key, null);
			} else if (Integer.class.isAssignableFrom(returnClass)) {
				return (T) new Integer(node.getInt(key, 0));
			} else if (Serializable.class.isAssignableFrom(returnClass)) {
				try {

					String value = node.get(key, "");

					ByteArrayInputStream byteArrayOutputStream = new ByteArrayInputStream(
							value.getBytes());
					ObjectInputStream objectOutputStream = new ObjectInputStream(
							byteArrayOutputStream);
					Object resultObject = objectOutputStream.readObject();
					objectOutputStream.close();

					return (T) resultObject;
				} catch (IOException | ClassNotFoundException e) {
					log.severe("Could not load local preference for key " + key
							+ ": " + e.getLocalizedMessage());
				}
			} else {
				log.severe("Could read local preference for key " + key
						+ ": No suitable type");
			}

			return null;
		}
	}

	private FilenameFilter propertiesFilenameFilter = new FilenameFilter() {

		@Override
		public boolean accept(File dir, String name) {
			return name.endsWith(".properties");
		}
	};
	private Properties propertiesCache = new Properties();

	ConfigurationProducer() {

	}

	@PostConstruct
	private void init() {
		ClassLoader classLoader = Thread.currentThread()
				.getContextClassLoader();
		try {
			Enumeration<URL> classesResources = classLoader.getResources("");

			while (classesResources.hasMoreElements()) {
				File resource = new File(classesResources.nextElement()
						.getPath());
				File[] resourceFiles = resource
						.listFiles(propertiesFilenameFilter);
				if (resourceFiles != null) {
					for (File propertiesFile : resourceFiles) {
						propertiesCache
								.load(new FileInputStream(propertiesFile));
					}
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(
					"Error while preparing properties cache", e);
		}
	}

	@Produces
	@Configuration
	public String produceConfigurationString(InjectionPoint injectionPoint) {

		// Configuration annotation is specified by definition
		String key = injectionPoint.getAnnotated()
				.getAnnotation(Configuration.class).value();
		boolean required = injectionPoint.getAnnotated()
				.getAnnotation(Configuration.class).required();

		String value = propertiesCache.getProperty(key);

		if (value == null) {
			// Handle unresolved configuration keys
			if (required) {
				throw new RuntimeException("Configuration with key " + key
						+ " was not found but set as required");
			} else {
				return null;
			}
		} else {
			// Configuration key found, return value
			return value;
		}
	}

	@Produces
	@Configuration
	public String[] produceConfigurationStringArray(
			InjectionPoint injectionPoint) {
		String configurationString = produceConfigurationString(injectionPoint);
		return configurationString.split(",\\s*");
	}

	@Produces
	@Configuration
	public Integer produceConfigurationInt(InjectionPoint injectionPoint) {
		String configurationString = produceConfigurationString(injectionPoint);

		if (configurationString != null) {
			return Integer.parseInt(configurationString);
		} else {
			return null;
		}
	}

	@Produces
	@Configuration
	public Preferences produceConfigurationPreferences(
			InjectionPoint injectionPoint) {
		return Preferences.userRoot().node(
				injectionPoint.getMember().getDeclaringClass().getName());
	}

	@Produces
	@Configuration
	public <T> LocalPreference<T> produceLocalConfigurationPreferences(
			InjectionPoint injectionPoint) {
		LocalPreference<T> localPreference = new LocalPreference<T>(
				injectionPoint);
		return localPreference;

	}

}
