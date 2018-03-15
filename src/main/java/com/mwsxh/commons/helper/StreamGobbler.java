package com.mwsxh.commons.helper;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

@Slf4j
public class StreamGobbler extends Thread {

	InputStream _is;
	String _type;
	OutputStream _os;

	public StreamGobbler(InputStream is, String type) {
		this(is, type, null);
	}

	StreamGobbler(InputStream is, String type, OutputStream redirect) {
		_is = is;
		_type = type;
		_os = redirect;
	}

	/**
	 * 打印命令行的输出结果，可以输出到指定到<class>OutputStream</class>。
	 */
	public void run() {
		try {
			PrintWriter writer = null;
			if (_os != null) {
				writer = new PrintWriter(_os);
			}

			BufferedReader reader = new BufferedReader(new InputStreamReader(_is));

			String line = null;
			while ((line = reader.readLine()) != null) {
				if (writer != null) {
					writer.println(line);
				}
				System.out.println(_type + "> " + line);
			}

			if (writer != null) {
				writer.flush();
			}
		} catch (IOException e) {
			log.error("", e);
		}
	}

}
