package com.muse.app.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muse.app.data.CreateRepository
import com.muse.app.data.model.PaletteRecord
import com.muse.app.data.model.PostcardRecord
import com.muse.app.data.model.ZineRecord
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class CreateHomeViewModel(repository: CreateRepository) : ViewModel() {
    data class Ui(
        val postcards: List<PostcardRecord>,
        val palettes: List<PaletteRecord>,
        val zines: List<ZineRecord>,
    )

    val ui: StateFlow<Ui> = combine(repository.postcards, repository.palettes, repository.zines) { p, l, z ->
        Ui(p, l, z)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Ui(emptyList(), emptyList(), emptyList()))
}
