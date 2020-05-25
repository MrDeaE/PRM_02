package com.example.prm_02

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.prm_02.db.AppDatabase
import com.example.prm_02.db.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel : ViewModel() {
    val imageObjects = MutableLiveData<List<Image>>()

    fun launchSelectFromDb(context: Context) {
        viewModelScope.launch {
            selectFromDb(context)
        }
    }

    suspend fun selectFromDb(context: Context) = withContext(Dispatchers.Default) {
        val db = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration()
        .build()
        val dao = db.imageDao()

        imageObjects.postValue(dao.getAll())
    }

}