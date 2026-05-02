package com.magenplay002.app.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magenplay002.app.util.FileUtils
import com.magenplay002.app.util.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoListViewModel : ViewModel() {

    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage

    fun loadVideos(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = ""
            try {
                val videoList = withContext(Dispatchers.IO) {
                    try {
                        FileUtils.getAllVideos(context)
                    } catch (e: SecurityException) {
                        Log.e("VideoListVM", "Permission denied", e)
                        _errorMessage.value = "Storage permission denied. Please grant permission."
                        emptyList()
                    } catch (e: Exception) {
                        Log.e("VideoListVM", "Error loading videos", e)
                        _errorMessage.value = "Error loading videos: ${e.message}"
                        emptyList()
                    }
                }
                _videos.value = videoList
            } catch (e: Exception) {
                Log.e("VideoListVM", "Unexpected error", e)
                _errorMessage.value = "Unexpected error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun search(query: String) {
        _searchQuery.value = query
    }

    fun clearError() {
        _errorMessage.value = ""
    }
}
