package com.serubii.zonereader.room.repositories;

import android.app.Application;
import android.os.AsyncTask;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.serubii.zonereader.room.dao.DocumentDao;
import com.serubii.zonereader.room.database.MRZDatabase;
import com.serubii.zonereader.room.entities.Document;

import java.util.List;

public class DocumentRepository {

    private DocumentDao dao;
    private LiveData<List<Document>> allItemsScanDesc;


    // Note that in order to unit test the Repository, you have to remove the Application
    // dependency. This adds complexity and much more code, and this sample is not about testing.
    // See the BasicSample in the android-architecture-components repository at
    // https://github.com/googlesamples
    public DocumentRepository(Application application) {
        MRZDatabase database = MRZDatabase.getDatabase(application);
        dao = database.documentDao();
        allItemsScanDesc = dao.getAllItemsByScanTimeDesc();
    }

    public LiveData<List<Document>> getAllItemsByScanTimeDesc() {
        return allItemsScanDesc;
    }

    public void insert(Document item) {
        new DocumentRepository.insertAsyncTask(dao).execute(item);
    }

    public void update(Document item) {
        new DocumentRepository.updateAsyncTask(dao).execute(item);
    }


    // You must call this on a non-UI thread or your app will throw an exception. Room ensures
    // that you're not doing any long running operations on the main thread, blocking the UI.
    private static class insertAsyncTask extends AsyncTask<Document, Void, Void> {

        private DocumentDao asyncDao;

        insertAsyncTask(DocumentDao dao) {
            this.asyncDao = dao;
        }


        @Override
        protected Void doInBackground(final Document... params) {
            Log.d("TEST", "doInBackground: " + params[0]);
            asyncDao.insert(params[0]);

            return null;
        }

    }

    private static class updateAsyncTask extends AsyncTask<Document, Void, Void> {

        private DocumentDao documentDao;

        private updateAsyncTask(DocumentDao documentDao) {
            this.documentDao = documentDao;
        }

        @Override
        protected Void doInBackground(Document... locations) {
            Document location = locations[0];
            documentDao.update(location.getId(), location.getId_type(), location.getIssuing_country(), location.getDocument_number(), location.getExpiration(), location.getSurname(), location.getGiven_name(), location.getGender(), location.getDate_of_birth(), location.getNationality(), location.getOptional_info(), location.getInitial_uri(), location.getFront_uri(), location.getBack_uri(), location.getScan_time());
            return null;
        }


    }
}
