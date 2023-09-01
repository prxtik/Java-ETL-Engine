package com.dwmt_assignment2;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataProcessingEngine {

    /**
     * This method extracts the relevant information from the data and then stores
     * it into the files.
     * 
     * @param fetchedData - data received from the API
     */
    public static void extractDataInFile(Map<String, String> fetchedData) {
        String strPattern = "\"[^\"]*\"";
        Pattern pattern = Pattern.compile(strPattern);
        Map<String, List<Map<Integer, List<String>>>> extractedMap = new LinkedHashMap<>();

        try {
            if (!fetchedData.isEmpty()) {
                System.out.println("Overview of data retrieved:");
                for (Map.Entry<String, String> pair : fetchedData.entrySet()) {
                    if (pair.getValue().contains("articles")) {
                        String splitAfterArticles = pair.getValue().split("articles\":\\[")[1];
                        if (!splitAfterArticles.isEmpty() && !splitAfterArticles.equals("]}")) {
                            String articles = splitAfterArticles.substring(0,
                                    splitAfterArticles.length() - 3);
                            if (!articles.isEmpty()) {
                                String[] articleList = articles.split("\\},\\{");
                                List<Map<Integer, List<String>>> filteredArticles = new ArrayList<>();

                                for (int i = 0; i < articleList.length; i++) {
                                    List<String> tempInnerList = new ArrayList<>();
                                    Map<Integer, List<String>> tempOuterMap = new LinkedHashMap<>();
                                    String element = articleList[i];
                                    String title = new String();
                                    String content = new String();

                                    String titleWOCheck = element.split("title\":")[1].split(",\"description")[0]
                                            .trim();
                                    Matcher matcher = pattern.matcher(titleWOCheck);

                                    if (matcher.find()) {
                                        title = titleWOCheck.substring(1, titleWOCheck.length() - 1);
                                    } else {
                                        title = titleWOCheck;
                                    }

                                    String contentWOCheck = element.split("content\":")[1].trim();
                                    matcher = pattern.matcher(contentWOCheck);

                                    if (matcher.find()) {
                                        content = contentWOCheck.substring(1, contentWOCheck.length() - 1);
                                    } else {
                                        content = contentWOCheck;
                                    }

                                    tempInnerList.add(title);
                                    tempInnerList.add(content);
                                    tempOuterMap.put(i, tempInnerList);
                                    filteredArticles.add(tempOuterMap);
                                }
                                extractedMap.put(pair.getKey(), filteredArticles);

                                System.out.println(
                                        "Keyword:    " + pair.getKey());
                                System.out.println("Articles count:    "
                                        + filteredArticles.size());
                                System.out.println("----------------------------------------");

                            }
                        }
                    }
                }
            } else {
                System.out.println("Data passed to Data Processing Engine is empty. Cannot proceed further.");
                System.exit(0);
            }
            writeToFile(extractedMap);
            TransformationEngine.readFilesAndUploadData();
        } catch (Exception e) {
            System.out.println("Error occurred while processing data.");
        }
    }

    /**
     * This method writes the filtered data into the files.
     * 
     * @param extractedMap - map of title and content for each article for each
     *                     keyword
     */
    public static void writeToFile(Map<String, List<Map<Integer, List<String>>>> extractedMap) {
        try {
            int batchSize = 5;
            File fileOuterDir = new File("./newsAPIData");
            if (fileOuterDir.exists()) {
                Boolean success = deleteDirectory(fileOuterDir);
                if (success)
                    fileOuterDir = new File("./newsAPIData");
            }
            if (fileOuterDir.mkdir() == true) {
                for (Map.Entry<String, List<Map<Integer, List<String>>>> pair : extractedMap.entrySet()) {
                    Double totalFiles = Math.ceil(pair.getValue().size() / 5) + 1;
                    int articlesProcessed = 0;

                    List<Map<Integer, List<String>>> tempList = pair.getValue();
                    File fileDir = new File("./newsAPIData/" + pair.getKey());
                    if (fileDir.mkdir() == true) {
                        for (int j = 0; j < totalFiles; j++) {
                            File file = new File(
                                    "./newsAPIData/" + pair.getKey() + "/" + pair.getKey() + "_" + (j + 1) + ".json");
                            FileWriter fileWriter = new FileWriter(file);
                            String writeToFile = new String();
                            writeToFile = writeToFile.concat("[");
                            for (int i = articlesProcessed; i < articlesProcessed + batchSize; i++) {
                                if (i < tempList.size()) {
                                    Map<Integer, List<String>> tempOuterMap = tempList.get(i);
                                    List<String> tempInnnerList = tempOuterMap.get(i);
                                    writeToFile = writeToFile.concat("{\"title\":\"" + tempInnnerList.get(0) + "\",");
                                    writeToFile = writeToFile.concat("\"content\":\"" + tempInnnerList.get(1) + "\"}");
                                    writeToFile = writeToFile.concat(",");
                                } else {
                                    break;
                                }
                            }
                            writeToFile = writeToFile.substring(0, writeToFile.length() - 1);
                            writeToFile = writeToFile.concat("]");
                            fileWriter.write(writeToFile);
                            fileWriter.close();

                            articlesProcessed = articlesProcessed + 5;
                            if (articlesProcessed == tempList.size()) {
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error occurred while writing data to files.");
            e.printStackTrace();
        }
    }

    /**
     * This method will delete the directory containing all sub-directories which
     * contains the files if they are already present.
     * 
     * @param directory - parent directory
     * @return Boolean - this denotes whether the deletion was successful
     */
    public static Boolean deleteDirectory(File directory) {
        Boolean isDirectoryDeleted = false;
        File[] files = directory.listFiles();

        if (files.length != 0) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }

        isDirectoryDeleted = directory.delete();
        return isDirectoryDeleted;
    }
}
