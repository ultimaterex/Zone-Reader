package com.serubii.zonereader.room.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.serubii.zonereader.room.dao.DocumentDao;
import com.serubii.zonereader.room.entities.Document;


@Database(entities = {
        Document.class
},
        version = 1, exportSchema = false)
public abstract class MRZDatabase extends RoomDatabase {

    private static final String database_name = "mrz_app_database";

    // Singleton to make sure we only have 1 database running at a time ;-;
    private static volatile MRZDatabase INSTANCE;


    public static MRZDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (MRZDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            MRZDatabase.class, database_name).build();
                }
            }
        }
        return INSTANCE;
    }

    public abstract DocumentDao documentDao();

}
