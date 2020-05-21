package com.serubii.zonereader.helpers.mrz

import com.serubii.zonereader.helpers.*
import java.util.*

/**
 *
 * MRZ class
 *
 *
 * @
 *
 * @property mrzType String
 * @property mrzKey String
 * @property idType String
 * @property issuingCountry String
 * @property documentNumber String
 * @property expiration String
 * @property surname String
 * @property givenName String
 * @property gender String
 * @property dateOfBirth String
 * @property nationality String
 * @property optionalInfo String
 * @constructor
 *
 * @author Rex Serubii
 */
class MRZ(mrz_code: String) {

    @Suppress("JoinDeclarationAndAssignment")
    private val mrzType: String
    private lateinit var mrzKey: String
    private lateinit var idType: String
    private lateinit var issuingCountry: String
    private lateinit var documentNumber: String
    private lateinit var expiration: String
    private lateinit var surname: String
    private lateinit var givenName: String
    private lateinit var gender: String
    private lateinit var dateOfBirth: String
    private lateinit var nationality: String
    private lateinit var optionalInfo: String

    val map = HashMap<String, String>()


    /**
     * Initialization checks if the code is a valid MRZ
     */
    init {
        mrzType = when (mrz_code.length) {
            90 -> {
                // TD1 has 3x30
                "TD1"
            }
            88 -> {
                // TD3 has 2x44
                "TD3"
            }
            71 -> {
                // TD 2 has 2x36
                "TD2"
            }
            else -> {
                throw InvalidMRZCodeException("Not a valid MRZ Code")
            }
        }
        buildVariables(mrz_code)
    }


    /**
     * Builds variables for the other methods for the formats
     *
     * @param mrz_code String
     */
    private fun buildVariables(mrz_code: String) {
        mrzKey = mrz_code
        when (mrzType) {
            "TD1" -> {
                // split MRZ into three lines:
                val line1 = mrz_code.substring(0, 30)
                val line2 = mrz_code.substring(30, 60)
                val line3 = mrz_code.substring(60, 90)

                // PROCESSES TD1 from https://en.wikipedia.org/wiki/Machine-readable_passport
                if (line1.length == 30) { // get Document Type
                    idType = line1.substring(0, 2).replace("<".toRegex(), "")
                    // get Issuing Country
                    issuingCountry = line1.substring(2, 5).replace("<".toRegex(), "")
                    //get Document Number
                    documentNumber = line1.substring(5, 14).replace("<".toRegex(), "")
                    // get Optional Info
                    optionalInfo = line1.substring(15, 30).replace("<".toRegex(), "")
                } else {
                    throw InvalidMRZCodeException(
                            "BAD OCR TYPE$line1"
                    )
                }
                // handle line 2 here
                if (line2.length == 30) { // Get Date of Birth
                    dateOfBirth = line2.substring(0, 6)
                    // Get GENDER
                    gender = line2.substring(7, 8).replace("<".toRegex(), "Unspecified")
                    // Get Expiration Date
                    expiration = line2.substring(8, 14)
                    // Get Nationality
                    nationality = line2.substring(15, 18).replace("<".toRegex(), "")
                } else {
                    throw InvalidMRZCodeException(
                            "BAD OCR $line2"
                    )
                }
                // Handle Line 3 Here
                if (line3.length == 30) {
                    val names = line3.split("<<").toTypedArray()
                    // Get SURNAME
                    surname = names[0].replace("<".toRegex(), "").trim { it <= ' ' }
                    // GET Given Name
                    givenName = names[1].replace("<".toRegex(), " ").trim { it <= ' ' }
                } else {
                    throw InvalidMRZCodeException(
                            "BAD OCR $line3"
                    )
                }

            }
            "TD2" -> {
                // split MRZ into two lines:
                val line1 = mrz_code.substring(0, 36)
                val line2 = mrz_code.substring(36, 72)

                // PROCESSES TD2 from https://en.wikipedia.org/wiki/Machine-readable_passport
                if (line1.length == 36) {
                    // get Document Type
                    idType = line1.substring(0, 2).replace("<".toRegex(), "")
                    // get Issuing Country
                    issuingCountry = line1.substring(2, 5).replace("<".toRegex(), "")

                    val names =
                            line1.substring(5, 36).split("<<").toTypedArray()
                    // Get Surname
                    surname =
                            names[0].replace("<".toRegex(), "").trim { it <= ' ' }.toUpperCase(
                                    Locale.ROOT
                            )
                    // GET Given Name
                    givenName =
                            names[1].replace("<".toRegex(), " ").trim { it <= ' ' }
                                    .toUpperCase(Locale.ROOT)
                } else {
                    throw RuntimeException("BAD OCR TYPE$line1")
                }
                // handle line 2 here
                if (line2.length == 36) {
                    //get Document Number
                    documentNumber = line2.substring(0, 9).replace("<".toRegex(), "")
                    // Get Nationality
                    nationality = line2.substring(10, 13).replace("<".toRegex(), "")
                    // Get Date of Birth
                    dateOfBirth = line2.substring(13, 19)
                    // Get GENDER
                    gender = line2.substring(20, 21).replace("<".toRegex(), "Unspecified")
                    // Get Expiration Date
                    expiration = line2.substring(21, 27)
                    // get Optional Info
                    optionalInfo = line2.substring(27, 36).replace("<".toRegex(), "")
                } else {
                    throw InvalidMRZCodeException(
                            "BAD OCR $line2"
                    )
                }
            }
            "TD3" -> {
                // split MRZ into two lines:
                val line1 = mrz_code.substring(0, 44)
                val line2 = mrz_code.substring(36, 88)

                // PROCESSES TD3 from https://en.wikipedia.org/wiki/Machine-readable_passport
                if (line1.length == 44) {
                    // get Document Type
                    idType = line1.substring(0, 2).replace("<".toRegex(), "")
                    // get Issuing Country
                    issuingCountry = line1.substring(2, 5).replace("<".toRegex(), "")

                    val names =
                            line1.substring(5, 44).split("<<").toTypedArray()
                    // Get Surname
                    surname =
                            names[0].replace("<".toRegex(), "").trim { it <= ' ' }.toUpperCase(
                                    Locale.ROOT
                            )
                    // GET Given Name
                    givenName =
                            names[1].replace("<".toRegex(), " ").trim { it <= ' ' }
                                    .toUpperCase(Locale.ROOT)
                } else {
                    throw RuntimeException("BAD OCR TYPE$line1")
                }
                // handle line 2 here
                if (line2.length == 44) {
                    //get Document Number
                    documentNumber = line2.substring(0, 9).replace("<".toRegex(), "")
                    // Get Nationality
                    nationality = line2.substring(10, 13).replace("<".toRegex(), "")
                    // Get Date of Birth
                    dateOfBirth = line2.substring(13, 19)
                    // Get GENDER
                    gender = line2.substring(20, 21).replace("<".toRegex(), "Unspecified")
                    // Get Expiration Date
                    expiration = line2.substring(21, 27)
                    // get Optional Info
                    optionalInfo = line2.substring(27, 42).replace("<".toRegex(), "")
                } else {
                    throw InvalidMRZCodeException(
                            "BAD OCR $line2"
                    )
                }


            }
        }
    }

    /**
     *  Returns the type of MRZ card
     * @return String
     */
    fun getMRZType(): String {
        return mrzType
    }

    /**
     * Returns the full MRZ code
     * @return String
     */
    fun getMRZCode(): String {
        return mrzKey
    }

    /**
     * Returns the Issuing Country
     * @return String
     */
    fun getIssuingCountry(): String {
        return issuingCountry
    }

    /** Returns the Document Number
     *
     * @return String
     */
    fun getDocumentNumber(): String {
        return documentNumber
    }

    /** Returns the Expiration Date
     *
     * @return String
     */
    fun getExpirationDate(): String {
        return expiration
    }

    /** Returns the Surname
     *
     * @return String
     */
    fun getSurname(): String {
        return surname
    }

    /** Returns the Given Name
     *
     * @return String
     */
    fun getGivenName(): String {
        return givenName
    }

    /** Returns the Gender
     *
     * @return String
     */
    fun getGender(): String {
        return gender
    }

    /** Returns the Date of Birth
     *
     * @return String
     */
    fun getDateOfBirth(): String {
        return dateOfBirth
    }

    /** Returns the Nationality
     *
     * @return String
     */
    fun getNationality(): String {
        return nationality
    }

    /** Returns any Optional Info
     *
     * @return String
     */
    fun getOptionalInfo(): String {
        return optionalInfo
    }

    /** Returns the entire collection as HashMap<String, String>
     *
     * @return HashMap<String, String>
     */
    fun getHashMap(): HashMap<String, String>{
        map[preference_code_id_type] =  idType
        map[preference_code_issue_country] = issuingCountry
        map[preference_code_expiration] =  expiration
        map[preference_code_surname] = surname
        map[preference_code_given_name] = givenName
        map[preference_code_gender] =  gender
        map[preference_code_dob] =  dateOfBirth
        map[preference_code_nationality] =  nationality
        map[preference_code_optional_info] = optionalInfo
        map[preference_code_mrz_code] =  mrzKey

        return map
    }
}

/**
 * This function is only ever returned when the MRZ is invalid
 * @constructor
 */
class InvalidMRZCodeException(message: String) : Exception(message) {

    private fun main(args: Array<String>) {
        throw InvalidMRZCodeException("Error!")            // >>> Exception in thread "main"
    }
}
