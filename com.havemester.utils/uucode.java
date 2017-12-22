package com.havemester.utils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.CRC32;
import java.util.zip.Checksum;


public class uucode {
	final static char uuOffset = ' ';
	final static int  uuLength = 45;
//	final static char uuOffset = ' ' + 1;
//	final static int  uuLength = 60;

	
	public static void main(String[] args) {
		System.out.println("UUENCODE Test");
		args = new String[] {"Test\\test.txt"};

		if (args.length > 0) {
			for (String fileName: args) {
				uuencode (fileName.replace('\\', '/'));
			}
		}
		
		System.out.println("\nUUDECODE Test");
		args = new String[] { "Test\\test.uue" };

		if (args.length > 0) {
			for (String fileName: args) {
				uudecode (fileName.replace('\\', '/'));
			}
		}
	}
	
	
	private static void uuencode(String fileName) {
		BufferedWriter out = null;
		
		try {
			// get file size
			File file    = new File(fileName);
			int fileSize = (int) file.length();
			
			System.out.println("Reading file:   " + fileName);
			System.out.println("Size:           " + fileSize);

			@SuppressWarnings("resource")
			InputStream is = new FileInputStream(fileName);
			
			// round fileSize to multiple of 3
			byte[] encoded = new byte[((fileSize / 3) * 3) + 3];

			// clear last 2 bytes of buffer
			encoded[encoded.length - 1] = 0;
			encoded[encoded.length - 2] = 0;
			
			if (is.read(encoded) != fileSize) {
				throw new IOException("I/O Error - file incomplete ");
			}
			
			Checksum checksum = new CRC32();
			checksum.update(encoded, 0, fileSize);
			System.out.format("Checksum (CRC): %05X\n", checksum.getValue());

			String directory = file.getParent().replace('\\', '/');
			String fName     = file.getName();
			
			int    dot        = fName.lastIndexOf('.');
			String base       = (dot == -1) ? fName : fName.substring(0, dot);
			String extension  = (dot == -1) ? "" : fName.substring(dot + 1);
			String outputName = directory + "/" + base + ".uue"; 
					
			out = new BufferedWriter(new FileWriter(outputName));
			
			out.write("begin 644 " + base + "-decode." + extension + "\n");
			System.out.println("Encoded file:   " + outputName);
			
			for (int offset = 0; offset < fileSize; offset += uuLength) {
				final String sLine = encodeLine(encoded, offset, fileSize);
				out.write(sLine + "\n");
			}

			out.write((char) (uuOffset + 64) + "\n");
			out.write("end\n");
		}
		catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (out != null) {
					out.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
	
	private static void uudecode(String fileName) {
		OutputStream os = null;
		BufferedOutputStream bos = null;

		try {
			int	lineNo = 0;
			int offset = 0;

			// get file size
			File file     = new File(fileName);
			int  fileSize = (int) file.length();

			System.out.println("Reading file:   " + fileName);
			System.out.println("Size:           " + fileSize);

			final byte[] decoded = new byte[(fileSize * 3) / 4];

			String directory = file.getParent();

			
			@SuppressWarnings("resource")
			BufferedReader in = new BufferedReader(new FileReader(fileName));
			
			String sLine;
			
			do {
				sLine = in.readLine();
				lineNo++;
			} while (!sLine.substring(0,5).equals("begin"));

			String[] begin = sLine.split(" ");

			System.out.println("Decoding file:  " + directory + "/" + begin[2]);

			
			os = new FileOutputStream(directory + "/" + begin[2]);
			bos = new BufferedOutputStream(os);
			
			do {
				sLine = in.readLine();
				lineNo++;

				if ((sLine.length() == 3) && (sLine.substring(0,3).equals("end"))) {
					break;
				}

				byte[] line = sLine.getBytes(); 
				int len = (line[0] - uuOffset) & 0x3f;

				int minLen = (len * 4 / 3) + 1;
				int maxLen = ((len + 2) * 4 / 3) + 1;
				
				if ((sLine.length() >= minLen) && (maxLen <= minLen)) {
					throw new IOException("Line size(" + sLine.length() + ") incorrect in line " + lineNo + "\n" + sLine);
				}

				decodeLine(decoded, offset, len, line);

				offset += len;
			} while (true);

			bos.write(decoded, 0, offset);

			Checksum checksum = new CRC32();
			checksum.update(decoded, 0, offset);
			
			System.out.println("Size:           " + offset);
			System.out.format("Checksum (CRC): %05X\n", checksum.getValue());
		}
		catch (IOException e) {
			System.err.println(e);
		}
		finally {
			try {
				if (os != null) {
					bos.close();
					os.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
	private static String encodeLine(byte[] r, int offset, int fileSize) throws IOException {
		int len = Integer.min(uuLength, fileSize - offset);
		final char c[] = new char[((len + 2) / 3) * 4 + 1];
		int cCount = 0;
		
		c[cCount++] = (char) (len + uuOffset);
		
		for (int i = 1; i <= len; i += 3) {
			byte b0 = r[offset++];
			byte b1 = r[offset++];
			byte b2 = r[offset++];

			c[cCount++] = (char) (((b0 >> 2) & 0x3f) + uuOffset);
			c[cCount++] = (char) (((b0 << 4) & 0x30) + ((b1 >> 4) & 0x0f) + uuOffset);
			c[cCount++] = (char) (((b1 << 2) & 0x3c) + ((b2 >> 6) & 0x03) + uuOffset);
			c[cCount++] = (char) ((b2 & 0x03f) + uuOffset);
		}
		
		for (int i= 0; i < c.length; i++) {
			if (c[i] == ' ') {
				c[i] = '`';
			}
		}
		
		return new String(c);
	}
	
	
	private static byte[]  decodeLine(byte[] r, int offset, int length, byte b[]) throws IOException {
		int rCount = offset;
		
		for (int i = 1; i < ((length * 4) / 3);) {
			int c0 = ((b[i++] - uuOffset) & 0x3f);
			int c1 = ((b[i++] - uuOffset) & 0x3f);
			int c2 = ((b[i++] - uuOffset) & 0x3f);
			int c3 = ((b[i++] - uuOffset) & 0x3f);
		
			r[rCount++] = (byte) ((((c0 << 2) & 0xfc) | ((c1 >>> 4) & 3)) & 0xff);
			r[rCount++] = (byte) ((((c1 << 4) & 0xf0) | ((c2 >>> 2) & 0xf)) & 0xff);
			r[rCount++] = (byte) ((((c2 << 6) & 0xc0) | (c3 & 0x3f)) & 0xff);
		}
		
		return(r);
	}
}
