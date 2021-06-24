package org.burningwave.core.io;

import static org.burningwave.core.assembler.StaticComponentContainer.ByteBufferHandler;
import static org.burningwave.core.assembler.StaticComponentContainer.Synchronizer;
import static org.burningwave.core.assembler.StaticComponentContainer.Throwables;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.function.Predicate;

import org.burningwave.core.Identifiable;
import org.burningwave.core.ManagedLogger;
import org.burningwave.core.function.Executor;
import org.burningwave.core.iterable.Properties;

class StreamsImpl implements Streams, Identifiable, Properties.Listener, ManagedLogger {
	/*int defaultBufferSize;
	Function<Integer, ByteBuffer> defaultByteBufferAllocator;*/
	String instanceId;
	
	StreamsImpl() {
		instanceId = getId();
	}
	
	@Override
	public boolean isArchive(File file) throws IOException {
		return is(file, this::isArchive);
	}
	
	@Override
	public boolean isJModArchive(File file) throws IOException {
		return is(file, this::isJModArchive);
	}
	
	@Override
	public boolean isClass(File file) throws IOException {
		return is(file, this::isClass);
	}
	
	@Override
	public boolean isArchive(ByteBuffer bytes) {
		return is(bytes, this::isArchive);
	}
	
	@Override
	public boolean isJModArchive(ByteBuffer bytes) {
		return is(bytes, this::isJModArchive);
	}
	
	@Override
	public boolean isClass(ByteBuffer bytes) {
		return is(bytes, this::isClass);
	}
	
	@Override
	public boolean is(File file, Predicate<Integer> predicate) throws IOException {
		try (RandomAccessFile raf = new RandomAccessFile(file, "r")){
			return raf.length() > 4 && predicate.test(raf.readInt());
	    }
	}
	
	private boolean is(ByteBuffer bytes, Predicate<Integer> predicate) {
		return bytes.capacity() > 4 && bytes.limit() > 4 && predicate.test(ByteBufferHandler.duplicate(bytes).getInt());
	}
	
	private boolean isArchive(int fileSignature) {
		return fileSignature == 0x504B0304 || fileSignature == 0x504B0506 || fileSignature == 0x504B0708 || isJModArchive(fileSignature);
	}
	
	private boolean isJModArchive(int fileSignature) {
		return fileSignature == 0x4A4D0100 || fileSignature == 0x4A4D0000;
	}
	
	private boolean isClass(int fileSignature) {
		return fileSignature == 0xCAFEBABE;
	}

	@Override
	public byte[] toByteArray(InputStream inputStream) {
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(ByteBufferHandler.getDefaultBufferSize())) {
			copy(inputStream, outputStream);
			return outputStream.toByteArray();
		} catch (Throwable exc) {
			return Throwables.throwException(exc);
		}
	}
	
	@Override
	public ByteBuffer toByteBuffer(InputStream inputStream, int streamSize) {
		/*try (ByteBufferOutputStream outputStream = new ByteBufferOutputStream(streamSize > -1? streamSize : defaultBufferSize)) {
			copy(inputStream, outputStream);
			return outputStream.toByteBuffer();
		}*/
		try {
			byte[] heapBuffer = new byte[ByteBufferHandler.getDefaultBufferSize()];
			int bytesRead;
			int byteBufferSize = streamSize > -1? streamSize : ByteBufferHandler.getDefaultBufferSize();
			ByteBuffer byteBuffer = ByteBufferHandler.allocate(byteBufferSize);
			while (-1 != (bytesRead = inputStream.read(heapBuffer))) {
				byteBuffer = ByteBufferHandler.put(byteBuffer, heapBuffer, bytesRead);
			}
			return shareContent(byteBuffer);
		} catch (Throwable exc) {
			return Throwables.throwException(exc);
		}
	}
	
	@Override
	public ByteBuffer toByteBuffer(InputStream inputStream) {
		return toByteBuffer(inputStream, -1);
	}
	
	@Override
	public StringBuffer getAsStringBuffer(InputStream inputStream) {
		return Executor.get(() -> {
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(
						inputStream
					)
				)
			) {
				StringBuffer result = new StringBuffer();
				String sCurrentLine;
				while ((sCurrentLine = reader.readLine()) != null) {
					result.append(sCurrentLine + "\n");
				}
				return result;
			}
		});
	}
	
	@Override
	public void copy(InputStream input, OutputStream output) {
		Executor.run(() -> {
			byte[] buffer = new byte[ByteBufferHandler.getDefaultBufferSize()];
			int bytesRead = 0;
			while (-1 != (bytesRead = input.read(buffer))) {
				output.write(buffer, 0, bytesRead);
			}
		});
	}
	
	@Override
	public byte[] toByteArray(ByteBuffer byteBuffer) {
    	byteBuffer = shareContent(byteBuffer);
    	byte[] result = new byte[ByteBufferHandler.limit(byteBuffer)];
    	byteBuffer.get(result, 0, result.length);
        return result;
    }

	@Override
	public ByteBuffer shareContent(ByteBuffer byteBuffer) {
		ByteBuffer duplicated = ByteBufferHandler.duplicate(byteBuffer);
		if (ByteBufferHandler.position(byteBuffer) > 0) {
			ByteBufferHandler.flip(duplicated);
		}		
		return duplicated;
	}
	
	@Override
	public FileSystemItem store(String fileAbsolutePath, byte[] bytes) {
		return store(fileAbsolutePath, ByteBufferHandler.allocate(bytes.length).put(bytes, 0, bytes.length));
	}
	
	@Override
	public FileSystemItem store(String fileAbsolutePath, ByteBuffer bytes) {
		ByteBuffer content = shareContent(bytes);
		File file = new File(fileAbsolutePath);
		Synchronizer.execute(fileAbsolutePath, () -> {
			if (!file.exists()) {
				new File(file.getParent()).mkdirs();
			} else {
				file.delete();
			}
			Executor.run(() -> {					
				try(ByteBufferInputStream inputStream = new ByteBufferInputStream(content); FileOutputStream fileOutputStream = FileOutputStream.create(file, true)) {
					copy(inputStream, fileOutputStream);
				}
			});
		});
		return FileSystemItem.ofPath(file.getAbsolutePath());
	}
}