package ru.vyarus.yaml.config.updater.parse.comments.util;

import java.util.Iterator;

/**
 * Iterator, remembering processed items count.
 *
 * @param <T> value type
 * @author Vyacheslav Rusakov
 * @since 23.04.2021
 */
public class CountingIterator<T> implements Iterator<T> {

    private final Iterator<T> it;
    private int pos;

    public CountingIterator(final Iterator<T> it) {
        this.it = it;
    }

    @Override
    public boolean hasNext() {
        return it.hasNext();
    }

    @Override
    public T next() {
        final T res = it.next();
        // increment after to not count invalid attempts at the end
        pos++;
        return res;
    }

    /**
     * @return current item position, starting from 0
     */
    public int getPosition() {
        return pos;
    }
}
