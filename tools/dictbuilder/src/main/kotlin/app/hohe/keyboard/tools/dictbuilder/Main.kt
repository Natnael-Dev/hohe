package app.hohe.keyboard.tools.dictbuilder

import app.hohe.keyboard.tools.dictbuilder.amharic.AmharicBuild
import app.hohe.keyboard.tools.dictbuilder.english.EnglishBuild

/** Entry point: `gradlew :tools:dictbuilder:run --args="<amharic|english> ..."`. */
fun main(args: Array<String>) {
    val rest = args.drop(1).toTypedArray()
    when (args.firstOrNull()) {
        "amharic" -> AmharicBuild.run(rest)
        "english" -> EnglishBuild.run(rest)
        else -> {
            System.err.println("usage: dictbuilder <amharic|english> [options]")
            kotlin.system.exitProcess(2)
        }
    }
}
