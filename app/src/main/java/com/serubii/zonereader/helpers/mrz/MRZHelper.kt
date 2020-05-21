package com.serubii.zonereader.helpers.mrz

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * A class that processes MRZ types from OCR
 *
 * use with [process] after calling OCR
 *
 * TD1 3x30
 * TD2 2x36
 * TD3 2x44
 *
 * data accessible from [HashMap]<[String], [String]> : with Keys available in constants or->
 * * ID_TYPE
 * * ISSUE_COUNTRY
 * * DOCUMENT_NUMBER
 * * EXPIRATION
 * * SURNAME
 * * GIVEN_NAME
 * * GENDER
 * * DOB
 * * NATIONALITY
 * * OPTIONAL_INFO
 * * MRZ
 *
 *
 * Realistically I should start abstracting these away
 *
 * @author Rex Serubii
 */
class MRZHelper(pContext: Context) {
    private val TAG = this.javaClass.simpleName
    private val context: Context

    /**
     * supportVal
     * Full Support (11)
     * Standard Support TD1, TD2, TD3 (10)
     *
     * Only TD1 and TD2 Support (9)
     * Only TD1 and TD3 Support (8)
     * Only TD2 and TD3 Support (7)
     *
     *
     * Only TD1 Support (6)
     * Only TD2 Support (5)
     * Only TD3 Support (4)
     *
     * No support? (0)
     *
     *  Other variations for Legacy support are not implemented.
     *  any time Legacy is enabled it will have to support the entire set
     */

    @Suppress("JoinDeclarationAndAssignment")
    private val supportVal: Int

    private var supportTD1: Boolean = true
    private var supportTD2: Boolean = true
    private var supportTD3: Boolean = true
    private var legacy: Boolean = true


    init {
        context = pContext
        supportVal = checkSettings()
    }


    /** Will try to process anything, respects preference settings
     *
     * @param text_array ArrayList<String>
     * @return HashMap<String, String>?
     */
    fun process(text_array: ArrayList<String>): HashMap<String, String>? {
        return when (supportVal) {
            11 -> {
                try {
                    processBlockData(text_array);
                } catch (e: Exception) {
                    e.printStackTrace()
                } as HashMap<String, String>?
            }
            in 4..10 -> {
                val x = processBlockData(text_array) ?: return null
                x.getHashMap()
            }
            else -> throw IllegalArgumentException("Did you forget to enable any support?")
        }
    }

    /**
     *  that processes text from OCR
     *
     * @param text_by_line [ArrayList]<[String]> containing the OCR text
     * @return [HashMap]<[String],[String]> containing the MRZ data
     */
    @Deprecated(
            message = "Deprecated because of Inconsistent behaviour",
            replaceWith = ReplaceWith("processBlockData()"),
            level = DeprecationLevel.WARNING
    )
    fun processDataLine(text_by_line: ArrayList<String>): HashMap<String, String>? {
        Log.i(TAG, " Starting Process data with $text_by_line")

        var linesList = ArrayList<String>()
        linesList.clear()
        var finalMap = HashMap<String, String>()

        // check for TD3 first
        linesList = checkForLineTD3(text_by_line)
        if (linesList.size == 2) {
            finalMap = processLineTD3(linesList)
        } else { // check for TD1 first
            linesList = checkForLineTD1(text_by_line)
            if (linesList.size == 3) {
                finalMap = processLineTD1(linesList)
            } else if (text_by_line.size >= 8) { // start for fallback id system
                linesList.clear()
                try {
                    linesList = checkForOldID(text_by_line)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                finalMap = processOldID(linesList)
            }
        }

        if (finalMap.isEmpty()) {
            return null
        }

        return finalMap
    }

    /**
     * ONLY SUPPORTS TD1 (3x30)
     *
     *  that processes block data returned from OCR
     *
     * @param text_by_block Blocks of text from OCR
     *
     * @return [HashMap]<[String], [String]> with MRZ data, will be empty if [text_by_block] is invalid
     *
     * @author Rex Serubii
     */
    private fun processBlockDataTD1(text_by_block: ArrayList<String>): HashMap<String, String> {
        Log.i(TAG, " Starting Process data with $text_by_block")

        val blockList = ArrayList<String>()
        blockList.clear()

        var finalMap = HashMap<String, String>()

        // returns null if it's false
        val mrz = checkCompatibility(text_by_block) ?: return finalMap

        finalMap = processBlockTD1(mrz.toUpperCase(Locale.ROOT))
        return finalMap
    }

    /**
     * Check what modes are  supported
     * @return Int
     */
    private fun checkSettings(): Int {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        supportTD1 = sharedPreferences.getBoolean("TD1", true)
        supportTD2 = sharedPreferences.getBoolean("TD2", true)
        supportTD3 = sharedPreferences.getBoolean("TD3", true)
        legacy = sharedPreferences.getBoolean("Legacy", false)

        // check for deprecation
        if (legacy && supportTD1 && supportTD2 && supportTD3) {
            // Full Support (11) delegate to legacy handler
            return 11
        } else if (supportTD1 && supportTD2 && supportTD3) {
            // Standard Support TD1, TD2, TD3 (10)
            return 10
        } else if (supportTD1 && supportTD2) {
            // Only TD1 and TD2 Support (9)
            return 9
        } else if (supportTD1 && supportTD3) {
            // Only TD1 and TD3 Support (8)
            return 8
        } else if (supportTD2 && supportTD3) {
            //  Only TD2 and TD3 Support (7)
            return 7
        } else if (supportTD1) {
            // Only TD1 Support (6)
            return 6
        } else if (supportTD2) {
            // Only TD2 Support (5)
            return 5
        } else if (supportTD3) {
            // Only TD3 Support (4)
            return 4
        }

        Log.wtf(TAG, "Did you forget to enable any of the supported formats?")
        return 0
    }

    /**
     * processes block data returned from OCR
     * @param text_by_block ArrayList<String>
     * @return MRZ?
     */
    private fun processBlockData(text_by_block: ArrayList<String>): MRZ? {

        val mrz = checkBlockCompatibility(text_by_block)
        if (mrz != null) {
            return MRZ(mrz)
        }
        return null
    }

    /**
     *  that processes all data from a single MRZ block according to TD1 format
     *
     * @param MRZ [String] of 90 characters
     * @return [HashMap]<[String],[String]> containing the full format data
     *
     */
    private fun processBlockTD1(MRZ: String): HashMap<String, String> {
        val finalIdType: String
        val finalIssueCountry: String
        val finalDocumentNumber: String
        val finalOptionalInfo: String
        val finalDOB: String
        val finalGender: String
        val finalExpiration: String
        val finalNationality: String
        val finalSurname: String
        val finalGivenName: String

        // split MRZ into three lines:
        val line_1 = MRZ.substring(0, 30)
        val line_2 = MRZ.substring(30, 60)
        val line_3 = MRZ.substring(60, 90)


        // disable unchecked warning because of unsafe type cast, (expected usage will always be (string,string)
        val finalMap = HashMap<String, String>()


        // PROCESSES TD1 from https://en.wikipedia.org/wiki/Machine-readable_passport
        if (line_1.length == 30) { // get Document Type
            finalIdType = line_1.substring(0, 2).replace("<".toRegex(), "D")
            // get Issuing Country
            finalIssueCountry = line_1.substring(2, 5).replace("<".toRegex(), "D")
            //get Document Number
            finalDocumentNumber = line_1.substring(5, 14).replace("<".toRegex(), "")
            // get Optional Info
            finalOptionalInfo = line_1.substring(15, 30).replace("<".toRegex(), "")
        } else {
            throw RuntimeException("BAD OCR TYPE$line_1")
        }
        // handle line 2 here
        if (line_2.length == 30) { // Get Date of Birth
            finalDOB = line_2.substring(0, 6)
            // Get GENDER
            finalGender = line_2.substring(7, 8).replace("<".toRegex(), "Unspecified")
            // Get Expiration Date
            finalExpiration = line_2.substring(8, 14)
            // Get Nationality
            finalNationality = line_2.substring(15, 18).replace("<".toRegex(), "")
        } else {
            throw RuntimeException("BAD OCR $line_2")
        }
        // Handle Line 3 Here
        if (line_3.length == 30) {
            val names = line_3.split("<<").toTypedArray()
            // Get SURNAME
            finalSurname = names[0].replace("<".toRegex(), "").trim { it <= ' ' }
            // GET Given Name
            finalGivenName = names[1].replace("<".toRegex(), " ").trim { it <= ' ' }
        } else {
            throw RuntimeException("BAD OCR $line_3")
        }

        // Pack all objects into HASHMAP
        finalMap["ID_TYPE"] = finalIdType
        finalMap["ISSUE_COUNTRY"] = finalIssueCountry
        finalMap["DOCUMENT_NUMBER"] = finalDocumentNumber
        finalMap["EXPIRATION"] = finalExpiration
        finalMap["SURNAME"] = finalSurname
        finalMap["GIVEN_NAME"] = finalGivenName
        finalMap["GENDER"] = finalGender
        finalMap["DOB"] = finalDOB
        finalMap["NATIONALITY"] = finalNationality
        finalMap["OPTIONAL_INFO"] = finalOptionalInfo
        finalMap["MRZ"] = MRZ
        Log.i(TAG, "Created FINAL MAP")
        return finalMap

    }

    /**
     *  that checks compatibility for accurate MRZ types.
     *
     * An accurate MRZ type is 3 lines x 30 characters long
     *
     * @param blocks a [ArrayList] containing blocks of text from OCR, keep size close to 90 characters
     *
     * @return [String] containing the valid MRZ type
     *
     * @author Rex Serubii
     */
    private fun checkCompatibility(blocks: ArrayList<String>): String? {
        val blockList = ArrayList<String>()
        var MRZ = ""


        // filter invalid MRZ types
        for (block in blocks) {
            if (block.startsWith("I") || block.startsWith("A") || block.startsWith("C")) {
                blockList.add(block)
            } else {
                Log.w(TAG, "Rejected Block: [$block] because of start")
            }
        }


        if (blockList.size == 1) {
            // valid MRZ is 3lines x 30character with size of 90
            val block = blockList[0]
            when (block.length) {
                90 -> {
                    val mrz = fixOCRInconsistenciesTD1(block)
                    // handle validation checker
                    return if (validateTD1Block(mrz)) {
                        mrz
                    } else {
                        null
                    }

                }
                in 0..79 -> {
                    Log.w(TAG, "Rejected Block [$block] because of Length")
                    return null
                }

                in 80..100 -> {
                    //handle mrz fixer
                    MRZ = stripWhiteSpace(block)
                    if (MRZ.length != 90) {
                        Log.w(
                                TAG,
                                "Rejected Block [$block] because of Inconsistent Length " + MRZ.length
                        )
                        return null
                    }

                    MRZ = fixOCRInconsistenciesTD1(MRZ)

                    // handle validation checker
                    return if (validateTD1Block(MRZ)) {
                        MRZ
                    } else {
                        Log.w(TAG, "Rejected MRZ [$MRZ] because of Invalid MRZ")
                        null
                    }

                }
            }
        } else {
            return null
        }

        return MRZ
    }

    /**
     *  Checks Compatibility for standard feature set
     * @param blocks ArrayList<String>
     * @return String?
     */
    private fun checkBlockCompatibility(blocks: ArrayList<String>): String? {
        val blockList = ArrayList<String>()
        var MRZ = ""


        // filter valid MRZ types
        for (block in blocks) {
            if (block.startsWith("A") ||
                    block.startsWith("C") ||
                    block.startsWith("I") ||
                    block.startsWith("P") ||
                    block.startsWith("V")) {
                blockList.add(block)
            } else {
                Log.w(TAG, "Rejected Block: [$block] because of start")
            }
        }


        if (blockList.size == 1) {
            // valid MRZ is 3lines x 30character with size of 90
            // valid MRZ is 2 lines x 44char/36char with size of 88/72
            val block = blockList[0]
            when (block.length) {
                //TD1
                90 -> {
                    if (!supportTD1) {
                        return null
                    }
                    val mrz = fixOCRInconsistenciesTD1(block)
                    // handle validation checker
                    return if (validateTD1Block(mrz)) {
                        mrz
                    } else {
                        null
                    }

                }
                // TD3
                88 -> {
                    if (!supportTD3) {
                        return null
                    }
                    val mrz = fixOCRInconsistenciesTD3(block)
                    // handle validation checker
                    return if (validateTD3Block(mrz)) {
                        mrz
                    } else {
                        null
                    }

                }
                // TD2
                72 -> {
                    if (!supportTD2) {
                        return null
                    }
                    val mrz = fixOCRInconsistenciesTD2(block)
                    // handle validation checker
                    return if (validateTD2Block(mrz)) {
                        mrz
                    } else {
                        null
                    }

                }

                in 0..71 -> {
                    Log.w(TAG, "Rejected Block [$block] because of Length")
                    return null
                }

                in 72..100 -> {
                    //handle mrz fixer
                    MRZ = stripWhiteSpace(block)
                    if (MRZ.length != 90 || MRZ.length != 88 || MRZ.length != 72) {
                        Log.w(
                                TAG,
                                "Rejected Block [$block] because of Inconsistent Length " + MRZ.length
                        )
                        return null
                    }

                    when (MRZ.length) {
                        90 -> {
                            MRZ = fixOCRInconsistenciesTD1(MRZ)
                            // handle validation checker
                            return if (validateTD1Block(MRZ)) {
                                MRZ
                            } else {
                                Log.w(TAG, "Rejected MRZ [$MRZ] because of Invalid MRZ")
                                null
                            }
                        }
                        88 -> {
                            MRZ = fixOCRInconsistenciesTD3(MRZ)
                            // handle validation checker
                            return if (validateTD3Block(MRZ)) {
                                MRZ
                            } else {
                                Log.w(TAG, "Rejected MRZ [$MRZ] because of Invalid MRZ")
                                null
                            }
                        }
                        72 -> {
                            MRZ = fixOCRInconsistenciesTD2(MRZ)
                            // handle validation checker
                            return if (validateTD2Block(MRZ)) {
                                MRZ
                            } else {
                                Log.w(TAG, "Rejected MRZ [$MRZ] because of Invalid MRZ")
                                null
                            }
                        }
                        else -> return null
                    }

                }
            }
        } else {
            return null
        }

        return MRZ
    }

    /**
     *  that Strips and removes any whitespace, this includes spaces and linebreaks
     *
     * @param MRZ A [String] you want to strip from.
     *
     * @return [String] that has been stripped.
     *
     * @author Rex Serubii
     */
    private fun stripWhiteSpace(MRZ: String): String {
        var fixedString = MRZ.replace("\n", "")
        fixedString = fixedString.replace(" ", "")
        return fixedString
    }

    /**
     *  that fixes certain inconsistencies with using firebase OCR
     *
     * Yes, this function is magic garbage
     *
     * @param MRZ [String] you want to fix
     *
     * @return [String] that has been fixed
     *
     * @author Rex Serubii
     */
    private fun fixOCRInconsistenciesTD1(MRZ: String): String {
        val firebaseOcharacterIndex =
                5 // firebase OCR seems to think DOC Number 0 is O
        val cutString = MRZ.substring(5, 14).replace("O", "0")

        var fixedString = cutString + MRZ.substring(14)

        // firebase sometimes sees < as k we replace <k< with <
        fixedString = fixedString.replace(("([<])([a-z])([<])").toRegex(), "<<<")

        // this is reaching a bit, but on position 18(17) sometimes 0 gets replaced by O
        // let's use REGEX to solve this problem for FRO0 to FR00
        fixedString = fixedString.replace(("(F)(R)([O])(0)").toRegex(), "FR00")


        return fixedString
    }

    /**
     *  that fixes certain inconsistencies with using firebase OCR
     *
     * Yes, this function is magic garbage
     *
     * @param MRZ [String] you want to fix
     *
     * @return [String] that has been fixed
     *
     * @author Rex Serubii
     */
    private fun fixOCRInconsistenciesTD2(MRZ: String): String {
        val firebaseOcharacterIndex =
                5 // firebase OCR seems to think DOC Number 0 is O
        val cutString = MRZ.substring(36, 45).replace("O", "0")

        var fixedString = cutString + MRZ.substring(45)

        // firebase sometimes sees < as k we replace <k< with <
        fixedString = fixedString.replace(("([<])([a-z])([<])").toRegex(), "<<<")

        return fixedString
    }

    /**
     *  that fixes certain inconsistencies with using firebase OCR
     *
     * Yes, this function is magic garbage
     *
     * @param MRZ [String] you want to fix
     *
     * @return [String] that has been fixed
     *
     * @author Rex Serubii
     */
    private fun fixOCRInconsistenciesTD3(MRZ: String): String {
        val firebaseOcharacterIndex =
                5 // firebase OCR seems to think DOC Number 0 is O
        val cutString = MRZ.substring(44, 53).replace("O", "0")

        var fixedString = cutString + MRZ.substring(53)

        // firebase sometimes sees < as k we replace <k< with <
        fixedString = fixedString.replace(("([<])([a-z])([<])").toRegex(), "<<<")


        return fixedString
    }

    /**
     *  validates MRZ TD1 (ID)
     *
     * We check if the size is 90,
     * the initial character is I; A or C,
     * the character at end of line 2 is a check digit(number)
     * the character at start of line is a letter
     *
     * @param MRZ [String] to validate
     *
     * @return [Boolean] true if valid, false otherwise
     *
     * @author Rex Serubii
     */
    private fun validateTD1Block(MRZ: String): Boolean {

        // check size
        if (MRZ.length != 90) {
            return false
        }

        // check for at pos 1 for I, A or C
        if (!(MRZ.startsWith("I") || MRZ.startsWith("A") || MRZ.startsWith("C"))) {
            return false
        }

        // check for digit at position 60(59) is a number
        if (!containsDigit(MRZ[59].toString())) {
            return false
        }

        // check for digit at position 61(60) is a letter
        if (containsDigit(MRZ[60].toString())) {
            return false
        }


        return true
    }

    /**
     *  validates MRZ TD2 (Visas)
     *
     * We check if the size is 72,
     * the initial character is V
     * the character at position 10 of line 2 is a check digit(number)
     * the character at position 20 of line 2 is a check digit(number)
     *
     *
     * @param MRZ [String] to validate
     *
     * @return [Boolean] true if valid, false otherwise
     *
     * @author Rex Serubii
     */
    private fun validateTD2Block(MRZ: String): Boolean {

        // check size
        if (MRZ.length != 72) {
            return false
        }

        // check for at pos 1 for V
        if (!MRZ.startsWith("V")) {
            return false
        }

        // check for digit at position 46(45) is a number
        if (!containsDigit(MRZ[45].toString())) {
            return false
        }

        // check for digit at position 56(55) is a letter
        if (containsDigit(MRZ[55].toString())) {
            return false
        }


        return true
    }

    /**
     *  validates MRZ TD3 (passport)
     *
     * We check if the size is 88,
     * the initial character is P
     * the character at pos 10 of line 2 is a check digit(number)
     * the character at pos 20 of line 2 is a check digit(number)
     * the character at pos 28 of line 2 is a check digit(number)
     * the character at pos 43 of line 2 is a check digit(number)
     * the character at pos 44 of line 2 is a check digit(number)
     *
     * @param MRZ [String] to validate
     *
     * @return [Boolean] true if valid, false otherwise
     *
     * @author Rex Serubii
     */
    private fun validateTD3Block(MRZ: String): Boolean {

        // check size
        if (MRZ.length != 88) {
            return false
        }

        // check for at pos 1 for P
        if (!MRZ.startsWith("P")) {
            return false
        }

        // check for digit at position 54(53) is a number
        if (!containsDigit(MRZ[53].toString())) {
            return false
        }

        // check for digit at position 64(63) is a letter
        if (containsDigit(MRZ[63].toString())) {
            return false
        }

        // check for digit at position 72(71) is a letter
        if (containsDigit(MRZ[71].toString())) {
            return false
        }

        // check for digit at position 87(86) is a letter
        if (containsDigit(MRZ[86].toString())) {
            return false
        }

        // check for digit at position 88(87) is a letter
        if (containsDigit(MRZ[87].toString())) {
            return false
        }

        return true
    }

    /**
     *  Checks if [ArrayList] passes TD1 specifications
     *
     * @param OCR_lines Lines from OCR to check for Compatibility
     * @return [ArrayList] of compatibility lines for MRZ
     */
    private fun checkForLineTD1(OCR_lines: ArrayList<String>): ArrayList<String> {
        val MRZ = java.util.ArrayList<String>()
        MRZ.clear()
        // String keeps track of previous line
        var runtimeString = ""
        for (text in OCR_lines) { // sanitize text
            var mText = text.replace("\\s".toRegex(), "").trim { it <= ' ' }
            // not sure if « represents 1 or 2 <'s, we've had better luck with one
//            text = text.replaceAll("«", "<<");
            mText = mText.replace("«".toRegex(), "<")
            if (mText.contains("<")) {
                if (mText.length > 30) {
                    Log.i(TAG, "Worthwhile String found")
                    var shortenedString =
                            mText.replace("\\s".toRegex(), "").trim { it <= ' ' }
                    if (shortenedString.length == 30) {
                        Log.i(TAG, "ADDED TO STACK: $shortenedString")
                    } else {
                        shortenedString = shortenedString.substring(0, 30)
                        MRZ.add(shortenedString)
                        Log.w(
                                TAG,
                                "ADDED HESISTANT STRING TO STACK $shortenedString"
                        )
                    }
                } else if (mText.length == 30) {
                    MRZ.add(mText)
                    Log.i(TAG, "ADDED TO STACK: $mText")
                } else if ((mText + runtimeString).length == 30) {
                    MRZ.add(mText + runtimeString)
                    Log.i(TAG, "ADDED TO STACK: $mText$runtimeString")
                    runtimeString = ""
                } else if (mText.length >= 27) {
                    Log.w(TAG, "ATTEMPTING SURGERY")
                    when (mText.length) {
                        27 -> {
                            mText = "$mText<<<"
                            MRZ.add(mText)
                            Log.w(TAG, "ADDED SURGERY STRING TO STACK $mText")
                        }
                        28 -> {
                            mText = "$mText<<"
                            MRZ.add(mText)
                            Log.w(TAG, "ADDED SURGERY STRING TO STACK $mText")
                        }
                        29 -> {
                            mText = "$mText<"
                            MRZ.add(mText)
                            Log.w(TAG, "ADDED SURGERY STRING TO STACK $mText")
                        }
                        else -> Log.w(TAG, "FAILED SURGERY $mText")
                    }
                } else {
                    runtimeString = mText
                }
            }
        }
        return MRZ
    }

    /**
     *  Checks if [ArrayList] passes TD3 specifications
     *
     * @param OCR_lines Lines from OCR to check for Compatibility
     * @return [ArrayList] of compatibility lines for MRZ
     */
    private fun checkForLineTD3(OCR_lines: ArrayList<String>): ArrayList<String> { // TD3 has 2x44char
        val MRZ = java.util.ArrayList<String>()
        MRZ.clear()
        // String keeps track of previous line
        var runtimeString = ""
        for (Text in OCR_lines) { // sanitize text
            var mText = Text.replace("\\s".toRegex(), "").trim { it <= ' ' }
            // not sure if « represents 1 or 2 <'s, we've had better luck with one
//            text = text.replaceAll("«", "<<");
            mText = mText.replace("«".toRegex(), "<")
            if (mText.contains("<")) {
                if (mText.length > 44) {
                    Log.i(TAG, "Worthwhile String found")
                    var shortenedString =
                            mText.replace("\\s".toRegex(), "").trim { it <= ' ' }
                    if (shortenedString.length == 44) {
                        Log.i(TAG, "ADDED TO STACK: $shortenedString")
                    } else {
                        shortenedString = shortenedString.substring(0, 44)
                        MRZ.add(shortenedString)
                        Log.w(
                                TAG,
                                "ADDED HESISTANT STRING TO STACK $shortenedString"
                        )
                    }
                } else if (mText.length == 44) {
                    MRZ.add(mText)
                    Log.i(TAG, "ADDED TO STACK: $mText")
                } else if ((mText + runtimeString).length == 44) {
                    MRZ.add(mText + runtimeString)
                    Log.i(TAG, "ADDED TO STACK: $mText$runtimeString")
                    runtimeString = ""
                } else if (mText.length >= 40) {
                    Log.w(TAG, "ATTEMPTING SURGERY")
                    when (mText.length) {
                        40 -> {
                            mText = "$mText<<<<"
                            MRZ.add(mText)
                            Log.w(TAG, "ADDED SURGERY STRING TO STACK $mText")
                        }
                        41 -> {
                            mText = "$mText<<<"
                            MRZ.add(mText)
                            Log.w(TAG, "ADDED SURGERY STRING TO STACK $mText")
                        }
                        42 -> {
                            mText = "$mText<<"
                            MRZ.add(mText)
                            Log.w(TAG, "ADDED SURGERY STRING TO STACK $mText")
                        }
                        43 -> {
                            mText = "$mText<"
                            MRZ.add(mText)
                            Log.w(TAG, "ADDED SURGERY STRING TO STACK $mText")
                            Log.w(TAG, "FAILED SURGERY $mText")
                        }
                        else -> Log.w(TAG, "FAILED SURGERY $mText")
                    }
                } else {
                    runtimeString = mText
                }
            }
        }
        return MRZ
    }

    /**
     *   processes TD1 MRZ format in [ArrayList]<[String]> and returns MRZ data
     *
     * @param mrz [ArrayList]<[String]> containing MRZ lines
     * @return [HashMap]<[String], [String]> of MRZ data
     */
    private fun processLineTD1(mrz: ArrayList<String>): HashMap<String, String> {
        val ID_TYPE: String
        val ISSUE_COUNTRY: String
        val DOCUMENT_NUMBER: String
        val OPTIONAL_INFO: String
        val DOB: String
        val GENDER: String
        val EXPIRATION: String
        val NATIONALITY: String
        val SURNAME: String
        val GIVEN_NAME: String
        // disable unchecked warning because of unsafe type cast, (expected usage will always be (string,string)
        val finalMap = HashMap<String, String>()
        // PROCESSES TD1 from https://en.wikipedia.org/wiki/Machine-readable_passport
        if (mrz.size == 3) { // handle line 1 here
            val line1 = mrz[0]
            if (line1.length == 30) { // get Document Type
                ID_TYPE = line1.substring(0, 2).replace("<".toRegex(), "D")
                // get Issuing Country
                ISSUE_COUNTRY = line1.substring(2, 5).replace("<".toRegex(), "D")
                //get Document Number
                DOCUMENT_NUMBER = line1.substring(5, 14).replace("<".toRegex(), "")
                // get Optional Info
                OPTIONAL_INFO = line1.substring(15, 30).replace("<".toRegex(), "")
            } else {
                throw RuntimeException("BAD OCR TYPE$line1")
            }
            // handle line 2 here
            val line2 = mrz[1]
            if (line2.length == 30) { // Get Date of Birth
                DOB = line2.substring(0, 6)
                // Get GENDER
                GENDER = line2.substring(7, 8).replace("<".toRegex(), "Unspecified")
                // Get Expiration Date
                EXPIRATION = line2.substring(8, 14)
                // Get Nationality
                NATIONALITY = line2.substring(15, 18).replace("<".toRegex(), "")
            } else {
                throw RuntimeException("BAD OCR $line2")
            }
            // Handle Line 3 Here
            val line3 = mrz[2]
            if (line3.length == 30) {
                val names = line3.split("<<").toTypedArray()
                // Get SURNAME
                SURNAME = names[0].replace("<".toRegex(), "").trim { it <= ' ' }
                // GET Given Name
                GIVEN_NAME = names[1].replace("<".toRegex(), " ").trim { it <= ' ' }
            } else {
                throw RuntimeException("BAD OCR $line3")
            }
        } else {
            throw RuntimeException("BAD OCR $mrz")
        }
        // Pack all objects into HASHMAP
        finalMap["ID_TYPE"] = ID_TYPE
        finalMap["ISSUE_COUNTRY"] = ISSUE_COUNTRY
        finalMap["DOCUMENT_NUMBER"] = DOCUMENT_NUMBER
        finalMap["EXPIRATION"] = EXPIRATION
        finalMap["SURNAME"] = SURNAME
        finalMap["GIVEN_NAME"] = GIVEN_NAME
        finalMap["GENDER"] = GENDER
        finalMap["DOB"] = DOB
        finalMap["NATIONALITY"] = NATIONALITY
        finalMap["OPTIONAL_INFO"] = OPTIONAL_INFO
        Log.i(TAG, "Created FINAL MAP")
        return finalMap
    }

    /**
     *   processes TD3 MRZ format in [ArrayList]<[String]> and returns MRZ data
     *
     * @param mrz [ArrayList]<[String]> containing MRZ lines
     * @return [HashMap]<[String], [String]> of MRZ data
     */
    private fun processLineTD3(mrz: ArrayList<String>): HashMap<String, String> {
        var ID_TYPE: String
        val ISSUE_COUNTRY: String
        val DOCUMENT_NUMBER: String
        val OPTIONAL_INFO: String
        val DOB: String
        val GENDER: String
        val EXPIRATION: String
        val NATIONALITY: String
        val SURNAME: String
        val GIVEN_NAME: String
        // disable unchecked warning because of unsafe type cast, (expected usage will always be (string,string)
        val FinalMap = HashMap<String, String>()

        // PROCESSES TD3 from https://en.wikipedia.org/wiki/Machine-readable_passport
        if (mrz.size == 2) { // handle line 1 here
            val line_1 = mrz[0]
            if (line_1.length == 44) { // get Document Type
                ID_TYPE = line_1.substring(0, 2).replace("<".toRegex(), "")
                if (ID_TYPE == "P") {
                    ID_TYPE = "Passport"
                }
                // get Issuing Country
                ISSUE_COUNTRY = line_1.substring(2, 5).replace("<".toRegex(), "D")
                val names =
                        line_1.substring(5, 44).split("<<").toTypedArray()
                // Get SURNAME
                SURNAME =
                        names[0].replace("<".toRegex(), "").trim { it <= ' ' }.toUpperCase(Locale.ROOT)
                // GET Given Name
                GIVEN_NAME =
                        names[1].replace("<".toRegex(), " ").trim { it <= ' ' }.toUpperCase(Locale.ROOT)
            } else {
                throw RuntimeException("BAD OCR TYPE$line_1")
            }
            // handle line 2 here
            val line_2 = mrz[1]
            if (line_2.length == 44) { //get Document Number
                DOCUMENT_NUMBER = line_2.substring(0, 9).replace("<".toRegex(), "")
                // Get Nationality
                NATIONALITY = line_2.substring(10, 13).replace("<".toRegex(), "")
                // Get Date of Birth
                DOB = line_2.substring(13, 19)
                // Get GENDER
                GENDER = line_2.substring(20, 21).replace("<".toRegex(), "Unspecified")
                // Get Expiration Date
                EXPIRATION = line_2.substring(21, 27)
                // get Optional Info
                OPTIONAL_INFO = line_2.substring(27, 42).replace("<".toRegex(), "")
            } else {
                throw RuntimeException("BAD OCR $line_2")
            }
        } else {
            throw RuntimeException("BAD OCR $mrz")
        }
        // Pack all objects into HASHMAP
        FinalMap["ID_TYPE"] = ID_TYPE
        FinalMap["ISSUE_COUNTRY"] = ISSUE_COUNTRY
        FinalMap["DOCUMENT_NUMBER"] = DOCUMENT_NUMBER
        FinalMap["EXPIRATION"] = EXPIRATION
        FinalMap["SURNAME"] = SURNAME
        FinalMap["GIVEN_NAME"] = GIVEN_NAME
        FinalMap["GENDER"] = GENDER
        FinalMap["DOB"] = DOB
        FinalMap["NATIONALITY"] = NATIONALITY
        FinalMap["OPTIONAL_INFO"] = OPTIONAL_INFO
        Log.i(TAG, "Created FINAL MAP")
        return FinalMap
    }

    /**
     *  Checks if [ArrayList] passes normal specifications
     *
     * This is very inconsistent and should not be relied on it also only supports old SUR ID's
     *
     * @param OCR_lines Lines from OCR to check for Compatibility
     * @return [ArrayList] of compatibility lines for MRZ
     */
    private fun checkForOldID(OCR_lines: ArrayList<String>): ArrayList<String> {
        val MRZ = ArrayList<String>()
        MRZ.clear()
        Log.i(TAG, "Starting FALLBACK MODE")
        val validId: Boolean
        var country = false
        var countryId = false
        var city = false
        // check if string contains SURINAME, SME and PRBO if not boolean false
        Log.w(TAG, "List when checking: $OCR_lines")
        for (text in OCR_lines) {
            if (text.contains("SURINAME")) {
                country = true
            }
        }
        for (text in OCR_lines) {
            if (text.contains("SME")) {
                countryId = true
            }
        }
        for (text in OCR_lines) {
            if (text.contains("PRBO")) {
                city = true
            }
        }
        validId = country && countryId && city
        if (validId) {
            for (text in OCR_lines) {
                if (text.contains("SURINAME")) {
                    MRZ.clear()
                    MRZ.add(text)
                } else {
                    MRZ.add(text)
                }
            }
        }
        return MRZ
    }

    /**
     *   processes OLD SUR ID format in [ArrayList]<[String]> and returns extracted  data
     *
     * @param mrz [ArrayList]<[String]> containing MRZ lines
     * @return [HashMap]<[String], [String]> of MRZ data
     */
    private fun processOldID(mrz: ArrayList<String>): HashMap<String, String> { //        foo
        Log.w(TAG, "LINES SIZE FOUND: " + mrz.size)
        Log.w(TAG, "LINES FOUND: $mrz")

        val ID_TYPE: String
        val ISSUE_COUNTRY: String
        var DOCUMENT_NUMBER: String
        val OPTIONAL_INFO: String
        var DOB = ""
        var GENDER: String
        val EXPIRATION: String
        var NATIONALITY = ""
        var SURNAME: String
        var GIVEN_NAME: String

        // disable unchecked warning because of unsafe type cast, (expected usage will always be (string,string)
        val finalMap = HashMap<String, String>()

        //check line size
        if (mrz.size < 3) {
            return finalMap
        }

        // PROCESSES OLD ID's according to
        // [- SURINAME-, 9 15, FR 002796 M, Baidjnath, Selby J, SME, 12 08 1999, PRBO, LATB| C, DE, DES, JSmA]
        // handle line 1 here
        // suriname always
        val line1 = mrz[0]
        // Issue Date
        val line_2 = mrz[1]
        // DOC NUM
        val line_3 = mrz[2]
        // SURNAME
        val line4 = mrz[3]
        // GIVEN NAMES
        val line5 = mrz[4]
        //NATIONALITY
        val line_6 = mrz[5]
        // B-DAY
        val line7 = mrz[6]
        //CITY
        val line_8 = mrz[7]
        if (line1.toUpperCase(Locale.ROOT).contains("SURINAME")) { // get Document Type
            ID_TYPE = "Old ID"
            // get Issuing Country
            ISSUE_COUNTRY = line1.replace("-".toRegex(), "").trim { it <= ' ' }
            //get Document Number
//            (DOCUMENT_NUMBER.length() - 1);
//            if (!GENDER.equals("M") && !GENDER.equals("V") && !GENDER.equals("F")) {
            DOCUMENT_NUMBER = mrz[4].trim { it <= ' ' }
            Log.w(TAG, "GOING WITH DN FOR: $DOCUMENT_NUMBER")
            //            String tX = DOCUMENT_NUMBER.substring(DOCUMENT_NUMBER.length() - 1);
//            if (!containsDigit(DOCUMENT_NUMBER)) {
//                if (!DOCUMENT_NUMBER.startsWith("F")) {
//                    Log.w(TAG, "WE GOT A FALSE");
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
//                    Log.w(TAG, "DIDN'T FIND ANY PROBLEMS WITH DOCUMENT NUMBER for normal standard");
//                    // fallback for (old-oldID, or when it fetches expiration on same line as doc id)
//                    if (!containsDigit(DOCUMENT_NUMBER)) {
//                        if (!DOCUMENT_NUMBER.contains("F")) {
//                            Log.w(TAG, "WE GOT A TRUE");
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
                DOCUMENT_NUMBER = ""
                for (i in mrz.indices) {
                    val line = mrz[i]
                    if (line.startsWith("F")) {
                        val tZ = line.substring(line.length - 1)
                        if (tZ.toUpperCase(Locale.ROOT) == "M" || tZ.toUpperCase(Locale.ROOT) == "V" || tZ.toUpperCase(
                                        Locale.ROOT
                                ) == "F"
                        ) {
                            DOCUMENT_NUMBER = line.trim { it <= ' ' }
                            Log.i(TAG, "Going with$DOCUMENT_NUMBER")
                        }
                    } else if (line.startsWith("1") || line.startsWith("0")) {
                        if (line.length >= 8) {
                            val x = line.substring(5)
                            if (x.trim { it <= ' ' }.startsWith("F")) {
                                Log.w(TAG, "found $x")
                                DOCUMENT_NUMBER = x
                                break
                            }
                        }
                    }
                }
            } else if (DOCUMENT_NUMBER.contains("SME")) {
                DOCUMENT_NUMBER = ""
                for (i in mrz.indices) {
                    val line = mrz[i]
                    if (line.startsWith("F")) {
                        val tZ = line.substring(line.length - 1)
                        if (tZ.toUpperCase(Locale.ROOT) == "M" || tZ.toUpperCase(Locale.ROOT) == "V" || tZ.toUpperCase(
                                        Locale.ROOT
                                ) == "F"
                        ) {
                            DOCUMENT_NUMBER = line.trim { it <= ' ' }
                            Log.i(TAG, "Going with$DOCUMENT_NUMBER")
                        }
                    } else if (line.startsWith("1") || line.startsWith("0")) {
                        if (line.length >= 8) {
                            val x = line.substring(5)
                            Log.w(TAG, "found $x")
                            DOCUMENT_NUMBER = x
                        }
                    }
                }
            }
            OPTIONAL_INFO = ""
            // Get Date of Birth
            if (line7.length == 10) {
                DOB = line7.trim { it <= ' ' }
            } else {
                for (i in mrz.indices) {
                    val line = mrz[i]
                    if (line.length == 10) {
                        if (line.matches("^\\d{1,2}[\\s]\\d{2}[\\s]\\d{4}$".toRegex())) {
                            Log.i(TAG, "Going with $DOB")
                            DOB = line.trim { it <= ' ' }
                        }
                    }
                }
            }
            // fallback for old standard
            if (DOB.length < 10) {
                for (i in mrz.indices) {
                    var line = mrz[i]
                    if (line.length > 13) { // SME 10 09 1991
                        if (line.trim { it <= ' ' }.startsWith("SME")) {
                            line = line.substring(3)
                            if (line.trim { it <= ' ' }.matches("^\\d{1,2}[\\s]\\d{2}[\\s]\\d{4}$".toRegex())) {
                                DOB = line.trim { it <= ' ' }
                                Log.i(TAG, "Going with$DOB")
                            } else if (line.contains("PRBO")) {
                                line = line.substring(0, line.length - 4).trim { it <= ' ' }
                                if (line.trim { it <= ' ' }.matches("^\\d{1,2}[\\s]\\d{2}[\\s]\\d{4}$".toRegex())) {
                                    DOB = line.trim { it <= ' ' }
                                    Log.i(TAG, "Going with$DOB")
                                }
                            }
                        } else if (line.contains("PRBO")) {
                            line = line.substring(0, line.length - 4).trim { it <= ' ' }
                            if (line.trim { it <= ' ' }.matches("^\\d{1,2}[\\s]\\d{2}[\\s]\\d{4}$".toRegex())) {
                                DOB = line.trim { it <= ' ' }
                                Log.i(TAG, "Going with$DOB")
                            }
                        }
                    }
                }
            }
            //fallback in case of 0
            if (DOB.length < 10) {
                for (i in mrz.indices) {
                    var line = mrz[i]
                    if (line.length > 13) { // SME 10 09 1991
                        if (containsDigit(line)) {
                            line = line.substring(0, line.length - 4).trim { it <= ' ' }
                            if (line.trim { it <= ' ' }.matches("^\\d{1,2}[\\s]\\d{2}[\\s]\\d{4}$".toRegex())) {
                                DOB = line.trim { it <= ' ' }
                                Log.i(TAG, "Going with$DOB")
                            }
                        }
                    }
                }
            }
            // Get GENDER
            GENDER = DOCUMENT_NUMBER.substring(DOCUMENT_NUMBER.length - 1)
            if (GENDER != "M" && GENDER != "V" && GENDER != "F") {
                for (line in mrz) {
                    if (line.length >= 8) {
                        val x = line.substring(line.length - 1)
                        if (x.toUpperCase(Locale.ROOT) == "M" || x.toUpperCase(Locale.ROOT) == "V" || x.toUpperCase(
                                        Locale.ROOT
                                ) == "F"
                        ) {
                            GENDER = x
                            Log.i(TAG, "Going with$GENDER")
                        }
                    }
                }
            }
            // Get Expiration Date
            EXPIRATION = "Expired"
            // Get Nationality
//            NATIONALITY =line_6.trim();
            for (line in mrz) {
                if (line.contains("SME")) {
                    NATIONALITY = "SME"
                }
            }
            // Get SURNAME
            SURNAME = line4.trim { it <= ' ' }
            if (SURNAME.length < 2) {
                if (containsDigit(SURNAME)) {
                    if (!SURNAME.matches("([A-Z][a-zA-Z]*)".toRegex())) {
                        for (line in mrz) {
                            if (line.matches("([A-Z][a-z]*)".toRegex()) && line.length >= 3 && line != DOCUMENT_NUMBER && !line.contains(
                                            "SME"
                                    ) && !line.contains("PRBO")
                            ) {
                                SURNAME = line
                                break
                            }
                        }
                    }
                }
            }
            // GET Given Name
            GIVEN_NAME = line5.trim { it <= ' ' }
            if (containsDigit(GIVEN_NAME)) {
                if (!GIVEN_NAME.matches("([A-Z][a-z]*)".toRegex())) {
                    for (line in mrz) {
                        if (line.matches("([A-Z][a-z]*)".toRegex()) && line.length >= 3) {
                            if (line != SURNAME && line != DOCUMENT_NUMBER && !line.contains("SME") && !line.contains(
                                            "PRBO"
                                    )
                            ) {
                                if (!line.contains(SURNAME)) {
                                    GIVEN_NAME = line
                                }
                            }
                        }
                    }
                }
            }
            if (GIVEN_NAME.contains(SURNAME)) {
                for (line in mrz) {
                    if (line.matches("([A-Z][a-z]*)".toRegex()) && line.length >= 3) {
                        if (line != SURNAME && line != DOCUMENT_NUMBER && !line.contains("SME") && !line.contains(
                                        "PRBO"
                                )
                        ) {
                            if (!line.contains(SURNAME)) {
                                GIVEN_NAME = line
                            }
                        }
                    }
                }
            }
            // Pack all objects into HASH MAP
            finalMap["ID_TYPE"] = ID_TYPE
            finalMap["ISSUE_COUNTRY"] = ISSUE_COUNTRY
            finalMap["DOCUMENT_NUMBER"] = DOCUMENT_NUMBER
            finalMap["EXPIRATION"] = EXPIRATION
            finalMap["SURNAME"] = SURNAME
            finalMap["GIVEN_NAME"] = GIVEN_NAME
            finalMap["GENDER"] = GENDER
            finalMap["DOB"] = DOB
            finalMap["NATIONALITY"] = NATIONALITY
            finalMap["OPTIONAL_INFO"] = OPTIONAL_INFO
            Log.i(TAG, "Created FINAL MAP")
        }
        return finalMap
    }

    /**
     *   checks if [String] contains a digit of type [Int]
     *
     * @param string [String] to validate
     *
     * @return [Boolean] true if contains digit, false otherwise
     *
     * @author Rex Serubii
     */
    private fun containsDigit(string: String?): Boolean {
        var containsDigit = false
        if (string != null && string.isNotEmpty()) {
            for (c in string.toCharArray()) {
                if (Character.isDigit(c).also { containsDigit = it }) {
                    break
                }
            }
        }
        Log.w(TAG, "Checked If Digit in $string with result $containsDigit")
        return containsDigit
    }


}
