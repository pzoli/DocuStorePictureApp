package hu.infokristaly.docustorepictureapp.utils

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hu.infokristaly.docustorepictureapp.model.DocInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ItemViewModel : ViewModel() {
    var currentPage = 1
    private var isLoading = false
    val items = MutableLiveData<List<DocInfo>>()
    var userName: String = ""
    var password: String = ""
    var serverAddress: String = ""

    fun loadNextPage(currentSearchTerm: String) {
        if (isLoading) return

        isLoading = true
        viewModelScope.launch {
            val newItems = if (currentSearchTerm.isEmpty()) fetchItemsFromApi(currentPage) else fetchFilteredItemsFromApi(currentSearchTerm,currentPage)
            if (newItems.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    val currentList = items.value.orEmpty().toMutableList()
                    currentList.addAll(newItems)
                    items.value = currentList
                    currentPage++
                }
            }
            isLoading = false
        }
    }

    private suspend fun fetchItemsFromApi(page: Int): List<DocInfo> = withContext(Dispatchers.IO) {
        try {
            return@withContext ApiRoutins.getDocInfos(userName, password, serverAddress, page).get()
        } catch (e:Exception) {
            return@withContext listOf()
        }
    }

    private suspend fun fetchFilteredItemsFromApi(filter: String, page: Int): List<DocInfo> = withContext(Dispatchers.IO) {
        try {
            return@withContext ApiRoutins.getFilteredDocInfos(userName, password, serverAddress, filter, page).get()
        } catch (e:Exception) {
            return@withContext listOf()
        }
    }

}