package com.cards.game.literature

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform