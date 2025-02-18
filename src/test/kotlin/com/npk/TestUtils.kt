package com.npk

import java.time.LocalDate
import java.util.*
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.random.nextUInt


internal inline fun <T> random(block: TestValueGenerator.() -> T): T = TestValueGenerator.block()

internal object TestValueGenerator {

    enum class CountryType(val valueFun: (Locale) -> String) {
        Code({ locale -> locale.country }),
        Iso3({ locale -> locale.isO3Country }),
        Name({ locale -> locale.getDisplayName(Locale.ROOT) })
    }

    private val CHAR_POOL = ('A'..'Z') + ('a'..'z')
    private val SRAND = Random.Default

    private val availableNames = listOf(
        "Cod", "Coral", "Trout", "Red", "Salmon", "Tiger", "Prawn", "Rock", "Lobster", "Ray", "Manta",
        "Hazel", "Nutt", "Bacon", "Creem", "Mellow", "Barb", "Akew", "Tsar", "Lamb", "Cesar", "Doe"
    )
    private val availableLocalesByCountries by lazy {
        Locale.getISOCountries().map { countryCode -> Locale.of("", countryCode) }
    }
    private val availableCurrencies by lazy { Currency.getAvailableCurrencies() }


    fun id(): String = UUID.randomUUID().toString()

    fun name(): String = availableNames.random(SRAND)

    fun country(type: CountryType = CountryType.Code): String = type.valueFun(availableLocalesByCountries.random(SRAND))

    fun currency(): Currency = availableCurrencies.random(SRAND)

    fun email(): String = "${name()}.${name()}@email.com".lowercase(Locale.getDefault())

    fun phoneNumber(): String = "+1-202-555-" + "${int(1..9999)}".padStart(4, '0')

    fun filename(ext: String = "pdf"): String = buildString {
        repeat(SRAND.nextInt(6, 12)) {
            append(CHAR_POOL.random(SRAND))
        }
        append('.')
        append(ext)
    }

    fun string(range: IntRange = 5 .. 20): String = buildString {
        var spacePosition = (10 .. 15).random(SRAND)
        append(CHAR_POOL.random(SRAND))
        repeat(SRAND.nextInt(range)) {
            if (it == spacePosition) {
                append(' ')
                spacePosition = (spacePosition + 5 .. spacePosition + 15).random(SRAND)
            } else {
                append(CHAR_POOL.random(SRAND))
            }
        }
    }

    fun int(range: IntRange = 1..Int.MAX_VALUE): Int = SRAND.nextInt(range)

    fun double(): Double = SRAND.nextDouble()

    fun boolean(hitPercent: UInt = 50u): Boolean = hitPercent >= SRAND.nextUInt(1u..100u)

    fun localDate(): LocalDate = LocalDate.ofEpochDay(SRAND.nextLong(0, LocalDate.now().toEpochDay()))

    fun <K> from(vararg items: K): K = items.random(SRAND)

    fun <K> from(items: Collection<K>): K = items.random(SRAND)

}