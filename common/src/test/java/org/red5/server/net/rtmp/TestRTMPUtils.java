package org.red5.server.net.rtmp;

import static org.junit.Assert.*;

import org.apache.mina.core.buffer.IoBuffer;
import org.junit.Test;

public class TestRTMPUtils {

  @Test
  public void testWriteAndReadReverseInt() {
    int value = 0x12345678;
    IoBuffer buffer = IoBuffer.allocate(4);
    RTMPUtils.writeReverseInt(buffer, value);
    buffer.flip();

    assertEquals((byte) 0x78, buffer.get());
    assertEquals((byte) 0x56, buffer.get());
    assertEquals((byte) 0x34, buffer.get());
    assertEquals((byte) 0x12, buffer.get());

    buffer.rewind();
    int readValue = RTMPUtils.readReverseInt(buffer);
    assertEquals(value, readValue);
  }

  @Test
  public void testWriteAndReadMediumInt() {
    int value = 0x123456;
    IoBuffer buffer = IoBuffer.allocate(3);
    RTMPUtils.writeMediumInt(buffer, value);
    buffer.flip();

    assertEquals((byte) 0x12, buffer.get());
    assertEquals((byte) 0x34, buffer.get());
    assertEquals((byte) 0x56, buffer.get());

    buffer.rewind();
    int readValue = RTMPUtils.readMediumInt(buffer);
    assertEquals(value, readValue);
    buffer.rewind();
    int readUnsignedValue = RTMPUtils.readUnsignedMediumInt(buffer);
    assertEquals(value, readUnsignedValue);
  }

  @Test
  public void testEncodeHeaderByte_ChannelId_OneByte() {
    IoBuffer buffer = IoBuffer.allocate(1);
    byte headerSize = 2;
    int channelId = 10;
    RTMPUtils.encodeHeaderByte(buffer, headerSize, channelId);
    buffer.flip();

    int expected = (headerSize << 6) + channelId;
    byte result = buffer.get();
    assertEquals((byte) expected, result);
  }

  @Test
  public void testEncodeHeaderByte_ChannelId_TwoBytes() {
    IoBuffer buffer = IoBuffer.allocate(2);
    byte headerSize = 1;
    int channelId = 100;
    RTMPUtils.encodeHeaderByte(buffer, headerSize, channelId);
    buffer.flip();

    byte first = buffer.get();
    byte second = buffer.get();
    assertEquals((byte) (headerSize << 6), first);
    assertEquals((byte) (channelId - 64), second);
  }

  @Test
  public void testEncodeHeaderByte_ChannelId_ThreeBytes() {
    IoBuffer buffer = IoBuffer.allocate(3);
    byte headerSize = 3;
    int channelId = 400;
    RTMPUtils.encodeHeaderByte(buffer, headerSize, channelId);
    buffer.flip();

    int expectedFirst = (headerSize << 6) | 1;
    byte first = buffer.get();
    byte second = buffer.get();
    byte third = buffer.get();
    assertEquals((byte) expectedFirst, first);
    int channelIdMinus64 = channelId - 64; // 400 - 64 = 336
    assertEquals((byte) (channelIdMinus64 & 0xff), second);
    assertEquals((byte) (channelIdMinus64 >> 8), third);
  }

  @Test
  public void testDecodeChannelId_OneByte() {

    int header = (2 << 6) + 10;
    int channelId = RTMPUtils.decodeChannelId(header, 1);
    assertEquals(10, channelId);
  }

  @Test
  public void testDecodeChannelId_TwoBytes() {

    int secondByte = 36;
    int channelId = RTMPUtils.decodeChannelId(secondByte, 2);
    assertEquals(100, channelId);
  }

  @Test
  public void testDecodeChannelId_ThreeBytes() {
    int header = (80 << 8) | 1;
    int channelId = RTMPUtils.decodeChannelId(header, 3);
    assertEquals(400, channelId);
  }

  @Test
  public void testDecodeHeaderSize() {

    int header = (2 << 6) | 10; // headerSize attendu = 2
    byte headerSize = RTMPUtils.decodeHeaderSize(header, 1);
    assertEquals((byte) 2, headerSize);

    int header2 = (3 << 14) | 0x3FFF; // headerSize attendu = 3
    headerSize = RTMPUtils.decodeHeaderSize(header2, 2);
    assertEquals((byte) 3, headerSize);

    int header3 = (1 << 22); // headerSize attendu = 1
    headerSize = RTMPUtils.decodeHeaderSize(header3, 3);
    assertEquals((byte) 1, headerSize);
  }

  @Test
  public void testGetHeaderLength() {

    assertEquals(12, RTMPUtils.getHeaderLength(RTMPUtils.HEADER_NEW));
    assertEquals(8, RTMPUtils.getHeaderLength(RTMPUtils.HEADER_SAME_SOURCE));
    assertEquals(4, RTMPUtils.getHeaderLength(RTMPUtils.HEADER_TIMER_CHANGE));
    assertEquals(1, RTMPUtils.getHeaderLength(RTMPUtils.HEADER_CONTINUE));

    assertEquals(-1, RTMPUtils.getHeaderLength((byte) 99));
  }

  @Test
  public void testCompareTimestampsAndDiffTimestamps() {

    int a = 1000, b = 1000;
    assertEquals(0, RTMPUtils.compareTimestamps(a, b));
    assertEquals(0, RTMPUtils.diffTimestamps(a, b));

    a = 1000;
    b = 2000;
    assertEquals(-1, RTMPUtils.compareTimestamps(a, b));
    assertTrue(RTMPUtils.diffTimestamps(a, b) < 0);

    a = 3000;
    b = 2000;
    assertEquals(1, RTMPUtils.compareTimestamps(a, b));
    assertTrue(RTMPUtils.diffTimestamps(a, b) > 0);

    a = 10;
    b = 0xFFFFFFF0;
    long diff = RTMPUtils.diffTimestamps(a, b);
    assertTrue(diff < 0);
  }
}
