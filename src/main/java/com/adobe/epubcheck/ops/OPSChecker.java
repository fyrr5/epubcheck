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

package com.adobe.epubcheck.ops;

import com.adobe.epubcheck.api.Report;
import com.adobe.epubcheck.messages.MessageId;
import com.adobe.epubcheck.messages.MessageLocation;
import com.adobe.epubcheck.ocf.OCFPackage;
import com.adobe.epubcheck.opf.ContentChecker;
import com.adobe.epubcheck.opf.DocumentValidator;
import com.adobe.epubcheck.opf.XRefChecker;
import com.adobe.epubcheck.util.EPUBVersion;
import com.adobe.epubcheck.util.GenericResourceProvider;
import com.adobe.epubcheck.util.OPSType;
import com.adobe.epubcheck.xml.XMLParser;
import com.adobe.epubcheck.xml.XMLValidator;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class OPSChecker implements ContentChecker, DocumentValidator
{

  class EpubValidator
  {
    XMLValidator xmlValidator = null;
    XMLValidator schValidator = null;

    public EpubValidator(XMLValidator xmlValidator,
        XMLValidator schValidator)
    {
      this.xmlValidator = xmlValidator;
      this.schValidator = schValidator;
    }
  }

  private OCFPackage ocf;

  private final Report report;

  private final String path;

  private final String mimeType;

  private XRefChecker xrefChecker;

  private final EPUBVersion version;


  private final GenericResourceProvider resourceProvider;

  private final String properties;

  private static final XMLValidator xhtmlValidator_20_NVDL = new XMLValidator(
      "schema/20/rng/ops20.nvdl");
  private static final XMLValidator svgValidator_20_RNG = new XMLValidator(
      "schema/20/rng/svg11.rng");

  private static final XMLValidator xhtmlValidator_30_RNC = new XMLValidator(
      "schema/30/epub-xhtml-30.rnc");
  private static final XMLValidator svgValidator_30_RNC = new XMLValidator(
      "schema/30/epub-svg-30.rnc");

  private static final XMLValidator xhtmlValidator_30_ISOSCH = new XMLValidator(
      "schema/30/epub-xhtml-30.sch");
  private static final XMLValidator svgValidator_30_ISOSCH = new XMLValidator(
      "schema/30/epub-svg-30.sch");
  private static final XMLValidator idUniqueValidator_20_ISOSCH = new XMLValidator(
      "schema/20/sch/id-unique.sch");


  private HashMap<OPSType, EpubValidator> epubValidatorMap;

  private void initEpubValidatorMap()
  {
    HashMap<OPSType, EpubValidator> map = new HashMap<OPSType, EpubValidator>();
    map.put(new OPSType("application/xhtml+xml", EPUBVersion.VERSION_2),
        new EpubValidator(xhtmlValidator_20_NVDL, idUniqueValidator_20_ISOSCH));
    map.put(new OPSType("application/xhtml+xml", EPUBVersion.VERSION_3),
        new EpubValidator(xhtmlValidator_30_RNC,
            xhtmlValidator_30_ISOSCH));

    map.put(new OPSType("image/svg+xml", EPUBVersion.VERSION_2),
        new EpubValidator(svgValidator_20_RNG, idUniqueValidator_20_ISOSCH));
    map.put(new OPSType("image/svg+xml", EPUBVersion.VERSION_3),
        new EpubValidator(svgValidator_30_RNC, svgValidator_30_ISOSCH));

    epubValidatorMap = map;
  }

  public OPSChecker(OCFPackage ocf, Report report, String path,
      String mimeType, String properties, XRefChecker xrefChecker,
      EPUBVersion version)
  {
    initEpubValidatorMap();
    this.ocf = ocf;
    this.resourceProvider = ocf;
    this.report = report;
    this.path = path;
    this.xrefChecker = xrefChecker;
    this.mimeType = mimeType;
    this.version = version;
    this.properties = properties;
  }

  public OPSChecker(String path, String mimeType,
      GenericResourceProvider resourceProvider, Report report,
      EPUBVersion version)
  {
    initEpubValidatorMap();
    this.resourceProvider = resourceProvider;
    this.mimeType = mimeType;
    this.report = report;
    this.path = path;
    this.version = version;
    this.properties = "singleFileValidation";
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
      validate();
    }
  }

  public boolean validate()
  {
    XMLValidator rngValidator = null;
    XMLValidator schValidator = null;
    int fatalErrorsSoFar = report.getFatalErrorCount();
    int errorsSoFar = report.getErrorCount();
    int warningsSoFar = report.getWarningCount();
    OPSType type = new OPSType(mimeType, version);
    EpubValidator epubValidator = epubValidatorMap
        .get(type);
    if (epubValidator != null)
    {
      rngValidator = epubValidator.xmlValidator;
      schValidator = epubValidator.schValidator;
    }
    try
    {
      validateAgainstSchemas(rngValidator, schValidator);
    }
    catch (IOException e)
    {
      report.message(MessageId.PKG_008, new MessageLocation(path, 0, 0), path);
    }
    return fatalErrorsSoFar == report.getFatalErrorCount()
        && errorsSoFar == report.getErrorCount()
        && warningsSoFar == report.getWarningCount();
  }

  void validateAgainstSchemas(XMLValidator rngValidator,
      XMLValidator schValidator) throws
      IOException
  {
    InputStream in = null;
    OPSHandler opsHandler;
    try
    {
      in = resourceProvider.getInputStream(path);
      XMLParser opsParser = new XMLParser( ocf,
          in, path, mimeType, report,
          version);

      if (version == EPUBVersion.VERSION_2)
      {
        opsHandler = new OPSHandler(ocf, path, xrefChecker, opsParser, report, version);
      }
      else
      {
        opsHandler = new OPSHandler30(ocf, path, mimeType, properties,
            xrefChecker, opsParser, report, version);
      }

      opsParser.addXMLHandler(opsHandler);

      if (rngValidator != null)
      {
        opsParser.addValidator(rngValidator);
      }

      if (schValidator != null)
      {
        opsParser.addValidator(schValidator);
      }

      opsParser.process();
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
      catch (Exception ignored)
      {

      }
    }

  }
}
