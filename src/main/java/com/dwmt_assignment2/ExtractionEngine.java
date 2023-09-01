package com.dwmt_assignment2;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.HttpURLConnection;
import java.net.URL;

import java.util.Scanner;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class ExtractionEngine {

    /*
     * This method fetches all the data from the API and then passes it onto the
     * Data Processing Engine.
     */
    public static void getRecordsFromAPI() {

        List<String> keywords = new ArrayList<String>();
        keywords.add("canada");
        keywords.add("university");
        keywords.add("dalhousie");
        keywords.add("halifax");
        keywords.add("canada education");
        keywords.add("moncton");
        keywords.add("hockey");
        keywords.add("fredericton");
        keywords.add("celebration");

        Map<String, String> envDetails = getAPIInfoFromEnv();
        String apiKey = envDetails.get("API_KEY");
        String apiType = envDetails.get("API_TYPE");
        String apiPrefix = "https://newsapi.org/v2/";
        String apiKeywordParameter = "?q=";
        String apiSuffix = "&apiKey=";

        try {
            if (apiKey == null || apiType == null) {
                System.out.println("Please specify API Key and API Type in `.env` file.");
                System.exit(0);
            } else {
                Map<String, String> articlesKeyword = new LinkedHashMap<>();
                for (int i = 0; i < keywords.size(); i++) {
                    String apiURL = new String();
                    apiURL = apiURL.concat(apiPrefix).concat(apiType).concat(apiKeywordParameter)
                            .concat(keywords.get(i))
                            .concat(apiSuffix).concat(apiKey);
                    URL url = new URL(apiURL);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.connect();

                    String line = new String();
                    Scanner sc = new Scanner(url.openStream());
                    while (sc.hasNextLine()) {
                        line = line.concat(sc.nextLine());
                    }
                    sc.close();
                    connection.disconnect();
                    articlesKeyword.put(keywords.get(i), line);
                }
                System.out.println("---------- News data retrieved from the newsAPI.org ----------");
                DataProcessingEngine.extractDataInFile(articlesKeyword);
            }
        } catch (Exception e) {
            System.out.println("Error occurred while writing retrieving data from API.");
        }

    }

    /**
     * This method reads the .env file and returns the type of API call that needs
     * to be made. For eg.: `everything` or `top-headlines`.
     * 
     * @return String - the type of API call that needs to be made
     */
    public static Map<String, String> getAPIInfoFromEnv() {
        Map<String, String> envMap = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader("./.env"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] keyValuePairs = line.split("=");
                if (keyValuePairs.length == 2) {
                    envMap.put(keyValuePairs[0], keyValuePairs[1]);
                }
            }
            if (!envMap.containsKey("API_TYPE")) {
                envMap.put("API_TYPE", "everything");
            }
        } catch (Exception e) {
            System.out.println("Error occurred while getting API details from `.env` file.");
        }

        return envMap;
    }
}
