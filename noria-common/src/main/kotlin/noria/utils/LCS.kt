package noria.utils

import kotlin.math.max

fun lcs(a: IntArray, b: IntArray): IntArray {
    val lengths = Array(a.size + 1) { IntArray(b.size + 1) }

    // row 0 and column 0 are initialized to 0 already
    var len = 0
    for (i in a.indices)
        for (j in b.indices)
            if (a[i] == b[j]) {
                len = lengths[i][j] + 1
                lengths[i + 1][j + 1] = len
            } else {
                len = max(lengths[i + 1][j], lengths[i][j + 1])
                lengths[i + 1][j + 1] = len
            }

    // read the substring out from the matrix
    val result = IntArray(len)
    var x = a.size
    var y = b.size
    var i = 0
    while (x != 0 && y != 0) {
        if (lengths[x][y] == lengths[x - 1][y])
            x--
        else if (lengths[x][y] == lengths[x][y - 1])
            y--
        else {
            result[i++] = a[x - 1]
            x--
            y--
        }
    }
    return result
}
