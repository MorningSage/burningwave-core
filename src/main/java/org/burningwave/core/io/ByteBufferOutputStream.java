/*
 * This file is part of Burningwave Core.
 *
 * Author: Roberto Gentili
 *
 * Hosted at: https://github.com/burningwave/core
 *
 * --
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Roberto Gentili
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
 * EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.burningwave.core.io;


import static org.burningwave.core.assembler.StaticComponentContainer.ByteBufferHandler;
import static org.burningwave.core.assembler.StaticComponentContainer.Streams;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;


public class ByteBufferOutputStream extends OutputStream {

    private Integer initialCapacity;
    private Integer initialPosition;
    private ByteBuffer buffer;
    
    public ByteBufferOutputStream() {
    	this(ByteBufferHandler.getDefaultBufferSize());
    }
    
    public ByteBufferOutputStream(ByteBuffer buffer) {
        this.buffer = buffer;
        this.initialPosition = ByteBufferHandler.position(buffer);
        this.initialCapacity = ByteBufferHandler.capacity(buffer);
    }

    public ByteBufferOutputStream(int initialCapacity) {
        this(ByteBufferHandler.allocate(initialCapacity));
    }
    
    @Override
	public void write(int b) {
        ensureRemaining(1);
        buffer.put((byte) b);
    }

    @Override
	public void write(byte[] bytes, int off, int len) {
        ensureRemaining(len);
        buffer.put(bytes, off, len);
    }

    public void write(ByteBuffer sourceBuffer) {
        ensureRemaining(sourceBuffer.remaining());
        buffer.put(sourceBuffer);
    }

    public int position() {
        return ByteBufferHandler.position(buffer);
    }

    public int remaining() {
        return ByteBufferHandler.remaining(buffer);
    }

    public int limit() {
        return ByteBufferHandler.limit(buffer);
    }

    public void position(int position) {
        ensureRemaining(position - ByteBufferHandler.position(buffer));
        ByteBufferHandler.position(buffer, position);
    }

    public int initialCapacity() {
        return initialCapacity;
    }

    public void ensureRemaining(int remainingBytesRequired) {
        if (remainingBytesRequired > buffer.remaining()) {
            expandBuffer(remainingBytesRequired);
        }
    }

    private void expandBuffer(int remainingRequired) {
    	int limit = ByteBufferHandler.limit(buffer);
    	int expandSize = Math.max((int) (limit * ByteBufferHandler.getReallocationFactor()), ByteBufferHandler.position(buffer) + remainingRequired);
        ByteBuffer temp = ByteBufferHandler.allocate(expandSize);        
        ByteBufferHandler.flip(buffer);
        temp.put(buffer);
        ByteBufferHandler.limit(buffer, limit);
        ByteBufferHandler.position(buffer, initialPosition);
        buffer = temp;
    }
    
    
    InputStream toBufferedInputStream() {
        return new ByteBufferInputStream(buffer);
    }
    
	public ByteBuffer toByteBuffer() {
		return Streams.shareContent(buffer);
	}

	public byte[] toByteArray() {
		return Streams.toByteArray(toByteBuffer());
	}
    
    @Override
    public void close() {
    	this.initialCapacity = null;
		this.initialPosition = null;
		this.buffer = null;
    }
}