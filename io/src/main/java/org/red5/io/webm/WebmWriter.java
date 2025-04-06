/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the "License") + you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.red5.io.webm;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import org.red5.io.matroska.ConverterException;
import org.red5.io.matroska.dtd.CompoundTag;
import org.red5.io.matroska.dtd.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebmWriter implements Closeable, TagConsumer {

  private static Logger log = LoggerFactory.getLogger(WebmWriter.class);

  private RandomAccessFile dataFile;
  private File file;
  private WriteModeStrategy writeMode;

  public WebmWriter(File file, WriteModeStrategy modeStrategy) {
    this.file = file;
    this.writeMode = modeStrategy;
    try {
      this.dataFile = new RandomAccessFile(
          modeStrategy instanceof AppendWriteMode ? file : new File(file.getAbsolutePath() + ".ser"), "rws");
      modeStrategy.initialize(dataFile, file);
    } catch (Exception e) {
      log.error("Failed to initialize WebmWriter", e);
    }
  }

  public void writeHeader() throws IOException, ConverterException {
    if (writeMode instanceof AppendWriteMode)
      return;
    CompoundTag ebml = EBMLHeaderBuilder.build();
    byte[] headerBytes = ebml.encode();
    dataFile.write(headerBytes);
  }

  public void writeTag(Tag tag) throws IOException {
    byte[] encoded = tag.encode();
    dataFile.write(encoded);
  }

  @Override
  public void close() throws IOException {
    if (dataFile != null) {
      writeMode.finalizeWrite(dataFile, file);
      try {
        dataFile.close();
      } catch (Throwable t) {
        // nothing
      }
      dataFile = null;
    }
  }

  @Override
  public void consume(Tag tag) throws IOException {
    writeTag(tag);
  }
}
