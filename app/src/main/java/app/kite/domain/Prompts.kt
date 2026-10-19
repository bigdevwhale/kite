package app.kite.domain

import kotlin.random.Random

/** One thing to notice, in both languages. */
class Prompt(val en: String, val ru: String) {
    fun of(ru: Boolean) = if (ru) this.ru else en
}

/**
 * What to name while reeling in: one thing you see, one you hear, one you feel. Small and concrete,
 * so the answer is found in the room, never in your head.
 */
object Prompts {
    val see = listOf(
        Prompt("Find something blue near you.", "Найди рядом что‑нибудь синее."),
        Prompt("Look at the farthest thing you can see.", "Посмотри на самое дальнее, что видно."),
        Prompt("Notice where the light is coming from.", "Заметь, откуда падает свет."),
        Prompt("Find something that is moving.", "Найди что‑нибудь, что движется."),
        Prompt("Look at your hands for a moment.", "Посмотри на свои руки."),
        Prompt("Find the smallest thing in front of you.", "Найди самую маленькую вещь перед собой."),
        Prompt("Notice one shadow.", "Заметь одну тень."),
        Prompt("Find something round.", "Найди что‑нибудь круглое."),
        Prompt("Find a colour you haven’t noticed today.", "Найди цвет, который сегодня ещё не замечал."),
        Prompt("Find something green.", "Найди что‑нибудь зелёное."),
        Prompt("Notice a texture: wood, fabric, stone.", "Заметь фактуру: дерево, ткань, камень."),
        Prompt("Look up. What is above you?", "Подними глаза. Что над тобой?"),
        Prompt("Find a straight line and follow it.", "Найди прямую линию и пройди по ней взглядом."),
        Prompt("Notice something you see every day.", "Заметь вещь, которую видишь каждый день."),
    )

    val hear = listOf(
        Prompt("Find the quietest sound around you.", "Найди самый тихий звук вокруг."),
        Prompt("What is the farthest sound you can hear?", "Какой звук доносится издалека?"),
        Prompt("Listen for a sound that repeats.", "Прислушайся к звуку, который повторяется."),
        Prompt("Hear your own breath.", "Услышь своё дыхание."),
        Prompt("Notice a sound from outside.", "Заметь звук с улицы."),
        Prompt("What is the closest sound?", "Какой звук ближе всего?"),
        Prompt("Listen to the silence between sounds.", "Послушай тишину между звуками."),
        Prompt("Is there a hum somewhere? Find it.", "Где‑то гудит? Найди этот звук."),
        Prompt("Listen for a voice, near or far.", "Прислушайся к голосу, близкому или далёкому."),
        Prompt("Notice a sound you had stopped hearing.", "Заметь звук, который перестал слышать."),
        Prompt("Hear the sound your clothes make when you move.", "Услышь, как шуршит одежда, когда шевелишься."),
        Prompt("Which sound is the highest?", "Какой звук самый высокий?"),
    )

    val feel = listOf(
        Prompt("Feel your feet on the floor.", "Почувствуй стопы на полу."),
        Prompt("Notice the weight of your body where you sit or stand.", "Почувствуй вес тела там, где сидишь или стоишь."),
        Prompt("Feel the air on your face.", "Почувствуй воздух на лице."),
        Prompt("Notice how warm or cold your hands are.", "Тёплые сейчас руки или холодные?"),
        Prompt("Feel the fabric on your shoulders.", "Почувствуй ткань на плечах."),
        Prompt("Take one slow breath and feel your chest rise.", "Сделай один медленный вдох и почувствуй, как поднимается грудь."),
        Prompt("Unclench your jaw.", "Разожми челюсть."),
        Prompt("Let your shoulders drop.", "Опусти плечи."),
        Prompt("Press your fingertips together.", "Прижми друг к другу кончики пальцев."),
        Prompt("Feel your back against what you lean on.", "Почувствуй спиной то, на что опираешься."),
        Prompt("Notice the phone’s weight in your hand.", "Почувствуй вес телефона в руке."),
        Prompt("Find the most relaxed place in your body.", "Найди самое расслабленное место в теле."),
    )

    fun of(s: Sense): List<Prompt> = when (s) {
        Sense.SEE -> see
        Sense.HEAR -> hear
        Sense.FEEL -> feel
    }

    /** The three for one reel: one of each sense, chosen by [seed] so a reopened screen keeps them. */
    fun reel(seed: Long): List<Prompt> {
        val r = Random(seed)
        return Sense.entries.map { s -> of(s).let { it[r.nextInt(it.size)] } }
    }
}
