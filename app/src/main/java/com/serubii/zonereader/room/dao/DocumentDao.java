package com.serubii.zonereader.room.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.serubii.zonereader.room.entities.Document;

import java.util.List;

@Dao
public interface DocumentDao {

    // allow the insert of the same id multiple times by passing a conflict resolution strategy

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Document document);

    // update all (yikes)
    @Query("update document set id_type = :id_type, issuing_country = :issuing_country, document_number = :document_number, expiration = :expiration," +
            " surname = :surname, given_name = :given_name, gender = :gender, date_of_birth = :date_of_birth, nationality = :nationality, optional_info = :optional_info, " +
            "initial_uri = :initial_uri, front_uri = :front_uri, back_uri = :back_uri, scan_time = :scan_time where id = :id")
    void update(Integer id, String id_type, String issuing_country, String document_number, String expiration, String surname, String given_name, String gender, String date_of_birth,
                String nationality, String optional_info, String initial_uri, String front_uri, String back_uri, Long scan_time);

    @Query("select * from document order by scan_time desc")
    LiveData<List<Document>> getAllItemsByScanTimeDesc();



}
