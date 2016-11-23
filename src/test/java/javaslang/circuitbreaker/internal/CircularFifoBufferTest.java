/*
 *
 *  Copyright 2015 Robert Winkler
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
package javaslang.circuitbreaker.internal;


import javaslang.collection.List;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;

public class CircularFifoBufferTest {

    private static final Logger LOG = LoggerFactory.getLogger(CircularFifoBufferTest.class);

    @Test
    public void testCircularFifoBuffer(){
        CircularFifoBuffer<Exception> exceptionBuffer = new CircularFifoBuffer<>(4);
        // The initial index is -1
        assertThat(exceptionBuffer.size()).isEqualTo(0);
        assertThat(exceptionBuffer.isEmpty()).isTrue();
        assertThat(exceptionBuffer.isFull()).isFalse();
        exceptionBuffer.add(new IllegalArgumentException("bla bla"));
        assertThat(exceptionBuffer.size()).isEqualTo(1);
        exceptionBuffer.add(new IOException("bla bla"));
        assertThat(exceptionBuffer.size()).isEqualTo(2);
        exceptionBuffer.add(new IllegalStateException("bla bla"));
        assertThat(exceptionBuffer.size()).isEqualTo(3);
        assertThat(exceptionBuffer.isFull()).isFalse();
        assertThat(exceptionBuffer.isEmpty()).isFalse();
        exceptionBuffer.add(new UnknownHostException("bla bla"));
        assertThat(exceptionBuffer.size()).isEqualTo(4);
        assertThat(exceptionBuffer.isFull()).isTrue();

        List<Exception> bufferedExceptions = exceptionBuffer.toList();

        assertThat(bufferedExceptions.size()).isEqualTo(4);
        assertThat(bufferedExceptions.get(0)).isInstanceOf(IllegalArgumentException.class);
        assertThat(bufferedExceptions.get(1)).isInstanceOf(IOException.class);
        assertThat(bufferedExceptions.get(2)).isInstanceOf(IllegalStateException.class);
        assertThat(bufferedExceptions.get(3)).isInstanceOf(UnknownHostException.class);

        // The size must still be 4, because the CircularFifoBuffer capacity is 4
        exceptionBuffer.add(new IOException("bla bla"));
        assertThat(exceptionBuffer.size()).isEqualTo(4);

        exceptionBuffer.add(new IOException("bla bla"));
        assertThat(exceptionBuffer.size()).isEqualTo(4);

        exceptionBuffer.add(new IOException("bla bla"));
        assertThat(exceptionBuffer.size()).isEqualTo(4);

        bufferedExceptions = exceptionBuffer.toList();
        assertThat(bufferedExceptions.get(0)).isInstanceOf(UnknownHostException.class);
        assertThat(bufferedExceptions.get(1)).isInstanceOf(IOException.class);
        assertThat(bufferedExceptions.get(2)).isInstanceOf(IOException.class);
        assertThat(bufferedExceptions.get(3)).isInstanceOf(IOException.class);

        //bufferedExceptions.forEach(e -> LOG.info(e.toString()));
    }
}
