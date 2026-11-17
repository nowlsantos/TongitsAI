package com.nowlsantos.tongitsai.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class FamousTrio(
    val groupTitle: String,
    val humanName: String,
    val ai1Name: String,
    val ai2Name: String
) {
    companion object {
        val PRESETS = listOf(
            FamousTrio("The Enterprise", "JT Kirk", "Mr Spock", "Dr McCoy"),
            FamousTrio("The Wizards", "Harry", "Hermione", "Ron"),
            FamousTrio("The Rebels", "Luke", "Lea", "Hans"),
            FamousTrio("The Lords", "Aragorn", "Legolas", "Gimli"),
            FamousTrio("The Musketeers", "Athos", "Porthos", "Aramis"),
            FamousTrio("The Disneys", "Mickey", "Donald", "Goofy"),
            FamousTrio("The SquarePants", "Spongebob", "Patrick", "Squidward"),
            FamousTrio("The Chipmunks", "Alvin", "Simon", "Theodore"),
            FamousTrio("The Comics", "Curly", "Moe", "Larry"),
            //FamousTrio("The Loonies", "Bugs Bunny", "Duffy Duck", "Porky Pig"),
            //FamousTrio("The GhostBusters", "Venkman", "Stantz", "Spengler"),
            //FamousTrio("The Amigos", "Dusty", "Lucky", "Ned"),
        )
    }
}
