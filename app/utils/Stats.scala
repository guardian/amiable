package utils

case class Percentiles(values: Seq[Int]) {

  private def percentile(s: Seq[Int], p: Double): Int = {
    if(p < 0 || p > 1) throw new RuntimeException("Percentile rank should be between 0 and 1")
    s.sortWith(_ < _)(Math.ceil((s.length - 1) * p).toInt)
  }

  def p95 = percentile(values, 0.95)
  def p90 = percentile(values, 0.90)
  def p75 = percentile(values, 0.75)
  def p50 = percentile(values, 0.50)
  def p25 = percentile(values, 0.25)
  def lowest = percentile(values, 0)
  def highest = percentile(values, 1)

}

