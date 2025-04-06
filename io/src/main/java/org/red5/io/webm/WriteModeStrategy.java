package org.red5.io.webm;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public interface WriteModeStrategy {
  void initialize(RandomAccessFile file, File originalFile) throws IOException;

  void finalizeWrite(RandomAccessFile file, File originalFile) throws IOException;
}
