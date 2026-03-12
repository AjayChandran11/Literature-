package com.cards.game.literature.di

import com.cards.game.literature.bot.BotPlayer
import com.cards.game.literature.bot.BotStrategy
import com.cards.game.literature.logic.CardTracker
import com.cards.game.literature.logic.GameEngine
import com.cards.game.literature.repository.GameRepository
import com.cards.game.literature.repository.LocalGameRepository
import com.cards.game.literature.viewmodel.GameViewModel
import com.cards.game.literature.viewmodel.ResultViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { GameEngine() }
    single { CardTracker() }
    single { BotStrategy(get()) }
    single { BotPlayer(get()) }
    single<GameRepository> { LocalGameRepository(get(), get()) }
    viewModel { GameViewModel(get()) }
    viewModel { ResultViewModel(get()) }
}
