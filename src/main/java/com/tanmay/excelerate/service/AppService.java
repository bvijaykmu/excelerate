package com.tanmay.excelerate.service;

import java.io.File;
import java.util.Date;
import java.util.List;

import com.tanmay.excelerate.dao.AppDao;
import com.tanmay.excelerate.entity.ReportManager;

/**
 * @author : tanmay
 * @created : 19-Jun-2017
 */
public class AppService {
	AppDao dao;
	private static final long D_HOURS = 24l;
	private static final long W_HOURS = 168l;
	private static final long M_HOURS = 744l;
	private static final String DAILY = "d";
	private static final String WEEKLY = "w";
	private static final String MONTHLY = "m";
	private static final String ASTERISK = "*";

	public AppService() {
		dao = new AppDao();
	}

	@SuppressWarnings("unchecked")
	public void generateReport() {
		List<ReportManager> allReports = dao.fetchAllReport();
		for (ReportManager report : allReports) {
			/*
			 * Check the difference between current time and last generation time
			 * #d -> generate when diff>24 hours and type is d
			 * #w -> generate when diff>168 hours and type is w
			 * #m -> generate when diff> 744 hours and type is m
			 * 
			 * check if directory exists
			 * #if not exists-> create
			 * #if exists do nothing
			 * 
			 * create and write csv
			 */
			if (eligibleForGeneration(report)) {
				if (!checkDirectoryPresence(report))
					continue;
			} else {
				continue;
			}
			String[] columnHeaders = extractHeadersFromQuery(report.getQuery());
			printArray(columnHeaders);
			//ExcelUtils.createWorkbook(report);
		}
	}

	private void printArray(String[] columnHeaders) {
		for (int i = 0; i < columnHeaders.length; i++) {
			System.out.println(columnHeaders[i]);
		}

	}

	private String[] extractHeadersFromQuery(String query) {
		String betweenSelectAndFrom = query.substring(6, query.lastIndexOf("from")).trim();
		String betweenFromAndWhere = "";
		if (query.lastIndexOf("where") == -1) {
			betweenFromAndWhere = query.substring(query.lastIndexOf("from") + 4).trim();
		} else {
			betweenFromAndWhere = query.substring(query.lastIndexOf("from") + 4, query.lastIndexOf("where")).trim();
		}
		String[] headers = betweenSelectAndFrom.split(",");

		//Check if * or joined table
		headers = manageAsterisk(headers, betweenSelectAndFrom, betweenFromAndWhere);
		for (int i = 0; i < headers.length; i++) {
			if (headers[i].contains(" as ") || headers[i].contains(" "))
				headers[i] = headers[i].substring(headers[i].lastIndexOf(" ")).trim();
			headers[i]=formatColumn(headers[i]);
		}
		return headers;
	}

	private String formatColumn(String column) {
		column=column.replaceAll("_", " ").toUpperCase();
		return column;
	}

	private String[] manageAsterisk(String[] headers, String betweenSelectAndFrom, String betweenFromAndWhere) {
		if (headers[0].equals(ASTERISK)) {
			if (betweenFromAndWhere.indexOf(" ") == -1 && betweenFromAndWhere.indexOf(" as ") == -1) {
				headers = dao.getColumnNames(betweenFromAndWhere.trim());
			} else if (betweenFromAndWhere.indexOf(" ") > -1) {
				headers = dao.getColumnNames(betweenFromAndWhere.substring(0, betweenFromAndWhere.indexOf(" ")).trim());
			} else
				headers = dao.getColumnNames(betweenFromAndWhere.substring(0, betweenFromAndWhere.indexOf(" as ")).trim());
		}
		return headers;
	}

	private boolean checkDirectoryPresence(ReportManager report) {
		File f = new File(report.getDownloadLocation());
		if (!f.exists()) {
			if (f.mkdir())
				System.out.println("DIRECTORY CREATED");
			else {
				dao.logFailure(report.getReportId(), "Error creating Directory :" + report.getDownloadLocation());
				return false;
			}
		}
		return true;
	}

	private boolean eligibleForGeneration(ReportManager report) {
		Boolean isEligible = Boolean.FALSE;
		if (null == report.getLastGeneratedOn())
			return true;
		long diffHours = (new Date().getTime() - report.getLastGeneratedOn().getTime()) / (60 * 60 * 1000);
		if (report.getType().equals(DAILY) && diffHours >= D_HOURS) {
			isEligible = true;
		}
		if (report.getType().equals(WEEKLY) && diffHours >= W_HOURS) {
			isEligible = true;
		}
		if (report.getType().equals(MONTHLY) && diffHours >= M_HOURS) {
			isEligible = true;
		}
		return isEligible;
	}
}
