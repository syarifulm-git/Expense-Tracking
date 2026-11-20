package com.example.expensetracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExpenseViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDb.get(app).dao()
    val expenses: StateFlow<List<Expense>> =
        dao.all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(e: Expense) = viewModelScope.launch { dao.insert(e) }
    fun update(e: Expense) = viewModelScope.launch { dao.update(e) }
    fun delete(e: Expense) = viewModelScope.launch { dao.delete(e) }
}
