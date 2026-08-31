package com.muse.app.di

import android.content.Context
import com.muse.app.data.ArchiveRepository
import com.muse.app.data.CreateRepository
import com.muse.app.data.Curator
import com.muse.app.data.MediaRepository
import com.muse.app.data.MuseStore

class AppContainer(context: Context) {
    val store = MuseStore(context)
    val media = MediaRepository(context.contentResolver)
    val curator = Curator(context, media, store)
    val archive = ArchiveRepository(store)
    val creations = CreateRepository(store)
}
