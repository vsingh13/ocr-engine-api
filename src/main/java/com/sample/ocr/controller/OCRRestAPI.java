package com.sample.ocr.controller;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/ocr")
public class OCRRestAPI {

	@RequestMapping(path = "/getdocument", method = RequestMethod.POST)
	public ResponseEntity sendMessageToTopic(@RequestBody String docReference) throws Exception {


		// Provide your user name and license code
		String license_code = "your license code";
		String user_name =  "your user name";

		// Extraction text with English language
		String ocrURL = "http://www.ocrwebservice.com/restservices/processDocument?gettext=true";

        // Full path to uploaded document
        String filePath = "file path";

        byte[] fileContent = Files.readAllBytes(Paths.get(filePath));

		URL url = new URL(ocrURL);

		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setRequestMethod("POST");

		connection.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString((user_name + ":" + license_code).getBytes()));

        // Specify Response format to JSON or XML (application/json or application/xml)
		connection.setRequestProperty("Content-Type", "application/json");

		connection.setRequestProperty("Content-Length", Integer.toString(fileContent.length));

		OutputStream stream = connection.getOutputStream();

		// Send POST request
		stream.write(fileContent);
		stream.close();

		int httpCode = connection.getResponseCode();

		System.out.println("HTTP Response code: " + httpCode);

		// Success request
		if (httpCode == HttpURLConnection.HTTP_OK)
		{
			// Get response stream
			String jsonResponse = GetResponseToString(connection.getInputStream());

			// Parse and print response from OCR server
			PrintOCRResponse(jsonResponse);
		}
		else if (httpCode == HttpURLConnection.HTTP_UNAUTHORIZED)
		{
			System.out.println("OCR Error Message: Unauthorizied request");
		}
		else
		{
			// Error occurred
			String jsonResponse = GetResponseToString(connection.getErrorStream());

		    JSONParser parser = new JSONParser();
		    JSONObject jsonObj = (JSONObject)parser.parse(jsonResponse);

		    // Error message
		    System.out.println("Error Message: " + jsonObj.get("ErrorMessage"));
		}

		connection.disconnect();
		return ResponseEntity.ok("success");

	}

	private static void PrintOCRResponse(String jsonResponse) throws ParseException, IOException
	{
        // Parse JSON data
	    JSONParser parser = new JSONParser();
	    JSONObject jsonObj = (JSONObject)parser.parse(jsonResponse);

	    // Get available pages
	    System.out.println("Available pages: " + jsonObj.get("AvailablePages"));

	    // get an array from the JSON object
	    JSONArray text= (JSONArray)jsonObj.get("OCRText");

        // For zonal OCR: OCRText[z][p]    z - zone, p - pages
	    for(int i=0; i<text.size(); i++){
	           System.out.println(" "+ text.get(i));
	    }

	    // Output file URL
	    String outputFileUrl = (String)jsonObj.get("OutputFileUrl");

	    // If output file URL is specified
	    if (outputFileUrl != null && !outputFileUrl.equals(""))
	    {
	    	// Download output file
	    	DownloadConvertedFile (outputFileUrl);
	    }
	}

	// Download converted output file from OCRWebService
	private static void DownloadConvertedFile(String outputFileUrl) throws IOException
	{
	    URL downloadUrl = new URL(outputFileUrl);
	    HttpURLConnection downloadConnection = (HttpURLConnection)downloadUrl.openConnection();

	    if (downloadConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {

	    	InputStream inputStream = downloadConnection.getInputStream();

	        // opens an output stream to save into file
	        FileOutputStream outputStream = new FileOutputStream("C:\\converted_file.doc");

	        int bytesRead = -1;
	        byte[] buffer = new byte[4096];
	        while ((bytesRead = inputStream.read(buffer)) != -1) {
	            outputStream.write(buffer, 0, bytesRead);
	        }

	        outputStream.close();
	        inputStream.close();
	    }

	    downloadConnection.disconnect();
	}

	private static String GetResponseToString(InputStream inputStream) throws IOException
	{
		InputStreamReader responseStream  = new InputStreamReader(inputStream);

        BufferedReader br = new BufferedReader(responseStream);
        StringBuffer strBuff = new StringBuffer();
        String s;
        while ( ( s = br.readLine() ) != null ) {
            strBuff.append(s);
        }

        return strBuff.toString();
	}

}
