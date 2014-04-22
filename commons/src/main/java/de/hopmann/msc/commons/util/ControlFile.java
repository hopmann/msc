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
package de.hopmann.msc.commons.util;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Class to read a Debian control file as used in R metadata descriptions based
 * on annotations and customizable class prototypes.
 * 
 * @param <T>
 */
public class ControlFile<T> implements Closeable {

	@Retention(RUNTIME)
	@Target({ FIELD, ANNOTATION_TYPE })
	public @interface ControlField {

		String value() default "";
	}

	@Retention(RUNTIME)
	@Target({ FIELD })
	public @interface ControlFields {

		ControlField[] value();
	}

	private static class FieldHolder<T> {
		private Field field;
		private Method producer;
		private Constructor<?> constructor;
		private Class<?> type;
		private boolean isArray;
		private Logger log = Logger
				.getLogger(FieldHolder.class.getSimpleName());

		public FieldHolder(Field field) {
			field.setAccessible(true);
			this.field = field;

			this.type = field.getType();
			this.isArray = this.type.isArray();
			if (isArray) {
				// Use array component type as type info if the field represents
				// an array
				this.type = type.getComponentType();
			}

			if (!String.class.isAssignableFrom(type)) {
				// Field not of type String -> find other ways to create
				// instance from a String
				try {
					constructor = type.getConstructor(String.class);
					constructor.setAccessible(true);
				} catch (NoSuchMethodException e) {
					try {
						producer = type.getDeclaredMethod("valueOf",
								String.class);
						producer.setAccessible(true);
					} catch (NoSuchMethodException e1) {
						throw new RuntimeException(
								"No valid constructor or 'valueOf' method", e1);
						// TODO warning no String constructor and no static
						// fromString method
					}
				}
			}
		}

		private void setValue(T record, String value)
				throws IllegalArgumentException {
			try {
				if (isArray) {
					// Array type
					String[] valueList = value.split(",");
					Object[] typeValues = (Object[]) Array.newInstance(type,
							valueList.length);

					for (int i = 0; i < valueList.length; i++) {
						typeValues[i] = getTypeValue(valueList[i].trim());
					}
					field.set(record, typeValues);
				} else {
					// no array
					field.set(record, getTypeValue(value));
				}
			} catch (IllegalAccessException e) {
				throw new IllegalArgumentException(e);
			} catch (IllegalArgumentException e) {
				log.warning(e.getLocalizedMessage());
			}
		}

		private Object getTypeValue(String value)
				throws IllegalArgumentException {
			try {
				Object resultValue = null;
				if (producer != null) {
					resultValue = producer.invoke(null, value);
				} else if (constructor != null) {
					resultValue = constructor.newInstance(value);
				} else {
					resultValue = value;
				}
				return resultValue;
			} catch (Exception e) {
				throw new IllegalArgumentException(
						"Could not construct object value from String", e);
			}
		}

	}

	private BufferedReader reader;
	private Constructor<T> recordConstructor;

	private Map<String, FieldHolder<T>> recordFieldsMap = new HashMap<String, FieldHolder<T>>();

	@Override
	public void close() throws IOException {
		if (reader != null) {
			reader.close();
		}

	}

	public ControlFile(Class<T> recordClass, Reader reader) {
		try {
			this.recordConstructor = recordClass.getConstructor();
			this.recordConstructor.setAccessible(true);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("No valid constructor defined", e);
		}

		this.reader = new BufferedReader(reader);
		// TODO Check that there is a proper constructor
		// TODO check that this is no non-static inner class

		Class<?> superClass = recordClass;
		while (superClass != null) {
			// For each superclass of recordClass and recordClass itself
			for (Field field : superClass.getDeclaredFields()) {
				// For all declared fields, i.e. not only public ones
				ControlField[] controlFieldAnnotations;

				ControlFields controlFieldsAnnotation = field
						.getAnnotation(ControlFields.class);
				if (controlFieldsAnnotation != null) {
					controlFieldAnnotations = controlFieldsAnnotation.value();
				} else {
					controlFieldAnnotations = new ControlField[] { field
							.getAnnotation(ControlField.class) };
				}

				for (ControlField controlFieldAnnotation : controlFieldAnnotations) {
					if (controlFieldAnnotation != null) {
						String name = controlFieldAnnotation.value();
						if (name == null || name.isEmpty()) {
							name = field.getName();
						}

						FieldHolder<T> fieldHolder = new FieldHolder<T>(field);
						recordFieldsMap.put(name, fieldHolder);
					}
				}
			}

			superClass = superClass.getSuperclass();
		}
	}

	private void storeTag(T record, String tag, String value)
			throws IllegalArgumentException, IllegalAccessException {
		FieldHolder<T> field = recordFieldsMap.get(tag);
		if (field != null && value != null) {
			field.setValue(record, value.trim());
		}
	}

	public T readRecord() throws IOException {
		T record = null;

		String value = null;
		String tag = null;
		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				// For every line

				if (line.isEmpty() || line.startsWith(" ")) {
					// Empty line or continuation line

					if (line.trim().isEmpty()) {
						// Empty line, finishes record or space between records

						if (record != null) {
							// Line finishes previous record

							if (tag != null && value != null) {
								// Store previous tag
								storeTag(record, tag, value);
							}
							return record;
						}
					} else {
						// Continuation line
						value += line.substring(1);
					}
				} else {
					int tagSepIndex = line.indexOf(":");
					if (tagSepIndex != -1) {
						// New tag

						if (record == null) {
							// Create record if needed
							record = recordConstructor.newInstance();
						}

						if (tag != null && value != null) {
							// Store previous tag
							storeTag(record, tag, value);
						}

						tag = line.substring(0, tagSepIndex);
						if (tagSepIndex + 1 < line.length()) {
							value = line.substring(tagSepIndex + 1);
						} else {
							value = "";
						}
					}
				}
			}
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}

		// TODO error handling

		// Returns current record after EOL or null if no record left
		return record;
	}
}
