package org.red5.server.net.rtmp.event;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.stream.IStreamData;

public abstract class MediaDataStreamEvent<T extends MediaDataStreamEvent<T>> extends BaseEvent
    implements IStreamData<T>, IStreamPacket {

  protected IoBuffer data;
  protected byte dataType;

  protected MediaDataStreamEvent(byte dataType) {
    this(IoBuffer.allocate(0).flip(), dataType);
  }

  protected MediaDataStreamEvent(IoBuffer data, byte dataType) {
    super(Type.STREAM_DATA);
    this.dataType = dataType;

    setData(data);
  }

  protected MediaDataStreamEvent(IoBuffer data, byte dataType, boolean copy) {
    super(Type.STREAM_DATA);
    this.dataType = dataType;

    if (copy) {
      byte[] array = new byte[data.remaining()];
      data.mark();
      data.get(array);
      data.reset();
      setData(array);
    } else {
      setData(data);
    }
  }

  @Override
  public byte getDataType() {
    return dataType;
  }

  public void setDataType(byte dataType) {
    this.dataType = dataType;
  }

  @Override
  public IoBuffer getData() {
    return data;
  }

  public void setData(IoBuffer data) {
    this.data = data;
  }

  public void setData(byte[] data) {
    this.data = IoBuffer.wrap(data);
  }

  @Override
  protected void releaseInternal() {
    if (data != null) {
      data.clear();
      data.free();
      data = null;
    }
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    super.readExternal(in);
    setData((byte[]) in.readObject());
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    byte[] array = (data == null) ? null
        : data.hasArray() ? data.array()
            : getBytesFromBuffer(data);
    out.writeObject(array);
  }

  public T duplicate() throws IOException, ClassNotFoundException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    writeExternal(oos);
    oos.close();

    byte[] buf = baos.toByteArray();
    ByteArrayInputStream bais = new ByteArrayInputStream(buf);
    ObjectInputStream ois = new ObjectInputStream(bais);

    T result = createInstance();
    result.readExternal(ois);

    if (header != null)
      result.setHeader(header.clone());
    result.setSourceType(sourceType);
    result.setSource(source);
    result.setTimestamp(timestamp);

    return result;
  }

  protected abstract T createInstance();

  private byte[] getBytesFromBuffer(IoBuffer buffer) {
    byte[] array = new byte[buffer.remaining()];
    buffer.mark();
    buffer.get(array);
    buffer.reset();
    return array;
  }

  @Override
  public String toString() {
    return String.format("%s - ts: %s length: %s", this.getClass().getSimpleName(), getTimestamp(),
        (data != null ? data.limit() : '0'));
  }

}
