package org.red5.io.webm;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class OverwriteWriteMode implements WriteModeStrategy {
  @Override
  public void initialize(RandomAccessFile file, File originalFile) throws IOException {
    File tempFile = new File(originalFile.getAbsolutePath() + ".ser");
    if (tempFile.exists()) {
      tempFile.delete();
      tempFile.createNewFile();
    }
  }

  @Override
  public void finalizeWrite(RandomAccessFile file, File originalFile) throws IOException {
    file.seek(0);
    try (RandomAccessFile rf = new RandomAccessFile(originalFile, "rw")) {
      rf.getChannel().transferFrom(file.getChannel(), 0, file.length());
    }
  }
}
