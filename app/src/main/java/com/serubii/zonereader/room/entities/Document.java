package com.serubii.zonereader.room.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "document")
public class Document {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private Integer id;

    @ColumnInfo(name = "id_type")
    private String id_type;

    @ColumnInfo(name = "issuing_country")
    private String issuing_country;

    @ColumnInfo(name = "document_number")
    private String document_number;

    @ColumnInfo(name = "expiration")
    private String expiration;

    @ColumnInfo(name = "surname")
    private String surname;

    @ColumnInfo(name = "given_name")
    private String given_name;

    @ColumnInfo(name = "gender")
    private String gender;

    @ColumnInfo(name = "date_of_birth")
    private String date_of_birth;

    @ColumnInfo(name = "nationality")
    private String nationality;

    @ColumnInfo(name = "optional_info")
    private String optional_info;

    @ColumnInfo(name = "initial_uri")
    private String initial_uri;

    @ColumnInfo(name = "front_uri")
    private String front_uri;


    @ColumnInfo(name = "back_uri")
    private String back_uri;

    @ColumnInfo(name = "scan_time")
    private Long scan_time;


    /*
    Constructor getters for every Column
    */
    public Document(Integer id, String id_type, String issuing_country, String document_number, String expiration, String surname, String given_name, String gender, String date_of_birth, String nationality, String optional_info, String initial_uri, String front_uri, String back_uri, Long scan_time) {
        this.id = id;
        this.id_type = id_type;
        this.issuing_country = issuing_country;
        this.document_number = document_number;
        this.expiration = expiration;
        this.surname = surname;
        this.given_name = given_name;
        this.gender = gender;
        this.date_of_birth = date_of_birth;
        this.nationality = nationality;
        this.optional_info = optional_info;
        this.initial_uri = initial_uri;
        this.front_uri = front_uri;
        this.back_uri = back_uri;
        this.scan_time = scan_time;
    }

    /*
   Generated getters for every Column
    */

    public Integer getId() {
        return id;
    }

    public String getId_type() {
        return id_type;
    }

    public String getIssuing_country() {
        return issuing_country;
    }

    public String getDocument_number() {
        return document_number;
    }

    public String getExpiration() {
        return expiration;
    }

    public String getSurname() {
        return surname;
    }

    public String getGiven_name() {
        return given_name;
    }

    public String getGender() {
        return gender;
    }

    public String getDate_of_birth() {
        return date_of_birth;
    }

    public String getNationality() {
        return nationality;
    }

    public String getOptional_info() {
        return optional_info;
    }

    public String getInitial_uri() {
        return initial_uri;
    }

    public String getFront_uri() {
        return front_uri;
    }

    public Long getScan_time() {
        return scan_time;
    }

    public String getBack_uri() {
        return back_uri;
    }
}
