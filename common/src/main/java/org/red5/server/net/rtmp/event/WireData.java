package org.red5.server.net.rtmp.event;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.amf3.AMF3;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.stream.IStreamData;

/**
 * Wire data event
 */
public class WireData extends BaseEvent implements IStreamData<WireData>, IStreamPacket {

    private static final long serialVersionUID = -41022344677940L;

    protected IoBuffer data;

    /**
     * Data type
     */
    private final byte dataType = TYPE_WIRE_DATA;

    /**
     * Format type - AMF3: TYPE_JSON, TYPE_KLV, TYPE_STRING, TYPE_BYTE_ARRAY
     */
    private byte formatType = AMF3.TYPE_JSON; // default to JSON

    /** Constructs a new WireData */
    public WireData() {
        this(IoBuffer.allocate(0).flip());
    }

    public WireData(IoBuffer data) {
        super(Type.STREAM_DATA);
        setData(data);
    }

    /**
     * Create wire data event with given data buffer
     *
     * @param data
     *            Audio data
     * @param copy
     *            true to use a copy of the data or false to use reference
     */
    public WireData(IoBuffer data, boolean copy) {
        super(Type.STREAM_DATA);
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

    /** {@inheritDoc} */
    @Override
    public byte getDataType() {
        return dataType;
    }

    public byte getFormatType() {
        return formatType;
    }

    public IoBuffer getData() {
        return data;
    }

    public void setData(IoBuffer data) {
        this.data = data;
    }

    public void setData(byte[] data) {
        setData(IoBuffer.wrap(data));
    }

    public void reset() {
        releaseInternal();
    }

    /** {@inheritDoc} */
    @Override
    protected void releaseInternal() {
        if (data != null) {
            data.free();
            data = null;
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        // read the format type
        formatType = in.readByte();
        // read the data
        byte[] byteBuf = (byte[]) in.readObject();
        if (byteBuf != null) {
            setData(byteBuf);
        }
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        // write the format type
        out.writeByte(formatType);
        if (data != null) {
            // write the data
            if (data.hasArray()) {
                out.writeObject(data.array());
            } else {
                byte[] array = new byte[data.remaining()];
                data.mark();
                data.get(array);
                data.reset();
                out.writeObject(array);
            }
        } else {
            out.writeObject(null);
        }
    }

    /**
     * Duplicate this message / event.
     *
     * @return duplicated event
     */
    public WireData duplicate() throws IOException, ClassNotFoundException {
        WireData result = new WireData();
        // serialize
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        writeExternal(oos);
        oos.close();
        // convert to byte array
        byte[] buf = baos.toByteArray();
        baos.close();
        // create input streams
        ByteArrayInputStream bais = new ByteArrayInputStream(buf);
        ObjectInputStream ois = new ObjectInputStream(bais);
        // deserialize
        result.readExternal(ois);
        ois.close();
        bais.close();
        // clone the header if there is one
        if (header != null) {
            result.setHeader(header.clone());
        }
        result.setSourceType(sourceType);
        result.setSource(source);
        result.setTimestamp(timestamp);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format("WireData - ts: %s length: %s", getTimestamp(), (data != null ? data.limit() : '0'));
    }

}
