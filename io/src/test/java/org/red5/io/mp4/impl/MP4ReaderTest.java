/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2023 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package org.red5.io.mp4.impl;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import junit.framework.TestCase;
import org.junit.Test;
import org.red5.io.ITag;
import org.red5.io.flv.IKeyFrameDataAnalyzer.KeyFrameMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MP4ReaderTest extends TestCase {

    private static Logger log = LoggerFactory.getLogger(MP4ReaderTest.class);

    @Test
    public void testCtor() throws Exception {

        File file = new File("target/test-classes/fixtures/mov_h264.mp4");
        if (!file.exists()) {
            log.warn("Test file not found: {}", file.getAbsolutePath());
            return;
        }

        MP4Reader reader = new MP4Reader(file);

        KeyFrameMeta meta = reader.analyzeKeyFrames();
        log.debug("KeyFrameMeta from {}: {}", file.getName(), meta);

        for (int t = 0; t < 10; t++) {
            if (!reader.hasMoreTags()) {
                break;
            }
            ITag tag = reader.readTag();
            log.debug("Tag #{} => {}", t, tag);
        }
        reader.close();
        log.info("----------------------------------------------------------------------------------");
    }

    @Test
    public void testAnalyzeKeyFrames() throws Exception {
        File file = new File("target/test-classes/fixtures/mov_h264.mp4");
        if (!file.exists()) {
            log.warn("Test file not found: {}", file.getAbsolutePath());
            return;
        }
        MP4Reader reader = new MP4Reader(file);
        KeyFrameMeta meta = reader.analyzeKeyFrames();
        assertNotNull(meta);
        log.debug("Keyframe meta: {}", meta);

        assertTrue("Duration should be > 0", meta.duration > 0);
        if (meta.positions != null && meta.timestamps != null) {
            assertEquals("positions/timestamps array length mismatch", meta.positions.length, meta.timestamps.length);
        }
        reader.close();
    }

    @Test
    public void testReadAllTags() throws Exception {
        File file = new File("target/test-classes/fixtures/mov_h264.mp4");
        if (!file.exists()) {
            log.warn("Test file not found: {}", file.getAbsolutePath());
            return;
        }
        MP4Reader reader = new MP4Reader(file);
        int count = 0;
        while (reader.hasMoreTags()) {
            ITag tag = reader.readTag();
            assertNotNull("readTag() returned null unexpectedly", tag);
            count++;
        }
        log.debug("Total tags read: {}", count);
        assertTrue("Should read at least 1 tag (metadata) from the file", count > 0);
        reader.close();
    }

    @Test
    public void testSeek() throws Exception {
        File file = new File("target/test-classes/fixtures/mov_h264.mp4");
        if (!file.exists()) {
            log.warn("Test file not found: {}", file.getAbsolutePath());
            return;
        }
        MP4Reader reader = new MP4Reader(file);

        long halfPosition = reader.getTotalBytes() / 2;
        reader.position(halfPosition);

        if (reader.hasMoreTags()) {
            ITag tag = reader.readTag();
            log.debug("Tag after seeking at pos {} => {}", halfPosition, tag);
            assertNotNull(tag);
        }
        reader.close();
    }

    @Test
    public void testBytes() throws Exception {
        // 00 40 94 00 00 00 00 00
        byte[] width = { (byte) 0x00, (byte) 0x40, (byte) 0x94, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };
        System.out.println("width: {}" + bytesToLong(width));

        byte[] arr = { (byte) 0, (byte) 0, (byte) 0x10, (byte) 0 };
        System.out.println("bbb: {}" + bytesToInt(arr));
    }

    public static long bytesToLong(byte[] data) {
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.put(data);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.flip();
        return buf.getLong();
    }

    public static int bytesToInt(byte[] data) {
        ByteBuffer buf = ByteBuffer.allocate(4);
        buf.put(data);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.flip();
        return buf.getInt();
    }

    public static short bytesToShort(byte[] data) {
        ByteBuffer buf = ByteBuffer.allocate(2);
        buf.put(data);
        buf.flip();
        return buf.getShort();
    }

    public static byte bytesToByte(byte[] data) {
        ByteBuffer buf = ByteBuffer.allocate(1);
        buf.put(data);
        buf.flip();
        return buf.get();
    }
}
