package org.red5.io.webm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class AppendWriteMode implements WriteModeStrategy {
  @Override
  public void initialize(RandomAccessFile file, File originalFile) throws IOException {
    if (!originalFile.exists() || !originalFile.canRead() || !originalFile.canWrite()) {
      throw new FileNotFoundException("File does not exist or cannot be accessed");
    }
  }

  @Override
  public void finalizeWrite(RandomAccessFile file, File originalFile) {
    // nothing for finalized
  }
}
