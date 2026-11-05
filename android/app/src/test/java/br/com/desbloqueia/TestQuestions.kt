package br.com.desbloqueia

/** Fixtures only in test/resources: not packaged with the app. */
object TestQuestions {
    fun text(): String=checkNotNull(javaClass.classLoader!!.getResourceAsStream("questions_fixture.json")).bufferedReader().use {it.readText()}
}
