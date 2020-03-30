/*
 * *
 *  * Created by Ubique Innovation AG on 3/30/20 2:55 PM
 *  * Copyright (c) 2020 . All rights reserved.
 *  * Last modified 3/30/20 2:54 PM
 *
 */
package ch.ubique.android.starsdk.util;

import android.content.Context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Date;

public class LogHelper {

	private static LogHelper instance = null;
	private File logFile;

	private LogHelper(Context context) {
		logFile = new File(context.getCacheDir(), "logfile");
		logFile.getParentFile().mkdirs();
	}

	public static void init(Context context) {
		if (instance == null) {
			instance = new LogHelper(context);
		}
	}

	public static synchronized void append(String text) {
		instance.appendInternal(text);
	}

	public static void append(Throwable t) {
		String text = t.getMessage();
		for (StackTraceElement stackTraceElement : t.getStackTrace()) {
			text += stackTraceElement.toString();
		}
		append(text);
	}

	public static String getLog() {
		return instance.getLogInternal();
	}

	private void appendInternal(String text) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(logFile, true));
			//noinspection deprecation
			bw.write(new Date().toLocaleString() + " " + text);
			bw.newLine();
			bw.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String getLogInternal() {
		StringBuilder text = new StringBuilder();
		try {
			BufferedReader br = new BufferedReader(new FileReader(logFile));
			String line;

			while ((line = br.readLine()) != null) {
				text.append(line);
				text.append('\n');
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return text.toString();
	}

}