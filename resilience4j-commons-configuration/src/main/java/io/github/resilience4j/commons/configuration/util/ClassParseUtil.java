/*
 *   Copyright 2023: Deepak Kumar
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package io.github.resilience4j.commons.configuration.util;

import io.github.resilience4j.commons.configuration.exception.ConfigParseException;

import java.lang.reflect.Array;
import java.util.List;
import java.util.stream.Collectors;

public class ClassParseUtil {
    /***
     * Utility to convert List of String representation of classes into Array of Classes.
     * Validates if string representation of classes are assignable to the target type.
     * @param classNames - List of String representation of classes
     * @param targetClassType - Target Class type
     * @return - Array of converted Class type
     */
    public static <T> Class<? extends T>[] convertStringListToClassTypeArray(List<String> classNames, Class<? extends T> targetClassType) {
        Class<? extends T>[] array = (Class<? extends T>[]) Array.newInstance(Class.class, classNames.size());

        return classNames.stream()
                .map(className -> (Class<? extends T>) convertStringToClassType(className, targetClassType))
                .collect(Collectors.toList())
                .toArray(array);
    }

    /***
     * Utility to convert a String representation of class into the Target Class type.
     * Validates if string representation of class is assignable to the target type.
     * @param className - String representation of class
     * @param targetClassType - Target Class type
     * @return - Converted Class type
     */
    public static <T> Class<? extends T> convertStringToClassType(String className, Class<? extends T> targetClassType) {
        try {
            Class<?> clazz = Class.forName(className);
            if (!targetClassType.isAssignableFrom(clazz)){
                throw new ConfigParseException("Class " + className + " is not a subclass of " + targetClassType.getName());
            }
            return (Class<? extends T>) clazz;
        } catch (ClassNotFoundException e) {
            throw new ConfigParseException("Class not found: " + className, e);
        }
    }
}
