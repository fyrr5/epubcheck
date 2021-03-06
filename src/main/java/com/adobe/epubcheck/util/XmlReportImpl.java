package com.adobe.epubcheck.util;

import com.adobe.epubcheck.api.MasterReport;
import com.adobe.epubcheck.messages.Message;
import com.adobe.epubcheck.messages.MessageLocation;
import com.adobe.epubcheck.messages.Severity;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;


public class XmlReportImpl extends MasterReport
{
  private final File outputFile;
  private PrintWriter out;

  private String epubCheckDate;
  private String epubCheckName = "epubcheck";
  private String epubCheckVersion;

  private String creationDate;
  private String lastModifiedDate;
  private String identifier;
  private String title;
  private Set<String> titles = new LinkedHashSet<String>();
  private final Set<String> creators = new LinkedHashSet<String>();
  private final Set<String> contributors = new LinkedHashSet<String>();
  private String publisher;
  private final Set<String> rights = new LinkedHashSet<String>();
  private String date;

  private String formatName;
  private String formatVersion;
  private long pagesCount;
  private long charsCount;
  private String language;
  private final Set<String> embeddedFonts = new LinkedHashSet<String>();
  private final Set<String> refFonts = new LinkedHashSet<String>();
  private final Set<String> references = new LinkedHashSet<String>();
  private boolean hasEncryption;
  private boolean hasSignatures;
  private boolean hasAudio;
  private boolean hasVideo;
  private boolean hasFixedLayout;
  private boolean hasScripts;

  private final List<String> warns = new ArrayList<String>();
  private final List<String> errors = new ArrayList<String>();
  private final List<String> fatalErrors = new ArrayList<String>();
  private final List<String> hints = new ArrayList<String>();

  public XmlReportImpl(File out, String ePubName, String versionEpubCheck)
  {
    this.outputFile = out;
    this.setEpubFileName(PathUtil.removeWorkingDirectory(ePubName));
    this.epubCheckVersion = versionEpubCheck;
  }

  public void initialize()
  {
  }

  @Override
  public void close()
  {
  }

  @Override
  public void message(Message message, MessageLocation location, Object... args)
  {
    if (message.getSeverity().equals(Severity.ERROR))
    {
      error(PathUtil.removeWorkingDirectory(location.getFileName()), location.getLine(), location.getColumn(), message.getMessage(args));
    }
    else if (message.getSeverity().equals(Severity.WARNING))
    {
      warning(PathUtil.removeWorkingDirectory(location.getFileName()), location.getLine(), location.getColumn(), message.getMessage(args));
    }
    else if (message.getSeverity().equals(Severity.FATAL))
    {
      fatalError(PathUtil.removeWorkingDirectory(location.getFileName()), location.getLine(), location.getColumn(), message.getMessage(args));
    }
  }

  void error(String resource, int line, int column, String message)
  {
    errors.add((resource == null ? "" : "/" + resource) +
        (line <= 0 ? "" : "(" + line + ")") + ": " + message);
    	
  }

  public void hint(String resource, int line, int column, String message)
  {
    hints.add((resource == null ? "" : "/" + resource) +
              (line <= 0 ? "" : "(" + line + ")") + ": " + message );

  }

  void fatalError(String resource, int line, int column, String message)
  {
    fatalErrors.add((resource == null ? "" : "/" + resource) +
        (line <= 0 ? "" : "(" + line + ")") + ": " + message);
  }

  void warning(String resource, int line, int column, String message)
  {
    warns.add((resource == null ? "" : "/" + resource) +
        (line <= 0 ? "" : "(" + line + ")") + ": " + message);
  }

  @Override
  public void info(String resource, FeatureEnum feature, String value)
  {
    switch (feature)
    {
      case TOOL_DATE:
        this.epubCheckDate = value;
        break;
      case TOOL_NAME:
        this.epubCheckName = value;
        break;
      case TOOL_VERSION:
        this.epubCheckVersion = value;
        break;
      case FORMAT_NAME:
        this.formatName = value;
        break;
      case FORMAT_VERSION:
        this.formatVersion = value;
        break;
      case CREATION_DATE:
        this.creationDate = value;
        break;
      case MODIFIED_DATE:
        this.lastModifiedDate = value;
        break;
      case PAGES_COUNT:
        this.pagesCount = Long.parseLong(value);
        break;
      case CHARS_COUNT:
        this.charsCount += Long.parseLong(value);
        break;
      case DECLARED_MIMETYPE:
        if (value != null && value.startsWith("audio/"))
        {
          this.hasAudio = true;
        }
        else if (value != null && value.startsWith("video/"))
        {
          this.hasVideo = true;
        }
        break;
      case FONT_EMBEDDED:
        this.embeddedFonts.add(value);
        break;
      case FONT_REFERENCE:
        this.refFonts.add(value);
        break;
      case REFERENCE:
        this.references.add(value);
        break;
      case DC_LANGUAGE:
        this.language = value;
        break;
      case DC_TITLE:
        this.titles.add(value);
        break;
      case DC_CREATOR:
        this.creators.add(value);
        break;
      case DC_CONTRIBUTOR:
        this.contributors.add(value);
        break;
      case DC_PUBLISHER:
        this.publisher = value;
        break;
      case DC_RIGHTS:
        this.rights.add(value);
        break;
      case DC_DATE:
        this.date = value;
        break;
      case UNIQUE_IDENT:
        this.identifier = value;
        break;

      case HAS_SIGNATURES:
        this.hasSignatures = true;
        break;
      case HAS_ENCRYPTION:
        this.hasEncryption = true;
        break;
      case HAS_FIXED_LAYOUT:
        this.hasFixedLayout = true;
        break;
      case HAS_SCRIPTS:
        this.hasScripts = true;
        break;
    }
  }

  private String getNameFromPath(String path)
  {
    if (path == null || path.length() == 0)
    {
      return null;
    }
    int lastSlash = path.lastIndexOf('/');
    if (lastSlash == -1)
    {
      return path;
    }
    else
    {
      return path.substring(lastSlash + 1);
    }
  }

  public int generate()
  {
    // Quick and dirty XML generation...
    int returnCode = 1;
    out = null;
    int ident = 0;

    try
    {
      out = new PrintWriter(outputFile, "UTF-8");
      out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
      String epubCheckDate = "2012-10-31";
      output(ident++,
          "<jhove xmlns=\"http://hul.harvard.edu/ois/xml/ns/jhove\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
              // " xsi:schemaLocation=\"http://hul.harvard.edu/ois/xml/ns/jhove jhove.xsd\"" + 
              " name=\"" + epubCheckName + "\" release=\"" + epubCheckVersion +
              "\" date=\"" + epubCheckDate + "\">");
      generateElement(ident, "date", fromTime(System.currentTimeMillis()));
            output(ident++, "<repInfo uri=\"" + encodeContent(getNameFromPath(getEpubFileName())) + "\">");
      generateElement(ident, "created", creationDate);
      generateElement(ident, "lastModified", lastModifiedDate);
      if (formatName == null)
      {
        generateElement(ident, "format", "application/octet-stream");
      }
      else
      {
        generateElement(ident, "format", formatName); //application/epub+zip
      }
      generateElement(ident, "version", formatVersion);
      String customMessageFileName = this.getCustomMessageFile();
      if (customMessageFileName != null && !customMessageFileName.isEmpty())
      {
        generateElement(ident, "customMessageFileName", customMessageFileName);
      }
      if (fatalErrors.isEmpty() && errors.isEmpty())
      {
        generateElement(ident, "status", "Well-formed");
      }
      else
      {
        generateElement(ident, "status", "Not well-formed");
      }
      if (!warns.isEmpty() || !fatalErrors.isEmpty() || !errors.isEmpty() || !hints.isEmpty())
      {
        output(ident++, "<messages>");
        for (String f : fatalErrors)
        {
          generateElement(ident, "message", "FATAL: " + encodeContent(f));
        }

        for (String w : warns)
        {
          generateElement(ident, "message", "WARN: " + encodeContent(w));
        }

        for (String e : errors)
        {
          generateElement(ident, "message", "ERROR: " + encodeContent(e));
        }

        for (String e : hints)
        {
            generateElement(ident, "message", "HINT: " + encodeContent(e));
        }
        output(--ident, "</messages>");
      }
      generateElement(ident, "mimeType", formatName);
      output(ident++, "<properties>");

      generateProperty(ident, "PageCount", pagesCount);
      generateProperty(ident, "CharacterCount", charsCount);
      generateProperty(ident, "Language", language, "String");

      output(ident++, "<property><name>Info</name><values arity=\"List\" type=\"Property\">");
      generateProperty(ident, "Identifier", identifier, "String");
      generateProperty(ident, "CreationDate", creationDate, "Date");
      generateProperty(ident, "ModDate", lastModifiedDate, "Date");

      if (!titles.isEmpty())
      {
          String[] cs = titles.toArray(new String[titles.size()]);
          generateProperty(ident, "Title", cs, "String");
      }
      if (!creators.isEmpty())
      {
        String[] cs = new String[0];
        generateProperty(ident, "Creator", cs, "String");
      }
      if (!contributors.isEmpty())
      {
        String[] cs = new String[0];
        generateProperty(ident, "Contributor", cs, "String");
      }
      generateProperty(ident, "Date", date, "String");
      generateProperty(ident, "Publisher", publisher, "String");
      if (!rights.isEmpty())
      {
        String[] cs = new String[0];
        generateProperty(ident, "Rights", cs, "String");
      }
      output(--ident, "</values></property>");

      if (!embeddedFonts.isEmpty() || !refFonts.isEmpty())
      {
        output(ident++, "<property><name>Fonts</name><values arity=\"List\" type=\"Property\">");

        for (String f : embeddedFonts)
        {
          output(ident++, "<property><name>Font</name><values arity=\"List\" type=\"Property\">");
          generateProperty(ident, "FontName", encodeContent(getNameFromPath(f)), "String");
          generateProperty(ident, "FontFile", true);
          output(--ident, "</values></property>");
        }
        for (String f : refFonts)
        {
          output(ident++, "<property><name>Font</name><values arity=\"List\" type=\"Property\">");
          generateProperty(ident, "FontName", encodeContent(getNameFromPath(f)), "String");
          generateProperty(ident, "FontFile", false);
          output(--ident, "</values></property>");
        }
        output(--ident, "</values></property>");
      }

      if (!references.isEmpty())
      {
        output(ident++, "<property><name>References</name><values arity=\"List\" type=\"Property\">");
        for (String r : references)
        {
          generateProperty(ident, "Reference", encodeContent(r), "String");
        }
        output(--ident, "</values></property>");
      }

      if (hasEncryption)
      {
        generateProperty(ident, "hasEncryption", hasEncryption);
      }
      if (hasSignatures)
      {
        generateProperty(ident, "hasSignatures", hasSignatures);
      }
      if (hasAudio)
      {
        generateProperty(ident, "hasAudio", hasAudio);
      }
      if (hasVideo)
      {
        generateProperty(ident, "hasVideo", hasVideo);
      }
      if (hasFixedLayout)
      {
        generateProperty(ident, "hasFixedLayout", hasFixedLayout);
      }
      if (hasScripts)
      {
        generateProperty(ident, "hasScripts", hasScripts);
      }

      boolean withDocumentMD = false;
      if (withDocumentMD)
      {
        output(ident++, "<property><name>DocumentMDMetadata</name><values arity=\"Scalar\" type=\"Object\"><value>");
        generateDocumentMD(ident);
        output(--ident, "</value></values></property>");
      }
      output(--ident, "</properties>");
      output(--ident, "</repInfo>");
      output(--ident, "</jhove>");
      returnCode = 0;
    }
    catch (FileNotFoundException e)
    {
      System.err.println("FileNotFound error: " + e.getMessage());
      returnCode = 1;
    }
    catch (UnsupportedEncodingException e)
    {
      System.err.println("FileNotFound error: " + e.getMessage());
      returnCode = 1;
    }
    catch (IOException e)
    {
      System.err.println("IOException error: " + e.getMessage());
      returnCode = 1;
    }
    catch (Exception e)
    {
      System.err.println("Exception encountered: " + e.getMessage());
      returnCode = 1;
    }
    finally
    {
      if (out != null)
      {
        out.close();
      }
    }
    return returnCode;
  }

  private void generateDocumentMD(int ident)
  {
    output(ident++, "<docmd:document xmlns:docmd=\"http://www.fcla.edu/docmd\">");

    generateElement(ident, "docmd:PageCount", pagesCount);
    generateElement(ident, "docmd:CharacterCount", charsCount);
    generateElement(ident, "docmd:Language", language);
    for (String f : embeddedFonts)
    {
      output(ident, "<docmd:Font FontName=\"" + encodeContent(getNameFromPath(f)) + "\" isEmbedded=\"true\" />");
    }
    for (String f : refFonts)
    {
      output(ident, "<docmd:Font FontName=\"" + encodeContent(getNameFromPath(f)) + "\" isEmbedded=\"false\" />");
    }
    for (String r : references)
    {
      generateElement(ident, "docmd:Reference", encodeContent(r));
    }
    //if (hasEncryption) generateElement(ident, "docmd:Features", "hasEncryption");
    //if (hasSignatures) generateElement(ident, "docmd:Features", "hasSignatures");
    if (hasAudio)
    {
      generateElement(ident, "docmd:Features", "hasAudio");
    }
    if (hasVideo)
    {
      generateElement(ident, "docmd:Features", "hasVideo");
    }
    if (hasFixedLayout)
    {
      generateElement(ident, "docmd:Features", "hasFixedLayout");
    }
    if (hasScripts)
    {
      generateElement(ident, "docmd:Features", "hasScripts");
    }

    output(--ident, "</docmd:document>");
  }

  private void output(int ident, String value)
  {
    char[] spaces = new char[ident];
    Arrays.fill(spaces, ' ');
    out.print(spaces);
    out.println(value);
  }

  private void generateElement(int ident, String name, String value)
  {
    if (value == null || value.trim().length() == 0)
    {
      return;
    }
    StringBuilder sb = new StringBuilder();
    sb.append('<').append(name).append('>');
    sb.append(encodeContent(value.trim()));
    sb.append("</").append(name).append('>');
    output(ident, sb.toString());
  }

  private void generateElement(int ident, String name, long value)
  {
    if (value == 0)
    {
      return;
    }
    generateElement(ident, name, Long.toString(value));
  }

  private void generateProperty(int ident, String name, String[] value, String type)
  {
    if (value == null || value.length == 0)
    {
      return;
    }
    StringBuilder sb = new StringBuilder();
    sb.append("<property><name>").append(name).append("</name>");
    sb.append("<values arity=\"").append(value.length == 1 ? "Scalar" : "Array").append("\" type=\"").append(type).append("\">");
    for (String v : value)
    {
      sb.append("<value>").append(encodeContent(v)).append("</value>");
    }
    sb.append("</values></property>");
    output(ident, sb.toString());
  }

  private void generateProperty(int ident, String name, String value, String type)
  {
    if (value == null || value.trim().length() == 0)
    {
      return;
    }
    StringBuilder sb = new StringBuilder();
    sb.append("<property><name>").append(name).append("</name><values arity=\"Scalar\" type=\"").append(type).append("\">");
    sb.append("<value>").append(encodeContent(value)).append("</value>");
    sb.append("</values></property>");
    output(ident, sb.toString());
  }

  private void generateProperty(int ident, String name, long value)
  {
    if (value == 0)
    {
      return;
    }
    generateProperty(ident, name, Long.toString(value), "Long");
  }

  private void generateProperty(int ident, String name, boolean value)
  {
    generateProperty(ident, name, value ? "true" : "false", "Boolean");
  }

  /**
   * Encodes a content String in XML-clean form, converting characters
   * to entities as necessary.  The null string will be
   * converted to an empty string.
   */
  private static String encodeContent(String content)
  {
    if (content == null)
    {
      content = "";
    }
    StringBuilder buffer = new StringBuilder(content);

    int n = 0;
    while ((n = buffer.indexOf("&", n)) > -1)
    {
      buffer.insert(n + 1, "amp;");
      n += 5;
    }
    n = 0;
    while ((n = buffer.indexOf("<", n)) > -1)
    {
      buffer.replace(n, n + 1, "&lt;");
      n += 4;
    }
    n = 0;
    while ((n = buffer.indexOf(">", n)) > -1)
    {
      buffer.replace(n, n + 1, "&gt;");
      n += 4;
    }

    return buffer.toString();
  }

  /**
   * Transform time into ISO 8601 string.
   */
  private static String fromTime(final long time)
  {
    Date date = new Date(time);
        // Waiting for Java 7: SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
    String formatted = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
        .format(date);
    return formatted.substring(0, 22) + ":" + formatted.substring(22);
  }
}
