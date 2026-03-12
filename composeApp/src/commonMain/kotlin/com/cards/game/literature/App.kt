package com.cards.game.literature

import androidx.compose.runtime.Composable
import com.cards.game.literature.di.appModule
import com.cards.game.literature.ui.navigation.AppNavigation
import com.cards.game.literature.ui.theme.LiteratureTheme
import org.koin.compose.KoinApplication

@Composable
fun App() {
    KoinApplication(application = {
        modules(appModule)
    }) {
        LiteratureTheme {
            AppNavigation()
        }
    }
}
