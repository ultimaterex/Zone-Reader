package com.serubii.zonereader.helpers.mrz;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * The  Legacy mrz helper.
 */
public class LegacyMRZHelper {
    private String TAG = this.getClass().getSimpleName();

    /**
     * Process data hash map.
     *
     * @param text_by_line the text by line
     * @return the hash map
     */
    public HashMap processData(ArrayList<String> text_by_line) {
        Log.i(TAG, " Starting Process data with " + text_by_line);

        ArrayList<String> linesList = new ArrayList<>();
        linesList.clear();
        HashMap FinalMap = new HashMap();


        // check for TD3 first
        linesList = checkForTD3(text_by_line);
        if (linesList.size() == 2) {
            FinalMap = processTD3(linesList);
        } else {
            // check for TD1 first
            linesList = checkForTD1(text_by_line);
            if (linesList.size() == 3) {
                FinalMap = processTD1(linesList);
            } else if (text_by_line.size() >= 8) {
                // start for fallback id system
                linesList.clear();
                linesList = checkForOldID(text_by_line);
                if (linesList != null) {
                    FinalMap = processOldID(linesList);
                }
            }
        }
        return FinalMap;

//
//        linesList = checkForTD1(text_by_line);
//            if (linesList.size() == 3) {
//                FinalMap = processTD1(linesList);
//            } else if (text_by_line.size() >= 8) {
//                // start for fallback id system
//                linesList.clear();
//                linesList = checkForOldID(text_by_line);
//                if (linesList != null) {
//                    FinalMap = processOldID(linesList);
//                }
//            }
//            return FinalMap;
    }

    private ArrayList<String> checkForTD1(ArrayList<String> OCR_lines) {
        ArrayList<String> MRZ = new ArrayList<>();
        MRZ.clear();

        // String keeps track of previous line
        String runtimeString = "";

        for (String text : OCR_lines) {
            // sanitize text
            text = text.replaceAll("\\s", "").trim();
            // not sure if « represents 1 or 2 <'s, we've had better luck with one
//            text = text.replaceAll("«", "<<");
            text = text.replaceAll("«", "<");
            if (text.contains("<")) {


                if (text.length() > 30) {
                    Log.i(TAG, "Worthwhile String found");
                    String ShortenedString = text.replaceAll("\\s", "").trim();
                    if (ShortenedString.length() == 30) {
                        Log.i(TAG, "ADDED TO STACK: " + ShortenedString);
                    } else {
                        ShortenedString = ShortenedString.substring(0, 30);
                        MRZ.add(ShortenedString);
                        Log.w(TAG, "ADDED HESISTANT STRING TO STACK " + ShortenedString);
                    }
                } else if (text.length() == 30) {
                    MRZ.add(text);
                    Log.i(TAG, "ADDED TO STACK: " + text);

                } else if ((text + runtimeString).length() == 30) {
                    MRZ.add(text + runtimeString);
                    Log.i(TAG, "ADDED TO STACK: " + text + runtimeString);
                    runtimeString = "";
                } else if (text.length() >= 27) {
                    Log.w(TAG, "ATTEMPTING SURGERY");
                    switch (text.length()) {
                        case 27:
                            text = text + "<<<";
                            MRZ.add(text);
                            Log.w(TAG, "ADDED SURGERY STRING TO STACK " + text);
                            break;
                        case 28:
                            text = text + "<<";
                            MRZ.add(text);
                            Log.w(TAG, "ADDED SURGERY STRING TO STACK " + text);
                            break;
                        case 29:
                            text = text + "<";
                            MRZ.add(text);
                            Log.w(TAG, "ADDED SURGERY STRING TO STACK " + text);
                            break;
                        default:
                            Log.wtf(TAG, "FAILED SURGERY " + text);
                            break;
                    }

                } else {
                    runtimeString = text;
                }
            }
        }

        return MRZ;
    }

    private ArrayList<String> checkForTD3(ArrayList<String> OCR_lines) {
        // TD3 has 2x44char
        ArrayList<String> MRZ = new ArrayList<>();
        MRZ.clear();

        // String keeps track of previous line
        String runtimeString = "";

        for (String text : OCR_lines) {
            // sanitize text
            text = text.replaceAll("\\s", "").trim();
            // not sure if « represents 1 or 2 <'s, we've had better luck with one
//            text = text.replaceAll("«", "<<");
            text = text.replaceAll("«", "<");
            if (text.contains("<")) {


                if (text.length() > 44) {
                    Log.i(TAG, "Worthwhile String found");
                    String ShortenedString = text.replaceAll("\\s", "").trim();
                    if (ShortenedString.length() == 44) {
                        Log.i(TAG, "ADDED TO STACK: " + ShortenedString);
                    } else {
                        ShortenedString = ShortenedString.substring(0, 44);
                        MRZ.add(ShortenedString);
                        Log.w(TAG, "ADDED HESISTANT STRING TO STACK " + ShortenedString);
                    }
                } else if (text.length() == 44) {
                    MRZ.add(text);
                    Log.i(TAG, "ADDED TO STACK: " + text);

                } else if ((text + runtimeString).length() == 44) {
                    MRZ.add(text + runtimeString);
                    Log.i(TAG, "ADDED TO STACK: " + text + runtimeString);
                    runtimeString = "";
                } else if (text.length() >= 40) {
                    Log.w(TAG, "ATTEMPTING SURGERY");
                    switch (text.length()) {
                        case 40:
                            text = text + "<<<<";
                            MRZ.add(text);
                            Log.w(TAG, "ADDED SURGERY STRING TO STACK " + text);
                            break;
                        case 41:
                            text = text + "<<<";
                            MRZ.add(text);
                            Log.w(TAG, "ADDED SURGERY STRING TO STACK " + text);
                            break;
                        case 42:
                            text = text + "<<";
                            MRZ.add(text);
                            Log.w(TAG, "ADDED SURGERY STRING TO STACK " + text);
                            break;
                        case 43:
                            text = text + "<";
                            MRZ.add(text);
                            Log.w(TAG, "ADDED SURGERY STRING TO STACK " + text);
                        default:
                            Log.wtf(TAG, "FAILED SURGERY " + text);
                            break;
                    }

                } else {
                    runtimeString = text;
                }
            }
        }

        return MRZ;
    }

    private HashMap processTD1(ArrayList<String> mrz) {
        String ID_TYPE, ISSUE_COUNTRY, DOCUMENT_NUMBER, OPTIONAL_INFO;
        String DOB, GENDER, EXPIRATION, NATIONALITY;
        String SURNAME, GIVEN_NAME;

        // disable unchecked warning because of unsafe type cast, (expected usage will always be (string,string)
        @SuppressWarnings(value = "unchecked")
        HashMap<String, String> FinalMap = new HashMap();

        // PROCESSES TD1 from https://en.wikipedia.org/wiki/Machine-readable_passport
        if (mrz.size() == 3) {


            // handle line 1 here
            String line_1 = mrz.get(0);
            if (line_1.length() == 30) {

                // get Document Type
                ID_TYPE = line_1.substring(0, 2).replaceAll("<", "D");

                // get Issuing Country
                ISSUE_COUNTRY = line_1.substring(2, 5).replaceAll("<", "D");

                //get Document Number
                DOCUMENT_NUMBER = line_1.substring(5, 14).replaceAll("<", "");

                // get Optional Info
                OPTIONAL_INFO = line_1.substring(15, 30).replaceAll("<", "");
            } else {
                throw new RuntimeException("BAD OCR TYPE" + line_1);
            }

            // handle line 2 here
            String line_2 = mrz.get(1);
            if (line_2.length() == 30) {

                // Get Date of Birth
                DOB = line_2.substring(0, 6);

                // Get GENDER
                GENDER = line_2.substring(7, 8).replaceAll("<", "Unspecified");

                // Get Expiration Date
                EXPIRATION = line_2.substring(8, 14);

                // Get Nationality
                NATIONALITY = line_2.substring(15, 18).replaceAll("<", "");

            } else {
                throw new RuntimeException("BAD OCR " + line_2);
            }

            // Handle Line 3 Here
            String line_3 = mrz.get(2);
            if (line_3.length() == 30) {

                String[] names = line_3.split("<<");
                // Get SURNAME
                SURNAME = names[0].replaceAll("<", "").trim();

                // GET Given Name
                GIVEN_NAME = names[1].replaceAll("<", " ").trim();

            } else {
                throw new RuntimeException("BAD OCR " + line_3);
            }

        } else {
            throw new RuntimeException("BAD OCR " + mrz);
        }


        // Pack all objects into HASHMAP
        FinalMap.put("ID_TYPE", ID_TYPE);
        FinalMap.put("ISSUE_COUNTRY", ISSUE_COUNTRY);
        FinalMap.put("DOCUMENT_NUMBER", DOCUMENT_NUMBER);
        FinalMap.put("EXPIRATION", EXPIRATION);
        FinalMap.put("SURNAME", SURNAME);
        FinalMap.put("GIVEN_NAME", GIVEN_NAME);
        FinalMap.put("GENDER", GENDER);
        FinalMap.put("DOB", DOB);
        FinalMap.put("NATIONALITY", NATIONALITY);
        FinalMap.put("OPTIONAL_INFO", OPTIONAL_INFO);


        Log.i(TAG, "Created FINAL MAP");
        return FinalMap;
    }

    private HashMap processTD3(ArrayList<String> mrz) {
        String ID_TYPE, ISSUE_COUNTRY, DOCUMENT_NUMBER, OPTIONAL_INFO;
        String DOB, GENDER, EXPIRATION, NATIONALITY;
        String SURNAME, GIVEN_NAME;

        // disable unchecked warning because of unsafe type cast, (expected usage will always be (string,string)
        @SuppressWarnings(value = "unchecked")
        HashMap<String, String> FinalMap = new HashMap();

        // PROCESSES TD3 from https://en.wikipedia.org/wiki/Machine-readable_passport
        if (mrz.size() == 2) {


            // handle line 1 here
            String line_1 = mrz.get(0);
            if (line_1.length() == 44) {

                // get Document Type
                ID_TYPE = line_1.substring(0, 2).replaceAll("<", "");
                if (ID_TYPE.equals("P")) {
                    ID_TYPE = "Passport";
                }

                // get Issuing Country
                ISSUE_COUNTRY = line_1.substring(2, 5).replaceAll("<", "D");

                String[] names = line_1.substring(5, 44).split("<<");

                // Get SURNAME
                SURNAME = names[0].replaceAll("<", "").trim().toUpperCase();

                // GET Given Name
                GIVEN_NAME = names[1].replaceAll("<", " ").trim().toUpperCase();


            } else {
                throw new RuntimeException("BAD OCR TYPE" + line_1);
            }

            // handle line 2 here
            String line_2 = mrz.get(1);
            if (line_2.length() == 44) {

                //get Document Number
                DOCUMENT_NUMBER = line_2.substring(0, 9).replaceAll("<", "");

                // Get Nationality
                NATIONALITY = line_2.substring(10, 13).replaceAll("<", "");

                // Get Date of Birth
                DOB = line_2.substring(13, 19);


                // Get GENDER
                GENDER = line_2.substring(20, 21).replaceAll("<", "Unspecified");

                // Get Expiration Date
                EXPIRATION = line_2.substring(21, 27);

                // get Optional Info
                OPTIONAL_INFO = line_2.substring(27, 42).replaceAll("<", "");


            } else {
                throw new RuntimeException("BAD OCR " + line_2);
            }


        } else {
            throw new RuntimeException("BAD OCR " + mrz);
        }


        // Pack all objects into HASHMAP
        FinalMap.put("ID_TYPE", ID_TYPE);
        FinalMap.put("ISSUE_COUNTRY", ISSUE_COUNTRY);
        FinalMap.put("DOCUMENT_NUMBER", DOCUMENT_NUMBER);
        FinalMap.put("EXPIRATION", EXPIRATION);
        FinalMap.put("SURNAME", SURNAME);
        FinalMap.put("GIVEN_NAME", GIVEN_NAME);
        FinalMap.put("GENDER", GENDER);
        FinalMap.put("DOB", DOB);
        FinalMap.put("NATIONALITY", NATIONALITY);
        FinalMap.put("OPTIONAL_INFO", OPTIONAL_INFO);


        Log.i(TAG, "Created FINAL MAP");
        return FinalMap;
    }

    private ArrayList<String> checkForOldID(ArrayList<String> OCR_lines) {
        ArrayList<String> MRZ = new ArrayList<>();
        MRZ.clear();
        Log.i(TAG, "Starting FALLBACK MODE");

        boolean valid_id = false;

        boolean country = false;
        boolean country_id = false;
        boolean city = false;

        // check if string contains SURINAME, SME and PRBO if not boolean false
        Log.wtf(TAG, "List when checking: " + OCR_lines);

        for (String text : OCR_lines) {
            if (text.contains("SURINAME")) {
                country = true;
            }
        }

        for (String text : OCR_lines) {
            if (text.contains("SME")) {
                country_id = true;
            }
        }

        for (String text : OCR_lines) {
            if (text.contains("PRBO")) {
                city = true;
            }
        }

        if (country && country_id && city) {
            valid_id = true;
        } else {
            return null;
        }

        if (valid_id) {
            for (String text : OCR_lines) {
                if (text.contains("SURINAME")) {
                    MRZ.clear();
                    MRZ.add(text);
                } else {
                    MRZ.add(text);
                }
            }
        }
        return MRZ;
    }

    private HashMap processOldID(ArrayList<String> mrz) {
//        foo
        Log.wtf(TAG, "LINES SIZE FOUND: " + mrz.size());
        Log.wtf(TAG, "LINES FOUND: " + mrz.toString());

        String ID_TYPE, ISSUE_COUNTRY, DOCUMENT_NUMBER, OPTIONAL_INFO;
        String DOB = "", GENDER, EXPIRATION, NATIONALITY = "";
        String SURNAME, GIVEN_NAME;

        // disable unchecked warning because of unsafe type cast, (expected usage will always be (string,string)
        @SuppressWarnings(value = "unchecked")
        HashMap<String, String> FinalMap = new HashMap();

        // PROCESSES OLD ID's according to
        // [- SURINAME-, 9 15, FR 002796 M, Baidjnath, Selby J, SME, 12 08 1999, PRBO, LATB| C, DE, DES, JSmA]

        // handle line 1 here

        // suriname always
        String line_1 = mrz.get(0);

        // Issue Date
        String line_2 = mrz.get(1);

        // DOC NUM
        String line_3 = mrz.get(2);

        // SURNAME
        String line_4 = mrz.get(3);

        // GIVEN NAMES
        String line_5 = mrz.get(4);

        //NATIONALITY
        String line_6 = mrz.get(5);

        // B-DAY
        String line_7 = mrz.get(6);

        //CITY
        String line_8 = mrz.get(7);


        if (line_1.toUpperCase().contains("SURINAME")) {

            // get Document Type
            ID_TYPE = "Old ID";

            // get Issuing Country
            ISSUE_COUNTRY = line_1.replaceAll("-", "").trim();

            //get Document Number
//            (DOCUMENT_NUMBER.length() - 1);
//            if (!GENDER.equals("M") && !GENDER.equals("V") && !GENDER.equals("F")) {

            DOCUMENT_NUMBER = mrz.get(4).trim();
            Log.wtf(TAG, "GOING WITH DN FOR: " + DOCUMENT_NUMBER);
//            String tX = DOCUMENT_NUMBER.substring(DOCUMENT_NUMBER.length() - 1);
//            if (!containsDigit(DOCUMENT_NUMBER)) {
//                if (!DOCUMENT_NUMBER.startsWith("F")) {
//                    Log.wtf(TAG, "WE GOT A FALSE");
//                    DOCUMENT_NUMBER = "";
//                    for (int i = 0; i < mrz.size(); i++) {
//                        String line = mrz.get(i);
//                        if (line.startsWith("F")) {
//                            String tZ = line.substring(line.length() - 1);
//                            if (tZ.toUpperCase().equals("M") || tZ.toUpperCase().equals("V") || tZ.toUpperCase().equals("F")) {
//                                DOCUMENT_NUMBER = line.trim();
//                                Log.i(TAG, "Going with" + DOCUMENT_NUMBER);
//
//                            }
//                        }
//                    }
//                } else {
//                    Log.wtf(TAG, "DIDN'T FIND ANY PROBLEMS WITH DOCUMENT NUMBER for normal standard");
//                    // fallback for (old-oldID, or when it fetches expiration on same line as doc id)
//                    if (!containsDigit(DOCUMENT_NUMBER)) {
//                        if (!DOCUMENT_NUMBER.contains("F")) {
//                            Log.wtf(TAG, "WE GOT A TRUE");
//                            for (int i = 0; i < mrz.size(); i++) {
//                                String line = mrz.get(i);
//                                if (line.startsWith("F")) {
//                                    String tZ = line.substring(line.length() - 1);
//                                    if (tZ.toUpperCase().equals("M") || tZ.toUpperCase().equals("V") || tZ.toUpperCase().equals("F")) {
//                                        DOCUMENT_NUMBER = line.trim();
//                                        Log.i(TAG, "Going with" + DOCUMENT_NUMBER);
//
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }


            // get Optional Info
//            OPTIONAL_INFO = line_6.trim() + ", " + line_8.trim();

            if (!containsDigit(DOCUMENT_NUMBER)) {
                DOCUMENT_NUMBER = "";
                for (int i = 0; i < mrz.size(); i++) {
                    String line = mrz.get(i);
                    if (line.startsWith("F")) {
                        String tZ = line.substring(line.length() - 1);
                        if (tZ.toUpperCase().equals("M") || tZ.toUpperCase().equals("V") || tZ.toUpperCase().equals("F")) {
                            DOCUMENT_NUMBER = line.trim();
                            Log.i(TAG, "Going with" + DOCUMENT_NUMBER);

                        }
                    } else if (line.startsWith("1") || line.startsWith("0")) {
                        if (line.length() >= 8) {
                            String x = line.substring(5);
                            if (x.trim().startsWith("F")) {
                                Log.wtf(TAG, "found " + x);
                                DOCUMENT_NUMBER = x;
                                break;
                            }
                        }
                    }

                }
            } else if (DOCUMENT_NUMBER.contains("SME")) {
                DOCUMENT_NUMBER = "";
                for (int i = 0; i < mrz.size(); i++) {
                    String line = mrz.get(i);
                    if (line.startsWith("F")) {
                        String tZ = line.substring(line.length() - 1);
                        if (tZ.toUpperCase().equals("M") || tZ.toUpperCase().equals("V") || tZ.toUpperCase().equals("F")) {
                            DOCUMENT_NUMBER = line.trim();
                            Log.i(TAG, "Going with" + DOCUMENT_NUMBER);

                        }
                    } else if (line.startsWith("1") || line.startsWith("0")) {
                        if (line.length() >= 8) {
                            String x = line.substring(5);
                            Log.wtf(TAG, "found " + x);
                            DOCUMENT_NUMBER = x;

                        }
                    }

                }
            }


            OPTIONAL_INFO = "";

            // Get Date of Birth
            if (line_7.length() == 10) {
                DOB = line_7.trim();
            } else {
                for (int i = 0; i < mrz.size(); i++) {
                    String line = mrz.get(i);
                    if (line.length() == 10) {
                        if (line.matches("^\\d{1,2}[\\s]\\d{2}[\\s]\\d{4}$")) {
                            Log.i(TAG, "Going with" + DOB);
                            DOB = line.trim();
                        }
                    }
                }
            }
            // fallback for old standard
            if (DOB.length() < 10) {
                for (int i = 0; i < mrz.size(); i++) {
                    String line = mrz.get(i);
                    if (line.length() > 13) {
                        // SME 10 09 1991
                        if (line.trim().startsWith("SME")) {
                            line = line.substring(3);
                            if (line.trim().matches("^\\d{1,2}[\\s]\\d{2}[\\s]\\d{4}$")) {
                                DOB = line.trim();
                                Log.i(TAG, "Going with" + DOB);
                            } else if (line.contains("PRBO")) {
                                line = line.substring(0, line.length() - 4).trim();
                                if (line.trim().matches("^\\d{1,2}[\\s]\\d{2}[\\s]\\d{4}$")) {
                                    DOB = line.trim();
                                    Log.i(TAG, "Going with" + DOB);
                                }
                            }
                        } else if (line.contains("PRBO")) {
                            line = line.substring(0, line.length() - 4).trim();
                            if (line.trim().matches("^\\d{1,2}[\\s]\\d{2}[\\s]\\d{4}$")) {
                                DOB = line.trim();
                                Log.i(TAG, "Going with" + DOB);
                            }
                        }
                    }
                }
            }
            //fallback in case of 0
            if (DOB.length() < 10) {
                for (int i = 0; i < mrz.size(); i++) {
                    String line = mrz.get(i);
                    if (line.length() > 13) {
                        // SME 10 09 1991
                        if (containsDigit(line)) {
                            line = line.substring(0, line.length() - 4).trim();
                            if (line.trim().matches("^\\d{1,2}[\\s]\\d{2}[\\s]\\d{4}$")) {
                                DOB = line.trim();
                                Log.i(TAG, "Going with" + DOB);
                            }
                        }
                    }
                }
            }


            // Get GENDER
            GENDER = DOCUMENT_NUMBER.substring(DOCUMENT_NUMBER.length() - 1);
            if (!GENDER.equals("M") && !GENDER.equals("V") && !GENDER.equals("F")) {
                for (String line : mrz) {
                    if (line.length() >= 8) {
                        String x = line.substring(line.length() - 1);
                        if (x.toUpperCase().equals("M") || x.toUpperCase().equals("V") || x.toUpperCase().equals("F")) {
                            GENDER = x;
                            Log.i(TAG, "Going with" + GENDER);

                        }
                    }

                }
            }

            // Get Expiration Date
            EXPIRATION = "Expired";

            // Get Nationality
//            NATIONALITY =line_6.trim();
            for (String line : mrz) {
                if (line.contains("SME")) {
                    NATIONALITY = "SME";
                }
            }


            // Get SURNAME
            SURNAME = line_4.trim();
            if (SURNAME.length() < 2) {
                if (containsDigit(SURNAME)) {
                    if (!SURNAME.matches("([A-Z][a-zA-Z]*)")) {
                        for (String line : mrz) {
                            if (line.matches("([A-Z][a-z]*)") && line.length() >= 3 && !line.equals(DOCUMENT_NUMBER) && !line.contains("SME") && !line.contains("PRBO")) {
                                SURNAME = line;
                                break;
                            }
                        }
                    }
                }
            }

            // GET Given Name
            GIVEN_NAME = line_5.trim();
            if (containsDigit(GIVEN_NAME)) {
                if (!GIVEN_NAME.matches("([A-Z][a-z]*)")) {
                    for (String line : mrz) {
                        if (line.matches("([A-Z][a-z]*)") && line.length() >= 3) {
                            if (!line.equals(SURNAME) && !line.equals(DOCUMENT_NUMBER) && !line.contains("SME") && !line.contains("PRBO")) {
                                if (!line.contains(SURNAME)) {
                                    GIVEN_NAME = line;
                                }
                            }
                        }
                    }
                }
            }
            if (GIVEN_NAME.contains(SURNAME)) {
                for (String line : mrz) {
                    if (line.matches("([A-Z][a-z]*)") && line.length() >= 3) {
                        if (!line.equals(SURNAME) && !line.equals(DOCUMENT_NUMBER) && !line.contains("SME") && !line.contains("PRBO")) {
                            if (!line.contains(SURNAME)) {
                                GIVEN_NAME = line;
                            }
                        }
                    }
                }
            }


            // Pack all objects into HASH MAP
            FinalMap.put("ID_TYPE", ID_TYPE);
            FinalMap.put("ISSUE_COUNTRY", ISSUE_COUNTRY);
            FinalMap.put("DOCUMENT_NUMBER", DOCUMENT_NUMBER);
            FinalMap.put("EXPIRATION", EXPIRATION);
            FinalMap.put("SURNAME", SURNAME);
            FinalMap.put("GIVEN_NAME", GIVEN_NAME);
            FinalMap.put("GENDER", GENDER);
            FinalMap.put("DOB", DOB);
            FinalMap.put("NATIONALITY", NATIONALITY);
            FinalMap.put("OPTIONAL_INFO", OPTIONAL_INFO);


            Log.i(TAG, "Created FINAL MAP");


        }
        return FinalMap;
    }


    /**
     * Contains digit boolean.
     *
     * @param s the s
     * @return the boolean
     */
    public final boolean containsDigit(String s) {
        boolean containsDigit = false;

        if (s != null && !s.isEmpty()) {
            for (char c : s.toCharArray()) {
                if (containsDigit = Character.isDigit(c)) {
                    break;
                }
            }
        }
        Log.wtf(TAG, "Checked If Digit in " + s + " with result " + containsDigit);

        return containsDigit;
    }
}






