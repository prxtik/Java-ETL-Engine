package com.dwmt_assignment2;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.InsertManyResult;

public class TransformationEngine {

    /*
     * This method reads the data stored in the files and then uploads the data to
     * MongoDB. For each keyword, a new Collection is created under the same
     * database and all the articles related to that keyword are stored in
     * respective collections.
     */
    public static void readFilesAndUploadData() {
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

        // MongoDB deployment's connection string
        String uri = "<YOUR_MONGODB_CONNECTION_STRING>";

        try (MongoClient mongoClient = MongoClients.create(uri)) {

            MongoDatabase database = mongoClient.getDatabase("myMongoNews");
            if (database != null) {
                database.drop();
                database = mongoClient.getDatabase("myMongoNews");
            }

            File allDirectories = new File("./newsAPIData");
            String[] directories = allDirectories.list();
            if (directories.length != 0) {
                System.out.println("---------- Uploading data to MongoDB ----------");
                for (String dir : directories) {
                    File directory = new File("./newsAPIData/" + dir);
                    if (keywords.contains(directory.getName())) {
                        String collectionName = directory.getName();
                        database.createCollection(collectionName);
                        MongoCollection<Document> collection = database
                                .getCollection(collectionName);
                        File[] files = directory.listFiles();
                        List<Document> articlesToUpload = new ArrayList<>();

                        for (int j = 1; j <= files.length; j++) {
                            if (files[j - 1].isFile()) {
                                String jsonString = new String(Files.readAllBytes(Paths
                                        .get("./newsAPIData/" + directory.getName() + "/" + directory.getName() + "_"
                                                + j
                                                + ".json")));
                                if (!jsonString.isEmpty()) {
                                    jsonString = jsonString.substring(2, jsonString.length() - 2);
                                    String[] articles = jsonString.split("\\},\\{");

                                    for (int i = 0; i < articles.length; i++) {
                                        String title = articles[i].split("title\":")[1].split("\"content\":")[0]
                                                .substring(
                                                        1,
                                                        articles[i].split("title\":")[1].split("\"content\":")[0]
                                                                .length() - 1);
                                        String content = articles[i].split("content\":")[1].substring(1,
                                                articles[i].split("content\":")[1].length() - 1);
                                        title = cleanData(title);
                                        content = cleanData(content);
                                        content = refactorContentString(content);

                                        articlesToUpload
                                                .add(new Document().append("title", title).append("content", content));

                                    }
                                }
                            }
                        }
                        InsertManyResult insertedResults = collection.insertMany(articlesToUpload);
                        System.out.println("\"" + collectionName + "\" collection is successfully inserted with "
                                + insertedResults.getInsertedIds().size() + " documents");
                        articlesToUpload = new ArrayList<>();
                    }
                }
            } else {
                System.out.println("Cannot find any data to upload to MongoDB.");
                mongoClient.close();
                System.exit(0);
            }
            mongoClient.close();
            System.out.println("---------- Upload to MongoDB Complete! ----------");
            System.exit(0);

        } catch (Exception e) {
            System.out.println("Error occurred while uploading data to MongoDB.");
        }
    }

    /**
     * This method cleans the data and removes any special characters, html tags,
     * tab separators, emoticons, URLs
     * 
     * @param str - the string that needs to be cleaned
     * @return String - the string that is cleaned
     */
    public static String cleanData(String str) {
        /*
         * Code for detecting URLs adapted from Stack Overflow and then refactored
         * Available:
         * https://stackoverflow.com/questions/5713558/detect-and-extract-url-from-a-
         * string
         */

        /*
         * Code for detecting emoticons adapted from Stack Overflow and then refactored
         * Available:
         * https://stackoverflow.com/questions/60293057/regex-to-filter-string-with-
         * unicode-characters-and-emojis-for-java
         */
        str = str
                .replaceAll("<[^>]*>", " ")// for html tags
                .replaceAll("[^\\p{L}\\p{N}\\p{P}\\p{Z}]", "")// for emoticons
                .replaceAll("\\[ntr]", " ")// for backslashes and line or tab separators
                .replaceAll(
                        "((https?|ftp|gopher|telnet|file|Unsure|http):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)",
                        "")// for urls
                .replaceAll("[^a-zA-Z0-9\\s+]", "");// for special characters
        return str;
    }

    /**
     * This methods refactors the overflowing data in a better and legible
     * structure. For eg.: Reformats the string "...8751 chars" to "... more
     * 8751 characters".
     * 
     * @param str - the string that needs to be refactored
     * @return String - the string that is refactored
     */
    public static String refactorContentString(String str) {
        String refactoredString = new String();
        String[] strArray = str.split(" ");
        if (strArray[strArray.length - 1].contains("chars") && strArray[strArray.length - 2].matches("\\d+")) {
            for (int i = 0; i < strArray.length - 1; i++) {
                if (i != strArray.length - 2) {
                    refactoredString = refactoredString.concat(strArray[i] + " ");
                } else {
                    refactoredString = refactoredString
                            .concat("...more " + strArray[i] + " characters.");
                    break;
                }
            }
        } else {
            refactoredString = str;
        }
        return refactoredString;
    }
}
