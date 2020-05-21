package com.serubii.zonereader.room.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.serubii.zonereader.room.entities.Document;
import com.serubii.zonereader.room.repositories.DocumentRepository;

import java.util.List;

public class DocumentViewModel extends AndroidViewModel {

    private DocumentRepository repository;
    private LiveData<List<Document>> allItemsScanDesc;

    public DocumentViewModel(@NonNull Application application) {
        super(application);

        repository = new DocumentRepository(application);
        allItemsScanDesc = repository.getAllItemsByScanTimeDesc();

    }


    public LiveData<List<Document>> getAllItemsByScanTimeDesc() {return allItemsScanDesc;}

    public void insert(Document item) {
        Log.d("TEST", "insert: " + item);
        repository.insert(item);
    }

    public void update(Document item) {repository.update(item);}


}
