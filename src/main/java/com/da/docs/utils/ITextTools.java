/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-03-25 22:14:57                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2025-06-18 22:52:45                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.io.image.ImageType;
import com.itextpdf.io.image.TiffImageData;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.EncryptionConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfDocumentInfo;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.WriterProperties;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.VerticalAlignment;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class ITextTools {

  public static boolean addWatermark(
      String fileExtension,
      InputStream in,
      OutputStream os,
      String watermarkText,
      int angle,
      float opacity) {

    PdfReader reader = null;
    ByteArrayOutputStream bos = null; // for image convert

    try {

      // if not pdf, convert to pdf, then create pdf reader
      if (!fileExtension.equals("pdf")) {
        bos = new ByteArrayOutputStream();
        boolean result = imgToPdf(in, bos);

        if (result == false) {
          return false;
        } else {
          reader = new PdfReader(new ByteArrayInputStream(bos.toByteArray()));
        }

      } else {
        reader = new PdfReader(in);
      }

      // set password
      WriterProperties properties = new WriterProperties()
          .setStandardEncryption(
              "".getBytes(), // user password
              "".getBytes(), // owner password
              EncryptionConstants.ALLOW_PRINTING,
              EncryptionConstants.ENCRYPTION_AES_256);

      PdfWriter writer = new PdfWriter(os, properties);
      PdfDocument pdf = new PdfDocument(reader, writer);

      if (reader.isEncrypted()) {
        pdf.close();
        return true;
      }

      PdfDocumentInfo info = pdf.getDocumentInfo();
      info.setCreator("Docs Server");

      PdfFont font = PdfFontFactory.createFont();
      Paragraph paragraph = new Paragraph(watermarkText).setFont(font);

      // these 2 parameters can be changed, but it's the best practice value
      int longN = 2;
      int shortN = 2;

      PdfExtGState gs = new PdfExtGState();
      gs.setFillOpacity(opacity);

      for (int i = 1; i <= pdf.getNumberOfPages(); i++) {
        PdfPage page = pdf.getPage(i);

        Rectangle pageRect = page.getPageSize();
        float pageHeight = pageRect.getHeight();
        float pageWidth = pageRect.getWidth();
        boolean isLandscape = pageWidth > pageHeight;
        int heightGap = isLandscape ? (int) pageHeight / shortN : (int) pageHeight / longN;
        int widthGap = isLandscape ? (int) pageWidth / longN : (int) pageWidth / shortN;

        PdfCanvas pdfCanvas = new PdfCanvas(pdf.getPage(i));
        pdfCanvas.setFillColor(ColorConstants.BLACK);
        pdfCanvas.setExtGState(gs);

        // set font size by the page size
        float fontSize = isLandscape ? pageHeight / 30 : pageWidth / 30;
        paragraph.setFontSize(fontSize);
        for (int height = 0; height < pageRect.getHeight(); height = height + heightGap) {
          for (int width = 0; width < pageRect.getWidth(); width = width + widthGap) {
            Canvas canvas = new Canvas(pdfCanvas, pageRect);
            canvas.showTextAligned(
                paragraph,
                width, height, i,
                TextAlignment.LEFT,
                VerticalAlignment.BOTTOM,
                (float) Math.toRadians(angle));
            canvas.close();
          }
        }
      }

      // close pdf
      pdf.close();
      log.trace("Add watermark success");
      return true;
    } catch (Exception e) {
      log.error("Add water marker failed: {}", e);
      return false;
    }
  }

  public static boolean imgToPdf(InputStream is, OutputStream os) {
    try {
      PdfWriter writer = new PdfWriter(os);
      PdfDocument pdf = new PdfDocument(writer);

      byte[] imageBytes = is.readAllBytes();
      ImageData imageData = ImageDataFactory.create(imageBytes, true);
      ImageType imageType = imageData.getOriginalType();
      if (!ImageDataFactory.isSupportedType(imageType)) {
        log.warn("image type is not supported");
        return false;
      }

      if (imageType == ImageType.TIFF) {
        int pageNum = TiffImageData.getNumberOfPages(imageBytes);
        log.trace("Tiff page num: {}", pageNum);

        Document document = null;
        for (int i = 1; i <= pageNum; i++) {

          // direct must be false
          Image image = new Image(ImageDataFactory.createTiff(imageBytes, true, i, false));
          float imageWidth = image.getImageWidth();
          float imageHeight = image.getImageHeight();

          PageSize pageSize = new PageSize(imageWidth, imageHeight);
          pdf.setDefaultPageSize(pageSize);

          document = new Document(pdf);
          document.setMargins(0, 0, 0, 0);

          document.add(image);
        }
        if (document != null) {
          document.close();
        }

      } else {
        Image image = new Image(ImageDataFactory.create(imageBytes));
        float imageWidth = image.getImageWidth();
        float imageHeight = image.getImageHeight();

        PageSize pageSize = new PageSize(imageWidth, imageHeight);
        pdf.setDefaultPageSize(pageSize);
        Document document = new Document(pdf);
        document.setMargins(0, 0, 0, 0);

        document.add(image);
        document.close();
      }

      return true;
    } catch (Exception e) {
      log.error("Convert to pdf failed: {}", e);
      return false;
    }
  }
}
