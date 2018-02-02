package io.github.resilience4j.feign;



import static feign.Util.checkArgument;
import static feign.Util.checkNotNull;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import feign.Request;
import feign.RequestTemplate;
import feign.Target;

public class FallbackTarget<T> implements Target<T> {

    private final Class<T> type;
    private final String name;
    private final String[] urls;
    private final AtomicInteger index;

    public FallbackTarget(Class<T> type, String[] urls) {
        this.type = checkNotNull(type, "type");
        checkArgument(urls != null && urls.length > 0 && urls[0] != null, "At least one url must be provided!");
        this.urls = urls;
        this.name = urls[0];
        this.index = new AtomicInteger(0);
    }

    @Override
    public Class<T> type() {
        return type;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String url() {
        return urls[index.get()];
    }

    public synchronized void reset() {
        index.set(0);
    }

    public synchronized boolean nextFallback() {
        final int i = index.get();
        if (i < urls.length - 1) {
            index.incrementAndGet();
            return true;
        }
        return false;
    }

    @Override
    public Request apply(RequestTemplate input) {
        if (input.url().indexOf("http") != 0) {
            input.insert(0, url());
        }
        return input.request();
    }

    @Override
    public String toString() {
        return "FallbackTarget [type=" + type + ", name=" + name + ", urls=" + Arrays.toString(urls) + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + Arrays.hashCode(urls);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final FallbackTarget<?> other = (FallbackTarget<?>) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        if (!Arrays.equals(urls, other.urls))
            return false;
        return true;
    }

}
