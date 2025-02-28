package com.logicaldoc.core.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.logicaldoc.core.AbstractCoreTCase;
import com.logicaldoc.core.security.Tenant;

public class PDFParserTest extends AbstractCoreTCase {

	private long startTime;

	private long mem1;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		super.setUp();
		this.startTime = System.currentTimeMillis();
		this.mem1 = Runtime.getRuntime().totalMemory();
		System.out.println("freeMemory: " + Runtime.getRuntime().freeMemory());
		System.out.println("totalMemory: " + Runtime.getRuntime().totalMemory());
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		long elapsedMillis = System.currentTimeMillis() - this.startTime;
		System.err.println("elapsedMillis: " + elapsedMillis);
		long mem2 = Runtime.getRuntime().totalMemory();

		System.err.println("freeMemory AFTER: " + Runtime.getRuntime().freeMemory());
		System.err.println("totalMemory AFTER: " + Runtime.getRuntime().totalMemory());

		System.err.println("Difference in memory allocation: " + ((mem2 - mem1) / 1024) + " KB");
		Runtime.getRuntime().gc(); // request garbage collection
		super.tearDown();
	}

	@Test
	public void testParse() throws UnsupportedEncodingException {

		// This is a pdf German document that PDFBox version 0.7.3 in not able
		// to read
		// The new version of PDFBox 1.3.1 is able to open such document
		// This pdf has been created with Adobe PDF Library 9.0
		String inputFile = "src/test/resources/Digital_Day.pdf";
		File file = new File(inputFile);
		String filename = file.getPath();

		Parser parser = ParserFactory.getParser(filename);
		PDFParser pdfp = (PDFParser) parser;
		// pdfp.parse(file);
		//
		// String title = pdfp.getTitle();
		// System.out.println("title: " + title);
		// assertTrue(StringUtils.isNotEmpty(title));
		// assertEquals("Folie 1", title);
		//
		// String author = pdfp.getAuthor();
		// System.out.println("author: " + author);
		// assertTrue(StringUtils.isNotEmpty(author));
		// assertEquals("Marcus Joost", author);
		//
		// String content = pdfp.getContent();
		// assertNotNull(content);
		// assertTrue(StringUtils.isNotEmpty(content));
		//
		// System.out.println("content.length(): " + content.length());
		// assertEquals(27179, content.length());

		inputFile = "src/test/resources/probiotic-1.4.pdf";
		file = new File(inputFile);
		filename = file.getPath();

		parser = ParserFactory.getParser(filename);
		pdfp = (PDFParser) parser;
		String content = pdfp.parse(file, filename, null, Locale.ENGLISH, Tenant.DEFAULT_NAME);
		Assert.assertTrue(content.contains("adequate"));
	}

	@Test
	public void testSmall() throws UnsupportedEncodingException {
		String inputFile = "src/test/resources/small.pdf";
		File file = new File(inputFile);
		String filename = file.getPath();

		for (int i = 0; i < 300; i++) {
			Parser parser = ParserFactory.getParser(filename);
			PDFParser pdfp = (PDFParser) parser;
			String content = pdfp.parse(file, filename, null, Locale.ENGLISH, Tenant.DEFAULT_NAME);

			assertTrue(content.startsWith("1: prova"));
		}
	}

	@Test
	public void testStress() throws UnsupportedEncodingException {
		File file1 = new File("src/test/resources/Digital_Day.pdf");
		String filename1 = file1.getPath();
		File file2 = new File("src/test/resources/Arabic/SharePoint.pdf");
		String filename2 = file2.getPath();

		for (int i = 0; i < 10; i++) {
			Parser parser = null;
			if (i % 2 == 0)
				parser = ParserFactory.getParser(filename1);
			else
				parser = ParserFactory.getParser(filename2);

			PDFParser pdfp = (PDFParser) parser;
			if (i % 2 == 0) {
				String content = pdfp.parse(file1, filename1, null, Locale.ENGLISH, Tenant.DEFAULT_NAME);
				assertEquals(28388, content.length());
			} else {
				String content = pdfp.parse(file2, filename2, null, Locale.ENGLISH, Tenant.DEFAULT_NAME);
				assertEquals(8759, content.length());
			}
		}
	}

	@Test
	public void testParseArabic() throws UnsupportedEncodingException {

		// This is a pdf document with two (2) columns, one english and one
		// Arabic on the right
		// The text in the left column is left aligned, while the text in the
		// right goes from right to left (Arabic)
		// The documentation of PDFBox 1.4.0 states that this requires ICU4J 3.8
		String inputFile = "src/test/resources/Arabic/imaging14.pdf";
		String outputFile = "src/test/resources/Arabic/UTF-8.txt";
		File file = new File(inputFile);
		String filename = file.getPath();

		Parser parser = ParserFactory.getParser(filename);
		PDFParser pdfp = (PDFParser) parser;
		String content = pdfp.parse(file, filename, null, Locale.ENGLISH, Tenant.DEFAULT_NAME);

		assertNotNull(content);
		assertTrue(StringUtils.isNotEmpty(content));

		System.err.println("content.length(): " + content.length());
		assertEquals(3034, content.length());

		try {
			FileOutputStream out = new FileOutputStream(outputFile);
			BufferedWriter BW = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
			BW.write(content);
			BW.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testParseArabic2() throws UnsupportedEncodingException {

		// This is an Arabic pdf document
		// The text goes from right to left (Arabic)
		// The documentation of PDFBox 1.4.0 states that this requires ICU4J 3.8
		String inputFile = "src/test/resources/Arabic/SharePoint.pdf";
		File file = new File(inputFile);
		String filename = file.getPath();

		Parser parser = ParserFactory.getParser(filename);
		PDFParser pdfp = (PDFParser) parser;
		String content = pdfp.parse(file, filename, null, Locale.ENGLISH, Tenant.DEFAULT_NAME);

		assertNotNull(content);
		assertTrue(StringUtils.isNotEmpty(content));

		System.err.println("content.length(): " + content.length());
		assertEquals(8759, content.length());
	}

	@Test
	public void testForm() throws UnsupportedEncodingException {
		String inputFile = "src/test/reaources/pdf_form_fields.pdf";
		File file = new File(inputFile);
		String filename = file.getPath();

		Parser parser = ParserFactory.getParser(filename);
		PDFParser pdfp = (PDFParser) parser;
		pdfp.parse(file, filename, null, Locale.ENGLISH, Tenant.DEFAULT_NAME);

		inputFile = "src/test/resources/fillablePDF1.pdf";
		file = new File(inputFile);
		filename = file.getPath();

		parser = ParserFactory.getParser(filename);
		pdfp = (PDFParser) parser;
		pdfp.parse(file, filename, null, Locale.ENGLISH, Tenant.DEFAULT_NAME);
	}
}
