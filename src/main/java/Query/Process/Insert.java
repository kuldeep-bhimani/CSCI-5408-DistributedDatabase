package Query.Process;

import Query.GlobalConfig;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Insert {

    Common common = new Common();
    GlobalConfig globalConfig = new GlobalConfig();
    String basePath = globalConfig.getBasePath();
    String filePathSeparator = globalConfig.getPathSeparator();
    String delimiter = globalConfig.getDelimiter();
    String rowDelimiter = globalConfig.getRowDelimiter();

    public boolean check(String queryString) {
        boolean isValidQuery = false;

        queryString = queryString.toLowerCase();

        String currentDatabase;

        String insertParseRegex = "(?:INSERT INTO)\\s+(?<table>\\w+)\\s+(?:VALUES)\\s+(?:[(](?<values>.+)[)]);";

        String insertValueRegex = "(?:\"(?<value>.|[^\"]+)\"(?:,|$))";

        Pattern insertParsePattern = Pattern.compile(insertParseRegex, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        Pattern insertValuePattern = Pattern.compile(insertValueRegex, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

        Matcher insertParseMatcher = insertParsePattern.matcher(queryString);

        if (queryString.toLowerCase().contains("insert")) {
            System.out.println("insert table detected");
            currentDatabase = globalConfig.getGlobalDatabase();
            if (insertParseMatcher.find()) {
                if (currentDatabase != null) {
                    if (insertParseMatcher.group("table").isEmpty() || insertParseMatcher.group("table").isBlank()) {
                        System.out.println("Invalid query no table name found");
                        isValidQuery = false;
                    } else {
                        System.out.println("Valid insert query");
                        String tableName = insertParseMatcher.group("table").trim();
                        String valuesToInsert = insertParseMatcher.group("values");

                        System.out.println("valuesToInsert = " + valuesToInsert);

                        Matcher insertValueMatcher = insertValuePattern.matcher(valuesToInsert);

                        List<String[]> metadataList = common.getStructure(tableName);
                        String primaryKey = common.getPrimaryKey(tableName);

                        System.out.println("metadataList = " + metadataList);
                        System.out.println("Arrays.toString(metadataList[0] = " + metadataList.get(0)[0]);
                        String[] columnNames = metadataList.get(0);
                        System.out.println("columnNames.length = " + columnNames.length);
                        String[] columnTypes = metadataList.get(1);


//                    System.out.println("insertValueMatcher.results().count() = " + insertValueMatcher.results().count());
//                    insertValueMatcher.reset();
                        if (insertValueMatcher.results().count() == columnNames.length) {
                            insertValueMatcher.reset();
                            System.out.println("Valid values");
                            int countValues = 0;
                            String rowValue = "[";
                            int primaryKeyIndex = common.getPrimaryIndex(tableName);
                            List<Object> primaryKeyValueList = common.getPrimaryKeyList(tableName);

                            boolean isPrimaryKeyValueUnique = true;
                            while (insertValueMatcher.find()) {
                                String value = insertValueMatcher.group("value").trim();
                                if (primaryKeyIndex != -1 && countValues == primaryKeyIndex && primaryKeyValueList.contains(value)) {
                                    isPrimaryKeyValueUnique = false;
                                }
                                if(isPrimaryKeyValueUnique){
                                    System.out.println("value = " + value);
                                    String columnType = columnTypes[countValues].trim();
                                    System.out.println("columnType = " + columnType);
                                    if (columnType.equals("int")) {
                                        try {
                                            int columnValue = Integer.parseInt(value);
                                            rowValue += value + rowDelimiter;
                                            isValidQuery = true;
                                        } catch (NumberFormatException nFE) {
                                            nFE.printStackTrace();
                                            System.out.println("Not integer value");
                                            isValidQuery = false;
                                            break;
                                        }
                                    } else if (columnType.matches("varchar|text")) {
                                        rowValue += value + rowDelimiter;
                                        isValidQuery = true;
                                    } else if (columnType.equals("boolean")) {
                                        if (value.equals("true") | value.equals("false")) {
                                            rowValue += value + rowDelimiter;
                                            isValidQuery = true;
                                        } else {
                                            System.out.println("Not boolean value");
                                            isValidQuery = false;
                                            break;
                                        }
                                    } else {
                                        System.out.println("Not valid type of data");
                                        isValidQuery = false;
                                        break;
                                    }
                                    countValues++;

                                }
                                else{
                                    System.out.println("Primary key value not unique");
                                    isValidQuery = false;
                                    break;
                                }

                            }
                            if (isValidQuery) {
                                rowValue = rowValue.substring(0, rowValue.length() - 1);
                                rowValue += "]";
                                System.out.println("rowValue = " + rowValue);
                                writeTable(currentDatabase, tableName, rowValue);
                                isValidQuery = true;
                            }

                        } else {
                            System.out.println("Invalid values count");
                        }

                    }
                } else {
                    System.out.println("Please set default schema/database first");
                    System.out.println("enter command : \n use <database_name>;");
                    isValidQuery = false;
                }
            }

        }

        return isValidQuery;
    }

    public boolean writeTable(String dbName, String tableName, String rowValue) {
        boolean isWritten = false;

        String fileName = basePath + dbName + filePathSeparator + "" + tableName + ".txt";
        System.out.println("fileName = " + fileName);

        try {
            File file = new File(fileName);
            if (file.exists()) {
                FileWriter fileWriter = new FileWriter(file, true);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                PrintWriter printWriter = new PrintWriter(bufferedWriter);
                printWriter.print(rowValue + delimiter);
                printWriter.close();
                bufferedWriter.close();
                fileWriter.close();
            }
            // create new file
            else {
                PrintWriter printWriter = new PrintWriter(file);
                printWriter.print(tableName);
                printWriter.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return isWritten;
    }

}
