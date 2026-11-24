package app.hohe.keyboard.amharic.dict

/**
 * `am_words.bin` format, version 1. All integers little-endian.
 *
 * ```
 * off size field
 *  0   4   magic "AMWL"
 *  4   2   format version (1)
 *  6   2   flags (0)
 *  8   4   nWords
 * 12   4   nSkel
 * 16   4   offWordStart  u32[nWords + 1]  char offset of each word in CHARS (last = total chars)
 * 20   4   offFreq       u16[nWords]      q = round(64 * ln(freq + 1))
 * 24   4   offChars      UTF-16LE Ethiopic text, words concatenated without separators
 * 28   4   offSkelStart  u32[nSkel + 1]   byte offset of each skeleton in SKELBYTES
 * 32   4   offSkelFirst  u32[nSkel + 1]   first word index of each skeleton bucket (last = nWords)
 * 36   4   offSkelBytes  ASCII skeleton strings, sorted ascending by byte value
 * 40   4   offMeta       3 x (u16 length + UTF-8): data version, licence id, attribution
 * 44   4   total length of the file
 * 48   4   CRC-32 of bytes [0, 48) followed by bytes [52, total)
 * ```
 *
 * Words are grouped by consonant skeleton ([Fidel.skeletonOf]); inside a bucket they are sorted by
 * descending frequency, then by text. A skeleton prefix range is therefore a contiguous word range.
 */
object AmharicDictionaryFormat {
    const val MAGIC: Int = 0x4C574D41 // "AMWL" read little-endian
    const val VERSION: Int = 1
    const val HEADER_SIZE: Int = 52
    const val CRC_OFFSET: Int = 48

    /** Frequency quantisation shared by writer and ranker. */
    fun quantise(freq: Long): Int = Math.round(64.0 * Math.log(freq.toDouble() + 1.0)).toInt().coerceIn(0, 0xFFFF)
}

/** Thrown by [AmharicDictionary.parse] for a missing, corrupt or version-mismatched file. */
class DictionaryFormatException(
    message: String,
) : Exception(message)
