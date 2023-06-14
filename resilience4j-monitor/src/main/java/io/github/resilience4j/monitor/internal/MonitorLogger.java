/*
 *
 *  Copyright 2023 Mariusz Kopylec
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.monitor.internal;

import io.github.resilience4j.monitor.LogLevel;
import io.github.resilience4j.monitor.LogMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MonitorLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitorLogger.class);

    private final LogMode logMode;
    private final LogLevel logLevel;

    MonitorLogger(LogMode logMode, LogLevel logLevel) {
        this.logMode = logMode;
        this.logLevel = logLevel;
    }

    void logStart(String operationName, Object input) {
        switch (logMode) {
            case DISABLED, SINGLE -> {}
            case SEPARATE -> log("OPERATION: {}\nINPUT: {}", operationName, input);
        }
    }

    void logSuccess(String operationName, Object input, String resultName, Object output) {
        switch (logMode) {
            case DISABLED -> {}
            case SINGLE -> log("OPERATION: {}\nINPUT: {}\nRESULT: Success | {}\nOUTPUT: {}", operationName, input, resultName, output);
            case SEPARATE -> log("OPERATION: {}\nRESULT: Success | {}\nOUTPUT: {}", operationName, resultName, output);
        }
    }

    void logFailure(String operationName, Object input, String resultName, Throwable throwable) {
        switch (logMode) {
            case DISABLED -> {}
            case SINGLE -> log("OPERATION: {}\nINPUT: {}\nRESULT: Failure | {}\nTHROWN: {}", operationName, input, resultName, throwable);
            case SEPARATE -> log("OPERATION: {}\nRESULT: Failure | {}\nTHROWN: {}", operationName, resultName, throwable);
        }
    }

    private void log(String message, Object... arguments) {
        switch (logLevel) {
            case TRACE -> LOGGER.trace(message, arguments);
            case DEBUG -> LOGGER.debug(message, arguments);
            case INFO -> LOGGER.info(message, arguments);
            case WARN -> LOGGER.warn(message, arguments);
            case ERROR -> LOGGER.error(message, arguments);
        }
    }
}
