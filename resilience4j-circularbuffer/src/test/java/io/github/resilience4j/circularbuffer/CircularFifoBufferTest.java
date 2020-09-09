/*
 *
 *  Copyright 2016 Robert Winkler and Bohdan Storozhuk
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
package io.github.resilience4j.circularbuffer;


import org.junit.Test;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CircularFifoBufferTest {

    @Test
    public void testCircularFifoBuffer() {
        CircularFifoBuffer<Exception> exceptionBuffer = new ConcurrentCircularFifoBuffer<>(4);

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

        assertThat(exceptionBuffer.toStream()).hasSize(4);
        assertThat(exceptionBuffer.toStream()).hasOnlyElementsOfTypes(IllegalArgumentException.class,
            IOException.class, IllegalStateException.class, UnknownHostException.class);

        // The size must still be 4, because the CircularFifoBuffer capacity is 4
        exceptionBuffer.add(new IOException("bla bla"));
        assertThat(exceptionBuffer.size()).isEqualTo(4);

        exceptionBuffer.add(new IOException("bla bla"));
        assertThat(exceptionBuffer.size()).isEqualTo(4);

        exceptionBuffer.add(new IOException("bla bla"));
        assertThat(exceptionBuffer.size()).isEqualTo(4);

        assertThat(exceptionBuffer.take().get()).isInstanceOf(UnknownHostException.class);
        assertThat(exceptionBuffer.take().get()).isInstanceOf(IOException.class);
        assertThat(exceptionBuffer.take().get()).isInstanceOf(IOException.class);
        assertThat(exceptionBuffer.take().get()).isInstanceOf(IOException.class);
        assertThat(exceptionBuffer.take().isEmpty()).isTrue();
    }
}
