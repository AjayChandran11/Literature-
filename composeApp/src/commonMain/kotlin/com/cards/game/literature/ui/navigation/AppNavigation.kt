package com.cards.game.literature.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cards.game.literature.ui.game.GameBoardScreen
import com.cards.game.literature.ui.home.HomeScreen
import com.cards.game.literature.ui.result.ResultScreen

object Routes {
    const val HOME = "home"
    const val GAME = "game/{playerName}/{playerCount}"
    const val RESULT = "result"

    fun game(playerName: String, playerCount: Int) = "game/$playerName/$playerCount"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onStartGame = { playerName, playerCount ->
                    navController.navigate(Routes.game(playerName, playerCount))
                }
            )
        }
        composable(Routes.GAME) { backStackEntry ->
            val playerName = backStackEntry.arguments?.getString("playerName") ?: "Player"
            val playerCount = backStackEntry.arguments?.getString("playerCount")?.toIntOrNull() ?: 6
            GameBoardScreen(
                playerName = playerName,
                playerCount = playerCount,
                onGameEnd = {
                    navController.navigate(Routes.RESULT) {
                        popUpTo(Routes.HOME)
                    }
                }
            )
        }
        composable(Routes.RESULT) {
            ResultScreen(
                onPlayAgain = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                },
                onGoHome = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                }
            )
        }
    }
}
