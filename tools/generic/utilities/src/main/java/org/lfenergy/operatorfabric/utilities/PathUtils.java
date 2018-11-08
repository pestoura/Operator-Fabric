/* Copyright (c) 2018, RTE (http://www.rte-france.com)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.lfenergy.operatorfabric.utilities;


import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

/**
 * <p></p>
 * Created on 13/04/18
 *
 * @author davibind
 */
@Slf4j
public class PathUtils {

  public static final String OPEN_BAR = "rwxrwxrwx";

  /**
   * Utility class don't need to be instantiated;
   */
  private PathUtils(){
  }


  /**
   * Extract absolute path from file
   * @param f source file
   * @return an absolute Path
   */
  public static Path getPath(File f) {
    return Paths.get(f.getAbsolutePath());
  }

  public static void moveDir(Path source, Path target) throws IOException {
    copyDir(source, target);
    deleteDir(source);
  }

  public static void copyDir(Path source, Path target) throws IOException {
    Files.walkFileTree(source, new CopyDir(source, target));
  }

  public static void deleteDir(Path source) throws IOException {
    if (source.toFile().exists())
      Files.walkFileTree(source, new DeleteDir());
    else
      throw new FileNotFoundException("Specified path to delete not found in file system");
  }

  public static void copy(Path source, Path target) throws IOException {
    if (source.toFile().isDirectory())
      copyDir(source, target);
    else
      Files.copy(source, target);
  }

  /**
   * Delete the file or directory targeted by source path. Logging exception instead of throwing them
   * @param source
   */
  public static boolean silentDelete(Path source) {
    try {
      delete(source);
      return true;
    } catch (IOException e) {
      log.warn("Unnable to silent delete "+source.toString(),e);
      return false;
    }
  }

  /**
   * Delete the file or directory targeted by source path
   * @param source
   * @throws IOException
   */
  public static void delete(Path source) throws IOException {
    if (!source.toFile().exists())
      throw new FileNotFoundException(source.toAbsolutePath().toString()+" does not exist");
    if (source.toFile().isDirectory())
      deleteDir(source);
    else {
      log.debug("deleting " + source.toString());
      Files.delete(source);
    }
  }

  /**
   *
   * @param is tar.gz inputstream
   * @param outPath output folder
   * @throws IOException
   */
  public static void unTarGz(InputStream is, Path outPath) throws IOException {
    createDirIfNeeded(outPath);
    try (BufferedInputStream bis = new BufferedInputStream(is);
         GzipCompressorInputStream gzis = new GzipCompressorInputStream(bis);
         TarArchiveInputStream tis = new TarArchiveInputStream(gzis)) {
      TarArchiveEntry entry;
      //loop over tar entries
      while ((entry = tis.getNextTarEntry()) != null) {
        if (entry.isDirectory()) {
          //create empty folders
          createDirIfNeeded(outPath.resolve(entry.getName()));
        } else {
          //copy entry to files
          Path curPath = outPath.resolve(entry.getName());
          createDirIfNeeded(curPath.getParent());
          Files.copy(tis, curPath);
          Files.setPosixFilePermissions(curPath, PosixFilePermissions.fromString(OPEN_BAR));
        }
      }
    }
  }

  private static void createDirIfNeeded(Path dir) throws IOException {
    if (!dir.toFile().exists()) {
      FileAttribute<Set<PosixFilePermission>> attr = openBarPerms();
      Files.createDirectories(dir, attr);
    }
  }

  private static FileAttribute<Set<PosixFilePermission>> openBarPerms() {
    Set<PosixFilePermission> perms = PosixFilePermissions.fromString(OPEN_BAR);
    return PosixFilePermissions.asFileAttribute(perms);
  }
}

@AllArgsConstructor
@Slf4j
class CopyDir extends SimpleFileVisitor<Path> {

  private Path sourceDir;
  private Path targetDir;

  @Override
  public FileVisitResult preVisitDirectory(Path dir,
                                           BasicFileAttributes attributes) {
    Path newDir = targetDir.resolve(sourceDir.relativize(dir));
    try {
      Files.createDirectories(newDir);
    } catch (IOException ex) {
      log.error("error creating directory " + newDir.toString(), ex);
    }

    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
    try {
      Path targetFile = targetDir.resolve(sourceDir.relativize(file));
      Files.copy(file, targetFile);
    } catch (IOException ex) {
      log.error("error copying " + file.toString(), ex);
    }
    return FileVisitResult.CONTINUE;
  }
}

@Slf4j
class DeleteDir extends SimpleFileVisitor<Path> {

  @Override
  public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
    try {
      log.debug("deleting " + file.toString());
      Files.delete(file);
    } catch (IOException ex) {
      log.error("error deleting " + file.toString(), ex);
    }
    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
    Files.delete(dir);
    return FileVisitResult.CONTINUE;
  }
}
