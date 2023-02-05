package `in`.samlav.noisegenerator

/**
 * Helper class for filtering white noise to be pink.
 * Created using values from http://www.cooperbaker.com/home/code/pink%20noise/
 *
 * @constructor Create empty Pink
 */
class Pink
{
    // Coefficients
    val a = listOf(0.000244106, 0.000976423, 0.003905693, 0.015622774, 0.062491095, 0.249964381, 0.999857524)

    // Gains
    val g = listOf(1.000000000, 0.501187234, 0.251188643, 0.125892541, 0.063095734, 0.031622777, 0.015848932)

    // States
    val y = mutableListOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
}