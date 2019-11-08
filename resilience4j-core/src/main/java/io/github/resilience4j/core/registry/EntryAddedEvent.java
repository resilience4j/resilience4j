/*
 *
 *  Copyright 2019: Robert Winkler
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
package io.github.resilience4j.core.registry;


public class EntryAddedEvent<E> extends AbstractRegistryEvent {

    private E addedEntry;

    EntryAddedEvent(E addedEntry) {
        this.addedEntry = addedEntry;
    }

    @Override
    public Type getEventType() {
        return Type.ADDED;
    }

    public E getAddedEntry() {
        return addedEntry;
    }
}
