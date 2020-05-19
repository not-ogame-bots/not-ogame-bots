package not.ogame.bots

package object facts {
  implicit class RichDouble(value: Double) {
    def pow(power: Double): Double = Math.pow(value, power)
  }
}
