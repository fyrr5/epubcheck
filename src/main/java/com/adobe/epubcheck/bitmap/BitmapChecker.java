/*
 * Copyright (c) 2007 Adobe Systems Incorporated
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of
 *  this software and associated documentation files (the "Software"), to deal in
 *  the Software without restriction, including without limitation the rights to
 *  use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 *  the Software, and to permit persons to whom the Software is furnished to do so,
 *  subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 *  FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 *  COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 *  IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package com.adobe.epubcheck.bitmap;

import com.adobe.epubcheck.api.Report;
import com.adobe.epubcheck.messages.MessageId;
import com.adobe.epubcheck.messages.MessageLocation;
import com.adobe.epubcheck.ocf.OCFPackage;
import com.adobe.epubcheck.ocf.OCFZipPackage;
import com.adobe.epubcheck.opf.ContentChecker;
import com.adobe.epubcheck.util.CheckUtil;
import com.sun.imageio.plugins.gif.GIFStreamMetadata;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

public class BitmapChecker implements ContentChecker
{
  private final OCFPackage ocf;
  private final Report report;
  private final String path;
  private final String mimeType;
  private static final int HEIGHT_MAX = 2 * 1080;
  private static final int WIDTH_MAX = 2 * 1920;
  private static final long IMAGESIZE_MAX = 4 * 1024 * 1024;

  BitmapChecker(OCFPackage ocf, Report report, String path, String mimeType)
  {
    this.ocf = ocf;
    this.report = report;
    this.path = path;
    this.mimeType = mimeType;
  }

  private void checkHeader(byte[] header)
  {
    boolean passed;
    if (mimeType.equals("image/jpeg"))
    {
      passed = header[0] == (byte) 0xFF && header[1] == (byte) 0xD8;
    }
    else if (mimeType.equals("image/gif"))
    {
      passed = header[0] == (byte) 'G' && header[1] == (byte) 'I'
          && header[2] == (byte) 'F' && header[3] == (byte) '8';
    }
    else
    {
      passed = !mimeType.equals("image/png") || header[0] == (byte) 0x89 && header[1] == (byte) 'P' && header[2] == (byte) 'N' && header[3] == (byte) 'G';
    }
    if (!passed)
    {
      report.message(MessageId.OPF_029, new MessageLocation(this.ocf.getName(), 0, 0), path, mimeType);
    }
  }


  /**
   * Gets image dimensions for given file
   *
   * @param imgFileName image file
   * @return dimensions of image
   * @throws IOException if the file is not a known image
   */
  public ImageHeuristics getImageSizes(String imgFileName) throws
      IOException
  {
    int pos = imgFileName.lastIndexOf(".");
    if (pos == -1)
    {
      throw new IOException("No extension for file: " + imgFileName);
    }

    String suffix = imgFileName.substring(pos + 1);
    File tempFile = null;
    ImageReader reader = null;
    if ("svg".compareToIgnoreCase(suffix) == 0)
    {
      tempFile = getImageFile(ocf, imgFileName);
      if (tempFile != null)
      {
        return new ImageHeuristics(0, 0, tempFile.length());
      }
      return null;
    }

    Iterator<ImageReader> iterator = ImageIO.getImageReadersBySuffix(suffix);
    if (iterator.hasNext())
    {
      reader = iterator.next();
      ImageInputStream stream = null;
      try
      {
        tempFile = getImageFile(ocf, imgFileName);

        stream = new FileImageInputStream(tempFile);
        reader.setInput(stream);

        IIOMetadata metadata = reader.getStreamMetadata();
        long length = tempFile.length();
        if (metadata instanceof GIFStreamMetadata)
        {
          GIFStreamMetadata gifMetadata = (GIFStreamMetadata) metadata;
          return new ImageHeuristics(gifMetadata.logicalScreenWidth,
              gifMetadata.logicalScreenHeight,
              length);
        }
        return new ImageHeuristics(reader.getWidth(0),reader.getHeight(0), length);
      }
      catch (IOException e)
      {
        report.message(MessageId.PKG_021, new MessageLocation(imgFileName, -1, -1, imgFileName));
        return null;
      }
      catch (IllegalArgumentException argex)
      {
        report.message(MessageId.PKG_021, new MessageLocation(imgFileName, -1, -1, imgFileName));
        return null;
      }
      finally
      {
        if (reader != null)
        {
          reader.dispose();
        }
        if (stream != null)
        {
          stream.close();
        }
      }
    }
    throw new IOException("Not a known image file: " + imgFileName);
  }

  private File getImageFile(OCFPackage ocf, String imgFileName) throws IOException
  {
    if (ocf.getClass() == OCFZipPackage.class)
    {
      return getTempImageFile((OCFZipPackage) ocf, imgFileName);
    }
    else
    {
      return new File(ocf.getPackagePath() + File.separator + imgFileName);
    }
  }

  private class ImageHeuristics
  {
    public int width;
    public int height;
    public long length;

    public ImageHeuristics(int width, int height, long length)
    {
      this.width = width;
      this.height = height;
      this.length = length;
    }
  }

  private File getTempImageFile(OCFZipPackage ocf, String imgFileName) throws IOException
  {
    File file = null;
    FileOutputStream os = null;
    InputStream is = null;
    try
    {
      int pos = imgFileName.lastIndexOf(".");
      if (pos == -1)
      {
        throw new IOException("No extension for file: " + imgFileName);
      }
      String suffix = imgFileName.substring(pos);
      String prefix = "img";

      file = File.createTempFile(prefix, suffix);
      file.deleteOnExit();
      os = new FileOutputStream(file);

      is = ocf.getInputStream(imgFileName);
      if (is == null)
      {
        return null;
      }
      byte[] bytes = new byte[32 * 1024];
      int read;
      while ((read = is.read(bytes)) > 0)
      {
        os.write(bytes, 0, read);
      }
    }
    finally
    {
      if (os != null)
      {
        os.flush();
        os.close();
      }
      if (is != null)
      {
        is.close();
      }
    }
    return file;  //To change body of created methods use File | Settings | File Templates.
  }

  private void checkImageDimensions(String imageFileName)
  {
    try
    {
      ImageHeuristics h = getImageSizes(imageFileName);
      if (h != null)
      {
        if (h.height >= HEIGHT_MAX || h.width >= WIDTH_MAX)
        {
          report.message(MessageId.OPF_051, new MessageLocation(imageFileName, -1, -1, imageFileName));
        }
        if (h.length >= IMAGESIZE_MAX)
        {
          report.message(MessageId.OPF_057, new MessageLocation(imageFileName, -1, -1, imageFileName));
        }
      }
    }
    catch (IOException ex)
    {
      report.message(MessageId.PKG_021, new MessageLocation(imageFileName, -1, -1, imageFileName) );
    }
  }

  public void runChecks()
  {
    if (!ocf.hasEntry(path))
    {
      report.message(MessageId.RSC_001, new MessageLocation(this.ocf.getName(), -1, -1), path);
    }
    else if (!ocf.canDecrypt(path))
    {
      report.message(MessageId.RSC_004, new MessageLocation(this.ocf.getName(), 0, 0), path);
    }
    else
    {
      InputStream in = null;
      try
      {
        in = ocf.getInputStream(path);
        if (in == null)
        {
          report.message(MessageId.RSC_001, new MessageLocation(this.ocf.getName(), 0, 0), path);
        }
        byte[] header = new byte[4];
        int rd = CheckUtil.readBytes(in, header, 0, 4);
        if (rd < 4)
        {
          report.message(MessageId.MED_004, new MessageLocation(path, 0, 0));
        }
        else
        {
          checkHeader(header);
        }
        checkImageDimensions(path);
      }
      catch (IOException e)
      {
        report.message(MessageId.PKG_021, new MessageLocation(path, 0, 0, path));
      }
      finally
      {
        try
        {
          if (in != null)
          {
            in.close();
          }
        }
        catch (IOException ignored)
        {
          // eat it
        }
      }
    }
  }
}
