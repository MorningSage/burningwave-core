package org.burningwave.core.jvm;

import static org.burningwave.core.assembler.StaticComponentContainer.BackgroundExecutor;
import static org.burningwave.core.assembler.StaticComponentContainer.Fields;
import static org.burningwave.core.assembler.StaticComponentContainer.IterableObjectHelper;
import static org.burningwave.core.assembler.StaticComponentContainer.LowLevelObjectsHandler;
import static org.burningwave.core.assembler.StaticComponentContainer.ManagedLoggersRepository;
import static org.burningwave.core.assembler.StaticComponentContainer.Methods;
import static org.burningwave.core.assembler.StaticComponentContainer.Throwables;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.burningwave.core.Component;
import org.burningwave.core.io.ByteBufferOutputStream;
import org.burningwave.core.iterable.Properties;
import org.burningwave.core.iterable.Properties.Event;

@SuppressWarnings("unchecked")
public class ByteBufferHandler implements Component {
	
	public static class Configuration {
		
		public static class Key {
		
			static final String BYTE_BUFFER_SIZE = "byte-buffer-handler.default-buffer-size";
			static final String BYTE_BUFFER_ALLOCATION_MODE = "byte-buffer-handler.default-allocation-mode";
		
		}
		
		public final static Map<String, Object> DEFAULT_VALUES;
		
		static {
			Map<String, Object> defaultValues = new HashMap<>();
			
			defaultValues.put(Key.BYTE_BUFFER_SIZE, "1024");
			defaultValues.put(
				Key.BYTE_BUFFER_ALLOCATION_MODE,
				"ByteBuffer::allocateDirect"
			);
			
			DEFAULT_VALUES = Collections.unmodifiableMap(defaultValues);
		}
	}
	
	Field directAllocatedByteBufferAddressField;
	int defaultBufferSize;
	Function<Integer, ByteBuffer> defaultByteBufferAllocator;
    final static float reallocationFactor = 1.1f;
	
	public ByteBufferHandler(java.util.Properties config) {
		init(config);
	}

	void init(java.util.Properties config) {
		setDefaultByteBufferSize(config);
		setDefaultByteBufferAllocationMode(config);
		if (config instanceof Properties) {
			listenTo((Properties)config);
		}
		BackgroundExecutor.createTask(() -> {
			deferredInit(config);
			synchronized (this) {
				this.notifyAll();
			}
		}).setName("ByteBufferHandler initializer").submit();
	}

	void deferredInit(java.util.Properties config) {
		try {
			if (LowLevelObjectsHandler == null) {
				synchronized (LowLevelObjectsHandler.class) {
					if (LowLevelObjectsHandler == null) {							
						LowLevelObjectsHandler.class.wait();
					}
				}
			}
			Class<?> directByteBufferClass = ByteBuffer.allocateDirect(0).getClass();
			while (directByteBufferClass != null && directAllocatedByteBufferAddressField == null) {
				directAllocatedByteBufferAddressField = LowLevelObjectsHandler.getDeclaredField(directByteBufferClass, field -> "address".equals(field.getName()));
				directByteBufferClass = directByteBufferClass.getSuperclass();
			}
		} catch (InterruptedException exc) {
			Throwables.throwException(exc);
		}
	}
	
	private void setDefaultByteBufferSize(java.util.Properties config) {
		String defaultBufferSize = IterableObjectHelper.resolveStringValue(config, Configuration.Key.BYTE_BUFFER_SIZE, Configuration.DEFAULT_VALUES);
		try {
			this.defaultBufferSize = Integer.valueOf(defaultBufferSize);
		} catch (Throwable exc) {
			String unit = defaultBufferSize.substring(defaultBufferSize.length()-2);
			String value = defaultBufferSize.substring(0, defaultBufferSize.length()-2);
			if (unit.equalsIgnoreCase("KB")) {
				this.defaultBufferSize = new BigDecimal(value).multiply(new BigDecimal(1024)).intValue();
			} else if (unit.equalsIgnoreCase("MB")) {
				this.defaultBufferSize = new BigDecimal(value).multiply(new BigDecimal(1024 * 1024)).intValue();
			} else {
				this.defaultBufferSize = Integer.valueOf(value);
			};
		}
		ManagedLoggersRepository.logInfo(getClass()::getName, "default buffer size: {} bytes", this.defaultBufferSize);
	}
	
	private void setDefaultByteBufferAllocationMode(java.util.Properties config) {
		String defaultByteBufferAllocationMode = IterableObjectHelper.resolveStringValue(config, Configuration.Key.BYTE_BUFFER_ALLOCATION_MODE, Configuration.DEFAULT_VALUES);
		if (defaultByteBufferAllocationMode.equalsIgnoreCase("ByteBuffer::allocate")) {
			this.defaultByteBufferAllocator = this::allocateInHeap;
			ManagedLoggersRepository.logInfo(getClass()::getName, "default allocation mode: ByteBuffer::allocate");
		} else {
			this.defaultByteBufferAllocator = this::allocateDirect;
			ManagedLoggersRepository.logInfo(getClass()::getName, "default allocation mode: ByteBuffer::allocateDirect");
		}
	}
	
	@Override
	public <K, V> void processChangeNotification(Properties config, Event event, K key, V newValue, V previousValue) {
		if (event.name().equals(Event.PUT.name())) {
			if (key instanceof String) {
				String keyAsString = (String)key;
				if (keyAsString.equals(Configuration.Key.BYTE_BUFFER_SIZE)) {
					setDefaultByteBufferSize(config);
				} else if (keyAsString.equals(Configuration.Key.BYTE_BUFFER_ALLOCATION_MODE)) {
					setDefaultByteBufferAllocationMode(config);
				}
			}
		}
	}
	
	public int getDefaultBufferSize() {
		return defaultBufferSize;
	}
	
	public static ByteBufferHandler create(java.util.Properties config) {
		return new ByteBufferHandler(config);
	}
	
	public ByteBuffer allocate(int capacity) {
		return defaultByteBufferAllocator.apply(capacity);
	}
	
	public ByteBuffer allocateInHeap(int capacity) {
		return ByteBuffer.allocate(capacity);
	}
	
	public ByteBuffer allocateDirect(int capacity) {
		return ByteBuffer.allocateDirect(capacity);
	}
	
	public ByteBuffer duplicate(ByteBuffer buffer) {
		return buffer.duplicate();
	}
	
	public <T extends Buffer> int limit(T buffer) {
		return ((Buffer)buffer).limit();
	}
	
	public <T extends Buffer> int position(T buffer) {
		return ((Buffer)buffer).position();
	}
	
	public <T extends Buffer> T limit(T buffer, int newLimit) {
		return (T)((Buffer)buffer).limit(newLimit);
	}

	public <T extends Buffer> T position(T buffer, int newPosition) {
		return (T)((Buffer)buffer).position(newPosition);
	}
	
	public <T extends Buffer> T flip(T buffer) {
		return (T)((Buffer)buffer).flip();
	}
	
	public <T extends Buffer> int capacity(T buffer) {
		return ((Buffer)buffer).capacity();
	}
	
	public <T extends Buffer> int remaining(T buffer) {
		return ((Buffer)buffer).remaining();
	}
	
	public ByteBuffer put(ByteBuffer byteBuffer, byte[] heapBuffer) {
		return put(byteBuffer, heapBuffer, heapBuffer.length);
	}
	
	public ByteBuffer put(ByteBuffer byteBuffer, byte[] heapBuffer, int bytesToWrite) {
		return put(byteBuffer, heapBuffer, bytesToWrite, 0);
	}
	
	public ByteBuffer shareContent(ByteBuffer byteBuffer) {
		ByteBuffer duplicated = duplicate(byteBuffer);
		if (position(byteBuffer) > 0) {
			flip(duplicated);
		}		
		return duplicated;
	}
	
	public ByteBuffer put(ByteBuffer byteBuffer, byte[] heapBuffer, int bytesToWrite, int initialPosition) {
		byteBuffer = ensureRemaining(byteBuffer, bytesToWrite, initialPosition);
		byteBuffer.put(heapBuffer, 0, bytesToWrite);
		return byteBuffer;
	}
	
	public byte[] toByteArray(ByteBuffer byteBuffer) {
    	byteBuffer = shareContent(byteBuffer);
    	byte[] result = new byte[limit(byteBuffer)];
    	byteBuffer.get(result, 0, result.length);
        return result;
	}
	
    public ByteBuffer ensureRemaining(ByteBuffer byteBuffer, int requiredBytes) {
        return ensureRemaining(byteBuffer, requiredBytes, 0);
    } 
	
    public ByteBuffer ensureRemaining(ByteBuffer byteBuffer, int requiredBytes, int initialPosition) {
        if (requiredBytes > remaining(byteBuffer)) {
        	return expandBuffer(byteBuffer, requiredBytes, initialPosition);
        }
        return byteBuffer;
    }  
	
	public ByteBuffer expandBuffer(ByteBuffer byteBuffer, int requiredBytes) {
		return expandBuffer(byteBuffer, requiredBytes, 0);
	}

	public ByteBuffer expandBuffer(ByteBuffer byteBuffer, int requiredBytes, int initialPosition) {
		int limit = limit(byteBuffer);
		ByteBuffer newBuffer = allocate(Math.max((int)(limit * reallocationFactor), position(byteBuffer) + requiredBytes));
		flip(byteBuffer);
		newBuffer.put(byteBuffer);
        limit(byteBuffer, limit);
        position(byteBuffer, initialPosition);
        return newBuffer;        
	}
	
	public <T extends Buffer> long getAddress(T buffer) {
		try {
			return (long)LowLevelObjectsHandler.getFieldValue(buffer, directAllocatedByteBufferAddressField);
		} catch (NullPointerException exc) {
			return (long)LowLevelObjectsHandler.getFieldValue(buffer, getDirectAllocatedByteBufferAddressField());
		}
	}
	
	private Field getDirectAllocatedByteBufferAddressField() {
		if (directAllocatedByteBufferAddressField == null) {
			synchronized (this) {
				if (directAllocatedByteBufferAddressField == null) {
					try {
						this.wait();
					} catch (InterruptedException exc) {
						Throwables.throwException(exc);
					}
				}
			}
		}
		return directAllocatedByteBufferAddressField;
	}

	public <T extends Buffer> boolean destroy(T buffer, boolean force) {
		if (buffer.isDirect()) {
			ByteBufferHandler.Cleaner cleaner = getCleaner(buffer, force);
			if (cleaner != null) {
				return cleaner.clean();
			}
			return false;
		} else {
			return true;
		}
	}
	
	private <T extends Buffer> Object getInternalCleaner(T buffer, boolean findInAttachments) {
		if (buffer.isDirect()) {
			if (buffer != null) {
				Object cleaner;
				if ((cleaner = Fields.get(buffer, "cleaner")) != null) {
					return cleaner;
				} else if (findInAttachments){
					return getInternalCleaner(Fields.getDirect(buffer, "att"), findInAttachments);
				}
			}
		}
		return null;
	}
	
	private <T extends Buffer> Object getInternalDeallocator(T buffer, boolean findInAttachments) {
		if (buffer.isDirect()) {
			Object cleaner = getInternalCleaner(buffer, findInAttachments);
			if (cleaner != null) {
				return Fields.getDirect(cleaner, "thunk");
			}
		}
		return null;
	}
	
	private <T extends Buffer> Collection<T> getAllLinkedBuffers(T buffer) {
		Collection<T> allLinkedBuffers = new ArrayList<>();
		allLinkedBuffers.add(buffer);
		while((buffer = Fields.getDirect(buffer, "att")) != null) {
			allLinkedBuffers.add(buffer);
		}
		return allLinkedBuffers;
	}
	
	public ByteBuffer newByteBufferWithDefaultSize() {
		return allocate(defaultBufferSize);
	}
	
	public ByteBuffer newByteBuffer(int size) {
		return allocate(size > -1? size : defaultBufferSize);
	}
	
	public ByteBufferOutputStream newByteBufferOutputStreamWithDefaultSize() {
		return new ByteBufferOutputStream(defaultBufferSize);
	}
	
	public ByteBufferOutputStream newByteBufferOutputStream(int size) {
		return new ByteBufferOutputStream(size > -1? size : defaultBufferSize);
	}
	
	public byte[] newByteArrayWithDefaultSize() {
		return new byte[defaultBufferSize];
	}
	
	public byte[] newByteArray(int size) {
		return new byte[size > -1? size : defaultBufferSize];
	}
	
	public  <T extends Buffer> ByteBufferHandler.Cleaner getCleaner(T buffer, boolean findInAttachments) {
		Object cleaner;
		if ((cleaner = getInternalCleaner(buffer, findInAttachments)) != null) {
			return new Cleaner () {
				
				@Override
				public boolean clean() {
					if (getAddress() != 0) {
						Methods.invokeDirect(cleaner, "clean");
						getAllLinkedBuffers(buffer).stream().forEach(linkedBuffer ->
							Fields.setDirect(linkedBuffer, "address", 0L)
						);							
						return true;
					}
					return false;
				}
				
				long getAddress() {
					return Long.valueOf((long)Fields.getDirect(Fields.getDirect(cleaner, "thunk"), "address"));
				}

				@Override
				public boolean cleaningHasBeenPerformed() {
					return getAddress() == 0;
				}
				
			};
		}
		return null;
	}
	
	public <T extends Buffer> ByteBufferHandler.Deallocator getDeallocator(T buffer, boolean findInAttachments) {
		if (buffer.isDirect()) {
			Object deallocator;
			if ((deallocator = getInternalDeallocator(buffer, findInAttachments)) != null) {
				return new Deallocator() {
					
					@Override
					public boolean freeMemory() {
						if (getAddress() != 0) {
							Methods.invokeDirect(deallocator, "run");
							getAllLinkedBuffers(buffer).stream().forEach(linkedBuffer ->
								Fields.setDirect(linkedBuffer, "address", 0L)
							);	
							return true;
						} else {
							return false;
						}
					}

					public long getAddress() {
						return Long.valueOf((long)Fields.getDirect(deallocator, "address"));
					}

					@Override
					public boolean memoryHasBeenReleased() {
						return getAddress() == 0;
					}
					
				};
			}
		}
		return null;
	}
	
	public static interface Deallocator {
		
		public boolean freeMemory();
		
		boolean memoryHasBeenReleased();
		
	}
	
	public static interface Cleaner {
		
		public boolean clean();
		
		public boolean cleaningHasBeenPerformed();
		
	}

}