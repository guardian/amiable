package utils

case class Percentiles(values: Seq[Int]) {

  private val percentilesRange: Range = 0 to 100
  private def percentile(s: Seq[Int], p: Int): Option[Int] = {
    if (p < percentilesRange.min || p > percentilesRange.max) None
    s.sortWith(_ < _) match {
      case Nil         => None
      case nonEmptySeq =>
        Some(
          nonEmptySeq(
            Math.ceil((s.length - 1) * p / percentilesRange.max).toInt
          )
        )
    }
  }

  def p95 = percentile(values, 95)
  def p90 = percentile(values, 90)
  def p75 = percentile(values, 75)
  def p50 = percentile(values, 50)
  def p25 = percentile(values, 25)
  def lowest = percentile(values, percentilesRange.min)
  def highest = percentile(values, percentilesRange.max)

}
