package com.muse.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.muse.app.MuseApplication
import com.muse.app.R
import com.muse.app.ui.collection.CollectionScreen
import com.muse.app.ui.components.MuseBottomBar
import com.muse.app.ui.components.MuseTab
import com.muse.app.ui.create.CreateHomeScreen
import com.muse.app.ui.create.PhotoPickerScreen
import com.muse.app.ui.create.PostcardEditorScreen
import com.muse.app.ui.explore.ExploreScreen
import com.muse.app.ui.explore.ExhibitionCoverScreen
import com.muse.app.ui.explore.ExhibitionGalleryScreen
import com.muse.app.ui.palette.PaletteToolScreen
import com.muse.app.ui.permission.PermissionScreen
import com.muse.app.ui.permission.rememberPhotoPermissions
import com.muse.app.ui.theme.MuseColors
import com.muse.app.ui.viewer.PhotoViewerOverlay
import com.muse.app.ui.viewer.PhotoViewerState
import com.muse.app.ui.zine.ZineEditorScreen
import com.muse.app.ui.zine.ZinePickerScreen
import com.muse.app.ui.zine.ZineReaderScreen

object Routes {
    const val EXPLORE = "explore"
    const val COVER = "exhibition/{key}"
    const val GALLERY = "exhibition/{key}/gallery"
    const val CREATE = "create"
    const val PICKER = "picker/{mode}"
    const val EDITOR = "editor?photoId={photoId}&place={place}&postcardId={postcardId}"
    const val PALETTE_TOOL = "palette?photoId={photoId}&paletteId={paletteId}"
    const val COLLECTION = "collection"
    const val ZINE_EDITOR = "zine_editor?zineId={zineId}"
    const val ZINE_READER = "zine/{id}"

    fun cover(key: String) = "exhibition/$key"
    fun gallery(key: String) = "exhibition/$key/gallery"
    fun picker(mode: String) = "picker/$mode"

    fun editor(photoId: Long? = null, place: String? = null, postcardId: Long? = null): String {
        val p = photoId?.toString() ?: ""
        val pl = android.net.Uri.encode(place ?: "")
        val pc = postcardId?.toString() ?: ""
        return "editor?photoId=$p&place=$pl&postcardId=$pc"
    }

    fun paletteTool(photoId: Long? = null, paletteId: Long? = null): String {
        val p = photoId?.toString() ?: ""
        val pal = paletteId?.toString() ?: ""
        return "palette?photoId=$p&paletteId=$pal"
    }

    fun zineEditor(zineId: Long? = null): String {
        val z = zineId?.toString() ?: ""
        return "zine_editor?zineId=$z"
    }

    fun zineReader(id: Long) = "zine/$id"
}

@Composable
fun MuseRoot(application: MuseApplication) {
    val perms = rememberPhotoPermissions()
    if (perms.granted) {
        MuseApp(application)
    } else {
        PermissionScreen(perms)
    }
}

@Composable
private fun MuseApp(application: MuseApplication) {
    val container = application.container
    val nav = rememberNavController()
    val viewer = remember { PhotoViewerState() }

    val collected by container.archive.collectedPhotoIds
        .collectAsStateWithLifecycle(initialValue = emptySet())

    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val topLevel = route == Routes.EXPLORE || route == Routes.CREATE || route == Routes.COLLECTION

    Box(Modifier.fillMaxSize().background(MuseColors.Void)) {
        MuseNavHost(nav, application, viewer,
            onCreatePostcardFromViewer = { photo ->
                viewer.dismissImmediate()
                nav.navigate(Routes.editor(photoId = photo.id))
            },
            onExtractPaletteFromViewer = { photo ->
                viewer.dismissImmediate()
                nav.navigate(Routes.paletteTool(photoId = photo.id))
            }
        )

        AnimatedVisibility(
            visible = topLevel && viewer.request == null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(tween(200)) +
                slideInVertically(spring(dampingRatio = 0.8f, stiffness = 300f)) { it },
            exit = fadeOut(tween(160)) +
                slideOutVertically(tween(200)) { it }
        ) {
            MuseBottomBar(
                current = route,
                paper = route == Routes.CREATE,
                items = listOf(
                    MuseTab(Routes.EXPLORE, R.drawable.ic_vector_nav_exhibits),
                    MuseTab(Routes.CREATE, R.drawable.ic_vector_nav_create),
                    MuseTab(Routes.COLLECTION, R.drawable.ic_vector_nav_saved)
                ),
                onSelect = { target ->
                    nav.navigate(target) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        PhotoViewerOverlay(
            state = viewer,
            collectedIds = collected,
            onToggleCollect = { photo, contextTitle ->
                container.archive.togglePhoto(photo, contextTitle)
            },
            onCreatePostcard = { photo ->
                viewer.dismissImmediate()
                nav.navigate(Routes.editor(photoId = photo.id))
            },
            onExtractPalette = { photo ->
                viewer.dismissImmediate()
                nav.navigate(Routes.paletteTool(photoId = photo.id))
            }
        )
    }
}

@Composable
private fun MuseNavHost(
    nav: NavHostController,
    application: MuseApplication,
    viewer: PhotoViewerState,
    onCreatePostcardFromViewer: (com.muse.app.data.model.Photo) -> Unit,
    onExtractPaletteFromViewer: (com.muse.app.data.model.Photo) -> Unit,
) {
    NavHost(
        navController = nav,
        startDestination = Routes.EXPLORE,
        enterTransition = { fadeIn(tween(240)) },
        exitTransition = { fadeOut(tween(200)) },
        popEnterTransition = { fadeIn(tween(240)) },
        popExitTransition = { fadeOut(tween(200)) }
    ) {
        composable(Routes.EXPLORE) {
            ExploreScreen(
                application = application,
                onOpenViewer = { request -> viewer.open(request) }
            )
        }

        composable(
            Routes.COVER,
            arguments = listOf(navArgument("key") { type = NavType.StringType }),
            enterTransition = {
                slideInVertically(spring(dampingRatio = 0.75f, stiffness = 300f)) { it } +
                    fadeIn(tween(200))
            },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = {
                slideOutVertically(spring(dampingRatio = 0.75f, stiffness = 300f)) { it } +
                    fadeOut(tween(160))
            }
        ) { backStackEntry ->
            val key = backStackEntry.arguments?.getString("key").orEmpty()
            ExhibitionCoverScreen(
                application = application,
                key = key,
                onClose = { nav.popBackStack() },
                onEnterGallery = {
                    nav.navigate(Routes.gallery(key)) {
                        popUpTo(Routes.cover(key)) { inclusive = true }
                    }
                }
            )
        }

        composable(
            Routes.GALLERY,
            arguments = listOf(navArgument("key") { type = NavType.StringType }),
            enterTransition = {
                slideInVertically(spring(dampingRatio = 0.75f, stiffness = 280f)) { it } +
                    fadeIn(tween(180))
            },
            exitTransition = { fadeOut(tween(200)) },
            popExitTransition = {
                slideOutVertically(spring(dampingRatio = 0.75f, stiffness = 300f)) { it } +
                    fadeOut(tween(160))
            }
        ) { backStackEntry ->
            val key = backStackEntry.arguments?.getString("key").orEmpty()
            ExhibitionGalleryScreen(
                application = application,
                key = key,
                onClose = { nav.popBackStack() },
                onOpenViewer = { request -> viewer.open(request) }
            )
        }

        composable(Routes.CREATE) {
            CreateHomeScreen(
                application = application,
                onPickPostcard = { nav.navigate(Routes.picker("POSTCARD")) },
                onPickPalette = { nav.navigate(Routes.picker("PALETTE")) },
                onOpenPostcard = { id -> nav.navigate(Routes.editor(postcardId = id)) },
                onOpenPalette = { id -> nav.navigate(Routes.paletteTool(paletteId = id)) },
                onCreateZine = { nav.navigate(Routes.picker("ZINE")) },
                onOpenZine = { id -> nav.navigate(Routes.zineReader(id)) }
            )
        }

        composable(
            Routes.PICKER,
            arguments = listOf(navArgument("mode") { type = NavType.StringType })
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "POSTCARD"
            if (mode == "ZINE") {
                ZinePickerScreen(
                    application = application,
                    onClose = { nav.popBackStack() },
                    onDone = { ids ->
                        application.container.creations.pendingZineSelection.value = ids
                        nav.navigate(Routes.zineEditor()) {
                            popUpTo(Routes.CREATE)
                        }
                    }
                )
            } else {
                PhotoPickerScreen(
                    application = application,
                    mode = mode,
                    onClose = { nav.popBackStack() },
                    onPicked = { photoId ->
                        if (mode == "PALETTE") {
                            nav.navigate(Routes.paletteTool(photoId = photoId)) {
                                popUpTo(Routes.CREATE)
                            }
                        } else {
                            nav.navigate(Routes.editor(photoId = photoId)) {
                                popUpTo(Routes.CREATE)
                            }
                        }
                    }
                )
            }
        }

        composable(
            Routes.EDITOR,
            arguments = listOf(
                navArgument("photoId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                },
                navArgument("place") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                },
                navArgument("postcardId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val photoId = backStackEntry.arguments?.getString("photoId")?.toLongOrNull()
            val place = backStackEntry.arguments?.getString("place")
            val postcardId = backStackEntry.arguments?.getString("postcardId")?.toLongOrNull()
            PostcardEditorScreen(
                application = application,
                photoId = photoId,
                place = place,
                postcardId = postcardId,
                onClose = { nav.popBackStack() }
            )
        }

        composable(
            Routes.PALETTE_TOOL,
            arguments = listOf(
                navArgument("photoId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                },
                navArgument("paletteId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val photoId = backStackEntry.arguments?.getString("photoId")?.toLongOrNull()
            val paletteId = backStackEntry.arguments?.getString("paletteId")?.toLongOrNull()
            PaletteToolScreen(
                application = application,
                photoId = photoId,
                paletteId = paletteId,
                onClose = { nav.popBackStack() },
                onApplyToPostcard = { pid ->
                    nav.navigate(Routes.editor(photoId = pid)) {
                        popUpTo(Routes.CREATE)
                    }
                }
            )
        }

        composable(
            Routes.ZINE_EDITOR,
            arguments = listOf(
                navArgument("zineId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                }
            ),
            enterTransition = {
                slideInVertically(spring(dampingRatio = 0.75f, stiffness = 300f)) { it } +
                    fadeIn(tween(200))
            },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = {
                slideOutVertically(spring(dampingRatio = 0.75f, stiffness = 300f)) { it } +
                    fadeOut(tween(160))
            }
        ) { backStackEntry ->
            val zineId = backStackEntry.arguments?.getString("zineId")?.toLongOrNull()
            ZineEditorScreen(
                application = application,
                zineId = zineId,
                onClose = { nav.popBackStack() },
                onBound = { id ->
                    nav.navigate(Routes.zineReader(id)) {
                        popUpTo(Routes.CREATE)
                    }
                }
            )
        }

        composable(
            Routes.ZINE_READER,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
            enterTransition = {
                slideInVertically(spring(dampingRatio = 0.8f, stiffness = 320f)) { it / 3 } +
                    fadeIn(tween(220))
            },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(200)) },
            popExitTransition = {
                slideOutVertically(spring(dampingRatio = 0.8f, stiffness = 340f)) { it / 3 } +
                    fadeOut(tween(160))
            }
        ) { backStackEntry ->
            val zineId = backStackEntry.arguments?.getLong("id") ?: 0L
            ZineReaderScreen(
                application = application,
                zineId = zineId,
                onClose = { nav.popBackStack() }
            )
        }

        composable(Routes.COLLECTION) {
            CollectionScreen(
                application = application,
                onGoExplore = {
                    nav.navigate(Routes.EXPLORE) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onOpenZine = { id -> nav.navigate(Routes.zineReader(id)) },
                onEditZine = { id -> nav.navigate(Routes.zineEditor(zineId = id)) }
            )
        }
    }
}
